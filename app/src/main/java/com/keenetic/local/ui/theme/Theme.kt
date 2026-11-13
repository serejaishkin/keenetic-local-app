package com.keenetic.local.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object KeeneticColors {
    // Distinct "Emerald Graphite" style (not tied to vendor branding)
    val Background = Color(0xFF101312)
    val Surface = Color(0xFF171B19)
    val SurfaceElevated = Color(0xFF202623)
    val Primary = Color(0xFF34D399)
    val PrimaryLight = Color(0xFF6EE7B7)
    val Secondary = Color(0xFF10B981)
    val TextPrimary = Color(0xFFEEF5F1)
    val TextSecondary = Color(0xFF9FB3A9)
    val Success = Color(0xFF22C55E)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Divider = Color(0xFF26302B)
    val CardBorder = Color(0xFF26302B)
    val Border = Color(0xFF26302B)
    val Card = Color(0xFF171B19)
    val TerminalBg = Color(0xFF0C100E)
    val TerminalText = Color(0xFF6EE7B7)
    val Accent = Primary

    val BackgroundDark = Color(0xFF101312)
    val SurfaceDark = Color(0xFF171B19)
    val PrimaryCyan = Color(0xFF34D399)
    val StatusOnline = Color(0xFF22C55E)
    val StatusOffline = Color(0xFF6B7C74)
    val AccentAmber = Color(0xFFF59E0B)
}

private val DarkColorScheme = darkColorScheme(
    primary = KeeneticColors.Primary,
    secondary = KeeneticColors.Secondary,
    background = KeeneticColors.Background,
    surface = KeeneticColors.Surface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = KeeneticColors.TextPrimary,
    onSurface = KeeneticColors.TextPrimary,
    error = KeeneticColors.Error
)

@Composable
fun KeeneticAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColorScheme, content = content)
}
