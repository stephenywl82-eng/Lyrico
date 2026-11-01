package com.lonx.lyrico.ui.components.editor

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import com.lonx.lyrico.ui.components.sdw.SdwBottomSheet

/** Single native window, with the existing Miuix content retained by the caller. */
@Composable
internal fun EditorSheet(
    show: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    enableNestedScroll: Boolean = false,
    tools: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    SdwBottomSheet(
            show = show,
            title = title,
            enableNestedScroll = enableNestedScroll,
            onDismissRequest = onDismissRequest,
            tools = tools,
            content = content,
        )
}
