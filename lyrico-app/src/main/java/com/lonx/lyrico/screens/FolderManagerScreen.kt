package com.lonx.lyrico.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.rememberTintedPainter
import com.lonx.lyrico.utils.UriUtils
import com.lonx.lyrico.viewmodel.SettingsViewModel
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.ItemOuterTitle
import com.moriafly.salt.ui.ItemSwitcher
import com.moriafly.salt.ui.ItemTip
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.icons.ArrowBack
import com.moriafly.salt.ui.icons.SaltIcons
import com.moriafly.salt.ui.rememberScrollState
import com.moriafly.salt.ui.verticalScroll
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, UnstableSaltUiApi::class)
@Composable
@Destination<RootGraph>(route = "folder_manager")
fun FolderManagerScreen(
    navigator: DestinationsNavigator
) {
    val viewModel: SettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val folders = uiState.folders
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val path = UriUtils.getFileAbsolutePath(context, it)

            if (path != null) {
                viewModel.addAndIgnoreFolder(path)
            } else {
                viewModel.addAndIgnoreFolder(it.toString())
            }

            val contentResolver = context.contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            contentResolver.takePersistableUriPermission(it, takeFlags)
        }
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background),
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarColors(
                    containerColor = SaltTheme.colors.background,
                    scrolledContainerColor = SaltTheme.colors.background,
                    navigationIconContentColor = SaltTheme.colors.text,
                    titleContentColor = SaltTheme.colors.text,
                    actionIconContentColor = SaltTheme.colors.text,
                    subtitleContentColor = SaltTheme.colors.subText
                ),
                title = { Text("文件夹管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(imageVector = SaltIcons.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        folderPickerLauncher.launch(null)
                    }) {
                        Icon(
                            painter = rememberTintedPainter(
                                painterResource(id = R.drawable.ic_addfolder_24dp), tint = SaltTheme.colors.text),
                            contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SaltTheme.colors.background)
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            ItemTip(text = "未启用的文件夹及其子目录中的歌曲将不会显示在库中，更改后请手动刷新歌曲列表")

            ItemOuterTitle("已发现的文件夹")
            RoundedColumn {
                if (folders.isEmpty()) {
                    ItemTip(text = "暂无文件夹数据，完成首次扫描后即可管理")
                } else {
                    folders.forEachIndexed { index, folder ->
                        ItemSwitcher(
                            state = !folder.isIgnored,
                            onChange = { viewModel.toggleFolderIgnore(folder) },
                            text = folder.path.substringAfterLast("/").ifBlank { "/" },
                            sub = folder.path + "\n包含歌曲: ${folder.songCount}"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}