package com.lonx.lyrico.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.ChipGrid
import com.lonx.lyrico.ui.components.ManagedChip
import com.lonx.lyrico.ui.components.scaffoldContentPadding
import com.lonx.lyrico.viewmodel.CustomTagKeyError
import com.lonx.lyrico.viewmodel.CustomTagManagementViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

/**
 * 自定义标签管理。
 *
 * 列表里的胶囊就是会显示在单曲编辑页和批量编辑页的标签，只有两个动作：
 * 添加、删除。没有"开关"概念——标签要么在列表里，要么被删掉。
 */
@Composable
@Destination<RootGraph>(route = "custom_tag_management")
fun CustomTagManagementScreen(
    navigator: DestinationsNavigator,
) {
    val viewModel: CustomTagManagementViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = stringResource(R.string.custom_tag_management_title),
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(
                            MiuixIcons.Back,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .fillMaxHeight(),
            contentPadding = scaffoldContentPadding(
                paddingValues = paddingValues,
                bottomExtra = 12.dp
            ),
            overscrollEffect = null,
        ) {
            item(key = "tags") {
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    if (uiState.visibleKeys.isEmpty()) {
                        FootnoteText(stringResource(R.string.custom_tag_empty))
                    } else {
                        ChipGrid(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            uiState.visibleKeys.forEach { key ->
                                ManagedChip(
                                    text = key,
                                    onDelete = { viewModel.removeKey(key) },
                                )
                            }
                        }
                    }

                    Text(
                        text = stringResource(R.string.custom_tag_hint),
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        fontSize = MiuixTheme.textStyles.footnote1.fontSize,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextButton(
                            text = stringResource(R.string.custom_tag_add_key),
                            onClick = {
                                viewModel.clearInputError()
                                showAddDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                        TextButton(
                            text = stringResource(R.string.reset_to_default),
                            onClick = viewModel::resetVisibleKeys,
                            enabled = uiState.visibleKeys.isNotEmpty(),
                            modifier = Modifier.weight(1f),
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
        onDismiss = {
            showAddDialog = false
            viewModel.clearInputError()
        },
        onConfirm = { typed ->
            scope.launch {
                if (viewModel.addKey(typed)) showAddDialog = false
            }
        },
    )
}

@Composable
private fun FootnoteText(text: String) {
    Text(
        text = text,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        fontSize = MiuixTheme.textStyles.footnote1.fontSize,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
    )
}

@Composable
private fun AddCustomTagDialog(
    show: Boolean,
    availableKeys: List<String>,
    error: CustomTagKeyError?,
    onAddAvailable: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var typed by remember(show) { mutableStateOf("") }

    WindowDialog(
        show = show,
        title = stringResource(R.string.custom_tag_add_key),
        onDismissRequest = onDismiss,
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
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = MiuixTheme.textStyles.footnote1.fontSize,
            )

            if (availableKeys.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    ChipGrid {
                        availableKeys.forEach { key ->
                            ManagedChip(
                                text = key,
                                onClick = { onAddAvailable(key) },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = typed,
                onValueChange = { typed = it },
                label = stringResource(R.string.custom_tag_key),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = when (error) {
                    null -> stringResource(R.string.custom_tag_key_hint)
                    CustomTagKeyError.EMPTY -> stringResource(R.string.invalid_empty_input)
                    CustomTagKeyError.INVALID -> stringResource(R.string.custom_tag_key_invalid)
                    CustomTagKeyError.DUPLICATE -> stringResource(R.string.duplicate_item)
                },
                color = if (error == null) {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                } else {
                    MiuixTheme.colorScheme.error
                },
                fontSize = MiuixTheme.textStyles.footnote1.fontSize,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(20.dp))
                TextButton(
                    text = stringResource(R.string.confirm),
                    onClick = { onConfirm(typed) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                )
            }
        }
    }
}
