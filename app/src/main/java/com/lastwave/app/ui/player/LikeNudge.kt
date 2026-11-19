package com.lastwave.app.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Key of the current track once it has been started [LIKE_NUDGE_PLAYS]+ times
 * without being liked; null otherwise. The like buttons read it to suggest a like.
 */
val LocalLikeNudge = compositionLocalOf<String?> { null }

internal const val LIKE_NUDGE_PLAYS = 3
private const val NUDGE_PULSES = 7

/** Permanent liked-heart red, independent of the active Material/Comic theme. */
internal val LikedHeartRed = Color(0xFFE53935)
internal val LikedHeartRedOnDark = Color(0xFFFF453A)
private val LikeNudgeGlowRed = Color(0xFFFF1744)

private val HeartSilhouettePath: Path by lazy {
    Path().apply {
        fun append(node: VectorNode) {
            when (node) {
                is VectorPath -> node.pathData.toPath(this)
                is VectorGroup -> for (i in 0 until node.size) append(node[i])
            }
        }
        append(Icons.Filled.Favorite.root)
    }
}

@Stable
internal class LikedTapMotion(
    private val scaleState: Animatable<Float, *>,
    private val rotationState: Animatable<Float, *>,
    private val burstState: Animatable<Float, *>,
) {
    val scale: Float get() = scaleState.value
    val rotationZ: Float get() = rotationState.value
    /** Sparkle burst progress, 0 at the like to 1 (done / idle). */
    val burst: Float get() = burstState.value
}

/**
 * Sparkles bursting out of a like button as it fills: six four-point stars fly past the
 * button's edge and fade, timed with [motion]'s pop. Draw only, outside the bounds, so
 * apply it before any clip.
 */
internal fun Modifier.likeSparkles(motion: LikedTapMotion, color: Color): Modifier =
    sparkleBurst({ motion.burst }, color)

/**
 * Six four-point stars flying out past the bounds and fading as [progress] runs 0..1
 * (1 = idle). Draw only, outside the bounds, so apply it before any clip.
 */
internal fun Modifier.sparkleBurst(progress: () -> Float, color: Color): Modifier = drawWithContent {
    drawContent()
    val p = progress()
    if (p >= 1f) return@drawWithContent
    val radius = size.minDimension / 2f
    for (i in 0 until SPARKLES) {
        val angle = Math.toRadians(i * 360.0 / SPARKLES - 75.0)
        val distance = radius * (0.7f + 0.65f * p)
        val at = center + Offset(cos(angle).toFloat(), sin(angle).toFloat()) * distance
        val arm = radius * (if (i % 2 == 0) 0.2f else 0.13f) * (1f - p * 0.6f)
        drawPath(sparklePath(at, arm), color.copy(alpha = (1f - p) * color.alpha))
    }
}

private const val SPARKLES = 6

/** Four-point star: long arms on the axes, pinched waist on the diagonals. */
private fun sparklePath(at: Offset, arm: Float): Path = Path().apply {
    val waist = arm * 0.28f
    moveTo(at.x, at.y - arm)
    lineTo(at.x + waist, at.y - waist)
    lineTo(at.x + arm, at.y)
    lineTo(at.x + waist, at.y + waist)
    lineTo(at.x, at.y + arm)
    lineTo(at.x - waist, at.y + waist)
    lineTo(at.x - arm, at.y)
    lineTo(at.x - waist, at.y - waist)
    close()
}

/**
 * Pop + jiggle motion triggered only when [liked] transitions from false to true,
 * so opening a screen on an already-liked song stays still.
 */
@Composable
internal fun rememberLikedTapMotion(liked: Boolean, initialScale: Float = 0.45f): LikedTapMotion {
    var previousLiked by remember { mutableStateOf(liked) }
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    val burst = remember { Animatable(1f) }
    LaunchedEffect(liked) {
        val justLiked = !previousLiked && liked
        previousLiked = liked
        if (!justLiked) {
            scale.snapTo(1f)
            rotation.snapTo(0f)
            burst.snapTo(1f)
            return@LaunchedEffect
        }
        coroutineScope {
            launch {
                burst.snapTo(0f)
                burst.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
            }
            launch {
                scale.snapTo(initialScale)
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.45f,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }
            launch {
                rotation.snapTo(0f)
                rotation.animateTo(-12f, tween(75, easing = FastOutSlowInEasing))
                rotation.animateTo(9f, tween(85, easing = FastOutSlowInEasing))
                rotation.animateTo(0f, tween(90, easing = FastOutSlowInEasing))
            }
        }
    }
    return remember(scale, rotation, burst) { LikedTapMotion(scale, rotation, burst) }
}

/**
 * Heart-shaped red glow and jiggle animation for the like icon itself:
 * [NUDGE_PULSES] beats each time a nudged song starts, then a subtle steady
 * heart-shaped red aura/outline until the song is liked.
 */
@Composable
internal fun Modifier.likeNudgeHeart(active: Boolean): Modifier {
    val key = if (active) LocalLikeNudge.current else null
    if (key == null) return this
    val pulse = remember(key) { Animatable(0f) }
    val jiggle = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        repeat(NUDGE_PULSES) {
            coroutineScope {
                launch {
                    pulse.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
                    pulse.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
                }
                launch {
                    jiggle.animateTo(-14f, tween(80, easing = FastOutSlowInEasing))
                    jiggle.animateTo(13f, tween(95, easing = FastOutSlowInEasing))
                    jiggle.animateTo(-9f, tween(85, easing = FastOutSlowInEasing))
                    jiggle.animateTo(5f, tween(75, easing = FastOutSlowInEasing))
                    jiggle.animateTo(0f, tween(80, easing = FastOutSlowInEasing))
                }
            }
            delay(180)
        }
    }
    return this
        .graphicsLayer {
            val scale = 1f + 0.16f * pulse.value
            scaleX = scale
            scaleY = scale
            rotationZ = jiggle.value
        }
        .drawBehind {
            val p = pulse.value
            val vw = Icons.Filled.Favorite.viewportWidth.takeIf { it > 0f } ?: 24f
            val vh = Icons.Filled.Favorite.viewportHeight.takeIf { it > 0f } ?: 24f
            val scaledHeart = Path().apply {
                addPath(HeartSilhouettePath)
                val matrix = Matrix()
                matrix.scale(size.width / vw, size.height / vh)
                transform(matrix)
            }
            val center = Offset(size.width / 2f, size.height / 2f)

            // Soft heart-shaped bloom behind the icon.
            if (p > 0.01f) {
                withTransform({ scale(1.48f, 1.48f, pivot = center) }) {
                    drawPath(scaledHeart, LikeNudgeGlowRed.copy(alpha = 0.14f * p))
                }
                withTransform({ scale(1.30f, 1.30f, pivot = center) }) {
                    drawPath(scaledHeart, LikeNudgeGlowRed.copy(alpha = 0.24f * p))
                }
            }
            withTransform({ scale(1.18f, 1.18f, pivot = center) }) {
                drawPath(scaledHeart, LikeNudgeGlowRed.copy(alpha = 0.14f + 0.22f * p))
            }
            // Crisp heart-shaped red outline hug.
            withTransform({ scale(1.10f, 1.10f, pivot = center) }) {
                drawPath(
                    path = scaledHeart,
                    color = LikeNudgeGlowRed.copy(alpha = 0.58f + 0.42f * p),
                    style = Stroke(
                        width = (1.5f + 1.1f * p).dp.toPx() / 1.10f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
}
