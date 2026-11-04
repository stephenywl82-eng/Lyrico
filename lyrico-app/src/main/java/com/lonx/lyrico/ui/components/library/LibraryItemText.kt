package com.lonx.lyrico.ui.components.library

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

/** Shared text semantics, rendered with the SDW (Material3) typography and palette.
 *  Defaults to [MaterialTheme.colorScheme.onSurface] so library items stay legible in both
 *  light and dark themes (the list rows use a plain `.background()` instead of a Material3
 *  `Surface`, so `LocalContentColor` is not derived from the surface color). */
@Composable
internal fun LibraryItemText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) = LibraryItemText(AnnotatedString(text), modifier, color, fontSize, fontWeight, textAlign, maxLines, overflow, style)

@Composable
internal fun LibraryItemText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    Text(
        text, modifier, color = color, fontSize = fontSize, fontWeight = fontWeight,
        textAlign = textAlign, maxLines = maxLines, overflow = overflow, style = style,
    )
}
