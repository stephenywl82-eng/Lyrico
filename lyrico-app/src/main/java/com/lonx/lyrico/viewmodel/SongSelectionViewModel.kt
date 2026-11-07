package com.lonx.lyrico.viewmodel

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lonx.lyrico.R
import com.lonx.lyrico.data.SharedSelectionManager
import com.lonx.lyrico.data.model.entity.SongEntity
import com.lonx.lyrico.data.model.entity.getUri
import com.lonx.lyrico.data.repository.PlaybackRepository
import com.lonx.lyrico.domain.song.usecase.DeleteSongsUseCase
import com.lonx.lyrico.domain.song.usecase.RenameSongResult
import com.lonx.lyrico.domain.song.usecase.RenameSongUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SongSelectionViewModel(
    private val deleteSongsUseCase: DeleteSongsUseCase,
    private val renameSongUseCase: RenameSongUseCase,
    private val playbackRepository: PlaybackRepository,
    private val selectionManager: SharedSelectionManager
) : ViewModel() {

    // Single-song rename/delete permission grants (collected by SongActionSheets).
    private val _permissionRequest = MutableSharedFlow<IntentSender?>(extraBufferCapacity = 1)
    val permissionRequest: SharedFlow<IntentSender?> = _permissionRequest.asSharedFlow()

    // Batch-delete permission grants (collected by SongBatchSelectionActions).
    private val _batchPermissionRequest = MutableSharedFlow<IntentSender?>(extraBufferCapacity = 1)
    val batchPermissionRequest: SharedFlow<IntentSender?> = _batchPermissionRequest.asSharedFlow()

    // Pending operations to retry once the user grants MediaStore write access,
    // so the rename/delete completes in one pass instead of requiring a re-trigger.
    private var pendingRename: Pair<SongEntity, String>? = null
    private var pendingDelete: SongEntity? = null
    private var pendingBatchDelete: List<SongEntity>? = null

    val selectedSongUris = selectionManager.selectedUris
    val isSelectionMode = selectionManager.isSelectionMode
    val swipeAnchorUri = selectionManager.swipeAnchorUri

    fun toggleSelection(uri: String) {
        selectionManager.toggle(uri)
    }

    fun swipeSelect(song: SongEntity, visibleSongs: List<SongEntity>) {
        selectionManager.selectSwipeRange(
            uri = song.uri,
            visibleUris = visibleSongs.map { it.uri }
        )
    }

    fun exitSelectionMode() {
        selectionManager.exitSelectionMode()
    }

    fun deselectAll() {
        selectionManager.deselectAll()
    }

    fun selectAll(songs: List<SongEntity>) {
        selectionManager.selectAll(songs.map { it.uri }.toSet())
    }

    fun setSelectionUris(): Boolean {
        val selectedUris = selectedSongUris.value
        if (selectedUris.isEmpty()) return false

        selectionManager.setUris(selectedUris)
        return true
    }

    fun play(context: Context, song: SongEntity) {
        playbackRepository.play(context, song.getUri)
    }

    fun delete(song: SongEntity) {
        viewModelScope.launch {
            val result = deleteSongsUseCase(listOf(song))
            if (result.permissionRequired != null) {
                pendingDelete = song
                _permissionRequest.tryEmit(result.permissionRequired)
            }
        }
    }

    fun batchDelete(songs: List<SongEntity>) {
        val selectedUris = selectedSongUris.value
        val toDelete = songs.filter { it.uri in selectedUris }

        viewModelScope.launch {
            deleteSongsNow(toDelete)
            exitSelectionMode()
        }
    }

    private suspend fun deleteSongsNow(songs: List<SongEntity>) {
        if (songs.isEmpty()) return
        val result = deleteSongsUseCase(songs)
        if (result.permissionRequired != null) {
            pendingBatchDelete = songs
            _batchPermissionRequest.tryEmit(result.permissionRequired)
        }
    }

    fun batchShare(context: Context, songs: List<SongEntity>) {
        val selectedUris = selectedSongUris.value
        val toShare = songs.filter { it.uri in selectedUris }
        if (toShare.isEmpty()) return

        val uris = toShare.map { it.uri.toUri() }.toCollection(ArrayList())

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "audio/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(
                intent,
                context.getString(R.string.share_chooser_title)
            )
        )
    }

    fun renameSong(song: SongEntity, newFileName: String) {
        viewModelScope.launch {
            when (val result = renameSongUseCase(song, newFileName)) {
                is RenameSongResult.Success -> pendingRename = null
                is RenameSongResult.NameConflict -> {
                    Log.w(TAG, "Rename skipped, name conflict: ${result.targetName}")
                }
                is RenameSongResult.PermissionRequired -> {
                    pendingRename = song to newFileName
                    _permissionRequest.tryEmit(result.intentSender)
                }
                is RenameSongResult.Failed -> {
                    Log.e(TAG, "Rename failed: ${song.title}", result.throwable)
                    pendingRename = null
                }
            }
        }
    }

    fun retryPendingRename() {
        val pending = pendingRename ?: return
        renameSong(pending.first, pending.second)
    }

    fun retryPendingDelete() {
        val pending = pendingDelete ?: return
        delete(pending)
    }

    fun retryPendingBatchDelete() {
        val pending = pendingBatchDelete ?: return
        pendingBatchDelete = null
        viewModelScope.launch { deleteSongsNow(pending) }
    }

    private companion object {
        const val TAG = "SongSelectionViewModel"
    }
}
