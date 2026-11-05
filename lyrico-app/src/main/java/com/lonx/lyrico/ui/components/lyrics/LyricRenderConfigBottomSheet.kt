
package com.lonx.lyrico.ui.components.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.base.ActionBottomSheet
import com.lonx.lyrico.ui.components.icons.PreferenceIconTone
import com.lonx.lyrico.ui.components.icons.PreferenceIcon
import com.lonx.lyrico.ui.components.icons.LucideIcons
import com.lonx.lyrico.data.model.ConversionMode
import com.lonx.lyrico.data.model.lyrics.LyricFormat
import com.lonx.lyrico.data.model.lyrics.LyricRenderConfig
import com.lonx.lyrico.data.model.lyrics.LyricLineTrack
import com.lonx.lyrico.data.model.lyrics.visibleLyricLineTracks
import com.lonx.lyrico.ui.components.sdw.SdwBottomSheet

import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import androidx.compose.material3.MaterialTheme

@Composable
fun LyricRenderConfigBottomSheet(
    show: Boolean,
    config: LyricRenderConfig?,
    onDismissRequest: () -> Unit,
    onLyricFormatChange: (LyricFormat) -> Unit,
    onRomaEnabledChange: (Boolean) -> Unit,
    onLineOrderChange: (List<LyricLineTrack>) -> Unit,
    onTranslationEnabledChange: (Boolean) -> Unit,
    onOnlyTranslationIfAvailableChange: (Boolean) -> Unit,
    onRemoveEmptyLinesChange: (Boolean) -> Unit,
    onConversionModeChange: (ConversionMode) -> Unit
) {
    val salt = false
    var showLineOrder by remember { mutableStateOf(false) }
    LaunchedEffect(show) {
        if (!show) showLineOrder = false
    }
    val context = LocalContext.current
    val visibleTracks = config?.let {
        visibleLyricLineTracks(it.showRomanization, it.showTranslation, it.onlyTranslationIfAvailable)
    }.orEmpty()
    val lineOrderSummary = config?.normalizedLineOrder.orEmpty()
        .filter { it in visibleTracks }
        .joinToString(" / ") { context.getString(it.labelRes) }
    ActionBottomSheet(
        show = showLineOrder && show,
        title = stringResource(R.string.lyric_line_order),
        onDismissRequest = { showLineOrder = false },
        scrollContent = false,
        content = {
            config?.let { lyricConfig ->
                LyricLineOrderBottomSheetContent(
                    lineOrder = lyricConfig.normalizedLineOrder,
                    visibleTracks = visibleTracks,
                    onLineOrderChange = onLineOrderChange,
                )
            }
        },
    )

    SdwBottomSheet(
        show = show,
        scrollContent = true,
        onDismissRequest = onDismissRequest
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.defaultColors(
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                config?.let { lyricConfig ->
                    val lyricFormatItems = LyricFormat.entries.map {
                        stringResource(it.labelRes)
                    }
                    val selectedLyricFormatIndex = LyricFormat.entries
                        .indexOf(lyricConfig.format)
                        .coerceAtLeast(0)
                    val conversionModeItems = ConversionMode.entries.map {
                        stringResource(it.labelRes)
                    }
                    val selectedConversionModeIndex = ConversionMode.entries
                        .indexOf(lyricConfig.conversionMode)
                        .coerceAtLeast(0)

                    WindowDropdownPreference(
                        startAction = { PreferenceIcon(LucideIcons.FileMusic, PreferenceIconTone.Appearance) },
                        title = stringResource(R.string.lyric_mode),
                        items = lyricFormatItems,
                        selectedIndex = selectedLyricFormatIndex,
                        onSelectedIndexChange = { index ->
                            onLyricFormatChange(LyricFormat.entries[index])
                        }
                    )
                    SwitchPreference(
                        startAction = { PreferenceIcon(LucideIcons.Type, PreferenceIconTone.Tools) },
                        title = stringResource(R.string.roma),
                        summary = stringResource(R.string.roma_hint),
                        checked = lyricConfig.showRomanization,
                        onCheckedChange = onRomaEnabledChange
                    )
                    SwitchPreference(
                        startAction = { PreferenceIcon(LucideIcons.Languages, PreferenceIconTone.Library) },
                        title = stringResource(R.string.translation),
                        summary = stringResource(R.string.translation_hint),
                        checked = lyricConfig.showTranslation,
                        onCheckedChange = onTranslationEnabledChange
                    )
                    AnimatedVisibility(visible = lyricConfig.showTranslation) {
                        SwitchPreference(
                            startAction = { PreferenceIcon(LucideIcons.Captions, PreferenceIconTone.Lyrics, enabled = lyricConfig.showTranslation) },
                            title = stringResource(R.string.only_translation_if_available),
                            summary = stringResource(
                                R.string.only_translation_if_available_hint
                            ),
                            enabled = lyricConfig.showTranslation,
                            checked = lyricConfig.onlyTranslationIfAvailable,
                            onCheckedChange = onOnlyTranslationIfAvailableChange
                        )
                    }
                    SwitchPreference(
                        startAction = { PreferenceIcon(LucideIcons.RemoveLines, PreferenceIconTone.Pink) },
                        title = stringResource(R.string.remove_empty_lines),
                        summary = stringResource(R.string.remove_empty_lines_hint),
                        checked = lyricConfig.removeEmptyLines,
                        onCheckedChange = onRemoveEmptyLinesChange
                    )
                    WindowDropdownPreference(
                        startAction = { PreferenceIcon(LucideIcons.ALargeSmall, PreferenceIconTone.Teal) },
                        title = stringResource(R.string.conversion_mode),
                        items = conversionModeItems,
                        selectedIndex = selectedConversionModeIndex,
                        onSelectedIndexChange = { index ->
                            onConversionModeChange(ConversionMode.entries[index])
                        }
                    )
                    ArrowPreference(
                        startAction = { PreferenceIcon(LucideIcons.ArrowDownUp, PreferenceIconTone.Lyrics) },
                        title = stringResource(R.string.lyric_line_order),
                        summary = lineOrderSummary,
                        holdDownState = showLineOrder,
                        onClick = { showLineOrder = true },
                    )
                }
            }
        }
    }
}
