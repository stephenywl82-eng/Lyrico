package com.lonx.lyrico.data.utils

import com.github.promeg.pinyinhelper.Pinyin

object SortKeyUtils {

    data class SortKeys(
        val groupKey: String,  // A-Z 或 0（数字）或 #（其他）
        val sortKey: String    // 用于真正排序的字符串（完整拼音/英文）
    )

    fun getSortKeys(text: String): SortKeys {
        val raw = text.trim()
        if (raw.isBlank()) return SortKeys("#", "#")

        val firstChar = raw[0]

        // 1) 数字：分组键用 "0"
        if (firstChar.isDigit()) {
            return SortKeys("0", "0$raw")  // 排序键前加 "0" 保证数字在一起
        }

        // 2) 英文：排序用完整英文
        if (firstChar.isLetter() && firstChar.code < 128) {
            val sortKey = raw.uppercase()
            val groupKey = sortKey[0].toString()
            return SortKeys(groupKey, sortKey)
        }

        // 3) 中文：排序用完整拼音
        val pinyinFull = try {
            Pinyin.toPinyin(raw, "")  // 示例：输入"北京" -> "BEIJING"
        } catch (e: Exception) {
            ""
        }

        if (pinyinFull.isNotBlank() && pinyinFull[0].isLetter()) {
            val sortKey = pinyinFull.uppercase()
            val groupKey = sortKey[0].toString()
            return SortKeys(groupKey, sortKey)
        }

        // 4) 其他字符（非字母、非数字）：分组键用 "#"
        return SortKeys("#", "#$raw")
    }
}
