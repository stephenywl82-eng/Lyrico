package com.lonx.lyrico.ui.components.library

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
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
            // M3 Scaffold 不替 topBar 消费状态栏 insets，且各 screen 的 TopAppBar 是
            // windowInsets = 0，所以由这里统一补安全区（E 批次换 Scaffold 后的回归）。
            // 注意 topBar（BlurredTopBar）自身已经加了状态栏 inset，这里**不要再**给外层
            // 套一层 inset，否则会双重缩进把顶栏整体压低一截。
            Box(Modifier.fillMaxWidth()) {
                // 顶栏本体（BlurredTopBar 自带 inset，其背景/控件从「状态栏下沿」开始）。
                Box(Modifier.fillMaxWidth()) { topBar() }

                // 状态栏安全区实色底：详情页内容（编辑页表单 / 歌词）会从毛玻璃顶栏下方滚过，
                // 滚到顶栏上方时会进入状态栏区域（时钟/电量文字处）。这里补一层**实色**把内容遮住，
                // 并 zIndex 置顶确保盖在半透明毛玻璃顶栏之上，避免内容压在状态栏文字上。
                // 用显式高度（状态栏高度）而非空盒 padding，防止个别机型上 Box 高度塌成 0 导致底色失效。
                val statusBarH = with(LocalDensity.current) {
                    WindowInsets.safeDrawing.getTop(this).toDp()
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(statusBarH)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .zIndex(1f)
                )
            }
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
