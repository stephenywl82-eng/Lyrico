package com.lonx.lyrico.ui.permissions

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Max URIs passed to a single [MediaStore.createWriteRequest]. Above this the
 * underlying PendingIntent Bundle overflows the binder transaction limit and throws,
 * which previously was silently swallowed and skipped the grant entirely — leaving
 * every write in a bulk operation to fail with PermissionRequired. Large selections
 * are split into chunks, each granted in turn.
 */
private const val BATCH_WRITE_PERMISSION_CHUNK = 500

/**
 * Returns a lambda that requests MediaStore write permission for the given [uris] and
 * invokes [onGranted] once the grant completes.
 *
 * Required on Android 11+ to write tags/covers/lyrics into non-self-created media
 * files. On lower versions or for an empty list it just calls [onGranted] directly.
 *
 * For selections larger than [BATCH_WRITE_PERMISSION_CHUNK] the request is split into
 * chunks so the binder limit is never exceeded; the user may see several grant
 * dialogs, after which [onGranted] runs exactly once.
 */
@Composable
fun rememberBatchWritePermissionRequester(): (List<String>, () -> Unit) -> Unit {
    val context = LocalContext.current
    var pendingGrant by remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { _ ->
        val action = pendingGrant
        pendingGrant = null
        action?.invoke()
    }

    return { uris: List<String>, onGranted: () -> Unit ->
        requestWritePermission(
            uris = uris,
            context = context,
            launcher = launcher,
            setPending = { pendingGrant = it },
            onGranted = onGranted
        )
    }
}

private fun requestWritePermission(
    uris: List<String>,
    context: Context,
    launcher: ActivityResultLauncher<IntentSenderRequest>,
    setPending: ((() -> Unit)?) -> Unit,
    onGranted: () -> Unit
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || uris.isEmpty()) {
        onGranted()
        return
    }
    val contentUris = uris.mapNotNull { raw ->
        runCatching { Uri.parse(raw) }.getOrNull()?.takeIf { it.scheme == "content" }
    }
    if (contentUris.isEmpty()) {
        onGranted()
        return
    }
    // Best UX for normal selections: request every URI at once (one dialog). If the
    // count exceeds the binder limit, createWriteRequest throws — fall back to
    // processing the list in chunks so every song still gets granted.
    val fullSender = try {
        MediaStore.createWriteRequest(context.contentResolver, contentUris).intentSender
    } catch (e: Exception) {
        Log.w("BatchWritePermission", "createWriteRequest(all) failed, falling back to chunks", e)
        null
    }
    if (fullSender != null) {
        setPending(onGranted)
        launcher.launch(IntentSenderRequest.Builder(fullSender).build())
        return
    }
    requestWritePermissionChunked(contentUris, 0, context, launcher, setPending, onGranted)
}

private fun requestWritePermissionChunked(
    contentUris: List<Uri>,
    index: Int,
    context: Context,
    launcher: ActivityResultLauncher<IntentSenderRequest>,
    setPending: ((() -> Unit)?) -> Unit,
    onGranted: () -> Unit
) {
    if (index >= contentUris.size) {
        onGranted()
        return
    }
    val end = minOf(index + BATCH_WRITE_PERMISSION_CHUNK, contentUris.size)
    val chunk = contentUris.subList(index, end)
    val sender = try {
        MediaStore.createWriteRequest(context.contentResolver, chunk).intentSender
    } catch (e: Exception) {
        Log.w("BatchWritePermission", "createWriteRequest(chunk $index) failed, skipping", e)
        requestWritePermissionChunked(contentUris, end, context, launcher, setPending, onGranted)
        return
    }
    if (sender == null) {
        requestWritePermissionChunked(contentUris, end, context, launcher, setPending, onGranted)
    } else {
        // After this chunk's dialog is confirmed, advance to the next chunk.
        setPending { requestWritePermissionChunked(contentUris, end, context, launcher, setPending, onGranted) }
        launcher.launch(IntentSenderRequest.Builder(sender).build())
    }
}
