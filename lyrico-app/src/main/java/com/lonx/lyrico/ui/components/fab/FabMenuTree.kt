package com.lonx.lyrico.ui.components.fab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.FloatingActionButtonMenuScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.icons.LucideIcons
import androidx.compose.ui.res.stringResource

/**
 * FAB 菜单树形节点模型，支撑「归类分组」与「官方 item 嵌套（下钻）」。
 * - [FabMenuLeaf]：叶子项，点击触发动作（动作内自带收起菜单）。
 * - [FabMenuGroup]：分组项，点击下钻到其子节点。
 *
 * 官方 FloatingActionButtonMenu 不支持原生嵌套，下钻由 [ExpandableFabMenu] 内部的
 * path 状态驱动；本模型只声明结构，渲染全部走官方 item 语义角色。
 */
sealed interface FabMenuItemNode {
    val key: String
    val label: String
    val icon: ImageVector
    val enabled: Boolean
}

data class FabMenuLeaf(
    override val key: String,
    override val label: String,
    override val icon: ImageVector,
    override val enabled: Boolean = true,
    val onClick: () -> Unit
) : FabMenuItemNode

data class FabMenuGroup(
    override val key: String,
    override val label: String,
    override val icon: ImageVector,
    override val enabled: Boolean = true,
    val children: List<FabMenuItemNode>
) : FabMenuItemNode

/**
 * 分组头：官方 item 样式，文本右侧加 ArrowRight 指示可下钻。
 * 禁用态按 M3 规范表达（38% alpha + disabled() 语义 + onClick guard）。
 */
@Composable
fun FloatingActionButtonMenuScope.FabMenuGroupHeader(
    group: FabMenuGroup,
    onClick: () -> Unit,
    style: ExpandableFabMenuStyle = ExpandableFabMenuStyle.default()
) {
    val stateModifier = if (group.enabled) Modifier else
        Modifier.alpha(0.38f).semantics { disabled() }

    FloatingActionButtonMenuItem(
        onClick = { if (group.enabled) onClick() },
        modifier = stateModifier,
        icon = {
            Icon(
                imageVector = group.icon,
                contentDescription = null,
                modifier = Modifier.size(style.itemIconSize)
            )
        },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )
                Icon(
                    imageVector = LucideIcons.ArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(style.itemIconSize)
                )
            }
        }
    )
}

/**
 * 下钻「返回」项：ArrowLeft + 文本，点击回到上一级。
 */
@Composable
fun FloatingActionButtonMenuScope.FabMenuBackItem(
    onClick: () -> Unit,
    style: ExpandableFabMenuStyle = ExpandableFabMenuStyle.default()
) {
    FloatingActionButtonMenuItem(
        onClick = onClick,
        icon = {
            Icon(
                imageVector = LucideIcons.ArrowLeft,
                contentDescription = null,
                modifier = Modifier.size(style.itemIconSize)
            )
        },
        text = {
            Text(
                text = stringResource(R.string.action_back),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
        }
    )
}
