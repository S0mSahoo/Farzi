package com.example.ui.util

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith

/**
 * Standard motion specifications for the Paisa application.
 * Centralizing these constants ensures a consistent motion language across all screens.
 */
object PaisaMotion {
    const val EnterFadeDurationMillis = 220
    const val ExitFadeDurationMillis = 180
    val MotionEasing = FastOutSlowInEasing

    /**
     * Standard fade-through transition for switching content in-place.
     */
    val DefaultContentTransform: ContentTransform = fadeIn(
        animationSpec = tween(EnterFadeDurationMillis, easing = MotionEasing)
    ) togetherWith fadeOut(
        animationSpec = tween(ExitFadeDurationMillis, easing = MotionEasing)
    )

    /**
     * Reusable enter transition.
     */
    val EnterTransition: EnterTransition = fadeIn(
        animationSpec = tween(EnterFadeDurationMillis, easing = MotionEasing)
    )

    /**
     * Reusable exit transition.
     */
    val ExitTransition: ExitTransition = fadeOut(
        animationSpec = tween(ExitFadeDurationMillis, easing = MotionEasing)
    )

    /**
     * Create a custom content transform with optional size transform behavior.
     */
    fun contentTransform(
        clip: Boolean = true,
        sizeTransform: SizeTransform? = null
    ): ContentTransform = (fadeIn(
        animationSpec = tween(EnterFadeDurationMillis, easing = MotionEasing)
    ) togetherWith fadeOut(
        animationSpec = tween(ExitFadeDurationMillis, easing = MotionEasing)
    )).apply {
        targetContentZIndex = 0f
    }
}
