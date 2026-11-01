package com.lonx.lyrico.ui.components.base

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** [B] 统一到 SDW：统计行单路（两端对齐），原 MIUIX / Salt 分支坍缩。 */
@Composable
internal fun SheetStatisticsRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(modifier, horizontalArrangement = Arrangement.SpaceBetween) { content() }
}
