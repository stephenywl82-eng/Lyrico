package com.lonx.lyrico.utils

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Environment
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
            // API 29+ 重建路径兜底用：DATA 为 null 时改用 RELATIVE_PATH + DISPLAY_NAME
            add(MediaStore.Audio.Media.RELATIVE_PATH)
            add(MediaStore.Audio.Media.VOLUME_NAME)
        }.toTypedArray()

        // 对齐 SDWMP3_CN：selection=null 取回全部音频，再在游标里做内存过滤。
        // 【真机根因 2026-11-05 · Motorola XT2409-5 / Android16 API36】
        // 绝不能在 SQL selection 里写「COALESCE(is_xxx, 0) = ?」并用 selectionArgs 绑定字符串 "0"：
        // COALESCE() 表达式没有列 affinity，ContentResolver 把 "0" 作为 TEXT 绑定后，
        // 与表达式的整数结果比较不成立（MediaProvider 的 SQL 重写不会做隐式转换），
        // 于是整机查询「合法地」返回 0 行、不抛异常、不计失败 —— 表现为 scanned 恒为 0。
        // 用 shell `content query --where "= 0"`（整数字面量）会被 SQLite 隐式转换而掩盖，
        // 故只有在 App 进程内用真 selectionArgs 才复现。实测：裸查询 count=399，带该 selection count=0。
        // 因此：SQL 层不加任何标志位条件，铃声/通知/闹钟/播客(+有声书) 的排除改为内存过滤。
        // 同时 IS_MUSIC=1 也不能用（部分 OEM ROM 不置位，会静默排除全部歌曲）。
        // 另注：Android 10（API 29）起禁止在 selection/sortOrder 使用 DATA 列，会抛异常导致静默空，
        // 故录制类路径排除也放内存，排序用 _ID。
        val excludeFlagColumns = buildList {
            add(MediaStore.Audio.Media.IS_RINGTONE)
            add(MediaStore.Audio.Media.IS_NOTIFICATION)
            add(MediaStore.Audio.Media.IS_ALARM)
            add(MediaStore.Audio.Media.IS_PODCAST)
            if (isAudiobookColumnAvailable) {
                add(MediaStore.Audio.Media.IS_AUDIOBOOK)
            }
        }
        val selection: String? = null
        val selectionArgs = emptyList<String>()
        val sortOrder = "${MediaStore.Audio.Media._ID} ASC"

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
                val relPathCol = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                // 铃声/通知/闹钟/播客(+有声书) 标志位：改为内存过滤（见上方 selection=null 说明）。
                val excludeFlagColIndices = excludeFlagColumns.map { cursor.getColumnIndex(it) }

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    // 标志位排除：任一被置 1（且非 null）则跳过。用 getInt 兼容 NULL→0。
                    if (excludeFlagColIndices.any { col -> col >= 0 && cursor.getInt(col) == 1 }) continue
                    val displayName = cursor.getString(nameCol) ?: continue
                    if (displayName.isBlank() || !isSupportedAudioFile(displayName)) continue
                    // API 29+ 仅持 READ_MEDIA_AUDIO 时，整机查询的 DATA 列被 MediaProvider 置为 null，
                    // 直接 getString(DATA) 拿到 null 会让全部歌曲被 `?: continue` 跳过 → scanned 恒为 0。
                    // 故优先用 DATA；为 null 时改用 RELATIVE_PATH + DISPLAY_NAME 重建路径。
                    val rawData = cursor.getString(dataCol)
                    val data = if (!rawData.isNullOrBlank()) {
                        rawData
                    } else {
                        val rel = if (relPathCol >= 0) cursor.getString(relPathCol) else null
                        buildString {
                            append(volumeRootPath())
                            if (!rel.isNullOrBlank()) append('/').append(rel.trimEnd('/'))
                            append('/').append(displayName)
                        }
                    }
                    // 内存过滤录制/语音类音频（对应 SDWMP3_CN 的路径关键字黑名单）。
                    // 不能用 DATA 列做 SQL 过滤，否则 API 29+ 直接抛异常使整机扫描静默返回空。
                    if (RECORDING_PATH_KEYWORDS.any { kw -> data.contains(kw, ignoreCase = true) }) continue

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
                Log.i(tag, "queryAllAudio: cursor=${cursor.count}, collected=${songs.size}") // selection=null 全量取回，标志位/录制路径均内存过滤
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to query MediaStore audio", e)
        }

        return songs
    }

    /** DATA 为 null 时重建路径用的卷根：主外部存储（绝大多数用户音乐所在）。
     *  次级卷（SD 卡）路径可能不精确，但歌曲仍会被正常扫描。 */
    private fun volumeRootPath(): String {
        return Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')
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
