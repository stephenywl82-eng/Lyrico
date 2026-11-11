package com.lonx.lyrico.data.song.file

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import com.lonx.lyrico.data.model.entity.SongEntity
import com.lonx.lyrico.data.model.log.AppLogType
import com.lonx.lyrico.data.repository.AppLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class SongFileRepositoryImpl(
    private val context: Context,
    private val fileAccess: AudioFileAccess,
    private val appLogRepository: AppLogRepository
) : SongFileRepository {

    override suspend fun deleteFile(song: SongEntity): DeleteSongFileResult {
        return if (song.source == "SAF") {
            deleteSafSongFile(song)
        } else {
            deleteMediaStoreSongFile(song)
        }
    }

    override suspend fun deleteFiles(songs: List<SongEntity>): BatchSongFileOperationResult {
        return BatchSongFileOperationResult(
            items = songs.map { song ->
                BatchSongFileOperationItem(song, deleteFile(song))
            }
        )
    }

    override suspend fun renameFile(
        song: SongEntity,
        newFileName: String
    ): RenameSongFileResult = withContext(Dispatchers.IO) {
        if (song.source == "SAF") {
            renameSafSong(song, newFileName)
        } else {
            // 非 SAF（来自 MediaStore 整机扫描）的歌曲不能用 File.renameTo：
            // Android 11+ 分区存储下对共享存储文件直接 renameTo 会返回 false 而静默失败。
            // 正确做法是走 MediaStore 的 ContentResolver.update(DISPLAY_NAME)，
            // 权限不足时回退到 FUSE 直接文件路径重命名。
            renameMediaStoreSong(song, newFileName)
        }
    }

    private suspend fun renameMediaStoreSong(
        song: SongEntity,
        newFileName: String
    ): RenameSongFileResult {
        val uri = song.uri.toUri()
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, newFileName)
        }
        return try {
            var rows = context.contentResolver.update(uri, values, null, null)
            if (rows <= 0) {
                // 直接 update 返回 0 行：原始 uri 可能不是规范的 MediaStore item（例如带
                // external_files 段或来自 SAF 但可匹配到库内媒体）。尝试解析为规范 uri 后重试一次。
                val canonical = fileAccess.resolveMediaStoreAudioUri(song.uri)?.toUri()
                if (canonical != null && canonical != uri) {
                    rows = context.contentResolver.update(canonical, values, null, null)
                }
            }
            if (rows <= 0) {
                // 仍 0 行：Android 11+ 分区存储下不能直接走 File.renameTo（会静默失败），
                // 因此只有在 uri 本身就是 file:// 且应用具备访问权限时才走 FUSE 更名，
                // 否则明确返回失败原因，避免误导用户为“无响应”。
                if (uri.scheme == "file") {
                    renameFilePathSong(song, newFileName)
                } else {
                    RenameSongFileResult.Failed(
                        IOException("MediaStore 重命名未生效，请确认已授予文件访问权限")
                    )
                }
            } else {
                val parentPath = song.filePath.substringBeforeLast("/", missingDelimiterValue = "")
                val newPath = if (parentPath.isBlank()) newFileName else "$parentPath/$newFileName"
                RenameSongFileResult.Success(
                    oldUri = song.uri,
                    newUri = song.uri,
                    newFilePath = newPath,
                    newFileName = newFileName
                )
            }
        } catch (e: RecoverableSecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                RenameSongFileResult.PermissionRequired(e.userAction.actionIntent.intentSender)
            } else {
                RenameSongFileResult.Failed(e)
            }
        } catch (e: Exception) {
            fileAccess.writePermissionFromThrowable(song.uri, e)?.let {
                return RenameSongFileResult.PermissionRequired(it)
            }
            Log.e(TAG, "Failed to rename MediaStore song: ${song.fileName}", e)
            logException("Failed to rename MediaStore song", e, song.uri)
            RenameSongFileResult.Failed(e)
        }
    }

    private suspend fun deleteSafSongFile(song: SongEntity): DeleteSongFileResult {
        return try {
            val uri = song.uri.toUri()
            val deleted = fileAccess.deleteDocument(uri)
            when {
                deleted -> DeleteSongFileResult.Deleted
                fileAccess.isUriMissing(uri) -> DeleteSongFileResult.AlreadyMissing
                else -> DeleteSongFileResult.Failed(IllegalStateException("SAF provider refused delete"))
            }
        } catch (e: FileNotFoundException) {
            DeleteSongFileResult.AlreadyMissing
        } catch (e: Exception) {
            fileAccess.writePermissionFromThrowable(song.uri, e)?.let {
                return DeleteSongFileResult.PermissionRequired(it)
            }
            if (fileAccess.isUriMissing(song.uri.toUri())) {
                DeleteSongFileResult.AlreadyMissing
            } else {
                Log.e(TAG, "Failed to delete SAF song: ${song.uri}", e)
                logException("Failed to delete SAF song", e, song.uri)
                DeleteSongFileResult.Failed(e)
            }
        }
    }

    private suspend fun deleteMediaStoreSongFile(song: SongEntity): DeleteSongFileResult {
        return try {
            val rowsDeleted = context.contentResolver.delete(song.uri.toUri(), null, null)
            if (rowsDeleted > 0) {
                DeleteSongFileResult.Deleted
            } else {
                DeleteSongFileResult.AlreadyMissing
            }
        } catch (e: FileNotFoundException) {
            DeleteSongFileResult.AlreadyMissing
        } catch (e: RecoverableSecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                DeleteSongFileResult.PermissionRequired(e.userAction.actionIntent.intentSender)
            } else {
                DeleteSongFileResult.Failed(e)
            }
        } catch (e: Exception) {
            fileAccess.writePermissionFromThrowable(song.uri, e)?.let {
                return DeleteSongFileResult.PermissionRequired(it)
            }
            Log.e(TAG, "Failed to delete song: ${song.title}", e)
            logException("Failed to delete song: ${song.title}", e, song.uri)
            DeleteSongFileResult.Failed(e)
        }
    }

    private suspend fun renameSafSong(
        song: SongEntity,
        newFileName: String
    ): RenameSongFileResult {
        return try {
            val renamedUri = fileAccess.renameDocument(song.uri.toUri(), newFileName)
                ?: return RenameSongFileResult.Failed(IllegalStateException("SAF provider returned null"))

            val parentPath = song.filePath.substringBeforeLast("/", missingDelimiterValue = "")
            val newPath = if (parentPath.isBlank()) newFileName else "$parentPath/$newFileName"

            RenameSongFileResult.Success(
                oldUri = song.uri,
                newUri = renamedUri.toString(),
                newFilePath = newPath,
                newFileName = newFileName
            )
        } catch (e: Exception) {
            fileAccess.writePermissionFromThrowable(song.uri, e)?.let {
                return RenameSongFileResult.PermissionRequired(it)
            }
            Log.e(TAG, "Failed to rename SAF song: ${song.uri}", e)
            logException("Failed to rename SAF song", e, song.uri)
            RenameSongFileResult.Failed(e)
        }
    }

    private fun renameFilePathSong(
        song: SongEntity,
        newFileName: String
    ): RenameSongFileResult {
        return try {
            val oldFile = File(song.filePath)
            if (!oldFile.exists()) {
                return RenameSongFileResult.Failed(FileNotFoundException(song.filePath))
            }

            val newFile = File(oldFile.parent, newFileName)
            if (newFile.exists()) {
                return RenameSongFileResult.NameConflict(newFileName)
            }

            if (!oldFile.renameTo(newFile)) {
                return RenameSongFileResult.Failed(IllegalStateException("File rename returned false"))
            }

            RenameSongFileResult.Success(
                oldUri = song.uri,
                newUri = newFile.toURI().toString(),
                newFilePath = newFile.absolutePath,
                newFileName = newFileName
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rename song: ${song.fileName}", e)
            RenameSongFileResult.Failed(e)
        }
    }

    private suspend fun logException(
        message: String,
        throwable: Throwable,
        relatedId: String? = null
    ) {
        try {
            appLogRepository.logException(
                type = AppLogType.APP,
                tag = TAG,
                message = message,
                throwable = throwable,
                relatedId = relatedId
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write exception log", e)
        }
    }

    private companion object {
        const val TAG = "SongFileRepository"
    }
}
