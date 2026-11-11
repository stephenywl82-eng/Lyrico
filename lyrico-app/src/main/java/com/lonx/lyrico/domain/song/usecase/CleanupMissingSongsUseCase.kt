package com.lonx.lyrico.domain.song.usecase

import com.lonx.lyrico.data.song.scan.LibraryScanRepository

class CleanupMissingSongsUseCase(
    private val libraryScanRepository: LibraryScanRepository
) {
    suspend operator fun invoke(
        onProgress: suspend (checked: Int, total: Int) -> Unit = { _, _ -> }
    ): Int {
        return libraryScanRepository.cleanupMissingSongs(onProgress)
    }
}
