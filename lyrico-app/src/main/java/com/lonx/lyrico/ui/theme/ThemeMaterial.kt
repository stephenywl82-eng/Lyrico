package com.lonx.lyrico.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.lonx.lyrico.data.model.ThemeMode

/**
 * Target design system: standard Material Design 3.
 *
 * Color scheme priority:
 * 1. Dynamic (Material You / monet) when enabled and supported (API >= 31).
 * 2. Seed color from [keyColor] when provided.
 * 3. Default Material3 baseline scheme otherwise.
 */
@Composable
internal fun LyricoMaterialTheme(
    colorMode: ThemeMode = ThemeMode.AUTO,
    keyColor: Color? = null,
    monetEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme =
        when (colorMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.AUTO -> isSystemInDarkTheme()
        }
    val colorScheme =
        when {
            monetEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            keyColor != null ->
                if (darkTheme) darkColorScheme(primary = keyColor) else lightColorScheme(primary = keyColor)
            else -> if (darkTheme) darkColorScheme() else lightColorScheme()
        }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
