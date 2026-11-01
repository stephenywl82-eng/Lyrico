package com.lonx.lyrico.ui.components.lyrics

import com.lonx.lyrico.ui.components.icons.LucideIcons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.utils.UiMessage
import com.lonx.lyrico.ui.components.library.EmptyStateKind
import com.lonx.lyrico.ui.components.library.IllustratedEmptyState

@Composable
fun LyricsPreviewPane(
    lyricsText: String?,
    isLoading: Boolean,
    error: UiMessage?,
    loadingText: String,
    failedText: String,
    emptyText: String,
    onCopyLyrics: () -> Unit,
    modifier: Modifier = Modifier,
    showCopyAction: Boolean = true,
    /**
     * 正文自带的内边距。放进 RoundedColumn / groupContent 卡片时由调用方指定与相邻 Item
     * 对齐的值，避免和外壳的内边距叠加。
     */
    contentPadding: PaddingValues = PaddingValues(12.dp),
) {
    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> item("loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outline,
                            )
                            Text(
                                text = loadingText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                error != null -> item("error") {
                    Text(
                        modifier = Modifier.padding(contentPadding),
                        text = error.asString() ?: failedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                lyricsText.isNullOrBlank() -> item("empty") {
                    IllustratedEmptyState(
                        kind = EmptyStateKind.Lyrics,
                        title = emptyText,
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }

                else -> item("lyrics") {
                    Text(
                        modifier = Modifier.padding(contentPadding),
                        text = lyricsText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }

        if (showCopyAction && !lyricsText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        shape = CircleShape
                    )
            ) {
                IconButton(
                    onClick = onCopyLyrics,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = LucideIcons.Copy,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
