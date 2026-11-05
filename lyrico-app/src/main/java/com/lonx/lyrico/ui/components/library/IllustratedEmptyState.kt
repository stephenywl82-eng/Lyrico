
package com.lonx.lyrico.ui.components.library

import com.lonx.lyrico.ui.components.icons.LucideIcons
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme

@Composable
fun IllustratedEmptyState(
    title: String,
    summary: String? = null,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: () -> Unit = {},
    illustration: (@Composable () -> Unit)? = null,
    icon: ImageVector = LucideIcons.ArrowRight,
    kind: EmptyStateKind = EmptyStateKind.List,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val scrollModifier = if (constraints.hasBoundedHeight) {
            Modifier.verticalScroll(rememberScrollState())
        } else {
            Modifier
        }
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .then(scrollModifier)
                .padding(if (compact) 16.dp else 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (illustration != null) illustration() else EmptyStateAnimation(kind, compact)
            Spacer(Modifier.height(if (compact) 12.dp else 24.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center
                )

                if (!summary.isNullOrBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

            val salt = false
            val actionColor = MaterialTheme.colorScheme.primary
            if (!actionText.isNullOrBlank()) {
                Spacer(Modifier.height(if (compact) 10.dp else 16.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = actionColor),
                            onClick = onAction
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp), // 保证触控舒适度
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    
                        Text(
                            text = actionText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = actionColor,
                            fontWeight = FontWeight.Medium,
                        )
                    
                    // Salt 原生 Icon 只接受 Painter，用矢量画笔保持同一图标与尺寸。
                    
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = actionColor,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(start = 2.dp)
                        )
                    
                }
            }
        }
    }
}
