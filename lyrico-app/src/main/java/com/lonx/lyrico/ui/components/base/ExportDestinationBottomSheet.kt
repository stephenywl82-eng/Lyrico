package com.lonx.lyrico.ui.components.base

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.lonx.lyrico.R
import com.lonx.lyrico.data.model.ExportDestination
import com.lonx.lyrico.ui.components.sdw.SdwRadioRow

/** [B] 统一到 SDW：导出目标选择弹层改用 SdwRadioRow，原 Salt/Miuix 分支坍缩为单路。 */
@Composable
fun ExportDestinationBottomSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (ExportDestination) -> Unit,
) {
    var destination by remember(show) {
        mutableStateOf(ExportDestination.SELECTED_DIRECTORY)
    }

    YesNoBottomSheet(
        show = show,
        title = stringResource(R.string.export_destination_title),
        onDismissRequest = onDismissRequest,
        onConfirm = { onConfirm(destination) },
        groupContent = false,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SdwRadioRow(
                    title = stringResource(R.string.export_destination_selected_folder),
                    summary = stringResource(R.string.export_destination_selected_folder_summary),
                    selected = destination == ExportDestination.SELECTED_DIRECTORY,
                    onClick = { destination = ExportDestination.SELECTED_DIRECTORY },
                )
                SdwRadioRow(
                    title = stringResource(R.string.export_destination_audio_folder),
                    summary = stringResource(R.string.export_destination_audio_folder_summary),
                    selected = destination == ExportDestination.AUDIO_DIRECTORY,
                    onClick = { destination = ExportDestination.AUDIO_DIRECTORY },
                )
            }
        },
    )
}
