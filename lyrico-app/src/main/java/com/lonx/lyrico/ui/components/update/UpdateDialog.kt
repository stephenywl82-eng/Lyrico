
package com.lonx.lyrico.ui.components.update

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.base.YesNoDialog
import dev.jeziellago.compose.markdowntext.MarkdownText
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material3.MaterialTheme

@Composable
fun UpdateDialog(
    show: Boolean,
    versionName: String,
    releaseNote: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onDismissFinished: () -> Unit = {},
) {
    val scrollState = rememberScrollState()

    YesNoDialog(
        show = show,
        onConfirm = onConfirm,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
        title = stringResource(
            id = R.string.dialog_title_update_available,
            versionName
        ),
        confirmText = stringResource(R.string.dialog_action_go_update),
        content = {
            Card(
                modifier = Modifier.padding(bottom = 12.dp),
                colors = CardDefaults.defaultColors(
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                MarkdownText(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .heightIn(max = 300.dp)
                        .verticalScroll(scrollState),
                    markdown = releaseNote
                )
            }
        }
    )
}
