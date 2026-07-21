package com.gymgym.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.gymgym.app.settings.AccentTheme

// Default brand accent + a premium near-black surface set. The app commits to a
// dark theme; only the accent changes with the selected color scheme. Amber is
// the brand default, matching the launcher icon.
val BrandAmber = Color(0xFFFFC24B)
val BrandAmberDeep = Color(0xFF9A6A12)

/** Accent trio exposed to components (buttons, wordmark) that draw gradients. */
data class Brand(val accent: Color, val accentDeep: Color, val onAccent: Color)

val LocalBrand = staticCompositionLocalOf {
    Brand(BrandAmber, BrandAmberDeep, Color(0xFF08130C))
}

/** Legible foreground for text/icons sitting on the accent fill. */
private fun onAccentFor(accent: Color): Color =
    if (accent.luminance() > 0.45f) Color(0xFF08130C) else Color(0xFFF3FBF5)

private fun schemeFor(accent: Color, deep: Color) = darkColorScheme(
    primary = accent,
    onPrimary = onAccentFor(accent),
    primaryContainer = deep,
    secondary = accent,
    onSecondary = onAccentFor(accent),
    tertiary = accent,
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
fun GymGymTheme(
    accent: AccentTheme = AccentTheme.AMBER,
    content: @Composable () -> Unit,
) {
    val accentColor = Color(accent.accent)
    val deep = Color(accent.accentDeep)
    val brand = Brand(accentColor, deep, onAccentFor(accentColor))
    CompositionLocalProvider(LocalBrand provides brand) {
        MaterialTheme(
            colorScheme = schemeFor(accentColor, deep),
            typography = GymTypography,
            content = content,
        )
    }
}
