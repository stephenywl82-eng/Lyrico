package com.lonx.lyrico.ui.components.base

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lonx.lyrico.ui.theme.sdw.SDWShapes

enum class PillButtonSize {
    Small,
    Medium,
    Large,
}

@Immutable
data class PillButtonStyle(
    val height: Dp,
    val contentPadding: PaddingValues,
    val iconOnlyPadding: PaddingValues,
    val spacing: Dp,
    val shape: Shape,
    val textStyle: TextStyle,
    val selectedTextStyle: TextStyle = textStyle,
)

@Immutable
data class PillButtonColors(
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
    val containerColor: Color,
    val contentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
) {
    fun containerColor(
        selected: Boolean,
        enabled: Boolean,
    ): Color {
        return when {
            !enabled -> disabledContainerColor
            selected -> selectedContainerColor
            else -> containerColor
        }
    }

    fun contentColor(
        selected: Boolean,
        enabled: Boolean,
    ): Color {
        return when {
            !enabled -> disabledContentColor
            selected -> selectedContentColor
            else -> contentColor
        }
    }
}

object PillButtonDefaults {

    @Composable
    fun style(size: PillButtonSize): PillButtonStyle {
        val base = MaterialTheme.typography.bodyMedium
        return when (size) {
            PillButtonSize.Small -> PillButtonStyle(
                height = 28.dp,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                iconOnlyPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                spacing = 4.dp,
                shape = SDWShapes.Pill,
                textStyle = base.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
                selectedTextStyle = base.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            )

            PillButtonSize.Medium -> PillButtonStyle(
                height = 32.dp,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                iconOnlyPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                spacing = 5.dp,
                shape = SDWShapes.Pill,
                textStyle = base.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                selectedTextStyle = base.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
            )

            PillButtonSize.Large -> PillButtonStyle(
                height = 38.dp,
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 7.dp),
                iconOnlyPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                spacing = 6.dp,
                shape = SDWShapes.Pill,
                textStyle = base.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                selectedTextStyle = base.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
        }
    }

    @Composable
    fun defaultContainerColor(): Color = MaterialTheme.colorScheme.surface

    @Composable
    fun colors(
        selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
        selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
        containerColor: Color = defaultContainerColor(),
        contentColor: Color = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor: Color = defaultContainerColor(),
        disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    ): PillButtonColors {
        return PillButtonColors(
            selectedContainerColor = selectedContainerColor,
            selectedContentColor = selectedContentColor,
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        )
    }

    /**
     * 无底色、强调色文字的胶囊，用于面包屑、加载更多这类行内操作。
     */
    @Composable
    fun plainColors(): PillButtonColors {
        val accent = MaterialTheme.colorScheme.primary
        val disabledContent = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        return colors(
            selectedContainerColor = Color.Transparent,
            selectedContentColor = accent,
            containerColor = Color.Transparent,
            contentColor = accent,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = disabledContent,
        )
    }
}

/**
 * [B] 统一到 SDW：胶囊按钮合并 Salt / MIUIX 两套实现为单一 Material3 实现，
 * 取色与形状全部走 MaterialTheme / SDW 契约（shape 使用 SDW 全圆角 Pill）。
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    showText: Boolean = true,
    style: PillButtonStyle = PillButtonDefaults.style(PillButtonSize.Medium),
    colors: PillButtonColors = PillButtonDefaults.colors(),
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val containerColor = colors.containerColor(selected, enabled)
    val contentColor = colors.contentColor(selected, enabled)

    val hasText = showText && text.isNotBlank()
    val hasLeading = leading != null
    val hasTrailing = trailing != null
    val iconOnly = !hasText && (hasLeading || hasTrailing)

    Box(
        modifier = modifier
            .height(style.height)
            .clip(style.shape)
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(if (iconOnly) style.iconOnlyPadding else style.contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leading != null) {
                leading()
            }

            if (leading != null && hasText) {
                Spacer(modifier = Modifier.width(style.spacing))
            }

            if (hasText) {
                val textStyle = if (selected) style.selectedTextStyle else style.textStyle
                androidx.compose.material3.Text(
                    text = text,
                    style = textStyle,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (trailing != null && hasText) {
                Spacer(modifier = Modifier.width(style.spacing))
            }

            if (trailing != null) {
                trailing()
            }
        }
    }
}
