package com.lonx.lyrico.data.model.plugin

import com.lonx.lyrico.data.model.ConversionMode
import kotlinx.serialization.Serializable

@Serializable
data class PluginFieldProcessConfig(
    val pluginId: String,
    val fieldRules: Map<String, FieldProcessRule> = emptyMap()
)

@Serializable
data class PluginFieldProcessConfigStore(
    val configs: Map<String, PluginFieldProcessConfig> = emptyMap()
)

@Serializable
data class FieldProcessRule(
    val scriptConversion: FieldScriptConversionMode = FieldScriptConversionMode.FOLLOW_GLOBAL,
    val trim: FollowGlobalBooleanMode = FollowGlobalBooleanMode.FOLLOW_GLOBAL,
    val normalizeWhitespace: FollowGlobalBooleanMode = FollowGlobalBooleanMode.FOLLOW_GLOBAL,
    val removeEmptyLines: FollowGlobalBooleanMode = FollowGlobalBooleanMode.FOLLOW_GLOBAL
)

@Serializable
enum class FollowGlobalBooleanMode {
    FOLLOW_GLOBAL,
    ENABLED,
    DISABLED
}

@Serializable
enum class FieldScriptConversionMode {
    FOLLOW_GLOBAL,
    DISABLED,
    SIMPLIFIED,
    TRADITIONAL
}

enum class PluginFieldValueType {
    TEXT,
    MULTILINE_TEXT,
    PERSON_LIST,
    NUMBER,
    DATE,
    URL,
    IMAGE_URL,
    IDENTIFIER,
    LYRICS,
    RAW
}

data class GlobalFieldProcessSettings(
    val scriptConversion: ConversionMode = ConversionMode.NONE,
    val trim: Boolean = false,
    val normalizeWhitespace: Boolean = false,
    val removeEmptyLines: Boolean = true
)

data class ResolvedFieldProcessRule(
    val scriptConversion: ConversionMode,
    val trim: Boolean,
    val normalizeWhitespace: Boolean,
    val removeEmptyLines: Boolean
)

fun defaultPluginFieldProcessConfig(pluginId: String): PluginFieldProcessConfig {
    return PluginFieldProcessConfig(pluginId = pluginId)
}

fun resolveFieldProcessRule(
    config: PluginFieldProcessConfig,
    fieldKey: String
): FieldProcessRule {
    return config.fieldRules[fieldKey] ?: FieldProcessRule()
}

fun resolveFieldProcessRule(
    global: GlobalFieldProcessSettings,
    rule: FieldProcessRule
): ResolvedFieldProcessRule {
    return ResolvedFieldProcessRule(
        scriptConversion = rule.scriptConversion.resolve(global.scriptConversion),
        trim = rule.trim.resolve(global.trim),
        normalizeWhitespace = rule.normalizeWhitespace.resolve(global.normalizeWhitespace),
        removeEmptyLines = rule.removeEmptyLines.resolve(global.removeEmptyLines)
    )
}

fun FollowGlobalBooleanMode.resolve(globalValue: Boolean): Boolean {
    return when (this) {
        FollowGlobalBooleanMode.FOLLOW_GLOBAL -> globalValue
        FollowGlobalBooleanMode.ENABLED -> true
        FollowGlobalBooleanMode.DISABLED -> false
    }
}

fun FieldScriptConversionMode.resolve(globalValue: ConversionMode): ConversionMode {
    return when (this) {
        FieldScriptConversionMode.FOLLOW_GLOBAL -> globalValue
        FieldScriptConversionMode.DISABLED -> ConversionMode.NONE
        FieldScriptConversionMode.SIMPLIFIED -> ConversionMode.TRADITIONAL_TO_SIMPLIFIED
        FieldScriptConversionMode.TRADITIONAL -> ConversionMode.SIMPLIFIED_TO_TRADITIONAL
    }
}

fun PluginMetadataFieldTarget.valueType(): PluginFieldValueType {
    return when (this) {
        PluginMetadataFieldTarget.TITLE,
        PluginMetadataFieldTarget.ALBUM,
        PluginMetadataFieldTarget.GENRE,
        PluginMetadataFieldTarget.COMMENT,
        PluginMetadataFieldTarget.COPYRIGHT,
        PluginMetadataFieldTarget.LANGUAGE,
        PluginMetadataFieldTarget.CUSTOM -> PluginFieldValueType.TEXT
        PluginMetadataFieldTarget.ARTIST,
        PluginMetadataFieldTarget.ALBUM_ARTIST,
        PluginMetadataFieldTarget.COMPOSER,
        PluginMetadataFieldTarget.LYRICIST -> PluginFieldValueType.PERSON_LIST
        PluginMetadataFieldTarget.LYRICS -> PluginFieldValueType.LYRICS
        PluginMetadataFieldTarget.TRACK_NUMBER,
        PluginMetadataFieldTarget.DISC_NUMBER,
        PluginMetadataFieldTarget.RATING,
        PluginMetadataFieldTarget.REPLAY_GAIN_TRACK_GAIN,
        PluginMetadataFieldTarget.REPLAY_GAIN_TRACK_PEAK,
        PluginMetadataFieldTarget.REPLAY_GAIN_ALBUM_GAIN,
        PluginMetadataFieldTarget.REPLAY_GAIN_ALBUM_PEAK,
        PluginMetadataFieldTarget.REPLAY_GAIN_REFERENCE_LOUDNESS -> PluginFieldValueType.NUMBER
        PluginMetadataFieldTarget.DATE -> PluginFieldValueType.DATE
        PluginMetadataFieldTarget.COVER -> PluginFieldValueType.IMAGE_URL
    }
}
