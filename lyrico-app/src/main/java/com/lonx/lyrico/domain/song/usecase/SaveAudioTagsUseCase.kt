package com.lonx.lyrico.domain.song.usecase

import android.content.IntentSender
import androidx.room.withTransaction
import com.lonx.audiotag.model.AudioTagData
import com.lonx.lyrico.data.LyricoDatabase
import com.lonx.lyrico.data.model.entity.SongEntity
import com.lonx.lyrico.data.repository.CustomTagKeyRepository
import com.lonx.lyrico.data.repository.LibraryIndexRepository
import com.lonx.lyrico.data.song.library.SongLibraryRepository
import com.lonx.lyrico.data.song.mapper.SongMetadataMapper
import com.lonx.lyrico.data.song.tag.AudioTagMutation
import com.lonx.lyrico.data.song.tag.AudioTagMutationMode
import com.lonx.lyrico.data.song.tag.AudioTagRepository
import com.lonx.lyrico.data.song.tag.AudioTagWriteResult

class SaveAudioTagsUseCase(
    private val database: LyricoDatabase,
    private val songLibraryRepository: SongLibraryRepository,
    private val audioTagRepository: AudioTagRepository,
    private val customTagKeyRepository: CustomTagKeyRepository,
    private val libraryIndexRepository: LibraryIndexRepository,
    private val songMetadataMapper: SongMetadataMapper
) {
    suspend operator fun invoke(
        uri: String,
        mutation: AudioTagMutation
    ): SaveAudioTagsResult {
        val writeResult = when (mutation.mode) {
            AudioTagMutationMode.Overwrite -> audioTagRepository.overwrite(uri, mutation)
            AudioTagMutationMode.Patch -> audioTagRepository.patch(uri, mutation)
        }

        return when (writeResult) {
            is AudioTagWriteResult.Success -> {
                val savedData = writeResult.savedData
                val song = songLibraryRepository.getSongByUri(uri)
                // If the write succeeded but the file could not be read back (read-only
                // parse failed even after the writable repair), the returned snapshot is
                // empty. Keep the existing library metadata instead of overwriting it with
                // a blank snapshot (which would wipe title/artist/album in the database).
                val usableMetadata = savedData.rawProperties != null || savedData.fileName.isNotEmpty()
                val updatedSong = if (song != null && usableMetadata) {
                    val updated = songMetadataMapper.applyAudioTagData(
                        old = song,
                        tag = savedData
                    )

                    database.withTransaction {
                        database.songDao().update(updated)
                        customTagKeyRepository.replaceForSong(updated.uri, savedData.customFields)
                        libraryIndexRepository.reindexSongInTransaction(updated)
                    }
                    updated
                } else {
                    song
                }

                SaveAudioTagsResult.Success(
                    song = updatedSong,
                    tagData = savedData
                )
            }
            is AudioTagWriteResult.PermissionRequired -> {
                SaveAudioTagsResult.PermissionRequired(writeResult.intentSender)
            }
            is AudioTagWriteResult.Failed -> SaveAudioTagsResult.Failed(writeResult.error)
        }
    }
}

sealed interface SaveAudioTagsResult {
    data class Success(
        val song: SongEntity?,
        val tagData: AudioTagData
    ) : SaveAudioTagsResult

    data class PermissionRequired(
        val intentSender: IntentSender
    ) : SaveAudioTagsResult

    data class Failed(
        val error: Throwable
    ) : SaveAudioTagsResult
}
