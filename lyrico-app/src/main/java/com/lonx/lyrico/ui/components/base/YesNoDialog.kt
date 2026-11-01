package com.lonx.lyrico.ui.components.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.sdw.SdwConfirmDialog

/** [B] 统一到 SDW：确认对话框直接走 SDW 基础件，原 MIUIX / Salt 分支坍缩为单路。 */
@Composable
fun YesNoDialog(
    show: Boolean,
    title: String? = null,
    summary: String? = null,
    content: @Composable (() -> Unit)? = null,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit = {},
    onConfirm: () -> Unit,
    cancelText: String = stringResource(R.string.cancel),
    confirmText: String = stringResource(R.string.confirm),
    confirmEnabled: Boolean = true,
) {
    SdwConfirmDialog(
        show = show,
        title = title,
        summary = summary,
        content = content,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        onConfirm = onConfirm,
        cancelText = cancelText,
        confirmText = confirmText,
        confirmEnabled = confirmEnabled,
    )
}
