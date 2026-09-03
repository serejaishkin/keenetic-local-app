package com.keenetic.local.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object KeeneticColors {
    val Background = Color(0xFF0B0F19)
    val Surface = Color(0xFF131B2E)
    val SurfaceElevated = Color(0xFF1E293B)
    val Primary = Color(0xFF00A3FF)
    val PrimaryLight = Color(0xFF38BDF8)
    val Secondary = Color(0xFF0EA5E9)
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Divider = Color(0xFF1E293B)
    val CardBorder = Color(0xFF1E293B)
    val Border = Color(0xFF1E293B)
    val Card = Color(0xFF131B2E)
    val TerminalBg = Color(0xFF0A0E1A)
    val TerminalText = Color(0xFF10B981)

    val BackgroundDark = Color(0xFF0B0F19)
    val SurfaceDark = Color(0xFF131B2E)
    val PrimaryCyan = Color(0xFF00A3FF)
    val StatusOnline = Color(0xFF10B981)
    val StatusOffline = Color(0xFF64748B)
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
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
