package com.lonx.lyrico.viewmodel

enum class SortBy(val displayName: String) {
    TITLE("歌曲名"),
    ARTIST("歌手"),
    DATE_MODIFIED("修改时间"),
    DATE_ADDED("添加时间")
}

enum class SortOrder {
    ASC,
    DESC
}

data class SortInfo(
    val sortBy: SortBy = SortBy.TITLE,
    val order: SortOrder = SortOrder.ASC
)
