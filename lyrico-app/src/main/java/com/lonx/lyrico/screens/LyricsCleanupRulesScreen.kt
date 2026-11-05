package com.lonx.lyrico.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.ui.components.blur.BlurredTopBar
import com.lonx.lyrico.ui.components.blur.blurSource
import com.lonx.lyrico.ui.components.blur.rememberBarBlurBackdrop
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.icons.LucideIcons
import com.lonx.lyrico.ui.components.ChipGrid
import com.lonx.lyrico.ui.components.ManagedChip
import com.lonx.lyrico.ui.components.library.LibraryDetailPage
import com.lonx.lyrico.ui.components.library.libraryDetailScroll
import com.lonx.lyrico.ui.components.scaffoldContentPadding
import com.lonx.lyrico.ui.components.sdw.SdwTextInput
import com.lonx.lyrico.viewmodel.SettingsViewModel

import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
@Destination<RootGraph>(route = "lyrics_cleanup_rules")
fun LyricsCleanupRulesScreen(
    navigator: DestinationsNavigator
) {
    val viewModel: SettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val topAppBarScrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var addingRule by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<String?>(null) }
    var deletingRule by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val invalidOrDuplicateRule = stringResource(R.string.invalid_or_duplicate_rule)

    val topBarBackdrop = rememberBarBlurBackdrop()
    LibraryDetailPage(
        title = stringResource(R.string.non_lyrics_cleanup_rules_title),
        onBack = { navigator.popBackStack() },
        miuixTopBar = {
            BlurredTopBar(topBarBackdrop) {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.non_lyrics_cleanup_rules_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.popBackStack() }) {
                            Icon(
                                LucideIcons.ArrowLeft,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    ),
                    windowInsets = WindowInsets(0.dp),
                    scrollBehavior = topAppBarScrollBehavior
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .blurSource(topBarBackdrop)
                .libraryDetailScroll(topAppBarScrollBehavior),
            contentPadding = scaffoldContentPadding(
                paddingValues = paddingValues,
                bottomExtra = 12.dp
            )
        ) {
            item(key = "rules") {

                SmallTitle(text = stringResource(R.string.non_lyrics_cleanup_rules_section))
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    ChipGrid(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        uiState.lyricsTagLineKeywords.forEach { rule ->
                            ManagedChip(
                                text = rule,
                                onClick = { editingRule = rule },
                                onDelete = { deletingRule = rule }
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.non_lyrics_cleanup_rules_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                errorMessage = null
                                addingRule = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                LucideIcons.Plus,
                                null,
                                Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.add_non_lyrics_cleanup_rule),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Button(
                            onClick = viewModel::resetNonLyricsContentRules,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(
                                LucideIcons.RotateCcw,
                                null,
                                Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.reset_to_default),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

            }
        }
    }

    CleanupRuleInputDialog(
        show = addingRule,
        title = stringResource(R.string.add_non_lyrics_cleanup_rule),
        initialValue = "",
        errorMessage = errorMessage,
        onDismiss = {
            addingRule = false
            errorMessage = null
        },
        onSave = { value ->
            if (viewModel.addNonLyricsContentRule(value)) {
                addingRule = false
                errorMessage = null
            } else {
                errorMessage = invalidOrDuplicateRule
            }
        }
    )

    CleanupRuleInputDialog(
        show = editingRule != null,
        title = stringResource(R.string.edit_non_lyrics_cleanup_rule),
        initialValue = editingRule.orEmpty(),
        errorMessage = errorMessage,
        onDismiss = {
            editingRule = null
            errorMessage = null
        },
        onSave = { value ->
            val oldRule = editingRule ?: return@CleanupRuleInputDialog
            if (viewModel.updateNonLyricsContentRule(oldRule, value)) {
                editingRule = null
                errorMessage = null
            } else {
                errorMessage = invalidOrDuplicateRule
            }
        }
    )

    WindowDialog(
        show = deletingRule != null,
        title = stringResource(R.string.non_lyrics_cleanup_delete_rule_title),
        onDismissRequest = { deletingRule = null }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(
                    R.string.non_lyrics_cleanup_delete_rule_message,
                    deletingRule.orEmpty()
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    onClick = { deletingRule = null },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(12.dp))
                TextButton(
                    onClick = {
                        deletingRule?.let(viewModel::removeNonLyricsContentRule)
                        deletingRule = null
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            }
        }
    }
}

@Composable
private fun CleanupRuleInputDialog(
    show: Boolean,
    title: String,
    initialValue: String,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
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
                modifier = Modifier.fillMaxWidth(),
                lineLimits = TextFieldLineLimits.SingleLine,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage ?: stringResource(R.string.non_lyrics_cleanup_rule_input_hint),
                color = if (errorMessage == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
                fontSize = MaterialTheme.typography.bodySmall.fontSize
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(modifier = Modifier.width(12.dp))
                TextButton(
                    onClick = { onSave(text) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}

@Composable
private fun SmallTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
    )
}
