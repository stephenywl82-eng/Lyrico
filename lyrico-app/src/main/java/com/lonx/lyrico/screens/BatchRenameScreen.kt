
package com.lonx.lyrico.screens

import com.lonx.lyrico.ui.components.icons.LucideIcons
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.library.EmptyStateKind
import com.lonx.lyrico.ui.components.library.IllustratedEmptyState
import com.lonx.lyrico.ui.components.library.LibraryDetailPage
import com.lonx.lyrico.data.model.RenamePreview
import com.lonx.lyrico.ui.components.scaffoldBottomPadding
import com.lonx.lyrico.ui.components.scaffoldTopHorizontalPadding
import com.lonx.lyrico.utils.TagField
import com.lonx.lyrico.viewmodel.BatchRenameViewModel
import com.lonx.lyrico.ui.components.sdw.SdwBottomSheet
import com.lonx.lyrico.ui.components.editor.rememberEditorTextFieldState
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel

import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.ui.semantics.Role
import com.lonx.lyrico.ui.components.base.AppLinearProgressIndicator
import com.lonx.lyrico.ui.components.base.SheetProgressAction
import com.lonx.lyrico.ui.components.base.SheetStatisticsRow
import com.lonx.lyrico.ui.components.library.LibraryDetailTabs
import com.ramcosta.composedestinations.generated.destinations.CharacterMappingDestination
import androidx.compose.material3.MaterialTheme

private enum class BatchRenameTab(val labelRes: Int) {
    Config(R.string.batch_rename_tab_config),
    Preview(R.string.batch_rename_tab_preview)
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
@Destination<RootGraph>(route = "batch_rename")
fun BatchRenameScreen(
    navigator: DestinationsNavigator
) {
    val viewModel: BatchRenameViewModel = koinViewModel()
    val renameFormat by viewModel.renameFormat.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val showDropdowns = remember { mutableStateOf(false) }
    val tabs = remember { BatchRenameTab.entries }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val salt = false

    var showPlaceholderInfo by remember { mutableStateOf(false) }

    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val tabLabels = tabs.map { tab ->
        when (tab) {
            BatchRenameTab.Config -> stringResource(tab.labelRes)
            BatchRenameTab.Preview -> stringResource(tab.labelRes, uiState.previews.size)
        }
    }

    LibraryDetailPage(
        title = stringResource(id = R.string.batch_rename_title),
        onBack = { if (!uiState.isRenamingInProgress) navigator.popBackStack() },
        miuixTopBar = {
            SmallTopAppBar(
                title = stringResource(id = R.string.batch_rename_title),
                navigationIcon = {
                    IconButton(
                        onClick = { if (!uiState.isRenamingInProgress) navigator.popBackStack() }) {
                        Icon(
                            imageVector = LucideIcons.ArrowLeft,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!uiState.isRenamingInProgress) {
                                navigator.navigate(CharacterMappingDestination)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = LucideIcons.List,
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = topAppBarScrollBehavior,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldTopHorizontalPadding(paddingValues))
        ) {
            
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 8.dp)
                ) {
                    TabRowWithContour(
                        tabs = tabLabels,
                        selectedTabIndex = pagerState.currentPage,
                        onTabSelected = { index ->
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                    )
                }
            
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { index -> tabs[index].name }
            ) { page ->
                when (tabs[page]) {
                    BatchRenameTab.Config -> BatchRenameConfigTab(
                        bottomPadding = scaffoldBottomPadding(paddingValues),
                        topAppBarNestedScrollConnection = topAppBarScrollBehavior.nestedScrollConnection,
                        renameFormat = renameFormat,
                        uiState = uiState,
                        showDropdowns = showDropdowns.value,
                        onDropdownVisibleChange = { showDropdowns.value = it },
                        showPlaceholderInfo = showPlaceholderInfo,
                        onShowPlaceholderInfoChange = { showPlaceholderInfo = it },
                        onFormatChange = viewModel::saveFormat,
                        onExecuteRename = viewModel::executeRename,
                        errorMessage = uiState.errorMessage?.asString(context)
                    )

                    BatchRenameTab.Preview -> BatchRenamePreviewTab(
                        bottomPadding = scaffoldBottomPadding(paddingValues),
                        topAppBarNestedScrollConnection = topAppBarScrollBehavior.nestedScrollConnection,
                        uiState = uiState
                    )
                }
            }
        }

            SdwBottomSheet(
                show = uiState.renameProgress != null,
                onDismissRequest = {
                    if (!uiState.isRenamingInProgress) viewModel.closeRenameDialog()
                },
                allowDismiss = !uiState.isRenamingInProgress,
                title = stringResource(R.string.batch_rename_title),
                footer = {
                    TextButton(
                        text = stringResource(if (uiState.isRenamingInProgress) R.string.action_abort else R.string.confirm),
                        onClick = {
                            if (uiState.isRenamingInProgress) viewModel.abortRename()
                            else viewModel.closeRenameDialog()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    BatchRenameProgressContent(
                        uiState = uiState,
                        salt = false,
                    )
                }
            }
        
    }
}

/** Progress content only; each engine keeps the action in its fixed footer. */
@Composable
private fun BatchRenameProgressContent(
    uiState: com.lonx.lyrico.viewmodel.BatchRenameUiState,
    salt: Boolean,
) {
    Column(
        // Salt 弹层内容留内边距，避免文字贴到卡片圆角。
        modifier = Modifier.padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        uiState.renameProgress?.let { (current, total) ->
            val progress =
                if (total > 0) current.toFloat() / total.toFloat() else 0f

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.isRenamingInProgress) {
                        uiState.currentFile
                    } else {
                        stringResource(
                            R.string.batch_matching_total_time,
                            uiState.renameTimeMillis / 1000.0
                        )
                    },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "$current / $total",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.End
                )
            }

            LinearProgressIndicator(progress = progress)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.batch_matching_success, uiState.successCount),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = stringResource(R.string.batch_replay_gain_skipped, uiState.skippedCount),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = stringResource(R.string.batch_matching_failure, uiState.failureCount),
            style = MaterialTheme.typography.bodyLarge
        )
    }

}

@Composable
private fun BatchRenameConfigTab(
    bottomPadding: androidx.compose.ui.unit.Dp,
    topAppBarNestedScrollConnection: NestedScrollConnection,
    renameFormat: String,
    uiState: com.lonx.lyrico.viewmodel.BatchRenameUiState,
    showDropdowns: Boolean,
    onDropdownVisibleChange: (Boolean) -> Unit,
    showPlaceholderInfo: Boolean,
    onShowPlaceholderInfoChange: (Boolean) -> Unit,
    onFormatChange: (String) -> Unit,
    onExecuteRename: () -> Unit,
    errorMessage: String?
) {
    val salt = false
    val formatState = rememberEditorTextFieldState(renameFormat, onFormatChange)
    LazyColumn(
        modifier = (Modifier
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(topAppBarNestedScrollConnection))
            .fillMaxHeight(),
        contentPadding = PaddingValues(bottom = bottomPadding + 12.dp),
        overscrollEffect = null,
    ) {
        item {
            
                SmallTitle(text = stringResource(id = R.string.rename_format))
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    TextField(
                        modifier = Modifier.padding(12.dp),
                        state = formatState,
                        trailingIcon = {
                            Box {
                                IconButton(
                                    modifier = Modifier.padding(end = 12.dp),
                                    onClick = { onDropdownVisibleChange(true) }) {
                                    Icon(
                                        imageVector = LucideIcons.FileMusic,
                                        contentDescription = null
                                    )
                                }
                                OverlayListPopup(
                                    show = showDropdowns,
                                    popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
                                    onDismissRequest = { onDropdownVisibleChange(false) },
                                    alignment = PopupPositionProvider.Align.TopEnd
                                ) {
                                    ListPopupColumn {
                                        uiState.presetFormats.forEach { format ->
                                            DropdownImpl(
                                                text = format,
                                                isSelected = renameFormat == format,
                                                optionSize = uiState.presetFormats.size,
                                                index = uiState.presetFormats.indexOf(format),
                                                onSelectedIndexChange = {
                                                    onFormatChange(format)
                                                    onDropdownVisibleChange(false)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        lineLimits = TextFieldLineLimits.MultiLine(),
                    )
                    Text(
                        modifier = Modifier.padding(12.dp),
                        text = stringResource(id = R.string.format_hint),
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SwitchPreference(
                        title = stringResource(id = R.string.format_preset_show_placeholders),
                        checked = showPlaceholderInfo,
                        onCheckedChange = onShowPlaceholderInfoChange
                    )
                    AnimatedVisibility(visible = showPlaceholderInfo) {
                        PlaceholderInfoContent()
                    }

                    TextButton(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        onClick = onExecuteRename,
                        text = stringResource(id = R.string.action_rename),
                        enabled = uiState.previews.isNotEmpty() && !uiState.isRenamingInProgress,
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            
        }

        errorMessage?.let { text ->
            item {
                
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = text,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                
            }
        }
    }
}

@Composable
private fun BatchRenamePreviewTab(
    bottomPadding: androidx.compose.ui.unit.Dp,
    topAppBarNestedScrollConnection: NestedScrollConnection,
    uiState: com.lonx.lyrico.viewmodel.BatchRenameUiState
) {
    var expandedPreviewKey by remember { mutableStateOf<String?>(null) }
    val salt = false

    LazyColumn(
        modifier = (Modifier
            .scrollEndHaptic()
            .overScrollVertical()
            .nestedScroll(topAppBarNestedScrollConnection))
            .fillMaxHeight(),
        contentPadding = PaddingValues(bottom = bottomPadding + 12.dp),
        overscrollEffect = null,
    ) {
        item {
            val title = stringResource(
                if (uiState.isGeneratingPreview)
                    R.string.preview_title_generating
                else
                    R.string.preview_title,
                uiState.previews.size
            )
            SmallTitle(text = title)
        }

        if (uiState.previews.isEmpty()) {
            item {
                IllustratedEmptyState(
                    kind = EmptyStateKind.Tasks,
                    title = stringResource(R.string.preview_empty_tip),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            itemsIndexed(
                items = uiState.previews,
                key = { _, preview -> preview.key }
            ) { _, preview ->
                val previewKey = preview.key
                val oldName = stringResource(
                    R.string.label_old_name,
                    preview.originalPath.substringAfterLast('/')
                )
                val newName = stringResource(
                    R.string.label_new_name,
                    preview.newPath.substringAfterLast('/')
                )
                
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                            .animateContentSize()
                    ) {
                        BasicComponent(
                            onClick = {
                                expandedPreviewKey = if (expandedPreviewKey == previewKey) null else previewKey
                            }
                        ) {
                            Text(
                                text = oldName,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = newName,
                                style = MaterialTheme.typography.bodyLarge.copy(if (preview.conflict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (preview.conflict) {
                                Text(
                                    text = stringResource(R.string.rename_conflict_warning),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            AnimatedVisibility(visible = expandedPreviewKey == previewKey) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.label_old_path, preview.originalPath),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.label_new_path, preview.newPath),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (preview.conflict) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                
            }
        }
    }
}

private val RenamePreview.key: String
    get() = "$originalPath\n$newPath"

@Composable
private fun PlaceholderInfoContent() {
    val placeholders = TagField.entries.map {
        "@${it.index}" to it.description
    }
    val salt = false

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        placeholders.forEach { (placeholder, description) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                
                    Text(
                        text = placeholder,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(description),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                
            }
        }
    }
}
