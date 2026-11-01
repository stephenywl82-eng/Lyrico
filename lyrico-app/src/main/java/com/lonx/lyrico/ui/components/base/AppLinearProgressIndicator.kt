package com.lonx.lyrico.ui.components.base

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * [B] 统一到 SDW：线性进度统一用 Material3 指示器（SDW 主题是 Material3）。
 * 原 Salt 回退到 Material3 的写法不再需要，单路即可。
 */
@Composable
fun AppLinearProgressIndicator(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier,
    )
}

/** 不确定进度。 */
@Composable
fun AppLinearProgressIndicator(modifier: Modifier = Modifier) {
    LinearProgressIndicator(modifier = modifier)
}
