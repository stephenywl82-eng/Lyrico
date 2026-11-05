
package com.lonx.lyrico.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lonx.lyrico.R
import com.lonx.lyrico.data.repository.SettingsRepository
import com.lonx.lyrico.ui.components.blur.BlurredTopBar
import com.lonx.lyrico.ui.components.blur.blurSource
import com.lonx.lyrico.ui.components.blur.rememberBarBlurBackdrop
import com.lonx.lyrico.ui.components.icons.LucideIcons
import com.lonx.lyrico.ui.components.icons.PreferenceIcon
import com.lonx.lyrico.ui.components.icons.PreferenceIconTone
import com.lonx.lyrico.ui.components.library.*
import com.lonx.lyrico.ui.components.scaffoldContentPadding
import com.lonx.lyrico.ui.theme.isDarkTheme
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import com.lonx.lyrico.ui.components.pro.ProDeactivationDialog
import com.lonx.lyrico.ui.components.pro.SponsorMethod
import com.lonx.lyrico.ui.components.pro.SponsorQrCodeSheet
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
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
internal fun AboutProEntry(onClick: () -> Unit) {
    
        ArrowRow(
            icon = LucideIcons.Crown,
            tone = PreferenceIconTone.Gold,
            title = "Lyrico Pro",
            summary = stringResource(R.string.pro_about_summary),
            onClick = onClick,
        )
    
}

@Composable
@Destination<RootGraph>(route = "pro")
fun ProScreen(navigator: DestinationsNavigator) {
    val settings: SettingsRepository = koinInject()
    var showDisablePro by rememberSaveable { mutableStateOf(false) }
    val onDisable: () -> Unit = { showDisablePro = true }
    val activated by settings.proActivated.collectAsStateWithLifecycle(initialValue = false)
    var selectedMethod by rememberSaveable { mutableIntStateOf(0) }
    var showQrCode by rememberSaveable { mutableStateOf(false) }
    val salt = false
    val behavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val backdrop = rememberBarBlurBackdrop()
    LibraryDetailPage(
        title = "Lyrico Pro",
        onBack = { navigator.popBackStack() },
        miuixTopBar = {
            BlurredTopBar(backdrop = backdrop) {
                TopAppBar(
                    title = { Text("Lyrico Pro", style = MaterialTheme.typography.titleLarge) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                    windowInsets = WindowInsets(0.dp),
                    navigationIcon = {
                        IconButton(onClick = { navigator.popBackStack() }) {
                            Icon(LucideIcons.ArrowLeft, stringResource(R.string.action_back))
                        }
                    },
                    scrollBehavior = behavior,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().then(
                Modifier.blurSource(backdrop).libraryDetailScroll(behavior)
            ),
            contentPadding = scaffoldContentPadding(padding, bottomExtra = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (activated) {
                item(key = "pro_identity") { ProIdentity() }
                item(key = "support_code") {
                    val methods: @Composable ColumnScope.() -> Unit = {
                        SponsorMethod.entries.forEachIndexed { index, method ->
                            val onClick = {
                                selectedMethod = index
                                showQrCode = true
                            }
                            
                                ArrowRow(
                                    icon = method.icon,
                                    tone = method.iconTone,
                                    title = stringResource(method.titleRes),
                                    onClick = onClick,
                                )
                            
                        }
                    }
                    Column {
                        SectionTitle(stringResource(R.string.sponsor_title))
                        Card(Modifier.padding(horizontal = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), content = methods)
                    }
                }
                item(key = "pro_settings") {
                    
                        Column {
                            SectionTitle(stringResource(R.string.pro_settings))
                            Card(Modifier.padding(horizontal = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                ActionRow(
                                    icon = LucideIcons.Power,
                                    tone = PreferenceIconTone.Danger,
                                    title = stringResource(R.string.pro_disable),
                                    onClick = onDisable,
                                )
                            }
                        }
                    
                }
            }
        }
    }
    ProDeactivationDialog(
        show = showDisablePro,
        onDismissRequest = { showDisablePro = false },
        onDisabled = { navigator.popBackStack() },
    )
    SponsorQrCodeSheet(
        show = activated && showQrCode,
        method = SponsorMethod.entries[selectedMethod],
        onDismissRequest = { showQrCode = false },
    )
}

@Composable
private fun ProIdentity() {
    
        Card(Modifier.padding(horizontal = 12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) { ProIdentityContent() }
    
}

@Composable
private fun ProIdentityContent() {
    val salt = false
    val dark = isDarkTheme
    val goldText = if (dark) Color(0xFFF7E5B9) else Color(0xFF4D3917)
    val goldSecondary = if (dark) Color(0xFFD9C69E) else Color(0xFF705C38)
    val goldStops = if (dark) listOf(Color(0xFF493E2B), Color(0xFF302B23), Color(0xFF3D3528))
        else listOf(Color(0xFFE4C689), Color(0xFFF5E5BF), Color(0xFFEDD7A7))
    val titleStyle = MaterialTheme.typography.titleMedium
    val bodyStyle = MaterialTheme.typography.bodyLarge
    Box(
        Modifier.fillMaxWidth().background(Brush.linearGradient(goldStops)),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width * 1.06f, -size.height * 0.16f)
            repeat(3) { index ->
                drawCircle(
                    goldText.copy(alpha = if (dark) 0.08f else 0.055f),
                    radius = size.height * (0.80f + index * 0.27f),
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(
                    painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).graphicsLayer(scaleX = 1.6f, scaleY = 1.6f),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Lyrico Pro", color = goldText, style = titleStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold))
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = if (dark) 0.07f else 0.28f))
                            .border(0.5.dp, Color.White.copy(alpha = if (dark) 0.15f else 0.5f), RoundedCornerShape(50))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(LucideIcons.Check, null, tint = goldText, modifier = Modifier.size(12.dp))
                        Text(stringResource(R.string.pro_status_active), color = goldSecondary, style = bodyStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
                    }
                }
            }
            Spacer(
                Modifier.fillMaxWidth(0.64f).height(1.dp)
                    .background(Brush.horizontalGradient(listOf(
                        Color.White.copy(alpha = if (dark) 0.45f else 0.85f),
                        Color.White.copy(alpha = if (dark) 0.06f else 0.12f),
                    ))),
            )
            Text(stringResource(R.string.pro_discovered), color = goldSecondary, style = bodyStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium))
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    tone: PreferenceIconTone,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PreferenceIcon(icon, tone, Modifier.size(24.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ArrowRow(
    icon: ImageVector,
    tone: PreferenceIconTone,
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PreferenceIcon(icon, tone, Modifier.size(24.dp))
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
