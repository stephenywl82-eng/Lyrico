package com.lonx.lyrico.ui.components.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.lonx.lyrico.ui.components.icons.PreferenceIconTone
import com.lonx.lyrico.ui.components.sdw.SdwItemRow

/** [B] 统一到 SDW：菜单操作行直接走 SdwItemRow，原 MIUIX / Salt 分支坍缩为单路。 */
@Composable
internal fun ItemMenuAction(
    title: String,
    icon: ImageVector,
    tone: PreferenceIconTone = PreferenceIconTone.Tools,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    SdwItemRow(
        title = title,
        icon = icon,
        tone = tone,
        summary = summary,
        enabled = enabled,
        onClick = onClick,
    )
}
