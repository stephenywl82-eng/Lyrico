package com.lonx.lyrico.ui.components.library

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.ui.components.scaffoldTopAppBarInsetsPadding

/** Detail pages share layout and actions; M3 Expressive implementation, themed by SDW. */
@Composable
internal fun LibraryDetailPageMiuix(
    topBar: @Composable () -> Unit,
    snackbarHost: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            // 与搜索系列页面一致：M3 Scaffold 不替 topBar 消费状态栏 insets，
            // 各 screen 的 TopAppBar 又是 windowInsets = 0，所以由这里统一补安全区，
            // 否则详情标题会顶到状态栏（E 批次换 Scaffold 后的回归）。
            Box(Modifier.scaffoldTopAppBarInsetsPadding()) { topBar() }
        },
        snackbarHost = snackbarHost,
        content = content,
    )
}

@Composable
internal fun LibraryDetailIconButtonMiuix(
    icon: ImageVector,
    description: String?,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(icon, description)
    }
}

@Composable
internal fun LibraryDetailTabsMiuix(
    tabs: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val outer = modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp)
        .padding(bottom = 8.dp)
    SingleChoiceSegmentedButtonRow(modifier = outer) {
        tabs.forEachIndexed { index, title ->
            SegmentedButton(
                selected = index == selectedIndex,
                onClick = { onSelected(index) },
                shape = SegmentedButtonDefaults.itemShape(index, tabs.size),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun LibraryDetailTextMiuix(
    text: String,
    modifier: Modifier = Modifier,
    tone: LibraryDetailTextTone = LibraryDetailTextTone.Body,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    Text(
        text,
        modifier = modifier,
        style =
            when (tone) {
                LibraryDetailTextTone.Title -> MaterialTheme.typography.titleMedium
                LibraryDetailTextTone.Body -> MaterialTheme.typography.bodyLarge
                LibraryDetailTextTone.Secondary -> MaterialTheme.typography.bodyMedium
            },
        color =
            if (tone == LibraryDetailTextTone.Secondary)
                MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
        maxLines = maxLines,
        overflow = overflow,
    )
}
