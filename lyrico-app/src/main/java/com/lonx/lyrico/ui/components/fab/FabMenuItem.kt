package com.lonx.lyrico.ui.components.fab

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.FloatingActionButtonMenuScope
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.ui.components.sdw.BasicComponent

/**
 * [M3 Expressive] FAB 菜单项：作为 [FloatingActionButtonMenuScope] 的扩展，
 * 内部直接渲染官方 FloatingActionButtonMenuItem（官方负责 48dp 触控目标、
 * 圆角容器、进出场 staggered 动画与窗口高度受限时的滚动）。
 *
 * 官方 item 没有 enabled 参数，禁用态按 M3 规范表达：
 * 38% alpha + disabled() 语义 + onClick guard，颜色仍交给官方语义角色。
 *
 * 按官方 Anatomy 要求，label 不再 maxLines=1 + Ellipsis 截断，也不设固定宽度，
 * 让文本自然换行、item hug contents。
 */
@Composable
fun FloatingActionButtonMenuScope.FabMenuItem(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    enabled: Boolean = true,
    style: ExpandableFabMenuStyle = ExpandableFabMenuStyle.default(),
    onClick: () -> Unit
) {
    val stateModifier = if (enabled) Modifier else
        Modifier.alpha(0.38f).semantics { disabled() }

    FloatingActionButtonMenuItem(
        onClick = { if (enabled) onClick() },
        modifier = modifier.then(stateModifier),
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(style.itemIconSize)
            )
        },
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
            )
        },
    )
}

/**
 * 兼容重载：供不处于 [FloatingActionButtonMenuScope] 的锚点场景使用
 * （例如 LyricsOrganizationMenu 的 anchor 需要普通可点击行 + 弹出式子菜单，
 * 这个 anchor 是在独立的 Box 里被组合的，拿不到官方 FAB Menu 的 scope）。
 *
 * 视觉与官方 item 对齐（48dp 最小行高、图标 + 文本、不截断），
 * 禁用态同样走 38% alpha + disabled() 语义。
 */
@Composable
fun FabMenuAnchorItem(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    enabled: Boolean = true,
    style: ExpandableFabMenuStyle = ExpandableFabMenuStyle.default(),
    onClick: () -> Unit
) {
    val contentColor = if (enabled) style.itemContentColor else style.itemDisabledContentColor

    BasicComponent(
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(if (enabled) Modifier else Modifier.alpha(0.38f).semantics { disabled() }),
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
        onClick = { if (enabled) onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
        )
    }
}
