
package com.lonx.lyrico.ui.components.batch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.data.model.BatchMatchConfig
import com.lonx.lyrico.data.model.PluginSelectionMode
import com.lonx.lyrico.data.model.lyrics.SearchSource
import com.lonx.lyrico.data.model.metadata.MetadataFieldTarget
import com.lonx.lyrico.data.model.metadata.MetadataWriteMode
import com.lonx.lyrico.ui.components.base.YesNoBottomSheet
import com.lonx.lyrico.ui.components.base.ConcurrencyDropdown
import com.lonx.lyrico.ui.components.icons.LucideIcons
import com.lonx.lyrico.viewmodel.BatchMatchType
import com.lonx.lyrico.ui.components.sdw.BasicComponent
import com.lonx.lyrico.ui.components.sdw.Card
import com.lonx.lyrico.ui.components.sdw.CardDefaults
import com.lonx.lyrico.ui.components.sdw.Checkbox
import com.lonx.lyrico.ui.components.sdw.ExpressiveSwitch as Switch
import com.lonx.lyrico.ui.components.sdw.RadioButtonPreference
import com.lonx.lyrico.ui.components.sdw.SdwDropdownRow
import androidx.compose.material3.Text
import com.lonx.lyrico.ui.components.sdw.CheckboxPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchMatchConfigBottomSheet(
    show: Boolean,
    matchType: BatchMatchType,
    initialConfig: BatchMatchConfig,
    visibleTargets: List<MetadataFieldTarget>,
    sources: List<SearchSource>,
    onDismissRequest: (BatchMatchConfig) -> Unit,
    onConfirm: (BatchMatchConfig) -> Unit
) {
    val singleTarget = matchType.targets.singleOrNull()

    var config by remember(show, initialConfig, singleTarget) {
        mutableStateOf(
            if (singleTarget != null &&
                (initialConfig.targetModes[singleTarget] ?: MetadataWriteMode.DISABLED) ==
                MetadataWriteMode.DISABLED
            ) {
                initialConfig.copy(
                    targetModes = initialConfig.targetModes +
                        (singleTarget to MetadataWriteMode.SUPPLEMENT)
                )
            } else {
                initialConfig
            }
        )
    }

    val targets = visibleTargets.filter { it in matchType.targets }

    fun updateTarget(
        target: MetadataFieldTarget,
        isSelected: Boolean,
        mode: MetadataWriteMode
    ) {
        val currentMap = config.targetModes.toMutableMap()
        currentMap[target] = if (isSelected) {
            mode.takeIf { it != MetadataWriteMode.DISABLED }
                ?: MetadataWriteMode.SUPPLEMENT
        } else {
            MetadataWriteMode.DISABLED
        }
        config = config.copy(targetModes = currentMap)
    }

    YesNoBottomSheet(
        show = show,
        title = stringResource(
            when (matchType) {
                BatchMatchType.METADATA -> R.string.batch_match_metadata_config_title
                BatchMatchType.LYRICS -> R.string.batch_match_lyrics_config_title
                BatchMatchType.COVER -> R.string.batch_match_cover_config_title
            }
        ),
        enableNestedScroll = false,
        scrollContent = false,
        content = {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                // 匹配插件：自动插件 / 自定义插件（指定单个插件）
                Card(
                    modifier = Modifier.padding(bottom = 12.dp),
                    colors = CardDefaults.defaultColors(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    )
                ) {
                    Text(
                        text = stringResource(R.string.batch_match_plugin_selection),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(
                            start = 12.dp,
                            top = 12.dp,
                            end = 12.dp,
                            bottom = 4.dp
                        )
                    )
                    RadioButtonPreference(
                        title = stringResource(R.string.batch_match_plugin_auto),
                        summary = stringResource(R.string.batch_match_plugin_auto_summary),
                        selected = config.pluginMode == PluginSelectionMode.AUTO,
                        onClick = {
                            config = config.copy(pluginMode = PluginSelectionMode.AUTO)
                        },
                        insideMargin = PaddingValues(12.dp)
                    )
                    RadioButtonPreference(
                        title = stringResource(R.string.batch_match_plugin_custom),
                        summary = stringResource(R.string.batch_match_plugin_custom_summary),
                        selected = config.pluginMode == PluginSelectionMode.SPECIFIC,
                        enabled = sources.isNotEmpty(),
                        onClick = {
                            val sourceIds = sources.map { it.id }
                            val current = config.selectedSourceIds[matchType.name]
                            val firstId = sources.firstOrNull()?.id
                            val nextSelected = if (firstId != null &&
                                (current == null || current !in sourceIds)
                            ) {
                                config.selectedSourceIds + (matchType.name to firstId)
                            } else {
                                config.selectedSourceIds
                            }
                            config = config.copy(
                                pluginMode = PluginSelectionMode.SPECIFIC,
                                selectedSourceIds = nextSelected
                            )
                        },
                        insideMargin = PaddingValues(12.dp)
                    )
                    if (config.pluginMode == PluginSelectionMode.SPECIFIC && sources.isNotEmpty()) {
                        val sourceIds = sources.map { it.id }
                        val selectedId = config.selectedSourceIds[matchType.name]
                            .takeIf { it in sourceIds } ?: sources.first().id
                        val selectedIndex = sources.indexOfFirst { it.id == selectedId }
                            .coerceAtLeast(0)
                        SdwDropdownRow(
                            title = stringResource(R.string.batch_match_plugin_source),
                            labels = sources.map { it.name },
                            selectedIndex = selectedIndex,
                            icon = LucideIcons.Plug,
                            onSelectedIndexChange = { index ->
                                config = config.copy(
                                    selectedSourceIds = config.selectedSourceIds +
                                        (matchType.name to sources[index].id)
                                )
                            }
                        )
                    } else if (config.pluginMode == PluginSelectionMode.SPECIFIC) {
                        Text(
                            text = stringResource(R.string.batch_match_plugin_no_source),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 8.dp
                            )
                        )
                    }
                }
                if (singleTarget == null) {
                    Card(
                        modifier = Modifier.padding(bottom = 12.dp),
                        colors = CardDefaults.defaultColors(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        )
                    ) {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 250.dp)
                        ) {
                            items(targets, key = { it.name }) { target ->
                                val mode = config.targetModes[target] ?: MetadataWriteMode.DISABLED
                                val isSelected = mode != MetadataWriteMode.DISABLED
                                val effectiveMode = if (isSelected) {
                                    mode
                                } else {
                                    MetadataWriteMode.SUPPLEMENT
                                }

                                BatchMatchTargetItem(
                                    target = target,
                                    isSelected = isSelected,
                                    mode = effectiveMode,
                                    onCheckedChange = { checked ->
                                        updateTarget(target, checked, effectiveMode)
                                    },
                                    onModeToggle = {
                                        updateTarget(
                                            target = target,
                                            isSelected = isSelected,
                                            mode = if (effectiveMode == MetadataWriteMode.OVERWRITE) {
                                                MetadataWriteMode.SUPPLEMENT
                                            } else {
                                                MetadataWriteMode.OVERWRITE
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
                Card(
                    colors = CardDefaults.defaultColors(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                    )
                ) {
                    if (singleTarget != null) {
                        // 单字段的匹配类型（如歌词 / 封面）打开即匹配，不需要勾选字段，
                        // 这一项只负责切换「覆盖」还是「补充」
                        CheckboxPreference(
                            title = stringResource(R.string.batch_match_overwrite_mode),
                            summary = stringResource(R.string.batch_match_overwrite_mode_summary),
                            checked = config.targetModes[singleTarget] == MetadataWriteMode.OVERWRITE,
                            onCheckedChange = { checked ->
                                updateTarget(
                                    target = singleTarget,
                                    isSelected = true,
                                    mode = if (checked) {
                                        MetadataWriteMode.OVERWRITE
                                    } else {
                                        MetadataWriteMode.SUPPLEMENT
                                    }
                                )
                            },
                            insideMargin = PaddingValues(12.dp)
                        )
                    }
                    CheckboxPreference(
                        title = stringResource(R.string.batch_match_prefer_filename),
                        summary = stringResource(R.string.batch_match_prefer_filename_summary),
                        checked = config.preferFileName,
                        onCheckedChange = { checked ->
                            val updatedTargetModes = config.targetModes.toMutableMap()

                            if (checked) {
                                if (updatedTargetModes[MetadataFieldTarget.TITLE] != MetadataWriteMode.DISABLED) {
                                    updatedTargetModes[MetadataFieldTarget.TITLE] = MetadataWriteMode.OVERWRITE
                                }
                                if (updatedTargetModes[MetadataFieldTarget.ARTIST] != MetadataWriteMode.DISABLED) {
                                    updatedTargetModes[MetadataFieldTarget.ARTIST] = MetadataWriteMode.OVERWRITE
                                }
                            }

                            config = config.copy(
                                preferFileName = checked,
                                targetModes = updatedTargetModes
                            )
                        },
                        insideMargin = PaddingValues(12.dp)
                    )
                    ConcurrencyDropdown(
                        title = stringResource(R.string.batch_match_config_concurrency),
                        summary = stringResource(R.string.search_limit_tip),
                        value = config.concurrency,
                        onChange = { config = config.copy(concurrency = it) },
                    )
                }
            }
        },
        onDismissRequest = { onDismissRequest(config) },
        onConfirm = {
            onConfirm(config)
            onDismissRequest(config)
        }
    )
}

@Composable
private fun BatchMatchTargetItem(
    target: MetadataFieldTarget,
    isSelected: Boolean,
    mode: MetadataWriteMode,
    onCheckedChange: (Boolean) -> Unit,
    onModeToggle: () -> Unit
) {

    BasicComponent(
        insideMargin = PaddingValues(horizontal = 12.dp),
        modifier = Modifier.fillMaxWidth(),
        startAction = {
            Checkbox(
                state = if (isSelected) ToggleableState.On else ToggleableState.Off,
                onClick = { onCheckedChange(!isSelected) }
            )
        },
        onClick = {
            onCheckedChange(!isSelected)
        },
        endActions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (isSelected) {
                    Text(
                        text = stringResource(mode.labelRes),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = mode == MetadataWriteMode.OVERWRITE,
                    onCheckedChange = { onModeToggle() },
                    enabled = isSelected
                )
            }
        }
    ) {
        Text(
            text = stringResource(target.labelRes),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
