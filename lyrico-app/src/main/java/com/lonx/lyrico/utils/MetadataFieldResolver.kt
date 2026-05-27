package com.lonx.lyrico.utils

import com.lonx.audiotag.model.AudioTagData
import com.lonx.audiotag.model.CustomTagField
import com.lonx.lyrico.data.model.plugin.PluginMetadataFieldTarget
import com.lonx.lyrico.data.model.plugin.PluginMetadataFieldWriteRule
import com.lonx.lyrico.data.model.plugin.PluginMetadataWriteMode
import com.lonx.lyrico.data.model.ScoredSearchResult
import com.lonx.lyrico.data.model.entity.SongEntity

class MetadataFieldResolver(
    private val minimumScore: Double = 0.35
) {
    fun resolve(
        currentSong: SongEntity,
        scoredResults: List<ScoredSearchResult>,
        rules: List<PluginMetadataFieldWriteRule>,
        currentTagData: AudioTagData? = null
    ): AudioTagData {
        var output = AudioTagData()

        rules.asSequence()
            .filter { it.mode != PluginMetadataWriteMode.DISABLED }
            .forEach { rule ->
                val value = findBestValue(rule, scoredResults) ?: return@forEach
                output = writeIfAllowed(output, currentSong, currentTagData, rule, value)
            }

        return output
    }

    fun mergeNonNull(first: AudioTagData, second: AudioTagData): AudioTagData {
        return first.copy(
            title = second.title ?: first.title,
            artist = second.artist ?: first.artist,
            album = second.album ?: first.album,
            albumArtist = second.albumArtist ?: first.albumArtist,
            genre = second.genre ?: first.genre,
            date = second.date ?: first.date,
            language = second.language ?: first.language,
            trackNumber = second.trackNumber ?: first.trackNumber,
            discNumber = second.discNumber ?: first.discNumber,
            composer = second.composer ?: first.composer,
            lyricist = second.lyricist ?: first.lyricist,
            comment = second.comment ?: first.comment,
            lyrics = second.lyrics ?: first.lyrics,
            copyright = second.copyright ?: first.copyright,
            rating = second.rating ?: first.rating,
            replayGainTrackGain = second.replayGainTrackGain ?: first.replayGainTrackGain,
            replayGainTrackPeak = second.replayGainTrackPeak ?: first.replayGainTrackPeak,
            replayGainAlbumGain = second.replayGainAlbumGain ?: first.replayGainAlbumGain,
            replayGainAlbumPeak = second.replayGainAlbumPeak ?: first.replayGainAlbumPeak,
            replayGainReferenceLoudness = second.replayGainReferenceLoudness
                ?: first.replayGainReferenceLoudness,
            picUrl = second.picUrl ?: first.picUrl,
            pictures = if (second.pictures.isNotEmpty()) second.pictures else first.pictures,
            customFields = if (second.customFields.isNotEmpty()) second.customFields else first.customFields
        )
    }

    private fun findBestValue(
        rule: PluginMetadataFieldWriteRule,
        scoredResults: List<ScoredSearchResult>
    ): String? {
        return scoredResults
            .asSequence()
            .filter { it.result.pluginId == rule.pluginId }
            .filter { source ->
                source.source?.metadataFields
                    ?.any { it.key == rule.normalizedKey && it.writeable && !it.internal } != false
            }
            .filter { it.score >= minimumScore }
            .sortedByDescending { it.score }
            .mapNotNull { it.result.normalizedFields()[rule.normalizedKey]?.takeIf(String::isNotBlank) }
            .firstOrNull()
    }

    private fun writeIfAllowed(
        currentOutput: AudioTagData,
        currentSong: SongEntity,
        currentTagData: AudioTagData?,
        rule: PluginMetadataFieldWriteRule,
        value: String
    ): AudioTagData {
        return when (rule.target) {
            PluginMetadataFieldTarget.TITLE -> if (canWriteText(rule.mode, currentTagData?.title ?: currentSong.title)) currentOutput.copy(title = value) else currentOutput
            PluginMetadataFieldTarget.ARTIST -> if (canWriteText(rule.mode, currentTagData?.artist ?: currentSong.artist)) currentOutput.copy(artist = value) else currentOutput
            PluginMetadataFieldTarget.ALBUM -> if (canWriteText(rule.mode, currentTagData?.album ?: currentSong.album)) currentOutput.copy(album = value) else currentOutput
            PluginMetadataFieldTarget.ALBUM_ARTIST -> if (canWriteText(rule.mode, currentTagData?.albumArtist ?: currentSong.albumArtist)) currentOutput.copy(albumArtist = value) else currentOutput
            PluginMetadataFieldTarget.GENRE -> if (canWriteText(rule.mode, currentTagData?.genre ?: currentSong.genre)) currentOutput.copy(genre = value) else currentOutput
            PluginMetadataFieldTarget.DATE -> if (canWriteText(rule.mode, currentTagData?.date ?: currentSong.date)) currentOutput.copy(date = value) else currentOutput
            PluginMetadataFieldTarget.TRACK_NUMBER -> if (canWriteText(rule.mode, currentTagData?.trackNumber ?: currentSong.trackerNumber)) currentOutput.copy(trackNumber = value) else currentOutput
            PluginMetadataFieldTarget.DISC_NUMBER -> if (canWriteText(rule.mode, (currentTagData?.discNumber ?: currentSong.discNumber)?.toString())) currentOutput.copy(discNumber = value.toIntOrNull()) else currentOutput
            PluginMetadataFieldTarget.COMPOSER -> if (canWriteText(rule.mode, currentTagData?.composer ?: currentSong.composer)) currentOutput.copy(composer = value) else currentOutput
            PluginMetadataFieldTarget.LYRICIST -> if (canWriteText(rule.mode, currentTagData?.lyricist ?: currentSong.lyricist)) currentOutput.copy(lyricist = value) else currentOutput
            PluginMetadataFieldTarget.COMMENT -> if (canWriteText(rule.mode, currentTagData?.comment ?: currentSong.comment)) currentOutput.copy(comment = value) else currentOutput
            PluginMetadataFieldTarget.LYRICS -> if (canWriteText(rule.mode, currentTagData?.lyrics ?: currentSong.lyrics)) currentOutput.copy(lyrics = value) else currentOutput
            PluginMetadataFieldTarget.COVER -> if (canWriteText(rule.mode, currentTagData?.picUrl)) currentOutput.copy(picUrl = value) else currentOutput
            PluginMetadataFieldTarget.LANGUAGE -> if (canWriteText(rule.mode, currentTagData?.language)) currentOutput.copy(language = value) else currentOutput
            PluginMetadataFieldTarget.COPYRIGHT -> if (canWriteText(rule.mode, currentTagData?.copyright)) currentOutput.copy(copyright = value) else currentOutput
            PluginMetadataFieldTarget.RATING -> if (canWriteText(rule.mode, currentTagData?.rating?.toString())) currentOutput.copy(rating = value.toIntOrNull()) else currentOutput
            PluginMetadataFieldTarget.REPLAY_GAIN_TRACK_GAIN -> if (canWriteText(rule.mode, currentTagData?.replayGainTrackGain ?: currentSong.replayGainTrackGain)) currentOutput.copy(replayGainTrackGain = value) else currentOutput
            PluginMetadataFieldTarget.REPLAY_GAIN_TRACK_PEAK -> if (canWriteText(rule.mode, currentTagData?.replayGainTrackPeak ?: currentSong.replayGainTrackPeak)) currentOutput.copy(replayGainTrackPeak = value) else currentOutput
            PluginMetadataFieldTarget.REPLAY_GAIN_ALBUM_GAIN -> if (canWriteText(rule.mode, currentTagData?.replayGainAlbumGain)) currentOutput.copy(replayGainAlbumGain = value) else currentOutput
            PluginMetadataFieldTarget.REPLAY_GAIN_ALBUM_PEAK -> if (canWriteText(rule.mode, currentTagData?.replayGainAlbumPeak)) currentOutput.copy(replayGainAlbumPeak = value) else currentOutput
            PluginMetadataFieldTarget.REPLAY_GAIN_REFERENCE_LOUDNESS -> if (canWriteText(rule.mode, currentTagData?.replayGainReferenceLoudness ?: currentSong.replayGainReferenceLoudness)) currentOutput.copy(replayGainReferenceLoudness = value) else currentOutput
            PluginMetadataFieldTarget.CUSTOM -> {
                val key = rule.customTagKey?.takeIf { it.isNotBlank() } ?: rule.normalizedKey
                val currentValue = currentTagData?.customFields?.firstOrNull { it.key == key }?.value
                if (canWriteText(rule.mode, currentValue)) {
                    currentOutput.copy(customFields = listOf(CustomTagField(key = key, value = value)))
                } else {
                    currentOutput
                }
            }
        }
    }

    private fun canWriteText(mode: PluginMetadataWriteMode, currentValue: String?): Boolean {
        return when (mode) {
            PluginMetadataWriteMode.DISABLED -> false
            PluginMetadataWriteMode.SUPPLEMENT -> currentValue.isNullOrBlank()
            PluginMetadataWriteMode.OVERWRITE -> true
        }
    }
}
