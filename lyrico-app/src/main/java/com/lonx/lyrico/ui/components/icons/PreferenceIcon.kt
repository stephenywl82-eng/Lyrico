package com.lonx.lyrico.ui.components.icons

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

internal enum class PreferenceIconTone {
    Appearance, Library, Lyrics, Tools, Teal, Orange, Pink, Gold, Danger;

    fun forItem(index: Int): PreferenceIconTone = if (this == Danger) Danger else at(index)

    companion object {
        private val itemTones = listOf(Appearance, Tools, Library, Lyrics, Pink, Teal, Orange)

        fun at(index: Int): PreferenceIconTone = itemTones[index.mod(itemTones.size)]
    }

    @Composable
    fun color(): Color {
        val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        return when (this) {
            Appearance -> if (dark) Color(0xFFBF9AFF) else Color(0xFF8950E6)
            Library -> if (dark) Color(0xFFF3B85E) else Color(0xFFC47B08)
            Lyrics -> if (dark) Color(0xFF65D8A0) else Color(0xFF189965)
            Tools -> if (dark) Color(0xFF7DB5FF) else Color(0xFF2979E8)
            Teal -> if (dark) Color(0xFF5AD5CB) else Color(0xFF08998F)
            Orange -> if (dark) Color(0xFFFFAC76) else Color(0xFFDB741F)
            Pink -> if (dark) Color(0xFFF393C5) else Color(0xFFD94C91)
            Gold -> if (dark) Color(0xFFE9C157) else Color(0xFFAF850A)
            Danger -> MaterialTheme.colorScheme.error
        }
    }
}

/** Decorative leading icon; the preference title supplies its accessible name. */
@Composable
internal fun PreferenceIcon(
    imageVector: ImageVector,
    tone: PreferenceIconTone = PreferenceIconTone.Tools,
    modifier: Modifier = Modifier.padding(end = 6.dp),
    enabled: Boolean = true,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = modifier.size(24.dp),
        tint = tone.color().let { if (enabled) it else it.copy(alpha = 0.38f) },
    )
}
