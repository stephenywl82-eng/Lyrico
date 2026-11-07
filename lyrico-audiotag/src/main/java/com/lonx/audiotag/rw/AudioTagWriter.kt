package com.lonx.audiotag.rw

import android.os.ParcelFileDescriptor
import android.util.Log
import com.lonx.audiotag.TagLib
import com.lonx.audiotag.internal.FdUtils
import com.lonx.audiotag.model.AudioPicture
import com.lonx.audiotag.model.Picture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.HashMap
import kotlin.collections.iterator

object AudioTagWriter {
    private const val TAG = "AudioTagWriter"

    suspend fun writeTags(
        pfd: ParcelFileDescriptor,
        updates: Map<String, String>,
        preserveOldTags: Boolean = true
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val fd = FdUtils.getNativeFd(pfd)
                val mapToSave = HashMap<String, Array<String>>()

                if (preserveOldTags) {
                    val oldFd = FdUtils.getNativeFd(pfd)
                    val oldMeta = TagLib.getMetadata(oldFd, false)
                    if (oldMeta != null) {
                        mapToSave.putAll(oldMeta.propertyMap)
                    }
                }

                for ((k, v) in updates) {
                    mapToSave[k] = arrayOf(v)
                }

                mapToSave.forEach { (string, strings) ->
                    Log.d(TAG, "Write tag: $string = $strings")
                }
                val ok = TagLib.savePropertyMap(fd, mapToSave)
                if (!ok) {
                    val detail = TagLib.getLastTagError()
                    val friendly = TagErrorMessages.friendly(detail)
                    throw IllegalStateException(
                        friendly ?: ("Audio tag write failed" + (detail?.let { ": $it" } ?: ""))
                    )
                }
                true
            } catch (e: Exception) {
                if (e is IllegalStateException) throw e
                Log.e(TAG, "Write tags error", e)
                false
            }
        }
    }


    suspend fun writePictures(pfd: ParcelFileDescriptor, pictures: List<AudioPicture>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val fd = FdUtils.getNativeFd(pfd)
                val libPics = ArrayList<Picture>()
                for (p in pictures) {
                    libPics.add(Picture(
                        data = p.data,
                        mimeType = p.mimeType,
                        description = p.description,
                        pictureType = p.pictureType
                    ))
                }
                val arr = libPics.toTypedArray()
                val ok = TagLib.savePictures(fd, arr)
                if (!ok) {
                    val detail = TagLib.getLastTagError()
                    val friendly = TagErrorMessages.friendly(detail)
                    throw IllegalStateException(
                        friendly ?: ("Audio picture write failed" + (detail?.let { ": $it" } ?: ""))
                    )
                }
                true
            } catch (e: Exception) {
                if (e is IllegalStateException) throw e
                Log.e(TAG, "Write pictures error", e)
                false
            }
        }
    }
}
