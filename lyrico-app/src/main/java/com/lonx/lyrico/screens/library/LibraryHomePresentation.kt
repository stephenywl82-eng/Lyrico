package com.lonx.lyrico.screens.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.lonx.lyrico.data.model.FloatingBarEffect

/** Single unified home presentation — Miuix-based layout, themed by SDW. */
@Composable
internal fun LibraryHomePresentation(
    tabs: List<LibraryTab>,
    selectedTab: LibraryTab,
    useNavigationRail: Boolean,
    floatingBottomBarEnabled: Boolean,
    floatingBarEffect: FloatingBarEffect,
    barBlurEnabled: Boolean,
    onTabSelected: (LibraryTab) -> Unit,
    onBottomPaddingChange: (Dp) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    LibraryHomeMiuix(
        tabs,
        selectedTab,
        useNavigationRail,
        floatingBottomBarEnabled,
        floatingBarEffect,
        barBlurEnabled,
        onTabSelected,
        onBottomPaddingChange,
        content,
    )
}
