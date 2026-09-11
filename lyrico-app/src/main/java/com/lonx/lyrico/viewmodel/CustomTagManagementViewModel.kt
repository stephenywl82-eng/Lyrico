package com.lonx.lyrico.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lonx.lyrico.data.model.dao.CustomTagKeyCount
import com.lonx.lyrico.data.repository.CustomTagKeyRepository
import com.lonx.lyrico.data.repository.CustomTagSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 手动添加标签键时的校验失败原因。 */
enum class CustomTagKeyError {
    EMPTY,
    INVALID,
    DUPLICATE,
}

data class CustomTagManagementUiState(
    /** 会显示在单曲编辑页和批量编辑页的标签，顺序即字段顺序。 */
    val visibleKeys: List<String> = emptyList(),
    /** 音乐库里扫到、但还没加入列表的键，供添加时挑选。 */
    val availableKeys: List<String> = emptyList(),
    val inputError: CustomTagKeyError? = null,
)

class CustomTagManagementViewModel(
    private val customTagSettingsRepository: CustomTagSettingsRepository,
    private val customTagKeyRepository: CustomTagKeyRepository,
) : ViewModel() {

    private val inputError = MutableStateFlow<CustomTagKeyError?>(null)

    val uiState: StateFlow<CustomTagManagementUiState> =
        combine(
            customTagSettingsRepository.settingsFlow,
            customTagKeyRepository.observeKeyCounts(),
            inputError,
        ) { settings, counts, error ->
            val visibleKeys = settings.visibleKeys
            val visible = visibleKeys.toSet()

            CustomTagManagementUiState(
                visibleKeys = visibleKeys,
                availableKeys = counts
                    .filter { it.key !in visible }
                    .sortedWith(
                        compareByDescending<CustomTagKeyCount> { it.songCount }
                            .thenBy { it.key.lowercase() }
                    )
                    .map { it.key },
                inputError = error,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CustomTagManagementUiState(),
        )

    /** 添加音乐库里扫到的键。 */
    fun addAvailableKey(key: String) {
        viewModelScope.launch {
            customTagSettingsRepository.addVisibleKeys(listOf(key))
        }
    }

    /** 手动输入一个键名，返回 true 表示校验通过并已写入。 */
    suspend fun addKey(input: String): Boolean {
        val normalized = CustomTagSettingsRepository.normalizeKey(input)
        if (normalized == null) {
            inputError.value =
                if (input.isBlank()) CustomTagKeyError.EMPTY else CustomTagKeyError.INVALID
            return false
        }
        if (normalized in customTagSettingsRepository.settingsFlow.first().visibleKeys) {
            inputError.value = CustomTagKeyError.DUPLICATE
            return false
        }

        inputError.value = null
        customTagSettingsRepository.addVisibleKeys(listOf(normalized))
        return true
    }

    fun removeKey(key: String) {
        viewModelScope.launch {
            customTagSettingsRepository.removeVisibleKey(key)
        }
    }

    fun resetVisibleKeys() {
        viewModelScope.launch {
            customTagSettingsRepository.reset()
        }
    }

    fun clearInputError() {
        inputError.update { null }
    }
}
