package com.lonx.lyrico.ui.components.base

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.lonx.lyrico.ui.components.sdw.SdwBottomSheet

/**
 * [B] 统一到 SDW：操作弹层外壳直接走 SdwBottomSheet；tools 作为 header、footer 透传，
 * 原 MIUIX / Salt 分支坍缩为单路（enableNestedScroll / saltFooter 等引擎专属参数已不再需要）。
 */
@Composable
fun ActionBottomSheet(
    show: Boolean,
    title: String? = null,
    enableNestedScroll: Boolean = true,
    allowDismiss: Boolean = true,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
    tools: @Composable (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null,
    groupContent: Boolean = false,
    /** 正文滚动出口：内容自带 LazyColumn/verticalScroll 时置 false，避免嵌套滚动。 */
    scrollContent: Boolean = true,
    saltFooter: (@Composable () -> Unit)? = null,
) {
    SdwBottomSheet(
        show = show,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        allowDismiss = allowDismiss,
        title = title,
        header = { tools?.invoke() },
        footer = footer ?: saltFooter,
        groupContent = groupContent,
        scrollContent = scrollContent,
        content = content,
    )
}
