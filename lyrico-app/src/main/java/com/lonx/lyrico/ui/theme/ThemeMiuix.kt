package com.lonx.lyrico.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.lonx.lyrico.data.model.ThemeMode
import com.lonx.lyrico.ui.theme.sdw.SDWAccentBlue
import com.lonx.lyrico.ui.theme.sdw.SDWBackground
import com.lonx.lyrico.ui.theme.sdw.SDWBluePrimaryLight
import com.lonx.lyrico.ui.theme.sdw.SDWCard
import com.lonx.lyrico.ui.theme.sdw.SDWDivider
import com.lonx.lyrico.ui.theme.sdw.SDWOnBackgroundLight
import com.lonx.lyrico.ui.theme.sdw.SDWOnSurfaceLight
import com.lonx.lyrico.ui.theme.sdw.SDWOnSurfaceVariantLight
import com.lonx.lyrico.ui.theme.sdw.SDWSecondaryContainerLight
import com.lonx.lyrico.ui.theme.sdw.SDWOnSecondaryContainerLight
import com.lonx.lyrico.ui.theme.sdw.SDWSurface
import com.lonx.lyrico.ui.theme.sdw.SDWTextPrimary
import com.lonx.lyrico.ui.theme.sdw.SDWTextSecondary
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

/**
 * Lyrico 的 Miuix 外层主题。
 *
 * 注意：这里**不再**走 Miuix 的 Monet 派生路径（ColorSchemeMode.Monet*）。
 * Miuix 的 Monet 暗色派生在部分设备/版本会把 onSurface / onSurfaceContainerVariant
 * 算成黑色，导致深色模式下歌曲名、歌手名、专辑文字全部不可见。
 *
 * 改为：用 Miuix 自带的 darkColorScheme()/lightColorScheme() 作为基底（其 on* 文字色是正确的），
 * 然后把「主题色（accent）」覆盖成壁纸 dynamic 取色（与 SDWMP3_CN 一致的莫奈取色默认行为），
 * 并把所有 on* 文字色**显式**写成正确的深浅值，彻底杜绝暗色黑字。
 * 这样外层 Miuix 与内层 MaterialExpressive 的配色完全一致，全站文字都遵循 M3 Expressive。
 */
@Composable
internal fun LyricoMiuixTheme(
    colorMode: ThemeMode,
    keyColor: Color?,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val controller = remember(colorMode, keyColor) {
        val effectiveLight = when (colorMode) {
            ThemeMode.LIGHT -> true
            ThemeMode.DARK -> false
            ThemeMode.AUTO -> !isSystemInDarkTheme()
        }
        val mode = when (colorMode) {
            ThemeMode.LIGHT -> ColorSchemeMode.Light
            ThemeMode.DARK -> ColorSchemeMode.Dark
            ThemeMode.AUTO -> ColorSchemeMode.System
        }
        ThemeController(
            colorSchemeMode = mode,
            lightColors = buildMiuixColors(context, isLight = true, keyColor = keyColor),
            darkColors = buildMiuixColors(context, isLight = false, keyColor = keyColor),
            keyColor = keyColor,
            colorSpec = ThemeColorSpec.Spec2021,
            paletteStyle = ThemePaletteStyle.TonalSpot,
            isDark = null,
        )
    }
    MiuixTheme(controller = controller, content = content)
}

/** 以白色/黑色保证与任意主色的对比度。 */
private fun contrastOn(color: Color): Color {
    val lum = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
    return if (lum > 0.5f) Color.Black else Color.White
}

/**
 * 构建一套 Miuix Colors：
 * - accent 主色系：优先用户选的 keyColor，否则壁纸 dynamic 主色，兜底 SDW 蓝；
 * - 表面：深色用 SDW 纯黑阶梯，浅色用 SDW 浅色；
 * - 所有 on* 文字色**显式**给定正确深浅，绝不使用可能偏黑的默认值。
 */
private fun buildMiuixColors(context: Context, isLight: Boolean, keyColor: Color?): Colors {
    val base = if (isLight) lightColorScheme() else darkColorScheme()
    val dyn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isLight) dynamicLightColorScheme(context) else dynamicDarkColorScheme(context)
    } else null

    // 主题色（accent）：用户选色 > 壁纸 dynamic 主色 > SDW 蓝兜底
    val seed = keyColor ?: dyn?.primary
    val accent = seed ?: (if (isLight) SDWBluePrimaryLight else SDWAccentBlue)

    // 主色系：有壁纸 dynamic 就直接用其完整方案（对比正确），否则用 accent 推导。
    val p = dyn?.primary ?: accent
    val onP = dyn?.onPrimary ?: contrastOn(accent)
    val pC = dyn?.primaryContainer ?: accent
    val onPC = dyn?.onPrimaryContainer ?: contrastOn(accent)
    val s = dyn?.secondary ?: accent
    val onS = dyn?.onSecondary ?: contrastOn(accent)
    val sC = dyn?.secondaryContainer ?: (if (isLight) SDWSecondaryContainerLight else accent)
    val onSC = dyn?.onSecondaryContainer ?: (if (isLight) SDWOnSecondaryContainerLight else contrastOn(accent))
    val tC = dyn?.tertiaryContainer ?: accent
    val onTC = dyn?.onTertiaryContainer ?: contrastOn(accent)
    val err = dyn?.error ?: base.error
    val onErr = dyn?.onError ?: base.onError
    val errC = dyn?.errorContainer ?: base.errorContainer
    val onErrC = dyn?.onErrorContainer ?: base.onErrorContainer

    return if (isLight) {
        base.copy(
            primary = p, onPrimary = onP,
            primaryContainer = pC, onPrimaryContainer = onPC,
            primaryVariant = p, onPrimaryVariant = onP,
            secondary = s, onSecondary = onS,
            secondaryContainer = sC, onSecondaryContainer = onSC,
            secondaryVariant = s, onSecondaryVariant = onS,
            tertiaryContainer = tC, onTertiaryContainer = onTC,
            error = err, onError = onErr, errorContainer = errC, onErrorContainer = onErrC,
            background = Color(0xFFF5F5F7), onBackground = SDWOnBackgroundLight,
            onBackgroundVariant = SDWOnSurfaceVariantLight,
            surface = Color(0xFFFFFFFF), onSurface = SDWOnSurfaceLight,
            surfaceContainer = Color(0xFFE8E8EA), onSurfaceContainer = SDWOnSurfaceLight,
            onSurfaceContainerVariant = SDWOnSurfaceVariantLight,
            surfaceVariant = Color(0xFFE8E8EA),
            onSurfaceSecondary = SDWOnSurfaceVariantLight,
            onSurfaceVariantSummary = SDWOnSurfaceVariantLight,
            onSurfaceVariantActions = SDWOnSurfaceVariantLight,
            outline = Color(0xFFD0D0D4), dividerLine = Color(0xFFD0D0D4),
        )
    } else {
        base.copy(
            primary = p, onPrimary = onP,
            primaryContainer = pC, onPrimaryContainer = onPC,
            primaryVariant = p, onPrimaryVariant = onP,
            secondary = s, onSecondary = onS,
            secondaryContainer = sC, onSecondaryContainer = onSC,
            secondaryVariant = s, onSecondaryVariant = onS,
            tertiaryContainer = tC, onTertiaryContainer = onTC,
            error = err, onError = onErr, errorContainer = errC, onErrorContainer = onErrC,
            background = SDWBackground, onBackground = SDWTextPrimary,
            onBackgroundVariant = SDWTextSecondary,
            surface = SDWCard, onSurface = SDWTextPrimary,
            surfaceContainer = SDWSurface, onSurfaceContainer = SDWTextPrimary,
            onSurfaceContainerVariant = SDWTextSecondary,
            surfaceVariant = SDWSurface,
            onSurfaceSecondary = SDWTextSecondary,
            onSurfaceVariantSummary = SDWTextSecondary,
            onSurfaceVariantActions = SDWTextSecondary,
            outline = SDWDivider, dividerLine = SDWDivider,
        )
    }
}
