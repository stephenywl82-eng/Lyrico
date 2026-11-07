
package com.lonx.lyrico.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import com.lonx.lyrico.ui.theme.Motion
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import com.lonx.lyrico.ui.components.library.libraryDetailScroll
import com.lonx.lyrico.ui.components.library.LocalLibraryBottomContentPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.lonx.lyrico.ui.components.library.SelectableLibraryItem
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.icons.PreferenceIconTone
import com.lonx.lyrico.ui.components.icons.PreferenceIcon
import com.lonx.lyrico.ui.components.icons.LucideIcons
import com.lonx.lyrico.ui.components.library.EmptyStateKind
import com.lonx.lyrico.ui.components.library.IllustratedEmptyState
import com.lonx.lyrico.ui.components.library.LibraryDetailPage

import com.lonx.lyrico.data.model.entity.FolderEntity
import com.lonx.lyrico.data.model.entity.SongEntity
import com.lonx.lyrico.ui.components.bar.SongBatchSelectionActions
import com.lonx.lyrico.ui.components.bar.SongSelectionTopAppBar
import com.lonx.lyrico.ui.components.scaffoldTopHorizontalPadding
import com.lonx.lyrico.ui.components.song.SongActionSheets
import com.lonx.lyrico.ui.components.song.SongListItemContent
import com.lonx.lyrico.ui.components.song.SongListItemActions
import com.lonx.lyrico.utils.UriUtils
import com.lonx.lyrico.viewmodel.FolderManagerViewModel
import com.lonx.lyrico.viewmodel.SongSelectionViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.EditMetadataDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.lonx.lyrico.ui.components.base.PillButton
import com.lonx.lyrico.ui.components.base.PillButtonDefaults
import com.lonx.lyrico.ui.components.base.PillButtonSize
import com.lonx.lyrico.ui.components.library.LibraryDetailTabs
import com.lonx.lyrico.screens.library.LibrarySelectionState
import com.lonx.lyrico.viewmodel.SortBy
import com.lonx.lyrico.viewmodel.SortInfo
import com.lonx.lyrico.viewmodel.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Destination<RootGraph>(route = "folder_manager")
fun FolderManagerScreen(
    navigator: DestinationsNavigator,
    embedded: Boolean = false
) {
    val viewModel: FolderManagerViewModel = koinViewModel()
    val selectionViewModel: SongSelectionViewModel = koinViewModel()

    val uiState by viewModel.uiState.collectAsState()
    val sortInfo by viewModel.sortInfo.collectAsState()
    val currentFolderSongs by viewModel.currentFolderSongs.collectAsState()
    val isSelectionMode by selectionViewModel.isSelectionMode.collectAsState()
    val selectedSongUris by selectionViewModel.selectedSongUris.collectAsState()
    val swipeAnchorUri by selectionViewModel.swipeAnchorUri.collectAsState()
    val swipeSelectionLabel = stringResource(
        if (!isSelectionMode) {
            R.string.swipe_selection_enter_selection
        } else if (swipeAnchorUri == null) {
            R.string.swipe_selection_range_start
        } else {
            R.string.swipe_selection_range_end
        }
    )
    val swipeSelectionSecondaryLabel = if (!isSelectionMode) {
        stringResource(R.string.swipe_selection_range_start)
    } else {
        null
    }

    val folders = uiState.folders
    val folderTree = remember(folders) {
        buildFolderTree(folders)
    }
    val context = LocalContext.current

    var currentFolderId by rememberSaveable { mutableLongStateOf(ROOT_FOLDER_ID) }
    var selectedFolderId by rememberSaveable { mutableLongStateOf(ROOT_FOLDER_ID) }
    var folderContentTransition by remember { mutableStateOf<FolderContentTransition?>(null) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var savedPagerPage by rememberSaveable { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(
        initialPage = savedPagerPage,
        pageCount = { 2 }
    )
    var selectedSong by remember { mutableStateOf<SongEntity?>(null) }
    var showMenuSheet by remember { mutableStateOf(false) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    val selectedFolder = remember(selectedFolderId, folders) {
        folders.find { it.id == selectedFolderId }
    }
    val currentFolder = remember(currentFolderId, folders) {
        folders.find { it.id == currentFolderId }
    }
    LaunchedEffect(currentFolder?.id) {
        viewModel.setCurrentFolderId(currentFolder?.id)
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page -> savedPagerPage = page }
    }
    val currentNode = remember(currentFolder, folderTree) {
        currentFolder?.let { folderTree.nodesById[it.id] }
    }
    val parentFolder = remember(currentNode, folderTree) {
        currentNode?.parentId?.let { folderTree.nodesById[it]?.folder }
    }
    val currentChildFolders = remember(currentNode, folderTree) {
        currentNode?.childFolders ?: folderTree.rootFolders
    }
    val currentSongs = if (
        currentFolder != null &&
        currentFolderSongs.all { song -> song.folderId == currentFolder.id }
    ) {
        currentFolderSongs
    } else {
        emptyList()
    }
    val rootPathLabel = stringResource(R.string.folder_root_path)
    val currentBreadcrumbs = remember(currentFolder, currentNode, folderTree, rootPathLabel) {
        buildFolderBreadcrumbs(currentFolder, currentNode, folderTree, rootPathLabel)
    }
    val showConfirmDialog = remember { mutableStateOf(false) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            try {
                context.contentResolver.takePersistableUriPermission(it, flags)
            } catch (e: SecurityException) {
                e.printStackTrace()
            }

            val path = UriUtils.getFileAbsolutePath(context, it)
                ?: it.toString()

            viewModel.addFolder(
                path = path,
                treeUri = it.toString()
            )
        }
    }

    val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val selectionScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    fun navigateToFolder(folderId: Long, forward: Boolean) {
        if (folderId != currentFolderId) {
            folderContentTransition = FolderContentTransition(
                fromFolderId = currentFolderId,
                toFolderId = folderId,
                forward = forward
            )
        }
        currentFolderId = folderId
    }

    BackHandler(enabled = isSelectionMode || currentFolder != null) {
        when {
            isFabMenuExpanded -> {
                isFabMenuExpanded = false
            }

            isSelectionMode -> {
                selectionViewModel.exitSelectionMode()
            }

            currentFolder != null -> {
                navigateToFolder(parentFolder?.id ?: ROOT_FOLDER_ID, forward = false)
            }
        }
    }

    val salt = false
    val folderTitle = if (currentFolderId == ROOT_FOLDER_ID) {
        stringResource(R.string.folder_manager_title)
    } else {
        currentFolder?.path
            ?.substringAfterLast("/")
            ?.ifBlank { currentFolder.path }
            ?: stringResource(R.string.folder_manager_title)
    }
    val navigateUpAction: () -> Unit = {
        if (currentFolder != null) {
            navigateToFolder(parentFolder?.id ?: ROOT_FOLDER_ID, forward = false)
        } else if (!embedded) {
            navigator.navigateUp()
        }
    }
    val allCurrentSongsSelected =
        currentSongs.isNotEmpty() && selectedSongUris.containsAll(currentSongs.map { it.uri })
    val saltSelectionState = null

    Box(modifier = Modifier.fillMaxSize()) {
        LibraryDetailPage(
            title = folderTitle,
            onBack = navigateUpAction,
            selection = saltSelectionState,
            miuixTopBar = {
                AnimatedContent(
                    targetState = isSelectionMode,
                    label = "FolderManagerTopBarAnimation",
                    transitionSpec = {
                        val animationDuration = Motion.contentDuration
                        val enter = fadeIn(tween(animationDuration)) +
                                slideInVertically(
                                    animationSpec = tween(
                                        durationMillis = animationDuration,
                                        easing = Motion.emphasized
                                    ),
                                    initialOffsetY = { -it / 3 }
                                )
                        val exit = fadeOut(tween(animationDuration)) +
                                slideOutVertically(
                                    animationSpec = tween(
                                        durationMillis = animationDuration,
                                        easing = Motion.emphasized
                                    ),
                                    targetOffsetY = { -it / 3 }
                                )

                        (enter togetherWith exit).using(SizeTransform(clip = false))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { selectionMode ->
                    if (selectionMode) {
                        SongSelectionTopAppBar(
                            songs = currentSongs,
                            selectedSongUris = selectedSongUris,
                            scrollBehavior = selectionScrollBehavior,
                            onSelectAll = selectionViewModel::selectAll,
                            onDeselectAll = selectionViewModel::deselectAll,
                            onClose = selectionViewModel::exitSelectionMode
                        )
                    } else {
                        TopAppBar(
                            title = {
                                Text(
                                    if (currentFolderId == ROOT_FOLDER_ID) {
                                        stringResource(R.string.folder_manager_title)
                                    } else {
                                        currentFolder?.path
                                            ?.substringAfterLast("/")
                                            ?.ifBlank { currentFolder.path }
                                            ?: stringResource(R.string.folder_manager_title)
                                    }
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent
                            ),
                            windowInsets = WindowInsets(0.dp),
                            navigationIcon = {
                                if (currentFolder != null || !embedded) {
                                    IconButton(
                                        onClick = navigateUpAction
                                    ) {
                                        Icon(
                                            imageVector = LucideIcons.ArrowLeft,
                                            contentDescription = null
                                        )
                                    }
                                }
                            },
                            actions = {
                                if (currentFolderId == ROOT_FOLDER_ID) {
                                    IconButton(
                                        onClick = { folderPickerLauncher.launch(null) }
                                    ) {
                                        Icon(
                                            imageVector = LucideIcons.FolderPlus,
                                            contentDescription = null
                                        )
                                    }
                                }

                                if (
                                    currentFolder != null &&
                                    pagerState.currentPage == 1 &&
                                    currentSongs.isNotEmpty()
                                ) {
                                    var sortMenuExpanded by remember { mutableStateOf(false) }
                                    val sortTypes = SortBy.entries.toList()
                                    IconButton(onClick = { sortMenuExpanded = true }) {
                                        Icon(
                                            imageVector = LucideIcons.Sort,
                                            contentDescription = stringResource(R.string.cd_sort)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = sortMenuExpanded,
                                        onDismissRequest = { sortMenuExpanded = false }
                                    ) {
                                        sortTypes.forEach { sortType ->
                                            val isSelected = sortInfo.sortBy == sortType
                                            DropdownMenuItem(
                                                text = { Text(stringResource(sortType.labelRes)) },
                                                trailingIcon = if (isSelected) {
                                                    { Icon(LucideIcons.Check, contentDescription = null) }
                                                } else null,
                                                onClick = {
                                                    sortMenuExpanded = false
                                                    val newOrder = if (isSelected) {
                                                        if (sortInfo.order == SortOrder.ASC) {
                                                            SortOrder.DESC
                                                        } else {
                                                            SortOrder.ASC
                                                        }
                                                    } else {
                                                        SortOrder.ASC
                                                    }
                                                    viewModel.onSortChange(
                                                        SortInfo(sortBy = sortType, order = newOrder)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            scrollBehavior = topAppBarScrollBehavior
                        )
                    }
                }
            }
        ) { paddingValues ->
            selectedFolder?.let { folder ->
                AlertDialog(
                    onDismissRequest = { showConfirmDialog.value = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showConfirmDialog.value = false
                                viewModel.deleteFolder(folder)
                                selectedFolderId = ROOT_FOLDER_ID
                                if (folder.id == currentFolderId) {
                                    navigateToFolder(
                                        parentFolder?.id ?: ROOT_FOLDER_ID,
                                        forward = false
                                    )
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(stringResource(R.string.confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmDialog.value = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    },
                    title = { Text(stringResource(R.string.dialog_remove_folder_title)) },
                    text = {
                        Column {
                            Text(
                                text = folder.path,
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = stringResource(R.string.dialog_remove_folder_content_tip),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = MaterialTheme.typography.bodyMedium.fontSize
                            )
                        }
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldTopHorizontalPadding(paddingValues))
            ) {
                FolderPathHeader(
                    breadcrumbs = currentBreadcrumbs,
                    folderCount = currentChildFolders.size,
                    songCount = currentSongs.size,
                    onBreadcrumbClick = { folderId ->
                        navigateToFolder(folderId, forward = false)
                    }
                )

                uiState.error?.let { error ->
                    Text(
                        text = stringResource(R.string.folder_scan_failed, error),
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                LibraryDetailTabs(
                    tabs = listOf(
                        "${stringResource(R.string.folder_tab_folders)} (${currentChildFolders.size})",
                        "${stringResource(R.string.folder_tab_songs)} (${currentSongs.size})"
                    ),
                    selectedIndex = pagerState.currentPage,
                    onSelected = { index ->
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )

                FolderPagerSlideContent(
                    currentFolderId = currentFolderId,
                    transition = folderContentTransition,
                    folders = folders,
                    folderTree = folderTree,
                    currentFolderSongs = currentSongs,
                    pagerState = pagerState,
                    scanningFolderIds = uiState.scanningFolderIds,
                    queuedFolderIds = uiState.queuedFolderIds,
                    isSelectionMode = isSelectionMode,
                    selectedSongUris = selectedSongUris,
                    swipeSelectionLabel = swipeSelectionLabel,
                    swipeSelectionSecondaryLabel = swipeSelectionSecondaryLabel,
                    topAppBarScrollBehavior = topAppBarScrollBehavior,
                    navigator = navigator,
                    selectionViewModel = selectionViewModel,
                    onFolderClick = { folder ->
                        navigateToFolder(folder.id, forward = true)
                    },
                    onDeleteFolder = { folder ->
                        selectedFolderId = folder.id
                        showConfirmDialog.value = true
                    },
                    onRefreshFolder = viewModel::refreshFolder,
                    onIgnoredChange = viewModel::setFolderIgnored,
                    onShowSongMenu = { song ->
                        selectedSong = song
                        showMenuSheet = true
                    },
                    onTransitionFinished = {
                        folderContentTransition = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

            SongActionSheets(
                selectedSong = selectedSong,
                showMenuSheet = showMenuSheet,
                showDetailSheet = showDetailSheet,
                showDeleteDialog = showDeleteDialog,
                showRenameDialog = showRenameDialog,
                onDismissMenu = { showMenuSheet = false },
                onDismissMenuFinished = {
                    if (!showMenuSheet && !showDetailSheet && !showDeleteDialog && !showRenameDialog) {
                        selectedSong = null
                    }
                },
                onDismissDetail = { showDetailSheet = false },
                onDismissDelete = { showDeleteDialog = false },
                onDismissRename = { showRenameDialog = false },
                onShowDetail = { showDetailSheet = true },
                onShowDelete = { showDeleteDialog = true },
                onShowRename = { showRenameDialog = true },
                onPlay = { song ->
                    selectionViewModel.play(context, song)
                },
                onDelete = { song ->
                    selectionViewModel.delete(song)
                },
                onRename = { song, newFileName ->
                    selectionViewModel.renameSong(song, newFileName)
                }
            )
        }

        SongBatchSelectionActions(
            navigator = navigator,
            songs = currentSongs,
            show = isSelectionMode && pagerState.currentPage == 1 && currentSongs.isNotEmpty(),
            expanded = isFabMenuExpanded,
            selectedSongUris = selectedSongUris,
            onExpandedChange = { isFabMenuExpanded = it },
            onSetSelectionUris = selectionViewModel::setSelectionUris,
            onBatchDelete = selectionViewModel::batchDelete,
            onBatchShare = selectionViewModel::batchShare
        )
    }
}

private const val ROOT_FOLDER_ID = -1L

private data class FolderContentTransition(
    val fromFolderId: Long,
    val toFolderId: Long,
    val forward: Boolean
)

private data class FolderContentSnapshot(
    val folderId: Long,
    val childFolders: List<FolderEntity>,
    val songs: List<SongEntity>
)

private data class FolderBreadcrumb(
    val folderId: Long,
    val label: String
)

@Composable
private fun FolderPagerSlideContent(
    currentFolderId: Long,
    transition: FolderContentTransition?,
    folders: List<FolderEntity>,
    folderTree: FolderTree,
    currentFolderSongs: List<SongEntity>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    scanningFolderIds: Set<Long>,
    queuedFolderIds: Set<Long>,
    isSelectionMode: Boolean,
    selectedSongUris: Set<String>,
    swipeSelectionLabel: String,
    swipeSelectionSecondaryLabel: String?,
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
    navigator: DestinationsNavigator,
    selectionViewModel: SongSelectionViewModel,
    onFolderClick: (FolderEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit,
    onRefreshFolder: (FolderEntity) -> Unit,
    onIgnoredChange: (FolderEntity, Boolean) -> Unit,
    onShowSongMenu: (SongEntity) -> Unit,
    onTransitionFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(1f) }

    LaunchedEffect(transition) {
        if (transition != null) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = Motion.contentDuration,
                    easing = Motion.emphasized
                )
            )
            onTransitionFinished()
        } else {
            progress.snapTo(1f)
        }
    }

    val currentSnapshot = remember(
        currentFolderId,
        folders,
        folderTree,
        currentFolderSongs
    ) {
        buildFolderContentSnapshot(
            folderId = currentFolderId,
            folders = folders,
            folderTree = folderTree,
            currentFolderId = currentFolderId,
            currentFolderSongs = currentFolderSongs,
            useCurrentFolderSongs = true
        )
    }

    val activeTransition = transition
    val fromSnapshot = remember(
        activeTransition?.fromFolderId,
        folders,
        folderTree
    ) {
        activeTransition?.let {
            buildFolderContentSnapshot(
                folderId = it.fromFolderId,
                folders = folders,
                folderTree = folderTree,
                currentFolderId = currentFolderId,
                currentFolderSongs = currentFolderSongs,
                useCurrentFolderSongs = false
            )
        }
    }
    val toSnapshot = remember(
        activeTransition?.toFolderId,
        currentFolderId,
        folders,
        folderTree,
        currentFolderSongs
    ) {
        activeTransition?.let {
            buildFolderContentSnapshot(
                folderId = it.toFolderId,
                folders = folders,
                folderTree = folderTree,
                currentFolderId = currentFolderId,
                currentFolderSongs = currentFolderSongs,
                useCurrentFolderSongs = it.toFolderId == currentFolderId
            )
        }
    }

    HorizontalPager(
        state = pagerState,
        userScrollEnabled =  false,
        modifier = modifier
    ) { page ->
        FolderPageSlideContent(
            page = page,
            currentSnapshot = currentSnapshot,
            fromSnapshot = fromSnapshot,
            toSnapshot = toSnapshot,
            transition = activeTransition,
            progress = progress.value,
            folderTree = folderTree,
            scanningFolderIds = scanningFolderIds,
            queuedFolderIds = queuedFolderIds,
            isSelectionMode = isSelectionMode,
            selectedSongUris = selectedSongUris,
            swipeSelectionLabel = swipeSelectionLabel,
            swipeSelectionSecondaryLabel = swipeSelectionSecondaryLabel,
            topAppBarScrollBehavior = topAppBarScrollBehavior,
            navigator = navigator,
            selectionViewModel = selectionViewModel,
            onFolderClick = onFolderClick,
            onDeleteFolder = onDeleteFolder,
            onRefreshFolder = onRefreshFolder,
            onIgnoredChange = onIgnoredChange,
            onShowSongMenu = onShowSongMenu,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun FolderPageSlideContent(
    page: Int,
    currentSnapshot: FolderContentSnapshot,
    fromSnapshot: FolderContentSnapshot?,
    toSnapshot: FolderContentSnapshot?,
    transition: FolderContentTransition?,
    progress: Float,
    folderTree: FolderTree,
    scanningFolderIds: Set<Long>,
    queuedFolderIds: Set<Long>,
    isSelectionMode: Boolean,
    selectedSongUris: Set<String>,
    swipeSelectionLabel: String,
    swipeSelectionSecondaryLabel: String?,
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
    navigator: DestinationsNavigator,
    selectionViewModel: SongSelectionViewModel,
    onFolderClick: (FolderEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit,
    onRefreshFolder: (FolderEntity) -> Unit,
    onIgnoredChange: (FolderEntity, Boolean) -> Unit,
    onShowSongMenu: (SongEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.clipToBounds()
    ) {
        val density = LocalDensity.current
        val travelPx = with(density) { maxWidth.toPx() * 0.45f }

        if (transition == null || fromSnapshot == null || toSnapshot == null) {
            FolderPage(
                page = page,
                snapshot = currentSnapshot,
                folderTree = folderTree,
                scanningFolderIds = scanningFolderIds,
                queuedFolderIds = queuedFolderIds,
                isSelectionMode = isSelectionMode,
                selectedSongUris = selectedSongUris,
                swipeSelectionLabel = swipeSelectionLabel,
                swipeSelectionSecondaryLabel = swipeSelectionSecondaryLabel,
                topAppBarScrollBehavior = topAppBarScrollBehavior,
                navigator = navigator,
                selectionViewModel = selectionViewModel,
                onFolderClick = onFolderClick,
                onDeleteFolder = onDeleteFolder,
                onRefreshFolder = onRefreshFolder,
                onIgnoredChange = onIgnoredChange,
                onShowSongMenu = onShowSongMenu,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val fromOffset = if (transition.forward) {
                -travelPx * progress
            } else {
                travelPx * progress
            }
            val toOffset = if (transition.forward) {
                travelPx * (1f - progress)
            } else {
                -travelPx * (1f - progress)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                FolderPage(
                    page = page,
                    snapshot = fromSnapshot,
                    folderTree = folderTree,
                    scanningFolderIds = scanningFolderIds,
                    queuedFolderIds = queuedFolderIds,
                    isSelectionMode = isSelectionMode,
                    selectedSongUris = selectedSongUris,
                    swipeSelectionLabel = swipeSelectionLabel,
                    swipeSelectionSecondaryLabel = swipeSelectionSecondaryLabel,
                    topAppBarScrollBehavior = topAppBarScrollBehavior,
                    navigator = navigator,
                    selectionViewModel = selectionViewModel,
                    onFolderClick = onFolderClick,
                    onDeleteFolder = onDeleteFolder,
                    onRefreshFolder = onRefreshFolder,
                    onIgnoredChange = onIgnoredChange,
                    onShowSongMenu = onShowSongMenu,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = fromOffset
                            alpha = 1f - progress
                        }
                )

                FolderPage(
                    page = page,
                    snapshot = toSnapshot,
                    folderTree = folderTree,
                    scanningFolderIds = scanningFolderIds,
                    queuedFolderIds = queuedFolderIds,
                    isSelectionMode = isSelectionMode,
                    selectedSongUris = selectedSongUris,
                    swipeSelectionLabel = swipeSelectionLabel,
                    swipeSelectionSecondaryLabel = swipeSelectionSecondaryLabel,
                    topAppBarScrollBehavior = topAppBarScrollBehavior,
                    navigator = navigator,
                    selectionViewModel = selectionViewModel,
                    onFolderClick = onFolderClick,
                    onDeleteFolder = onDeleteFolder,
                    onRefreshFolder = onRefreshFolder,
                    onIgnoredChange = onIgnoredChange,
                    onShowSongMenu = onShowSongMenu,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = toOffset
                            alpha = progress
                        }
                )
            }
        }
    }
}

@Composable
private fun FolderPage(
    page: Int,
    snapshot: FolderContentSnapshot,
    folderTree: FolderTree,
    scanningFolderIds: Set<Long>,
    queuedFolderIds: Set<Long>,
    isSelectionMode: Boolean,
    selectedSongUris: Set<String>,
    swipeSelectionLabel: String,
    swipeSelectionSecondaryLabel: String?,
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
    navigator: DestinationsNavigator,
    selectionViewModel: SongSelectionViewModel,
    onFolderClick: (FolderEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit,
    onRefreshFolder: (FolderEntity) -> Unit,
    onIgnoredChange: (FolderEntity, Boolean) -> Unit,
    onShowSongMenu: (SongEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    when (page) {
        0 -> {
            FolderChildrenPage(
                folders = snapshot.childFolders,
                folderTree = folderTree,
                scanningFolderIds = scanningFolderIds,
                queuedFolderIds = queuedFolderIds,
                isSelectionMode = isSelectionMode,
                topAppBarScrollBehavior = topAppBarScrollBehavior,
                onFolderClick = onFolderClick,
                onDeleteFolder = onDeleteFolder,
                onRefreshFolder = onRefreshFolder,
                onIgnoredChange = onIgnoredChange,
                modifier = modifier
            )
        }

        1 -> {
            FolderCurrentSongsPage(
                songs = snapshot.songs,
                isSelectionMode = isSelectionMode,
                selectedSongUris = selectedSongUris,
                swipeSelectionLabel = swipeSelectionLabel,
                swipeSelectionSecondaryLabel = swipeSelectionSecondaryLabel,
                topAppBarScrollBehavior = topAppBarScrollBehavior,
                onSongClick = { song ->
                    navigator.navigate(
                        EditMetadataDestination(songFileUri = song.uri)
                    )
                },
                onToggleSelection = { song ->
                    selectionViewModel.toggleSelection(song.uri)
                },
                onSwipeSelection = { song ->
                    selectionViewModel.swipeSelect(song, snapshot.songs)
                },
                onShowSongMenu = onShowSongMenu,
                modifier = modifier
            )
        }
    }
}

private fun buildFolderContentSnapshot(
    folderId: Long,
    folders: List<FolderEntity>,
    folderTree: FolderTree,
    currentFolderId: Long,
    currentFolderSongs: List<SongEntity>,
    useCurrentFolderSongs: Boolean
): FolderContentSnapshot {
    val folder = folders.find { it.id == folderId }
    val node = folder?.let { folderTree.nodesById[it.id] }
    val songs = when {
        folderId == ROOT_FOLDER_ID -> emptyList()
        useCurrentFolderSongs && folderId == currentFolderId -> currentFolderSongs
        else -> emptyList()
    }

    return FolderContentSnapshot(
        folderId = folderId,
        childFolders = node?.childFolders ?: folderTree.rootFolders,
        songs = songs
    )
}

@Composable
private fun FolderChildrenPage(
    folders: List<FolderEntity>,
    folderTree: FolderTree,
    scanningFolderIds: Set<Long>,
    queuedFolderIds: Set<Long>,
    isSelectionMode: Boolean,
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
    onFolderClick: (FolderEntity) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit,
    onRefreshFolder: (FolderEntity) -> Unit,
    onIgnoredChange: (FolderEntity, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .libraryDetailScroll(topAppBarScrollBehavior)
            .fillMaxHeight(),
        contentPadding = PaddingValues(bottom = 12.dp + LocalLibraryBottomContentPadding.current),
        overscrollEffect = null
    ) {
        if (folders.isEmpty()) {
            item("empty") {
                IllustratedEmptyState(
                    kind = EmptyStateKind.Folders,
                    title = stringResource(R.string.empty_folders_title),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        items(
            items = folders,
            key = { folder -> "folder-${folder.id}" }
        ) { folder ->
            FolderListItem(
                folder = folder,
                node = folderTree.nodesById.getValue(folder.id),
                isScanning = folder.id in scanningFolderIds,
                isQueued = folder.id in queuedFolderIds,
                isSelectionMode = isSelectionMode,
                onClick = {
                    if (!isSelectionMode) {
                        onFolderClick(folder)
                    }
                },
                canRemove = folder.addedBySaf,
                onDelete = {
                    onDeleteFolder(folder)
                },
                onRefresh = {
                    onRefreshFolder(folder)
                },
                onIgnoredChange = { ignored ->
                    onIgnoredChange(folder, ignored)
                }
            )
        }
    }
}

@Composable
private fun FolderCurrentSongsPage(
    songs: List<SongEntity>,
    isSelectionMode: Boolean,
    selectedSongUris: Set<String>,
    swipeSelectionLabel: String,
    swipeSelectionSecondaryLabel: String?,
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
    onSongClick: (SongEntity) -> Unit,
    onToggleSelection: (SongEntity) -> Unit,
    onSwipeSelection: (SongEntity) -> Unit,
    onShowSongMenu: (SongEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier
            .libraryDetailScroll(topAppBarScrollBehavior)
            .fillMaxHeight(),
        state = listState,
        contentPadding = PaddingValues(bottom = 12.dp + LocalLibraryBottomContentPadding.current),
        overscrollEffect = null
    ) {
        if (songs.isEmpty()) {
            item("empty") {
                IllustratedEmptyState(
                    kind = EmptyStateKind.Songs,
                    title = stringResource(R.string.empty_songs_title),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        items(
            items = songs,
            key = { song ->
                song.uri.takeIf { it.isNotBlank() && it != "0" } ?: "song-${song.id}"
            }
        ) { song ->
            SelectableLibraryItem(
                itemKey = song.uri,
                modifier = Modifier.animateItem(),
                isSelectionMode = isSelectionMode,
                isSelected = selectedSongUris.contains(song.uri),
                swipeSelectionLabel = swipeSelectionLabel,
                swipeSelectionSecondaryLabel = swipeSelectionSecondaryLabel,
                onClick = {
                    onSongClick(song)
                },
                onToggleSelection = {
                    onToggleSelection(song)
                },
                onSwipeSelection = {
                    onSwipeSelection(song)
                }
            ) {
                SongListItemContent(
                    song = song,
                    trailingContent = {
                        Box(modifier = Modifier.padding(end = 8.dp)) {
                            SongListItemActions(
                                isSelectionMode = isSelectionMode,
                                isSelected = selectedSongUris.contains(song.uri),
                                onToggleSelection = {
                                    onToggleSelection(song)
                                },
                                onShowMenu = {
                                    onShowSongMenu(song)
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FolderPathHeader(
    breadcrumbs: List<FolderBreadcrumb>,
    folderCount: Int,
    songCount: Int,
    onBreadcrumbClick: (Long) -> Unit
) {

    Card(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    LucideIcons.Folder,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                breadcrumbs.forEachIndexed { index, breadcrumb ->
                    if (index > 0) {
                        Icon(
                            LucideIcons.ArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val isCurrent = index == breadcrumbs.lastIndex
                    Text(
                        text = breadcrumb.label,
                        style = if (isCurrent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clickable(enabled = !isCurrent) { onBreadcrumbClick(breadcrumb.folderId) }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.folder_browser_count_format, folderCount, songCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FolderListItem(
    folder: FolderEntity,
    node: FolderTreeNode,
    isScanning: Boolean,
    isQueued: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    canRemove: Boolean,
    onDelete: () -> Unit,
    onRefresh: () -> Unit,
    onIgnoredChange: (Boolean) -> Unit
) {
    val folderName = folder.path.substringAfterLast("/").ifBlank { folder.path }
    val isBusy = isScanning || isQueued || isSelectionMode
    val statusText = when {
        isScanning -> {
            stringResource(R.string.folder_scanning)
        }

        isQueued -> {
            stringResource(R.string.folder_scan_queued)
        }

        folder.isIgnored -> {
            stringResource(
                R.string.folder_hidden_child_song_count_format,
                node.childFolders.size,
                node.subtreeSongCount
            )
        }

        else -> {
            stringResource(
                R.string.folder_child_song_count_format,
                node.childFolders.size,
                node.subtreeSongCount
            )
        }
    }

    var folderMenuExpanded by remember { mutableStateOf(false) }

    com.lonx.lyrico.ui.components.FolderManagementItem(
        name = folderName,
        path = folder.path,
        status = statusText,
        actions = {
            Box {
                IconButton(
                    enabled = !isBusy,
                    onClick = { folderMenuExpanded = true }
                ) {
                    Icon(LucideIcons.EllipsisVertical, stringResource(R.string.cd_more_actions))
                }
                DropdownMenu(
                    expanded = folderMenuExpanded,
                    onDismissRequest = { folderMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (folder.isIgnored) {
                                    stringResource(R.string.folder_action_show)
                                } else {
                                    stringResource(R.string.folder_action_hide)
                                }
                            )
                        },
                        leadingIcon = {
                            PreferenceIcon(
                                if (folder.isIgnored) LucideIcons.Eye else LucideIcons.EyeOff,
                                PreferenceIconTone.Appearance,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = {
                            folderMenuExpanded = false
                            onIgnoredChange(!folder.isIgnored)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_refresh_folder)) },
                        leadingIcon = {
                            PreferenceIcon(
                                LucideIcons.RefreshCw,
                                PreferenceIconTone.Tools,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        enabled = !isBusy,
                        onClick = {
                            folderMenuExpanded = false
                            if (!isBusy) onRefresh()
                        }
                    )
                    if (canRemove) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.folder_action_remove)) },
                            leadingIcon = {
                                PreferenceIcon(
                                    LucideIcons.Folder,
                                    PreferenceIconTone.Library,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            enabled = !isBusy,
                            onClick = {
                                folderMenuExpanded = false
                                if (!isBusy) onDelete()
                            }
                        )
                    }
                }
            }
        },
        enabled = !isBusy,
        isLoading = isScanning,
        onClick = onClick
    )
}

private data class FolderTree(
    val nodesById: Map<Long, FolderTreeNode>,
    val rootFolders: List<FolderEntity>
)

private data class FolderTreeNode(
    val folder: FolderEntity,
    val parentId: Long?,
    val childFolders: List<FolderEntity>,
    val directSongCount: Int,
    val subtreeSongCount: Int
)

private fun buildFolderBreadcrumbs(
    currentFolder: FolderEntity?,
    currentNode: FolderTreeNode?,
    folderTree: FolderTree,
    rootLabel: String
): List<FolderBreadcrumb> {
    if (currentFolder == null || currentNode == null) {
        return listOf(FolderBreadcrumb(ROOT_FOLDER_ID, rootLabel))
    }

    val ancestors = mutableListOf<FolderTreeNode>()
    var node: FolderTreeNode? = currentNode
    while (node != null) {
        ancestors += node
        node = node.parentId?.let { parentId -> folderTree.nodesById[parentId] }
    }

    return buildList {
        add(FolderBreadcrumb(ROOT_FOLDER_ID, rootLabel))
        ancestors.asReversed().forEach { ancestor ->
            add(
                FolderBreadcrumb(
                    folderId = ancestor.folder.id,
                    label = ancestor.folder.path.substringAfterLast("/")
                        .ifBlank { ancestor.folder.path }
                )
            )
        }
    }
}

private fun buildFolderTree(
    folders: List<FolderEntity>
): FolderTree {
    val normalizedPaths = folders.associate { folder ->
        folder.id to folder.path.normalizeFolderPath()
    }

    val directSongCounts = folders.associate { folder ->
        folder.id to folder.songCount
    }

    val parentIds = folders.associate { folder ->
        val path = normalizedPaths.getValue(folder.id)
        val parent = folders
            .filter { candidate ->
                val candidatePath = normalizedPaths.getValue(candidate.id)
                candidate.id != folder.id &&
                        candidatePath.isNotBlank() &&
                        path.startsWith("$candidatePath/")
            }
            .maxByOrNull { candidate ->
                normalizedPaths.getValue(candidate.id).length
            }

        folder.id to parent?.id
    }

    val childrenByParentId = folders
        .groupBy { folder -> parentIds.getValue(folder.id) }
        .mapValues { (_, children) ->
            children.sortedBy { it.path.normalizeFolderPath() }
        }

    val subtreeSongCounts = mutableMapOf<Long, Int>()

    fun subtreeSongCount(folderId: Long): Int {
        return subtreeSongCounts.getOrPut(folderId) {
            directSongCounts[folderId].orZero() +
                    childrenByParentId[folderId].orEmpty().sumOf { child ->
                        subtreeSongCount(child.id)
                    }
        }
    }

    val nodesById = folders.associate { folder ->
        folder.id to FolderTreeNode(
            folder = folder,
            parentId = parentIds.getValue(folder.id),
            childFolders = childrenByParentId[folder.id].orEmpty(),
            directSongCount = directSongCounts[folder.id].orZero(),
            subtreeSongCount = subtreeSongCount(folder.id)
        )
    }

    return FolderTree(
        nodesById = nodesById,
        rootFolders = childrenByParentId[null].orEmpty()
    )
}

private fun Int?.orZero(): Int = this ?: 0

private fun String.normalizeFolderPath(): String {
    return replace('\\', '/')
        .trim()
        .trimEnd('/')
}
