package com.emnio.windowspc.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emnio.windowspc.R
import com.emnio.windowspc.model.HardwareConfig
import com.emnio.windowspc.model.VGpu
import com.emnio.windowspc.model.WinEdition

/** Papel de parede do Windows escolhido (imagens originais geradas para o app, estilo Win10/Win11). */
fun wallpaperFor(os: WinEdition): Int =
    if (os.isWin11) R.drawable.wallpaper_win11 else R.drawable.wallpaper_win10

fun thumbFor(os: WinEdition): Int =
    if (os.isWin11) R.drawable.thumb_win11 else R.drawable.thumb_win10

@Composable
fun OsThumb(os: WinEdition) {
    Box(Modifier.size(72.dp, 42.dp).clip(RoundedCornerShape(8.dp))) {
        Image(painterResource(thumbFor(os)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        WindowsLogo(os.isWin11, Modifier.align(Alignment.Center).size(16.dp))
    }
}

@Composable
fun GpuBadge(gpu: VGpu) {
    val (text, color) = when (gpu) {
        VGpu.INTEGRATED -> "HD" to Color(0xFF0071C5)
        VGpu.ADRENO -> "ADRENO" to Color(0xFFD9432B)
        VGpu.RTX3060 -> "RTX" to Color(0xFF76B900)
        VGpu.RX6700 -> "RX" to Color(0xFFED1C24)
    }
    Box(
        Modifier.size(52.dp, 32.dp).clip(RoundedCornerShape(8.dp))
            .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = .55f)))),
        contentAlignment = Alignment.Center
    ) { Text(text, color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MachinePreview(c: HardwareConfig) {
    val shape = RoundedCornerShape(18.dp)
    Box(Modifier.fillMaxWidth().height(170.dp).clip(shape).border(1.dp, EmnioColors.Purple, shape)) {
        Crossfade(c.os.isWin11, label = "wp") { win11 ->
            Image(
                painterResource(if (win11) R.drawable.wallpaper_win11 else R.drawable.wallpaper_win10),
                null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop
            )
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33000000), Color(0xDD0F111A)))))
        Row(Modifier.align(Alignment.TopStart).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            WindowsLogo(c.os.isWin11, Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(c.os.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        FlowRow(
            Modifier.align(Alignment.BottomStart).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "${c.ramGb} GB RAM", "${c.vcpu} vCPU", c.gpu.deviceName,
                "${c.storageGb} GB SSD", "${c.fps} FPS", "${c.resolution.w}x${c.resolution.h}"
            ).forEach { SpecChip(it) }
        }
    }
}

@Composable
private fun SpecChip(text: String) {
    Text(
        text, color = Color.White, fontSize = 11.sp, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0x99000000))
            .border(1.dp, EmnioColors.Neon.copy(.6f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 3.dp)
    )
}
