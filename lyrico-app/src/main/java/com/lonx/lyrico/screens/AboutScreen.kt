
package com.lonx.lyrico.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lonx.lyrico.App.Companion.OWNER_ID
import com.lonx.lyrico.App.Companion.REPO_NAME
import com.lonx.lyrico.App.Companion.TELEGRAM_GROUP_LINK
import com.lonx.lyrico.App.Companion.MOTO_MUSIC_GITHUB_URL
import com.lonx.lyrico.ui.components.library.LibraryDetailPage
import com.lonx.lyrico.BuildConfig
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.icons.LucideIcons
import com.lonx.lyrico.ui.components.icons.PreferenceIcon
import com.lonx.lyrico.ui.components.icons.PreferenceIconTone
import com.lonx.lyrico.ui.components.scaffoldContentPadding
import com.lonx.lyrico.utils.UpdateEffect
import com.lonx.lyrico.viewmodel.AboutViewModel
import com.lonx.lyrico.viewmodel.ProVersionTapResult
import com.lonx.lyrico.ui.components.pro.ProDeactivationDialog
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.OpensourceLicenceDestination
import com.ramcosta.composedestinations.generated.destinations.ProDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.lonx.lyrico.ui.components.sdw.ExpressiveSwitch as Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.lonx.lyrico.screens.library.libraryScrollFeedback
import androidx.compose.material3.MaterialTheme

@Composable
@Destination<RootGraph>(route = "about")
fun AboutScreen(
    navigator: DestinationsNavigator
) {
    val viewModel: AboutViewModel = koinViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val proActivated by viewModel.proActivated.collectAsStateWithLifecycle()
    var showDisablePro by rememberSaveable { mutableStateOf(false) }
    val version = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
    val onVersionClick: () -> Unit = {
        scope.launch {
            try {
                when (viewModel.onVersionTap()) {
                    ProVersionTapResult.ACTIVATED -> navigator.navigate(ProDestination())
                    ProVersionTapResult.CONFIRM_DEACTIVATION -> showDisablePro = true
                    ProVersionTapResult.NONE -> Unit
                }
            } catch (_: IOException) {
                Toast.makeText(context, R.string.pro_activation_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val updateEffect by viewModel.updateEffect.collectAsStateWithLifecycle(
        initialValue = UpdateEffect(R.string.about_check_update_default)
    )
    val aboutScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    LibraryDetailPage(
        title = stringResource(R.string.about_title),
        onBack = { navigator.popBackStack() },
        miuixTopBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(
                            LucideIcons.ArrowLeft,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                windowInsets = WindowInsets(0.dp),
                scrollBehavior = aboutScrollBehavior
            )
        }
    ) { paddingValues ->
        val outerMargin = 12.dp
        val pagePadding = scaffoldContentPadding(paddingValues = paddingValues, bottomExtra = 12.dp)
        val layoutDirection = LocalLayoutDirection.current
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val availableWidth = maxWidth - pagePadding.calculateLeftPadding(layoutDirection) -
                pagePadding.calculateRightPadding(layoutDirection) - outerMargin * 2
            val minCellWidth = 104.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
            val columnCount = ((availableWidth + 8.dp) / (minCellWidth + 8.dp)).toInt().coerceAtLeast(1)
            LazyColumn(
                modifier = Modifier
                    .libraryScrollFeedback(aboutScrollBehavior)
                    .fillMaxHeight(),
                contentPadding = pagePadding,
                overscrollEffect = null,
            ) {
                item {
                    
                        AboutSectionTitle(text = stringResource(R.string.about_app_info))
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                        ) {
                            AboutRow(
                                icon = LucideIcons.Info,
                                tone = PreferenceIconTone.Appearance,
                                title = stringResource(R.string.about_app_version),
                                summary = version,
                                showChevron = false,
                                onClick = onVersionClick,
                            )
                            if (proActivated) {
                                AboutRow(
                                    icon = LucideIcons.Crown,
                                    tone = PreferenceIconTone.Gold,
                                    title = "Lyrico Pro",
                                    summary = stringResource(R.string.pro_about_summary),
                                    onClick = { navigator.navigate(ProDestination()) },
                                )
                            }
                            AboutRow(
                                icon = LucideIcons.CodeXml,
                                tone = PreferenceIconTone.Tools,
                                title = stringResource(R.string.about_project_url),
                                summary = stringResource(R.string.about_project_url_sub),
                                onClick = {
                                    viewModel.openBrowser(
                                        context,
                                        "https://github.com/$OWNER_ID/$REPO_NAME"
                                    )
                                },
                            )
                            AboutRow(
                                icon = LucideIcons.Music,
                                tone = PreferenceIconTone.Tools,
                                title = stringResource(R.string.about_moto_music_url),
                                summary = stringResource(R.string.about_moto_music_url_sub),
                                onClick = {
                                    viewModel.openBrowser(context, MOTO_MUSIC_GITHUB_URL)
                                },
                            )
                            AboutRow(
                                icon = LucideIcons.Send,
                                tone = PreferenceIconTone.Library,
                                title = "Telegram",
                                summary = TELEGRAM_GROUP_LINK,
                                onClick = {
                                    viewModel.openBrowser(context, TELEGRAM_GROUP_LINK)
                                },
                            )
                            AboutRow(
                                icon = LucideIcons.Scale,
                                tone = PreferenceIconTone.Lyrics,
                                title = stringResource(R.string.title_opensource_licence),
                                onClick = {
                                    navigator.navigate(OpensourceLicenceDestination())
                                },
                            )
                            AboutRow(
                                icon = LucideIcons.Download,
                                tone = PreferenceIconTone.Teal,
                                title = stringResource(R.string.about_check_update),
                                summary = stringResource(
                                    updateEffect.messageRes,
                                    *updateEffect.formatArgs.toTypedArray()
                                ),
                                onClick = viewModel::checkUpdate,
                            )
                        }
                    
                }

        }
    }
    }
    ProDeactivationDialog(
        show = showDisablePro,
        onDismissRequest = { showDisablePro = false },
    )
}

@Composable
private fun AboutSectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AboutRow(
    icon: ImageVector,
    tone: PreferenceIconTone,
    title: String,
    summary: String? = null,
    showChevron: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreferenceIcon(icon, tone, Modifier.size(24.dp).padding(end = 12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showChevron) {
            Icon(
                LucideIcons.ArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

