package com.lonx.lyrico.ui.components.song

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.data.song.scan.LibraryScanProgress
import com.lonx.lyrico.data.song.scan.LibraryScanStage

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

    Text(
        text = title,
        modifier = modifier
            .padding(horizontal = 24.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
}
