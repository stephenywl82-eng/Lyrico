package com.lonx.lyrico.screens

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lonx.lyrico.viewmodel.BatchRenameViewModel
import com.lonx.lyrico.viewmodel.SongForBatchRename
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.lonx.lyrico.data.model.RenamePreview
import com.lonx.lyrico.ui.components.ItemExt
import com.lonx.lyrico.ui.theme.LyricoColors.modifiedBorder
import com.lonx.lyrico.utils.RenameEngine
import com.lonx.lyrico.utils.TagField
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.ItemButton
import com.moriafly.salt.ui.ItemCheck
import com.moriafly.salt.ui.ItemDivider
import com.moriafly.salt.ui.ItemDropdown
import com.moriafly.salt.ui.ItemInfo
import com.moriafly.salt.ui.ItemInfoType
import com.moriafly.salt.ui.ItemOuterTitle
import com.moriafly.salt.ui.ItemSwitcher
import com.moriafly.salt.ui.ItemTip
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.dialog.YesNoDialog
import com.moriafly.salt.ui.icons.Check
import com.moriafly.salt.ui.icons.SaltIcons
import com.moriafly.salt.ui.rememberScrollState
import com.moriafly.salt.ui.verticalScroll

@OptIn(UnstableSaltUiApi::class)
@SuppressLint("LocalContextGetResourceValueCall")
@Composable
@Destination<RootGraph>(route = "batch_rename")
fun BatchRenameScreen(
    navigator: DestinationsNavigator,
    filePaths: Array<String> = arrayOf()
) {
    val viewModel: BatchRenameViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 解析传入的文件路径
    LaunchedEffect(filePaths) {
        if (filePaths.isNotEmpty()) {
            val songList = filePaths.map { path ->
                SongForBatchRename(
                    filePath = path,
                    fileName = path.substringAfterLast('/'),
                    tagData = null
                )
            }
            viewModel.setSongs(context, songList)
        }
    }

    var showPlaceholderInfo by remember { mutableStateOf(false) }

    BasicScreenBox(
        title = "批量重命名",
        onBack = {
            if (!uiState.isRenamingInProgress) {
                navigator.popBackStack()
            }
        },
        toolbar = {
//            IconButton(
//                onClick = {
//                    viewModel.executeRename()
//                },
//                modifier = Modifier
//                    .size(56.dp)
//            ) {
//                Icon(
//                    imageVector = SaltIcons.Check,
//                    contentDescription = null,
//                    modifier = Modifier
//                        .size(SaltTheme.dimens.itemIcon)
//                )
//            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 1. 命名格式设置区
            ItemOuterTitle(text = "命名格式")
            RoundedColumn {
                // 输入框 Item
                ItemEdit(
                    value = uiState.format,
                    onValueChange = { viewModel.setFormat(context, it) },
                    placeholder = "{@1} - {@4}",
                    hintText = "输入命名规则，例如 {@1} - {@4}"
                )

                ItemDropdown(
                    text = "重命名预设",
                    value = uiState.format,
                    content = {
                        uiState.presetFormats.forEach { format ->
                            ItemCheck(
                                text = format,
                                state = uiState.format == format,
                                onChange = {
                                    viewModel.setFormat(context, format)
                                    state.dismiss()
                                }
                            )
                        }
                    }
                )

                // 占位符说明开关
                ItemSwitcher(
                    text = "查看占位符说明",
                    state = showPlaceholderInfo,
                    onChange = { showPlaceholderInfo = it }
                )
                AnimatedVisibility(visible = showPlaceholderInfo) {
                    PlaceholderInfoContent()
                }
                ItemButton(
                    onClick = {
                        viewModel.executeRename()
                    },
                    enabled = uiState.previews.isNotEmpty() && !uiState.isRenamingInProgress,
                    text = "执行重命名",
                )
            }

            ItemOuterTitle(
                text = "预览 (${uiState.previews.size})" + if (uiState.isGeneratingPreview) " (生成中...)" else ""
            )

            RoundedColumn {
                if (uiState.previews.isEmpty()) {
                    ItemTip(text = "没有可预览的文件")
                } else {
                    uiState.previews.forEachIndexed { index, preview ->
                        PreviewItem(preview = preview)
                        if (index < uiState.previews.size - 1) {
                            ItemDivider()
                        }
                    }
                }
                if (uiState.errorMessage != null) {
                    ItemInfo(
                        text = uiState.errorMessage!!,
                        infoType = ItemInfoType.Error
                    )

                }
            }


            Spacer(modifier = Modifier.height(SaltTheme.dimens.padding))

        }

        // 结果弹窗
        if (uiState.renameResult != null) {
            RenameResultDialog(
                result = uiState.renameResult!!,
                onDismiss = {
                    viewModel.clearResult()
                    navigator.popBackStack()
                }
            )
        }
    }
}

@Composable
private fun PreviewItem(preview: RenamePreview) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "旧: ${preview.originalPath.substringAfterLast('/')}",
            fontSize = 13.sp,
            color = SaltTheme.colors.subText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "新: ${preview.newPath.substringAfterLast('/')}",
            fontSize = 14.sp,
            color = if (preview.conflict) modifiedBorder else SaltTheme.colors.highlight,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (preview.conflict) {
            Text(
                text = "⚠️ 名称冲突，将自动加数字后缀",
                fontSize = 11.sp,
                color = modifiedBorder,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun PlaceholderInfoContent() {
    val placeholders = TagField.entries.map {
        "@${it.index}" to it.description
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        placeholders.forEach { (placeholder, description) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = placeholder,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = SaltTheme.colors.highlight
                )
                Text(
                    text = stringResource(description),
                    fontSize = 13.sp,
                    color = SaltTheme.colors.subText
                )
            }
        }
    }
}

@Composable
private fun RenameResultDialog(
    result: RenameEngine.Result,
    onDismiss: () -> Unit
) {
    YesNoDialog(
        onDismissRequest = onDismiss,
        onConfirm = onDismiss,
        title = if (result.isSuccessful) "重命名成功" else "重命名结束",
        confirmText = "确定",
        content = buildString {
            append("成功: ${result.successCount} / 总数: ${result.totalCount}\n")
            if (result.failureCount > 0) {
                append("失败: ${result.failureCount}\n")
                result.failed.take(3).forEach { (_, error) ->
                    append("• $error\n")
                }
                if (result.failed.size > 3) {
                    append("...")
                }
            }
        }
    )
}

@Composable
private fun ItemEdit(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    hintText: String,
    readOnly: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(SaltTheme.dimens.padding)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(text = placeholder, color = SaltTheme.colors.subText, fontSize = 14.sp) },
            readOnly = readOnly,
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = SaltTheme.colors.subText.copy(alpha = 0.2f),
                focusedBorderColor = SaltTheme.colors.highlight,
                cursorColor = SaltTheme.colors.highlight
            ),
            textStyle = TextStyle(fontSize = 14.sp, color = SaltTheme.colors.text)
        )
        Text(
            text = hintText,
            fontSize = 11.sp,
            color = SaltTheme.colors.subText,
            modifier = Modifier.padding(top = 4.dp, start = 2.dp)
        )
    }
}
