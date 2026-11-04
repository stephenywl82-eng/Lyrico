
package com.lonx.lyrico.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.icons.PreferenceIconTone
import com.lonx.lyrico.ui.components.icons.LucideIcons
import com.lonx.lyrico.ui.components.library.EmptyStateKind
import com.lonx.lyrico.ui.components.library.IllustratedEmptyState
import com.lonx.lyrico.ui.components.poster.PosterPreviewMenuAction
import com.lonx.lyrico.viewmodel.ArtistPosterFile
import com.lonx.lyrico.viewmodel.ArtistPosterFolder
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import androidx.compose.material3.MaterialTheme

@Composable
internal fun ArtistPosterFolderContents(
    folder: ArtistPosterFolder,
    revision: Long,
    isLoading: Boolean,
    enabled: Boolean,
    hasError: Boolean,
    onChangeFolder: () -> Unit,
    onRefresh: () -> Unit,
    onFileMenu: (ArtistPosterFile) -> Unit,
    onCrop: (ArtistPosterFile, android.graphics.Bitmap) -> Unit,
    onRename: (ArtistPosterFile) -> Unit,
    onArtistDetails: (ArtistPosterFile) -> Unit,
    onPreviewSelection: (String) -> Unit,
    previewUri: String?,
    onDismissPreview: () -> Unit,
    padding: PaddingValues,
    scrollBehavior: ScrollBehavior
) {
    val salt = false
    val files = folder.posters.orEmpty()
    val scrollModifier = (Modifier
        .scrollEndHaptic()
        .overScrollVertical()
        .nestedScroll(scrollBehavior.nestedScrollConnection))
        .fillMaxSize()
    if (files.isEmpty()) {
        // 空/加载态也必须是可滚动容器：纯 Column 在内容不足一屏时完全不能滚动，
        // 和列表页（空状态作为列表项）不一致。
        LazyColumn(
            modifier = scrollModifier,
            contentPadding = padding,
            overscrollEffect = null,
        ) {
            item(key = "folder") {
                ArtistPosterDirectoryCard(folder, Modifier.padding(top = 12.dp))
            }
            item(key = "state") {
                // 撑满一屏，保证内容一定超过视口，短内容也能拖动。
                val blockModifier = Modifier.fillMaxWidth().fillParentMaxHeight()
                if (isLoading) {
                    Box(blockModifier, contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val unavailable = folder.posters == null
                    IllustratedEmptyState(
                        kind = EmptyStateKind.Cover,
                        title = stringResource(if (unavailable) R.string.artist_poster_folder_unavailable else R.string.artist_poster_no_files),
                        summary = stringResource(if (hasError) R.string.artist_poster_folder_error else if (unavailable) R.string.artist_poster_folders_empty else R.string.artist_poster_empty_hint),
                        icon = if (unavailable) LucideIcons.Folder else LucideIcons.RefreshCw,
                        actionText = stringResource(if (unavailable) R.string.artist_poster_folder_change else R.string.action_refresh_folder),
                        onAction = if (unavailable) onChangeFolder else onRefresh,
                        modifier = blockModifier
                    )
                }
            }
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = padding,
        modifier = scrollModifier,
        overscrollEffect = null
    ) {
        item(key = "folder", span = { GridItemSpan(maxLineSpan) }) {
            ArtistPosterDirectoryCard(folder, Modifier.padding(top = 12.dp))
        }
        if (isLoading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(12.dp))
            }
        }
        if (hasError) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(R.string.artist_poster_folder_error),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        items(files, key = { it.uri }) { file ->
            
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = if (enabled && !isLoading) { { onFileMenu(file) } } else null,
                    showIndication = true
                ) {
                    PosterImage(file, revision, Modifier.fillMaxWidth().aspectRatio(4f / 5f), ContentScale.Crop)
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(
                            file.artistName ?: stringResource(R.string.label_artist_image_unmatched),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            file.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            
        }
    }
    val previewIndex = files.indexOfFirst { it.uri == previewUri }
    if (previewIndex >= 0) {
        PosterPreview(files, previewIndex, revision,
            onRename = onRename, onArtistDetails = onArtistDetails, onPreviewSelection = onPreviewSelection,
            onCrop = onCrop, enabled = enabled,
            onDismiss = onDismissPreview)
    }
}

/**
 * 文件夹信息：只展示名称与完整路径，不换行截断、也不放操作。
 * 切换文件夹的入口在顶栏菜单里，页面上不再重复一个。
 */
@Composable
private fun ArtistPosterDirectoryCard(
    folder: ArtistPosterFolder,
    modifier: Modifier = Modifier
) {

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = folder.path,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun PosterImage(file: ArtistPosterFile, revision: Long, modifier: Modifier, scale: ContentScale) {
    val salt = false
    val context = LocalContext.current
    val request = remember(file, revision, context) {
        ImageRequest.Builder(context)
            .data(if (file.name.endsWith(".mp4", true)) file else file.uri.toUri())
            .memoryCacheKey("poster:${file.uri}:${file.lastModified}:$revision")
            .diskCacheKey("poster:${file.uri}:${file.lastModified}:$revision")
            .build()
    }
    var failed by remember(file, revision) { mutableStateOf(false) }
    var loading by remember(file, revision) { mutableStateOf(true) }
    val background = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.background(background), contentAlignment = Alignment.Center) {
        AsyncImage(request, file.name, Modifier.fillMaxSize(), contentScale = scale,
            onSuccess = { loading = false; failed = false }, onError = { loading = false; failed = true })
        if (loading) {
            CircularProgressIndicator(size = 24.dp)
        }
        if (failed) Column(horizontalAlignment = Alignment.CenterHorizontally) {
            
                Icon(LucideIcons.Image, null)
                Text(stringResource(R.string.artist_poster_preview_error), fontSize = MaterialTheme.typography.bodyMedium.fontSize)
            
        }
    }
}

@Composable
private fun PosterPreview(files: List<ArtistPosterFile>, initialPage: Int, revision: Long,
    onRename: (ArtistPosterFile) -> Unit, onArtistDetails: (ArtistPosterFile) -> Unit,
    onPreviewSelection: (String) -> Unit, onCrop: (ArtistPosterFile, android.graphics.Bitmap) -> Unit,
    enabled: Boolean, onDismiss: () -> Unit) {
    val renameLabel = stringResource(R.string.action_rename)
    val detailsLabel = stringResource(R.string.poster_artist_details)
    val unmatchedLabel = stringResource(R.string.poster_artist_unmatched)
    com.lonx.lyrico.ui.components.poster.ArtistPosterPreview(
        items = remember(files, revision) { files.map { file ->
            com.lonx.lyrico.ui.components.poster.PosterPreviewMedia(file.name, file.uri,
                isVideo = file.name.endsWith(".mp4", true), revision = revision + file.lastModified)
        } }, initialPage = initialPage,
        editingEnabled = enabled,
        onCrop = { index, bitmap -> onCrop(files[index], bitmap) },
        menuActions = { index ->
            val file = files[index]
            listOf(
                PosterPreviewMenuAction(
                    title = renameLabel, icon = LucideIcons.Pencil, tone = PreferenceIconTone.Appearance,
                    enabled = enabled, onClick = { onRename(file) }),
                PosterPreviewMenuAction(
                    title = detailsLabel, icon = LucideIcons.Users, tone = PreferenceIconTone.Tools,
                    enabled = enabled && file.artistId != null,
                    summary = if (file.artistId == null) unmatchedLabel else null,
                    onClick = { onArtistDetails(file) }),
            )
        },
        onCurrentChanged = { onPreviewSelection(files[it].uri) }, onDismiss = onDismiss
    )
}
