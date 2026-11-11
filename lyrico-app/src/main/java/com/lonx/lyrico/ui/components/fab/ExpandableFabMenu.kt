package com.lonx.lyrico.ui.components.fab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuScope
import androidx.compose.material3.Icon
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ExpandableFabMenuPosition {
    BottomEnd,
    BottomStart,
    TopEnd,
    TopStart,
    CenterStart,
    CenterEnd
}

/**
 * [M3 Expressive] 展开式 FAB 菜单。
 *
 * 内层已由自绘 morph 面板替换为官方 FloatingActionButtonMenu +
 * ToggleFloatingActionButton + FloatingActionButtonMenuItem：
 * 官方负责 56dp close button、48dp 触控目标、圆角 item、进出场 staggered 动画、
 * 窗口高度受限时的纵向滚动、以及 shape morph 与 motion scheme。
 *
 * 仍保留自绘外壳（官方组件不提供的能力）：全屏 scrim 点击关闭、6 种屏幕位置、
 * windowInsets 避让、visible / enabled 状态门控、双 FAB 叠放、自定义主图标与配色。
 *
 * [menuContent] 的 receiver 是官方 [FloatingActionButtonMenuScope]，因此调用点里
 * 的 FabMenuItem(...) 会解析到该 scope 的扩展实现，无需改造。
 */
@Composable
fun BoxScope.ExpandableFabMenu(
    visible: Boolean,
    expanded: Boolean,
    enabled: Boolean,
    itemCount: Int = 0,
    modifier: Modifier = Modifier,
    style: ExpandableFabMenuStyle = ExpandableFabMenuStyle.default(),
    position: ExpandableFabMenuPosition = ExpandableFabMenuPosition.BottomEnd,
    menuTree: List<FabMenuItemNode>? = null,
    onExpandedChange: (Boolean) -> Unit,
    menuContent: @Composable FloatingActionButtonMenuScope.() -> Unit = {}
) {
    val effectiveExpanded = visible && expanded && enabled

    // 官方 FAB menu 没有 scrim；保留项目「点击遮罩关闭」的既有交互。
    AnimatedVisibility(
        visible = effectiveExpanded,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(style.scrimColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onExpandedChange(false)
                }
        )
    }

    val baseModifier = modifier
        .align(position.alignment)
        .then(
            position.windowInsets?.let { Modifier.windowInsetsPadding(it) } ?: Modifier
        )
        .padding(position.padding)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = baseModifier
    ) {
        FloatingActionButtonMenu(
            expanded = effectiveExpanded,
            horizontalAlignment = position.horizontalContentAlignment,
            button = {
                ToggleFloatingActionButton(
                    checked = effectiveExpanded,
                    onCheckedChange = { checked -> if (enabled) onExpandedChange(checked) },
                    modifier = Modifier.semantics {
                        // 官方无障碍规范：close button 需朗读 Toggle menu + 展开/折叠状态
                        contentDescription = "Toggle menu"
                        stateDescription = if (effectiveExpanded) "Expanded" else "Collapsed"
                    },
                ) {
                    Icon(
                        imageVector = style.mainIcon,
                        contentDescription = null,
                        tint = style.mainContentColor,
                        modifier = Modifier.size(style.mainIconSize)
                    )
                }
            },
            content = {
                if (menuTree != null) {
                    FabMenuDrillDown(
                        items = menuTree,
                        expanded = effectiveExpanded
                    )
                } else {
                    menuContent()
                }
            },
        )
    }
}

/**
 * 树形下钻渲染：官方 FloatingActionButtonMenu 不支持原生嵌套，这里用 path 状态
 * 在同一列官方 item 内实现「主菜单 → 子菜单」的层级切换。
 * - 非根层级时先渲染一个「返回」项；
 * - [FabMenuLeaf] 走 [FabMenuItem]（48dp 触控目标、圆角、staggered 动画、禁用 38% alpha）；
 * - [FabMenuGroup] 走 [FabMenuGroupHeader]（右侧 ArrowRight 指示可下钻）。
 * 收起菜单时重置 path，避免下次打开残留子层级。
 */
@Composable
private fun FloatingActionButtonMenuScope.FabMenuDrillDown(
    items: List<FabMenuItemNode>,
    expanded: Boolean
) {
    val path = remember { mutableStateOf<List<FabMenuGroup>>(emptyList()) }
    LaunchedEffect(expanded) {
        if (!expanded) path.value = emptyList()
    }

    val currentChildren = if (path.value.isEmpty()) items else path.value.last().children

    if (path.value.isNotEmpty()) {
        FabMenuBackItem(onClick = { path.value = path.value.dropLast(1) })
    }

    currentChildren.forEach { node ->
        when (node) {
            is FabMenuLeaf -> FabMenuItem(
                label = node.label,
                icon = node.icon,
                enabled = node.enabled,
                onClick = node.onClick
            )

            is FabMenuGroup -> FabMenuGroupHeader(
                group = node,
                onClick = { path.value = path.value + node }
            )
        }
    }
}

private val ExpandableFabMenuPosition.alignment: Alignment
    get() = when (this) {
        ExpandableFabMenuPosition.BottomEnd -> Alignment.BottomEnd
        ExpandableFabMenuPosition.BottomStart -> Alignment.BottomStart
        ExpandableFabMenuPosition.TopEnd -> Alignment.TopEnd
        ExpandableFabMenuPosition.TopStart -> Alignment.TopStart
        ExpandableFabMenuPosition.CenterStart -> Alignment.CenterStart
        ExpandableFabMenuPosition.CenterEnd -> Alignment.CenterEnd
    }

/**
 * 官方规范：FAB menu 与 FAB 的边距在 large / extra large 窗口由 16dp 增至 24dp。
 */
private val ExpandableFabMenuPosition.padding: PaddingValues
    @Composable
    get() {
        val large = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600
        val edge = if (large) 24.dp else 16.dp
        return when (this) {
            ExpandableFabMenuPosition.BottomEnd,
            ExpandableFabMenuPosition.CenterEnd -> PaddingValues(
                end = edge,
                bottom = if (this == ExpandableFabMenuPosition.BottomEnd) 24.dp else 0.dp
            )

            ExpandableFabMenuPosition.BottomStart,
            ExpandableFabMenuPosition.CenterStart -> PaddingValues(
                start = edge,
                bottom = if (this == ExpandableFabMenuPosition.BottomStart) 24.dp else 0.dp
            )

            ExpandableFabMenuPosition.TopEnd -> PaddingValues(top = 24.dp, end = edge)
            ExpandableFabMenuPosition.TopStart -> PaddingValues(top = 24.dp, start = edge)
        }
    }

private val ExpandableFabMenuPosition.windowInsets: WindowInsets?
    @Composable
    get() = when (this) {
        ExpandableFabMenuPosition.TopStart,
        ExpandableFabMenuPosition.TopEnd -> WindowInsets.statusBars

        ExpandableFabMenuPosition.BottomStart,
        ExpandableFabMenuPosition.BottomEnd -> WindowInsets.navigationBars

        ExpandableFabMenuPosition.CenterStart,
        ExpandableFabMenuPosition.CenterEnd -> null
    }

private val ExpandableFabMenuPosition.horizontalContentAlignment: Alignment.Horizontal
    get() = when (this) {
        ExpandableFabMenuPosition.BottomEnd,
        ExpandableFabMenuPosition.TopEnd,
        ExpandableFabMenuPosition.CenterEnd -> Alignment.End

        ExpandableFabMenuPosition.BottomStart,
        ExpandableFabMenuPosition.TopStart,
        ExpandableFabMenuPosition.CenterStart -> Alignment.Start
    }

/**
 * 兼容保留：旧的 morph 面板按 itemCount 计算展开高度。
 * 官方 FloatingActionButtonMenu 自行测量并在窗口高度受限时滚动，本函数不再参与布局，
 * 但保留给仍引用它的代码（避免连锁改动）。
 */
@Composable
fun ExpandableFabMenuStyle.expandedHeightFor(itemCount: Int): Dp {
    val verticalPadding = contentPadding.calculateTopPadding() + contentPadding.calculateBottomPadding()
    return (itemHeight * itemCount.coerceAtLeast(1) + verticalPadding)
        .coerceIn(minExpandedHeight, maxExpandedHeight)
}
