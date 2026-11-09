package com.lonx.lyrico.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable

/**
 * M3 Expressive 共享动效规格。
 *
 * Compose 1.6.x 早于 Material 3 官方的 MotionScheme，这里手工对齐 Material 3 Expressive 的
 * emphasized 缓动曲线与「fade-through」导航转场：出页先快速淡出、入页延迟淡入，任意时刻只有一个
 * 页面可读，转场读作「位移」而不是「硬切」。返回（pop）方向用 spring 携带手势速度实现预测返回跟手。
 *
 * 规格来源：对标 SDWMP3_CN 的 M3 Expressive 转场（emphasized 曲线 + fade-through + pop spring）。
 */
object Motion {

    /** M3 Expressive emphasized decelerate 缓动，官方 MaterialSharedAxis 同源曲线。 */
    val emphasized: CubicBezierEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    // ---- 导航 push / pop：fade-through ----
    // 出页比入页先走完淡出，入页延迟淡入，两者不重叠 —— 避免旧版「两页半透明错位」出现的竖直硬缝。
    // 时长刻意比常规 300ms 更长：push 太快用户跟不上。
    val navSlideIn: FiniteAnimationSpec<IntOffset> = tween(640, easing = emphasized)
    val navSlideOut: FiniteAnimationSpec<IntOffset> = tween(520, easing = emphasized)

    // 返回（pop）用 spring 而非 tween：预测返回手势松手时从 seek 处带速度收尾，跟手不脱节。
    // 临界阻尼（NoBouncy）保证整页滑动不回弹、落点干净。
    val navPopSlideIn: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 120f
    )
    val navPopSlideOut: FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 160f
    )

    // 淡入延迟 200ms 起，与出页 180ms 淡出错开，保证「同一时刻只有一个页面可读」。
    val navFadeIn: FiniteAnimationSpec<Float> = tween(320, delayMillis = 200, easing = emphasized)
    val navFadeOut: FiniteAnimationSpec<Float> = tween(180, easing = FastOutSlowInEasing)

    // ---- 内容级转场（列表/卡片内状态切换：选择态顶栏、卡片展开、占位图等）----
    // 与导航转场同源的 emphasized 曲线，保证全站 M3 Expressive 观感一致；
    // 时长刻意比整页导航转场(640/520ms)更短，内容切换更利落。
    val contentDuration = 300

    /**
     * [M3 Expressive] 内容级动效规格：从主题注入的 MotionScheme（MaterialExpressiveTheme
     * 默认 MotionScheme.expressive()）取官方弹簧规格，取代各页面散落的
     * tween(contentDuration, emphasized) 手写常量，做到「主题定义一次、全站继承」。
     *
     * - spatial：位置/尺寸/形状变化（带过冲），用于 slide/expand/animateContentSize
     * - effects：颜色/透明度变化（不过冲），用于 fade
     *
     * 注意：本函数是 @Composable，调用方须在 Composable 作用域先取好返回值，
     * 再捕获进 transitionSpec 等非 Composable lambda。
     */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun contentSpecs(): ContentMotionSpecs {
        val scheme = MaterialTheme.motionScheme
        return ContentMotionSpecs(
            effectsFloat = scheme.defaultEffectsSpec(),
            effectsColor = scheme.defaultEffectsSpec(),
            spatialIntOffset = scheme.defaultSpatialSpec(),
            spatialIntSize = scheme.defaultSpatialSpec(),
            spatialDp = scheme.defaultSpatialSpec(),
        )
    }
}

/**
 * 一组按动画值类型分好的官方 MotionScheme 规格。
 * 各规格是同一个 spring 策略的不同泛型实例，视觉一致、深浅/设备自适应由主题保证。
 */
data class ContentMotionSpecs(
    val effectsFloat: FiniteAnimationSpec<Float>,
    val effectsColor: FiniteAnimationSpec<Color>,
    val spatialIntOffset: FiniteAnimationSpec<IntOffset>,
    val spatialIntSize: FiniteAnimationSpec<IntSize>,
    val spatialDp: FiniteAnimationSpec<Dp>,
)
