package com.lonx.lyrico.screens.library

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

/** Pull-to-refresh behavior shared by all three library presentations.
 *  The inner scroll container is responsible for its own content padding
 *  (via scaffoldContentPadding), so no extra top padding is applied here —
 *  otherwise the list would be pushed down by double the top inset. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryPullToRefresh(
    isRefreshing: Boolean, onRefresh: () -> Unit, modifier: Modifier,
    content: @Composable () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        content()
    }
}

@Composable
internal fun Modifier.libraryScrollFeedback(behavior: TopAppBarScrollBehavior): Modifier =
    this.nestedScroll(behavior.nestedScrollConnection)

@Composable
internal fun rememberLibraryOverscrollEffect(): OverscrollEffect? =
    null
