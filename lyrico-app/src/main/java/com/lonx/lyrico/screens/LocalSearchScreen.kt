package com.lonx.lyrico.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.viewmodel.LocalSearchViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import com.lonx.lyrico.ui.components.searchBar.SearchBar
import com.lonx.lyrico.ui.components.searchBar.SearchBarConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Destination<RootGraph>(route = "local_search")
fun LocalSearchScreen(
    keyword: String? = null,
    navigator: DestinationsNavigator
) {
    val viewModel: LocalSearchViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val groupedSongs by viewModel.groupedSongs.collectAsState()

    // 初始化搜索关键词
    LaunchedEffect(keyword) {
        if (keyword != null && keyword != uiState.searchQuery) {
            viewModel.onSearchQueryChanged(keyword)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                SearchBar(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier.padding(start = 8.dp),
                    config = SearchBarConfig(
                        placeholderTextString = "搜索标题/歌手/专辑..."
                    ),
                    onSearch = { viewModel.search() },
                )
            }
            // Results
            if (uiState.isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    groupedSongs.forEach { category ->
                        stickyHeader {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = category.category,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        items(items = category.songs, key = { it.filePath }) { song ->
                            SongListItem(song = song, navigator = navigator)
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}