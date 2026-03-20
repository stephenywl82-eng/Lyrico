package com.lonx.lyrico.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme

object LyricoColors {
    val coverPlaceholder: Color
        @Composable
        get() = MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)

    val secondaryText: Color
        @Composable
        get() = MiuixTheme.colorScheme.onSurfaceVariantSummary

    val modifiedBackground: Color
        @Composable
        get() = MiuixTheme.colorScheme.primaryContainer

    val modifiedBorder: Color
        @Composable
        get() = MiuixTheme.colorScheme.primary

    val modifiedBadgeBackground: Color
        @Composable
        get() = MiuixTheme.colorScheme.primaryContainer

    val modifiedText: Color
        @Composable
        get() = MiuixTheme.colorScheme.onPrimaryContainer

    val inputBorder: Color
        @Composable
        get() = MiuixTheme.colorScheme.outline

    val inputFocusedBorder: Color
        @Composable
        get() = MiuixTheme.colorScheme.primary

    val coverPlaceholderIcon: Color
        @Composable
        get() = MiuixTheme.colorScheme.onSurfaceContainerVariant
}
