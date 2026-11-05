
package com.lonx.lyrico.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.ui.components.library.LibraryDetailPage
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.icons.LucideIcons
import com.lonx.lyrico.ui.components.blur.BlurredTopBar
import com.lonx.lyrico.ui.components.blur.blurSource
import com.lonx.lyrico.ui.components.blur.rememberBarBlurBackdrop
import com.lonx.lyrico.ui.components.scaffoldContentPadding
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import my.nanihadesuka.compose.LazyColumnScrollbar
import my.nanihadesuka.compose.ScrollbarSelectionMode
import my.nanihadesuka.compose.ScrollbarSettings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior

@Composable
@Destination<RootGraph>(route = "opensource_licence")
fun OpenSourceLicenceScreen(
    navigator: DestinationsNavigator
) {
    val libsState = produceLibraries(R.raw.aboutlibraries)
    val libs = libsState.value
    val libraries = libs?.libraries
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current
    val topAppBarScrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val topBarBackdrop = rememberBarBlurBackdrop()
    val salt = false
    LibraryDetailPage(
        title = stringResource(R.string.title_opensource_licence),
        onBack = { navigator.popBackStack() },
        miuixTopBar = {
            BlurredTopBar(backdrop = topBarBackdrop) {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.title_opensource_licence), style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                navigator.popBackStack()
                            }
                        ) {
                            Icon(
                                imageVector = LucideIcons.ArrowLeft,
                                contentDescription = null
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                    windowInsets = WindowInsets(0.dp),
                    scrollBehavior = topAppBarScrollBehavior
                )
            }
        }
    ) { paddingValues ->
        LazyColumnScrollbar(
            state = listState,
            settings = ScrollbarSettings.Default.copy(
                selectionMode = ScrollbarSelectionMode.Full,
                thumbUnselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                thumbSelectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        ) {
            LazyColumn(
                state = listState,
                modifier = (Modifier
                    .blurSource(topBarBackdrop)
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection))
                    .fillMaxHeight(),
                contentPadding = scaffoldContentPadding(
                    paddingValues = paddingValues,
                    bottomExtra = 12.dp
                ),
            ) {

                libraries?.let { libs ->
                    items(libs, key = { it.uniqueId }) { library ->
                        val summary = buildString {
                            append(library.artifactVersion)
                            val licenseNames = library.licenses.map { it.name }.filter { it.isNotBlank() }
                            if (licenseNames.isNotEmpty()) {
                                append("\n")
                                append(licenseNames.joinToString(", "))
                            }
                        }
                        
                            Card(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .padding(top = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                ArrowRow(
                                    title = library.name,
                                    summary = summary,
                                    onClick = {
                                        library.website?.let {
                                            uriHandler.openUri(it)
                                        }
                                    }
                                )
                            }
                        
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun ArrowRow(
    title: String,
    summary: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = LucideIcons.ArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
