
package com.lonx.lyrico.screens

import android.content.ClipData
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.lonx.lyrico.data.model.log.AppLogLevel
import com.lonx.lyrico.data.model.log.AppLogType
import com.lonx.lyrico.data.model.log.LogRetentionOption
import com.lonx.lyrico.data.model.entity.AppLogEntity
import com.lonx.lyrico.ui.components.scaffoldContentPadding
import com.lonx.lyrico.viewmodel.AppLogEvent
import com.lonx.lyrico.viewmodel.AppLogViewModel
import com.lonx.lyrico.ui.components.sdw.SdwBottomSheet
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import com.lonx.lyrico.ui.components.sdw.WindowDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
@Composable
@Destination<RootGraph>(route = "app_logs")
fun AppLogScreen(
    navigator: DestinationsNavigator
) {
    val viewModel: AppLogViewModel = koinViewModel()
    val clipboardManager = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topAppBarScrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedLevel by remember { mutableStateOf<AppLogLevel?>(null) }
    var selectedType by remember { mutableStateOf<AppLogType?>(null) }
    var selectedLog by remember { mutableStateOf<AppLogEntity?>(null) }
    var selectedLogIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showDetailSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var pendingDeleteIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var pendingExportIds by remember { mutableStateOf<List<Long>?>(null) }
    val copiedMessage = stringResource(R.string.msg_copied_to_clipboard)
    val logRetentionOption by viewModel.logRetentionOption.collectAsState()
    val logRetentionItems = LogRetentionOption.entries.map { stringResource(it.labelRes) }
    val selectedLogRetentionIndex = LogRetentionOption.entries.indexOf(logRetentionOption).coerceAtLeast(0)
    val filteredLogs by remember(viewModel, selectedLevel, selectedType) {
        viewModel.observeLogs(selectedLevel, selectedType)
    }.collectAsState(initial = emptyList())
    var preparingDelete by remember { mutableStateOf(false) }
    val prepareDelete: () -> Unit = {
        if (!preparingDelete) {
            preparingDelete = true
            val level = selectedLevel
            val type = selectedType
            scope.launch {
                try {
                    pendingDeleteIds = viewModel.getMatchingIds(level, type)
                    showDeleteConfirmDialog = pendingDeleteIds.isNotEmpty()
                } finally {
                    preparingDelete = false
                }
            }
        }
    }

    val levelItems = remember {
        AppLogLevel.entries.map { it.labelRes }
    }
    val typeItems = remember {
        AppLogType.entries.map { it.labelRes }
    }
    val selectedLevelIndex = remember(selectedLevel) {
        selectedLevel?.let { level -> AppLogLevel.entries.indexOf(level) + 1 } ?: 0
    }
    val selectedTypeIndex = remember(selectedType) {
        selectedType?.let { type -> AppLogType.entries.indexOf(type) + 1 } ?: 0
    }
    val salt = false

    LaunchedEffect(filteredLogs) {
        val visibleIds = filteredLogs.mapTo(mutableSetOf()) { it.id }
        selectedLogIds = selectedLogIds.intersect(visibleIds)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        uri?.let { viewModel.exportLogs(context, it, pendingExportIds) }
        pendingExportIds = null
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppLogEvent.ShowMessage -> {
                    event.message.asString(context)?.let { text ->
                        snackbarHostState.showSnackbar(text)
                    }
                }
            }
        }
    }

    val topBarBackdrop = rememberBarBlurBackdrop()
    LibraryDetailPage(
        title = stringResource(R.string.app_log_title),
        onBack = { navigator.popBackStack() },
        // SaltUI 3.0.0-beta02 没有 Snackbar，Salt 侧改用平台 Toast（与项目其它 Salt 路径一致）。
        snackbarHost = { SnackbarHost(snackbarHostState) },
        miuixTopBar = {
            BlurredTopBar(backdrop = topBarBackdrop) {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_log_title), style = MaterialTheme.typography.titleLarge) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                    windowInsets = WindowInsets(0.dp),
                    navigationIcon = {
                        IconButton(onClick = { navigator.popBackStack() }) {
                            Icon(LucideIcons.ArrowLeft, stringResource(R.string.action_back))
                        }
                    },
                    actions = {
                        IconButton(
                            enabled = filteredLogs.isNotEmpty(),
                            onClick = {
                                pendingExportIds = filteredLogs.map { it.id }
                                exportLauncher.launch("lyrico_log_${System.currentTimeMillis()}.log")
                            }
                        ) {
                            Icon(
                                imageVector = LucideIcons.Share2,
                                contentDescription = stringResource(R.string.action_export_logs)
                            )
                        }
                        IconButton(
                            enabled = filteredLogs.isNotEmpty(),
                            onClick = {
                                prepareDelete()
                            }
                        ) {
                            Icon(
                                imageVector = LucideIcons.Trash,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    scrollBehavior = topAppBarScrollBehavior
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .blurSource(topBarBackdrop)
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
            contentPadding = scaffoldContentPadding(
                paddingValues = paddingValues,
                topExtra = 8.dp,
                bottomExtra = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item("filters") {
                val levelLabels = listOf(stringResource(R.string.app_log_filter_all)) +
                        levelItems.map { stringResource(it) }
                val typeLabels = listOf(stringResource(R.string.app_log_filter_all)) +
                        typeItems.map { stringResource(it) }
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    DropdownPreferenceRow(
                        leadingIcon = { PreferenceIcon(LucideIcons.ListFilter, PreferenceIconTone.Appearance) },
                        title = stringResource(R.string.app_log_filter_level),
                        items = levelLabels,
                        selectedIndex = selectedLevelIndex,
                        onSelectedIndexChange = { index ->
                            selectedLevel = if (index == 0) null else AppLogLevel.entries[index - 1]
                        }
                    )
                    DropdownPreferenceRow(
                        leadingIcon = { PreferenceIcon(LucideIcons.Logs, PreferenceIconTone.Tools) },
                        title = stringResource(R.string.app_log_filter_type),
                        items = typeLabels,
                        selectedIndex = selectedTypeIndex,
                        onSelectedIndexChange = { index ->
                            selectedType = if (index == 0) null else AppLogType.entries[index - 1]
                        }
                    )
                    DropdownPreferenceRow(
                        leadingIcon = { PreferenceIcon(LucideIcons.Calendar, PreferenceIconTone.Library) },
                        title = stringResource(R.string.log_retention_title),
                        summary = stringResource(R.string.log_retention_summary),
                        items = logRetentionItems,
                        selectedIndex = selectedLogRetentionIndex,
                        onSelectedIndexChange = { index ->
                            viewModel.setLogRetentionOption(LogRetentionOption.entries[index])
                        }
                    )
                }
            }

            if (filteredLogs.isEmpty()) {
                item("empty") {
                    IllustratedEmptyState(
                        kind = EmptyStateKind.Logs,
                        title = stringResource(R.string.app_log_empty),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                items(filteredLogs, key = { it.id }) { log ->
                    AppLogItem(
                        log = log,
                        onClick = {
                            selectedLog = log
                            showDetailSheet = true
                        }
                    )
                }
            }
        }
    }

    AppLogDetailSheet(
        show = showDetailSheet,
        log = selectedLog,
        onDismiss = { showDetailSheet = false },
        onDismissFinished = {
            showDetailSheet = false
            selectedLog = null
        },
        onCopy = { log ->
            scope.launch {
                val clipData = ClipData.newPlainText("copy log", log.formatForCopy())
                val clipEntry = ClipEntry(clipData)
                clipboardManager.setClipEntry(clipEntry)
                snackbarHostState.showSnackbar(copiedMessage)
            }
            showDetailSheet = false
        }
    )

    WindowDialog(
        show = showDeleteConfirmDialog,
        title = stringResource(R.string.app_log_delete_title),
        onDismissRequest = { showDeleteConfirmDialog = false }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.app_log_delete_message, pendingDeleteIds.size),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.cancel)) }
                Spacer(modifier = Modifier.width(20.dp))
                TextButton(
                    onClick = {
                        viewModel.deleteLogs(pendingDeleteIds)
                        selectedLogIds = emptySet()
                        pendingDeleteIds = emptyList()
                        showDeleteConfirmDialog = false
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) { Text(stringResource(R.string.confirm)) }
            }
        }
    }
}

@Composable
private fun DropdownPreferenceRow(
    leadingIcon: @Composable (() -> Unit)? = null,
    title: String,
    summary: String? = null,
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Box(modifier = Modifier.padding(end = 12.dp)) { leadingIcon() }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box {
            Text(
                text = items.getOrNull(selectedIndex).orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                items.forEachIndexed { index, label ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSelectedIndexChange(index)
                            expanded = false
                        },
                        trailingIcon = if (index == selectedIndex) {
                            { Icon(LucideIcons.Check, contentDescription = null) }
                        } else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppLogItem(
    log: AppLogEntity,
    onClick: () -> Unit
) {
    val salt = false

    Card(
        modifier = Modifier.padding(horizontal = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SeverityBadge(log.level)
                    Text(
                        text = stringResource(log.type.labelRes),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    style = MaterialTheme.typography.bodyMedium,
                    text = formatTime(log.createdAt),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = log.tag,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            log.relatedId?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SeverityBadge(level: AppLogLevel) {
    val salt = false
    val background = when (level) {
        AppLogLevel.ERROR -> MaterialTheme.colorScheme.error
        AppLogLevel.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        AppLogLevel.INFO -> MaterialTheme.colorScheme.primary
        AppLogLevel.DEBUG -> MaterialTheme.colorScheme.secondaryContainer
    }
    val content = when (level) {
        AppLogLevel.ERROR -> MaterialTheme.colorScheme.onError
        AppLogLevel.WARNING -> MaterialTheme.colorScheme.onSurface
        AppLogLevel.INFO -> MaterialTheme.colorScheme.onPrimary
        AppLogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = stringResource(level.labelRes),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        color = content,
        fontWeight = FontWeight.Bold,
        maxLines = 1
    )
}

@Composable
private fun AppLogDetailSheet(
    show: Boolean,
    log: AppLogEntity?,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onCopy: (AppLogEntity) -> Unit
) {

    SdwBottomSheet(
        show = show,
        enableNestedScroll = false,
        title = stringResource(R.string.app_log_detail_title),
        tools = log?.let { current ->
            {
                IconButton(onClick = { onCopy(current) }) {
                    Icon(
                        imageVector = LucideIcons.Copy,
                        contentDescription = stringResource(R.string.action_copy_log)
                    )
                }
            }
        },
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished
    ) {
        val current = log ?: return@SdwBottomSheet
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            AppLogDetailContent(current)
        }
    }
}

@Composable
private fun AppLogDetailContent(log: AppLogEntity) {
    val salt = false
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        DetailRow(
            stringResource(R.string.app_log_field_time),
            formatDateTime(log.createdAt)
        )
        DetailRow(
            stringResource(R.string.app_log_field_level),
            stringResource(log.level.labelRes)
        )
        DetailRow(
            stringResource(R.string.app_log_field_type),
            stringResource(log.type.labelRes)
        )
        DetailRow(stringResource(R.string.app_log_field_tag), log.tag)
        log.relatedId?.let {
            DetailRow(stringResource(R.string.app_log_field_related), it)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.app_log_field_message),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        SelectionContainer {
            Text(
                text = log.message,
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        log.detail?.takeIf { it.isNotBlank() }?.let { detail ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.app_log_field_detail),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            SelectionContainer {
                Text(
                    text = detail,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    val salt = false
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        SelectionContainer {
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun AppLogEntity.formatForCopy(): String = buildString {
    appendLine("${formatDateTime(createdAt)} [$level/$type] $tag")
    appendLine(message)
    relatedId?.let { appendLine("Related: $it") }
    detail?.takeIf { it.isNotBlank() }?.let {
        appendLine()
        appendLine(it)
    }
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

private fun formatDateTime(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

private fun Set<Long>.toggle(id: Long): Set<Long> =
    if (contains(id)) this - id else this + id
