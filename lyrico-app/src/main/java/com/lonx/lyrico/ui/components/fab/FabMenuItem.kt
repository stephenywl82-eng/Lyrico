package com.lonx.lyrico.ui.components.fab

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.material3.MaterialTheme

@Composable
fun FabMenuItem(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    enabled: Boolean = true,
    style: ExpandableFabMenuStyle = ExpandableFabMenuStyle.default(),
    onClick: () -> Unit
) {
    val contentColor = if (enabled) {
        style.itemContentColor
    } else {
        style.itemDisabledContentColor
    }

    BasicComponent(
        modifier = modifier.height(style.itemHeight),
        enabled = enabled,
        startAction = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(style.itemIconSize)
            )
        },
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        onClick = onClick
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
