package com.lonx.lyrico.ui.components.library

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material3.MaterialTheme
import com.lonx.lyrico.ui.theme.LocalHapticEnabled

/**
 * 专辑卡片容器（Miuix）。
 * 原先按引擎分支渲染 Salt Card / Miuix Card，Phase 3 收拢双引擎后只保留 Miuix 分支。
 */
@Composable
internal fun LibraryAlbumSurface(
    modifier: Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val hapticEnabled = LocalHapticEnabled.current
    val onLongPress = onLongClick?.let { action ->
        {
            if (hapticEnabled) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            action()
        }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        onLongPress = onLongPress,
    ) {
        BasicComponent(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant),
            insideMargin = PaddingValues(8.dp),
        ) { content() }
    }
}
