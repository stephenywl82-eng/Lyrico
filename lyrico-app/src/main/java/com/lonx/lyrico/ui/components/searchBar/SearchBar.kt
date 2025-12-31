package com.lonx.lyrico.ui.components.searchBar


import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    config: SearchBarConfig = SearchBarConfig(),
    onSearch: () -> Unit = {}
) {

    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .width(320.dp)
            .height(config.height)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .border(
                    width = config.searchBarBorderWidth,
                    color = config.searchBarBorderColor,
                    shape = RoundedCornerShape(config.searchBarCornerRadius)
                )
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch()
                        focusManager.clearFocus()
                    }),
                textStyle = TextStyle(
                    color = Color.Black, fontSize = 16.sp, lineHeight = 18.sp
                ),
                cursorBrush = SolidColor(Color.DarkGray),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Box(
                            modifier = Modifier, contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                config.placeholderTextString,
                                color = config.placeholderTextColor,
                                fontSize = 16.sp
                            )
                        }
                    }
                    innerTextField()
                })

            IconButton(
                onClick = {
                    onValueChange("")

                }, modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear Search",
                    tint = config.clearIconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
