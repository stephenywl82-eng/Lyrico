package com.lonx.lyrico.ui.components.lyrics

import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/**
 * 歌词正文的 Salt 阅读样式。Salt 主文 16sp 在长歌词下单屏行数偏少，统一小一档；
 * 字号集中在此维护，各页歌词预览不再各写各的。
 */
@Composable
internal fun saltLyricsReadingStyle(): TextStyle =
    MiuixTheme.textStyles.main.copy(fontSize = 14.sp)
