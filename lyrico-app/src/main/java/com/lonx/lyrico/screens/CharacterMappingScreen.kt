
package com.lonx.lyrico.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.data.model.CharacterMappingRule
import com.lonx.lyrico.data.model.ReplacementCharOption
import com.lonx.lyrico.data.model.toReplacementOption
import com.lonx.lyrico.ui.components.icons.LucideIcons
import com.lonx.lyrico.ui.components.library.LibraryDetailPage
import com.lonx.lyrico.ui.components.library.libraryDetailScroll
import com.lonx.lyrico.ui.components.scaffoldContentPadding
import com.lonx.lyrico.viewmodel.CharacterMappingViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

@Composable
@Destination<RootGraph>(route = "character_mapping")
fun CharacterMappingScreen(
    navigator: DestinationsNavigator
) {
    val viewModel: CharacterMappingViewModel = koinViewModel()
    val characterMappingConfig by viewModel.characterMappingConfig.collectAsState()
    val topAppBarScrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val salt = false
    LibraryDetailPage(
        title = stringResource(R.string.configure_character_mapping),
        onBack = { navigator.popBackStack() },
        miuixTopBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.configure_character_mapping), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(
                        onClick = { navigator.popBackStack() }) {
                        Icon(
                            imageVector = LucideIcons.ArrowLeft,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                windowInsets = WindowInsets(0.dp),
                scrollBehavior = topAppBarScrollBehavior
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().libraryDetailScroll(topAppBarScrollBehavior),
            contentPadding = scaffoldContentPadding(paddingValues, bottomExtra = 12.dp)
        ) {
            val config = characterMappingConfig
            if (config != null && config.rules.isNotEmpty()) {
                items(items = config.rules, key = { it.id }) { rule ->
                    CharacterMappingRuleSection(
                        rule = rule,
                        onCharacterMappingChanged = { character, replacementChar ->
                            viewModel.updateCharacterMappingInRule(rule.id, character, replacementChar)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CharacterMappingRuleSection(
    rule: CharacterMappingRule,
    onCharacterMappingChanged: (character: String, replacementChar: String?) -> Unit
) {
    val salt = false
    if (rule.charMappings.isEmpty()) {

        Card(
            modifier = Modifier.padding(horizontal = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Text(
                text = stringResource(R.string.no_character_mappings),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val optionLabels = listOf(
        stringResource(R.string.replacement_not_selected)
    ) + ReplacementCharOption.entries.map { option ->
        stringResource(option.labelRes)
    }

    val rows: @Composable () -> Unit = {
        rule.charMappings.entries.forEach { (character, currentReplacement) ->
            val currentOption = currentReplacement.toReplacementOption()
            val selectedIndex = currentOption?.let { ReplacementCharOption.entries.indexOf(it) + 1 } ?: 0
            val onSelected: (Int) -> Unit = { index ->
                val replacementValue = if (index == 0) null else ReplacementCharOption.entries[index - 1].value
                onCharacterMappingChanged(character, replacementValue)
            }
            DropdownPreferenceRow(
                title = stringResource(R.string.character_to_replace, character),
                summary = stringResource(R.string.character_replacement_selector_subtitle),
                items = optionLabels,
                selectedIndex = selectedIndex,
                onSelectedIndexChange = onSelected
            )
        }
    }
    Card(modifier = Modifier.padding(horizontal = 12.dp).padding(top = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) { rows() }
}

@Composable
private fun DropdownPreferenceRow(
    title: String,
    summary: String,
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
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
