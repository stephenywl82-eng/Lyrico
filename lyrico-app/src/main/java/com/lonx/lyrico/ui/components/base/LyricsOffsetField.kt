package com.lonx.lyrico.ui.components.base

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lonx.lyrico.R
import com.lonx.lyrico.ui.components.sdw.SdwConfirmDialog
import com.lonx.lyrico.ui.components.sdw.SdwTextInput
import com.lonx.lyrico.ui.theme.sdw.SDWShapes

const val LYRICS_OFFSET_STEP_MS: Long = 100L

const val LYRICS_OFFSET_MIN_MS: Long = -10_000L

const val LYRICS_OFFSET_MAX_MS: Long = 10_000L

private val STEPPER_HEIGHT = 40.dp

private val STEPPER_BUTTON_WIDTH = 74.dp

private val STEPPER_VALUE_WIDTH = 86.dp

/**
 * 歌词偏移调整控件。[B] 统一到 SDW：原 Salt / MIUIX 两套步进器合并为单一 Material3 实现，
 * 取色与形状全部走 MaterialTheme / SDW 契约。
 */
@Composable
fun LyricsOffsetField(
    offset: Long,
    onOffsetChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    minOffset: Long = LYRICS_OFFSET_MIN_MS,
    maxOffset: Long = LYRICS_OFFSET_MAX_MS,
    step: Long = LYRICS_OFFSET_STEP_MS,
    enabled: Boolean = true,
    animated: Boolean = true,
) {
    var showInputDialog by remember { mutableStateOf(false) }
    val safeOffset = offset.coerceIn(minOffset, maxOffset)
    val haptics = LocalHapticFeedback.current
    val canDecrease = enabled && safeOffset > minOffset
    val canIncrease = enabled && safeOffset < maxOffset

    fun stepBy(delta: Long) {
        val next = (safeOffset + delta).coerceIn(minOffset, maxOffset)
        if (next == safeOffset) return
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onOffsetChange(next)
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // 分段步进器：一个容器里放两侧按钮与中间数值，避免三个控件各自为政。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(STEPPER_HEIGHT)
                .clip(SDWShapes.Pill)
                .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer)
                .semantics { contentDescription = formatLyricsOffset(safeOffset) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperSideButton(
                label = "-${step}ms",
                enabled = canDecrease,
                onClick = { stepBy(-step) },
            )

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(enabled = enabled) { showInputDialog = true },
                contentAlignment = Alignment.Center,
            ) {
                // 数值是控件的信息锚点：比正文重一档、但不压过页面标题，胶囊底暗示可点击手动输入。
                Box(
                    modifier = Modifier
                        .width(STEPPER_VALUE_WIDTH)
                        .clip(SDWShapes.Pill)
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.surface)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedContent(
                        targetState = safeOffset,
                        transitionSpec = {
                            if (!animated) {
                                fadeIn() togetherWith fadeOut()
                            } else {
                                val forward = targetState > initialState
                                val slide = spring<IntOffset>(dampingRatio = 0.85f, stiffness = 900f)
                                val enter = slideInVertically(animationSpec = slide) { height ->
                                    if (forward) height else -height
                                } + fadeIn(animationSpec = tween(durationMillis = 90))
                                val exit = slideOutVertically(animationSpec = slide) { height ->
                                    if (forward) -height else height
                                } + fadeOut(animationSpec = tween(durationMillis = 90))
                                enter togetherWith exit
                            }
                        },
                        label = "lyricsOffsetValue",
                    ) { value ->
                        Text(
                            text = formatLyricsOffset(value),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 8.dp),
                color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
            )

            StepperSideButton(
                label = "+${step}ms",
                enabled = canIncrease,
                onClick = { stepBy(step) },
            )
        }

        // 次要动作弱化为文字按钮，不再和步进器抢视觉。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.action_reset),
                modifier = Modifier
                    .weight(1f)
                    .clip(SDWShapes.Row)
                    .clickable(enabled = enabled && safeOffset != 0L) { onOffsetChange(0L) }
                    .padding(vertical = 8.dp),
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                color = if (enabled && safeOffset != 0L) {
                    androidx.compose.material3.MaterialTheme.colorScheme.primary
                } else {
                    androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                },
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.action_manual_input),
                modifier = Modifier
                    .weight(1f)
                    .clip(SDWShapes.Row)
                    .clickable(enabled = enabled) { showInputDialog = true }
                    .padding(vertical = 8.dp),
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                color = if (enabled) {
                    androidx.compose.material3.MaterialTheme.colorScheme.primary
                } else {
                    androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                },
                maxLines = 1,
            )
        }
    }

    LyricsOffsetInputDialog(
        show = showInputDialog,
        initialValue = safeOffset,
        minOffset = minOffset,
        maxOffset = maxOffset,
        onDismiss = { showInputDialog = false },
        onConfirm = { value ->
            showInputDialog = false
            onOffsetChange(value)
        },
    )
}

@Composable
private fun StepperSideButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(STEPPER_BUTTON_WIDTH)
            .fillMaxHeight()
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = if (enabled) {
                androidx.compose.material3.MaterialTheme.colorScheme.primary
            } else {
                androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            },
            maxLines = 1,
        )
    }
}

/**
 * 手动输入歌词偏移值的对话框。数字键盘多数不提供负号，因此输入框只接受绝对值，
 * 正负号由左侧的符号按钮切换。
 */
@Composable
fun LyricsOffsetInputDialog(
    show: Boolean,
    initialValue: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    modifier: Modifier = Modifier,
    minOffset: Long = LYRICS_OFFSET_MIN_MS,
    maxOffset: Long = LYRICS_OFFSET_MAX_MS,
) {
    var negative by remember(show, initialValue) { mutableStateOf(initialValue < 0L) }
    val magnitudeState = remember(show, initialValue) {
        TextFieldState(kotlin.math.abs(initialValue).toString())
    }
    val magnitudeInput = magnitudeState.text.toString()
    val magnitude = magnitudeInput.trim().toLongOrNull()
    val value = magnitude?.let { if (negative) -it else it }
    val isValid = value != null && value in minOffset..maxOffset
    val rangeHint = stringResource(R.string.lyrics_offset_input_hint, minOffset, maxOffset)

    SdwConfirmDialog(
        show = show,
        title = stringResource(R.string.label_lyrics_offset),
        onDismissRequest = onDismiss,
        onConfirm = { value?.let(onConfirm) },
        cancelText = stringResource(R.string.cancel),
        confirmText = stringResource(R.string.confirm),
        confirmEnabled = isValid,
        dismissOnConfirm = false,
    ) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(SDWShapes.Media)
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { negative = !negative },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (negative) "−" else "+",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            SdwTextInput(
                state = magnitudeState,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = when {
                magnitude == null -> stringResource(R.string.lyrics_offset_input_invalid)
                !isValid -> stringResource(R.string.lyrics_offset_input_out_of_range, rangeHint)
                else -> rangeHint
            },
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = if (isValid) {
                androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                androidx.compose.material3.MaterialTheme.colorScheme.error
            },
        )
    }
}

/** 把偏移值格式化为带符号的展示文本，例如 `+100ms`、`-100ms`、`0ms`。 */
fun formatLyricsOffset(offset: Long): String {
    return when {
        offset > 0L -> "+${offset}ms"
        else -> "${offset}ms"
    }
}
