package com.lonx.lyrico.ui.components.library

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.data.model.FloatingBarEffect
import com.lonx.lyrico.screens.library.LibraryTab
import com.lonx.lyrico.ui.components.blur.BlurredBar
import com.lonx.lyrico.ui.components.blur.LayerBackdrop
import com.lonx.lyrico.ui.components.icons.LucideIcons

/**
 * 阶段6-C：库页导航的纯 M3 Expressive 实现。
 * 原先 Miuix NavigationBar(icon = ImageVector, label = String) 换成 M3 同名组件
 * （icon/label 改为 composable lambda、颜色走 NavigationBarItemDefaults 语义色）。
 */
@Composable
fun LibraryBottomNavigationBar(
    tabs: List<LibraryTab>,
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop? = null,
) {
    val haptic = LocalHapticFeedback.current
    BlurredBar(
        backdrop = backdrop,
        modifier = Modifier.fillMaxWidth(),
    ) {
        NavigationBar(
            modifier = modifier,
            containerColor = Color.Transparent,
        ) {
            tabs.forEach { tab ->
                NavigationBarItem(
                    selected = tab == selectedTab,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTabSelected(tab)
                    },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = stringResource(tab.titleRes),
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = { Text(stringResource(tab.titleRes)) },
                    colors = NavigationBarItemDefaults.colors(),
                )
            }
        }
    }
}

@Composable
fun LibraryNavigationRail(
    tabs: List<LibraryTab>,
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    NavigationRail(modifier = modifier) {
        tabs.forEach { tab ->
            NavigationRailItem(
                selected = tab == selectedTab,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTabSelected(tab)
                },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = stringResource(tab.titleRes),
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = { Text(stringResource(tab.titleRes)) },
                colors = NavigationRailItemDefaults.colors(),
            )
        }
    }
}

/** 手机端悬浮底栏：C 批次后统一走纯 M3 语义容器的 [BlurNavigationBarV2]。 */
@Composable
fun LibraryBlurBottomBar(
    backdrop: LayerBackdrop?,
    effect: FloatingBarEffect,
    tabs: List<LibraryTab>,
    selectedTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    BlurNavigationBarV2(
        modifier = modifier,
        backdrop = backdrop,
        selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0),
        itemCount = tabs.size,
        effect = effect,
        onSelectionChanged = { index ->
            tabs.getOrNull(index)?.let(onTabSelected)
        },
    ) {
        tabs.forEachIndexed { index, tab ->
            BlurNavigationBarItemV2(
                index = index,
                selected = tab == selectedTab,
                onClick = {},
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = stringResource(tab.titleRes),
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = stringResource(tab.titleRes),
            )
        }
    }
}

private val LibraryTab.icon: ImageVector
    get() = when (this) {
        LibraryTab.Songs -> LucideIcons.Music
        LibraryTab.Artists -> LucideIcons.Users
        LibraryTab.Albums -> LucideIcons.Disc
    }
