@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.lonx.lyrico.screens

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import com.lonx.lyrico.ui.theme.Motion
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.icons.LucideIcons
import com.lonx.lyrico.data.model.entity.SourcePluginEntity
import com.lonx.lyrico.data.model.entity.capabilities
import com.lonx.lyrico.data.model.entity.displaySourceTypes
import com.lonx.lyrico.data.model.entity.displayName
import com.lonx.lyrico.data.model.entity.isEnabledFor
import com.lonx.lyrico.data.model.entity.sortOrderFor
import com.lonx.lyrico.data.model.plugin.PluginSourceType
import com.lonx.lyrico.data.model.plugin.supportsSourceType
import com.lonx.lyrico.data.model.plugin.displaySourceTypes
import com.lonx.lyrico.plugin.source.PluginInstallCandidate
import com.lonx.lyrico.plugin.source.PluginInstallFailed
import com.lonx.lyrico.plugin.source.PluginVersionConflict
import com.lonx.lyrico.ui.components.base.YesNoBottomSheet
import com.lonx.lyrico.ui.components.base.YesNoDialog
import com.lonx.lyrico.ui.components.library.EmptyStateKind
import com.lonx.lyrico.ui.components.library.IllustratedEmptyState
import com.lonx.lyrico.ui.components.library.LibraryDetailPage
import com.lonx.lyrico.ui.components.plugin.PluginIcon
import com.lonx.lyrico.ui.components.ItemOrderState
import com.lonx.lyrico.ui.components.scaffoldTopHorizontalPadding
import com.lonx.lyrico.ui.theme.isDarkTheme
import com.lonx.lyrico.viewmodel.PluginViewModel
import com.lonx.lyrico.viewmodel.PluginMessage
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.PluginConfigDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import com.lonx.lyrico.ui.components.sdw.ExpressiveSwitch as Switch
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import com.lonx.lyrico.ui.components.base.PillButton
import com.lonx.lyrico.ui.components.base.PillButtonDefaults
import com.lonx.lyrico.ui.components.base.PillButtonSize
import com.lonx.lyrico.ui.components.blur.BlurredTopBar
import com.lonx.lyrico.ui.components.blur.blurSource
import com.lonx.lyrico.ui.components.blur.rememberBarBlurBackdrop
import com.lonx.lyrico.ui.components.library.libraryDetailScroll
import com.lonx.lyrico.ui.components.sdw.SdwTextInput
import java.io.File
import com.lonx.lyrico.ui.components.sdw.WindowDialog
@Composable
private fun PluginMessage.localizedText(): String = when (this) {
    is PluginMessage.Failure -> stringResource(R.string.plugin_action_failed, reason)
    is PluginMessage.Status -> stringResource(when (this) {
        PluginMessage.Status.PACKAGE_SCANNED -> R.string.plugin_action_scanned
        PluginMessage.Status.INSTALLED -> R.string.plugin_action_imported
        PluginMessage.Status.DELETED -> R.string.plugin_action_deleted
        PluginMessage.Status.NAME_SAVED -> R.string.plugin_action_name_saved
        PluginMessage.Status.NO_PENDING_IMPORT -> R.string.plugin_action_no_pending
        PluginMessage.Status.NO_SELECTION -> R.string.plugin_action_no_selection
        PluginMessage.Status.BUSY -> R.string.plugin_action_busy
        PluginMessage.Status.WORKING -> R.string.plugin_action_working
        PluginMessage.Status.TIMED_OUT -> R.string.plugin_action_timeout
        PluginMessage.Status.CANNOT_OPEN_FILE -> R.string.plugin_action_cannot_open
        PluginMessage.Status.NO_INSTALLABLE_PLUGIN -> R.string.plugin_action_none_installable
        PluginMessage.Status.CANNOT_DELETE_FILES -> R.string.plugin_action_remove_failed
    })
}

private enum class PluginTypeTab(
    val sourceType: PluginSourceType,
    val labelRes: Int
) {
    METADATA(PluginSourceType.METADATA, R.string.plugin_type_metadata),
    LYRICS(PluginSourceType.LYRICS, R.string.plugin_type_lyrics),
    COVER(PluginSourceType.COVER, R.string.plugin_type_cover)
}

@Composable
@Destination<RootGraph>(route = "plugin_manager")
fun PluginManagerScreen(
    navigator: DestinationsNavigator
) {
    val viewModel: PluginViewModel = koinViewModel()
    val plugins by viewModel.plugins.collectAsState()
    val localeConfiguration = androidx.compose.ui.platform.LocalConfiguration.current
    LaunchedEffect(localeConfiguration) {
        com.lonx.lyrico.plugin.i18n.PluginLocales.update(localeConfiguration)
    }
    val uiState by viewModel.uiState.collectAsState()
    val pendingImport = uiState.pendingImport
    val context: Context = LocalContext.current
    var selectedTypeTab by rememberSaveable { mutableStateOf(PluginTypeTab.METADATA) }
    var compactMode by rememberSaveable {
        mutableStateOf(
            context.getSharedPreferences(
                PLUGIN_MANAGER_PREFERENCES,
                Context.MODE_PRIVATE
            ).getBoolean(KEY_COMPACT_MODE, false)
        )
    }
    val savedPlugins = remember(plugins, selectedTypeTab) {
        plugins
            .filter {
                it.capabilities.supportsSourceType(selectedTypeTab.sourceType)
            }
            .sortedWith(
                compareBy<SourcePluginEntity> {
                    it.sortOrderFor(selectedTypeTab.sourceType)
                }.thenBy { it.displayName }
            )
    }
    val savedOrder = savedPlugins.map { it.id }
    val orderState = remember(selectedTypeTab) { ItemOrderState(savedOrder) }
    val pluginsById = savedPlugins.associateBy { it.id }
    val currentList = orderState.order.mapNotNull { pluginsById[it] }
    LaunchedEffect(savedOrder) { orderState.updateSaved(savedOrder) }
    // Long-lived pointer input callbacks must read the current state and category.
    val saveOrder by rememberUpdatedState {
        viewModel.setPluginOrder(
            orderState.order.mapNotNull { pluginsById[it] },
            selectedTypeTab.sourceType
        )
    }
    val lazyListState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current
    val reorderableLazyColumnState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (orderState.move(from.key, to.key)) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    val topAppBarScrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val topBarBackdrop = rememberBarBlurBackdrop()
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importPlugin(context, it) }
    }
    val operationMessage = uiState.message?.localizedText()
    LaunchedEffect(uiState.messageVersion) {
        operationMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    var showUninstallDialog by rememberSaveable { mutableStateOf(false) }
    var pendingUninstallPluginId by rememberSaveable { mutableStateOf<String?>(null) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var pendingRenamePluginId by rememberSaveable { mutableStateOf<String?>(null) }
    val nameState = rememberTextFieldState()
    val customNameInput = nameState.text.toString()
    var showImportPreviewSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pendingImport) {
        if (pendingImport != null) {
            showImportPreviewSheet = true
        }
    }

    val salt = false
    val toggleCompactMode: () -> Unit = {
        compactMode = !compactMode
        context.getSharedPreferences(
            PLUGIN_MANAGER_PREFERENCES,
            Context.MODE_PRIVATE
        ).edit().putBoolean(KEY_COMPACT_MODE, compactMode).apply()
    }

    LibraryDetailPage(
        title = stringResource(id = R.string.plugin_manager_title),
        onBack = { navigator.navigateUp() },
        miuixTopBar = {
            BlurredTopBar(backdrop = topBarBackdrop) {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.plugin_manager_title), style = MaterialTheme.typography.titleLarge) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                    windowInsets = WindowInsets(0.dp),
                    navigationIcon = {
                        IconButton(onClick = { navigator.navigateUp() }) {
                            Icon(LucideIcons.ArrowLeft, stringResource(R.string.action_back))
                        }
                    },
                    actions = {
                    IconButton(
                        onClick = toggleCompactMode
                    ) {
                        Icon(
                            imageVector = if (compactMode) {
                                LucideIcons.LayoutGrid
                            } else {
                                LucideIcons.List
                            },
                            contentDescription = stringResource(
                                if (compactMode) {
                                    R.string.plugin_switch_to_detailed
                                } else {
                                    R.string.plugin_switch_to_compact
                                }
                            )
                        )
                    }

                    IconButton(
                        onClick = {
                            if (!uiState.isBusy) importLauncher.launch(arrayOf("*/*"))
                        }
                    ) {
                        Icon(
                            imageVector = LucideIcons.Plus,
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = topAppBarScrollBehavior
            )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blurSource(topBarBackdrop)
                .libraryDetailScroll(topAppBarScrollBehavior)
                .padding(scaffoldTopHorizontalPadding(paddingValues))
        ) {
            val typeTabs = PluginTypeTab.entries.map { stringResource(it.labelRes) }
            val onTypeTabSelected: (Int) -> Unit = { index ->
                selectedTypeTab = PluginTypeTab.entries[index]
            }

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(top = 8.dp),
                ) {
                    typeTabs.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = index == selectedTypeTab.ordinal,
                            onClick = { onTypeTabSelected(index) },
                            shape = SegmentedButtonDefaults.itemShape(index, typeTabs.size),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(label)
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.search_source_priority_tip),
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxHeight()
                        .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                if (currentList.isEmpty()) {
                    item("empty") {
                        IllustratedEmptyState(
                            kind = EmptyStateKind.Plugins,
                            title = if (plugins.isEmpty()) {
                                stringResource(R.string.plugin_empty)
                            } else {
                                stringResource(
                                    R.string.plugin_type_empty,
                                    stringResource(selectedTypeTab.labelRes)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            icon = LucideIcons.FileDown,
                            actionText = stringResource(R.string.plugin_import_archive),
                            onAction = {
                                if (!uiState.isBusy) importLauncher.launch(arrayOf("*/*"))
                            },
                        )
                    }
                }

                itemsIndexed(
                    items = currentList,
                    key = { _, plugin -> plugin.id }
                ) { index, plugin ->
                    ReorderableItem(
                        state = reorderableLazyColumnState,
                        key = plugin.id
                    ) { _ ->
                        val interactionSource = remember { MutableInteractionSource() }

                        PluginItem(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = if (compactMode) 6.dp else 12.dp)
                                .clip(
                                    MaterialTheme.shapes.medium
                                )
                                .longPressDraggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = {
                                        saveOrder()
                                    },
                                    interactionSource = interactionSource
                                ),
                            plugin = plugin,
                            compact = compactMode,
                            enabled = plugin.isEnabledFor(selectedTypeTab.sourceType),
                            updateUrl = "",
                            onUninstall = {
                                pendingUninstallPluginId = plugin.id
                                showUninstallDialog = true
                            },
                            onRename = {
                                pendingRenamePluginId = plugin.id
                                nameState.setTextAndPlaceCursorAtEnd(plugin.customName ?: plugin.name)
                                showRenameDialog = true
                            },
                            onCheckChanged = { enabled ->
                                viewModel.setEnabled(
                                    plugin.id,
                                    selectedTypeTab.sourceType,
                                    enabled
                                )
                            },
                            onConfig = {
                                navigator.navigate(PluginConfigDestination(plugin.id))
                            }
                        )
                    }
                }
            }
        }
    }

    val uninstallMessage = pendingUninstallPluginId?.let { id ->
        plugins.find { it.id == id }?.displayName?.let { name ->
            stringResource(R.string.plugin_uninstall_confirm_message, name)
        }
    }

    YesNoDialog(
        show = showUninstallDialog,
        title = stringResource(R.string.plugin_uninstall),
        summary = uninstallMessage,
        onDismissRequest = {
            showUninstallDialog = false
        },
        onDismissFinished = {
            showUninstallDialog = false
            pendingUninstallPluginId = null
        },
        onConfirm = {
            pendingUninstallPluginId?.let { viewModel.uninstallPlugin(it) }
            showUninstallDialog = false
            pendingUninstallPluginId = null
        }
    )

    val pendingRenamePlugin = pendingRenamePluginId?.let { id ->
        plugins.find { it.id == id }
    }

    WindowDialog(
        title = stringResource(R.string.plugin_custom_name),
        show = showRenameDialog,
        onDismissRequest = {
            showRenameDialog = false
        },
        onDismissFinished = {
            pendingRenamePluginId = null
        }
    ) {
        Column {
            SdwTextInput(
                state = nameState,
                label = stringResource(R.string.plugin_custom_name),
                lineLimits = TextFieldLineLimits.SingleLine,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.plugin_custom_name_hint,
                    pendingRenamePlugin?.name.orEmpty()
                ),
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        showRenameDialog = false
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.cancel)) }
                Spacer(modifier = Modifier.width(20.dp))
                TextButton(
                    onClick = {
                        showRenameDialog = false
                        pendingRenamePluginId?.let { id ->
                            viewModel.setCustomName(id, customNameInput)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) { Text(stringResource(R.string.confirm)) }
            }
        }
    }

    YesNoBottomSheet(
        show = showImportPreviewSheet,
        scrollContent = true,
        title = stringResource(R.string.plugin_import_found_title),
        onDismissRequest = {
            viewModel.discardPendingImportFiles()
            showImportPreviewSheet = false
        },
        enableNestedScroll = false,
        onDismissFinished = {
            if (!showImportPreviewSheet) {
                viewModel.clearPendingImport()
            }
        },
        onCancel = {
            viewModel.discardPendingImportFiles()
            showImportPreviewSheet = false
        },
        confirmText = stringResource(R.string.plugin_import_install),
        confirmEnabled = !uiState.isBusy && uiState.selectedImportRoots.isNotEmpty(),
        onConfirm = {
            viewModel.installPendingImport()
            showImportPreviewSheet = false
        },
        content = {
            pendingImport?.let { session ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    if (session.candidates.isNotEmpty()) {
                        SmallTitle(
                            text = stringResource(
                                R.string.plugin_import_installable_title,
                                session.candidates.size
                            ),
                            insideMargin = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        )
                    }

                    session.candidates.forEach { candidate ->
                        val selected =
                            candidate.relativeRootInArchive in uiState.selectedImportRoots

                        PluginImportCandidateItem(
                            candidate = candidate,
                            selected = selected,
                            onCheckedChange = { checked ->
                                viewModel.setImportCandidateSelected(
                                    candidate.relativeRootInArchive,
                                    checked
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (session.failed.isNotEmpty()) {
                    SmallTitle(
                        text = stringResource(
                            R.string.plugin_import_failed_title,
                            session.failed.size
                        ),
                        textColor = colorScheme.error
                    )

                    session.failed.forEach { failed ->
                        PluginImportFailedItem(failed = failed)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.padding(vertical = 12.dp))
            }
        }
    )
}

@Composable
private fun PluginImportCandidateItem(
    candidate: PluginInstallCandidate,
    selected: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val localeTags = androidx.compose.ui.platform.LocalConfiguration.current.locales.toLanguageTags()
    val manifest by androidx.compose.runtime.produceState(candidate.manifest, candidate, localeTags) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                com.lonx.lyrico.plugin.i18n.PluginStrings.load(candidate.pluginRoot, candidate.manifest)
                    .snapshot(localeTags.split(',')).localize(candidate.manifest)
            }.getOrDefault(candidate.manifest)
        }
    }
    val conflictText = candidate.versionConflict.toImportConflictText()
    val conflictColor = candidate.versionConflict.toImportConflictColor()
    val iconPath = manifest.icon?.let { File(candidate.pluginRoot, it).absolutePath }
    val versionText = if (candidate.existingPlugin != null) {
        "${candidate.existingPlugin.versionName} -> ${manifest.versionName}"
    } else {
        manifest.versionName
    }
    val locationText = candidate.relativeRootInArchive.ifBlank { "/" }
    val pluginTypeText = candidate.manifest.capabilities
        .displaySourceTypes()
        .map { stringResource(it.labelRes()) }
        .joinToString(" / ")

    Card(
        colors = CardDefaults.cardColors(containerColor = colorScheme.secondaryContainer)
    ) {
        BasicComponent(
            insideMargin = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            onClick = {
                onCheckedChange(!selected)
            },
            startAction = {
                PluginIcon(
                    iconPath = iconPath,
                    contentDescription = manifest.name,
                    size = 34.dp
                )
            },
            endActions = {
                Checkbox(
                    checked = selected,
                    onCheckedChange = {
                        onCheckedChange(!selected)
                    }
                )
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = manifest.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                ImportStatusBadge(
                    text = conflictText,
                    color = conflictColor
                )
            }

            Text(
                text = manifest.id,
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = stringResource(R.string.plugin_import_version, versionText),
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.plugin_type_with_value, pluginTypeText),
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.plugin_import_path,locationText),
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (manifest.description.isNotBlank()) {
                Text(
                    text = stringResource(R.string.plugin_import_description,manifest.description),
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

    }
}

@Composable
private fun PluginImportFailedItem(
    failed: PluginInstallFailed
) {
    var expanded by rememberSaveable(
        failed.rootPath,
        failed.reason,
        failed.pluginId,
        failed.versionName
    ) {
        mutableStateOf(false)
    }

    val title = failed.displayName

    val subtitle = buildString {
        failed.pluginId
            ?.takeIf { it.isNotBlank() && it != title }
            ?.let { append(it) }

        failed.versionName
            ?.takeIf { it.isNotBlank() }
            ?.let {
                if (isNotEmpty()) append("\n")
                append(stringResource(R.string.plugin_import_version))
                append(" ")
                append(it)
            }

        if (failed.rootPath.isNotBlank()) {
            if (isNotEmpty()) append("\n")
            append(failed.rootPath)
        }
    }

    val versionChange = buildString {
        val oldVersion = failed.existingVersionName
        val newVersion = failed.versionName
        if (!oldVersion.isNullOrBlank() && !newVersion.isNullOrBlank()) {
            append(oldVersion)
            append(" -> ")
            append(newVersion)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer)
    ) {
        BasicComponent(
            onClick = {
                expanded = !expanded
            },
            modifier = Modifier.animateContentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onErrorContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                failed.conflict?.let { conflict ->
                    ImportStatusBadge(
                        text = conflict.toImportConflictText(),
                        color = colorScheme.error
                    )
                }

                Text(
                    text = if (expanded) {
                        stringResource(R.string.plugin_import_collapse)
                    } else {
                        stringResource(R.string.plugin_import_expand)
                    },
                    fontSize = 12.sp,
                    color = colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colorScheme.onErrorContainer.copy(alpha = 0.82f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (versionChange.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = versionChange,
                    fontSize = 12.sp,
                    color = colorScheme.onErrorContainer.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = failed.reason,
                fontSize = 12.sp,
                color = colorScheme.onErrorContainer,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ImportStatusBadge(
    text: String,
    color: Color
) {

    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun PluginVersionConflict.toImportConflictColor(): Color {

    return when (this) {
        PluginVersionConflict.NONE -> colorScheme.primary
        PluginVersionConflict.UPDATE -> colorScheme.primary
        PluginVersionConflict.OVERWRITE -> colorScheme.onTertiaryContainer
        PluginVersionConflict.DOWNGRADE -> colorScheme.error
    }
}

@Composable
private fun PluginVersionConflict.toImportConflictText(): String {
    return when (this) {
        PluginVersionConflict.NONE ->
            stringResource(R.string.plugin_import_conflict_new)

        PluginVersionConflict.UPDATE ->
            stringResource(R.string.plugin_import_conflict_update)

        PluginVersionConflict.OVERWRITE ->
            stringResource(R.string.plugin_import_conflict_overwrite)

        PluginVersionConflict.DOWNGRADE ->
            stringResource(R.string.plugin_import_conflict_downgrade)
    }
}

@Composable
fun PluginItem(
    plugin: SourcePluginEntity,
    compact: Boolean,
    enabled: Boolean,
    updateUrl: String,
    onUninstall: () -> Unit,
    onRename: () -> Unit,
    onCheckChanged: (Boolean) -> Unit,
    onConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (compact) {
        CompactPluginItem(
            plugin = plugin,
            enabled = enabled,
            onUninstall = onUninstall,
            onRename = onRename,
            onCheckChanged = onCheckChanged,
            onConfig = onConfig,
            modifier = modifier
        )
        return
    }

    val secondaryContainer = colorScheme.secondaryContainer.copy(alpha = 0.8f)
    val actionIconTint = colorScheme.onSurface.copy(alpha = if (isDarkTheme) 0.7f else 0.9f)

    val pluginId = plugin.id
    val pluginName = plugin.displayName
    val pluginAuthor = plugin.author
    val pluginVersion = plugin.versionName
    val pluginDescription = plugin.description
    val pluginEnabled = enabled
    val pluginTypeText = plugin.displaySourceTypes
        .map { stringResource(it.labelRes()) }
        .joinToString(" / ")

    val hasDescription = pluginDescription.isNotBlank()
    val hasUpdateSource = updateUrl.isNotBlank()

    var expanded by rememberSaveable(pluginId) {
        mutableStateOf(false)
    }

    Card(
        modifier = modifier,
        onClick = {
            if (hasDescription) expanded = !expanded
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PluginIcon(
                iconPath = plugin.iconPath,
                contentDescription = pluginName,
                size = 40.dp
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = pluginName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight(550),
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (hasUpdateSource) {
                        PluginBadge(
                            text = stringResource(R.string.plugin_update_source_configured)
                        )
                    }

                }

                Text(
                    text = pluginTypeText,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = stringResource(R.string.plugin_version_with_value, pluginVersion),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = stringResource(
                        R.string.plugin_api_versions_with_value,
                        plugin.apiVersion,
                        plugin.minHostApiVersion
                    ),
                    fontSize = 12.sp,
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = stringResource(R.string.plugin_author_with_value, pluginAuthor),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 1.dp),
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = pluginEnabled,
                onCheckedChange = { checked ->
                    if (checked != pluginEnabled) {
                        onCheckChanged(checked)
                    }
                }
            )
        }

        if (hasDescription) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .animateContentSize(
                        animationSpec = tween(
                            durationMillis = Motion.contentDuration,
                            easing = Motion.emphasized
                        )
                    )
            ) {
                Text(
                    text = pluginDescription,
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant,
                    overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                    maxLines = if (expanded) Int.MAX_VALUE else 3
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 0.5.dp,
            color = colorScheme.outline.copy(alpha = 0.5f)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PluginActionChip(
                text = stringResource(R.string.plugin_config),
                icon = LucideIcons.Settings,
                tint = actionIconTint,
                background = secondaryContainer,
                onClick = onConfig
            )
            PluginActionChip(
                text = stringResource(R.string.plugin_custom_name),
                icon = LucideIcons.Pencil,
                tint = actionIconTint,
                background = secondaryContainer,
                onClick = onRename
            )

            Spacer(modifier = Modifier.weight(1f))

            PluginActionChip(
                text = stringResource(R.string.plugin_uninstall),
                icon = LucideIcons.Trash,
                tint = colorScheme.error,
                background = colorScheme.errorContainer.copy(alpha = 0.45f),
                onClick = onUninstall
            )
        }
    }
}

@Composable
private fun CompactPluginItem(
    plugin: SourcePluginEntity,
    enabled: Boolean,
    onUninstall: () -> Unit,
    onRename: () -> Unit,
    onCheckChanged: (Boolean) -> Unit,
    onConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pluginName = plugin.displayName
    val pluginTypeText = plugin.displaySourceTypes
        .map { stringResource(it.labelRes()) }
        .joinToString(" / ")
    val neutralActionBackground = colorScheme.secondaryContainer.copy(alpha = 0.8f)
    val neutralActionTint = colorScheme.onSurface.copy(alpha = if (isDarkTheme) 0.7f else 0.9f)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PluginIcon(
                iconPath = plugin.iconPath,
                contentDescription = pluginName,
                size = 36.dp
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = pluginName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight(550),
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                }

                Text(
                    text = pluginTypeText,
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = stringResource(R.string.plugin_version_with_value, plugin.versionName),
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = stringResource(
                        R.string.plugin_api_versions_with_value,
                        plugin.apiVersion,
                        plugin.minHostApiVersion
                    ),
                    fontSize = 11.sp,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Switch(
                checked = enabled,
                onCheckedChange = { checked ->
                    if (checked != enabled) onCheckChanged(checked)
                }
            )
        }

        Row(
            modifier = Modifier.padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            CompactPluginActionButton(
                icon = LucideIcons.Settings,
                contentDescription = stringResource(R.string.plugin_config),
                tint = neutralActionTint,
                background = neutralActionBackground,
                onClick = onConfig
            )
            CompactPluginActionButton(
                icon = LucideIcons.Pencil,
                contentDescription = stringResource(R.string.plugin_custom_name),
                tint = neutralActionTint,
                background = neutralActionBackground,
                onClick = onRename
            )
            CompactPluginActionButton(
                icon = LucideIcons.Trash,
                contentDescription = stringResource(R.string.plugin_uninstall),
                tint = colorScheme.error,
                background = colorScheme.errorContainer.copy(alpha = 0.45f),
                onClick = onUninstall
            )
        }
    }
}

@Composable
private fun CompactPluginActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    background: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = null),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun PluginSourceType.labelRes(): Int = when (this) {
    PluginSourceType.METADATA -> R.string.plugin_type_metadata
    PluginSourceType.LYRICS -> R.string.plugin_type_lyrics
    PluginSourceType.COVER -> R.string.plugin_type_cover
}

private const val PLUGIN_MANAGER_PREFERENCES = "plugin_manager_preferences"
private const val KEY_COMPACT_MODE = "compact_mode"

@Composable
private fun PluginBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colorScheme.tertiaryContainer.copy(alpha = 0.6f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        fontWeight = FontWeight(700),
        maxLines = 1,
        softWrap = false
    )
}

@Composable
private fun PluginActionChip(
    text: String,
    icon: ImageVector,
    tint: Color,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 35.dp)
            .clip(CircleShape)
            .background(background)
            .combinedClickable(
                onClick = {
                    onClick()
                },
                onLongClick = null
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )

        Text(
            text = text,
            color = tint,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier.padding(start = 5.dp, end = 2.dp)
        )
    }
}

@Composable
private fun BasicComponent(
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    startAction: (@Composable () -> Unit)? = null,
    endActions: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(insideMargin),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (startAction != null) {
            Box(modifier = Modifier.padding(end = 12.dp)) { startAction() }
        }
        // 必须用 Column：content 里是「标题 Row + 多行说明 Text」，放进 Box 会全部层叠
        // （插件包预览里插件名/id/版本/类型/路径相互重叠的根因）。
        Column(modifier = Modifier.weight(1f)) { content() }
        if (endActions != null) {
            endActions()
        }
    }
}

@Composable
private fun SmallTitle(
    text: String,
    modifier: Modifier = Modifier,
    insideMargin: PaddingValues = PaddingValues(0.dp),
    textColor: Color? = null,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = textColor ?: MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(insideMargin)
    )
}
