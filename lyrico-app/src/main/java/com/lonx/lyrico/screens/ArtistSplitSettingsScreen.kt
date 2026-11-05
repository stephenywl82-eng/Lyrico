@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.lonx.lyrico.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lonx.lyrico.ui.components.blur.BlurredTopBar
import com.lonx.lyrico.ui.components.blur.blurSource
import com.lonx.lyrico.ui.components.blur.rememberBarBlurBackdrop
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.icons.LucideIcons
import com.lonx.lyrico.ui.components.icons.PreferenceIcon
import com.lonx.lyrico.ui.components.icons.PreferenceIconTone
import com.lonx.lyrico.ui.components.sdw.SdwTextInput
import com.lonx.lyrico.data.model.artist.ArtistSplitDefaults
import com.lonx.lyrico.data.model.artist.CustomArtistSeparator
import com.lonx.lyrico.data.model.artist.CustomNoSplitArtist
import com.lonx.lyrico.ui.components.library.LibraryDetailPage
import com.lonx.lyrico.ui.components.library.libraryDetailScroll
import com.lonx.lyrico.ui.components.scaffoldContentPadding
import com.lonx.lyrico.viewmodel.ArtistSplitSettingsViewModel
import com.lonx.lyrico.viewmodel.ArtistSplitValidationError
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import com.lonx.lyrico.ui.components.sdw.WindowDialog

@Composable
@OptIn(ExperimentalLayoutApi::class)
@Destination<RootGraph>(route = "artist_split_settings")
fun ArtistSplitSettingsScreen(
    navigator: DestinationsNavigator
) {
    val viewModel: ArtistSplitSettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var editingSeparator by remember { mutableStateOf<CustomArtistSeparator?>(null) }
    var addingSeparator by remember { mutableStateOf(false) }
    var editingNoSplitArtist by remember { mutableStateOf<CustomNoSplitArtist?>(null) }
    var addingNoSplitArtist by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PendingRuleDelete?>(null) }

    uiState.error?.let { error ->
        val message = when (error) {
            ArtistSplitValidationError.EMPTY -> stringResource(R.string.invalid_empty_input)
            ArtistSplitValidationError.DUPLICATE -> stringResource(R.string.duplicate_item)
            ArtistSplitValidationError.DUPLICATE_BUILTIN -> stringResource(R.string.duplicate_builtin_item)
            ArtistSplitValidationError.REBUILD_FAILED -> stringResource(R.string.artist_index_rebuild_failed)
        }
        LaunchedEffect(error) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    if (uiState.rebuildCompleted) {
        val message = stringResource(R.string.artist_index_rebuild_complete)
        LaunchedEffect(Unit) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearRebuildCompleted()
        }
    }

    val salt = false
    val separatorChips = ArtistSplitDefaults.BUILTIN_SEPARATORS
        .filterNot { it.id in uiState.config.hiddenBuiltinSeparatorIds }
        .map { separator ->
            ArtistSplitChipItem(
                id = separator.id,
                text = separator.displayName,
                checked = uiState.config.builtinSeparatorOverrides[separator.id]
                    ?: separator.defaultEnabled,
                isBuiltin = true
            )
        } + uiState.config.customSeparators.map { separator ->
            ArtistSplitChipItem(
                id = separator.id,
                text = separator.value,
                checked = separator.enabled,
                isBuiltin = false
            )
        }

    val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val topBarBackdrop = rememberBarBlurBackdrop()
    LibraryDetailPage(
        title = stringResource(R.string.artist_split_settings_title),
        onBack = { navigator.popBackStack() },
        miuixTopBar = {
            BlurredTopBar(backdrop = topBarBackdrop) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                    windowInsets = WindowInsets(0.dp),
                    title = {
                        Text(
                            text = stringResource(R.string.artist_split_settings_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.popBackStack() }) {
                            Icon(
                                LucideIcons.ArrowLeft,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    },
                    scrollBehavior = topAppBarScrollBehavior
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = (Modifier.blurSource(topBarBackdrop))
                .libraryDetailScroll(topAppBarScrollBehavior)
                .fillMaxHeight(),
            contentPadding = scaffoldContentPadding(
                paddingValues = paddingValues,
                bottomExtra = 12.dp
            ),
            overscrollEffect = null
        ) {

                item(key = "config") {
                    SectionTitle(text = stringResource(R.string.artist_split_general_section))
                    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                        ToggleRow(
                            icon = LucideIcons.Split,
                            tone = PreferenceIconTone.Appearance,
                            title = stringResource(R.string.artist_split_enabled),
                            summary = stringResource(R.string.artist_split_enabled_summary),
                            checked = uiState.config.enabled,
                            onCheckedChange = viewModel::setEnabled
                        )
                        ArrowRow(
                            icon = LucideIcons.RefreshCw,
                            tone = PreferenceIconTone.Tools,
                            title = stringResource(
                                if (uiState.hasPendingIndexRebuild) {
                                    R.string.rebuild_artist_index_now
                                } else {
                                    R.string.rebuild_artist_index
                                }
                            ),
                            summary = when {
                                uiState.isRebuildingIndex -> stringResource(R.string.artist_index_rebuilding)
                                uiState.hasPendingIndexRebuild -> stringResource(R.string.artist_index_rebuild_pending)
                                else -> stringResource(R.string.artist_index_current)
                            },
                            onClick = viewModel::rebuildArtistIndex
                        )
                    }
                }

                item(key = "separators") {
                    SectionTitle(text = stringResource(R.string.section_artist_separators))
                    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                        ChipGrid(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            separatorChips.forEach { chip ->
                                RuleChip(
                                    text = chip.text,
                                    checked = chip.checked,
                                    onCheckedChange = {
                                        if (chip.isBuiltin) {
                                            viewModel.setBuiltinSeparatorEnabled(chip.id, it)
                                        } else {
                                            viewModel.setCustomSeparatorEnabled(chip.id, it)
                                        }
                                    },
                                    onDelete = {
                                        pendingDelete = PendingRuleDelete.Separator(chip.id, chip.text, chip.isBuiltin)
                                    }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { addingSeparator = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(LucideIcons.Plus, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.add_custom_separator),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            FilledTonalButton(
                                onClick = viewModel::resetSeparators,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(LucideIcons.RotateCcw, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.reset_to_default),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }

                item(key = "custom_no_split") {
                    SectionTitle(text = stringResource(R.string.section_no_split_artists))
                    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                        if (uiState.config.customNoSplitArtists.isNotEmpty()) {
                            ChipGrid(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                uiState.config.customNoSplitArtists.forEach { artist ->
                                    RuleChip(
                                        text = artist.name,
                                        checked = artist.enabled,
                                        onCheckedChange = {
                                            viewModel.setCustomNoSplitArtistEnabled(artist.id, it)
                                        },
                                        onDelete = {
                                            pendingDelete =
                                                PendingRuleDelete.NoSplitArtist(artist.id, artist.name)
                                        }
                                    )
                                }
                            }
                        }
                        ArrowRow(
                            icon = LucideIcons.UserPlus,
                            tone = PreferenceIconTone.Library,
                            title = stringResource(R.string.add_no_split_artist),
                            onClick = { addingNoSplitArtist = true }
                        )
                    }
                }

        }
    }

    ArtistSplitInputDialog(
        show = addingSeparator,
        title = stringResource(R.string.add_custom_separator),
        initialValue = "",
        hint = stringResource(R.string.separator_input_hint),
        showDelete = false,
        onDismiss = { addingSeparator = false },
        onSave = {
            if (viewModel.addCustomSeparator(it)) addingSeparator = false
        }
    )

    ArtistSplitInputDialog(
        show = editingSeparator != null,
        title = stringResource(R.string.edit_custom_separator),
        initialValue = editingSeparator?.value.orEmpty(),
        hint = stringResource(R.string.separator_input_hint),
        showDelete = true,
        onDismiss = { editingSeparator = null },
        onDelete = {
            editingSeparator?.let { viewModel.removeCustomSeparator(it.id) }
            editingSeparator = null
        },
        onSave = { value ->
            val separator = editingSeparator ?: return@ArtistSplitInputDialog
            if (viewModel.updateCustomSeparator(separator.id, value)) editingSeparator = null
        }
    )

    ArtistSplitInputDialog(
        show = addingNoSplitArtist,
        title = stringResource(R.string.add_no_split_artist),
        initialValue = "",
        hint = stringResource(R.string.artist_name_input_hint),
        showDelete = false,
        onDismiss = { addingNoSplitArtist = false },
        onSave = {
            if (viewModel.addCustomNoSplitArtist(it)) addingNoSplitArtist = false
        }
    )

    ArtistSplitInputDialog(
        show = editingNoSplitArtist != null,
        title = stringResource(R.string.edit_no_split_artist),
        initialValue = editingNoSplitArtist?.name.orEmpty(),
        hint = stringResource(R.string.artist_name_input_hint),
        showDelete = true,
        onDismiss = { editingNoSplitArtist = null },
        onDelete = {
            editingNoSplitArtist?.let { viewModel.removeCustomNoSplitArtist(it.id) }
            editingNoSplitArtist = null
        },
        onSave = { name ->
            val artist = editingNoSplitArtist ?: return@ArtistSplitInputDialog
            if (viewModel.updateCustomNoSplitArtist(artist.id, name)) editingNoSplitArtist = null
        }
    )

    ConfirmRuleDeleteDialog(
        pendingDelete = pendingDelete,
        onDismiss = { pendingDelete = null },
        onConfirm = { delete ->
            when (delete) {
                is PendingRuleDelete.Separator -> {
                    if (delete.isBuiltin) {
                        viewModel.removeBuiltinSeparator(delete.id)
                    } else {
                        viewModel.removeCustomSeparator(delete.id)
                    }
                }
                is PendingRuleDelete.NoSplitArtist -> {
                    viewModel.removeCustomNoSplitArtist(delete.id)
                }
            }
            pendingDelete = null
        }
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    tone: PreferenceIconTone,
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreferenceIcon(icon, tone, Modifier.size(24.dp).padding(end = 12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
        )
    }
}

@Composable
private fun ArrowRow(
    icon: ImageVector,
    tone: PreferenceIconTone,
    title: String,
    summary: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreferenceIcon(icon, tone, Modifier.size(24.dp).padding(end = 12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            LucideIcons.ArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

private data class ArtistSplitChipItem(
    val id: String,
    val text: String,
    val checked: Boolean,
    val isBuiltin: Boolean
)

private sealed interface PendingRuleDelete {
    val id: String
    val label: String

    data class Separator(
        override val id: String,
        override val label: String,
        val isBuiltin: Boolean
    ) : PendingRuleDelete

    data class NoSplitArtist(
        override val id: String,
        override val label: String
    ) : PendingRuleDelete
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGrid(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@Composable
private fun RuleChip(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val background = if (checked) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val borderColor = if (checked) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.48f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable { onCheckedChange(!checked) }
            .padding(start = 8.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(
                    if (checked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
                    }
                )
                .clickable { onCheckedChange(!checked) },
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text(
                    text = "✓",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize
                )
            }
        }
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = LucideIcons.Trash,
            contentDescription = stringResource(R.string.common_delete),
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ArtistSplitInputDialog(
    show: Boolean,
    title: String,
    initialValue: String,
    hint: String,
    showDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit = {}
) {
    val inputState = remember(show, initialValue) { TextFieldState(initialValue) }
    val text = inputState.text.toString()

    WindowDialog(
        show = show,
        title = title,
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SdwTextInput(
                state = inputState,
                hint = hint,
                lineLimits = TextFieldLineLimits.SingleLine,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                if (showDelete) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(R.string.common_delete))
                    }
                    Spacer(Modifier.width(12.dp))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(Modifier.width(12.dp))
                TextButton(
                    onClick = { onSave(text) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}

@Composable
private fun ConfirmRuleDeleteDialog(
    pendingDelete: PendingRuleDelete?,
    onDismiss: () -> Unit,
    onConfirm: (PendingRuleDelete) -> Unit
) {

    WindowDialog(
        show = pendingDelete != null,
        title = stringResource(R.string.artist_split_delete_rule_title),
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(
                    R.string.artist_split_delete_rule_message,
                    pendingDelete?.label.orEmpty()
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(Modifier.width(12.dp))
                TextButton(
                    onClick = {
                        pendingDelete?.let(onConfirm)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            }
        }
    }
}
