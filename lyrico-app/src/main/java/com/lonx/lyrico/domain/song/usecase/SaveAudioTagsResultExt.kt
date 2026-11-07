package com.lonx.lyrico.domain.song.usecase

/**
 * Human/diagnostic detail for a non-success [SaveAudioTagsResult].
 *
 * Batch processors previously only reported a generic "Write failed", which hid the
 * real cause (e.g. a missing MediaStore write grant vs. a download/encoding error).
 * Centralizing it here keeps every caller consistent and avoids silent failures.
 */
fun SaveAudioTagsResult?.failureReason(): String = when (this) {
    null -> "Unknown (no result)"
    is SaveAudioTagsResult.Success -> ""
    is SaveAudioTagsResult.Failed -> error.message ?: error.toString()
    is SaveAudioTagsResult.PermissionRequired -> "PermissionRequired"
}
