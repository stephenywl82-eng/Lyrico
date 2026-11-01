package com.lonx.lyrico.data.model

import androidx.annotation.StringRes
import com.lonx.lyrico.R

enum class FloatingBarEffect(
    @field:StringRes val labelRes: Int
) {
    NONE(R.string.floating_bar_effect_none),
    FROSTED_GLASS(R.string.floating_bar_effect_frosted_glass),
    LIQUID_GLASS(R.string.floating_bar_effect_liquid_glass);

    companion object {
        /** 宽松解析持久化的枚举名，遇到未知值回退到 [NONE]。 */
        fun fromName(name: String?): FloatingBarEffect =
            entries.firstOrNull { it.name == name } ?: NONE
    }
}
