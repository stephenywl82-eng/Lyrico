package com.lonx.lyrico.ui.components.crop

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.base.SheetConfirmActions
import com.lonx.lyrico.ui.components.sdw.SdwBottomSheet

import top.yukonga.miuix.kmp.basic.CircularProgressIndicator

/** The crop workflow is shared; each engine keeps its own sheet chrome and confirm affordance. */
@Composable
fun ImageCropperSheet(
    show: Boolean,
    bitmap: Bitmap?,
    title: String,
    onCrop: (Bitmap) -> Unit,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit
) {
    val state = bitmap?.let { rememberImageCropperState(it) }

    SdwBottomSheet(
        show = show, title = title, enableNestedScroll = false,
        onDismissRequest = onDismissRequest, onDismissFinished = onDismissFinished,
        footer = {
            SheetConfirmActions(
                cancelText = stringResource(R.string.cancel),
                confirmText = stringResource(R.string.confirm),
                onCancel = onDismissRequest,
                confirmEnabled = state != null,
                onConfirm = {
                    state?.crop()?.let(onCrop)
                    onDismissRequest()
                },
            )
        }
    ) {
        Column(Modifier.fillMaxWidth()) {
            if (state != null) ImageCropper(state, Modifier.fillMaxWidth())
            else Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}
