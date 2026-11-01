package com.lonx.lyrico.ui.components.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lonx.lyrico.R

/** 评分星：统一到 SDW / Material3，跟随当前主题取色与点击反馈。 */
@Composable
internal fun RatingStars(rating: Int?, onRatingChange: (Int) -> Unit) {
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant
    val buttonWidth = 36.dp
    val buttonHeight = 40.dp
    // 抵消起始侧触控框的空白，使图标与标签对齐。
    Row(Modifier.offset(x = -(buttonWidth - 24.dp) / 2).selectableGroup()) {
        for (value in 1..5) {
            val filled = value <= (rating ?: 0)
            val description = stringResource(R.string.rating_stars_value, value)
            Box(
                modifier = Modifier.size(width = buttonWidth, height = buttonHeight).clip(CircleShape).selectable(
                    selected = rating == value,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.RadioButton,
                    onClick = { onRatingChange(if (rating == value) 0 else value) },
                ),
                contentAlignment = Alignment.Center,
            ) {
                val painter = painterResource(if (filled) R.drawable.ic_filled_star_24dp else R.drawable.ic_outline_star_24dp)
                val tint: Color = if (filled) active else inactive.copy(alpha = 0.45f)
                Icon(painter, description, Modifier.size(24.dp), tint)
            }
        }
    }
}
