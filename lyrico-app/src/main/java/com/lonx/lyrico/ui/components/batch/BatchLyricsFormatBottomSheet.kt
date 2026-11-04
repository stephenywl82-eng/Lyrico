package com.lonx.lyrico.ui.components.batch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.base.SheetStatisticsRow
import com.lonx.lyrico.ui.components.base.ActionBottomSheet
import com.lonx.lyrico.ui.components.base.AppLinearProgressIndicator
import com.lonx.lyrico.viewmodel.BatchLyricsFormatUiState
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material3.MaterialTheme

@Composable
fun BatchLyricsFormatBottomSheet(
    batchLyricsFormatUiState: BatchLyricsFormatUiState,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
    onAbort: () -> Unit
) {
    val salt = false
    ActionBottomSheet(
        saltFooter = {
            com.lonx.lyrico.ui.components.base.SheetProgressAction(
                running = batchLyricsFormatUiState.isRunning, onAbort = onAbort, onClose = onDismissRequest,
            )
        },
        show = batchLyricsFormatUiState.showProgressDialog,
        onDismissRequest = {
            onDismissRequest()
        },
        onDismissFinished = {
            onDismissFinished()
        },
        allowDismiss = !batchLyricsFormatUiState.isRunning,
        title = stringResource(batchLyricsFormatUiState.operation.titleRes),
        groupContent = true,
        scrollContent = false,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (batchLyricsFormatUiState.operation == com.lonx.lyrico.data.model.lyrics.LyricsOperation.CONVERT) {
                        val targetFormatText = stringResource(
                            R.string.current_target_format,
                            batchLyricsFormatUiState.targetFormat?.let { stringResource(it.labelRes) }.orEmpty()
                        )
                        Text(
                            text = targetFormatText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    batchLyricsFormatUiState.progress?.let { (current, total) ->
                        val progress =
                            if (total > 0) current.toFloat() / total.toFloat() else 0f

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (batchLyricsFormatUiState.isRunning) {
                                    stringResource(R.string.batch_edit_processing)
                                } else {
                                    stringResource(
                                        R.string.batch_replay_gain_total_time,
                                        batchLyricsFormatUiState.totalTimeMillis / 1000.0
                                    )
                                },
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "$current / $total",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.End
                            )
                        }

                        AppLinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                SheetStatisticsRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    
                        Text(
                            text = stringResource(
                                R.string.batch_replay_gain_success,
                                batchLyricsFormatUiState.successCount
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(
                                R.string.batch_replay_gain_skipped,
                                batchLyricsFormatUiState.skippedCount
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(
                                R.string.batch_replay_gain_failure,
                                batchLyricsFormatUiState.failureCount
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    
                }
            }
        },
        footer = {
            val actionLabel = if (batchLyricsFormatUiState.isRunning) stringResource(R.string.action_abort)
            else stringResource(R.string.action_close)
            val onAction = {
                if (batchLyricsFormatUiState.isRunning) {
                    onAbort()
                } else {
                    onDismissRequest()
                }
            }
            TextButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
        }
    )
}
