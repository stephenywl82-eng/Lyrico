package com.lonx.lyrico.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class LyricsExportDestination {
    SELECTED_DIRECTORY,
    AUDIO_DIRECTORY
}
