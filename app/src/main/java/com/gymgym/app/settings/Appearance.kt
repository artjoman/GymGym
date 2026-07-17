package com.gymgym.app.settings

/**
 * Selectable accent color scheme. Only the accent (and its deep shade for
 * gradients) changes; the app keeps its near-black dark surfaces so overlays and
 * the camera stay consistent. [accent]/[accentDeep] are ARGB longs consumed by
 * the theme.
 */
enum class AccentTheme(val label: String, val accent: Long, val accentDeep: Long) {
    EMERALD("Emerald", 0xFF7FE0A0L, 0xFF1B6B3AL),
    AZURE("Azure", 0xFF4FB8FFL, 0xFF1C5B94L),
    VIOLET("Violet", 0xFFB98CFFL, 0xFF5B3AA0L),
    MAGENTA("Magenta", 0xFFFF6FC1L, 0xFF8E2E66L),
    AMBER("Amber", 0xFFFFC24BL, 0xFF9A6A12L),
    CRIMSON("Crimson", 0xFFFF6B6BL, 0xFF8E2A2AL),
    LIME("Lime", 0xFFB6F24BL, 0xFF5E8A12L),
    AQUA("Aqua", 0xFF3FE0D0L, 0xFF166B62L),
}

/**
 * Home-screen background choice. Presets map to bundled drawables in the UI
 * layer; [CUSTOM] uses a user-picked image copied into app storage; [NONE] is a
 * plain dark ground.
 */
enum class BackgroundStyle(val label: String) {
    NONE("None"),
    GYM_EMERALD("Emerald"),
    GYM_AZURE("Azure"),
    GYM_VIOLET("Violet"),
    GYM_AMBER("Amber"),
    CUSTOM("Your photo"),
}
