package com.lonx.lyrico.ui.components.base

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R

/** [B] 统一到 SDW：进度弹层固定操作（运行=中止、空闲=关闭）改用 Material3 按钮。 */
@Composable
internal fun SheetProgressAction(
    running: Boolean,
    onAbort: () -> Unit,
    onClose: () -> Unit,
    label: String? = null,
) {
    Button(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
        onClick = if (running) onAbort else onClose,
        colors = if (running) ButtonDefaults.outlinedButtonColors() else ButtonDefaults.buttonColors(),
    ) {
        Text(label ?: stringResource(if (running) R.string.action_abort else R.string.action_close))
    }
}
