package com.lonx.lyrico.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

@Serializable
data class CustomTagSettings(
    /** 在单曲编辑页和批量编辑页显示的自定义标签键，顺序即字段显示顺序。 */
    val visibleKeys: List<String> = emptyList(),
)

/**
 * 自定义标签的显示配置。
 *
 * 这里只保存"哪些键要显示"；音乐库里实际存在哪些键由 [CustomTagKeyRepository] 提供，
 * 两者取并集才是管理页展示的完整列表。
 */
class CustomTagSettingsRepository(
    private val context: Context,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    },
) {

    val settingsFlow: Flow<CustomTagSettings> =
        context.settingsDataStore.data.map { it.readSettings() }

    /** 追加若干标签键，已存在的会被忽略；顺序即编辑页的字段顺序。 */
    suspend fun addVisibleKeys(keys: Collection<String>) {
        val normalized = keys.mapNotNull(::normalizeKey).distinct()
        if (normalized.isEmpty()) return

        context.settingsDataStore.edit { preferences ->
            val current = preferences.readSettings()
            preferences[CUSTOM_TAG_SETTINGS] =
                encodeSettings(
                    current.copy(visibleKeys = (current.visibleKeys + normalized).distinct())
                )
        }
    }

    suspend fun removeVisibleKey(key: String) {
        val normalizedKey = normalizeKey(key) ?: return

        context.settingsDataStore.edit { preferences ->
            val current = preferences.readSettings()
            preferences[CUSTOM_TAG_SETTINGS] =
                encodeSettings(current.copy(visibleKeys = current.visibleKeys - normalizedKey))
        }
    }

    suspend fun reset() {
        context.settingsDataStore.edit { preferences ->
            preferences.remove(CUSTOM_TAG_SETTINGS)
        }
    }

    private fun Preferences.readSettings(): CustomTagSettings =
        this[CUSTOM_TAG_SETTINGS]
            ?.let { raw -> runCatching { json.decodeFromString<CustomTagSettings>(raw) }.getOrNull() }
            ?.sanitize()
            ?: CustomTagSettings()

    private fun encodeSettings(settings: CustomTagSettings): String =
        json.encodeToString(settings.sanitize())

    private fun CustomTagSettings.sanitize(): CustomTagSettings =
        copy(visibleKeys = visibleKeys.mapNotNull(::normalizeKey).distinct())

    companion object {
        const val MAX_KEY_LENGTH = 64

        /** 归一化标签键：去空白、转大写；不合法时返回 null。 */
        fun normalizeKey(input: String): String? {
            val key = input.trim()

            return when {
                key.isBlank() -> null
                key.length > MAX_KEY_LENGTH -> null
                key.any { it == '\n' || it == '\r' } -> null
                else -> key.uppercase(Locale.ROOT)
            }
        }

        private val CUSTOM_TAG_SETTINGS =
            stringPreferencesKey("custom_tag_settings")
    }
}
