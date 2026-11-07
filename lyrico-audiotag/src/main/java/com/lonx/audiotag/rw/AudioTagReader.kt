package com.lonx.audiotag.rw

import android.media.MediaMetadataRetriever
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import android.util.Log
import com.lonx.audiotag.TagLib
import com.lonx.audiotag.internal.FdUtils
import com.lonx.audiotag.model.AudioPicture
import com.lonx.audiotag.model.AudioPictureType
import com.lonx.audiotag.model.AudioTagData
import com.lonx.audiotag.model.AudioTagKeys
import com.lonx.audiotag.model.CustomTagField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AudioTagReader {

    private const val TAG = "AudioTagReader"

    suspend fun read(
        pfd: ParcelFileDescriptor,
        readPictures: Boolean = true,
        multiValueSeparator: String = "/",
        strict: Boolean = false,
        filePath: String? = null
    ): AudioTagData {
        return withContext(Dispatchers.IO) {
            try {
                val nativeFd = FdUtils.getNativeFd(pfd)

                // 读取音频属性
                val audioProps = TagLib.getAudioProperties(nativeFd)

                // TagLib 在 fd 流下常读不出音频属性（时长返回 0），用 MediaMetadataRetriever 兜底
                var durationMs = audioProps?.length ?: 0
                var bitrateKbps = audioProps?.bitrate ?: 0
                var sampleRateHz = audioProps?.sampleRate ?: 0
                var channelCount = audioProps?.channels ?: 0
                if (durationMs <= 0) {
                    try {
                        val retriever = MediaMetadataRetriever()
                        var usedPath = false
                        // 优先用真实文件路径（最可靠，对 m4a/opus 也能正确解析音频属性）
                        val path = filePath
                        if (!path.isNullOrEmpty() && (path.startsWith("/") || path.startsWith("file://"))) {
                            val cleanPath = if (path.startsWith("file://")) path.substring(7) else path
                            try {
                                retriever.setDataSource(cleanPath)
                                usedPath = true
                            } catch (pe: Exception) {
                                Log.w(TAG, "MediaMetadataRetriever setDataSource(path) failed: $cleanPath", pe)
                            }
                        }
                        // 后备：用 fd。dup() 与原始 fd 共享文件偏移，TagLib 读完后 fd 已位于末尾，
                        // MediaMetadataRetriever 从当前偏移读取会失败，需先复位到文件开头。
                        if (!usedPath) {
                            try {
                                Os.lseek(pfd.fileDescriptor, 0L, OsConstants.SEEK_SET)
                            } catch (_: Exception) { /* 不支持 seek 的 fd 忽略 */ }
                            retriever.setDataSource(pfd.fileDescriptor)
                        }
                        val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                        if (dur != null && dur > 0) {
                            durationMs = dur.toInt()
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                                ?.toIntOrNull()?.let { bitrateKbps = it / 1000 }
                            // 采样率/声道数 MediaMetadataRetriever 不提供标准 KEY，沿用 TagLib 结果（为 0 时不强求）
                        } else {
                            Log.w(TAG, "MediaMetadataRetriever returned no duration (usedPath=$usedPath)")
                        }
                        retriever.release()
                    } catch (re: Exception) {
                        Log.w(TAG, "MediaMetadataRetriever fallback failed", re)
                    }
                }

                // 读取 Metadata
                val metaFd = FdUtils.getNativeFd(pfd)
                val metadata = TagLib.getMetadata(metaFd, readPictures)
                    ?: run {
                        val detail = TagLib.getLastTagError()
                        val friendly = TagErrorMessages.friendly(detail)
                        throw IllegalStateException(
                            friendly
                                ?: ("Unable to read audio metadata" +
                                    (detail?.let { ": $it" } ?: ""))
                        )
                    }

                // 处理图片
                val picList = ArrayList<AudioPicture>()
                if (readPictures) {
                    for (pic in metadata.pictures) {
                        picList.add(AudioPicture(
                            data = pic.data,
                            mimeType = pic.mimeType,
                            description = pic.description,
                            pictureType = pic.pictureType
                        ))
                    }
                }

                // 处理属性 Map
                val props = metadata.propertyMap

                props.forEach{(key, value) ->
                    Log.d(TAG, "Read tag: $key = ${value.joinToString()}")
                }

                fun firstOf(vararg keys: String): String? {
                    for (key in keys) {
                        val arr = props[key]
                        if (!arr.isNullOrEmpty()) {
                            val value = arr[0].trim()
                            if (value.isNotEmpty()) return value
                        }
                    }
                    return null
                }

                fun joinedOf(separator: String, vararg keys: String): String? {
                    for (key in keys) {
                        val arr = props[key]
                        if (!arr.isNullOrEmpty()) {
                            val filtered = arr.map { it.trim() }.filter { it.isNotEmpty() }
                            if (filtered.isNotEmpty()) {
                                return filtered.joinToString(separator)
                            }
                        }
                    }
                    return null
                }

                fun firstIntOf(vararg keys: String): Int? {
                    val raw = firstOf(*keys) ?: return null
                    return raw.substringBefore('/').toIntOrNull()
                }

                val lyrics = joinedOf(
                    multiValueSeparator,
                    "LYRICS",
                    "UNSYNCED LYRICS",
                    "USLT",
                    "LYRIC",
                    "LYRICSENG"

                )



                val albumArtist = joinedOf(
                    multiValueSeparator,
                    "ALBUMARTIST",     // FLAC/Vorbis
                    "ALBUM ARTIST",
                    "TPE2",            // ID3v2
                    "aART",            // MP4
                    "ALBUMARTISTSORT"
                )

                val discNumber = firstIntOf(
                    "DISCNUMBER",
                    "DISC",
                    "TPOS",           // ID3v2
                    "DISKNUMBER"
                )

                val composer = joinedOf(
                    multiValueSeparator,
                    "COMPOSER",
                    "TCOM",           // ID3v2
                    "©wrt"            // MP4
                )

                val lyricist = joinedOf(
                    multiValueSeparator,
                    "LYRICIST",
                    "TEXT",           // ID3v2 作词
                    "WRITER",
                    "LYRICS BY"
                )

                val comment = joinedOf(
                    multiValueSeparator,
                    "COMMENT",
                    "COMM",           // ID3
                    "DESCRIPTION"
                )

                val copyright = joinedOf(
                    multiValueSeparator,
                    "COPYRIGHT",
                    "TCOP",
                    "CPRO",
                    "©cpy"
                )

                var ratingStar: Int? = null
                val rawRating = firstOf("RATING", "POPM", "RATE")
                if (rawRating != null) {
                    if (rawRating.contains("|")) {
                        val popm = rawRating.split("|").getOrNull(1)?.toIntOrNull() ?: 0
                        ratingStar = when {
                            popm >= 255 -> 5
                            popm >= 196 -> 4
                            popm >= 128 -> 3
                            popm >= 64 -> 2
                            popm > 0 -> 1
                            else -> 0
                        }
                    } else {
                        val r = rawRating.toIntOrNull() ?: 0
                        ratingStar = if (r > 5) {
                            when {
                                r >= 100 -> 5
                                r >= 80 -> 4
                                r >= 60 -> 3
                                r >= 40 -> 2
                                r > 0 -> 1
                                else -> 0
                            }
                        } else {
                            r
                        }
                    }
                    if (ratingStar == 0) ratingStar = null
                }

                val customFields = props.entries
                    .asSequence()
                    .filterNot { (key, _) -> AudioTagKeys.isReserved(key) }
                    .map { (key, values) ->
                        CustomTagField(
                            key = key,
                            value = values.joinToString("; ")
                        )
                    }
                    .sortedBy { it.key.uppercase() }
                    .toList()

                return@withContext AudioTagData(
                    title = joinedOf(multiValueSeparator, "TITLE"),
                    artist = joinedOf(multiValueSeparator, "ARTIST"),
                    album = joinedOf(multiValueSeparator, "ALBUM"),
                    genre = joinedOf(multiValueSeparator, "GENRE", "TCON")
                        ?: joinedOf(multiValueSeparator, "STYLE", "SUBGENRE", "MOOD"),
                    date = joinedOf(multiValueSeparator, "DATE", "YEAR"),
                    language = joinedOf(multiValueSeparator, "LANGUAGE", "TLAN"),
                    trackNumber = firstIntOf("TRACKNUMBER", "TRACK", "TRCK")?.toString(),

                    albumArtist = albumArtist,
                    discNumber = discNumber,
                    composer = composer,
                    lyricist = lyricist,
                    comment = comment,
                    lyrics = lyrics,
                    copyright = copyright,
                    rating = ratingStar,
                    replayGainTrackGain = joinedOf(multiValueSeparator, "REPLAYGAIN_TRACK_GAIN"),
                    replayGainTrackPeak = joinedOf(multiValueSeparator, "REPLAYGAIN_TRACK_PEAK"),
                    replayGainAlbumGain = joinedOf(multiValueSeparator, "REPLAYGAIN_ALBUM_GAIN"),
                    replayGainAlbumPeak = joinedOf(multiValueSeparator, "REPLAYGAIN_ALBUM_PEAK"),
                    replayGainReferenceLoudness = joinedOf(
                        multiValueSeparator,
                        "REPLAYGAIN_REFERENCE_LOUDNESS"
                    ),

                    durationMilliseconds = durationMs,
                    bitrate = bitrateKbps,
                    sampleRate = sampleRateHz,
                    channels = channelCount,
                    rawProperties = props,
                    customFields = customFields,
                    pictures = picList,
                    supportsTypedPictures = metadata.supportsTypedPictures
                )

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException || strict) throw e
                Log.e(TAG, "Read error", e)
                AudioTagData()
            }
        }
    }
    /**
     * @param description 需要优先匹配的图片描述（艺术家图片用它记录归属），为空时只按类型取。
     */
    suspend fun readPicture(
        pfd: ParcelFileDescriptor,
        pictureType: AudioPictureType = AudioPictureType.FrontCover,
        fallbackPictureTypes: List<AudioPictureType> = emptyList(),
        fallbackToAny: Boolean = pictureType == AudioPictureType.FrontCover,
        description: String? = null
    ): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                val metaFd = FdUtils.getNativeFd(pfd)
                val metadata = TagLib.getPicture(
                    fd = metaFd,
                    pictureType = pictureType,
                    fallbackPictureTypes = fallbackPictureTypes,
                    fallbackToAny = fallbackToAny,
                    description = description
                )
                val pic = metadata?.data
                return@withContext pic ?: byteArrayOf()
            } catch (e: Exception) {
                Log.e(TAG, "Read error", e)
                byteArrayOf()
            }
        }
    }
}
