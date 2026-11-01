package com.lonx.lyrico.ui.components.base

import androidx.compose.runtime.Composable
import com.lonx.lyrico.ui.components.sdw.SdwSheetActions

/** [B] 统一到 SDW：确认/取消操作区直接走 SdwSheetActions，原 MIUIX / Salt 分支坍缩为单路。 */
@Composable
internal fun SheetConfirmActions(
    cancelText: String,
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
) {
    SdwSheetActions(cancelText, confirmText, onCancel, onConfirm, confirmEnabled)
}
