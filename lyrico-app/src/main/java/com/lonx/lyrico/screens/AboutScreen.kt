package com.lonx.lyrico.screens

import android.R
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lonx.lyrico.App.Companion.OWNER_ID
import com.lonx.lyrico.App.Companion.REPO_NAME
import com.lonx.lyrico.App.Companion.TELEGRAM_GROUP_LINK
import com.lonx.lyrico.BuildConfig
import com.lonx.lyrico.R.string.action_back
import com.lonx.lyrico.R.string.about_title
import com.lonx.lyrico.data.repository.SettingsRepository
import com.lonx.lyrico.ui.components.icons.LucideIcons
import com.lonx.lyrico.ui.components.icons.PreferenceIcon
import com.lonx.lyrico.ui.components.icons.PreferenceIconTone
import com.lonx.lyrico.ui.components.library.EmptyStateKind
import com.lonx.lyrico.ui.components.library.IllustratedEmptyState
import com.lonx.lyrico.ui.components.scaffoldContentPadding
import com.lonx.lyrico.utils.UpdateEffect
import com.lonx.lyrico.viewmodel.AboutViewModel
import com.lonx.lyrico.viewmodel.ProVersionTapResult
import com.lonx.lyrico.viewmodel.UiError
import com.lonx.lyrico.R.string.about_app_info
import com.lonx.lyrico.R.string.about_check_update_default
import com.lonx.lyrico.R.string.about_app_version
import com.lonx.lyrico.R.string.about_auto_check_update
import com.lonx.lyrico.R.string.about_auto_check_update_sub
import com.lonx.lyrico.R.string.about_check_update
import com.lonx.lyrico.R.string.about_contributors
import com.lonx.lyrico.R.string.about_no_contributors
import com.lonx.lyrico.R.string.about_project_url
import com.lonx.lyrico.R.string.about_project_url_sub
import com.lonx.lyrico.R.string.load_failed
import com.lonx.lyrico.R.string.pro_about_summary
import com.lonx.lyrico.R.string.pro_activation_failed
import com.lonx.lyrico.R.string.pro_deactivation_failed
import com.lonx.lyrico.R.string.pro_disable
import com.lonx.lyrico.R.string.pro_disable_confirmation
import com.lonx.lyrico.R.string.refresh
import com.lonx.lyrico.R.string.title_opensource_licence
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.OpensourceLicenceDestination
import com.ramcosta.composedestinations.generated.destinations.ProDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Destination<RootGraph>(route = "about")
fun AboutScreen(
    navigator: DestinationsNavigator
) {
    val viewModel: AboutViewModel = koinViewModel()
    val checkUpdateEnabled by viewModel.checkUpdateEnabled.collectAsStateWithLifecycle()
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
                Toast.makeText(context, pro_activation_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val updateEffect by viewModel.updateEffect.collectAsStateWithLifecycle(
        initialValue = UpdateEffect(about_check_update_default)
    )
    val contributors by viewModel.contributors.collectAsStateWithLifecycle()
    val loading by viewModel.loadingContributors.collectAsStateWithLifecycle()
    val error by viewModel.contributorsError.collectAsStateWithLifecycle()

    val errorText = when (val e = error) {
        null -> null
        UiError.LoadFailed -> stringResource(load_failed)
        is UiError.Message -> e.text
    }

    val openProject = { viewModel.openBrowser(context, "https://github.com/$OWNER_ID/$REPO_NAME") }
    val openTelegram = { viewModel.openBrowser(context, TELEGRAM_GROUP_LINK) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(about_title)) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(
                            imageVector = LucideIcons.ArrowLeft,
                            contentDescription = stringResource(action_back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        val pagePadding = scaffoldContentPadding(paddingValues = paddingValues, bottomExtra = 16.dp)
        val layoutDirection = LocalLayoutDirection.current
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val availableWidth = maxWidth - pagePadding.calculateLeftPadding(layoutDirection) -
                pagePadding.calculateRightPadding(layoutDirection) - 24.dp
            val minCellWidth = 104.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
            val columnCount = ((availableWidth + 8.dp) / (minCellWidth + 8.dp)).toInt().coerceAtLeast(1)
            val contributorRows = remember(contributors, columnCount) { contributors.chunked(columnCount) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = pagePadding,
            ) {
                item {
                    Text(
                        text = stringResource(about_app_info),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                    )
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        InfoRow(
                            icon = LucideIcons.Info,
                            tone = PreferenceIconTone.Appearance,
                            title = stringResource(about_app_version),
                            subtitle = version,
                            onClick = onVersionClick,
                        )
                        if (proActivated) {
                            HorizontalDivider()
                            InfoRow(
                                icon = LucideIcons.Crown,
                                tone = PreferenceIconTone.Gold,
                                title = "Lyrico Pro",
                                subtitle = stringResource(pro_about_summary),
                                onClick = { navigator.navigate(ProDestination()) },
                            )
                        }
                        HorizontalDivider()
                        InfoRow(
                            icon = LucideIcons.CodeXml,
                            tone = PreferenceIconTone.Tools,
                            title = stringResource(about_project_url),
                            subtitle = stringResource(about_project_url_sub),
                            onClick = openProject,
                        )
                        HorizontalDivider()
                        InfoRow(
                            icon = LucideIcons.Send,
                            tone = PreferenceIconTone.Library,
                            title = "Telegram",
                            subtitle = TELEGRAM_GROUP_LINK,
                            onClick = openTelegram,
                        )
                        HorizontalDivider()
                        InfoRow(
                            icon = LucideIcons.Scale,
                            tone = PreferenceIconTone.Lyrics,
                            title = stringResource(title_opensource_licence),
                            onClick = { navigator.navigate(OpensourceLicenceDestination()) },
                        )
                        HorizontalDivider()
                        ListItem(
                            leadingContent = { PreferenceIcon(LucideIcons.CloudDownload, PreferenceIconTone.Pink) },
                            headlineContent = { Text(stringResource(about_auto_check_update)) },
                            supportingContent = { Text(stringResource(about_auto_check_update_sub)) },
                            trailingContent = {
                                Switch(
                                    checked = checkUpdateEnabled,
                                    onCheckedChange = viewModel::setCheckUpdateEnabled,
                                )
                            },
                        )
                        HorizontalDivider()
                        InfoRow(
                            icon = LucideIcons.Download,
                            tone = PreferenceIconTone.Teal,
                            title = stringResource(about_check_update),
                            subtitle = stringResource(updateEffect.messageRes, *updateEffect.formatArgs.toTypedArray()),
                            onClick = viewModel::checkUpdate,
                        )
                    }
                }

                item {
                    Text(
                        text = stringResource(about_contributors),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                    )
                }

                when {
                    loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                            }
                        }
                    }

                    errorText != null -> {
                        item {
                            IllustratedEmptyState(
                                kind = EmptyStateKind.Artists,
                                title = errorText,
                                modifier = Modifier.fillMaxWidth(),
                                actionText = stringResource(refresh),
                                icon = LucideIcons.RefreshCw,
                                onAction = viewModel::loadContributors,
                            )
                        }
                    }

                    contributors.isEmpty() -> {
                        item {
                            IllustratedEmptyState(
                                kind = EmptyStateKind.Artists,
                                title = stringResource(about_no_contributors),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    else -> {
                        items(
                            items = contributorRows,
                            key = { row -> row.first().id },
                        ) { row ->
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                row.forEach { contributor ->
                                    ContributorGridItem(
                                        name = contributor.login,
                                        avatarUrl = contributor.avatar_url,
                                        onClick = { viewModel.openBrowser(context, contributor.html_url) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(columnCount - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDisablePro) {
        val settings: SettingsRepository = koinInject()
        var saving by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!saving) showDisablePro = false },
            title = { Text(stringResource(pro_disable)) },
            text = { Text(stringResource(pro_disable_confirmation)) },
            confirmButton = {
                TextButton(
                    enabled = !saving,
                    onClick = {
                        if (!saving) {
                            saving = true
                            scope.launch {
                                try {
                                    settings.deactivatePro()
                                    showDisablePro = false
                                } catch (_: IOException) {
                                    Toast.makeText(context, pro_deactivation_failed, Toast.LENGTH_SHORT).show()
                                } finally {
                                    saving = false
                                }
                            }
                        }
                    },
                ) { Text(stringResource(pro_disable)) }
            },
            dismissButton = {
                TextButton(onClick = { if (!saving) showDisablePro = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tone: PreferenceIconTone,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        modifier = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier,
        leadingContent = { PreferenceIcon(icon, tone) },
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) { { Text(subtitle) } } else null,
        trailingContent = if (onClick != null) {
            {
                Icon(
                    imageVector = LucideIcons.ArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else null,
    )
}

@Composable
private fun ContributorGridItem(name: String, avatarUrl: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
    val color = MaterialTheme.colorScheme.onSurface
    val background = MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier.size(48.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = name,
            modifier = Modifier.fillMaxWidth(),
            style = style,
            color = color,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
