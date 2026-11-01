
package com.lonx.lyrico.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.base.YesNoDialog

@Composable
internal fun SettingsCacheDialog(
    show: Boolean,
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    YesNoDialog(
        show = show,
        title = stringResource(R.string.clear_cache),
        content = {
            Text(
                text,
                modifier =
                    Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
            )
        },
        onDismissRequest = onDismiss,
        onConfirm = onConfirm,
    )
}
