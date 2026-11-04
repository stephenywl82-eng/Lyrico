package com.lonx.lyrico.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.screens.library.libraryScrollFeedback
import com.lonx.lyrico.ui.components.blur.BlurredTopBar
import com.lonx.lyrico.ui.components.blur.blurSource
import com.lonx.lyrico.ui.components.blur.rememberBarBlurBackdrop
import com.lonx.lyrico.ui.components.icons.LucideIcons
import com.lonx.lyrico.ui.components.icons.PreferenceIcon
import com.lonx.lyrico.ui.components.icons.PreferenceIconTone
import com.lonx.lyrico.ui.components.scaffoldContentPadding

/** Material3 Expressive settings page skeleton. */
@Composable
internal fun SettingsPageMaterial(
    initialized: Boolean,
    onBack: () -> Unit,
    content: SettingsItemsScope.() -> Unit,
) {
    val listState = rememberLazyListState()
    val title = stringResource(R.string.settings_title)
    val behavior = TopAppBarDefaults.pinnedScrollBehavior()
    val backdrop = rememberBarBlurBackdrop()
    Scaffold(
        topBar = {
            BlurredTopBar(backdrop) {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                LucideIcons.ArrowLeft,
                                stringResource(R.string.action_back),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                    windowInsets = WindowInsets(0.dp),
                    scrollBehavior = behavior,
                )
            }
        }
    ) { padding ->
        if (initialized) {
            Box(Modifier.fillMaxSize().blurSource(backdrop)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .libraryScrollFeedback(behavior)
                        .fillMaxSize(),
                    contentPadding = scaffoldContentPadding(padding, bottomExtra = 12.dp),
                ) {
                    SettingsItemsScope(this, MaterialSettingsItems).content()
                }
            }
        } else {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

internal object MaterialSettingsItems : SettingsItemPresentation {
    @Composable
    override fun SectionTitle(text: String) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    @Composable
    override fun Group(
        modifier: Modifier,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        ) {
            Column(Modifier.fillMaxWidth()) {
                content()
            }
        }
    }

    @Composable
    override fun Action(
        title: String,
        summary: String?,
        icon: ImageVector?,
        tone: PreferenceIconTone,
        value: String?,
        holdDownState: Boolean,
        onClick: () -> Unit,
    ) {
        SettingsRow(
            icon = icon,
            tone = tone,
            title = title,
            summary = summary,
            background = if (holdDownState) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            } else {
                Color.Unspecified
            },
            onClick = onClick,
        ) {
            if (!value.isNullOrBlank()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Icon(
                LucideIcons.ArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }

    @Composable
    override fun Toggle(
        title: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        summary: String?,
        enabled: Boolean,
        icon: ImageVector?,
        tone: PreferenceIconTone,
    ) {
        SettingsRow(
            icon = icon,
            tone = tone,
            title = title,
            summary = summary,
            enabled = enabled,
            onClick = { if (enabled) onCheckedChange(!checked) },
        ) {
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
            )
        }
    }

    @Composable
    override fun Choice(
        title: String,
        items: List<String>,
        selectedIndex: Int,
        onSelectedIndexChange: (Int) -> Unit,
        summary: String?,
        icon: ImageVector?,
        tone: PreferenceIconTone,
    ) {
        var expanded by remember { mutableStateOf(false) }
        SettingsRow(
            icon = icon,
            tone = tone,
            title = title,
            summary = summary,
            onClick = { expanded = true },
        ) {
            Text(
                text = items.getOrNull(selectedIndex).orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 4.dp),
            )
            Icon(
                LucideIcons.ArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onSelectedIndexChange(index)
                    },
                    trailingIcon = if (index == selectedIndex) {
                        {
                            Icon(
                                LucideIcons.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else null,
                )
            }
        }
    }

    @Composable
    override fun Spinner(
        title: String,
        items: List<PaletteOption>,
        selectedIndex: Int,
        onSelectedIndexChange: (Int) -> Unit,
        icon: ImageVector?,
        tone: PreferenceIconTone,
    ) {
        var expanded by remember { mutableStateOf(false) }
        SettingsRow(
            icon = icon,
            tone = tone,
            title = title,
            onClick = { expanded = true },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 4.dp),
            ) {
                items.forEachIndexed { index, option ->
                    val selected = index == selectedIndex
                    Box(
                        Modifier
                            .size(if (selected) 20.dp else 16.dp)
                            .clip(CircleShape)
                            .background(option.color),
                    )
                }
            }
            Icon(
                LucideIcons.ArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            items.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option.title) },
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(option.color),
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelectedIndexChange(index)
                    },
                    trailingIcon = if (index == selectedIndex) {
                        {
                            Icon(
                                LucideIcons.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else null,
                )
            }
        }
    }

    @Composable
    override fun Range(
        title: String,
        value: Float,
        onValueChange: (Float) -> Unit,
        onValueChangeFinished: () -> Unit,
        valueText: String,
        summary: String?,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int,
        icon: ImageVector?,
        tone: PreferenceIconTone,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    PreferenceIcon(icon, tone, Modifier.size(24.dp).padding(end = 12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!summary.isNullOrBlank()) {
                        Text(
                            summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    valueText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SettingsRow(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: PreferenceIconTone = PreferenceIconTone.Tools,
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    background: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val clickModifier = if (onClick != null && enabled) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    val backgroundModifier = if (background != Color.Unspecified) {
        Modifier.background(background)
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(backgroundModifier)
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            PreferenceIcon(icon, tone, Modifier.size(24.dp).padding(end = 12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing()
    }
}
