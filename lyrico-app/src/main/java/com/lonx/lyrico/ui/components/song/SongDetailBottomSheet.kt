package com.lonx.lyrico.ui.components.song

import com.lonx.lyrico.ui.components.icons.LucideIcons
import android.content.ClipData
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lonx.lyrico.BuildConfig
import com.lonx.lyrico.R
import com.lonx.lyrico.data.model.entity.SongEntity
import com.lonx.lyrico.data.model.entity.getUri
import com.lonx.lyrico.ui.components.CoverRequest
import com.lonx.lyrico.ui.components.sdw.SdwBottomSheet

import kotlinx.coroutines.launch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SongDetailBottomSheet(
    show: Boolean,
    song: SongEntity,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val dateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }
    val salt = false
    val copiedMessage = stringResource(R.string.msg_copied_to_clipboard)
    val copyToClipboard: (String) -> Unit = { text ->
        coroutineScope.launch {
            val clipData = ClipData.newPlainText("copy detail", text)
            val clipEntry = ClipEntry(clipData)
            clipboardManager.setClipEntry(clipEntry)
            Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    val rows: List<Pair<String, String?>> = buildList {
        add(stringResource(R.string.label_album) to song.album)
        add(stringResource(R.string.label_year) to song.date)
        add(stringResource(R.string.label_genre) to song.genre)
        add(stringResource(R.string.label_track_number) to song.trackerNumber)
        add(
            stringResource(R.string.label_duration) to if (song.durationMilliseconds > 0) {
                val min = song.durationMilliseconds / 60000
                val sec = (song.durationMilliseconds % 60000) / 1000
                String.format("%d:%02d", min, sec)
            } else null
        )
        add(stringResource(R.string.label_bitrate) to if (song.bitrate > 0) "${song.bitrate} kbps" else null)
        add(stringResource(R.string.label_sample_rate) to if (song.sampleRate > 0) "${song.sampleRate} Hz" else null)
        add(stringResource(R.string.label_channels) to if (song.channels > 0) "${song.channels}" else null)
        add(
            stringResource(R.string.label_date_added) to if (song.fileAdded > 0) {
                dateFormat.format(Date(song.fileAdded))
            } else null
        )
        add(
            stringResource(R.string.label_date_modified) to if (song.fileLastModified > 0) {
                dateFormat.format(Date(song.fileLastModified))
            } else null
        )
        add(stringResource(R.string.label_file_path) to song.filePath)
        add(
            stringResource(R.string.label_file_size) to if (song.fileSize > 0) {
                Formatter.formatFileSize(context, song.fileSize)
            } else null
        )
        if (BuildConfig.DEBUG) {
            add("文件URI" to song.uri)
            add("文件ID" to song.id.toString())
            add("文件名" to song.fileName)
        }
    }

    val title = song.title.takeIf { !it.isNullOrBlank() } ?: song.fileName
    val artist = song.artist.takeIf { !it.isNullOrBlank() } ?: stringResource(R.string.unknown_artist)

    SdwBottomSheet(
        show = show,
        enableNestedScroll = false,
        onDismissRequest = {
            onDismissRequest()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model = CoverRequest(song.getUri, song.fileLastModified),
                    contentDescription = stringResource(R.string.cd_cover),
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.ic_album_24dp),
                    error = painterResource(R.drawable.ic_album_24dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Card(
                modifier = Modifier.padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                )
            ) {
                rows.forEach { (label, value) ->
                    SongDetailItem(label = label, value = value, onCopy = copyToClipboard)
                }
            }
        }
    }
}

@Composable
fun SongDetailItem(
    label: String,
    value: String?,
    onCopy: ((String) -> Unit)? = null
) {
    if (value.isNullOrBlank()) return

    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            if (onCopy != null) {
                IconButton(
                    modifier = Modifier.size(20.dp),
                    onClick = { onCopy(value) }
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = LucideIcons.Copy,
                        contentDescription = "复制"
                    )
                }
            }
        }
    }
}
