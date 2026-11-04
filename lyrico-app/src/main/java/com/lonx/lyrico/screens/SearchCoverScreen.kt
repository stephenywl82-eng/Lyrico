
package com.lonx.lyrico.screens

import com.lonx.lyrico.ui.components.icons.LucideIcons
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton as MaterialTextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.library.EmptyStateKind
import com.lonx.lyrico.ui.components.library.IllustratedEmptyState
import com.lonx.lyrico.ui.components.bar.SearchBar
import com.lonx.lyrico.ui.components.rememberTintedPainter
import com.lonx.lyrico.ui.components.scaffoldTopAppBarInsetsPadding
import com.lonx.lyrico.ui.components.scaffoldTopHorizontalPadding
import com.lonx.lyrico.ui.theme.LyricoColors
import com.lonx.lyrico.ui.theme.isDarkTheme
import com.lonx.lyrico.utils.MusicMatchUtils
import com.lonx.lyrico.viewmodel.CoverSearchResult
import com.lonx.lyrico.viewmodel.CoverSearchUiState
import com.lonx.lyrico.viewmodel.CoverSearchViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.result.ResultBackNavigator
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.material3.MaterialTheme

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Destination<RootGraph>(route = "search_cover")
fun SearchCoverScreen(
    keyword: String?,
    resultNavigator: ResultBackNavigator<String>
) {
    val viewModel: CoverSearchViewModel = koinViewModel()
    val uiState by viewModel.coverUiState.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { uiState.availableSources.size + 1 }
    val coverSearchState = rememberTextFieldState(initialText = keyword ?: uiState.searchKeyword)
    val salt = false

    // 用于缓存图片尺寸的Map
    val imageSizeCache = remember { mutableStateOf<Map<String, Pair<Int, Int>>>(emptyMap()) }

    LaunchedEffect(keyword) {
        keyword?.let { viewModel.performCoverSearch(it) }
    }

    LaunchedEffect(uiState.selectedSource?.id, uiState.availableSources) {
        val selectedSource = uiState.selectedSource
        val targetPage = if (selectedSource == null) {
            0
        } else {
            uiState.availableSources.indexOfFirst { it.id == selectedSource.id } + 1
        }
        if (targetPage >= 0 && pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState, uiState.availableSources.map { it.id }) {
        snapshotFlow { pagerState.currentPage }
            .collectLatest { page ->
                if (page == 0) {
                    viewModel.onAllSourcesSelected()
                } else {
                    uiState.availableSources.getOrNull(page - 1)?.let {
                        viewModel.onSourceSelected(it)
                    }
                }
            }
    }
    val localSegments = remember(uiState.searchKeyword) {
        MusicMatchUtils.splitToSegments(uiState.searchKeyword)
            .ifEmpty { listOf(uiState.searchKeyword.trim()) }
            .filter { it.isNotBlank() }
    }

    val searchField: @Composable (Modifier) -> Unit = { fieldModifier ->
        SearchBar(
            modifier = fieldModifier,
            state = coverSearchState,
            placeholder = stringResource(id = R.string.search_cover_placeholder),
            onSearch = { searchKeyword ->
                keyboardController?.hide()
                viewModel.onCoverKeywordChanged(searchKeyword)
                viewModel.performCoverSearch()
            },
            actions = {
                val submit = {
                    keyboardController?.hide()
                    viewModel.onCoverKeywordChanged(coverSearchState.text.toString())
                    viewModel.performCoverSearch()
                }
                
                    MaterialTextButton(onClick = submit) {
                        Text(
                            text = stringResource(id = R.string.action_search),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .scaffoldTopAppBarInsetsPadding()
                    .padding(vertical = 8.dp)
            ) {
                searchField(Modifier.padding(horizontal = 12.dp))
            }
        }
    ) { paddingValues ->
        SearchCoverContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldTopHorizontalPadding(paddingValues)),
            viewModel = viewModel,
            uiState = uiState,
            pagerState = pagerState,
            localSegments = localSegments,
            imageSizeCache = imageSizeCache,
            salt = false,
            onCoverClick = { cover -> resultNavigator.navigateBack(cover.url) }
        )
    }
}

@Composable
private fun SearchCoverContent(
    modifier: Modifier,
    viewModel: CoverSearchViewModel,
    uiState: CoverSearchUiState,
    pagerState: PagerState,
    localSegments: List<String>,
    imageSizeCache: MutableState<Map<String, Pair<Int, Int>>>,
    salt: Boolean,
    onCoverClick: (CoverSearchResult) -> Unit,
) {
    val scope = rememberCoroutineScope()
    Column(modifier = modifier) {
        if (uiState.isInitializing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val tabs = listOf(
            SourcePillTab(
                label = stringResource(id = R.string.search_type_all),
                imageVector = LucideIcons.Search
            )
        ) + uiState.availableSources.map { source ->
            SourcePillTab(
                label = source.labelRes?.let { stringResource(id = it) } ?: source.name,
                iconPath = source.iconPath
            )
        }
        SourcePillTabRow(
            tabs = tabs,
            selectedTabIndex = pagerState.targetPage,
            tabStyle = uiState.searchSourceTabStyle,
            onTabSelected = { index ->
                scope.launch {
                    pagerState.animateScrollToPage(index)
                }
            },
            modifier = Modifier.padding(bottom = 10.dp)
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val source = if (page == 0) {
                null
            } else {
                uiState.availableSources.getOrNull(page - 1)
            }
            val results = if (page == 0) {

                uiState.coverResults
                    .mapIndexed { index, cover ->
                        val score = MusicMatchUtils.calculateCoverMatchScore(
                            localSegments = localSegments,
                            coverTitle = cover.title,
                            coverArtist = cover.artist,
                            rankIndex = index
                        )

                        cover to score
                    }
                    .sortedWith(
                        compareByDescending<Pair<CoverSearchResult, Double>> { (_, score) ->
                            score
                        }.thenByDescending { (cover, _) ->
                            imageSizeCache.value[cover.url]?.let { it.first * it.second } ?: 0
                        }
                    )
                    .map { it.first }
            } else {
                uiState.coverResults.filter { it.source == source }
            }
            val sourceIds = if (source == null) {
                uiState.availableSources.map { it.id }
            } else {
                listOf(source.id)
            }
            val loadMoreError = sourceIds.firstNotNullOfOrNull { sourceId ->
                uiState.loadMoreErrors[sourceId]?.let { sourceId to it }
            }
            val isLoadingMore = sourceIds.any { it in uiState.loadingMoreSourceIds }
            val canLoadMore = sourceIds.any { uiState.hasMoreBySource[it] == true }
            val searchError = if (source == null) {
                uiState.searchErrors.values.firstOrNull()
            } else {
                uiState.searchErrors[source.id]
            }

            when {
                uiState.isSearching && (page == 0 || source == uiState.selectedSource) -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                searchError != null && results.isEmpty() -> {
                    IllustratedEmptyState(
                        kind = EmptyStateKind.Cover,
                        title = stringResource(R.string.load_failed),
                        summary = searchError.asString(),
                        modifier = Modifier.fillMaxSize(),
                        actionText = stringResource(R.string.refresh),
                        icon = LucideIcons.RefreshCw,
                        onAction = { viewModel.performCoverSearch() },
                    )
                }

                results.isEmpty() -> {
                    IllustratedEmptyState(
                        kind = EmptyStateKind.Cover,
                        title = stringResource(R.string.search_no_results),
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                else -> {
                    val gridState = rememberLazyGridState()
                    LaunchedEffect(uiState.searchKeyword) {
                        gridState.scrollToItem(0)
                    }
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            results,
                            key = { "${it.source.id}\u0000${it.id}\u0000${it.url}" }
                        ) { cover ->
                            CoverGridItem(
                                cover = cover,
                                onClick = { onCoverClick(cover) },
                                onImageSizeLoaded = { url, size ->
                                    imageSizeCache.value = imageSizeCache.value + (url to size)
                                }
                            )
                        }

                        when {
                            isLoadingMore -> {
                                item(
                                    key = "cover_search_loading_more",
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(size = 20.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.search_loading_more),
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            loadMoreError != null -> {
                                item(
                                    key = "cover_search_load_more_error",
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        TextButton(
                                            text = stringResource(R.string.search_load_more_failed),
                                            onClick = {
                                                viewModel.loadNextPage(
                                                    loadMoreError.first
                                                )
                                            },
                                            colors = ButtonDefaults.textButtonColorsPrimary()
                                        )
                                    }
                                }
                            }

                            canLoadMore -> {
                                item(
                                    key = "cover_search_load_more",
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        TextButton(
                                            text = stringResource(R.string.search_load_more),
                                            onClick = {
                                                viewModel.loadNextPage(source?.id)
                                            },
                                            colors = ButtonDefaults.textButtonColorsPrimary()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoverGridItem(
    cover: CoverSearchResult,
    onClick: () -> Unit,
    onImageSizeLoaded: (String, Pair<Int, Int>) -> Unit = { _, _ -> }
) {
    var imageSize by remember(cover.url) { mutableStateOf<Pair<Int, Int>?>(null) }
    val salt = false

    LaunchedEffect(cover.url) {
        if (cover.url.isNotBlank()) {
            imageSize = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeStream(
                        java.net.URL(cover.url).openStream(),
                        null,
                        options
                    )
                    if (options.outWidth > 0 && options.outHeight > 0) {
                        options.outWidth to options.outHeight
                    } else null
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            imageSize?.let { size ->
                onImageSizeLoaded(cover.url, size)
            }
        }
    }

    val cardContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(CardDefaults.CornerRadius))
                .clickable(onClick = onClick),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(LyricoColors.coverPlaceholder)
            ) {
                AsyncImage(
                    model = cover.url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    placeholder = rememberTintedPainter(
                        painter = painterResource(R.drawable.ic_album_24dp),
                        tint = LyricoColors.coverPlaceholderIcon
                    ),
                    error = rememberTintedPainter(
                        painter = painterResource(R.drawable.ic_album_24dp),
                        tint = LyricoColors.coverPlaceholderIcon
                    )
                )

                val textColor = if (isDarkTheme) Color.Black else Color.White

                // 来源标签 - 左上角
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = cover.source.labelRes?.let { stringResource(it) } ?: cover.source.name,
                        color = textColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 尺寸标签 - 右下角
                imageSize?.let {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${it.first}×${it.second}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 歌曲信息
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                val title = cover.title.ifBlank { stringResource(R.string.unknown_title) }
                val artist = cover.artist.ifBlank { stringResource(R.string.unknown_artist) }
                val album = cover.album.ifBlank { stringResource(R.string.unknown_album) }
                val date = cover.date.ifBlank { stringResource(R.string.unknown_year) }
                
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = album,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                
            }
        }
    }

        Card(
            modifier = Modifier,
            colors = CardDefaults.defaultColors(
                color = MaterialTheme.colorScheme.secondaryContainer
            )
        ) { cardContent() }
    
}
