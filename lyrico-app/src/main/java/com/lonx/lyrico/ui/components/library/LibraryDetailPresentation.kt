package com.lonx.lyrico.ui.components.library

import androidx.compose.foundation.layout.*
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.lonx.lyrico.screens.library.LibrarySelectionState

/** Detail pages share layout and actions; single unified M3 Expressive implementation, themed by SDW. */
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
internal fun Modifier.libraryDetailScroll(behavior: TopAppBarScrollBehavior): Modifier =
    this.nestedScroll(behavior.nestedScrollConnection)

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
