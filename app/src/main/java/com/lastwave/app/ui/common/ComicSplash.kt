package com.lastwave.app.ui.common

import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.lastwave.app.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private const val SPLASH_MS = 4000

// 4-color brand palette matching the CW emblem, Globe, Equalizer Bars & ClaudeWaves wordmark
private val PortalCyan = Color(0xFF0ECAE8)
private val PortalCobalt = Color(0xFF267CFF)
private val PortalViolet = Color(0xFFB05CFF)
private val PortalRose = Color(0xFFFA4C6E)

private val IconBlueGlow = PortalCobalt
private val IconVioletGlow = PortalViolet
private val MistCenter = Color(0xFFCCD6EC)
private val MistEdge = Color(0xFFA8B8D6)
private val MidnightTop = Color(0xFF05070D)
private val MidnightMid = Color(0xFF0A0E1C)
private val MidnightBot = Color(0xFF070913)
private val AuroraCobalt = PortalCobalt
private val AuroraAmethyst = PortalViolet
private val InkDark = Color(0xFF0A0F20)
private val SubSilver = Color(0xFFB0BEE0)

private const val WORDMARK = "ClaudeWaves"
private const val SUBTITLE = "MUSIC STREAMING"

internal fun splashSpan(edge0: Float, edge1: Float, x: Float): Float =
    ((x - edge0) / max(1e-6f, edge1 - edge0)).coerceIn(0f, 1f)

internal fun splashSmoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = splashSpan(edge0, edge1, x)
    return t * t * (3f - 2f * t)
}

internal fun splashSmootherstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = splashSpan(edge0, edge1, x)
    return t * t * t * (t * (t * 6f - 15f) + 10f)
}

internal fun splashEaseOutCubic(edge0: Float, edge1: Float, x: Float): Float {
    val t = splashSpan(edge0, edge1, x)
    val u = 1f - t
    return 1f - u * u * u
}

internal fun splashEaseInCubic(edge0: Float, edge1: Float, x: Float): Float {
    val t = splashSpan(edge0, edge1, x)
    return t * t * t
}

internal fun splashEaseOutBackSoft(edge0: Float, edge1: Float, x: Float, overshoot: Float = 0.90f): Float {
    val t = splashSpan(edge0, edge1, x)
    val u = t - 1f
    return 1f + (overshoot + 1f) * u * u * u + overshoot * u * u
}

internal fun splashLerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/** Normalized radius multiplier (`0f..1.1f`) for the central Earth + Music orb over progress [p]. */
internal fun splashEarthOrbRadiusScale(p: Float): Float = when {
    p < 0.045f || p > 0.385f -> 0f
    p < 0.300f -> splashEaseOutBackSoft(0.045f, 0.125f, p, overshoot = 0.90f)
    else -> 1f - splashSmootherstep(0.300f, 0.382f, p)
}

/** Envelope (`0f..1f`) controlling when the studio headphones and floating music notes rest over the Earth globe. */
internal fun splashHeadphonesEnvelope(p: Float): Float =
    (splashEaseOutBackSoft(0.068f, 0.125f, p, overshoot = 0.85f) *
        (1f - splashSmootherstep(0.268f, 0.315f, p))).coerceIn(0f, 1.15f)

/** Normalized Reverse-XOR organic aurora wave progression (`0f` = light Slate Mist, `1f` = full Midnight Carbon Aurora). */
internal fun splashAuroraBlobProgress(p: Float): Float =
    if (p < 0.635f) 0f else if (p >= 0.790f) 1f else splashSmootherstep(0.635f, 0.790f, p)

private data class ContinentSpec(
    val lonCenter: Float,
    val latCenter: Float,
    val sx: Float,
    val sy: Float,
    val seed: Float,
    val color: Color,
)

private data class EqBarSpec(
    val cwXOff: Float,
    val cwYOff: Float,
    val cwHalfH: Float,
    val eqFreq: Float,
    val colTop: Color,
    val colMid: Color,
    val colBot: Color,
)

private data class RibbonSpec(
    val startP: Float,
    val endP: Float,
    val glyphIdx: Int,
    val arcY: Float,
    val thick: Float,
    val color: Color,
)

private val Continents = listOf(
    ContinentSpec(-0.95f, -0.28f, 0.25f, 0.24f, 0.6f, PortalCyan),
    ContinentSpec(-0.68f, 0.34f, 0.18f, 0.24f, 1.4f, PortalRose),
    ContinentSpec(0.28f, -0.30f, 0.28f, 0.18f, 2.1f, PortalCobalt),
    ContinentSpec(0.38f, 0.24f, 0.22f, 0.26f, 0.9f, PortalRose),
    ContinentSpec(1.55f, -0.22f, 0.38f, 0.22f, 1.7f, PortalViolet),
    ContinentSpec(1.92f, 0.38f, 0.18f, 0.13f, 3.0f, PortalRose),
    ContinentSpec(2.85f, -0.15f, 0.24f, 0.16f, 2.6f, PortalCyan),
)

// 10 Equalizer Pulse Bars pixel-locked to every vertical capsule column of ic_splash_logo.png
private val EqBars = listOf(
    EqBarSpec(-61.5f, 3.3f, 26.1f, 1.1f, Color(0xFF2AB9EE), Color(0xFF26B6EC), Color(0xFF38A8E4)),
    EqBarSpec(-41.3f, 0.4f, 53.5f, 1.4f, Color(0xFF2D94FC), Color(0xFF3AA2EA), Color(0xFFDA669A)),
    EqBarSpec(-19.4f, -52.5f, 20.0f, 2.0f, Color(0xFF3080FF), Color(0xFF3886FC), Color(0xFF448CF6)),
    EqBarSpec(-19.4f, 52.5f, 20.0f, 1.8f, Color(0xFFD66A9E), Color(0xFFEE5C86), Color(0xFFF6567A)),
    EqBarSpec(5.1f, -66.0f, 17.5f, 2.3f, Color(0xFF387AFF), Color(0xFF427CFF), Color(0xFF4E7EFC)),
    EqBarSpec(5.1f, 66.0f, 17.0f, 1.7f, Color(0xFFEA5E8A), Color(0xFFF4587E), Color(0xFFF85476)),
    EqBarSpec(29.5f, -53.5f, 19.0f, 2.1f, Color(0xFF6C70FF), Color(0xFF7670FF), Color(0xFF8070FC)),
    EqBarSpec(29.5f, 53.5f, 19.0f, 1.9f, Color(0xFFD062C4), Color(0xFFD860BA), Color(0xFFE05EAE)),
    EqBarSpec(51.8f, -48.5f, 11.4f, 1.4f, Color(0xFF946CFF), Color(0xFF9A6CFF), Color(0xFFA26AFC)),
    EqBarSpec(51.8f, 48.5f, 11.4f, 1.5f, Color(0xFFC664D8), Color(0xFFCE62D0), Color(0xFFD660C6)),
)

private val Ribbons = listOf(
    RibbonSpec(0.510f, 0.615f, 0, -40f, 6.4f, PortalCyan),
    RibbonSpec(0.538f, 0.645f, 2, -36f, 5.6f, Color(0xFF18A6F4)),
    RibbonSpec(0.565f, 0.672f, 4, -44f, 5.8f, PortalCobalt),
    RibbonSpec(0.592f, 0.702f, 6, -48f, 6.6f, PortalCobalt),
    RibbonSpec(0.625f, 0.735f, 8, -38f, 5.6f, PortalViolet),
    RibbonSpec(0.658f, 0.768f, 10, -34f, 5.2f, PortalRose),
    RibbonSpec(0.690f, 0.785f, 10, 28f, 4.6f, PortalRose),
)

private fun resolveInterTypeface(context: Context, weight: Int): Typeface {
    val fallback = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    return runCatching {
        val base = ResourcesCompat.getFont(context, R.font.inter) ?: fallback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Typeface.create(base, weight.coerceIn(100, 900), false)
        } else {
            Typeface.create(base, Typeface.BOLD)
        }
    }.getOrDefault(fallback)
}

private class SplashDrawScratch(context: Context) {
    val blobPath = Path()
    val irisPath = Path()
    val sphereClipPath = Path()
    val workPath = Path()
    val auxPath = Path()
    val cwClipPath = Path().apply { fillType = PathFillType.EvenOdd }
    val notePath = Path()
    val wordPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = resolveInterTypeface(context, 850)
        textSkewX = -0.165f
    }
    val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = resolveInterTypeface(context, 620)
        color = SubSilver.toArgb()
    }
    val glyphWidths = FloatArray(WORDMARK.length)
    val glyphOffsets = FloatArray(WORDMARK.length)
    var cachedWordUnit = -1f
    var totalWordWidth = 0f
    var cachedWavesShaderX0 = Float.NaN
    var wavesShader: LinearGradient? = null
    var cachedSubShaderX0 = Float.NaN
    var subShader: LinearGradient? = null

    fun ensureWordMetrics(unit: Float) {
        if (abs(cachedWordUnit - unit) < 0.01f) return
        cachedWordUnit = unit
        wordPaint.textSize = 72f * unit
        var x = 0f
        for (i in WORDMARK.indices) {
            val ch = WORDMARK[i]
            val adv = wordPaint.measureText(ch.toString())
            glyphWidths[i] = adv
            glyphOffsets[i] = x
            val kern = if (ch == 'e' && i == 5) 1.5f * unit else -2.0f * unit
            x += adv + kern
        }
        totalWordWidth = x + 2f * unit
    }

    fun getWavesGradient(startX: Float): LinearGradient {
        val wavesX0 = startX + glyphOffsets[6]
        if (abs(cachedWavesShaderX0 - wavesX0) < 0.5f && wavesShader != null) {
            return wavesShader!!
        }
        cachedWavesShaderX0 = wavesX0
        val wavesX1 = startX + totalWordWidth
        return LinearGradient(
            wavesX0,
            0f,
            wavesX1,
            0f,
            intArrayOf(
                PortalCyan.toArgb(),
                PortalCobalt.toArgb(),
                PortalViolet.toArgb(),
                PortalRose.toArgb(),
            ),
            floatArrayOf(0f, 0.34f, 0.68f, 1f),
            Shader.TileMode.CLAMP,
        ).also { wavesShader = it }
    }

    fun getSubtitleGradient(x0: Float, x1: Float): LinearGradient {
        if (abs(cachedSubShaderX0 - x0) < 0.5f && subShader != null) {
            return subShader!!
        }
        cachedSubShaderX0 = x0
        return LinearGradient(
            x0,
            0f,
            x1,
            0f,
            intArrayOf(
                0xFF82E8F8.toInt(),
                0xFF8AB4FF.toInt(),
                0xFFCCA0FF.toInt(),
                0xFFFF98AE.toInt(),
            ),
            floatArrayOf(0f, 0.34f, 0.68f, 1f),
            Shader.TileMode.CLAMP,
        ).also { subShader = it }
    }
}

/**
 * Full-screen launch splash (`4.0s` natural pacing) matching the Android system splash at `p = 0`,
 * opening seamlessly from the indigo launcher palette into a calm Slate-Titanium Mist featuring a
 * rotating Earth + Studio Headphones + Music Notes orb, pulsing Equalizer Capsule Bars directly into
 * the `CW` waveform emblem, and revealing `ClaudeWaves` across the Reverse-XOR Midnight Carbon Aurora.
 */
@Composable
fun ComicSplash(
    readyToAnimate: Boolean = true,
    onDone: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val latestReady by rememberUpdatedState(readyToAnimate)
    LaunchedEffect(Unit) {
        if (!latestReady) {
            withTimeoutOrNull(150L) {
                snapshotFlow { latestReady }.first { it }
            }
        }
        progress.animateTo(1f, tween(SPLASH_MS, easing = LinearEasing))
        onDone()
    }

    val p = progress.value
    val context = LocalContext.current
    val scratch = remember(context) { SplashDrawScratch(context) }
    val logoBitmap = ImageBitmap.imageResource(R.drawable.ic_splash_logo)
    val baseSplashColor = colorResource(R.color.splash_background)

    val exitT = splashSmootherstep(0.945f, 1.000f, p)
    val exitZoom = splashLerp(1f, 1.055f, exitT)

    // Keep system splash mark pixel-locked at 288dp on frame 0 so WindowSplash -> Compose has 0 pop.
    val openingCwAlpha = 1f - splashSmoothstep(0.018f, 0.072f, p)
    val openingCwScale = splashLerp(1f, 0.24f, splashEaseInCubic(0f, 0.075f, p))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MidnightTop)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDone,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = 1f - exitT
                    scaleX = exitZoom
                    scaleY = exitZoom
                },
        ) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f
            val cy = h * 0.49f
            val unit = min(w, h * 0.56f) / 720f

            val blobP = splashAuroraBlobProgress(p)
            when {
                blobP <= 0f -> {
                    drawLightPolarityStage(
                        p = p,
                        cx = cx,
                        cy = cy,
                        unit = unit,
                        baseSplashColor = baseSplashColor,
                        logoBitmap = logoBitmap,
                        scratch = scratch,
                    )
                }
                blobP >= 1f -> {
                    drawDarkAuroraStage(
                        p = p,
                        cx = cx,
                        cy = cy,
                        unit = unit,
                        logoBitmap = logoBitmap,
                        scratch = scratch,
                    )
                }
                else -> {
                    buildOrganicBlobPath(scratch.blobPath, w, h, cx, blobP)
                    clipPath(scratch.blobPath, ClipOp.Difference) {
                        drawLightPolarityStage(
                            p = p,
                            cx = cx,
                            cy = cy,
                            unit = unit,
                            baseSplashColor = baseSplashColor,
                            logoBitmap = logoBitmap,
                            scratch = scratch,
                        )
                    }
                    clipPath(scratch.blobPath, ClipOp.Intersect) {
                        drawDarkAuroraStage(
                            p = p,
                            cx = cx,
                            cy = cy,
                            unit = unit,
                            logoBitmap = logoBitmap,
                            scratch = scratch,
                        )
                    }
                }
            }

            if (p >= 0.760f) {
                drawSubtitle(p = p, cx = cx, cy = cy, unit = unit, scratch = scratch)
            }
        }

        if (openingCwAlpha > 0.01f && p < 0.080f) {
            Image(
                painter = painterResource(R.drawable.ic_splash_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(288.dp)
                    .graphicsLayer {
                        alpha = openingCwAlpha
                        scaleX = openingCwScale
                        scaleY = openingCwScale
                        translationY = splashLerp(0f, -size.height * 0.01f, splashSmoothstep(0.012f, 0.072f, p))
                    },
            )
        }
    }
}

private fun DrawScope.drawLightPolarityStage(
    p: Float,
    cx: Float,
    cy: Float,
    unit: Float,
    baseSplashColor: Color,
    logoBitmap: ImageBitmap,
    scratch: SplashDrawScratch,
) {
    val w = size.width
    val h = size.height

    val drawMistBase: DrawScope.() -> Unit = {
        drawRect(color = MistEdge)
        drawRect(
            brush = Brush.radialGradient(
                0f to MistCenter,
                0.68f to MistCenter.copy(alpha = 0.55f),
                1f to Color.Transparent,
                center = Offset(cx, cy),
                radius = 560f * unit,
            ),
        )
    }

    if (p < 0.092f) {
        drawRect(color = baseSplashColor)
        val iconWashAlpha = splashSmoothstep(0.002f, 0.040f, p)
        if (iconWashAlpha > 0.005f) {
            drawCircle(
                brush = Brush.radialGradient(
                    0f to IconBlueGlow.copy(alpha = 0.26f * iconWashAlpha),
                    1f to Color.Transparent,
                    center = Offset(w * 0.25f, h * 0.30f),
                    radius = w * 0.65f,
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.25f, h * 0.30f),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    0f to IconVioletGlow.copy(alpha = 0.22f * iconWashAlpha),
                    1f to Color.Transparent,
                    center = Offset(w * 0.75f, h * 0.70f),
                    radius = w * 0.65f,
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.75f, h * 0.70f),
            )
        }

        val irisT = splashSmootherstep(0.012f, 0.092f, p)
        val irisR = splashLerp(0f, hypot(w, h) * 0.75f, irisT)
        if (irisR > 1f) {
            scratch.irisPath.reset()
            scratch.irisPath.addOval(Rect(cx - irisR, cy - irisR * 1.25f, cx + irisR, cy + irisR * 1.25f))
            clipPath(scratch.irisPath, ClipOp.Intersect) {
                drawMistBase()
                drawEarthMusicOrbIfVisible(p = p, cx = cx, cy = cy, unit = unit, scratch = scratch)
            }
        }
    } else {
        drawMistBase()
        drawEarthMusicOrbIfVisible(p = p, cx = cx, cy = cy, unit = unit, scratch = scratch)
    }

    drawMotionAndBrandInk(
        p = p,
        cx = cx,
        cy = cy,
        unit = unit,
        inkColor = InkDark,
        logoBitmap = logoBitmap,
        scratch = scratch,
    )
}

private fun DrawScope.drawDarkAuroraStage(
    p: Float,
    cx: Float,
    cy: Float,
    unit: Float,
    logoBitmap: ImageBitmap,
    scratch: SplashDrawScratch,
) {
    val w = size.width
    val h = size.height
    drawRect(
        brush = Brush.verticalGradient(
            0f to MidnightTop,
            0.5f to MidnightMid,
            1f to MidnightBot,
        ),
    )
    drawCircle(
        brush = Brush.radialGradient(
            0f to AuroraCobalt.copy(alpha = 0.30f),
            1f to Color.Transparent,
            center = Offset(w * 0.22f, h * 0.30f),
            radius = w * 0.72f,
        ),
        radius = w * 0.72f,
        center = Offset(w * 0.22f, h * 0.30f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            0f to AuroraAmethyst.copy(alpha = 0.28f),
            1f to Color.Transparent,
            center = Offset(w * 0.78f, h * 0.72f),
            radius = w * 0.72f,
        ),
        radius = w * 0.72f,
        center = Offset(w * 0.78f, h * 0.72f),
    )

    if (p >= 0.635f) {
        val glowAlpha = splashSmoothstep(0.635f, 0.790f, p) * 0.24f
        val glowCy = cy - 79.4f * unit
        drawCircle(
            brush = Brush.radialGradient(
                0f to PortalCobalt.copy(alpha = glowAlpha),
                1f to Color.Transparent,
                center = Offset(cx, glowCy - 36f * unit),
                radius = 185f * unit,
            ),
            radius = 185f * unit,
            center = Offset(cx, glowCy - 36f * unit),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to PortalCyan.copy(alpha = glowAlpha * 0.85f),
                1f to Color.Transparent,
                center = Offset(cx - 48f * unit, glowCy),
                radius = 165f * unit,
            ),
            radius = 165f * unit,
            center = Offset(cx - 48f * unit, glowCy),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to PortalViolet.copy(alpha = glowAlpha * 0.90f),
                1f to Color.Transparent,
                center = Offset(cx + 36f * unit, glowCy),
                radius = 175f * unit,
            ),
            radius = 175f * unit,
            center = Offset(cx + 36f * unit, glowCy),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to PortalRose.copy(alpha = glowAlpha * 0.85f),
                1f to Color.Transparent,
                center = Offset(cx, glowCy + 42f * unit),
                radius = 165f * unit,
            ),
            radius = 165f * unit,
            center = Offset(cx, glowCy + 42f * unit),
        )
    }

    drawMotionAndBrandInk(
        p = p,
        cx = cx,
        cy = cy,
        unit = unit,
        inkColor = Color.White,
        logoBitmap = logoBitmap,
        scratch = scratch,
    )
}

private fun DrawScope.drawEarthMusicOrbIfVisible(
    p: Float,
    cx: Float,
    cy: Float,
    unit: Float,
    scratch: SplashDrawScratch,
) {
    val scale = splashEarthOrbRadiusScale(p)
    val radius = 84f * unit * scale
    if (radius <= 0.5f) return

    val drift = splashSmootherstep(0.305f, 0.382f, p)
    val orbCx = cx
    val beatGroove = sin(p * 22f) * 1.15f * unit *
        splashSmoothstep(0.075f, 0.130f, p) * (1f - splashSmoothstep(0.265f, 0.315f, p))
    val orbCy = cy + splashLerp(0f, -28f * unit, drift) + beatGroove

    if (radius < 11f * unit) {
        val dotAlpha = (radius / (11f * unit)).coerceIn(0f, 1f)
        drawCircle(color = PortalCobalt.copy(alpha = dotAlpha), radius = radius, center = Offset(orbCx, orbCy))
        return
    }

    // Translucent halo ring & acoustic ripple pulses in the 4-color brand palette
    val pulse = 1f + 0.03f * sin(p * 15f)
    val haloRadius = radius * 1.29f * pulse
    drawCircle(
        color = Color(0xFFE4ECFF).copy(alpha = 0.78f),
        radius = haloRadius,
        center = Offset(orbCx, orbCy),
    )
    drawCircle(
        brush = Brush.linearGradient(
            0f to PortalCyan.copy(alpha = 0.62f),
            0.35f to PortalCobalt.copy(alpha = 0.62f),
            0.70f to PortalViolet.copy(alpha = 0.62f),
            1f to PortalRose.copy(alpha = 0.62f),
            start = Offset(orbCx - haloRadius, orbCy - haloRadius),
            end = Offset(orbCx + haloRadius, orbCy + haloRadius),
        ),
        radius = haloRadius,
        center = Offset(orbCx, orbCy),
        style = Stroke(width = max(1.6f, radius * 0.026f)),
    )

    if (p in 0.075f..0.335f) {
        val ripT = ((p - 0.075f) / 0.130f).mod(1.0f)
        val ripR = radius * (1.29f + 0.42f * ripT)
        val ripAlpha = 0.52f * (1f - ripT) * sin(ripT * PI.toFloat()) * (1f - splashSmoothstep(0.285f, 0.335f, p))
        if (ripAlpha > 0.01f) {
            val ripCol = lerpColor(PortalCyan, PortalViolet, ripT)
            drawCircle(
                color = ripCol.copy(alpha = ripAlpha),
                radius = ripR,
                center = Offset(orbCx, orbCy),
                style = Stroke(width = 1.8f * unit),
            )
        }
    }

    // Sculpted rotating 3D Earth with Midnight-Aurora ocean & 4-color continents + equatorial soundwave
    val iconLineage = (1f - splashSmoothstep(0.048f, 0.125f, p)) * 0.55f
    val oceanBase = lerpColor(Color(0xFF090F24), Color(0xFF102052), iconLineage)

    scratch.sphereClipPath.reset()
    scratch.sphereClipPath.addOval(Rect(orbCx - radius, orbCy - radius, orbCx + radius, orbCy + radius))
    clipPath(scratch.sphereClipPath, ClipOp.Intersect) {
        drawCircle(color = oceanBase, radius = radius, center = Offset(orbCx, orbCy))
        drawCircle(
            brush = Brush.radialGradient(
                0f to PortalCobalt.copy(alpha = 0.46f),
                0.65f to PortalCyan.copy(alpha = 0.22f),
                1f to Color.Transparent,
                center = Offset(orbCx - radius * 0.32f, orbCy - radius * 0.30f),
                radius = radius * 0.95f,
            ),
            radius = radius * 1.15f,
            center = Offset(orbCx, orbCy),
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to PortalViolet.copy(alpha = 0.42f),
                0.65f to PortalRose.copy(alpha = 0.22f),
                1f to Color.Transparent,
                center = Offset(orbCx + radius * 0.34f, orbCy + radius * 0.34f),
                radius = radius * 0.95f,
            ),
            radius = radius * 1.15f,
            center = Offset(orbCx, orbCy),
        )

        val spin = p * 4.6f
        val meridianW = max(1f, radius * 0.018f)
        for (idx in 0 until 4) {
            var lon = (idx * PI.toFloat() * 0.5f + spin) % (2f * PI.toFloat())
            if (lon > PI.toFloat()) lon -= 2f * PI.toFloat()
            if (abs(lon) < PI.toFloat() * 0.5f) {
                scratch.workPath.reset()
                for (k in 0..16) {
                    val lat = splashLerp(-PI.toFloat() * 0.46f, PI.toFloat() * 0.46f, k / 16f)
                    val mx = orbCx + sin(lon) * cos(lat) * radius
                    val my = orbCy + sin(lat) * radius
                    if (k == 0) scratch.workPath.moveTo(mx, my) else scratch.workPath.lineTo(mx, my)
                }
                val merCol = if (idx % 2 == 0) PortalCyan else PortalViolet
                drawPath(
                    path = scratch.workPath,
                    color = merCol.copy(alpha = 0.28f),
                    style = Stroke(width = meridianW, cap = StrokeCap.Round),
                )
            }
        }

        // Stylized rotating continents colored across the 4-portal palette
        for (cSpec in Continents) {
            var lon = (cSpec.lonCenter + spin) % (2f * PI.toFloat())
            if (lon > PI.toFloat()) lon -= 2f * PI.toFloat()
            if (abs(lon) >= PI.toFloat() * 0.56f) continue
            val px = orbCx + sin(lon) * cos(cSpec.latCenter) * radius * 0.94f
            val py = orbCy + sin(cSpec.latCenter) * radius * 0.90f
            val foreshorten = max(0.10f, cos(lon))
            scratch.workPath.reset()
            for (k in 0 until 24) {
                val a = (2f * PI.toFloat() * k) / 24f
                val rad = 1f + 0.34f * sin(2f * a + cSpec.seed) + 0.20f * cos(3f * a - 0.6f)
                val vx = px + cos(a) * radius * cSpec.sx * rad * foreshorten
                val vy = py + sin(a) * radius * cSpec.sy * rad
                if (k == 0) scratch.workPath.moveTo(vx, vy) else scratch.workPath.lineTo(vx, vy)
            }
            scratch.workPath.close()
            val contTint = lerpColor(cSpec.color, Color.White, 0.20f)
            drawPath(path = scratch.workPath, color = contTint.copy(alpha = 0.92f))
        }

        // 4-color equatorial soundwave wrapping cleanly around Earth
        val wavePhase = p * 10.5f
        scratch.workPath.reset()
        val nWave = 36
        for (dir in intArrayOf(-1, 1)) {
            val range = if (dir < 0) 0 until nWave else nWave - 1 downTo 0
            for (i in range) {
                val u = i / (nWave - 1f)
                val lon = splashLerp(-PI.toFloat() * 0.52f, PI.toFloat() * 0.52f, u)
                val sx = orbCx + sin(lon) * radius * 1.02f
                val waveVal = (sin(lon * 2.4f + wavePhase) * 0.085f * radius +
                    cos(lon * 3.8f - wavePhase * 0.8f) * 0.032f * radius) * cos(lon * 0.7f)
                val halfT = max(1f, radius * 0.034f * (0.45f + 0.55f * cos(lon)))
                val wy = orbCy + 0.04f * radius + waveVal + dir * halfT
                if (dir < 0 && i == 0) scratch.workPath.moveTo(sx, wy) else scratch.workPath.lineTo(sx, wy)
            }
        }
        scratch.workPath.close()
        drawPath(
            path = scratch.workPath,
            brush = Brush.linearGradient(
                0f to PortalCyan.copy(alpha = 0.90f),
                0.34f to PortalCobalt.copy(alpha = 0.90f),
                0.68f to PortalViolet.copy(alpha = 0.90f),
                1f to PortalRose.copy(alpha = 0.90f),
                start = Offset(orbCx - radius, orbCy),
                end = Offset(orbCx + radius, orbCy),
            ),
        )

        // Upper-left Cyan-Ice & lower-right Rose-Violet glass rim sheens
        drawArc(
            color = lerpColor(PortalCyan, Color.White, 0.45f).copy(alpha = 0.68f),
            startAngle = 190f,
            sweepAngle = 85f,
            useCenter = false,
            topLeft = Offset(orbCx - radius * 0.96f, orbCy - radius * 0.96f),
            size = Size(radius * 1.92f, radius * 1.92f),
            style = Stroke(width = max(1.5f, radius * 0.032f), cap = StrokeCap.Round),
        )
        drawArc(
            color = lerpColor(PortalRose, PortalViolet, 0.40f).copy(alpha = 0.58f),
            startAngle = 15f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = Offset(orbCx - radius * 0.96f, orbCy - radius * 0.96f),
            size = Size(radius * 1.92f, radius * 1.92f),
            style = Stroke(width = max(1.4f, radius * 0.028f), cap = StrokeCap.Round),
        )
    }

    // Over-ear Studio Headphones + Floating Music Notes + Sound Echo Arcs
    val hpEnv = splashHeadphonesEnvelope(p)
    if (hpEnv > 0.02f && radius > 9f * unit) {
        val hpClamp = hpEnv.coerceIn(0f, 1f)
        val hpScale = splashLerp(0.86f, 1f, hpClamp)
        val hpAlpha = (hpEnv * 1.25f).coerceIn(0f, 1f)
        val bandR = radius * 1.15f * hpScale
        val bandW = max(2.5f, radius * 0.105f * hpClamp)

        drawArc(
            color = InkDark.copy(alpha = hpAlpha),
            startAngle = -172f,
            sweepAngle = 164f,
            useCenter = false,
            topLeft = Offset(orbCx - bandR, orbCy - bandR * 0.98f),
            size = Size(bandR * 2f, bandR * 1.96f),
            style = Stroke(width = bandW, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawArc(
            brush = Brush.linearGradient(
                0f to PortalCyan.copy(alpha = hpAlpha * 0.92f),
                0.5f to PortalCobalt.copy(alpha = hpAlpha * 0.92f),
                1f to PortalViolet.copy(alpha = hpAlpha * 0.92f),
                start = Offset(orbCx - bandR, orbCy - bandR),
                end = Offset(orbCx + bandR, orbCy - bandR),
            ),
            startAngle = -152f,
            sweepAngle = 124f,
            useCenter = false,
            topLeft = Offset(orbCx - bandR, orbCy - bandR * 0.98f),
            size = Size(bandR * 2f, bandR * 1.96f),
            style = Stroke(width = max(1.2f, bandW * 0.32f), cap = StrokeCap.Round),
        )

        val cupW = radius * 0.25f * hpClamp
        val cupH = radius * 0.46f * hpClamp
        for (side in floatArrayOf(-1f, 1f)) {
            val ecx = orbCx + side * radius * 1.05f * hpScale
            val ecy = orbCy + radius * 0.04f
            drawRoundRect(
                color = InkDark.copy(alpha = hpAlpha),
                topLeft = Offset(ecx - cupW * 0.60f, ecy - cupH * 0.56f),
                size = Size(cupW * 1.20f, cupH * 1.12f),
                cornerRadius = CornerRadius(min(cupW, cupH) * 0.48f),
            )
            val cushX = ecx - side * cupW * 0.28f
            val cushCol = if (side < 0f) PortalCyan else PortalRose
            drawRoundRect(
                color = cushCol.copy(alpha = hpAlpha),
                topLeft = Offset(cushX - cupW * 0.22f, ecy - cupH * 0.44f),
                size = Size(cupW * 0.44f, cupH * 0.88f),
                cornerRadius = CornerRadius(max(1.5f, cupW * 0.16f)),
            )
        }

        val notesT = splashSmoothstep(0.080f, 0.130f, p) * (1f - splashSmoothstep(0.255f, 0.302f, p))
        if (notesT > 0.02f) {
            val liftL = splashSmoothstep(0.080f, 0.302f, p) * radius * 0.30f
            drawEighthNote(
                cx = orbCx - radius * 1.18f,
                cy = orbCy - radius * 0.78f - liftL,
                size = radius * 0.26f,
                color = PortalCyan.copy(alpha = 0.95f * notesT),
                scratch = scratch,
            )
            val liftR = splashSmoothstep(0.090f, 0.302f, p) * radius * 0.36f
            drawBeamedNotes(
                cx = orbCx + radius * 1.24f,
                cy = orbCy - radius * 0.88f - liftR,
                size = radius * 0.27f,
                color1 = PortalViolet.copy(alpha = 0.95f * notesT),
                color2 = PortalRose.copy(alpha = 0.95f * notesT),
                scratch = scratch,
            )

            val rip = ((p - 0.08f) * 3.8f).mod(1.0f)
            val arcAlpha = 0.62f * sin(rip * PI.toFloat()) * notesT
            if (arcAlpha > 0.02f) {
                val ar = radius * (1.36f + 0.18f * rip)
                val echoSize = Size(ar * 2f, ar * 1.76f)
                val echoTopLeft = Offset(orbCx - ar, orbCy - ar * 0.88f)
                val echoStroke = Stroke(width = max(1.3f, radius * 0.028f), cap = StrokeCap.Round)
                drawArc(PortalCyan.copy(alpha = arcAlpha), 158f, 44f, false, echoTopLeft, echoSize, style = echoStroke)
                drawArc(PortalRose.copy(alpha = arcAlpha), -22f, 44f, false, echoTopLeft, echoSize, style = echoStroke)
            }
        }
    }
}

private fun DrawScope.drawEighthNote(
    cx: Float,
    cy: Float,
    size: Float,
    color: Color,
    scratch: SplashDrawScratch,
) {
    val rx = size * 0.36f
    val ry = size * 0.27f
    val nx = cx - size * 0.16f
    val ny = cy + size * 0.30f
    drawOval(color = color, topLeft = Offset(nx - rx, ny - ry), size = Size(rx * 2f, ry * 2f))
    val sx = nx + rx * 0.72f
    val stTop = cy - size * 0.52f
    val sw = max(1.4f, size * 0.14f)
    drawLine(color = color, start = Offset(sx, ny), end = Offset(sx, stTop), strokeWidth = sw, cap = StrokeCap.Round)
    scratch.notePath.reset()
    scratch.notePath.moveTo(sx - sw * 0.3f, stTop)
    scratch.notePath.lineTo(sx + size * 0.45f, stTop + size * 0.24f)
    scratch.notePath.lineTo(sx + size * 0.52f, stTop + size * 0.52f)
    scratch.notePath.lineTo(sx + size * 0.28f, stTop + size * 0.34f)
    scratch.notePath.lineTo(sx, stTop + size * 0.18f)
    scratch.notePath.close()
    drawPath(path = scratch.notePath, color = color)
}

private fun DrawScope.drawBeamedNotes(
    cx: Float,
    cy: Float,
    size: Float,
    color1: Color,
    color2: Color,
    scratch: SplashDrawScratch,
) {
    val rx = size * 0.28f
    val ry = size * 0.21f
    val n1x = cx - size * 0.36f
    val n1y = cy + size * 0.28f
    val n2x = cx + size * 0.22f
    val n2y = cy + size * 0.18f
    drawOval(color = color1, topLeft = Offset(n1x - rx, n1y - ry), size = Size(rx * 2f, ry * 2f))
    drawOval(color = color2, topLeft = Offset(n2x - rx, n2y - ry), size = Size(rx * 2f, ry * 2f))
    val sw = max(1.3f, size * 0.12f)
    val s1x = n1x + rx * 0.70f
    val s2x = n2x + rx * 0.70f
    val t1y = cy - size * 0.42f
    val t2y = cy - size * 0.52f
    drawLine(color = color1, start = Offset(s1x, n1y), end = Offset(s1x, t1y), strokeWidth = sw, cap = StrokeCap.Round)
    drawLine(color = color2, start = Offset(s2x, n2y), end = Offset(s2x, t2y), strokeWidth = sw, cap = StrokeCap.Round)
    val beamH = size * 0.17f
    scratch.notePath.reset()
    scratch.notePath.moveTo(s1x - sw * 0.4f, t1y)
    scratch.notePath.lineTo(s2x + sw * 0.4f, t2y)
    scratch.notePath.lineTo(s2x + sw * 0.4f, t2y + beamH)
    scratch.notePath.lineTo(s1x - sw * 0.4f, t1y + beamH)
    scratch.notePath.close()
    drawPath(path = scratch.notePath, color = color1)
}

private fun DrawScope.drawMotionAndBrandInk(
    p: Float,
    cx: Float,
    cy: Float,
    unit: Float,
    inkColor: Color,
    logoBitmap: ImageBitmap,
    scratch: SplashDrawScratch,
) {
    if (p in 0.255f..0.595f) {
        drawEqualizerPulseBars(p = p, cx = cx, cy = cy, unit = unit)
    }
    if (p >= 0.470f) {
        drawCwWriteOn(p = p, cx = cx, cy = cy, unit = unit, logoBitmap = logoBitmap, scratch = scratch)
    }
    if (p >= 0.510f) {
        drawWordmarkAndRibbons(p = p, cx = cx, cy = cy, unit = unit, color = inkColor, scratch = scratch)
    }
}

private fun cwCurtainX(p: Float, cx: Float, unit: Float): Float {
    val boxSize = 485.6f * unit
    val left = cx - boxSize * 0.5f
    val tC = splashSmootherstep(0.470f, 0.595f, p)
    return splashLerp(left + 0.350f * boxSize, left + 0.630f * boxSize, tC)
}

/**
 * Pilihan B — Equalizer Pulse Bars:
 * 10 vertical rounded capsule equalizer bars pulse out of the Earth's equator, glide upward in a
 * wave cascade to `(cx, cwCy)`, lock into the exact pixel columns, heights, and vertical gradients
 * of `CW`, and hand off column-by-column as `drawCwWriteOn` sweeps left-to-right with zero overlap.
 */
private fun DrawScope.drawEqualizerPulseBars(
    p: Float,
    cx: Float,
    cy: Float,
    unit: Float,
) {
    val cwCy = cy - 79.4f * unit
    val curtainX = if (p >= 0.470f) cwCurtainX(p = p, cx = cx, unit = unit) else -1e9f

    for (idx in EqBars.indices) {
        val spec = EqBars[idx]
        val stagger = idx * 0.006f
        val bt = splashSmootherstep(0.262f + stagger, 0.465f + stagger * 0.5f, p)
        if (bt <= 0.005f) continue

        val startX = cx + (idx - 4.5f) * 12.5f * unit
        val startY = cy - sin(bt * PI.toFloat()) * 14f * unit
        val arcX = -sin(bt * PI.toFloat()) * (1f - bt) * 22f * unit
        val bx = splashLerp(startX, cx + spec.cwXOff * unit, bt) + arcX
        val by = splashLerp(startY, cwCy + spec.cwYOff * unit, bt)

        val distPast = curtainX - bx
        val barAlpha = (1f - splashSmootherstep(-8f * unit, 6f * unit, distPast)).coerceIn(0f, 1f)
        if (barAlpha <= 0.01f) continue

        val eqOsc = 0.55f + 0.65f * abs(sin(p * 24f * spec.eqFreq + idx * 0.85f))
        val curHalfH = splashLerp(20f * eqOsc, spec.cwHalfH, splashSmootherstep(0f, 1f, bt)) * unit
        val barHalfW = splashLerp(4.6f, 6.6f, bt) * unit
        val topY = by - curHalfH
        val botY = by + curHalfH
        drawRoundRect(
            brush = Brush.verticalGradient(
                0f to spec.colTop.copy(alpha = barAlpha),
                0.5f to spec.colMid.copy(alpha = barAlpha),
                1f to spec.colBot.copy(alpha = barAlpha),
                startY = topY,
                endY = botY,
            ),
            topLeft = Offset(bx - barHalfW, topY),
            size = Size(barHalfW * 2f, curHalfH * 2f),
            cornerRadius = CornerRadius(barHalfW, barHalfW),
        )
    }
}

private fun DrawScope.drawCwWriteOn(
    p: Float,
    cx: Float,
    cy: Float,
    unit: Float,
    logoBitmap: ImageBitmap,
    scratch: SplashDrawScratch,
) {
    // Active glyph inside ic_splash_logo.png is 214/576 wide -> draw full square at 485.6*unit so mark matches 180*unit
    val boxSize = 485.6f * unit
    val cwCy = cy - 79.4f * unit
    val left = cx - boxSize * 0.5f
    val top = cwCy - boxSize * 0.5f
    val dstOffset = IntOffset(left.roundToInt(), top.roundToInt())
    val dstSize = IntSize(boxSize.roundToInt().coerceAtLeast(1), boxSize.roundToInt().coerceAtLeast(1))

    // Exact connected-component bounding boxes inside ic_splash_logo.png (576x576)
    val wRect = Rect(
        left + 0.478f * boxSize,
        top + 0.438f * boxSize,
        left + 0.640f * boxSize,
        top + 0.570f * boxSize,
    )
    val lDotRect = Rect(
        left + 0.310f * boxSize,
        top + 0.478f * boxSize,
        left + 0.350f * boxSize,
        top + 0.520f * boxSize,
    )
    val rDotRect = Rect(
        left + 0.646f * boxSize,
        top + 0.476f * boxSize,
        left + 0.688f * boxSize,
        top + 0.518f * boxSize,
    )

    val tC = splashSmootherstep(0.470f, 0.595f, p)
    val tW = splashSmootherstep(0.555f, 0.648f, p)
    val tD = splashEaseOutBackSoft(0.590f, 0.668f, p, overshoot = 1.15f)

    if (tC >= 0.999f && tW >= 0.999f && abs(tD - 1f) < 0.01f && p > 0.672f) {
        drawImage(image = logoBitmap, dstOffset = dstOffset, dstSize = dstSize)
        return
    }

    // 1. Outer C ring & wave bars revealed column-by-column left-to-right in sync with cwCurtainX
    if (tC > 0f) {
        scratch.cwClipPath.reset()
        scratch.cwClipPath.addRect(Rect(left, top, left + boxSize, top + boxSize))
        scratch.cwClipPath.addRect(wRect)
        scratch.cwClipPath.addRect(lDotRect)
        scratch.cwClipPath.addRect(rDotRect)
        clipPath(scratch.cwClipPath, ClipOp.Intersect) {
            if (tC >= 0.999f) {
                drawImage(image = logoBitmap, dstOffset = dstOffset, dstSize = dstSize)
            } else {
                val xStart = left + 0.350f * boxSize
                val xEnd = left + 0.630f * boxSize
                val xCur = splashLerp(xStart, xEnd, tC)
                scratch.workPath.reset()
                scratch.workPath.addRect(Rect(xStart, top, xCur, top + boxSize))
                clipPath(scratch.workPath, ClipOp.Intersect) {
                    drawImage(image = logoBitmap, dstOffset = dstOffset, dstSize = dstSize)
                }
            }
        }
    }

    // 2. Inner W smooth rounded-capsule wipe across its exact bounding box
    if (tW >= 0.999f) {
        scratch.workPath.reset()
        scratch.workPath.addRect(wRect)
        clipPath(scratch.workPath, ClipOp.Intersect) {
            drawImage(image = logoBitmap, dstOffset = dstOffset, dstSize = dstSize)
        }
    } else if (tW > 0f) {
        val xLim = splashLerp(wRect.left, wRect.right + 8f * unit, tW)
        scratch.workPath.reset()
        scratch.workPath.addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                rect = Rect(wRect.left, wRect.top, min(wRect.right + 4f * unit, xLim), wRect.bottom),
                cornerRadius = CornerRadius(12f * unit, 12f * unit),
            ),
        )
        scratch.auxPath.reset()
        scratch.auxPath.addRect(wRect)
        clipPath(scratch.workPath, ClipOp.Intersect) {
            clipPath(scratch.auxPath, ClipOp.Intersect) {
                drawImage(image = logoBitmap, dstOffset = dstOffset, dstSize = dstSize)
            }
        }
    }

    // 3. Left & Right flanking dots pop
    if (tD > 0.01f) {
        val dScale = tD.coerceIn(0.05f, 1.15f)
        scratch.workPath.reset()
        scratch.workPath.addRect(lDotRect)
        scratch.workPath.addRect(rDotRect)
        scale(scaleX = dScale, scaleY = dScale, pivot = Offset(cx, cwCy)) {
            clipPath(scratch.workPath, ClipOp.Intersect) {
                drawImage(image = logoBitmap, dstOffset = dstOffset, dstSize = dstSize)
            }
        }
    }
}

private fun DrawScope.drawWordmarkAndRibbons(
    p: Float,
    cx: Float,
    cy: Float,
    unit: Float,
    color: Color,
    scratch: SplashDrawScratch,
) {
    scratch.ensureWordMetrics(unit)

    val wordCy = cy + 83.6f * unit
    val startX = cx - scratch.totalWordWidth * 0.5f
    val fm = scratch.wordPaint.fontMetrics
    val baselineY = wordCy - (fm.ascent + fm.descent) * 0.5f
    val topY = baselineY + fm.ascent
    val botY = baselineY + fm.descent
    val glyphH = (botY - topY).coerceAtLeast(16f * unit)
    val wavesShader = scratch.getWavesGradient(startX)

    for (i in WORDMARK.indices) {
        val t0 = 0.535f + i * 0.018f
        val gt = ((p - t0) / 0.062f).coerceIn(0f, 1f)
        if (gt <= 0f) continue

        if (i < 6) {
            scratch.wordPaint.shader = null
            val claudeCol = if (i == 5) lerpColor(color, PortalCyan, 0.22f) else color
            scratch.wordPaint.color = claudeCol.toArgb()
        } else {
            scratch.wordPaint.color = android.graphics.Color.WHITE
            scratch.wordPaint.shader = wavesShader
        }

        val gx0 = startX + scratch.glyphOffsets[i]
        val gw = scratch.glyphWidths[i].coerceAtLeast(10f * unit)
        val gEase = splashSmootherstep(0f, 1f, gt)
        val dy = (1f - splashEaseOutBackSoft(0f, 1f, gt, overshoot = 0.85f)) * 6f * unit
        val chStr = WORDMARK[i].toString()

        if (gEase >= 0.998f) {
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(chStr, gx0, baselineY - dy, scratch.wordPaint)
            }
        } else {
            val origX = splashLerp(gx0, gx0 + gw * 0.5f, gEase * 0.6f)
            val origY = if (i % 2 == 0) topY else botY
            val rad = hypot(gw, glyphH) * 1.35f * gEase
            scratch.workPath.reset()
            scratch.workPath.addOval(
                Rect(
                    origX - rad * 0.95f,
                    origY - rad * 1.05f,
                    origX + rad * 1.15f,
                    origY + rad * 1.05f,
                ),
            )
            clipPath(scratch.workPath, ClipOp.Intersect) {
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(chStr, gx0, baselineY - dy, scratch.wordPaint)
                }
            }
        }
    }
    scratch.wordPaint.shader = null

    // Tapered calligraphic motion ribbons gliding over ClaudeWaves in the 4-color palette
    for (spec in Ribbons) {
        if (p !in spec.startP..spec.endP) continue
        val u = splashSmootherstep(spec.startP, spec.endP, p)
        val idx = spec.glyphIdx.coerceIn(0, WORDMARK.lastIndex)
        val ax = startX + scratch.glyphOffsets[idx] + scratch.glyphWidths[idx] * 0.5f
        val ay = if (spec.arcY < 0f) topY else botY
        buildTaperedRibbonPath(scratch.workPath, ax, ay, spec.arcY * unit, spec.thick * unit, u, unit)
        drawPath(path = scratch.workPath, color = spec.color)
    }
}

private fun buildTaperedRibbonPath(
    path: Path,
    ax: Float,
    ay: Float,
    arcH: Float,
    thick: Float,
    u: Float,
    unit: Float,
) {
    path.reset()
    val env = sin(u * PI.toFloat()).pow(0.55f)
    val w0 = max(1.8f * unit, thick * env)
    val w1 = max(1.0f * unit, 2.4f * unit * env)
    val steps = 19
    for (dir in intArrayOf(1, -1)) {
        val range = if (dir > 0) 0..steps else steps downTo 0
        for (k in range) {
            val s = k / steps.toFloat()
            val phase = (u * 0.68f + s * 0.32f).coerceIn(0f, 1f)
            val px = ax + splashLerp(-78f, 22f, phase) * unit
            val py = ay + arcH * sin(phase * PI.toFloat())
            val prevPhase = (u * 0.68f + ((k - 1).coerceAtLeast(0) / steps.toFloat()) * 0.32f).coerceIn(0f, 1f)
            val nextPhase = (u * 0.68f + ((k + 1).coerceAtMost(steps) / steps.toFloat()) * 0.32f).coerceIn(0f, 1f)
            val dx = (nextPhase - prevPhase) * 100f * unit
            val dySeg = arcH * (sin(nextPhase * PI.toFloat()) - sin(prevPhase * PI.toFloat()))
            val ln = hypot(dx, dySeg) + 1e-4f
            val nx = (-dySeg / ln) * dir
            val ny = (dx / ln) * dir
            val r = max(0.6f * unit, splashLerp(w0, w1, s) * sin(s * PI.toFloat()).pow(0.35f))
            val vx = px + nx * r
            val vy = py + ny * r
            if (dir > 0 && k == 0) path.moveTo(vx, vy) else path.lineTo(vx, vy)
        }
    }
    path.close()
}

private fun DrawScope.drawSubtitle(
    p: Float,
    cx: Float,
    cy: Float,
    unit: Float,
    scratch: SplashDrawScratch,
) {
    val subT = splashSmootherstep(0.760f, 0.845f, p)
    if (subT <= 0.01f) return
    val subPaint = scratch.subPaint
    subPaint.textSize = 15f * unit
    subPaint.letterSpacing = 0.72f
    val width = subPaint.measureText(SUBTITLE)
    val startX = cx - width * 0.5f
    subPaint.color = android.graphics.Color.WHITE
    subPaint.shader = scratch.getSubtitleGradient(startX, startX + width)
    subPaint.alpha = (subT * 255f).roundToInt().coerceIn(0, 255)
    val subY = cy + 159.6f * unit + (1f - splashEaseOutCubic(0.760f, 0.845f, p)) * 10f * unit
    val fm = subPaint.fontMetrics
    val baseline = subY - (fm.ascent + fm.descent) * 0.5f
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawText(SUBTITLE, startX, baseline, subPaint)
    }
    subPaint.shader = null
}

private fun buildOrganicBlobPath(
    path: Path,
    w: Float,
    h: Float,
    cx: Float,
    blobT: Float,
) {
    path.reset()
    val maxReach = hypot(w, h) * 0.78f
    val rBase = splashLerp(0f, maxReach, blobT)
    val unit = min(w, h * 0.56f) / 720f
    val bx = splashLerp(cx - 28f * unit, cx, blobT)
    val by = splashLerp(h * 0.484f, h * 0.50f, blobT)
    val rot = -0.25f + 0.55f * blobT
    val steps = 72
    for (i in 0 until steps) {
        val ang = (2f * PI.toFloat() * i) / steps
        val lobe = 1f +
            0.16f * cos(2f * ang - 0.5f) +
            0.10f * sin(3f * ang + 0.8f) +
            0.04f * cos(4f * ang - 1.0f)
        val r = rBase * lobe
        val vx = bx + cos(ang + rot) * r * 1.12f
        val vy = by + sin(ang + rot) * r * 0.94f
        if (i == 0) path.moveTo(vx, vy) else path.lineTo(vx, vy)
    }
    path.close()
}
