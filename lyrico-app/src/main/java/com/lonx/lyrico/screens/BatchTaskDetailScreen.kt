
package com.lonx.lyrico.screens

import com.lonx.lyrico.ui.components.icons.LucideIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.library.EmptyStateKind
import com.lonx.lyrico.ui.components.library.IllustratedEmptyState
import com.lonx.lyrico.ui.components.library.LibraryDetailPage
import com.lonx.lyrico.data.model.BatchTaskStatus
import com.lonx.lyrico.data.model.entity.BatchTaskItemEntity
import com.lonx.lyrico.ui.components.scaffoldTopHorizontalPadding
import com.lonx.lyrico.viewmodel.BatchTaskDetailViewModel

import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.EditMetadataDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.lonx.lyrico.ui.components.blur.BlurredTopBar
import com.lonx.lyrico.ui.components.blur.blurSource
import com.lonx.lyrico.ui.components.blur.rememberBarBlurBackdrop
import com.lonx.lyrico.ui.components.library.libraryDetailScroll

enum class TaskDetailTab(val labelRes: Int) {
    SUCCEEDED(R.string.batch_task_status_succeeded),
    FAILED(R.string.batch_task_status_failed),
    SKIPPED(R.string.batch_task_status_skipped)
}

@Destination<RootGraph>(route = "batch_task_detail")
@Composable
fun BatchTaskDetailScreen(
    taskId: String,
    navigator: DestinationsNavigator
) {
    val viewModel: BatchTaskDetailViewModel = koinViewModel { parametersOf(taskId) }
    val task by viewModel.task.collectAsState()
    val items by viewModel.items.collectAsState()
    val topAppBarScrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val topBarBackdrop = rememberBarBlurBackdrop()
    val scope = rememberCoroutineScope()
    val salt = false

    val tabs = remember { TaskDetailTab.entries }
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val tabLabels = tabs.map { stringResource(it.labelRes) }

    LibraryDetailPage(
        title = stringResource(R.string.batch_task_detail_title),
        onBack = { navigator.popBackStack() },
        miuixTopBar = {
            BlurredTopBar(backdrop = topBarBackdrop) {
                TopAppBar(
                    title = { Text(stringResource(R.string.batch_task_detail_title), style = MaterialTheme.typography.titleLarge) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                    windowInsets = WindowInsets(0.dp),
                    navigationIcon = {
                        IconButton(onClick = { navigator.popBackStack() }) {
                            Icon(LucideIcons.ArrowLeft, stringResource(R.string.action_back))
                        }
                    },
                    actions = {
                    val t = task
                    if (t != null && (t.status == BatchTaskStatus.RUNNING || t.status == BatchTaskStatus.QUEUED)) {
                        IconButton(
                            onClick = { viewModel.cancelTask() }
                        ) {
                            Icon(
                                painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel),
                                contentDescription = stringResource(R.string.action_close),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
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
            val t = task
            if (t != null) {
                val typeLabel = stringResource(t.type.labelRes)
                val statusLabel = stringResource(t.status.labelRes)
                val startedAt = t.startedAt
                val finishedAt = t.finishedAt
                val durationSecs = if (startedAt != null && finishedAt != null) {
                    (finishedAt - startedAt) / 1000.0
                } else null

                val summary = buildString {
                    append("$typeLabel / $statusLabel")

                    append("\n")
                    append(
                        stringResource(
                            R.string.batch_match_stat_format,
                            t.successCount,
                            t.failureCount,
                            t.skippedCount
                        )
                    )

                    if (durationSecs != null) {
                        append("\n")
                        append(
                            stringResource(
                                R.string.batch_match_duration_format,
                                durationSecs
                            )
                        )
                    }
                }
                val progressTitle = stringResource(R.string.batch_task_detail_progress, t.current, t.total)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = progressTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                
            }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tabLabels.forEachIndexed { index, label ->
                            Tab(
                                selected = index == pagerState.currentPage,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                text = { Text(label) }
                            )
                        }
                    }
                }
            
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize(),
                key = { index -> tabs[index].name }
            ) { pageIndex ->
                val currentTab = tabs[pageIndex]
                val filteredItems = remember(items, currentTab) {
                    when (currentTab) {
                        TaskDetailTab.SUCCEEDED -> items.filter { it.status == BatchTaskStatus.SUCCEEDED }
                        TaskDetailTab.FAILED -> items.filter { it.status == BatchTaskStatus.FAILED }
                        TaskDetailTab.SKIPPED -> items.filter { it.status == BatchTaskStatus.SKIPPED }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (filteredItems.isEmpty()) {
                        item {
                            IllustratedEmptyState(
                                kind = EmptyStateKind.Tasks,
                                title = stringResource(R.string.batch_task_detail_no_records),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        items(
                            items = filteredItems,
                            key = { it.itemId }
                        ) { item ->
                            BatchTaskItemCard(
                                item = item,
                                onClick = {
                                    navigator.navigate(EditMetadataDestination(item.songUri))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchTaskItemCard(
    item: BatchTaskItemEntity,
    onClick: () -> Unit
) {
    val summary = buildString {
        append(item.filePath ?: item.songUri)
        if (item.errorMessage != null) {
            append("\n")
            append(item.errorMessage)
        }
    }

    Card(
        modifier = Modifier.padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        ArrowRow(
            title = item.fileName,
            summary = summary,
            onClick = onClick
        )
    }
}

@Composable
private fun ArrowRow(
    title: String,
    summary: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = LucideIcons.ArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
