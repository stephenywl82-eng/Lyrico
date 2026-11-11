package com.lonx.lyrico.data.song.scan

interface LibraryScanRepository {
    suspend fun synchronize(
        request: LibraryScanRequest,
        onProgress: suspend (LibraryScanProgress) -> Unit = {}
    ): LibraryScanResult

    /**
     * 清理曲库中「文件本体已不存在」的歌曲（如已被删除、移动或媒体库记录过期）。
     * 通过真正打开文件描述符来判定是否存在，从而绕过未刷新的 MediaStore 缓存。
     *
     * @param onProgress 进度回调：(已检查数, 总数)
     * @return 被清理的歌曲数量
     */
    suspend fun cleanupMissingSongs(
        onProgress: suspend (checked: Int, total: Int) -> Unit = { _, _ -> }
    ): Int
}
