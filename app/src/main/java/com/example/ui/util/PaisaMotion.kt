package com.example.ui.util

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

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
}

/**
 * Reusable animated text component that smoothly transitions when financial or numeric text changes.
 */
@Composable
fun AnimatedFinancialText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    label: String = "animated_financial_text"
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = { PaisaMotion.DefaultContentTransform },
        label = label,
        modifier = modifier
    ) { targetText ->
        Text(
            text = targetText,
            style = style,
            color = color,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing,
            textAlign = textAlign,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines
        )
    }
}

/**
 * Reusable wrapper for animating a small data section or custom content when its target state changes.
 */
@Composable
fun <T> AnimatedDataContent(
    targetState: T,
    modifier: Modifier = Modifier,
    label: String = "animated_data_content",
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        transitionSpec = { PaisaMotion.DefaultContentTransform },
        label = label,
        modifier = modifier
    ) { state ->
        content(state)
    }
}
