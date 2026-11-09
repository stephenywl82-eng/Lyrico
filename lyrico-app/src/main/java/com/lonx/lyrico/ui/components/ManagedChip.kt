package com.lonx.lyrico.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.icons.LucideIcons

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipGrid(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

/**
 * 自定义标签、非歌词内容过滤规则、艺术家拆分规则三个页面共用的胶囊。
 *
 * [checked] 为 null 时不渲染勾选圈，胶囊始终按"已生效"着色，适合只有一个动作
 * （点击编辑或直接删除）的规则；传入 true/false 时会渲染勾选圈，并按状态切换高亮，
 * 适合可开关的规则。
 */
@Composable
fun ManagedChip(
    text: String,
    modifier: Modifier = Modifier,
    checked: Boolean? = null,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    editDescription: String = stringResource(R.string.common_edit),
) {
    val showCheckbox = checked != null
    val highlighted = checked != false
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (highlighted) {
                    colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    colorScheme.surfaceContainer
                }
            )
            .border(
                width = 1.dp,
                color = if (highlighted) {
                    colorScheme.primary.copy(alpha = 0.48f)
                } else {
                    colorScheme.outlineVariant
                },
                shape = CircleShape,
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(
                start = if (showCheckbox) 8.dp else 10.dp,
                top = 6.dp,
                end = 6.dp,
                bottom = 6.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCheckbox) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        if (highlighted) {
                            colorScheme.primary
                        } else {
                            colorScheme.outlineVariant
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (highlighted) {
                    Text(
                        text = "✓",
                        color = colorScheme.onPrimary,
                        fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    )
                }
            }
        }

        Text(
            text = text,
            modifier = Modifier.padding(
                start = if (showCheckbox) 8.dp else 0.dp,
                end = 8.dp,
            ),
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        onEdit?.let { edit ->
            Icon(
                imageVector = LucideIcons.Pencil,
                contentDescription = editDescription,
                modifier = Modifier.size(18.dp).clip(CircleShape).clickable(onClick = edit),
                tint = colorScheme.onSurfaceVariant,
            )
        }

        if (onDelete != null) {
            Icon(
                imageVector = LucideIcons.Trash,
                contentDescription = stringResource(R.string.common_delete),
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDelete),
                tint = colorScheme.onSurfaceVariant,
            )
        }
    }
}
