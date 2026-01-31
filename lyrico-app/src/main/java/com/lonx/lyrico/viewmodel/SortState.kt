package com.lonx.lyrico.viewmodel

enum class SortBy(val displayName: String, val supportsIndex: Boolean) {
    TITLE("歌曲名", true),
    ARTIST("歌手", true),
    DATE_MODIFIED("修改时间", false),
    DATE_ADDED("添加时间", false)
}

enum class SortOrder {
    ASC,
    DESC
}

data class SortInfo(
    val sortBy: SortBy = SortBy.TITLE,
    val order: SortOrder = SortOrder.ASC
)
