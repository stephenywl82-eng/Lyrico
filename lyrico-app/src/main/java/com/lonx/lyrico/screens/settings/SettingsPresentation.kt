package com.lonx.lyrico.screens.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.lonx.lyrico.ui.components.icons.PreferenceIconTone

/**
 * M3 Expressive 设置板块色调：按分组轮换 primary / secondary / tertiary 三种语义色，
 * 让设置页的分组卡、图标徽标、标题取官方的 *Container / on*Container 配色，
 * 而不是旧的彩虹硬编码色。
 */
internal enum class SectionTone { Primary, Secondary, Tertiary }

internal val LocalSectionTone = staticCompositionLocalOf { SectionTone.Primary }

/** Shared settings definitions; M3 Expressive controls live in SettingsMaterial. */
@Composable
internal fun SettingsPage(
    initialized: Boolean,
    onBack: () -> Unit,
    content: SettingsItemsScope.() -> Unit,
) {
    SettingsPageMaterial(initialized, onBack, content)
}

internal data class PaletteOption(val title: String, val color: Color)

internal class SettingsItemsScope(
    private val listScope: LazyListScope,
    presentation: SettingsItemPresentation,
) : SettingsItemPresentation by presentation {
    private var sectionToneOrdinal = 0
    fun item(key: String, content: @Composable () -> Unit) {
        val tone = SectionTone.entries[sectionToneOrdinal % SectionTone.entries.size]
        sectionToneOrdinal++
        listScope.item(key = key) {
            CompositionLocalProvider(LocalSectionTone provides tone) { content() }
        }
    }
}

internal interface SettingsItemPresentation {
    @Composable fun SectionTitle(text: String)

    @Composable
    fun Group(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit)

    @Composable
    fun Action(
        title: String,
        summary: String? = null,
        icon: ImageVector? = null,
        tone: PreferenceIconTone = PreferenceIconTone.Tools,
        value: String? = null,
        holdDownState: Boolean = false,
        onClick: () -> Unit,
    )

    @Composable
    fun Toggle(
        title: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        summary: String? = null,
        enabled: Boolean = true,
        icon: ImageVector? = null,
        tone: PreferenceIconTone = PreferenceIconTone.Tools,
    )

    @Composable
    fun Choice(
        title: String,
        items: List<String>,
        selectedIndex: Int,
        onSelectedIndexChange: (Int) -> Unit,
        summary: String? = null,
        icon: ImageVector? = null,
        tone: PreferenceIconTone = PreferenceIconTone.Tools,
    )

    @Composable
    fun Spinner(
        title: String,
        items: List<PaletteOption>,
        selectedIndex: Int,
        onSelectedIndexChange: (Int) -> Unit,
        icon: ImageVector? = null,
        tone: PreferenceIconTone = PreferenceIconTone.Tools,
    )

    @Composable
    fun Range(
        title: String,
        value: Float,
        onValueChange: (Float) -> Unit,
        onValueChangeFinished: () -> Unit,
        valueText: String,
        summary: String? = null,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int,
        icon: ImageVector? = null,
        tone: PreferenceIconTone = PreferenceIconTone.Tools,
    )
}
