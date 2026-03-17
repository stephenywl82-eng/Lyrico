package com.lonx.lyrico.data.model

import kotlinx.serialization.Serializable

/**
 * 字符映射规则
 *
 * @param id 规则ID（唯一标识）
 * @param name 规则名称
 * @param charMappings 字符映射表：key = 原字符，value = 替换字符（null表示移除）
 * @param description 规则描述
 * @param isBuiltIn 是否为内置规则
 * @param isEnabled 是否启用该规则
 */
@Serializable
data class CharacterMappingRule(
    val id: String,
    val name: String,
    val charMappings: Map<String, String?> = emptyMap(),
    val description: String = "",
    val isBuiltIn: Boolean = false,
    val isEnabled: Boolean = true
)

/**
 * 字符映射配置
 *
 * @param rules 所有映射规则（内置规则）
 */
@Serializable
data class CharacterMappingConfig(
    val rules: List<CharacterMappingRule> = emptyList()
)

/**
 * 预定义的替换字符选项
 */
object ReplacementCharOptions {
    const val REMOVE = "" // 空字符表示移除
    const val IDEOGRAPHIC_COMMA = "、" // 顿号
    const val HALF_WIDTH_COMMA = "," // 半角逗号
    const val FULL_WIDTH_COMMA = "，" // 全角逗号
    const val AMPERSAND = "&" // & 符号
    
    val ALL_OPTIONS = listOf(
        REMOVE,
        IDEOGRAPHIC_COMMA,
        HALF_WIDTH_COMMA,
        FULL_WIDTH_COMMA,
        AMPERSAND
    )
    
    fun getDisplayName(char: String?): String = when(char) {
        REMOVE -> "移除"
        IDEOGRAPHIC_COMMA -> "顿号 (、)"
        HALF_WIDTH_COMMA -> "半角逗号 (,)"
        FULL_WIDTH_COMMA -> "全角逗号 (，)"
        AMPERSAND -> "与符号 (&)"
        else -> "未选择"
    }
}

object CharacterMappingDefaults {
    // 系统默认的非法字符规则 - 使用新的 charMappings 方式
    val DEFAULT_INVALID_CHARS = CharacterMappingRule(
        id = "default_invalid_chars",
        name = "文件系统非法字符",
        charMappings = mapOf(
            "\\" to "、",
            "/" to "、",
            ":" to "、",
            "*" to "、",
            "?" to "、",
            "\"" to "、",
            "<" to "、",
            ">" to "、",
            "|" to "、"
        ),
        description = "Windows/Linux文件系统中的非法字符",
        isBuiltIn = true,
        isEnabled = true
    )

    val ALL_BUILTIN_RULES = listOf(
        DEFAULT_INVALID_CHARS
    )
}
