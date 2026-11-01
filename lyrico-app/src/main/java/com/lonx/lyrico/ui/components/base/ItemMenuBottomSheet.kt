package com.lonx.lyrico.ui.components.base

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.lonx.lyrico.ui.components.sdw.SdwMenuBottomSheet

/** [B] 统一到 SDW：Item 菜单弹层直接走 SdwMenuBottomSheet，原 MIUIX / Salt 分支坍缩为单路。 */
@Composable
fun ItemMenuBottomSheet(
    show: Boolean,
    label: String,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit = {},
    enableNestedScroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    SdwMenuBottomSheet(
        show = show,
        label = label,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        content = content,
    )
}
