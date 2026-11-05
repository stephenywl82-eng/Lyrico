package com.lonx.lyrico.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.lonx.lyrico.data.model.ThemeMode
import com.lonx.lyrico.ui.theme.sdw.LyricoSdwTheme

/**
 * Lyrico 统一主题（纯 M3 Expressive，阶段6 收尾后已移除 Miuix 外层）。
 * [keyColor] 保留签名兼容但主题层不再消费用户选色：默认走壁纸 dynamic / 系统默认主题色。
 * [hapticEnabled] 全站震动反馈开关，默认 false（关闭）。
 */
@Composable
fun LyricoTheme(
    colorMode: ThemeMode = ThemeMode.AUTO,
    keyColor: Color? = null,
    hapticEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val indication = ripple()
    ProvideHapticFeedback(enabled = hapticEnabled) {
        LyricoSdwTheme(colorMode, keyColor) {
            CompositionLocalProvider(
                LocalIndication provides indication,
                content = content,
            )
        }
    }
}
