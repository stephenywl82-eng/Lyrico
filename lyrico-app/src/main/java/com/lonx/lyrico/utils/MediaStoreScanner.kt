package com.lonx.lyrico.utils

import android.content.ContentUris
import android.content.Context
import android.os.Build
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

        // IS_AUDIOBOOK 仅在 Android 11(R/30)+ 才有该列；minSdk=28，低于此版本的
        // 设备查询该列会直接抛异常。故按系统版本决定是否纳入过滤。
        val isAudiobookColumnAvailable =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.DATA)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.IS_MUSIC)
            add(MediaStore.Audio.Media.IS_RINGTONE)
            add(MediaStore.Audio.Media.IS_NOTIFICATION)
            add(MediaStore.Audio.Media.IS_ALARM)
            add(MediaStore.Audio.Media.IS_PODCAST)
            if (isAudiobookColumnAvailable) {
                add(MediaStore.Audio.Media.IS_AUDIOBOOK)
            }
        }.toTypedArray()

        // 对齐 SDWMP3_CN：只取「音乐」，排除系统音效与录制音频。
        // 关键：MediaStore 里 IS_RINGTONE/IS_NOTIFICATION/... 这些列对普通音乐文件
        // 通常是 NULL 而非 0，直接写 `IS_RINGTONE = 0` 在 NULL 时整条判定为 NULL
        // （非真）会把歌曲全部误排除。因此用 COALESCE(列, 0) = 0：NULL 视为「非该类别」保留。
        // 1) 标志位排除 铃声 / 通知 / 闹钟 / 播客（+ 30+ 的有声书）；
        // 2) 路径关键字排除 录音机、微信语音、钉钉 等用户录制音频。
        val excludeFlagColumns = buildList {
            add(MediaStore.Audio.Media.IS_RINGTONE)
            add(MediaStore.Audio.Media.IS_NOTIFICATION)
            add(MediaStore.Audio.Media.IS_ALARM)
            add(MediaStore.Audio.Media.IS_PODCAST)
            if (isAudiobookColumnAvailable) {
                add(MediaStore.Audio.Media.IS_AUDIOBOOK)
            }
        }

        val selectionParts = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()
        selectionParts.add("COALESCE(${MediaStore.Audio.Media.IS_MUSIC}, 0) = ?")
        selectionArgs.add("1")
        for (flag in excludeFlagColumns) {
            selectionParts.add("COALESCE($flag, 0) = ?")
            selectionArgs.add("0")
        }
        for (pathKeyword in RECORDING_PATH_KEYWORDS) {
            selectionParts.add("(COALESCE(${MediaStore.Audio.Media.DATA}, '') NOT LIKE ?)")
            selectionArgs.add("%$pathKeyword%")
        }
        val selection = selectionParts.joinToString(" AND ")
        val sortOrder = "${MediaStore.Audio.Media.DATA} ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs.toTypedArray(),
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

    /**
     * 路径关键字黑名单（大小写敏感，按 %keyword% 模糊匹配 DATA 列）。
     * 用于排除「用户录制」的音频：录音机、微信语音、钉钉语音、系统提示音等。
     * 与 SDWMP3_CN 的过滤策略一致。
     */
    private val RECORDING_PATH_KEYWORDS = listOf(
        "Record",        // Recordings / Recorder / Recording / Records
        "录音",
        "录音机",
        "WhatsApp Voice Notes",
        "MicroMsg",      // 微信语音
        "WeChat",
        "DingTalk",      // 钉钉语音
        "Ringtones",
        "Notifications",
        "Alarms",
        "Podcasts",
        "Sounds"
    )

    private fun isSupportedAudioFile(fileName: String): Boolean {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase()
        return extension in supportedAudioExtensions
    }
}
