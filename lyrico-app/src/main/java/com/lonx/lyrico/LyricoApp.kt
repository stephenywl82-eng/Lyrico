package com.lonx.lyrico

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.generated.NavGraphs
import com.ramcosta.composedestinations.generated.destinations.EditMetadataDestination
import com.ramcosta.composedestinations.spec.Direction
import androidx.compose.material3.Surface
import com.lonx.lyrico.ui.theme.Motion

@Composable
fun LyricoApp(externalUri: Uri?, externalEditRequestId: Long = 0L) {
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        val externalUriString = externalUri?.toString()
        key(externalUriString, externalEditRequestId) {
            val navController = rememberNavController()
            val startDirection: Direction =
                externalUriString?.let {
                    EditMetadataDestination(it, externalEditRequestId = externalEditRequestId)
                }
                    ?: NavGraphs.root.defaultStartDirection

            Log.d("LyricoApp", "LyricoApp: $startDirection")
            DestinationsNavHost(
                navGraph = NavGraphs.root,
                navController = navController,
                start = startDirection,
                dependenciesContainerBuilder = {
                },
                // M3 Expressive 转场：fade-through（出页先快速淡出、入页延迟淡入，任意时刻只一个页面可读）
                // + emphasized 缓动；返回（pop）方向用 spring 携带手势速度实现预测返回跟手。覆盖全站所有路由。
                defaultTransitions = object : NavHostAnimatedDestinationStyle() {
                    override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                        {
                            slideInHorizontally(Motion.navSlideIn) { it / 6 } + fadeIn(Motion.navFadeIn)
                        }

                    override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                        {
                            slideOutHorizontally(Motion.navSlideOut) { -it / 10 } + fadeOut(Motion.navFadeOut)
                        }

                    override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
                        {
                            slideInHorizontally(Motion.navPopSlideIn) { -it / 6 } + fadeIn(Motion.navFadeIn)
                        }

                    override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
                        {
                            slideOutHorizontally(Motion.navPopSlideOut) { it / 10 } + fadeOut(Motion.navFadeOut)
                        }
                }
            )
        }
    }
}
