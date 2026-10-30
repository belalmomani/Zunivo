package com.zunivo.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette taken from the approved Zunivo mockup (background shades are approximate).
val Indigo = Color(0xFF6366F1)
val Violet = Color(0xFF8B5CF6)
val Cyan = Color(0xFF06B6D4)
val Emerald = Color(0xFF10B981)
val Amber = Color(0xFFF59E0B)
val Rose = Color(0xFFF43F5E)
val NavyBackground = Color(0xFF0B1020)
val NavySurface = Color(0xFF141A30)

private val DarkColors = darkColorScheme(
    primary = Indigo,
    secondary = Violet,
    tertiary = Cyan,
    background = NavyBackground,
    surface = NavySurface,
    onBackground = Color(0xFFF1F3FF),
    onSurface = Color(0xFFF1F3FF),
    onSurfaceVariant = Color(0xFFA9B0CF),
    error = Rose,
)

@Composable
fun ZunivoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
