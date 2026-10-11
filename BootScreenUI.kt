package com.emnio.windowspc.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emnio.windowspc.model.HardwareConfig
import kotlinx.coroutines.delay

private fun bootLog(c: HardwareConfig) = listOf(
    "[Simulação local · nenhum Windows real está sendo executado]",
    "Emnio Virtual BIOS v1.0 — POST OK",
    "Hardware virtual: ${c.vcpu} vCPU · ${c.ramGb} GB RAM",
    "[ OK ] Carregando winload.efi",
    "[ OK ] Carregando ntoskrnl.exe",
    "[ OK ] Carregando hal.dll",
    "[ OK ] Driver de vídeo: ${c.gpu.deviceName} (vga.sys)",
    "[ OK ] Camada Direct3D / Vulkan inicializada (dxgkrnl.sys)",
    "[ OK ] Carregando disk.sys · VHD ${c.storageGb} GB",
    "[ OK ] Carregando ndis.sys · tcpip.sys",
    "[ OK ] Carregando win32k.sys · user32.dll · kernel32.dll",
    "Iniciando ${c.os.label}…",
    "Preparando área de trabalho ${c.resolution.w}x${c.resolution.h} @ ${c.fps} Hz"
)

@Composable
fun BootScreenUI(config: HardwareConfig, onFinished: () -> Unit) {
    val logs = remember(config) { bootLog(config) }
    var shown by remember { mutableIntStateOf(1) }
    val finished by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        while (shown < logs.size) { delay(420); shown++ }
        delay(700)
        finished()
    }

    Row(
        Modifier.fillMaxSize().background(Color.Black).systemBarsPadding().padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            WindowsLogo(config.os.isWin11, Modifier.size(100.dp))
            Spacer(Modifier.height(24.dp))
            SpinnerRing(Modifier.size(44.dp))
            Spacer(Modifier.height(12.dp))
            Text(config.os.label, color = Color.White.copy(.8f), fontSize = 13.sp)
        }
        Column(Modifier.weight(1.3f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            logs.take(shown).takeLast(9).forEach {
                Text(it, color = Color(0xFF39FF14), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun SpinnerRing(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "spin")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "a")
    Canvas(modifier) {
        val stroke = Stroke(width = 6f, cap = StrokeCap.Round)
        drawArc(Color.White.copy(.15f), 0f, 360f, false, style = stroke)
        drawArc(Color.White, angle, 100f, false, style = stroke)
    }
}

/** Logo estilizado (painéis azuis). Para o logo oficial, adicione o recurso em res/drawable. */
@Composable
fun WindowsLogo(win11: Boolean, modifier: Modifier = Modifier) {
    val blue = Color(0xFF00A4EF)
    Canvas(modifier) {
        if (win11) {
            val g = size.width * 0.04f
            val s = (size.width - g) / 2f
            listOf(Offset(0f, 0f), Offset(s + g, 0f), Offset(0f, s + g), Offset(s + g, s + g))
                .forEach { drawRect(blue, it, Size(s, s)) }
        } else {
            poly(listOf(0f to 13f, 45f to 7f, 45f to 47f, 0f to 47f), blue)
            poly(listOf(50f to 6f, 100f to 0f, 100f to 47f, 50f to 47f), blue)
            poly(listOf(0f to 53f, 45f to 53f, 45f to 93f, 0f to 87f), blue)
            poly(listOf(50f to 53f, 100f to 53f, 100f to 100f, 50f to 94f), blue)
        }
    }
}

private fun DrawScope.poly(pts: List<Pair<Float, Float>>, color: Color) {
    val p = Path()
    pts.forEachIndexed { i, (x, y) ->
        val px = x / 100f * size.width; val py = y / 100f * size.height
        if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
    }
    p.close()
    drawPath(p, color)
}
