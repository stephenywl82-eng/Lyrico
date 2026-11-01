package com.lonx.lyrico.screens.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.utils.overScrollVertical
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.ScrollBehavior

/** Pull-to-refresh behavior shared by all three library presentations. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryPullToRefresh(
    isRefreshing: Boolean, onRefresh: () -> Unit, modifier: Modifier,
    contentPadding: PaddingValues, topAppBarScrollBehavior: ScrollBehavior,
    refreshTexts: List<String>, content: @Composable () -> Unit,
) {
    top.yukonga.miuix.kmp.basic.PullToRefresh(
        isRefreshing = isRefreshing, onRefresh = onRefresh, modifier = modifier,
        contentPadding = contentPadding, topAppBarScrollBehavior = topAppBarScrollBehavior,
        refreshTexts = refreshTexts,
    ) { content() }
}

@Composable
internal fun Modifier.libraryScrollFeedback(behavior: ScrollBehavior): Modifier =
    
        scrollEndHaptic().overScrollVertical().nestedScroll(behavior.nestedScrollConnection)
    
@Composable
internal fun rememberLibraryOverscrollEffect(): OverscrollEffect? =
    null
