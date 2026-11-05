package com.lonx.lyrico.ui.components.blur

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lonx.lyrico.data.repository.SettingsRepository
import com.lonx.lyrico.ui.components.scaffoldTopAppBarInsetsPadding
import org.koin.compose.koinInject

/**
 * 阶段6-C：顶栏/底栏模糊链路的**纯 M3 Expressive 实现**。
 *
 * 原先依赖 Miuix 的 `LayerBackdrop`/`textureBlur`/`BlurColors` 做 RenderEffect 玻璃质感，
 * 但这些特效默认全关（BAR_BLUR_ENABLED=false / FLOATING_BAR_EFFECT=NONE），
 * 且 Miuix blur API 与项目内置的 com.kyant.backdrop 不同名同包、无法 drop-in 替换。
 * 收尾时改为：保留全部对外类型与函数签名（20+ 调用点零改动），
 * 内部用 M3 语义容器色（surface / surfaceContainer）替代模糊，视觉与默认关闭路径一致。
 */

/** 模糊源标记：纯 M3 版本只作为「是否启用特殊容器色」的信号，不再承载 RenderEffect。 */
@Stable
class LayerBackdrop

// A host can share its preference subscription with all child pages.
internal val LocalBarBlurEnabled = staticCompositionLocalOf<Boolean?> { null }

private const val BarSurfaceAlpha = 0.68f

@Composable
internal fun rememberBarBlurBackdrop(
    enabled: Boolean = rememberBarBlurEnabled(),
): LayerBackdrop? = rememberBlurBackdrop(enableBlur = enabled)

/** 纯 M3 版本无需记录图层，保持签名兼容即可。 */
internal fun Modifier.blurSource(backdrop: LayerBackdrop?): Modifier = this

@Composable
internal fun BlurredTopBar(
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BlurredBar(
        backdrop = backdrop,
        modifier = modifier.scaffoldTopAppBarInsetsPadding(),
        content = content,
    )
}

@Composable
internal fun rememberBlurBackdrop(enableBlur: Boolean = true): LayerBackdrop? =
    if (enableBlur) LayerBackdrop() else null

/** 顶栏/底栏模糊开关，各页面统一从这里读。 */
@Composable
internal fun rememberBarBlurEnabled(): Boolean {
    LocalBarBlurEnabled.current?.let { return it }
    val settings: SettingsRepository = koinInject()
    val enabled by settings.barBlurEnabled.collectAsStateWithLifecycle(initialValue = false)
    return enabled
}

/** 用 M3 语义容器色替代 Miuix textureBlur：有 backdrop 时用 surfaceContainer 半透明叠加。 */
@Composable
internal fun BlurredBar(
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val container = if (backdrop != null) {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = BarSurfaceAlpha)
    } else {
        surfaceColor
    }
    Box(modifier = Modifier.background(container).then(modifier)) {
        content()
    }
}
