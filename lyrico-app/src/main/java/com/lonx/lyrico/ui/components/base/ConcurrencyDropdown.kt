package com.lonx.lyrico.ui.components.base

import androidx.compose.runtime.Composable
import com.lonx.lyrico.ui.components.icons.LucideIcons
import com.lonx.lyrico.ui.components.sdw.SdwDropdownRow

/** [B] 统一到 SDW：并发下拉直接走 SdwDropdownRow，原 MIUIX / Salt 分支坍缩为单路。 */
@Composable
internal fun ConcurrencyDropdown(
    title: String,
    value: Int,
    onChange: (Int) -> Unit,
    summary: String? = null,
) {
    val labels = (1..5).map { it.toString() }
    SdwDropdownRow(
        title = title,
        sub = summary,
        labels = labels,
        selectedIndex = value - 1,
        icon = LucideIcons.Parallel,
        onSelectedIndexChange = { onChange(it + 1) },
    )
}
