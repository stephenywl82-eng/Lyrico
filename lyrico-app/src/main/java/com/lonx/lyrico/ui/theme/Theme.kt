package com.lonx.lyrico.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.lonx.lyrico.data.model.ThemeMode
import com.lonx.lyrico.ui.theme.sdw.LyricoSdwTheme

/**
 * [B] 统一到 SDW 一套：最内层 MaterialTheme 由 LyricoSdwTheme 提供，使 SDW 基础件获得 SDW 调色板 / 排版；
 * 外层的 MIUIX / Salt 主题暂时保留，仅为尚未迁移的 80+ 处组件提供其专属主题令牌（Phase 3 删除）。
 */
@Composable
fun LyricoTheme(
    colorMode: ThemeMode = ThemeMode.AUTO,
    keyColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val indication = ripple()
    LyricoMiuixTheme(colorMode, keyColor) {
        LyricoSdwTheme(colorMode, keyColor) {
            CompositionLocalProvider(
                LocalIndication provides indication,
                content = content,
            )
        }
    }
}
