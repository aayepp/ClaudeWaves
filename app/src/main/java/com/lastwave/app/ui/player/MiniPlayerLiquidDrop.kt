package com.lastwave.app.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.lastwave.app.playback.PlayableTrack
import com.lastwave.app.ui.common.ArtworkImage
import com.lastwave.app.ui.theme.ComicInk
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.LocalLiquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

enum class LiquidDropKind {
    PLAY,
    QUEUE,
}

@Immutable
data class LiquidDropRequest(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val startCenterInRoot: Offset,
    val kind: LiquidDropKind,
)

/**
 * Coordinates the 3D water droplet flight from any song row into [MiniPlayer] and
 * exposes snapshot floats read strictly in draw/graphicsLayer phases (zero idle recomposition).
 */
@Stable
class MiniPlayerDropController {
    private var nextId = 1L

    var activeDrop by mutableStateOf<LiquidDropRequest?>(null)
        private set

    var impactXFraction by mutableFloatStateOf(0.5f)
        internal set

    var crownHeightPx by mutableFloatStateOf(0f)
        internal set

    var miniBounce by mutableFloatStateOf(0f)
        internal set

    var miniWobbleTilt by mutableFloatStateOf(0f)
        internal set

    var impactRipple by mutableFloatStateOf(0f)
        internal set

    var queueBannerProgress by mutableFloatStateOf(0f)
        internal set

    fun triggerDrop(
        track: PlayableTrack,
        startCenterInRoot: Offset,
        kind: LiquidDropKind,
    ) = triggerDrop(
        title = track.title,
        artist = track.artist,
        artworkUrl = track.artworkUrl,
        startCenterInRoot = startCenterInRoot,
        kind = kind,
    )

    fun triggerDrop(
        title: String,
        artist: String,
        artworkUrl: String?,
        startCenterInRoot: Offset,
        kind: LiquidDropKind,
    ) {
        if (kind == LiquidDropKind.PLAY) {
            queueBannerProgress = 0f
        }
        activeDrop = LiquidDropRequest(
            id = nextId++,
            title = title,
            artist = artist,
            artworkUrl = artworkUrl,
            startCenterInRoot = startCenterInRoot,
            kind = kind,
        )
    }

    internal fun triggerFromBounds(
        title: String,
        artist: String,
        artworkUrl: String?,
        rowBoundsInRoot: Rect?,
        kind: LiquidDropKind,
        thumbOffsetPx: Float,
        horizontalShiftPx: Float = 0f,
    ) {
        val origin = if (rowBoundsInRoot != null) {
            Offset(
                x = rowBoundsInRoot.left + thumbOffsetPx + horizontalShiftPx,
                y = rowBoundsInRoot.center.y,
            )
        } else {
            Offset.Unspecified
        }
        triggerDrop(title, artist, artworkUrl, origin, kind)
    }

    internal fun clearIfMatches(id: Long) {
        if (activeDrop?.id == id) {
            activeDrop = null
        }
    }
}

/** Plain root-bounds holder so song rows track drop origin with zero scroll recompositions. */
internal class DropRowBoundsHolder {
    var rect: Rect? = null
}

val LocalMiniPlayerDropController = staticCompositionLocalOf<MiniPlayerDropController?> { null }

private data class PeelSample(
    val x: Float,
    val y: Float,
    val angleRad: Float,
    val stretch: Float,
)

private data class SplashParticleSpec(
    val delay: Float,
    val dur: Float,
    val vxDp: Float,
    val vyDp: Float,
    val gDp: Float,
    val r0Dp: Float,
    val r1Dp: Float,
    val isTeardrop: Boolean,
)

private val CRYSTAL_SPLASH_SPECS = arrayOf(
    // Primary symmetrical flying teardrop jets
    SplashParticleSpec(delay = 0.00f, dur = 0.84f, vxDp = -58f, vyDp = 92f, gDp = 152f, r0Dp = 5.6f, r1Dp = 1.8f, isTeardrop = true),
    SplashParticleSpec(delay = 0.02f, dur = 0.82f, vxDp = +62f, vyDp = 84f, gDp = 150f, r0Dp = 5.2f, r1Dp = 1.6f, isTeardrop = true),
    // High graceful secondary arcs
    SplashParticleSpec(delay = 0.03f, dur = 0.86f, vxDp = -30f, vyDp = 122f, gDp = 170f, r0Dp = 4.2f, r1Dp = 1.2f, isTeardrop = true),
    SplashParticleSpec(delay = 0.03f, dur = 0.84f, vxDp = +35f, vyDp = 114f, gDp = 168f, r0Dp = 3.9f, r1Dp = 1.1f, isTeardrop = true),
    // Center crown spritzer
    SplashParticleSpec(delay = 0.00f, dur = 0.80f, vxDp = -5f, vyDp = 132f, gDp = 180f, r0Dp = 3.2f, r1Dp = 0.9f, isTeardrop = true),
    // Wide lateral sparkling mist beads
    SplashParticleSpec(delay = 0.06f, dur = 0.76f, vxDp = -82f, vyDp = 62f, gDp = 138f, r0Dp = 2.9f, r1Dp = 0.8f, isTeardrop = false),
    SplashParticleSpec(delay = 0.07f, dur = 0.76f, vxDp = +86f, vyDp = 66f, gDp = 140f, r0Dp = 2.7f, r1Dp = 0.8f, isTeardrop = false),
    // 2 tiny shimmer micro-sparks near crest
    SplashParticleSpec(delay = 0.02f, dur = 0.58f, vxDp = -16f, vyDp = 96f, gDp = 175f, r0Dp = 1.9f, r1Dp = 0.5f, isTeardrop = false),
    SplashParticleSpec(delay = 0.04f, dur = 0.58f, vxDp = +20f, vyDp = 90f, gDp = 172f, r0Dp = 1.8f, r1Dp = 0.5f, isTeardrop = false),
)

private fun samplePeelTrajectory(
    uRaw: Float,
    start: Offset,
    target: Offset,
    peelLiftPx: Float,
    plungeLeadPx: Float,
): PeelSample {
    val u = uRaw.coerceIn(0f, 1f)
    // Continuous zero-stop velocity curve: peels smoothly off thumbnail then glides down center
    val s = (u * 1.12f - 0.12f * u * u + sin(u * PI.toFloat()) * 0.04f).coerceIn(0f, 1f)
    val omt = 1f - s
    val p1Y = (minOf(start.y, target.y) - peelLiftPx).coerceAtLeast(32f)
    val p1 = Offset(lerp(start.x, target.x, 0.54f), p1Y)
    val p2Y = (target.y - plungeLeadPx).coerceAtLeast(p1Y + 12f).coerceAtMost(target.y - 18f)
    val p2 = Offset(target.x, p2Y)

    val x = omt * omt * omt * start.x +
        3f * omt * omt * s * p1.x +
        3f * omt * s * s * p2.x +
        s * s * s * target.x
    val y = omt * omt * omt * start.y +
        3f * omt * omt * s * p1.y +
        3f * omt * s * s * p2.y +
        s * s * s * target.y
    val vx = 3f * omt * omt * (p1.x - start.x) +
        6f * omt * s * (p2.x - p1.x) +
        3f * s * s * (target.x - p2.x)
    val vy = 3f * omt * omt * (p1.y - start.y) +
        6f * omt * s * (p2.y - p1.y) +
        3f * s * s * (target.y - p2.y)
    val speed = hypot(vx, vy)
    val stretch = 1f + ((speed / 2100f) * sin(u * PI.toFloat()) * 0.20f).coerceIn(0f, 0.20f)
    return PeelSample(x = x, y = y, angleRad = atan2(vy, vx), stretch = stretch)
}

@Composable
internal fun MiniPlayerLiquidDropOverlay(
    controller: MiniPlayerDropController,
    miniCardBounds: () -> Rect?,
    hostOrigin: () -> Offset,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val drop = controller.activeDrop
    if (drop == null || expanded) {
        if (expanded && controller.activeDrop != null) {
            LaunchedEffect(expanded) {
                controller.crownHeightPx = 0f
                controller.miniBounce = 0f
                controller.miniWobbleTilt = 0f
                controller.impactRipple = 0f
                controller.queueBannerProgress = 0f
                controller.clearIfMatches(drop?.id ?: -1L)
            }
        }
        return
    }

    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val isGlass = LocalLiquidGlass.current
    val isComic = LocalComicTheme.current
    val isDark = LocalIsDarkTheme.current
    val accent = MaterialTheme.colorScheme.primary

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val hostWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val hostHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val thumbOffsetPx = with(density) { 42.dp.toPx() }
        val maxCrownPx = with(density) { 24.dp.toPx() }
        val dropRestRadiusPx = with(density) { 33.dp.toPx() }
        val bridgeMaxGapPx = with(density) { 92.dp.toPx() }
        val peelLiftPx = with(density) { 42.dp.toPx() }
        val plungeLeadPx = with(density) { 112.dp.toPx() }

        val origin = hostOrigin()
        val miniRect = miniCardBounds()
        val crownMarginPx = with(density) { 48.dp.toPx() }
        val start = remember(drop.id, origin, hostWidthPx, hostHeightPx) {
            if (drop.startCenterInRoot == Offset.Unspecified) {
                Offset(hostWidthPx * 0.36f, hostHeightPx * 0.42f)
            } else {
                Offset(
                    x = (drop.startCenterInRoot.x - origin.x).coerceIn(hostWidthPx * 0.14f, hostWidthPx * 0.86f),
                    y = (drop.startCenterInRoot.y - origin.y).coerceIn(40f, hostHeightPx - 140f),
                )
            }
        }
        val target = remember(drop.id, miniRect, hostWidthPx, hostHeightPx) {
            if (miniRect != null && miniRect.width > 20f) {
                val pillR = (miniRect.height * 0.5f).coerceAtLeast(with(density) { 28.dp.toPx() })
                val minSafeX = miniRect.left + pillR + crownMarginPx
                val maxSafeX = miniRect.right - pillR - crownMarginPx
                val preferredFrac = if (drop.kind == LiquidDropKind.PLAY) 0.46f else 0.50f
                val rawTx = miniRect.left + miniRect.width * preferredFrac
                val tx = if (maxSafeX > minSafeX) rawTx.coerceIn(minSafeX, maxSafeX) else miniRect.center.x
                Offset(tx, miniRect.top + with(density) { 4.dp.toPx() })
            } else {
                Offset(hostWidthPx * 0.48f, hostHeightPx - with(density) { 104.dp.toPx() })
            }
        }

        val progressAnim = remember(drop.id) { Animatable(0f) }
        val splashAnim = remember(drop.id) { Animatable(0f) }
        var bridgeStrength by remember(drop.id) { mutableFloatStateOf(0f) }
        var isImpactTriggered by remember(drop.id) { mutableStateOf(false) }

        LaunchedEffect(drop.id) {
            val finalFrac = if (miniRect != null && miniRect.width > 20f) {
                ((target.x - miniRect.left) / miniRect.width).coerceIn(0.22f, 0.78f)
            } else {
                0.48f
            }
            controller.impactXFraction = finalFrac
            controller.crownHeightPx = 0f
            controller.miniBounce = 0f
            controller.miniWobbleTilt = 0f
            controller.impactRipple = 0f
            bridgeStrength = 0f

            // Stage 1+2 — Single continuous Liquid Peel-Off glide directly from tapped cover to MiniPlayer
            val flightMs = if (drop.kind == LiquidDropKind.PLAY) 740 else 820
            progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = flightMs,
                    easing = LinearEasing,
                ),
            ) {
                val u = value
                if (u >= 0.56f && miniRect != null && miniRect.width > 20f) {
                    val curX = samplePeelTrajectory(u, start, target, peelLiftPx, plungeLeadPx).x
                    controller.impactXFraction = ((curX - miniRect.left) / miniRect.width).coerceIn(0.22f, 0.78f)
                }
                if (u >= 0.56f) {
                    val ct = ((u - 0.56f) / 0.44f).coerceIn(0f, 1f)
                    controller.crownHeightPx = maxCrownPx * sin(ct * PI.toFloat())
                }
                if (u >= 0.66f) {
                    val bt = ((u - 0.66f) / 0.34f).coerceIn(0f, 1f)
                    bridgeStrength = sin(bt * PI.toFloat())
                }
            }
            controller.impactXFraction = finalFrac

            bridgeStrength = 0f
            isImpactTriggered = true
            haptics.performHapticFeedback(
                if (drop.kind == LiquidDropKind.QUEUE) HapticFeedbackType.Confirm else HapticFeedbackType.TextHandleMove,
            )

            // Stage 3 — Impact elastic jelly wobble, pill-clipped internal ripple & 7-droplet crystal splash
            val tiltSide = when {
                finalFrac < 0.42f -> -1f
                finalFrac > 0.58f -> 1f
                else -> -0.55f
            }
            val bounceJob = launch {
                val anim = Animatable(0f)
                anim.animateTo(1f, tween(durationMillis = 680, easing = LinearEasing)) {
                    val bt = value
                    controller.miniBounce = sin(bt * 4.0f * PI.toFloat()) * exp(-2.6f * bt)
                    controller.miniWobbleTilt = tiltSide * 2.6f * sin(bt * 3.0f * PI.toFloat()) * exp(-2.8f * bt)
                    controller.crownHeightPx = (maxCrownPx * 0.55f) * sin(bt * 4.0f * PI.toFloat()) * exp(-3.2f * bt)
                    controller.impactRipple = (bt * 1.25f).coerceIn(0f, 1f)
                }
                controller.miniBounce = 0f
                controller.miniWobbleTilt = 0f
                controller.crownHeightPx = 0f
                controller.impactRipple = 0f
            }

            val splashJob = launch {
                splashAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 540,
                        easing = LinearEasing,
                    ),
                )
            }

            if (drop.kind == LiquidDropKind.QUEUE) {
                val bannerAnim = Animatable(controller.queueBannerProgress)
                bannerAnim.animateTo(1f, tween(durationMillis = 220, easing = FastOutSlowInEasing)) {
                    controller.queueBannerProgress = value
                }
                delay(1350L)
                if (controller.activeDrop?.id == drop.id) {
                    bannerAnim.animateTo(0f, tween(durationMillis = 260, easing = FastOutSlowInEasing)) {
                        controller.queueBannerProgress = value
                    }
                }
            }

            bounceJob.join()
            splashJob.join()
            controller.clearIfMatches(drop.id)
        }

        val u = progressAnim.value
        val sample = samplePeelTrajectory(u, start, target, peelLiftPx, plungeLeadPx)
        val cornerBlend = (1f - u / 0.18f).coerceIn(0f, 1f)
        val peelWave = if (u < 0.25f) sin((u / 0.25f) * PI.toFloat() * 1.2f) else 0f
        val squashX = 1f + 0.13f * peelWave
        val squashY = 1f - 0.09f * peelWave
        val waterMix = 1f - cornerBlend * 0.70f

        val dropRadiusPx = when {
            u < 0.18f -> lerp(dropRestRadiusPx * 0.78f, dropRestRadiusPx, (u / 0.18f).coerceIn(0f, 1f))
            u > 0.84f -> lerp(dropRestRadiusPx, dropRestRadiusPx * 0.62f, ((u - 0.84f) / 0.16f).coerceIn(0f, 1f))
            else -> dropRestRadiusPx
        }
        val dropAlpha = if (isImpactTriggered) {
            0f
        } else if (u > 0.92f) {
            (1f - (u - 0.92f) / 0.08f).coerceIn(0f, 1f)
        } else {
            (0.68f + u * 6f).coerceAtMost(1f)
        }
        val boxWidthPx = dropRadiusPx * 2.8f
        val boxHeightPx = dropRadiusPx * 3.8f
        val boxWidthDp = with(density) { boxWidthPx.toDp() }
        val boxHeightDp = with(density) { boxHeightPx.toDp() }

        // Background Canvas: tap ripple, peel-off liquid neck, trailing beads, concave bridge & 7-droplet crystal splash
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1) Crisp tap ripple ring right at the row thumbnail origin
            if (!isImpactTriggered && u < 0.24f) {
                val rp = u / 0.24f
                val fade = (1f - rp) * (1f - rp)
                val ringColor = if (isComic) ComicInk else if (isGlass) Color(0xFFA8EDFF) else accent
                drawCircle(
                    color = ringColor.copy(alpha = 0.72f * fade),
                    radius = lerp(18.dp.toPx(), 46.dp.toPx(), rp),
                    center = start,
                    style = Stroke(width = 1.8.dp.toPx()),
                )
            }

            // 2) Glistening Liquid Peel-Off neck connecting thumbnail -> detaching water droplet
            if (!isImpactTriggered && u in 0.01f..0.28f) {
                val peelP = u / 0.28f
                val neckFade = sin(peelP * PI.toFloat())
                val dist = hypot(sample.x - start.x, sample.y - start.y)
                if (dist in 3.dp.toPx()..78.dp.toPx()) {
                    val midX = (start.x + sample.x) * 0.5f
                    val midY = (start.y + sample.y) * 0.5f + 5.dp.toPx()
                    val w1 = 13.dp.toPx() * (1f - peelP * 0.70f)
                    val w2 = 9.dp.toPx() * (1f - peelP * 0.45f)
                    val neckPath = Path().apply {
                        moveTo(start.x - w1, start.y)
                        quadraticTo(midX - w2 * 0.5f, midY, sample.x - w2, sample.y)
                        lineTo(sample.x + w2, sample.y)
                        quadraticTo(midX + w2 * 0.5f, midY, start.x + w1, start.y)
                        close()
                    }
                    val neckColor = if (isComic) accent else if (isGlass) Color(0xFF82E8FF) else accent
                    drawPath(path = neckPath, color = neckColor.copy(alpha = 0.44f * neckFade))
                }
            }

            // 3) 3 trailing shimmering water micro-beads along the continuous arc
            if (!isImpactTriggered && u in 0.08f..0.92f) {
                val trailLags = floatArrayOf(0.055f, 0.11f, 0.165f)
                val trailRadiiDp = floatArrayOf(6.8f, 4.8f, 3.2f)
                val trailOffX = floatArrayOf(-5f, 6f, -3f)
                for (i in trailLags.indices) {
                    val lu = u - trailLags[i]
                    if (lu in 0.04f..0.90f) {
                        val pos = samplePeelTrajectory(lu, start, target, peelLiftPx, plungeLeadPx)
                        val fade = sin(((lu - 0.04f) / 0.86f).coerceIn(0f, 1f) * PI.toFloat())
                        drawMicroWaterBead(
                            center = Offset(pos.x + trailOffX[i].dp.toPx(), pos.y),
                            radiusPx = trailRadiiDp[i].dp.toPx(),
                            alpha = 0.90f * fade,
                            accentTint = accent,
                            isGlass = isGlass,
                            isComic = isComic,
                        )
                    }
                }
            }

            // 4) Close-contact concave cubic-Bezier liquid bridge between descending droplet & MiniPlayer crest
            if (!isImpactTriggered && bridgeStrength > 0.04f && dropAlpha > 0.02f) {
                val baseY = target.y - controller.crownHeightPx * 0.72f
                val gap = baseY - sample.y
                if (gap in 2f..bridgeMaxGapPx) {
                    val neckW = dropRadiusPx * (0.48f + 0.36f * bridgeStrength)
                    val baseW = neckW * 1.65f
                    val waistW = neckW * (0.44f + 0.34f * bridgeStrength)
                    val waistY = (sample.y + baseY) * 0.52f
                    val topConnectY = sample.y + dropRadiusPx * 0.20f
                    val topX = sample.x
                    val baseX = if (miniRect != null && miniRect.width > 20f) {
                        miniRect.left + controller.impactXFraction * miniRect.width
                    } else {
                        target.x
                    }
                    val waistX = (topX + baseX) * 0.5f

                    val bridgePath = Path().apply {
                        moveTo(baseX - baseW, baseY)
                        cubicTo(baseX - waistW, baseY - 4f, waistX - waistW * 0.85f, waistY, topX - neckW * 0.82f, topConnectY)
                        lineTo(topX + neckW * 0.82f, topConnectY)
                        cubicTo(waistX + waistW * 0.85f, waistY, baseX + waistW, baseY - 4f, baseX + baseW, baseY)
                        close()
                    }
                    val bridgeTopColor = when {
                        isComic -> accent
                        isGlass -> Color(0xFF8BE2FF)
                        else -> accent
                    }
                    val bridgeMidColor = when {
                        isComic -> accent.copy(alpha = 0.55f * bridgeStrength)
                        isDark -> Color(0xFF568CA4).copy(alpha = 0.42f * bridgeStrength)
                        else -> Color(0xFFAAD8EB).copy(alpha = 0.45f * bridgeStrength)
                    }
                    drawPath(
                        path = bridgePath,
                        brush = Brush.verticalGradient(
                            0.00f to bridgeTopColor.copy(alpha = 0.62f * bridgeStrength),
                            0.55f to bridgeMidColor,
                            1.00f to Color.Transparent,
                            startY = topConnectY,
                            endY = baseY + 2f,
                        ),
                    )
                    val leftNeck = Path().apply {
                        moveTo(baseX - baseW, baseY)
                        cubicTo(baseX - waistW, baseY - 4f, waistX - waistW * 0.85f, waistY, topX - neckW * 0.82f, topConnectY)
                    }
                    val rightNeck = Path().apply {
                        moveTo(baseX + baseW, baseY)
                        cubicTo(baseX + waistW, baseY - 4f, waistX + waistW * 0.85f, waistY, topX + neckW * 0.82f, topConnectY)
                    }
                    val neckPeakColor = if (isComic) ComicInk.copy(alpha = 0.88f * bridgeStrength) else Color.White.copy(alpha = 0.82f * bridgeStrength)
                    val neckBrush = Brush.verticalGradient(
                        0.00f to neckPeakColor,
                        0.72f to neckPeakColor.copy(alpha = neckPeakColor.alpha * 0.45f),
                        1.00f to Color.Transparent,
                        startY = topConnectY,
                        endY = baseY,
                    )
                    drawPath(leftNeck, brush = neckBrush, style = Stroke(width = 1.6.dp.toPx()))
                    drawPath(rightNeck, brush = neckBrush, style = Stroke(width = 1.6.dp.toPx()))
                }
            }

            // 5) Upgraded Crystal Crown Splash: 7 gravity-parabola water droplets + 2 micro shimmer sparks above gunung crest
            if (isImpactTriggered && splashAnim.value in 0.005f..0.995f) {
                val st = splashAnim.value
                val crestCenterY = target.y - 2.dp.toPx()
                for (p in CRYSTAL_SPLASH_SPECS) {
                    val localT = (st - p.delay) / p.dur
                    if (localT <= 0.01f || localT >= 1.0f) continue
                    val px = target.x + p.vxDp.dp.toPx() * localT
                    val py = crestCenterY - (p.vyDp * localT - 0.5f * p.gDp * localT * localT).dp.toPx()
                    val vyNow = -(p.vyDp - p.gDp * localT)
                    val angDeg = (atan2(vyNow, p.vxDp) * 180f / PI.toFloat()) + 90f
                    val fade = ((1f - localT).pow(1.45f)) * (localT * 10f).coerceAtMost(1f)
                    val rPx = lerp(p.r0Dp.dp.toPx(), p.r1Dp.dp.toPx(), localT)
                    if (p.isTeardrop) {
                        drawSplashTeardrop(
                            center = Offset(px, py),
                            radiusPx = rPx,
                            angleDeg = angDeg,
                            alpha = fade,
                            accentTint = accent,
                            isGlass = isGlass,
                            isComic = isComic,
                        )
                    } else {
                        drawMicroWaterBead(
                            center = Offset(px, py),
                            radiusPx = rPx,
                            alpha = fade,
                            accentTint = accent,
                            isGlass = isGlass,
                            isComic = isComic,
                        )
                    }
                }
            }
        }

        // Foreground 3D Living Water Globule morphing smoothly from rounded cover -> liquid water droplet
        if (dropAlpha > 0.02f) {
            val flightEnvelope = if (u < 0.02f) {
                0f
            } else {
                sin((u * 1.25f).coerceAtMost(1f) * PI.toFloat() * 0.85f)
            }
            val phaseRad = u * 9.8f
            var raw = (sample.angleRad * 180f / PI.toFloat()) - 90f
            while (raw > 180f) raw -= 360f
            while (raw < -180f) raw += 360f
            val angleDeg = raw * (u / 0.16f).coerceIn(0f, 1f)

            val dropletShape = LivingWaterDropletShape(
                bellyRadiusPx = dropRadiusPx,
                stretch = flightEnvelope,
                phaseRad = phaseRad,
                cornerBlend = cornerBlend,
                squashX = squashX,
                squashY = squashY,
            )
            val pivotOrigin = TransformOrigin(0.5f, 0.62f)
            val sway = sin(phaseRad)

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (sample.x - boxWidthPx * 0.5f).roundToInt(),
                            (sample.y - boxHeightPx * 0.62f).roundToInt(),
                        )
                    }
                    .size(boxWidthDp, boxHeightDp)
                    .graphicsLayer {
                        alpha = dropAlpha
                        transformOrigin = pivotOrigin
                        rotationZ = angleDeg
                    },
            ) {
                // Soft floating drop shadow & aura matching the live organic water contour
                Canvas(Modifier.fillMaxSize()) {
                    val cx = size.width * 0.5f
                    val cy = size.height * 0.62f
                    val dropletPath = buildLivingWaterDropletPath(
                        cx = cx,
                        cy = cy,
                        radius = dropRadiusPx + 1.5.dp.toPx(),
                        stretch = flightEnvelope,
                        phaseRad = phaseRad,
                        cornerBlend = cornerBlend,
                        squashX = squashX,
                        squashY = squashY,
                    )
                    translate(2.5.dp.toPx(), 6.dp.toPx()) {
                        drawPath(path = dropletPath, color = Color.Black.copy(alpha = 0.38f))
                    }
                    if (!isComic) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(accent.copy(alpha = 0.28f * waterMix), Color.Transparent),
                                center = Offset(cx, cy),
                                radius = (dropRadiusPx + 9.dp.toPx()).coerceAtLeast(1f),
                            ),
                            radius = dropRadiusPx + 9.dp.toPx(),
                            center = Offset(cx, cy),
                        )
                    }
                }

                // Upright artwork cleanly clipped inside the living capillary water contour
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(dropletShape),
                ) {
                    ArtworkImage(
                        name = drop.title,
                        artist = drop.artist,
                        embeddedUrl = drop.artworkUrl,
                        fallbackIcon = Icons.Filled.MusicNote,
                        crossfadeMs = 0,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                transformOrigin = pivotOrigin
                                rotationZ = -angleDeg
                            },
                    )
                    // 3D Water refraction optics & strokes anchored to the belly and living contour
                    Canvas(Modifier.fillMaxSize()) {
                        val cx = size.width * 0.5f
                        val cy = size.height * 0.62f
                        val r = dropRadiusPx
                        if (r <= 1f) return@Canvas
                        val dropletPath = buildLivingWaterDropletPath(
                            cx = cx,
                            cy = cy,
                            radius = r,
                            stretch = flightEnvelope,
                            phaseRad = phaseRad,
                            cornerBlend = cornerBlend,
                            squashX = squashX,
                            squashY = squashY,
                        )
                        if (isComic) {
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.42f * waterMix), Color.Transparent),
                                    startY = (cy - r * 1.6f).coerceAtLeast(0f),
                                    endY = cy,
                                ),
                            )
                            drawPath(
                                path = dropletPath,
                                color = ComicInk,
                                style = Stroke(width = 2.2.dp.toPx()),
                            )
                            return@Canvas
                        }

                        // Subtle wet aqua depth gradient
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFD2F5FF).copy(alpha = 0.18f * waterMix),
                                    Color.Transparent,
                                    Color(0xFF0A1626).copy(alpha = 0.28f * waterMix),
                                ),
                                start = Offset(cx - r, cy - r * 1.4f),
                                end = Offset(cx + r, cy + r),
                            ),
                        )

                        // Glowing lower internal caustic crescent
                        drawOval(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFFE1FCFF).copy(alpha = 0.68f * waterMix)),
                                startY = cy + r * 0.08f,
                                endY = cy + r,
                            ),
                            topLeft = Offset(cx - r * 0.62f, cy + r * 0.18f),
                            size = Size(r * 1.24f, r * 0.68f),
                        )

                        // Curved upper specular meniscus dome with subtle capillary shimmer
                        drawOval(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.85f * waterMix), Color.Transparent),
                                startY = cy - r * 0.84f,
                                endY = cy,
                            ),
                            topLeft = Offset(cx - r * 0.58f + sway * r * 0.06f, cy - r * 0.82f),
                            size = Size(r * 1.12f, r * 0.62f),
                        )

                        // Pinpoint studio reflections
                        drawOval(
                            color = Color.White.copy(alpha = 0.96f * waterMix),
                            topLeft = Offset(cx - r * 0.42f + sway * r * 0.05f, cy - r * 0.58f),
                            size = Size(r * 0.26f, r * 0.16f),
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.92f * waterMix),
                            radius = r * 0.06f,
                            center = Offset(cx - r * 0.12f + sway * r * 0.04f, cy - r * 0.64f),
                        )

                        // Dark Fresnel edge + cyan chromatic dispersion rim + crisp white outer specular contour
                        drawPath(
                            path = dropletPath,
                            color = Color(0xFF050E18).copy(alpha = 0.34f),
                            style = Stroke(width = 3.dp.toPx()),
                        )
                        if (isGlass) {
                            drawPath(
                                path = dropletPath,
                                color = Color(0xFF82F2FF).copy(alpha = 0.55f * waterMix),
                                style = Stroke(width = 2.2.dp.toPx()),
                            )
                        }
                        drawPath(
                            path = dropletPath,
                            color = Color.White.copy(alpha = (0.90f - 0.28f * cornerBlend)),
                            style = Stroke(width = 1.4.dp.toPx()),
                        )
                    }
                }
            }
        }
    }
}

private fun buildLivingWaterDropletPath(
    cx: Float,
    cy: Float,
    radius: Float,
    stretch: Float,
    phaseRad: Float,
    cornerBlend: Float = 0f,
    squashX: Float = 1f,
    squashY: Float = 1f,
): Path {
    val count = 44
    val pts = Array(count) { Offset.Zero }
    val step = (2.0 * PI / count).toFloat()
    val pExp = 4.2f
    val sqR = radius * 0.92f
    for (i in 0 until count) {
        val a = i * step
        val sinA = sin(a)
        val cosA = cos(a)
        val upFactor = (-sinA).coerceAtLeast(0f)
        val tailPull = 0.88f * stretch * upFactor.pow(2.4f)
        val oblate = 0.09f * stretch * cos(2f * a)
        val wobble = (0.08f + 0.07f * stretch) * (
            0.65f * sin(2f * a + phaseRad * 2.2f) +
                0.35f * sin(3f * a - phaseRad * 3.1f)
            ) * (1f - cornerBlend * 0.75f)
        val rad = radius * (1f + oblate + tailPull + wobble)
        val pinchX = 1f - 0.34f * stretch * upFactor.pow(1.8f)
        val dx = rad * cosA * pinchX * squashX
        val dy = rad * sinA * squashY

        val absCosP = abs(cosA).pow(pExp)
        val absSinP = abs(sinA).pow(pExp)
        val sqNorm = (absCosP + absSinP).coerceAtLeast(1e-4f).pow(-1f / pExp)
        val sx = sqR * sqNorm * cosA * squashX
        val sy = sqR * sqNorm * sinA * squashY

        pts[i] = Offset(cx + lerp(dx, sx, cornerBlend), cy + lerp(dy, sy, cornerBlend))
    }
    return Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (i in 0 until count) {
            val p0 = pts[(i - 1 + count) % count]
            val p1 = pts[i]
            val p2 = pts[(i + 1) % count]
            val p3 = pts[(i + 2) % count]
            val c1x = p1.x + (p2.x - p0.x) / 6f
            val c1y = p1.y + (p2.y - p0.y) / 6f
            val c2x = p2.x - (p3.x - p1.x) / 6f
            val c2y = p2.y - (p3.y - p1.y) / 6f
            cubicTo(c1x, c1y, c2x, c2y, p2.x, p2.y)
        }
        close()
    }
}

private data class LivingWaterDropletShape(
    val bellyRadiusPx: Float,
    val stretch: Float,
    val phaseRad: Float,
    val cornerBlend: Float = 0f,
    val squashX: Float = 1f,
    val squashY: Float = 1f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = Outline.Generic(
        buildLivingWaterDropletPath(
            cx = size.width * 0.5f,
            cy = size.height * 0.62f,
            radius = bellyRadiusPx,
            stretch = stretch,
            phaseRad = phaseRad,
            cornerBlend = cornerBlend,
            squashX = squashX,
            squashY = squashY,
        ),
    )
}

private fun DrawScope.drawSplashTeardrop(
    center: Offset,
    radiusPx: Float,
    angleDeg: Float,
    alpha: Float,
    accentTint: Color,
    isGlass: Boolean,
    isComic: Boolean,
) {
    if (radiusPx < 1f || alpha <= 0.02f) return
    val a = alpha.coerceIn(0f, 1f)
    val tearPath = Path().apply {
        moveTo(center.x, center.y - radiusPx * 1.95f)
        cubicTo(
            center.x + radiusPx * 0.82f,
            center.y - radiusPx * 0.65f,
            center.x + radiusPx * 1.08f,
            center.y + radiusPx * 0.28f,
            center.x,
            center.y + radiusPx * 1.05f,
        )
        cubicTo(
            center.x - radiusPx * 1.08f,
            center.y + radiusPx * 0.28f,
            center.x - radiusPx * 0.82f,
            center.y - radiusPx * 0.65f,
            center.x,
            center.y - radiusPx * 1.95f,
        )
        close()
    }
    rotate(degrees = angleDeg, pivot = center) {
        translate(0.8.dp.toPx(), 1.8.dp.toPx()) {
            drawPath(path = tearPath, color = Color.Black.copy(alpha = 0.28f * a))
        }
        val fillColor = if (isComic) accentTint else if (isGlass) Color(0xFF7DE8FF) else accentTint
        drawPath(path = tearPath, color = fillColor.copy(alpha = if (isComic) a else 0.56f * a))
        drawPath(
            path = tearPath,
            color = (if (isComic) ComicInk else Color.White).copy(alpha = 0.94f * a),
            style = Stroke(width = 1.15.dp.toPx().coerceAtLeast(1f)),
        )
        drawOval(
            color = Color.White.copy(alpha = 0.92f * a),
            topLeft = Offset(center.x - radiusPx * 0.52f, center.y - radiusPx * 0.56f),
            size = Size(radiusPx * 0.56f, radiusPx * 0.84f),
        )
    }
}

private fun DrawScope.drawMicroWaterBead(
    center: Offset,
    radiusPx: Float,
    alpha: Float,
    accentTint: Color,
    isGlass: Boolean,
    isComic: Boolean,
) {
    if (radiusPx < 1f || alpha <= 0.02f) return
    val a = alpha.coerceIn(0f, 1f)
    drawCircle(
        color = Color.Black.copy(alpha = 0.35f * a),
        radius = radiusPx,
        center = Offset(center.x + 1.dp.toPx(), center.y + 2.dp.toPx()),
    )
    if (isComic) {
        drawCircle(color = accentTint.copy(alpha = a), radius = radiusPx, center = center)
        drawCircle(color = ComicInk.copy(alpha = a), radius = radiusPx, center = center, style = Stroke(width = 1.3.dp.toPx()))
        return
    }
    drawCircle(
        color = (if (isGlass) Color(0xFFA8EDFF) else accentTint).copy(alpha = 0.36f * a),
        radius = radiusPx,
        center = center,
    )
    drawOval(
        color = Color(0xFFCEFAFF).copy(alpha = 0.52f * a),
        topLeft = Offset(center.x - radiusPx * 0.45f, center.y + radiusPx * 0.18f),
        size = Size(radiusPx * 0.92f, radiusPx * 0.44f),
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.85f * a),
        radius = radiusPx,
        center = center,
        style = Stroke(width = 1.dp.toPx().coerceAtLeast(1f)),
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.95f * a),
        radius = (radiusPx * 0.28f).coerceAtLeast(1f),
        center = Offset(center.x - radiusPx * 0.45f, center.y - radiusPx * 0.45f),
    )
}
