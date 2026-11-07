package com.lonx.lyrico.data.song.tag

import android.util.Log
import com.lonx.lyrico.data.song.file.AudioFileAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

interface ImageBytesFetcher {
    suspend fun fetch(source: PictureSource): ByteArray?
}

class DefaultImageBytesFetcher(
    private val fileAccess: AudioFileAccess,
    private val okHttpClient: OkHttpClient
) : ImageBytesFetcher {
    override suspend fun fetch(source: PictureSource): ByteArray? = withContext(Dispatchers.IO) {
        when (source) {
            is PictureSource.Bytes -> source.bytes
            is PictureSource.UriSource -> fileAccess.openInputBytes(source.uri)
            is PictureSource.UrlSource -> fetchUrl(source.url)
        }
    }

    private fun fetchUrl(url: String): ByteArray? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "image/avif,image/webp,image/apng,image/jpeg,image/png,image/*,*/*;q=0.8")
            .header("Connection", "close")
            .build()
        return try {
            // 使用本地的长超时 client，避免默认连接/读取超时在并发批量下载时把大图判为失败。
            val callClient = okHttpClient.newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()
            callClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body.bytes()
                } else {
                    // 之前这里静默返回 null，导致上层只看到笼统的 "Write failed"，
                    // 真正的 HTTP 错误被吞掉。现在打印出来，方便定位（如 403 缺 UA、404 等）。
                    Log.e(TAG, "Cover download failed: HTTP ${response.code} for $url")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cover download error for $url", e)
            null
        }
    }

    private companion object {
        const val TAG = "ImageBytesFetcher"
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
