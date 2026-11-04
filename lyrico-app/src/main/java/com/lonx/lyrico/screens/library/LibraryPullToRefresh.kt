package com.lonx.lyrico.screens.library

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

/** Pull-to-refresh behavior shared by all three library presentations. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryPullToRefresh(
    isRefreshing: Boolean, onRefresh: () -> Unit, modifier: Modifier,
    contentPadding: PaddingValues, content: @Composable () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
internal fun Modifier.libraryScrollFeedback(behavior: TopAppBarScrollBehavior): Modifier =
    this.nestedScroll(behavior.nestedScrollConnection)

@Composable
internal fun rememberLibraryOverscrollEffect(): OverscrollEffect? =
    null
