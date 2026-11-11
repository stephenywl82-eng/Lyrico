package com.lonx.lyrico.ui.components.song

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.data.song.scan.LibraryScanProgress
import com.lonx.lyrico.data.song.scan.LibraryScanStage
import com.lonx.lyrico.utils.LibraryScanState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryScanProgressText(
    progress: LibraryScanProgress,
    modifier: Modifier = Modifier
) {
    val title = when (progress.stage) {
        LibraryScanStage.LISTING_FILES -> stringResource(R.string.scan_progress_listing)
        LibraryScanStage.READING_METADATA -> stringResource(
            R.string.scan_progress_reading,
            progress.current,
            progress.total
        )
        LibraryScanStage.WRITING_DATABASE -> stringResource(R.string.scan_progress_writing)
        LibraryScanStage.FINISHED -> stringResource(R.string.scan_progress_finished)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        LoadingIndicator(
            modifier = Modifier.size(72.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 主界面常驻的「扫描中」提示条。扫描进行时出现，展示阶段、进度与当前文件，
 * 风格与 SDWMP3_CN 一致。不依赖具体 tab，由 LibraryHomeScreen 全局挂载。
 */
@Composable
fun LibraryScanningBanner(
    scanState: LibraryScanState,
    modifier: Modifier = Modifier
) {
    val progress = scanState.progress ?: return
    val title = when (progress.stage) {
        LibraryScanStage.LISTING_FILES -> stringResource(R.string.scan_progress_listing)
        LibraryScanStage.READING_METADATA -> stringResource(
            R.string.scan_progress_reading,
            progress.current,
            progress.total
        )
        LibraryScanStage.WRITING_DATABASE -> stringResource(R.string.scan_progress_writing)
        LibraryScanStage.FINISHED -> stringResource(R.string.scan_progress_finished)
    }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (progress.total > 0) {
                LinearProgressIndicator(
                    progress = {
                        (progress.current.toFloat() / progress.total)
                            .coerceIn(0f, 1f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
            }
            if (!progress.currentFile.isNullOrBlank()) {
                Text(
                    text = progress.currentFile.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(0.dp))
        }
    }
}
