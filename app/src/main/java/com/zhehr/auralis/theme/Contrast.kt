package com.zhehr.auralis.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/** WCAG 2.x contrast helpers. Every palette (preset, custom, artwork-generated) must pass through these. */
object Contrast {
    const val AA_NORMAL = 4.5f
    const val AA_LARGE = 3.0f

    fun ratio(a: Color, b: Color): Float {
        val la = a.luminance()
        val lb = b.luminance()
        return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
    }

    fun meetsAA(foreground: Color, background: Color, largeText: Boolean = false): Boolean =
        ratio(foreground, background) >= if (largeText) AA_LARGE else AA_NORMAL

    /** Returns [preferred] if it is readable on [background], otherwise black or white, whichever reads better. */
    fun readableOn(background: Color, preferred: Color): Color {
        if (meetsAA(preferred, background)) return preferred
        return if (ratio(Color.White, background) >= ratio(Color.Black, background)) Color.White else Color.Black
    }
}
