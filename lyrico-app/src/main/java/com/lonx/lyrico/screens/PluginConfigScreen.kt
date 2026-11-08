package com.lonx.lyrico.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.blur.BlurredTopBar
import com.lonx.lyrico.ui.components.blur.blurSource
import com.lonx.lyrico.ui.components.blur.rememberBarBlurBackdrop
import com.lonx.lyrico.ui.components.blur.rememberBarBlurEnabled
import com.lonx.lyrico.ui.components.editor.rememberEditorTextFieldState
import com.lonx.lyrico.ui.components.icons.LucideIcons
import com.lonx.lyrico.ui.components.library.EmptyStateKind
import com.lonx.lyrico.ui.components.library.IllustratedEmptyState
import com.lonx.lyrico.ui.components.library.LibraryDetailPage
import com.lonx.lyrico.ui.components.sdw.SdwTextInput
import com.lonx.lyrico.ui.components.scaffoldContentPadding
import com.lonx.lyrico.data.model.plugin.PluginConfigField
import com.lonx.lyrico.data.model.plugin.PluginConfigFieldType
import com.lonx.lyrico.data.model.plugin.PluginConfigOption
import com.lonx.lyrico.data.model.plugin.PluginSourceType
import com.lonx.lyrico.utils.isSatisfied
import com.lonx.lyrico.viewmodel.SearchSourceConfigViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

import dev.jeziellago.compose.markdowntext.MarkdownText
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.lonx.lyrico.ui.components.sdw.ExpressiveSwitch as Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior

@Composable
@Destination<RootGraph>(route = "plugin_config")
fun PluginConfigScreen(
    pluginId: String,
    navigator: DestinationsNavigator
) {
    val viewModel: SearchSourceConfigViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val topAppBarScrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val topBarBackdrop = rememberBarBlurBackdrop(enabled = rememberBarBlurEnabled())

    val requiredMessage = stringResource(R.string.source_config_required_error)

    LaunchedEffect(pluginId, configuration) {
        com.lonx.lyrico.plugin.i18n.PluginLocales.update(configuration)
        viewModel.load(pluginId)
    }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            Toast.makeText(context, R.string.source_config_saved, Toast.LENGTH_SHORT).show()
            viewModel.consumeSaved()
        }
    }

    val title = uiState.title.ifBlank { stringResource(R.string.plugin_config_title) }

    val hasConfigContent by remember {
        derivedStateOf {
            uiState.errorMessage == null &&
                    !uiState.isLoading &&
                    uiState.configFields.any { it.dependency.isSatisfied(uiState.values) }
        }
    }

    LibraryDetailPage(
        title = title,
        onBack = { navigator.navigateUp() },
        miuixTopBar = {
            BlurredTopBar(topBarBackdrop) {
                TopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.navigateUp() }) {
                            Icon(
                                imageVector = LucideIcons.ArrowLeft,
                                contentDescription = stringResource(R.string.action_back)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                viewModel.save(requiredMessage)
                            }
                        ) {
                            Icon(
                                imageVector = LucideIcons.Check,
                                contentDescription = stringResource(R.string.source_config_save)
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
        Column(
            modifier = Modifier
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                .fillMaxSize()
                .blurSource(topBarBackdrop)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(scaffoldContentPadding(paddingValues, bottomExtra = 12.dp))
        ) {
            if (uiState.errorMessage != null) {
                Text(
                    text = stringResource(R.string.source_config_invalid_source),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@LibraryDetailPage
            }

            val typeText = stringResource(
                R.string.plugin_type_with_value,
                uiState.sourceTypes
                    .map { stringResource(it.labelRes()) }
                    .joinToString(" / ")
            )
            val apiText = stringResource(
                R.string.plugin_api_versions_with_value,
                uiState.apiVersion,
                uiState.minHostApiVersion
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (uiState.sourceTypes.isNotEmpty()) {
                        Text(
                            text = typeText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = apiText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }

            if (!hasConfigContent) {
                IllustratedEmptyState(
                    kind = EmptyStateKind.Plugins,
                    title = stringResource(R.string.source_config_empty),
                    modifier = Modifier.fillMaxWidth(),
                )
                return@LibraryDetailPage
            }

            if (!uiState.pluginEnabled) {
                val hint = stringResource(R.string.source_config_plugin_disabled_hint)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Text(
                        text = hint,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }

            PluginConfigFormItems(
                fields = uiState.configFields,
                values = uiState.values,
                validationErrors = uiState.validationErrors,
                onValueChange = viewModel::updateValue
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PluginConfigFormItems(
    fields: List<PluginConfigField>,
    values: Map<String, String>,
    validationErrors: Map<String, String>,
    onValueChange: (String, String) -> Unit
) {
    if (fields.isEmpty()) return

    val grouped = fields.groupBy { field ->
        field.group.ifBlank { DEFAULT_CONFIG_GROUP }
    }

    grouped.forEach { (group, groupFields) ->
        val hasVisibleField = groupFields.any { field ->
            field.dependency.isSatisfied(values)
        }

        if (!hasVisibleField) {
            return@forEach
        }

        val groupTitle = if (group == DEFAULT_CONFIG_GROUP) {
            stringResource(R.string.source_config_basic)
        } else {
            groupFields.first().groupTitle ?: group
        }

        SmallTitle(text = groupTitle)

        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier.animateContentSize()
            ) {
                groupFields.forEach { field ->
                    val visible = field.dependency.isSatisfied(values)

                    AnimatedVisibility(visible = visible) {
                        PluginConfigFormItem(
                            field = field,
                            value = values[field.key].orEmpty(),
                            error = validationErrors[field.key],
                            onValueChange = { value ->
                                onValueChange(field.key, value)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PluginConfigFormItem(
    field: PluginConfigField,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit
) {
    when (field.type) {
        PluginConfigFieldType.SWITCH -> {
            SwitchPreferenceRow(
                title = field.title,
                summary = error ?: field.summary,
                checked = value.toBooleanStrictOrNull() ?: false,
                onCheckedChange = { checked ->
                    onValueChange(checked.toString())
                }
            )
        }

        PluginConfigFieldType.DROPDOWN -> {
            val selectedIndex = field.options
                .indexOfFirst { it.value == value }
                .coerceAtLeast(0)

            DropdownPreferenceRow(
                title = field.title,
                summary = error ?: field.summary,
                options = field.options,
                selectedIndex = selectedIndex,
                enabled = field.options.isNotEmpty(),
                onSelectedIndexChange = { index ->
                    field.options.getOrNull(index)?.let { onValueChange(it.value) }
                }
            )
        }

        PluginConfigFieldType.TEXT,
        PluginConfigFieldType.TEXTAREA,
        PluginConfigFieldType.PASSWORD,
        PluginConfigFieldType.NUMBER -> {
            var passwordVisible by rememberSaveable(field.key) {
                mutableStateOf(false)
            }

            val isPassword = field.type == PluginConfigFieldType.PASSWORD
            val keyboardOptions = if (field.type == PluginConfigFieldType.NUMBER) {
                KeyboardOptions(keyboardType = KeyboardType.Number)
            } else {
                KeyboardOptions.Default
            }
            val inputState = rememberEditorTextFieldState(value, onValueChange)
            val lineLimits = if (field.type == PluginConfigFieldType.TEXTAREA) {
                TextFieldLineLimits.MultiLine(minHeightInLines = 2, maxHeightInLines = 8)
            } else TextFieldLineLimits.SingleLine
            val numberInputTransformation = if (field.type == PluginConfigFieldType.NUMBER) {
                InputTransformation {
                    val digits = asCharSequence().filter(Char::isDigit)
                    if (digits.length != length) replace(0, length, digits)
                }
            } else null

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                SdwTextInput(
                    state = inputState,
                    label = field.title,
                    modifier = Modifier.fillMaxWidth(),
                    secure = isPassword,
                    passwordVisible = passwordVisible,
                    keyboardOptions = keyboardOptions,
                    lineLimits = lineLimits,
                    inputTransformation = numberInputTransformation,
                    summary = field.summary,
                    error = error,
                    actionContent = if (isPassword) {
                        {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible }
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        LucideIcons.EyeOff
                                    } else {
                                        LucideIcons.Eye
                                    },
                                    contentDescription = if (passwordVisible) {
                                        stringResource(R.string.password_hide)
                                    } else {
                                        stringResource(R.string.password_show)
                                    }
                                )
                            }
                        }
                    } else null
                )
            }
        }

        PluginConfigFieldType.MARKDOWN -> {
            val markdown = field.defaultValue.ifBlank { field.summary }
            if (!markdown.isNullOrEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    field.title.takeIf { it.isNotBlank() }?.let { markdownTitle ->
                        Text(
                            text = markdownTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    MarkdownText(
                        modifier = Modifier.fillMaxWidth(),
                        markdown = markdown,
                        linkColor = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
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

@Composable
private fun SwitchPreferenceRow(
    title: String,
    summary: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DropdownPreferenceRow(
    title: String,
    summary: String?,
    options: List<PluginConfigOption>,
    selectedIndex: Int,
    enabled: Boolean = true,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable { expanded = true } else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box {
            Text(
                text = options.getOrNull(selectedIndex)?.label.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(option.label)
                                val optionSummary = option.summary
                                if (!optionSummary.isNullOrBlank()) {
                                    Text(
                                        optionSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
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

private const val DEFAULT_CONFIG_GROUP = "__basic__"

private fun PluginSourceType.labelRes(): Int = when (this) {
    PluginSourceType.METADATA -> R.string.plugin_type_metadata
    PluginSourceType.LYRICS -> R.string.plugin_type_lyrics
    PluginSourceType.COVER -> R.string.plugin_type_cover
}
