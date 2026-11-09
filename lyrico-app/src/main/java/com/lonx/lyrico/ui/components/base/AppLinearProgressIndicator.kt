package com.lonx.lyrico.ui.components.base

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * [B] 统一到 SDW：线性进度统一用 Material3 指示器（SDW 主题是 Material3）。
 *
 * 确定性进度仍走官方 [LinearProgressIndicator]（进度数值语义明确）；
 * 不确定进度（加载型）改用 M3 Expressive 的 [LinearWavyProgressIndicator]，
 * 波形动效由主题 MotionScheme 驱动，取代标准直线扫描。
 */
@Composable
fun AppLinearProgressIndicator(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier,
    )
}

/** 不确定进度：M3 Expressive 波形指示器。 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppLinearProgressIndicator(modifier: Modifier = Modifier) {
    LinearWavyProgressIndicator(modifier = modifier)
}
