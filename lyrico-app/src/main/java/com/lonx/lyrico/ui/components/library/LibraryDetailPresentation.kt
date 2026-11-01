package com.lonx.lyrico.ui.components.library

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.lonx.lyrico.screens.library.LibrarySelectionState
import top.yukonga.miuix.kmp.basic.ScrollBehavior

/** Detail pages share layout and actions; single unified (Miuix-based) implementation, themed by SDW. */
@Composable
internal fun LibraryDetailPage(
    title: String,
    onBack: () -> Unit,
    selection: LibrarySelectionState? = null,
    snackbarHost: @Composable () -> Unit = {},
    miuixTopBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    LibraryDetailPageMiuix(miuixTopBar, snackbarHost, content)
}

@Composable
internal fun Modifier.libraryDetailScroll(behavior: ScrollBehavior): Modifier =
    libraryDetailScrollMiuix(behavior)

@Composable
internal fun LibraryDetailIconButton(icon: ImageVector, description: String?, onClick: () -> Unit) {
    LibraryDetailIconButtonMiuix(icon, description, onClick)
}

@Composable
internal fun LibraryDetailTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LibraryDetailTabsMiuix(tabs, selectedIndex, onSelected, modifier)
}

internal enum class LibraryDetailTextTone {
    Title,
    Body,
    Secondary,
}

@Composable
internal fun LibraryDetailText(
    text: String,
    modifier: Modifier = Modifier,
    tone: LibraryDetailTextTone = LibraryDetailTextTone.Body,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    LibraryDetailTextMiuix(text, modifier, tone, maxLines, overflow)
}
