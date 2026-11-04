
package com.lonx.lyrico.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.icons.LucideIcons
import com.lonx.lyrico.ui.components.icons.PreferenceIcon
import com.lonx.lyrico.ui.components.icons.PreferenceIconTone
import com.lonx.lyrico.data.editfield.EditFieldBlock
import com.lonx.lyrico.data.editfield.EditFieldDefinition
import com.lonx.lyrico.data.editfield.EditFieldListItem
import com.lonx.lyrico.data.editfield.EditFieldRegistry
import com.lonx.lyrico.data.editfield.toEditFieldBlocks
import com.lonx.lyrico.ui.components.ItemOrderState
import com.lonx.lyrico.ui.components.ManagedChip
import com.lonx.lyrico.ui.components.base.PillButton
import com.lonx.lyrico.ui.components.base.PillButtonDefaults
import com.lonx.lyrico.ui.components.base.PillButtonSize
import com.lonx.lyrico.ui.components.base.YesNoDialog
import com.lonx.lyrico.ui.components.blur.BlurredTopBar
import com.lonx.lyrico.ui.components.blur.blurSource
import com.lonx.lyrico.ui.components.blur.rememberBarBlurBackdrop
import com.lonx.lyrico.ui.components.library.LibraryDetailPage
import com.lonx.lyrico.ui.components.scaffoldContentPadding
import com.lonx.lyrico.viewmodel.CustomTagKeyError
import com.lonx.lyrico.viewmodel.EditFieldSettingsViewModel
import com.lonx.lyrico.ui.components.sdw.SdwBottomSheet

import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.FieldSongsDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import com.lonx.lyrico.ui.components.library.libraryDetailScroll
import com.lonx.lyrico.ui.components.sdw.SdwTextInput
import top.yukonga.miuix.kmp.window.WindowDialog
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material3.MaterialTheme
@Composable
@Destination<RootGraph>(route = "edit_field_settings")
fun EditFieldSettingsScreen(
    navigator: DestinationsNavigator,
) {
    val viewModel: EditFieldSettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDeleteKey by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    var componentKey by remember { mutableStateOf<String?>(null) }
    var showComponentSheet by remember { mutableStateOf(false) }
    var pendingFieldSongs by remember { mutableStateOf<EditFieldDefinition?>(null) }
    val blocks = remember(uiState.items) { uiState.items.map { it.field }.toEditFieldBlocks() }
    val selectedComponent = blocks.firstOrNull { it.key == componentKey }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val topBarBackdrop = rememberBarBlurBackdrop()
    val lazyListState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    val savedOrder = blocks.map { it.key }
    val orderState = remember { ItemOrderState(savedOrder) }
    val displayCodes = orderState.order
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (orderState.move(from.key, to.key)) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    LaunchedEffect(savedOrder) { orderState.updateSaved(savedOrder) }

    val salt = false

    LibraryDetailPage(
        title = stringResource(R.string.edit_field_settings_title),
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
                            stringResource(R.string.edit_field_settings_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.popBackStack() }) {
                            Icon(LucideIcons.ArrowLeft, stringResource(R.string.action_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showResetDialog = true }) {
                            Icon(LucideIcons.RotateCcw, stringResource(R.string.reset_to_default))
                        }
                        IconButton(onClick = {
                            viewModel.clearInputError()
                            showAddDialog = true
                        }) {
                            Icon(
                                LucideIcons.Plus,
                                contentDescription = stringResource(R.string.custom_tag_add_key),
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = lazyListState,
            modifier = (Modifier
                .blurSource(topBarBackdrop)
                .libraryDetailScroll(scrollBehavior))
                .fillMaxHeight(),
            contentPadding = scaffoldContentPadding(
                paddingValues = paddingValues,
                bottomExtra = 12.dp,
            ),
            overscrollEffect = null,
        ) {
            item(key = "hint") {
                val hintText = stringResource(R.string.edit_field_settings_hint)
                val countText = stringResource(
                    R.string.edit_field_settings_enabled_count,
                    uiState.enabledCount,
                    uiState.items.size,
                )
                
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = hintText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = countText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            maxLines = 1,
                        )
                    }
                
            }

            items(
                items = displayCodes,
                key = { code -> code },
            ) { code ->
                val block = blocks.firstOrNull { it.key == code }
                val item = uiState.items.firstOrNull { it.field.code == block?.fields?.firstOrNull()?.code }

                ReorderableItem(state = reorderableState, key = code) {
                    if (item != null) {
                        EditFieldRow(
                            item = item,
                            iconTone = PreferenceIconTone.at(displayCodes.indexOf(code)),
                            component = block?.takeIf { it.isComposite },
                            checked = if (block?.isComposite == true) uiState.componentOverrides[block.key] ?: true else item.enabled,
                            onManageComponent = {
                                componentKey = block?.key
                                showComponentSheet = true
                            },
                            onCheckedChange = { checked ->
                                if (block?.isComposite == true) viewModel.setComponentEnabled(block.key, checked)
                                else viewModel.setEnabled(item.field.code, checked)
                            },
                            onDelete = {
                                EditFieldRegistry.customTagKeyOf(item.field.code)
                                    ?.let { pendingDeleteKey = it }
                            },
                            onOpenSongs = {
                                navigator.navigate(FieldSongsDestination(
                                    fieldCode = item.field.code,
                                    component = block?.isComposite == true,
                                ))
                            },
                            dragHandleModifier = Modifier.longPressDraggableHandle(
                                onDragStarted = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = {
                                    viewModel.setBlockOrder(orderState.order)
                                },
                            ),
                        )
                    }
                }
            }

        }
    }

    AddCustomTagDialog(
        show = showAddDialog,
        availableKeys = uiState.availableKeys,
        error = uiState.inputError,
        onAddAvailable = viewModel::addAvailableKey,
        onDismiss = { showAddDialog = false },
        onDismissFinished = viewModel::clearInputError,
        onConfirm = { typed ->
            scope.launch {
                if (viewModel.addCustomTag(typed)) showAddDialog = false
            }
        },
    )

        YesNoDialog(
            show = pendingDeleteKey != null,
            title = stringResource(R.string.custom_tag_delete_title),
            summary = stringResource(R.string.custom_tag_delete_message, pendingDeleteKey.orEmpty()),
            confirmText = stringResource(R.string.common_delete),
            onDismissRequest = { pendingDeleteKey = null },
            onConfirm = {
                pendingDeleteKey?.let(viewModel::removeCustomTag)
                pendingDeleteKey = null
            },
        )

        YesNoDialog(
            show = showResetDialog,
            title = stringResource(R.string.reset_to_default),
            summary = stringResource(R.string.edit_field_settings_reset_message),
            confirmText = stringResource(R.string.confirm),
            onDismissRequest = { showResetDialog = false },
            onConfirm = {
                viewModel.resetAll()
                orderState.resetPending()
                showResetDialog = false
            },
        )
    
    ComponentFieldsBottomSheet(
        show = showComponentSheet,
        component = selectedComponent,
        items = uiState.items,
        onEnabledChange = viewModel::setEnabled,
        onOrderChange = { codes -> componentKey?.let { viewModel.setComponentOrder(it, codes) } },
        onOpenSongs = { field ->
            pendingFieldSongs = field
            showComponentSheet = false
        },
        onDismissRequest = { showComponentSheet = false },
        onDismissFinished = {
            if (!showComponentSheet) {
                componentKey = null
                pendingFieldSongs?.let { field ->
                    navigator.navigate(FieldSongsDestination(fieldCode = field.code))
                }
                pendingFieldSongs = null
            }
        },
    )
}

private fun EditFieldDefinition?.customTagKey(): String =
    this?.let { EditFieldRegistry.customTagKeyOf(it.code) }.orEmpty()

@Composable
private fun EditFieldDefinition?.displayTitle(): String = when {
    this == null -> ""
    custom -> customTagKey()
    else -> stringResource(titleRes)
}

@Composable
private fun EditFieldRow(
    item: EditFieldListItem,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onOpenSongs: () -> Unit,
    dragHandleModifier: Modifier,
    component: EditFieldBlock? = null,
    checked: Boolean = item.enabled,
    onManageComponent: () -> Unit = {},
    inSheet: Boolean = false,
    iconTone: PreferenceIconTone = PreferenceIconTone.Tools,
) {
    var showMenu by remember { mutableStateOf(false) }
    val menuItems = buildList<Triple<String, ImageVector, () -> Unit>> {
        if (component != null) add(Triple(stringResource(R.string.edit_field_manage_members), LucideIcons.Settings, onManageComponent))
        add(Triple(stringResource(R.string.edit_field_view_songs), LucideIcons.ListMusic, onOpenSongs))
        if (item.field.custom) add(Triple(stringResource(R.string.common_delete), LucideIcons.Trash, onDelete))
    }

    val content: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth().then(dragHandleModifier)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PreferenceIcon(
                imageVector = if (component?.isComposite == true) LucideIcons.Volume2 else item.field.settingsIcon(),
                tone = iconTone,
                modifier = Modifier,
            )
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = component?.title() ?: item.field.displayTitle(),
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (component != null || item.field.custom) {
                    FieldTagChip(stringResource(
                        if (component != null) R.string.edit_field_component_short_tag
                        else R.string.edit_field_custom_short_tag
                    ))
                }
            }
            Box {
                PillButton(
                    text = stringResource(R.string.cd_more_actions),
                    showText = false,
                    style = PillButtonDefaults.style(PillButtonSize.Small),
                    onClick = { showMenu = true },
                    leading = { Icon(LucideIcons.EllipsisVertical, stringResource(R.string.cd_more_actions), Modifier.size(20.dp)) },
                )
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    menuItems.forEachIndexed { index, (label, icon, action) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            leadingIcon = {
                                PreferenceIcon(
                                    icon,
                                    if (icon == LucideIcons.Trash) PreferenceIconTone.Danger else PreferenceIconTone.at(index),
                                    Modifier.size(24.dp),
                                )
                            },
                            onClick = {
                                showMenu = false
                                action()
                            },
                        )
                    }
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
    if (inSheet) content() else Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
    ) { content() }
}

@Composable
private fun EditFieldBlock.title(): String = stringResource(
    if (isComposite) R.string.group_replay_gain else fields.first().titleRes
)

private fun EditFieldDefinition.settingsIcon(): ImageVector = when (code) {
    "picture" -> LucideIcons.Image
    "rating" -> LucideIcons.Star
    "title" -> LucideIcons.Music
    "artist", "album_artist" -> LucideIcons.Users
    "album", "disc_number" -> LucideIcons.Disc
    "date" -> LucideIcons.Calendar
    "language" -> LucideIcons.Languages
    "genre" -> LucideIcons.Tags
    "track_number" -> LucideIcons.ListMusic
    "lyricist" -> LucideIcons.NotebookPen
    "composer" -> LucideIcons.Music
    "copyright" -> LucideIcons.Copyright
    "comment" -> LucideIcons.MessageSquare
    "track_gain", "album_gain", "reference_loudness" -> LucideIcons.Volume2
    "track_peak", "album_peak" -> LucideIcons.Activity
    "lyrics" -> LucideIcons.Captions
    "lyrics_offset" -> LucideIcons.ArrowDownUp
    else -> LucideIcons.Tags
}

@Composable
private fun ComponentFieldsBottomSheet(
    show: Boolean,
    component: EditFieldBlock?,
    items: List<EditFieldListItem>,
    onEnabledChange: (String, Boolean) -> Unit,
    onOrderChange: (List<String>) -> Unit,
    onOpenSongs: (EditFieldDefinition) -> Unit,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {

    SdwBottomSheet(
        show = show,
        title = component?.title().orEmpty(),
        enableNestedScroll = false,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
    ) {
        ComponentFieldsSheetContent(
            component = component,
            items = items,
            onEnabledChange = onEnabledChange,
            onOrderChange = onOrderChange,
            onOpenSongs = onOpenSongs,
            salt = false,
        )
    }
}

@Composable
private fun ComponentFieldsSheetContent(
    component: EditFieldBlock?,
    items: List<EditFieldListItem>,
    onEnabledChange: (String, Boolean) -> Unit,
    onOrderChange: (List<String>) -> Unit,
    onOpenSongs: (EditFieldDefinition) -> Unit,
    salt: Boolean,
) {
    val savedCodes = component?.fields?.map { it.code }.orEmpty()
    val orderState = remember(component?.key) { ItemOrderState(savedCodes) }
    val displayCodes = orderState.order
    val latestOnOrderChange by rememberUpdatedState(onOrderChange)
    val listState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        if (orderState.move(from.key, to.key)) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    LaunchedEffect(savedCodes) { orderState.updateSaved(savedCodes) }
    Column(
        Modifier
            .fillMaxWidth()
            .then(Modifier.padding(bottom = 32.dp))
    ) {
        Text(
            stringResource(R.string.edit_field_component_hint),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        val listContent: @Composable () -> Unit = {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                overscrollEffect = null,
            ) {
                items(displayCodes, key = { it }) { code ->
                    val item = items.firstOrNull { it.field.code == code }
                    ReorderableItem(reorderState, key = code) {
                        if (item != null) EditFieldRow(
                            item = item,
                            iconTone = PreferenceIconTone.at(displayCodes.indexOf(code)),
                            inSheet = true,
                            onCheckedChange = { onEnabledChange(code, it) },
                            onDelete = {},
                            onOpenSongs = { onOpenSongs(item.field) },
                            dragHandleModifier = Modifier.longPressDraggableHandle(
                                onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                onDragStopped = { latestOnOrderChange(orderState.order) },
                            ),
                        )
                    }
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) { listContent() }
    }
}

/** 字段行上的小标记，用来把自定义标签和内置字段区分开。 */
@Composable
private fun FieldTagChip(text: String) {

    Text(
        text = text,
        fontSize = MaterialTheme.typography.bodySmall.fontSize,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCustomTagDialog(
    show: Boolean,
    availableKeys: List<String>,
    error: CustomTagKeyError?,
    onAddAvailable: (String) -> Unit,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val inputState = remember(show) { TextFieldState("") }
    val typed = inputState.text.toString()

    WindowDialog(
        show = show,
        title = stringResource(R.string.custom_tag_add_key),
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(
                    if (availableKeys.isEmpty()) {
                        R.string.custom_tag_library_empty
                    } else {
                        R.string.custom_tag_library_section
                    }
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
            )

            if (availableKeys.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    availableKeys.forEach { key ->
                        ManagedChip(text = key, onClick = { onAddAvailable(key) })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            SdwTextInput(
                state = inputState,
                label = stringResource(R.string.custom_tag_key),
                modifier = Modifier.fillMaxWidth(),
                lineLimits = TextFieldLineLimits.MultiLine(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = when (error) {
                    null -> stringResource(R.string.custom_tag_key_hint)
                    CustomTagKeyError.EMPTY -> stringResource(R.string.invalid_empty_input)
                    CustomTagKeyError.INVALID -> stringResource(R.string.custom_tag_key_invalid)
                    CustomTagKeyError.DUPLICATE -> stringResource(R.string.duplicate_item)
                },
                color = if (error == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(Modifier.width(20.dp))
                TextButton(
                    onClick = { onConfirm(typed) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}
