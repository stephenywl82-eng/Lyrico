package com.lonx.lyrico.ui.theme

import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import com.lonx.lyrico.R

val isDarkTheme: Boolean
    @Composable
    get() = isSystemInDarkTheme()
object LyricoColors {
    val emptyIllustrationWarm: Color
        @Composable
        get() = if (isDarkTheme) Color(0xFFFFBD7A) else Color(0xFFEBA15B)

    val emptyIllustrationFresh: Color
        @Composable
        get() = if (isDarkTheme) Color(0xFF80D4C3) else Color(0xFF55B7A5)

    /**
     * Placeholder background color for album covers.
     * In light mode: light gray, in dark mode: darker gray.
     */
    val coverPlaceholder: Color
        @Composable
        get() = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    /**
     * Modified state background color (for edited items).
     * 走 M3 语义色 tertiaryContainer，自动适配深/浅色与动态主题色。
     */
    val modifiedBackground: Color
        @Composable
        get() = MaterialTheme.colorScheme.tertiaryContainer

    /**
     * Modified state border color.
     */
    val modifiedBorder: Color
        @Composable
        get() = MaterialTheme.colorScheme.outline

    /**
     * Modified state badge background.
     */
    val modifiedBadgeBackground: Color
        @Composable
        get() = MaterialTheme.colorScheme.tertiaryContainer

    /**
     * Modified state text color.
     */
    val modifiedText: Color
        @Composable
        get() = MaterialTheme.colorScheme.onTertiaryContainer

    /**
     * Icon color for album cover placeholder.
     * Adapts to light/dark mode using subText color.
     */
    val coverPlaceholderIcon: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
}

data class KeyColor(
    @field:StringRes val nameResId: Int,
    val color: Color?
)

val KeyColors = listOf(
    KeyColor(R.string.color_default, null),
    KeyColor(R.string.color_blue, Color(0xFF3482FF)),
    KeyColor(R.string.color_green, Color(0xFF36D167)),
    KeyColor(R.string.color_yellow, Color(0xFFFFB21D)),
    KeyColor(R.string.color_orange, Color(0xFFFF5722)),
    KeyColor(R.string.color_purple, Color(0xFF7C4DFF)),
    KeyColor(R.string.color_pink, Color(0xFFE91E63)),
    KeyColor(R.string.color_teal, Color(0xFF00BCD4))
)
