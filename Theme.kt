package com.emnio.windowspc.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object EmnioColors {
    val Neon = Color(0xFF00D2FF)
    val Purple = Color(0xFF8A2BE2)
    val Bg = Color(0xFF0F111A)
    val Card = Color(0xFF1A1D2E)
}

@Composable
fun EmnioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = EmnioColors.Neon,
            secondary = EmnioColors.Purple,
            background = EmnioColors.Bg,
            surface = EmnioColors.Card,
            onPrimary = Color.Black
        ),
        content = content
    )
}
