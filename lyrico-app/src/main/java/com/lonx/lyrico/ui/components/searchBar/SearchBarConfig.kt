package com.lonx.lyrico.ui.components.searchBar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


data class SearchBarConfig(
    val height: Dp = 48.dp,

    val searchBarCornerRadius: Dp = 35.dp,
    val searchBarBorderColor: Color = Color(0xFFE0E0E0),
    val searchBarBorderWidth: Dp = 2.dp,

    val clearIconTint: Color = Color.Black,
    val placeholderTextColor: Color = Color.Black,
    val placeholderTextString: String = "Search"
)

