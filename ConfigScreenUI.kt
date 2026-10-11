package com.emnio.windowspc.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.emnio.windowspc.R
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emnio.windowspc.model.*
import com.emnio.windowspc.remote.RemoteDesktopLauncher
import com.emnio.windowspc.viewmodel.HardwareConfigViewModel
import kotlin.math.roundToInt

@Composable
fun ConfigScreenUI(vm: HardwareConfigViewModel, onBoot: () -> Unit) {
    val c by vm.config.collectAsState()
    val context = LocalContext.current

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(EmnioColors.Bg, Color(0xFF1B1038))))
    ) {
        LazyColumn(
            Modifier.fillMaxSize().systemBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                val bs = RoundedCornerShape(20.dp)
                Box(Modifier.fillMaxWidth().height(190.dp).clip(bs).border(1.dp, EmnioColors.Neon.copy(.6f), bs)) {
                    Image(painterResource(R.drawable.hero_banner), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC0F111A)))))
                    Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                        Text("EMNIO WINDOWS PC", color = EmnioColors.Neon, fontSize = 26.sp,
                            fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                        Text("Configure seu PC virtual", color = Color.White.copy(alpha = .8f))
                    }
                }
            }

            item { MachinePreview(c) }

            item {
                Section("1 · SISTEMA OPERACIONAL") {
                    WinEdition.entries.forEach { os ->
                        SelectCard(os.label, "Build ${os.build}", c.os == os, leading = { OsThumb(os) }) {
                            vm.update { cfg -> cfg.copy(os = os) }
                        }
                    }
                }
            }

            item {
                Section("2 · HARDWARE VIRTUAL") {
                    val idx = Options.ram.indexOf(c.ramGb).coerceAtLeast(0)
                    Text("Memória RAM: ${c.ramGb} GB", color = Color.White, fontWeight = FontWeight.Bold)
                    Slider(
                        value = idx.toFloat(),
                        onValueChange = { v -> vm.update { cfg -> cfg.copy(ramGb = Options.ram[v.roundToInt()]) } },
                        valueRange = 0f..(Options.ram.size - 1).toFloat(),
                        steps = Options.ram.size - 2,
                        colors = SliderDefaults.colors(thumbColor = EmnioColors.Neon, activeTrackColor = EmnioColors.Purple)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Options.ram.forEach { Text("$it GB", color = Color.White.copy(.6f), fontSize = 11.sp) }
                    }
                    Text("Processador virtual (vCPU)", color = Color.White, fontWeight = FontWeight.Bold)
                    ChipGroup(Options.vcpu, c.vcpu, { "$it Cores" }) { v -> vm.update { cfg -> cfg.copy(vcpu = v) } }
                    Text("Placa de vídeo virtual (vGPU)", color = Color.White, fontWeight = FontWeight.Bold)
                    VGpu.entries.forEach { g ->
                        SelectCard(g.label, null, c.gpu == g, leading = { GpuBadge(g) }) { vm.update { cfg -> cfg.copy(gpu = g) } }
                    }
                }
            }

            item {
                Section("3 · ARMAZENAMENTO (SSD VIRTUAL / VHD)") {
                    ChipGroup(Options.storage, c.storageGb, { "$it GB" }) { v -> vm.update { cfg -> cfg.copy(storageGb = v) } }
                }
            }

            item {
                Section("4 · DESEMPENHO") {
                    Text("Limite de FPS", color = Color.White, fontWeight = FontWeight.Bold)
                    ChipGroup(Options.fps, c.fps, { "$it FPS" }) { v -> vm.update { cfg -> cfg.copy(fps = v) } }
                    Text("Resolução virtual", color = Color.White, fontWeight = FontWeight.Bold)
                    ChipGroup(VResolution.entries.toList(), c.resolution, { it.label }) { v ->
                        vm.update { cfg -> cfg.copy(resolution = v) }
                    }
                }
            }

            item {
                Section("5 · PC REMOTO REAL (OPCIONAL)") {
                    Text(
                        "Para jogar de verdade (ex.: Roblox a 240 FPS), conecte a um PC/nuvem com GPU via Microsoft Remote Desktop.",
                        color = Color.White.copy(.7f), fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = c.remoteHost,
                        onValueChange = { v -> vm.update { cfg -> cfg.copy(remoteHost = v) } },
                        label = { Text("Endereço (IP ou host:porta)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = { RemoteDesktopLauncher.open(context, c.remoteHost) },
                        enabled = c.remoteHost.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("CONECTAR VIA REMOTE DESKTOP") }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                GlowButton("INICIAR MÁQUINA VIRTUAL / BOOT SYSTEM", onBoot)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(EmnioColors.Card)
            .border(1.dp, EmnioColors.Purple.copy(.5f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, color = EmnioColors.Neon, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
        content()
    }
}

@Composable
fun SelectCard(
    title: String, subtitle: String?, selected: Boolean,
    leading: (@Composable () -> Unit)? = null, onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(if (selected) EmnioColors.Purple.copy(.25f) else Color(0xFF14172A))
            .border(1.5.dp, if (selected) EmnioColors.Neon else Color.White.copy(.1f), shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected, onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = EmnioColors.Neon))
        if (leading != null) { Spacer(Modifier.width(4.dp)); leading(); Spacer(Modifier.width(8.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp)
            if (subtitle != null) Text(subtitle, color = Color.White.copy(.5f), fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipGroup(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { o ->
            FilterChip(
                selected = o == selected,
                onClick = { onSelect(o) },
                label = { Text(label(o)) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = Color.White,
                    selectedContainerColor = EmnioColors.Purple,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
fun GlowButton(text: String, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "glow")
    val glow by t.animateFloat(
        initialValue = 8f, targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "g"
    )
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.fillMaxWidth().height(60.dp)
            .shadow(glow.dp, shape, ambientColor = EmnioColors.Neon, spotColor = EmnioColors.Neon)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(EmnioColors.Neon, EmnioColors.Purple)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.sp)
    }
}
