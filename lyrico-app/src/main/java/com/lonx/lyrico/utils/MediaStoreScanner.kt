package com.lonx.lyrico.utils

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.lonx.lyrico.data.model.SongFile
import java.io.File

/**
 * 通过 MediaStore 扫描整机音频，无需用户先手动添加自定义文件夹。
 * 行为对齐 Moto Music / SDWMP3_CN：直接查询 [MediaStore.Audio.Media]，
 * 返回设备上所有音乐文件，并附带用于重建文件夹层级的路径信息。
 */
data class MediaStoreSong(
    val songFile: SongFile,
    /** 歌曲所在文件夹的绝对路径（用于重建文件夹层级） */
    val folderPath: String,
    /** 该路径所属卷根（如 /storage/emulated/0），作为文件夹树的起点 */
    val volumeRoot: String
)

class MediaStoreScanner(
    private val context: Context
) {
    private val tag = "MediaStoreScanner"

    fun queryAllAudio(): List<MediaStoreSong> {
        val songs = mutableListOf<MediaStoreSong>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.IS_MUSIC
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = ?"
        val selectionArgs = arrayOf("1")
        val sortOrder = "${MediaStore.Audio.Media.DATA} ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val modCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
                val addedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val displayName = cursor.getString(nameCol) ?: continue
                    val data = cursor.getString(dataCol) ?: continue
                    if (displayName.isBlank() || !isSupportedAudioFile(displayName)) continue

                    val parentDir = File(data).parent ?: continue
                    val uri = ContentUris.withAppendedId(collection, id)
                    val lastModified = if (modCol >= 0) cursor.getLong(modCol) * 1000L else System.currentTimeMillis()
                    val dateAdded = if (addedCol >= 0) cursor.getLong(addedCol) * 1000L else lastModified
                    val duration = if (durCol >= 0) cursor.getLong(durCol) else 0L
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L

                    songs.add(
                        MediaStoreSong(
                            songFile = SongFile(
                                mediaId = id,
                                uri = uri,
                                filePath = data,
                                fileName = displayName,
                                lastModified = lastModified,
                                dateAdded = dateAdded,
                                duration = duration,
                                fileSize = size
                            ),
                            folderPath = parentDir,
                            volumeRoot = computeVolumeRoot(parentDir)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to query MediaStore audio", e)
        }

        return songs
    }

    /** 取路径前三个分段作为卷根，例如 /storage/emulated/0 或 /storage/ABCD-1234 */
    private fun computeVolumeRoot(path: String): String {
        val parts = path.split('/').filter { it.isNotEmpty() }
        val take = if (parts.size >= 3) parts.take(3) else parts
        return "/" + take.joinToString("/")
    }

    private val supportedAudioExtensions = setOf(
        "mp3", "flac", "m4a", "ogg", "opus", "wav", "aac", "wma", "ape"
    )

    private fun isSupportedAudioFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
        return extension in supportedAudioExtensions
    }
}
