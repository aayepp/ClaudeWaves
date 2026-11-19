package com.lastwave.app.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Shared, restrained Material 3 Expressive motion language for the app. */
object ExpressiveMotion {
    const val Quick = 160
    const val Standard = 300
    const val Emphasized = 440

    // Spatial springs: overshoot and bounce for layout and scale transitions
    fun <T> spatialFast() = spring<T>(
        dampingRatio = 0.75f,
        stiffness = Spring.StiffnessMedium, // 1500f: immediate, tactile button feedback (~180ms)
    )

    fun <T> spatialDefault() = spring<T>(
        dampingRatio = 0.8f,
        stiffness = 650f, // Snappy & fluid Material 3 expressive motion (~280ms)
    )

    fun <T> spatialSlow() = spring<T>(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessMediumLow, // 400f
    )

    // Legacy alias
    fun <T> spatialSpring() = spatialDefault<T>()

    // Effects springs: no overshoot (for alpha, color)
    fun <T> effectsFast() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium, // 1500f
    )

    fun <T> effectsDefault() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 700f, // Clean fade without sluggish trailing (~250ms)
    )

    fun <T> effectsSlow() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow, // 400f
    )

    // Legacy alias
    fun <T> smoothSpring() = effectsDefault<T>()

    // Screen transitions: physics-driven spring slide + fast fade so 120Hz/165Hz
    // panels track fluid deceleration without dual full-screen scale offscreen layers.
    private val navSlideSpring = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 620f,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    )

    fun forwardEnter(): EnterTransition =
        slideInHorizontally(navSlideSpring) { it / 8 } +
            fadeIn(tween(150))

    fun forwardExit(): ExitTransition =
        slideOutHorizontally(navSlideSpring) { -it / 20 } +
            fadeOut(tween(120))

    fun backEnter(): EnterTransition =
        slideInHorizontally(navSlideSpring) { -it / 20 } +
            fadeIn(tween(140))

    fun backExit(): ExitTransition =
        slideOutHorizontally(navSlideSpring) { it / 8 } +
            fadeOut(tween(120))
}

/**
 * Tactile spring press feedback modifier conforming to Material 3 Expressive.
 * Subtly squishes the component when held and springs back dynamically on release.
 */
@Composable
fun Modifier.expressivePressScale(
    interactionSource: MutableInteractionSource,
    targetScale: Float = 0.96f,
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = ExpressiveMotion.spatialFast(),
        label = "expressivePressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Clickable surface with expressive spring-based bounce feedback.
 */
@Composable
fun Modifier.expressiveBounceClickable(
    enabled: Boolean = true,
    targetScale: Float = 0.96f,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .expressivePressScale(interactionSource, targetScale)
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            enabled = enabled,
            onClick = onClick,
        )
}

/**
 * Returns horizontal track-change direction (`+1` next, `-1` previous, `0` neutral crossfade)
 * by comparing queue indices across track switches without reacting to metadata updates.
 */
internal fun computeTrackStepDirection(
    oldKey: String?,
    oldIdx: Int,
    oldSize: Int,
    trackKey: String,
    queueIndex: Int,
    queueSize: Int,
): Int = when {
    oldKey == null || oldKey == trackKey -> 0
    oldIdx < 0 || queueIndex < 0 || queueSize <= 1 || oldSize != queueSize -> 0
    // Wrap-around across queue ends conflicts with 0<->1 direction in 2-track queues.
    queueSize > 2 && oldIdx == queueSize - 1 && queueIndex == 0 -> 1
    queueSize > 2 && oldIdx == 0 && queueIndex == queueSize - 1 -> -1
    queueIndex > oldIdx -> 1
    queueIndex < oldIdx -> -1
    else -> 0
}

@Composable
fun rememberTrackStepDirection(
    trackKey: String,
    queueIndex: Int,
    queueSize: Int,
): Int {
    val prevMeta = remember { intArrayOf(-1, -1) }
    val lastKey = remember { arrayOf<String?>(null) }
    val direction = remember(trackKey) {
        computeTrackStepDirection(
            oldKey = lastKey[0],
            oldIdx = prevMeta[0],
            oldSize = prevMeta[1],
            trackKey = trackKey,
            queueIndex = queueIndex,
            queueSize = queueSize,
        )
    }
    SideEffect {
        lastKey[0] = trackKey
        prevMeta[0] = queueIndex
        prevMeta[1] = queueSize
    }
    return direction
}

private data class TrackFrame<T>(val key: String, val value: T)

private val trackSlideSpring = spring<IntOffset>(
    dampingRatio = 0.85f,
    stiffness = 560f,
    visibilityThreshold = IntOffset.VisibilityThreshold,
)

/**
 * Directional slide + fade transition when skipping Next (`+1`) or Previous (`-1`),
 * keyed strictly by [trackKey] so background artwork or duration updates never re-trigger it.
 */
@Composable
fun <T> DirectionalTrackContent(
    trackKey: String,
    track: T,
    stepDirection: Int,
    modifier: Modifier = Modifier,
    clip: Boolean = true,
    slideFraction: Float = 0.24f,
    label: String = "directionalTrack",
    content: @Composable AnimatedContentScope.(T) -> Unit,
) {
    AnimatedContent(
        targetState = TrackFrame(trackKey, track),
        transitionSpec = {
            val enter = when {
                stepDirection > 0 -> slideInHorizontally(trackSlideSpring) { (it * slideFraction).roundToInt() } + fadeIn(tween(200))
                stepDirection < 0 -> slideInHorizontally(trackSlideSpring) { -(it * slideFraction).roundToInt() } + fadeIn(tween(200))
                else -> fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.96f)
            }
            val exit = when {
                stepDirection > 0 -> slideOutHorizontally(trackSlideSpring) { -(it * (slideFraction * 0.8f)).roundToInt() } + fadeOut(tween(150))
                stepDirection < 0 -> slideOutHorizontally(trackSlideSpring) { (it * (slideFraction * 0.8f)).roundToInt() } + fadeOut(tween(150))
                else -> fadeOut(tween(160))
            }
            (enter togetherWith exit) using SizeTransform(clip = clip)
        },
        contentKey = { it.key },
        label = label,
        modifier = modifier,
    ) { frame ->
        content(frame.value)
    }
}
