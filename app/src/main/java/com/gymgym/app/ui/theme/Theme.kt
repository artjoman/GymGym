package com.gymgym.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand accent + a premium near-black surface set. The app commits to a single
// dark theme so the gym photo background and neon accents stay consistent.
val BrandGreen = Color(0xFF7FE0A0)
val BrandGreenDeep = Color(0xFF1B6B3A)
val NeonCyan = Color(0xFF35E0FF)
val NeonMagenta = Color(0xFFFF3DAE)

private val DarkColors = darkColorScheme(
    primary = BrandGreen,
    onPrimary = Color(0xFF06210F),
    secondary = NeonCyan,
    onSecondary = Color(0xFF001A20),
    tertiary = NeonMagenta,
    background = Color(0xFF07090C),
    onBackground = Color(0xFFEAF2EC),
    surface = Color(0xFF10151B),
    onSurface = Color(0xFFEAF2EC),
    surfaceVariant = Color(0xFF1A2129),
    onSurfaceVariant = Color(0xFFB6C2BC),
    outline = Color(0xFF2A333C),
    outlineVariant = Color(0xFF1E262E),
)

@Composable
fun GymGymTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = GymTypography,
        content = content,
    )
}
