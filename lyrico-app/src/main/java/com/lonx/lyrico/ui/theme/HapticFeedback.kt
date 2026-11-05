package com.lonx.lyrico.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * 全站「震动反馈」开关，默认 false（关闭）。
 *
 * 用户在设置里开启后才为 true。所有 View 侧 `view.performHapticFeedback(...)` 调用点读它判断，
 * Compose 侧 `LocalHapticFeedback` 则由 [ProvideHapticFeedback] 统一覆盖，一处 gate 全部调用点。
 * 默认 false 也保证：即便某个角落忘了 provide，也不会误触发震动。
 */
val LocalHapticEnabled = compositionLocalOf { false }

/**
 * 用接口委托包装平台 HapticFeedback：关闭时把必然存在的
 * `performHapticFeedback(HapticFeedbackType)` 覆盖为空操作，其余方法（不同 Compose 版本可能新增）
 * 由 `by delegate` 自动转发，无需关心接口形态。
 */
private class GatedHapticFeedback(
    private val delegate: HapticFeedback,
) : HapticFeedback by delegate {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

/**
 * 在主题根部按开关提供震动反馈：
 * - enabled=true：沿用平台 HapticFeedback（真实震动）；
 * - enabled=false：替换为空实现，所有 `LocalHapticFeedback.current.performHapticFeedback(...)`
 *   调用自动静默，无需逐个改动调用点。
 */
@Composable
fun ProvideHapticFeedback(
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    val platform = LocalHapticFeedback.current
    val effective: HapticFeedback = remember(enabled, platform) {
        if (enabled) platform else GatedHapticFeedback(platform)
    }
    CompositionLocalProvider(
        LocalHapticEnabled provides enabled,
        LocalHapticFeedback provides effective,
        content = content,
    )
}
