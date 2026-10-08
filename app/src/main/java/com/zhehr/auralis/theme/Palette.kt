package com.zhehr.auralis.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Dark"),
    AMOLED("AMOLED"),
    LIGHT("Light"),
}

/**
 * The app's own colour tokens. Material's ColorScheme is derived from this, not the other way round,
 * so Stage 6 (Theme Studio, artwork themes, JSON import/export) only has to produce a palette.
 */
@Immutable
data class AuralisPalette(
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val accent: Color,
    val onAccent: Color,
    val text: Color,
    val mutedText: Color,
    val isDark: Boolean,
)

object AuralisPalettes {
    // One dominant accent: warm amber. Deliberately not green, not purple.
    val Dark = AuralisPalette(
        background = Color(0xFF0B0D12),
        surface = Color(0xFF141824),
        surfaceHigh = Color(0xFF1D2232),
        accent = Color(0xFFFFB454),
        onAccent = Color(0xFF1A1203),
        text = Color(0xFFF2F1EE),
        mutedText = Color(0xFF9AA0B2),
        isDark = true,
    )

    val Amoled = Dark.copy(
        background = Color(0xFF000000),
        surface = Color(0xFF0C0E14),
        surfaceHigh = Color(0xFF161A26),
    )

    val Light = AuralisPalette(
        background = Color(0xFFF7F5F0),
        surface = Color(0xFFFFFFFF),
        surfaceHigh = Color(0xFFECE8DF),
        accent = Color(0xFFB35A00),
        onAccent = Color(0xFFFFFFFF),
        text = Color(0xFF15161A),
        mutedText = Color(0xFF5C6070),
        isDark = false,
    )
}
