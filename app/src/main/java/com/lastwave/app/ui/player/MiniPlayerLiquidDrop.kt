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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
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

val LocalMiniPlayerDropController = staticCompositionLocalOf<MiniPlayerDropController?> { null }

private data class ParabolaSample(
    val x: Float,
    val y: Float,
    val angleRad: Float,
    val stretch: Float,
)

private fun sampleParabolaDrop(
    t: Float,
    start: Offset,
    end: Offset,
    bulgeX: Float,
    arcLiftPx: Float,
): ParabolaSample {
    val u = t.coerceIn(0f, 1f)
    val cx = (start.x + end.x) * 0.5f + bulgeX
    val cy = minOf(start.y, end.y) - arcLiftPx
    val omt = 1f - u
    val x = omt * omt * start.x + 2f * omt * u * cx + u * u * end.x
    val y = omt * omt * start.y + 2f * omt * u * cy + u * u * end.y
    val vx = 2f * omt * (cx - start.x) + 2f * u * (end.x - cx)
    val vy = 2f * omt * (cy - start.y) + 2f * u * (end.y - cy)
    val speed = hypot(vx, vy)
    val stretch = 1f + ((speed / 2100f) * sin(u * PI.toFloat()) * 0.20f).coerceIn(0f, 0.20f)
    return ParabolaSample(x = x, y = y, angleRad = atan2(vy, vx), stretch = stretch)
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
        val popLiftPx = with(density) { 18.dp.toPx() }

        val origin = hostOrigin()
        val miniRect = miniCardBounds()
        val start = remember(drop.id, origin) {
            if (drop.startCenterInRoot == Offset.Unspecified) {
                Offset(hostWidthPx * 0.25f, hostHeightPx * 0.42f)
            } else {
                Offset(
                    x = (drop.startCenterInRoot.x - origin.x).coerceIn(28f, hostWidthPx - 28f),
                    y = (drop.startCenterInRoot.y - origin.y).coerceIn(40f, hostHeightPx - 140f),
                )
            }
        }
        val flightStart = remember(drop.id, start) {
            Offset(start.x, (start.y - popLiftPx).coerceAtLeast(36f))
        }
        val target = remember(drop.id, miniRect, hostWidthPx, hostHeightPx) {
            if (miniRect != null && miniRect.width > 20f) {
                val tx = if (drop.kind == LiquidDropKind.PLAY) {
                    (miniRect.left + thumbOffsetPx).coerceIn(miniRect.left + 24f, miniRect.right - 24f)
                } else {
                    miniRect.center.x
                }
                Offset(tx, miniRect.top + with(density) { 4.dp.toPx() })
            } else {
                Offset(hostWidthPx * 0.5f, hostHeightPx - with(density) { 104.dp.toPx() })
            }
        }
        val arcBulgeX = remember(drop.id, flightStart, target) {
            val dir = if (target.x >= flightStart.x) 1f else -1f
            dir * with(density) { 44.dp.toPx() }
        }
        val arcLiftPx = with(density) { 56.dp.toPx() }

        val popAnim = remember(drop.id) { Animatable(0f) }
        val flightAnim = remember(drop.id) { Animatable(0f) }
        val splashAnim = remember(drop.id) { Animatable(0f) }
        var bridgeStrength by remember(drop.id) { mutableFloatStateOf(0f) }
        var isImpactTriggered by remember(drop.id) { mutableStateOf(false) }

        LaunchedEffect(drop.id) {
            val frac = if (miniRect != null && miniRect.width > 20f) {
                ((target.x - miniRect.left) / miniRect.width).coerceIn(0.14f, 0.86f)
            } else if (drop.kind == LiquidDropKind.PLAY) {
                0.18f
            } else {
                0.5f
            }
            controller.impactXFraction = frac
            controller.crownHeightPx = 0f
            controller.miniBounce = 0f
            controller.miniWobbleTilt = 0f
            controller.impactRipple = 0f
            bridgeStrength = 0f

            // Stage 1 — Snappy elastic pop-out right at the song row so eyes lock onto the 3D droplet
            popAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 200,
                    easing = CubicBezierEasing(0.22f, 1.35f, 0.36f, 1.0f),
                ),
            )

            // Stage 2 — Graceful gravity arc down into the MiniPlayer pill
            val flightMs = if (drop.kind == LiquidDropKind.PLAY) 700 else 780
            flightAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = flightMs,
                    easing = CubicBezierEasing(0.26f, 0.0f, 0.78f, 0.96f),
                ),
            ) {
                val ft = value
                if (ft >= 0.56f) {
                    val ct = ((ft - 0.56f) / 0.44f).coerceIn(0f, 1f)
                    controller.crownHeightPx = maxCrownPx * sin(ct * PI.toFloat())
                }
                if (ft >= 0.68f) {
                    val bt = ((ft - 0.68f) / 0.32f).coerceIn(0f, 1f)
                    bridgeStrength = sin(bt * PI.toFloat())
                }
            }

            bridgeStrength = 0f
            isImpactTriggered = true
            haptics.performHapticFeedback(
                if (drop.kind == LiquidDropKind.QUEUE) HapticFeedbackType.Confirm else HapticFeedbackType.TextHandleMove,
            )

            // Stage 3 — Impact elastic jelly wobble, ripple shockwave & splash beads
            val tiltSide = when {
                frac < 0.42f -> -1f
                frac > 0.58f -> 1f
                else -> -0.55f
            }
            val bounceJob = launch {
                val anim = Animatable(0f)
                anim.animateTo(1f, tween(durationMillis = 680, easing = LinearEasing)) {
                    val bt = value
                    controller.miniBounce = sin(bt * 4.0f * PI.toFloat()) * exp(-2.6f * bt)
                    controller.miniWobbleTilt = tiltSide * 2.6f * sin(bt * 3.0f * PI.toFloat()) * exp(-2.8f * bt)
                    controller.crownHeightPx = (maxCrownPx * 0.55f) * sin(bt * 4.0f * PI.toFloat()) * exp(-3.2f * bt)
                    controller.impactRipple = (bt * 1.45f).coerceIn(0f, 1f)
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
                        durationMillis = 380,
                        easing = CubicBezierEasing(0.18f, 0.85f, 0.32f, 1.0f),
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

        val pt = popAnim.value
        val ft = flightAnim.value
        val sample = if (ft <= 0.001f) {
            ParabolaSample(
                x = start.x,
                y = lerp(start.y, flightStart.y, pt.coerceIn(0f, 1.15f)),
                angleRad = 0f,
                stretch = 1f,
            )
        } else {
            sampleParabolaDrop(ft, flightStart, target, arcBulgeX, arcLiftPx)
        }
        val dropRadiusPx = when {
            ft <= 0.001f -> lerp(dropRestRadiusPx * 0.64f, dropRestRadiusPx, pt)
            ft > 0.84f -> lerp(dropRestRadiusPx, dropRestRadiusPx * 0.62f, ((ft - 0.84f) / 0.16f).coerceIn(0f, 1f))
            else -> dropRestRadiusPx
        }
        val dropAlpha = if (isImpactTriggered) {
            0f
        } else if (ft > 0.91f) {
            (1f - (ft - 0.91f) / 0.09f).coerceIn(0f, 1f)
        } else {
            (pt * 2.5f).coerceIn(0f, 1f)
        }
        val dropDpSize = with(density) { (dropRadiusPx * 2f).toDp() }

        // Background Canvas: trailing crystal water beads, close-contact concave metaball bridge & impact splash beads
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1) 3 trailing shimmering water micro-beads along the arc
            if (!isImpactTriggered && ft in 0.05f..0.92f) {
                val trailLags = floatArrayOf(0.048f, 0.098f, 0.148f)
                val trailRadiiDp = floatArrayOf(7.2f, 5.0f, 3.4f)
                val trailOffX = floatArrayOf(-5f, 6f, -3f)
                for (i in trailLags.indices) {
                    val bt = ft - trailLags[i]
                    if (bt in 0.04f..0.90f) {
                        val pos = sampleParabolaDrop(bt, flightStart, target, arcBulgeX, arcLiftPx)
                        val fade = sin(((bt - 0.04f) / 0.86f).coerceIn(0f, 1f) * PI.toFloat())
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

            // 2) Close-contact concave cubic-Bezier liquid bridge between descending droplet & MiniPlayer crest
            if (!isImpactTriggered && bridgeStrength > 0.04f && dropAlpha > 0.02f) {
                val baseY = target.y - controller.crownHeightPx * 0.45f
                val gap = baseY - sample.y
                if (gap in 2f..bridgeMaxGapPx) {
                    val neckW = dropRadiusPx * (0.48f + 0.36f * bridgeStrength)
                    val baseW = neckW * 1.65f
                    val waistW = neckW * (0.44f + 0.34f * bridgeStrength)
                    val waistY = (sample.y + baseY) * 0.52f
                    val topConnectY = sample.y + dropRadiusPx * 0.20f
                    val bx = sample.x

                    val bridgePath = Path().apply {
                        moveTo(bx - baseW, baseY)
                        cubicTo(bx - waistW, baseY - 4f, bx - waistW * 0.85f, waistY, bx - neckW * 0.82f, topConnectY)
                        lineTo(bx + neckW * 0.82f, topConnectY)
                        cubicTo(bx + waistW * 0.85f, waistY, bx + waistW, baseY - 4f, bx + baseW, baseY)
                        close()
                    }
                    val bridgeTopColor = if (isComic) accent else Color(0xFF8BE2FF)
                    val bridgeBotColor = when {
                        isComic -> Color(0xFF1F2127)
                        isDark -> Color(0xFF3A404A)
                        else -> Color(0xFFDCE3EC)
                    }
                    drawPath(
                        path = bridgePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                bridgeTopColor.copy(alpha = 0.78f * bridgeStrength),
                                bridgeBotColor.copy(alpha = 0.90f * bridgeStrength),
                            ),
                            startY = topConnectY,
                            endY = baseY + 2f,
                        ),
                    )
                    val leftNeck = Path().apply {
                        moveTo(bx - baseW, baseY)
                        cubicTo(bx - waistW, baseY - 4f, bx - waistW * 0.85f, waistY, bx - neckW * 0.82f, topConnectY)
                    }
                    val rightNeck = Path().apply {
                        moveTo(bx + baseW, baseY)
                        cubicTo(bx + waistW, baseY - 4f, bx + waistW * 0.85f, waistY, bx + neckW * 0.82f, topConnectY)
                    }
                    val neckStrokeColor = if (isComic) ComicInk.copy(alpha = 0.88f * bridgeStrength) else Color.White.copy(alpha = 0.85f * bridgeStrength)
                    drawPath(leftNeck, color = neckStrokeColor, style = Stroke(width = 1.6.dp.toPx()))
                    drawPath(rightNeck, color = neckStrokeColor, style = Stroke(width = 1.6.dp.toPx()))
                }
            }

            // 3) Symmetrical splash micro-beads popping cleanly off impact
            if (isImpactTriggered && splashAnim.value in 0.01f..0.99f) {
                val st = splashAnim.value
                val fade = (1f - st) * (1f - st)
                val arcH = 36.dp.toPx() * sin(st * PI.toFloat())
                val spreadL = lerp(14.dp.toPx(), 52.dp.toPx(), st)
                val spreadR = lerp(16.dp.toPx(), 56.dp.toPx(), st)
                drawMicroWaterBead(
                    center = Offset(target.x - spreadL, target.y - arcH),
                    radiusPx = lerp(6.0.dp.toPx(), 2.2.dp.toPx(), st),
                    alpha = fade,
                    accentTint = accent,
                    isGlass = isGlass,
                    isComic = isComic,
                )
                drawMicroWaterBead(
                    center = Offset(target.x + spreadR, target.y - arcH * 0.84f),
                    radiusPx = lerp(5.2.dp.toPx(), 1.8.dp.toPx(), st),
                    alpha = fade,
                    accentTint = accent,
                    isGlass = isGlass,
                    isComic = isComic,
                )
                drawMicroWaterBead(
                    center = Offset(target.x - lerp(4.dp.toPx(), 18.dp.toPx(), st), target.y - arcH * 1.18f),
                    radiusPx = lerp(3.6.dp.toPx(), 1.2.dp.toPx(), st),
                    alpha = fade * 0.9f,
                    accentTint = accent,
                    isGlass = isGlass,
                    isComic = isComic,
                )
            }
        }

        // Foreground 3D Wet Glass Water Droplet with upright clipped album cover + water optics
        if (dropAlpha > 0.02f) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (sample.x - dropRadiusPx).roundToInt(),
                            (sample.y - dropRadiusPx).roundToInt(),
                        )
                    }
                    .size(dropDpSize)
                    .graphicsLayer {
                        alpha = dropAlpha
                        // Soft elastic velocity squash-stretch while keeping album artwork readable & upright
                        val dyFactor = absSinTilt(sample.angleRad)
                        scaleX = lerp(sample.stretch, 1f / kotlin.math.sqrt(sample.stretch), dyFactor)
                        scaleY = lerp(1f / kotlin.math.sqrt(sample.stretch), sample.stretch, dyFactor)
                    },
            ) {
                // Soft floating drop shadow & aura beneath the circular droplet
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension * 0.5f
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.38f),
                        radius = r + 1.5.dp.toPx(),
                        center = Offset(center.x + 2.5.dp.toPx(), center.y + 6.dp.toPx()),
                    )
                    if (!isComic) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(accent.copy(alpha = 0.28f), Color.Transparent),
                                center = center,
                                radius = (r + 7.dp.toPx()).coerceAtLeast(1f),
                            ),
                            radius = r + 7.dp.toPx(),
                            center = center,
                        )
                    }
                }

                // Upright artwork cleanly clipped inside the water sphere
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                ) {
                    ArtworkImage(
                        name = drop.title,
                        artist = drop.artist,
                        embeddedUrl = drop.artworkUrl,
                        fallbackIcon = Icons.Filled.MusicNote,
                        crossfadeMs = 0,
                        modifier = Modifier.fillMaxSize(),
                    )
                    // 3D Water refraction optics: internal lower caustic, upper specular dome & chromatic rim
                    Canvas(Modifier.fillMaxSize()) {
                        val r = size.minDimension * 0.5f
                        if (r <= 1f) return@Canvas
                        if (isComic) {
                            drawCircle(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.42f), Color.Transparent),
                                    startY = 0f,
                                    endY = size.height * 0.48f,
                                ),
                            )
                            drawCircle(
                                color = ComicInk,
                                radius = r - 1.dp.toPx(),
                                style = Stroke(width = 2.2.dp.toPx()),
                            )
                            return@Canvas
                        }

                        // Subtle wet aqua depth gradient
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFD2F5FF).copy(alpha = 0.16f),
                                    Color.Transparent,
                                    Color(0xFF0A1626).copy(alpha = 0.26f),
                                ),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height),
                            ),
                        )

                        // Glowing lower internal caustic crescent
                        drawOval(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFFE1FCFF).copy(alpha = 0.68f)),
                                startY = center.y + r * 0.08f,
                                endY = size.height,
                            ),
                            topLeft = Offset(center.x - r * 0.62f, center.y + r * 0.18f),
                            size = Size(r * 1.24f, r * 0.68f),
                        )

                        // Curved upper-left specular meniscus dome
                        drawOval(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
                                startY = r * 0.08f,
                                endY = center.y,
                            ),
                            topLeft = Offset(center.x - r * 0.62f, r * 0.10f),
                            size = Size(r * 1.16f, r * 0.66f),
                        )

                        // Pinpoint studio reflections
                        drawOval(
                            color = Color.White.copy(alpha = 0.96f),
                            topLeft = Offset(center.x - r * 0.44f, center.y - r * 0.56f),
                            size = Size(r * 0.26f, r * 0.16f),
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.92f),
                            radius = r * 0.06f,
                            center = Offset(center.x - r * 0.14f, center.y - r * 0.62f),
                        )

                        // Dark Fresnel edge + cyan chromatic dispersion rim + crisp white outer specular ring
                        drawCircle(
                            color = Color(0xFF050E18).copy(alpha = 0.34f),
                            radius = r - 1.5.dp.toPx(),
                            style = Stroke(width = 3.dp.toPx()),
                        )
                        if (isGlass) {
                            drawCircle(
                                color = Color(0xFF82F2FF).copy(alpha = 0.55f),
                                radius = r - 1.dp.toPx(),
                                style = Stroke(width = 2.2.dp.toPx()),
                            )
                        }
                        drawCircle(
                            color = Color.White.copy(alpha = 0.90f),
                            radius = r - 0.75.dp.toPx(),
                            style = Stroke(width = 1.4.dp.toPx()),
                        )
                    }
                }
            }
        }
    }
}

private fun absSinTilt(angleRad: Float): Float = kotlin.math.abs(sin(angleRad))

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
