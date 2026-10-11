package com.emnio.windowspc.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emnio.windowspc.model.HardwareConfig
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

enum class AppId(val title: String, val icon: ImageVector, val color: Color) {
    THIS_PC("Este Computador", Icons.Filled.Computer, Color(0xFF1E88E5)),
    DEVICES("Gerenciador de Dispositivos", Icons.Filled.Memory, Color(0xFF607D8B)),
    BROWSER("Navegador Web", Icons.Filled.Language, Color(0xFF00A99D)),
    ROBLOX("Roblox / Steam Launcher", Icons.Filled.SportsEsports, Color(0xFFE53935))
}

enum class InputMode { TOUCHPAD, GAMEPAD }

private val WinBlue = Color(0xFF4CC2FF)

/* ---------- Registro de alvos clicáveis (permite o "mouse virtual" clicar nos elementos) ---------- */
class ClickRegistry {
    val targets = LinkedHashMap<String, Pair<Rect, () -> Unit>>()
    fun click(p: Offset): Boolean {
        val hit = targets.entries.toList().lastOrNull { it.value.first.contains(p) } ?: return false
        hit.value.second(); return true
    }
    fun idAt(p: Offset, prefix: String): String? =
        targets.entries.toList().lastOrNull { it.key.startsWith(prefix) && it.value.first.contains(p) }?.key
}

fun Modifier.registerBounds(reg: ClickRegistry, id: String, onClick: () -> Unit = {}): Modifier = composed {
    val latest by rememberUpdatedState(onClick)
    DisposableEffect(id) { onDispose { reg.targets.remove(id) } }
    onGloballyPositioned { reg.targets[id] = it.boundsInRoot() to { latest() } }
}

fun Modifier.target(reg: ClickRegistry, id: String, onClick: () -> Unit): Modifier =
    registerBounds(reg, id, onClick).clickable(onClick = onClick)

/* ------------------------------------- Tela principal ------------------------------------------ */
@Composable
fun WindowsDesktopScreenUI(config: HardwareConfig, onShutdown: () -> Unit) {
    val reg = remember { ClickRegistry() }
    val open = remember { mutableStateListOf<AppId>() }
    val offsets = remember { mutableStateMapOf<AppId, IntOffset>() }
    var cursor by remember { mutableStateOf(Offset(500f, 300f)) }
    var screen by remember { mutableStateOf(IntSize.Zero) }
    var mode by remember { mutableStateOf(InputMode.TOUCHPAD) }
    var sensitivity by remember { mutableFloatStateOf(1.6f) }
    var showKeyboard by remember { mutableStateOf(false) }
    var showTune by remember { mutableStateOf(false) }
    var startOpen by remember { mutableStateOf(false) }
    var held by remember { mutableStateOf<String?>(null) }
    var context by remember { mutableStateOf<Offset?>(null) }
    var buffer by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var padState by remember { mutableStateOf("") }
    val density = LocalDensity.current
    val measuredFps = rememberMeasuredFps()

    fun openApp(app: AppId) {
        if (app in open) open.remove(app) else {
            val i = open.size
            offsets[app] = with(density) {
                IntOffset((90.dp.toPx() + i * 28.dp.toPx()).roundToInt(), (14.dp.toPx() + i * 22.dp.toPx()).roundToInt())
            }
        }
        open.add(app); startOpen = false
    }

    fun handleKey(k: String) {
        when (k) {
            "Esc" -> { startOpen = false; context = null }
            "Ctrl+W" -> open.lastOrNull()?.let { open.remove(it) }
            "⌫" -> buffer = buffer.dropLast(1)
            "Space" -> buffer += " "
            "Enter" -> if (buffer.isNotBlank()) {
                url = buffer; buffer = ""
                if (AppId.BROWSER !in open) openApp(AppId.BROWSER)
            }
            else -> if (k.length == 1) buffer += k
        }
    }

    fun clickAtCursor() {
        context = null
        if (!reg.click(cursor)) startOpen = false
    }

    Box(Modifier.fillMaxSize().onSizeChanged { screen = it }) {
        // Papel de parede conforme o Windows escolhido (toque no vazio fecha menus)
        Image(
            painterResource(wallpaperFor(config.os)), null,
            Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { startOpen = false; context = null }) },
            contentScale = ContentScale.Crop
        )

        // Ícones da área de trabalho
        Column(
            Modifier.align(Alignment.TopStart).padding(start = 10.dp, top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AppId.entries.forEach { app -> DesktopIcon(app, reg) { openApp(app) } }
        }

        // Janelas
        open.toList().forEach { app ->
            key(app) {
                WinWindow(app, offsets[app] ?: IntOffset.Zero, { offsets[app] = it }, reg, { open.remove(app) }) {
                    when (app) {
                        AppId.THIS_PC -> ThisPcUI(config)
                        AppId.DEVICES -> VirtualDeviceManagerUI(config)
                        AppId.BROWSER -> BrowserUI(buffer, url)
                        AppId.ROBLOX -> RobloxLauncherUI(config)
                    }
                }
            }
        }

        // Menu de contexto (clique direito)
        context?.let { p ->
            Column(
                Modifier.offset { IntOffset(p.x.roundToInt(), p.y.roundToInt()) }.width(170.dp)
                    .clip(RoundedCornerShape(8.dp)).background(Color(0xF2202030)).padding(6.dp)
            ) {
                listOf("Exibir", "Atualizar", "Novo", "Configurações de exibição", "Personalizar")
                    .forEach { Text(it, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(5.dp)) }
            }
        }

        // Menu Iniciar
        if (startOpen) {
            Box(Modifier.align(if (config.os.isWin11) Alignment.BottomCenter else Alignment.BottomStart).padding(start = 6.dp, bottom = 44.dp)) {
                StartMenu(reg, { openApp(it) }, onShutdown)
            }
        }

        Taskbar(
            Modifier.align(Alignment.BottomCenter), reg, config.os.isWin11, open, startOpen, buffer,
            onStart = { startOpen = !startOpen }, onApp = { openApp(it) }
        )

        // Entrada: touchpad ou gamepad
        if (mode == InputMode.TOUCHPAD) {
            TouchpadPad(
                Modifier.align(Alignment.BottomStart).padding(start = 12.dp, bottom = 52.dp),
                sensitivity,
                onMove = { d ->
                    cursor = Offset(
                        (cursor.x + d.x).coerceIn(0f, screen.width.toFloat()),
                        (cursor.y + d.y).coerceIn(0f, screen.height.toFloat())
                    )
                    held?.let { id ->
                        val app = AppId.valueOf(id.removePrefix("title-"))
                        offsets[app] = (offsets[app] ?: IntOffset.Zero) + IntOffset(d.x.roundToInt(), d.y.roundToInt())
                    }
                },
                onLeftClick = { clickAtCursor() },
                onRightClick = { context = cursor },
                onHold = { on -> held = if (on) reg.idAt(cursor, "title-") else null }
            )
        } else {
            GamepadOverlay({ padState = it })
        }

        if (showKeyboard) {
            VirtualKeyboard(Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 46.dp)) { handleKey(it) }
        }

        ControlOverlay(
            Modifier.align(Alignment.TopEnd), measuredFps, config.fps, mode, { mode = it },
            showKeyboard, { showKeyboard = !showKeyboard }, showTune, { showTune = !showTune },
            sensitivity, { sensitivity = it }, padState
        )

        if (mode == InputMode.TOUCHPAD) VirtualCursor(cursor, held != null)
    }
}

/* ------------------------------------------ Medidores ------------------------------------------ */
/** FPS REAL de renderização do app (limitado pela taxa de atualização da tela). Não é FPS de jogo. */
@Composable
fun rememberMeasuredFps(): Int {
    var fps by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        var frames = 0; var last = 0L
        while (true) {
            withFrameNanos { t ->
                if (last == 0L) last = t
                frames++
                if (t - last >= 1_000_000_000L) { fps = frames; frames = 0; last = t }
            }
        }
    }
    return fps
}

@Composable
fun rememberBatteryPercent(): Int {
    val ctx = LocalContext.current
    var pct by remember { mutableIntStateOf(-1) }
    DisposableEffect(Unit) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val s = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                if (l >= 0) pct = l * 100 / s
            }
        }
        ctx.registerReceiver(r, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let { r.onReceive(ctx, it) }
        onDispose { ctx.unregisterReceiver(r) }
    }
    return pct
}

/* --------------------------------------- Área de trabalho -------------------------------------- */
@Composable
fun AppIconTile(app: AppId, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.24f))
            .background(Brush.verticalGradient(listOf(app.color, app.color.copy(alpha = .7f)))),
        contentAlignment = Alignment.Center
    ) { Icon(app.icon, null, tint = Color.White, modifier = Modifier.size(size * 0.6f)) }
}

@Composable
fun DesktopIcon(app: AppId, reg: ClickRegistry, onOpen: () -> Unit) {
    Column(
        Modifier.width(78.dp).target(reg, "icon-${app.name}", onOpen).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIconTile(app, 38.dp)
        Spacer(Modifier.height(3.dp))
        Text(
            app.title, color = Color.White, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 12.sp,
            style = TextStyle(shadow = Shadow(Color.Black, Offset(1f, 1f), 6f))
        )
    }
}

@Composable
fun WinWindow(
    app: AppId, offset: IntOffset, onOffset: (IntOffset) -> Unit, reg: ClickRegistry,
    onClose: () -> Unit, content: @Composable () -> Unit
) {
    val current by rememberUpdatedState(offset)
    val shape = RoundedCornerShape(8.dp)
    val pop = remember { Animatable(0.92f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, tween(150)) }
    Column(
        Modifier.offset { offset }.graphicsLayer { scaleX = pop.value; scaleY = pop.value; alpha = ((pop.value - 0.92f) * 12.5f).coerceIn(0f, 1f) }
            .width(440.dp).height(250.dp)
            .shadow(14.dp, shape).clip(shape).background(Color.White)
    ) {
        Row(
            Modifier.fillMaxWidth().height(30.dp).background(Color(0xFF2B2F3A))
                .registerBounds(reg, "title-${app.name}")
                .pointerInput(app) {
                    detectDragGestures { ch, d ->
                        ch.consume()
                        onOffset(current + IntOffset(d.x.roundToInt(), d.y.roundToInt()))
                    }
                }
                .padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(app.icon, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(app.title, color = Color.White, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f))
            Box(
                Modifier.width(44.dp).fillMaxHeight().target(reg, "close-${app.name}", onClose),
                contentAlignment = Alignment.Center
            ) { Text("✕", color = Color.White, fontSize = 13.sp) }
        }
        Box(Modifier.fillMaxSize()) { content() }
    }
}

/* -------------------------------------- Conteúdo das janelas ----------------------------------- */
@Composable
fun ThisPcUI(config: HardwareConfig) {
    val usedC = if (config.os.isWin11) 27.0 else 21.0
    val totalC = config.storageGb * 0.75
    val totalD = config.storageGb * 0.25
    Column(
        Modifier.fillMaxSize().padding(10.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Dispositivos e unidades (3)", color = Color(0xFF1F3A5F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        DriveRow("Disco Local (C:) · Windows", totalC, usedC)
        DriveRow("Dados (D:) · partição virtual", totalD, 0.4)
        Text("Partição EFI (100 MB) e Recuperação (650 MB) ocultas", color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
private fun DriveRow(name: String, total: Double, used: Double) {
    Column {
        Text(name, color = Color.Black, fontSize = 12.sp)
        LinearProgressIndicator(
            progress = { (used / total).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            color = Color(0xFF26A0DA), trackColor = Color(0xFFE6E6E6)
        )
        Text("%.1f GB livres de %.1f GB".format(total - used, total), color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun BrowserUI(buffer: String, url: String) {
    Column(Modifier.fillMaxSize().background(Color(0xFFF3F3F3))) {
        Box(
            Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(14.dp)).background(Color.White).padding(8.dp)
        ) {
            Text(buffer.ifEmpty { url.ifBlank { "https://www.roblox.com" } }, color = Color.Black, fontSize = 12.sp, maxLines = 1)
        }
        Column(Modifier.padding(12.dp)) {
            Text(if (url.isBlank()) "Página inicial" else "Resultados para “$url”", color = Color(0xFF111111), fontWeight = FontWeight.Bold)
            Text("Navegador simulado. Para navegação real, troque este bloco por um WebView.", color = Color.Gray, fontSize = 11.sp)
        }
    }
}

@Composable
fun RobloxLauncherUI(config: HardwareConfig) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(3500)) }
    Column(
        Modifier.fillMaxSize().background(Color(0xFF14172A)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("ROBLOX · ALTA PERFORMANCE", color = EmnioColors.Neon, fontWeight = FontWeight.Black)
        LinearProgressIndicator(progress = { p.value }, modifier = Modifier.fillMaxWidth(), color = EmnioColors.Purple)
        Text(
            if (p.value < 1f) "Iniciando… ${(p.value * 100).roundToInt()}%"
            else "Pronto (simulação) · alvo ${config.fps} FPS @ ${config.resolution.w}x${config.resolution.h}",
            color = Color.White, fontSize = 12.sp
        )
        Text("O jogo real só roda em um PC remoto conectado via Remote Desktop.", color = Color.White.copy(.5f), fontSize = 11.sp)
    }
}

/* ----------------------------------------- Barra de tarefas ------------------------------------ */
@Composable
fun Taskbar(
    modifier: Modifier, reg: ClickRegistry, win11: Boolean, open: List<AppId>, startOpen: Boolean, buffer: String,
    onStart: () -> Unit, onApp: (AppId) -> Unit
) {
    val battery = rememberBatteryPercent()
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { now = LocalDateTime.now(); delay(1000) } }

    Box(modifier.fillMaxWidth().height(40.dp).background(if (win11) Color(0xD91B1F2B) else Color(0xEB101820))) {
        Row(
            Modifier.align(if (win11) Alignment.Center else Alignment.CenterStart).padding(start = if (win11) 0.dp else 6.dp).fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(6.dp)).target(reg, "start", onStart),
                contentAlignment = Alignment.Center
            ) { WindowsLogo(win11, Modifier.size(16.dp)) }
            Spacer(Modifier.width(6.dp))
            Row(
                Modifier.width(if (win11) 130.dp else 170.dp).height(28.dp)
                    .clip(RoundedCornerShape(if (win11) 14.dp else 2.dp)).background(Color(0x33FFFFFF)).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (startOpen && buffer.isNotEmpty()) buffer else "Pesquisar", color = Color.White, fontSize = 12.sp, maxLines = 1)
            }
            Spacer(Modifier.width(8.dp))
            open.forEach { app ->
                Box(Modifier.padding(horizontal = 2.dp).size(34.dp).target(reg, "tb-${app.name}") { onApp(app) }.padding(5.dp)) {
                    AppIconTile(app, 24.dp)
                }
            }
        }
        Row(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Wifi, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.VolumeUp, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.BatteryFull, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text(if (battery >= 0) "$battery%" else "--", color = Color.White, fontSize = 10.sp)
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(now.format(DateTimeFormatter.ofPattern("HH:mm")), color = Color.White, fontSize = 11.sp)
                Text(now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), color = Color.White, fontSize = 9.sp)
            }
        }
    }
}

@Composable
fun StartMenu(reg: ClickRegistry, onOpen: (AppId) -> Unit, onShutdown: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.width(230.dp).clip(shape).background(Color(0xF2202030)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Fixados", color = Color.White.copy(.6f), fontSize = 11.sp)
        AppId.entries.forEach { app ->
            Row(
                Modifier.fillMaxWidth().target(reg, "sm-${app.name}") { onOpen(app) }.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconTile(app, 22.dp)
                Spacer(Modifier.width(8.dp))
                Text(app.title, color = Color.White, fontSize = 13.sp)
            }
        }
        HorizontalDivider(color = Color.White.copy(.2f))
        Row(
            Modifier.fillMaxWidth().target(reg, "sm-off", onShutdown).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.PowerSettingsNew, null, tint = Color(0xFFFF6B6B), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Desligar", color = Color.White, fontSize = 13.sp)
        }
    }
}

/* ------------------------------------- Overlay de controle -------------------------------------- */
@Composable
fun ControlOverlay(
    modifier: Modifier, fps: Int, target: Int, mode: InputMode, onMode: (InputMode) -> Unit,
    keyboardOn: Boolean, onKeyboard: () -> Unit, tuneOpen: Boolean, onTune: () -> Unit,
    sensitivity: Float, onSensitivity: (Float) -> Unit, padState: String
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.padding(8.dp).clip(shape).background(Color(0xCC000000)).border(1.dp, EmnioColors.Neon, shape).padding(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text("UI $fps FPS · alvo $target", color = EmnioColors.Neon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Row {
            IconToggle(Icons.Filled.Keyboard, keyboardOn, onKeyboard)
            IconToggle(Icons.Filled.TouchApp, mode == InputMode.TOUCHPAD) { onMode(InputMode.TOUCHPAD) }
            IconToggle(Icons.Filled.Gamepad, mode == InputMode.GAMEPAD) { onMode(InputMode.GAMEPAD) }
            IconToggle(Icons.Filled.Tune, tuneOpen, onTune)
        }
        if (tuneOpen) {
            Text("Sensibilidade %.1fx".format(sensitivity), color = Color.White, fontSize = 11.sp)
            Slider(sensitivity, onSensitivity, valueRange = 0.5f..4f, modifier = Modifier.width(160.dp))
        }
        if (mode == InputMode.GAMEPAD) Text(padState, color = Color.White, fontSize = 10.sp)
    }
}

@Composable
private fun IconToggle(icon: ImageVector, on: Boolean, onClick: () -> Unit) {
    IconButton(onClick, Modifier.size(34.dp)) {
        Icon(icon, null, tint = if (on) EmnioColors.Neon else Color.White)
    }
}

/* --------------------------------- Teclado virtual completo ------------------------------------ */
@Composable
fun VirtualKeyboard(modifier: Modifier, onKey: (String) -> Unit) {
    var shift by remember { mutableStateOf(false) }
    var ctrl by remember { mutableStateOf(false) }
    var alt by remember { mutableStateOf(false) }
    val rows = listOf(
        listOf("Esc", "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "⌫"),
        listOf("Tab", "q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("Shift", "a", "s", "d", "f", "g", "h", "j", "k", "l", "Enter"),
        listOf("Ctrl", "Alt", "z", "x", "c", "v", "b", "n", "m", "←", "↑", "↓", "→"),
        listOf("Space")
    )

    fun press(k: String) {
        when (k) {
            "Shift" -> shift = !shift
            "Ctrl" -> ctrl = !ctrl
            "Alt" -> alt = !alt
            else -> {
                val out = if (shift && k.length == 1 && k[0].isLetter()) k.uppercase() else k
                val prefix = (if (ctrl) "Ctrl+" else "") + (if (alt) "Alt+" else "")
                onKey(if (prefix.isNotEmpty()) prefix + out.uppercase() else out)
                shift = false; ctrl = false; alt = false
            }
        }
    }

    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier.width(500.dp).clip(shape).background(Color(0xE60F111A)).border(1.dp, EmnioColors.Purple, shape).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEach { k ->
                    val active = (k == "Shift" && shift) || (k == "Ctrl" && ctrl) || (k == "Alt" && alt)
                    Box(
                        Modifier.weight(1f).height(30.dp).clip(RoundedCornerShape(5.dp))
                            .background(if (active) EmnioColors.Neon.copy(.5f) else Color(0xFF2A2D3E))
                            .clickable { press(k) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (shift && k.length == 1) k.uppercase() else k, color = Color.White, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/* -------------------------------------- Touchpad / mouse virtual -------------------------------- */
@Composable
fun TouchpadPad(
    modifier: Modifier, sensitivity: Float,
    onMove: (Offset) -> Unit, onLeftClick: () -> Unit, onRightClick: () -> Unit, onHold: (Boolean) -> Unit
) {
    val move by rememberUpdatedState(onMove)
    val left by rememberUpdatedState(onLeftClick)
    val right by rememberUpdatedState(onRightClick)
    val hold by rememberUpdatedState(onHold)
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier.size(220.dp, 120.dp).clip(shape).background(Color(0x99000000)).border(1.dp, EmnioColors.Neon, shape)
            .pointerInput(sensitivity) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val start = down.uptimeMillis
                    var maxPointers = 1
                    var travel = 0f
                    var dragMode = false
                    while (true) {
                        val ev = awaitPointerEvent()
                        val pressed = ev.changes.count { it.pressed }
                        if (pressed > maxPointers) maxPointers = pressed
                        val ch = ev.changes.first()
                        val delta = ch.position - ch.previousPosition
                        if (pressed == 1 && maxPointers == 1) {
                            if (!dragMode && travel < 8f && ch.uptimeMillis - start > 450) { dragMode = true; hold(true) }
                            move(delta * sensitivity)
                        }
                        travel += delta.getDistance()
                        ev.changes.forEach { it.consume() }
                        if (ev.changes.all { !it.pressed }) {
                            val dur = ch.uptimeMillis - start
                            if (dragMode) hold(false)
                            else if (travel < 12f && dur < 300) { if (maxPointers >= 2) right() else left() }
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "TOUCHPAD\ntoque = clique · 2 dedos = direito\nsegure e arraste = arrastar/soltar",
            color = Color.White.copy(.5f), fontSize = 10.sp, textAlign = TextAlign.Center
        )
    }
}

@Composable
fun VirtualCursor(cursor: Offset, held: Boolean) {
    Canvas(Modifier.offset { IntOffset(cursor.x.roundToInt(), cursor.y.roundToInt()) }.size(16.dp, 22.dp)) {
        val w = size.width; val h = size.height
        val path = Path().apply {
            moveTo(0f, 0f); lineTo(0f, h * .85f); lineTo(w * .3f, h * .66f); lineTo(w * .52f, h)
            lineTo(w * .72f, h * .9f); lineTo(w * .5f, h * .6f); lineTo(w * .85f, h * .6f); close()
        }
        drawPath(path, if (held) EmnioColors.Neon else Color.White)
        drawPath(path, Color.Black, style = Stroke(width = 2f))
    }
}

/* ------------------------------------ Gamepad na tela (analógico + botões) ---------------------- */
@Composable
fun GamepadOverlay(onUpdate: (String) -> Unit) {
    val r = with(LocalDensity.current) { 55.dp.toPx() }
    var knob by remember { mutableStateOf(Offset.Zero) }
    var btn by remember { mutableStateOf("") }
    LaunchedEffect(knob, btn) { onUpdate("Stick (%.2f, %.2f) %s".format(knob.x / r, knob.y / r, btn)) }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.align(Alignment.BottomStart).padding(start = 30.dp, bottom = 60.dp).size(110.dp)
                .clip(CircleShape).background(Color(0x66000000)).border(2.dp, EmnioColors.Neon, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(onDragEnd = { knob = Offset.Zero }, onDragCancel = { knob = Offset.Zero }) { ch, d ->
                        ch.consume()
                        val n = knob + d
                        knob = if (n.getDistance() > r) n * (r / n.getDistance()) else n
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.offset { IntOffset(knob.x.roundToInt(), knob.y.roundToInt()) }
                    .size(46.dp).clip(CircleShape).background(EmnioColors.Neon.copy(.8f))
            )
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 60.dp).size(130.dp)) {
            PadButton("Y", Modifier.align(Alignment.TopCenter)) { btn = it }
            PadButton("X", Modifier.align(Alignment.CenterStart)) { btn = it }
            PadButton("B", Modifier.align(Alignment.CenterEnd)) { btn = it }
            PadButton("A", Modifier.align(Alignment.BottomCenter)) { btn = it }
        }
    }
}

@Composable
private fun PadButton(label: String, modifier: Modifier, onState: (String) -> Unit) {
    Box(
        modifier.size(42.dp).clip(CircleShape).background(EmnioColors.Purple.copy(.7f))
            .border(1.dp, Color.White.copy(.5f), CircleShape)
            .pointerInput(Unit) { detectTapGestures(onPress = { onState(label); tryAwaitRelease(); onState("") }) },
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color.White, fontWeight = FontWeight.Bold) }
}
