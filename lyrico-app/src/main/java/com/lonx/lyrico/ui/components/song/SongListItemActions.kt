package com.lonx.lyrico.ui.components.song

import com.lonx.lyrico.ui.components.icons.LucideIcons
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme

@Composable
fun SongListItemActions(
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onShowMenu: () -> Unit,
) {
    if (isSelectionMode) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelection() }
        )
    } else {
        IconButton(onClick = onShowMenu) {
            Icon(
                imageVector = LucideIcons.EllipsisVertical,
                contentDescription = "More",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
