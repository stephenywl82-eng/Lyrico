
package com.lonx.lyrico.ui.components.batch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.base.YesNoBottomSheet
import com.lonx.lyrico.ui.components.base.ConcurrencyDropdown
import androidx.compose.material3.MaterialTheme
import com.lonx.lyrico.ui.components.sdw.Card
import com.lonx.lyrico.ui.components.sdw.CardDefaults

@Composable
fun BatchRGConfigBottomSheet(
    show: Boolean,
    initialConcurrency: Int,
    onDismissRequest: (Int) -> Unit,
    onConfirm: (Int) -> Unit
) {
    var concurrency by remember { mutableIntStateOf(initialConcurrency) }
    val salt = false
    val tempConcurrency = remember(initialConcurrency) {
        mutableIntStateOf(initialConcurrency)
    }

    YesNoBottomSheet(
        show = show,
        title = stringResource(R.string.action_batch_replay_gain),
        content = {
            val field: @Composable () -> Unit = {
                ConcurrencyDropdown(
                    title = stringResource(R.string.batch_replay_gain_concurrency),
                    summary = stringResource(R.string.batch_replay_gain_concurrency_tip),
                    value = tempConcurrency.intValue,
                    onChange = {
                        tempConcurrency.intValue = it
                        concurrency = it
                    },
                )
            }
            Card(
                colors = CardDefaults.defaultColors(color = MaterialTheme.colorScheme.surfaceContainer),
            ) { field() }
        },
        onDismissRequest = { onDismissRequest(concurrency) },
        onConfirm = {
            onConfirm(concurrency)
        }
    )
}
