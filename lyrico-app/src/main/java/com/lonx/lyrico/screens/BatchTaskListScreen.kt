
package com.lonx.lyrico.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.ui.components.blur.BlurredTopBar
import com.lonx.lyrico.ui.components.blur.blurSource
import com.lonx.lyrico.ui.components.blur.rememberBarBlurBackdrop
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.icons.LucideIcons
import com.lonx.lyrico.ui.components.icons.PreferenceIcon
import com.lonx.lyrico.ui.components.icons.PreferenceIconTone
import com.lonx.lyrico.ui.components.library.EmptyStateKind
import com.lonx.lyrico.ui.components.library.IllustratedEmptyState
import com.lonx.lyrico.ui.components.library.LibraryDetailPage
import com.lonx.lyrico.data.model.BatchTaskStatus
import com.lonx.lyrico.data.model.BatchTaskType
import com.lonx.lyrico.data.model.entity.BatchTaskEntity
import com.lonx.lyrico.ui.components.scaffoldContentPadding
import com.lonx.lyrico.viewmodel.BatchTaskListViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.BatchTaskDetailDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.MaterialTheme
@Destination<RootGraph>(route = "batch_task_list")
@Composable
fun BatchTaskListScreen(
    navigator: DestinationsNavigator
) {
    val viewModel: BatchTaskListViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val topAppBarScrollBehavior = MiuixScrollBehavior()
    val salt = false

    val typeItems = remember {
        BatchTaskType.entries.map { it.labelRes }
    }
    val statusItems = remember {
        BatchTaskStatus.entries.map { it.labelRes }
    }
    val selectedTypeIndex = remember(uiState.filterType) {
        uiState.filterType?.let { type -> BatchTaskType.entries.indexOf(type) + 1 } ?: 0
    }
    val selectedStatusIndex = remember(uiState.filterStatus) {
        uiState.filterStatus?.let { status -> BatchTaskStatus.entries.indexOf(status) + 1 } ?: 0
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf<String?>(null) }
    var selectedTaskIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingDeleteTaskIds by remember { mutableStateOf<List<String>>(emptyList()) }

    val deletableFilteredTaskIds = remember(uiState.filteredTasks) {
        uiState.filteredTasks
            .filterNot { it.status == BatchTaskStatus.RUNNING || it.status == BatchTaskStatus.QUEUED }
            .map { it.taskId }
    }

    LaunchedEffect(uiState.filteredTasks) {
        val visibleIds = uiState.filteredTasks.mapTo(mutableSetOf()) { it.taskId }
        selectedTaskIds = selectedTaskIds.intersect(visibleIds)
    }

    val deleteMessage = if (pendingDeleteTaskIds.size > 1) {
        stringResource(
            R.string.batch_task_delete_selected_message,
            pendingDeleteTaskIds.size
        )
    } else {
        stringResource(R.string.batch_task_delete_message)
    }

        WindowDialog(
            title = stringResource(R.string.batch_task_delete_title),
            show = showDeleteDialog,
            onDismissRequest = {
                showDeleteDialog = false
            },
            onDismissFinished = {
                showDeleteDialog = false
                selectedTaskId = null
                pendingDeleteTaskIds = emptyList()
            }
        ) {
            Column {
                Text(
                    text = deleteMessage,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = {
                            showDeleteDialog = false
                            selectedTaskId = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        text = stringResource(R.string.confirm),
                        onClick = {
                            if (pendingDeleteTaskIds.isNotEmpty()) {
                                viewModel.deleteTasks(pendingDeleteTaskIds)
                            } else {
                                selectedTaskId?.let(viewModel::deleteTask)
                            }
                            selectedTaskIds = emptySet()
                            showDeleteDialog = false
                            selectedTaskId = null
                            pendingDeleteTaskIds = emptyList()
                        },
                        modifier = Modifier.weight(1f),
                        colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }

        WindowDialog(
            title = stringResource(R.string.batch_task_clear_title),
            show = showClearDialog,
            onDismissRequest = { showClearDialog = false }
        ) {
            Column {
                Text(
                    text = stringResource(R.string.batch_task_clear_message),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TextButton(
                        text = stringResource(R.string.cancel),
                        onClick = { showClearDialog = false },
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        text = stringResource(R.string.confirm),
                        onClick = {
                            viewModel.deleteTasks(deletableFilteredTaskIds)
                            selectedTaskIds = emptySet()
                            showClearDialog = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = top.yukonga.miuix.kmp.basic.ButtonDefaults.textButtonColorsPrimary()
                    )
                }
            }
        }
    
    val canClearFinishedTasks = deletableFilteredTaskIds.isNotEmpty()
    val topBarBackdrop = rememberBarBlurBackdrop()
    LibraryDetailPage(
        title = stringResource(R.string.batch_task_list_title),
        onBack = { navigator.popBackStack() },
        miuixTopBar = {
            BlurredTopBar(backdrop = topBarBackdrop) {
                SmallTopAppBar(
                    color = Color.Transparent,
                    defaultWindowInsetsPadding = false,
                    title = stringResource(R.string.batch_task_list_title),
                    navigationIcon = {
                        IconButton(
                            onClick = { navigator.popBackStack() }
                        ) {
                            Icon(
                                imageVector = LucideIcons.ArrowLeft,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            enabled = canClearFinishedTasks,
                            onClick = {
                                if (canClearFinishedTasks) {
                                    showClearDialog = true
                                }
                            }
                        ) {
                            Icon(
                                LucideIcons.Trash,
                                contentDescription = stringResource(R.string.batch_task_clear_title),
                                tint = if (canClearFinishedTasks) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    },
                    scrollBehavior = topAppBarScrollBehavior
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = (Modifier
                .blurSource(topBarBackdrop)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection))
                .fillMaxHeight(),
            contentPadding = scaffoldContentPadding(
                paddingValues = paddingValues,
                bottomExtra = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            overscrollEffect = null,
        ) {
            item {
                val typeLabels = listOf(stringResource(R.string.batch_task_filter_all)) +
                        typeItems.map { stringResource(it) }
                val statusLabels = listOf(stringResource(R.string.batch_task_filter_all)) +
                        statusItems.map { stringResource(it) }
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    WindowDropdownPreference(
                        startAction = { PreferenceIcon(LucideIcons.ListFilter, PreferenceIconTone.Appearance) },
                        title = stringResource(R.string.batch_task_filter_type),
                        items = typeLabels,
                        selectedIndex = selectedTypeIndex,
                        onSelectedIndexChange = { index ->
                            viewModel.setFilterType(
                                if (index == 0) null else BatchTaskType.entries[index - 1]
                            )
                        }
                    )
                    WindowDropdownPreference(
                        startAction = { PreferenceIcon(LucideIcons.ListChecks, PreferenceIconTone.Tools) },
                        title = stringResource(R.string.batch_task_filter_status),
                        items = statusLabels,
                        selectedIndex = selectedStatusIndex,
                        onSelectedIndexChange = { index ->
                            viewModel.setFilterStatus(
                                if (index == 0) null else BatchTaskStatus.entries[index - 1]
                            )
                        }
                    )
                }
            }
            if (uiState.filteredTasks.isEmpty()) {
                item {
                    IllustratedEmptyState(
                        kind = EmptyStateKind.Tasks,
                        title = stringResource(R.string.batch_task_no_tasks),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                items(
                    items = uiState.filteredTasks,
                    key = { it.taskId }
                ) { task ->
                    BatchTaskCard(
                        modifier = Modifier.animateItem(),
                        task = task,
                        formattedDate = dateFormat.format(Date(task.createdAt)),
                        onClick = {
                            navigator.navigate(BatchTaskDetailDestination(task.taskId))
                        },
                        onDeleteClick = {
                            selectedTaskId = task.taskId
                            pendingDeleteTaskIds = emptyList()
                            showDeleteDialog = true
                        },
                        onCancelClick = {
                            viewModel.cancelTask(task.taskId)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BatchTaskCard(
    modifier: Modifier = Modifier,
    task: BatchTaskEntity,
    formattedDate: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val isRunning =
        task.status == BatchTaskStatus.RUNNING || task.status == BatchTaskStatus.QUEUED

    Card(
        modifier = modifier.padding(horizontal = 12.dp)
    ) {
        BasicComponent(
            onClick = onClick,
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            endActions = {
                if (isRunning) {
                    IconButton(onClick = onCancelClick) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel),
                            contentDescription = stringResource(R.string.action_close),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            LucideIcons.Trash,
                            contentDescription = stringResource(R.string.common_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        ) {
            BatchTaskCardDetails(
                task = task,
                formattedDate = formattedDate,
                salt = false,
                isRunning = isRunning
            )
        }
    }
}

/** 卡片正文：两引擎共用同一组字段与顺序，只换排版语义。 */
@Composable
private fun BatchTaskCardDetails(
    task: BatchTaskEntity,
    formattedDate: String,
    salt: Boolean,
    isRunning: Boolean,
) {
    val typeLabel = stringResource(task.type.labelRes)
    val statusLabel = stringResource(task.status.labelRes)
    val durationSecs = if (task.startedAt != null && task.finishedAt != null) {
        (task.finishedAt - task.startedAt) / 1000.0
    } else null
    val finished = task.status == BatchTaskStatus.SUCCEEDED ||
            task.status == BatchTaskStatus.FAILED ||
            task.status == BatchTaskStatus.CANCELLED

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formattedDate,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.batch_task_type_label) + typeLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.batch_task_status_label) + statusLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (finished) {
                Text(
                    text = stringResource(
                        R.string.batch_match_stat_format,
                        task.successCount,
                        task.failureCount,
                        task.skippedCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else if (isRunning) {
                Text(
                    text = "${task.current}/${task.total}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (durationSecs != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.batch_match_duration_format,
                        durationSecs
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        return
    
    Column {
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.batch_task_type_label) + typeLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.batch_task_status_label) + statusLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (finished) {
            Text(
                text = stringResource(
                    R.string.batch_match_stat_format,
                    task.successCount,
                    task.failureCount,
                    task.skippedCount
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (isRunning) {
            Text(
                text = "${task.current}/${task.total}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (durationSecs != null) {
            Text(
                text = stringResource(
                    R.string.batch_match_duration_format,
                    durationSecs
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
