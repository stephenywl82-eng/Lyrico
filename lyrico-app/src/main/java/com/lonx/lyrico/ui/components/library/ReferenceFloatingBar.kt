package com.lonx.lyrico.ui.components.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lonx.lyrico.data.model.FloatingBarEffect
import com.lonx.lyrico.ui.components.blur.LayerBackdrop

/**
 * 阶段6-C：手机端悬浮底栏的**纯 M3 Expressive 实现**。
 *
 * 原实现是 615 行液态玻璃/磨砂玻璃（Miuix textureBlur + drawBackdrop + Highlight +
 * BloomStroke + rememberDeviceTilt + 自研 AGSL 折射 shader）。这些特效默认全关
 * （FLOATING_BAR_EFFECT=NONE / BAR_BLUR_ENABLED=false），且 Miuix blur API 无法 drop-in
 * 替换到项目内置的 com.kyant.backdrop。收尾时保留两个对外函数签名与选择回调机制，
 * 渲染改为 M3 语义容器色胶囊（surfaceContainerHigh + 选中项 primaryContainer 指示），
 * 与设置页/全站 M3 Expressive 风格一致，默认路径零视觉回归。
 */

private val LocalReferenceBarSelection = compositionLocalOf<(Int) -> Unit> { {} }

/** 悬浮胶囊底栏。effect / backdrop 保留在签名里兼容调用方，纯 M3 版本统一渲染同一套容器色。 */
@Composable
internal fun BlurNavigationBarV2(
    modifier: Modifier = Modifier,
    backdrop: LayerBackdrop?,
    selectedIndex: Int,
    itemCount: Int,
    effect: FloatingBarEffect,
    onSelectionChanged: (Int) -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    if (itemCount <= 0) return
    CompositionLocalProvider(LocalReferenceBarSelection provides onSelectionChanged) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                content()
            }
        }
    }
}

@Composable
internal fun RowScope.BlurNavigationBarItemV2(
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String,
) {
    val selection = LocalReferenceBarSelection.current
    val haptic = LocalHapticFeedback.current
    val tint = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = Modifier
            .defaultMinSize(minWidth = 76.dp)
            .fillMaxHeight()
            .weight(1f)
            .padding(horizontal = 2.dp)
            .clip(CircleShape)
            .then(
                if (selected) Modifier.background(
                    MaterialTheme.colorScheme.primaryContainer,
                    CircleShape,
                ) else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    selection(index)
                    onClick()
                },
            )
            .semantics { role = Role.Tab },
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides tint,
        ) {
            Box(modifier = Modifier.size(22.dp)) { icon() }
            Text(
                text = label,
                color = tint,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
            )
        }
    }
}
