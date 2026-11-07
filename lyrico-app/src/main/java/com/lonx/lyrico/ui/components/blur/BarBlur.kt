package com.lonx.lyrico.ui.components.blur

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.lonx.lyrico.data.repository.SettingsRepository
import com.lonx.lyrico.ui.components.scaffoldTopAppBarInsetsPadding
import org.koin.compose.koinInject

/**
 * 重新接回真正的玻璃效果（com.kyant.backdrop）。
 *
 * 阶段6-C 收尾时把模糊链路改成了纯 M3 语义容器色，导致 `LayerBackdrop` 退化为空桩、
 * `blurSource` 是 no-op，于是设置里「毛玻璃 / 液态玻璃」切换毫无视觉变化。
 * 现把 `LayerBackdrop` 直接 typealias 到 kyant 的真实实现（对外签名不变，20+ 调用点零改动），
 * 并让 `blurSource` 真正把内容层录制进 backdrop，供悬浮底栏的 `drawBackdrop` 采样。
 */

// 对外类型保持不变：直接指向 com.kyant.backdrop 的真实 LayerBackdrop（内部构造为 internal，
// 调用方统一走 rememberLayerBackdrop()，本文件的 LayerBackdrop 仅为兼容别名）。
typealias LayerBackdrop = com.kyant.backdrop.backdrops.LayerBackdrop

// A host can share its preference subscription with all child pages.
internal val LocalBarBlurEnabled = staticCompositionLocalOf<Boolean?> { null }

private const val BarSurfaceAlpha = 0.82f

@Composable
internal fun rememberBarBlurBackdrop(
    enabled: Boolean = rememberBarBlurEnabled(),
): LayerBackdrop? = rememberBlurBackdrop(enableBlur = enabled)

/** 把内容层录制进 backdrop（真正的玻璃采样源）。非 null 时走 com.kyant.backdrop 的
 *  drawPlainBackdrop 把当前层画面导出到 backdrop.graphicsLayer，供悬浮底栏的 drawBackdrop 采样。 */
/**
 * 把内容层录制进 backdrop（真正的玻璃采样源）。
 *
 * 关键：被采样的源（backdrop 参数）与导出目标（exportedBackdrop）**绝不能指向同一个
 * LayerBackdrop**——否则录制层在写入目标层时会再次绘制自身，形成 RenderNode 自引用环，
 * 触发 RenderThread 无限递归（prepareTreeImpl 栈溢出）导致启动即闪退。因此源固定为
 * emptyBackdrop()，仅把本层成品导出到目标层供悬浮底栏消费。
 */
internal fun Modifier.blurSource(backdrop: LayerBackdrop?): Modifier =
    if (backdrop != null)
        this.drawPlainBackdrop(
            backdrop = emptyBackdrop(),
            shape = { RectangleShape },
            effects = {},
            exportedBackdrop = backdrop,
        )
    else this

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
    if (enableBlur) rememberLayerBackdrop() else null

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
