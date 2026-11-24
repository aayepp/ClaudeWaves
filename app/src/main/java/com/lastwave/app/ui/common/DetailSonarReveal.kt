package com.lastwave.app.ui.common

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lastwave.app.ui.theme.ComicAccent
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

private const val LIQUID_RIPPLE_AGSL = """
uniform shader uContent;
uniform float2 uCenter;
uniform float uR0;
uniform float uR1;
uniform float uAmp0;
uniform float uAmp1;
uniform float uSigma;
uniform float uCa;
uniform float uRingAlpha0;
uniform float uRingAlpha1;
uniform float3 uCool;
uniform float3 uWarm;

half4 main(float2 fragCoord) {
    float2 delta = fragCoord - uCenter;
    float dist = length(delta);
    float2 dir = dist > 0.5 ? delta / dist : float2(0.0, 0.0);

    float s = max(uSigma, 1.0);
    float inv2S = 1.0 / (2.0 * s * s);
    float d0 = dist - uR0;
    float g0 = exp(-(d0 * d0) * inv2S);
    float slope0 = -(d0 / s) * g0;

    float d1 = dist - uR1;
    float g1 = exp(-(d1 * d1) * inv2S);
    float slope1 = -(d1 / s) * g1;

    float disp = slope0 * uAmp0 + slope1 * uAmp1;
    float ca = (g0 + g1) * uCa;

    half4 srcB = uContent.eval(fragCoord - dir * (disp + ca));
    half4 srcG = uContent.eval(fragCoord - dir * disp);
    half4 srcR = uContent.eval(fragCoord - dir * (disp - ca));

    half txtA = max(srcG.a, max(srcB.a, srcR.a));
    float ringF = clamp(g0 * uRingAlpha0 + g1 * uRingAlpha1, 0.0, 0.45);
    float prism = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);
    half3 ringCol = half3(mix(uCool, uWarm, prism));

    half ringA = half(ringF);
    half invTxtA = half(1.0) - txtA;
    half3 outRgb = half3(srcR.r, srcG.g, srcB.b) + (ringCol * ringA) * invTxtA;
    half outA = txtA + ringA * invTxtA;
    return half4(outRgb, outA);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class LiquidRippleShaderHolder {
    private val shader = RuntimeShader(LIQUID_RIPPLE_AGSL)

    fun updateAndCreateEffect(
        cx: Float,
        cy: Float,
        r0: Float,
        r1: Float,
        amp0: Float,
        amp1: Float,
        sigma: Float,
        ca: Float,
        ringAlpha0: Float,
        ringAlpha1: Float,
        cool: Color,
        warm: Color,
    ): ComposeRenderEffect? {
        return runCatching {
            shader.setFloatUniform("uCenter", cx, cy)
            shader.setFloatUniform("uR0", r0)
            shader.setFloatUniform("uR1", r1)
            shader.setFloatUniform("uAmp0", amp0)
            shader.setFloatUniform("uAmp1", amp1)
            shader.setFloatUniform("uSigma", sigma)
            shader.setFloatUniform("uCa", ca)
            shader.setFloatUniform("uRingAlpha0", ringAlpha0)
            shader.setFloatUniform("uRingAlpha1", ringAlpha1)
            shader.setFloatUniform("uCool", cool.red, cool.green, cool.blue)
            shader.setFloatUniform("uWarm", warm.red, warm.green, warm.blue)
            RenderEffect.createRuntimeShaderEffect(shader, "uContent").asComposeRenderEffect()
        }.getOrNull()
    }
}

private val HeroGlideEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

@Stable
class DetailRevealState(initiallyLoading: Boolean) {
    val revealProgress = Animatable(if (initiallyLoading) 0f else 1f)
    var rootOrigin by mutableStateOf(Offset.Zero)
    var heroTitleTopLeft by mutableStateOf<Offset?>(null)
    var heroTitleMaxWidthPx by mutableStateOf(0)
    var wasEverLoading by mutableStateOf(initiallyLoading)

    val progress: Float
        get() = revealProgress.value

    val showRealHeroTitle: Boolean
        get() = !wasEverLoading || progress >= 0.985f

    val heroArtworkAlpha: Float
        get() = if (!wasEverLoading) 1f else ((progress - 0.12f) / 0.62f).coerceIn(0f, 1f)

    val heroArtworkScale: Float
        get() = if (!wasEverLoading) 1f else 1f + 0.07f * (1f - heroArtworkAlpha)

    val contentAlpha: Float
        get() = if (!wasEverLoading) 1f else ((progress - 0.36f) / 0.56f).coerceIn(0f, 1f)
}

@Composable
fun rememberDetailRevealState(
    isLoading: Boolean,
    isReady: Boolean,
    resetKey: Any?,
): DetailRevealState {
    val state = remember(resetKey) { DetailRevealState(initiallyLoading = isLoading && !isReady) }

    LaunchedEffect(isLoading, isReady, resetKey) {
        when {
            isLoading -> {
                state.wasEverLoading = true
                state.revealProgress.snapTo(0f)
            }
            isReady -> {
                if (state.wasEverLoading) {
                    withTimeoutOrNull(120L) {
                        snapshotFlow { state.heroTitleTopLeft }.first { it != null }
                    }
                    if (state.revealProgress.value < 0.999f) {
                        state.revealProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 680, easing = HeroGlideEasing),
                        )
                    }
                } else {
                    state.revealProgress.snapTo(1f)
                }
            }
            else -> {
                state.revealProgress.snapTo(1f)
            }
        }
    }

    return state
}

fun Modifier.detailRevealRoot(state: DetailRevealState): Modifier =
    onGloballyPositioned { coords ->
        state.rootOrigin = coords.positionInWindow()
    }

fun Modifier.detailRevealHeroTitle(state: DetailRevealState): Modifier = this
    .onGloballyPositioned { coords ->
        state.heroTitleTopLeft = coords.positionInWindow() - state.rootOrigin
        state.heroTitleMaxWidthPx = coords.size.width
    }
    .graphicsLayer {
        alpha = if (state.showRealHeroTitle) 1f else 0f
    }

fun Modifier.detailRevealContent(state: DetailRevealState): Modifier =
    graphicsLayer {
        val a = state.contentAlpha
        alpha = a
        translationY = 22f * this.density * (1f - a)
    }

/**
 * Displays a centered title surrounded by concentric breathing sonar rings while
 * an Artist or Album page loads, then glides that title smoothly onto the
 * real hero header once content arrives.
 */
@Composable
fun DetailSonarRevealOverlay(
    title: String,
    subtitle: String? = null,
    isLoading: Boolean,
    isReady: Boolean,
    state: DetailRevealState,
    modifier: Modifier = Modifier,
) {
    val showOverlay by remember(isLoading, isReady, state) {
        derivedStateOf { isLoading || (state.wasEverLoading && isReady && state.progress < 0.999f) }
    }
    if (!showOverlay || title.isBlank()) return

    val onBg = MaterialTheme.colorScheme.onBackground
    val primary = MaterialTheme.colorScheme.primary
    val isDark = LocalIsDarkTheme.current
    val isComic = LocalComicTheme.current
    val comicAccent = ComicAccent
    val coolRingTint = remember(onBg, primary, isDark, isComic, comicAccent) {
        when {
            isComic -> comicAccent
            isDark -> lerpColor(Color(0xFFA5CDF5), primary, 0.22f)
            else -> lerpColor(Color(0xFF2A62B8), primary, 0.35f)
        }
    }
    val warmRingTint = remember(onBg, primary, isDark, isComic, comicAccent) {
        when {
            isComic -> comicAccent
            isDark -> lerpColor(Color(0xFFE8D0B9), primary, 0.18f)
            else -> lerpColor(Color(0xFF5873C6), primary, 0.35f)
        }
    }

    val useAgsl = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isComic
    val shaderHolder = remember(useAgsl) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && useAgsl) {
            runCatching { LiquidRippleShaderHolder() }.getOrNull()
        } else null
    }
    val agslActive = shaderHolder != null

    val infiniteTransition = rememberInfiniteTransition(label = "detailSonarPulse")
    val pulseCycleState: State<Float> = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "detailSonarCycle",
    )

    val density = LocalDensity.current
    var flyingTitleSize by remember(title) { mutableStateOf(IntSize.Zero) }
    var subtitleSize by remember(subtitle) { mutableStateOf(IntSize.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .zIndex(5f)
            .graphicsLayer {
                if (shaderHolder != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val p = if (isLoading) 0f else state.progress
                    val ringFade = if (isLoading) 1f else (1f - (p / 0.58f).coerceIn(0f, 1f))
                    val refractStrength = if (isLoading) 1f else (1f - (p / 0.38f).coerceIn(0f, 1f))
                    if (size.width <= 1f || size.height <= 1f || ringFade <= 0.005f) {
                        renderEffect = null
                    } else {
                        compositingStrategy = CompositingStrategy.Offscreen
                        val p0 = pulseCycleState.value
                        val p1 = (p0 + 0.5f) % 1f
                        val decel0 = 1f - (1f - p0).pow(1.85f)
                        val decel1 = 1f - (1f - p1).pow(1.85f)
                        val minR = 10f * this.density
                        val maxR = min(size.width, size.height) * 0.27f
                        val r0 = lerpFloat(minR, maxR, decel0)
                        val r1 = lerpFloat(minR, maxR, decel1)
                        val env0 = sin(p0 * PI).toFloat().coerceAtLeast(0f).pow(1.35f) * ringFade
                        val env1 = sin(p1 * PI).toFloat().coerceAtLeast(0f).pow(1.35f) * ringFade
                        val peakAlpha = if (isDark) 0.16f else 0.13f
                        val amp0 = lerpFloat(9.5f * this.density, 3.0f * this.density, decel0) * env0 * refractStrength
                        val amp1 = lerpFloat(9.5f * this.density, 3.0f * this.density, decel1) * env1 * refractStrength
                        val ca = (if (isDark) 1.35f else 0.65f) * this.density * refractStrength * (env0 + env1) * 0.5f
                        renderEffect = shaderHolder.updateAndCreateEffect(
                            cx = size.width / 2f,
                            cy = size.height / 2f - 18f * this.density,
                            r0 = r0,
                            r1 = r1,
                            amp0 = amp0,
                            amp1 = amp1,
                            sigma = 19f * this.density,
                            ca = ca,
                            ringAlpha0 = env0 * peakAlpha,
                            ringAlpha1 = env1 * peakAlpha,
                            cool = coolRingTint,
                            warm = warmRingTint,
                        )
                    }
                }
            },
    ) {
        if (!agslActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val p = if (isLoading) 0f else state.progress
                val ringFade = if (isLoading) 1f else (1f - (p / 0.58f).coerceIn(0f, 1f))
                if (ringFade <= 0.01f) return@Canvas
                val pulseCycle = pulseCycleState.value
                val cx = size.width / 2f
                val cy = size.height / 2f - 18.dp.toPx()
                val minRadius = 18.dp.toPx()
                val maxRadius = min(size.width, size.height) * 0.27f
                val ringCount = 2
                for (i in 0 until ringCount) {
                    val phase = (pulseCycle + i * 0.5f) % 1f
                    val decel = 1f - (1f - phase).pow(1.85f)
                    val radius = lerpFloat(minRadius, maxRadius, decel)
                    val envelope = sin(phase * PI).toFloat().coerceAtLeast(0f).pow(1.35f)
                    if (envelope <= 0.01f) continue
                    if (isComic) {
                        drawCircle(
                            color = coolRingTint.copy(alpha = 0.28f * envelope * ringFade),
                            radius = radius,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.5.dp.toPx()),
                        )
                    } else {
                        val peakAlpha = (if (isDark) 0.14f else 0.11f) * envelope * ringFade
                        val baseWidth = 14.dp.toPx()
                        val ringBrush = Brush.verticalGradient(
                            0f to coolRingTint.copy(alpha = peakAlpha * 0.42f),
                            1f to warmRingTint.copy(alpha = peakAlpha * 0.42f),
                            startY = cy - radius,
                            endY = cy + radius,
                        )
                        for (layer in 0 until 4) {
                            val layerWidth = baseWidth * (0.7f + layer * 0.55f)
                            drawCircle(
                                brush = ringBrush,
                                radius = radius,
                                center = Offset(cx, cy),
                                alpha = 1f / (1f + layer * 0.9f),
                                style = Stroke(width = layerWidth),
                            )
                        }
                    }
                }
            }
        }

        val heroWidthDp = with(density) {
            if (state.heroTitleMaxWidthPx > 0) state.heroTitleMaxWidthPx.toDp()
            else maxWidth - 64.dp
        }
        val maxContainerWidthPx = with(density) { maxWidth.toPx() }
        val maxContainerHeightPx = with(density) { maxHeight.toPx() }
        val centerOffsetYPx = with(density) { 18.dp.toPx() }
        val subtitleGapPx = with(density) { 10.dp.toPx() }

        Box(
            modifier = Modifier
                .widthIn(max = heroWidthDp)
                .onSizeChanged { flyingTitleSize = it }
                .graphicsLayer {
                    val p = if (isLoading) 0f else state.progress
                    val waveAmp = if (agslActive) 0f else (1f - p)
                    val wRad = pulseCycleState.value * 2f * PI.toFloat()
                    val swayX = cos(wRad) * 2.5f * this.density * waveAmp
                    val bobY = sin(wRad) * 3.5f * this.density * waveAmp
                    val breath = if (agslActive) 1f else (1f + 0.025f * sin(wRad - 0.6f) * waveAmp)
                    val flyingScale = lerpFloat(1.14f, 1f, p) * breath
                    val targetTopLeft = state.heroTitleTopLeft
                    transformOrigin = TransformOrigin(0f, 0f)
                    val fw = flyingTitleSize.width.toFloat()
                    val fh = flyingTitleSize.height.toFloat()
                    val startScale = 1.14f * breath
                    val startX = (maxContainerWidthPx - fw * startScale) / 2f + swayX
                    val startY = (maxContainerHeightPx / 2f - centerOffsetYPx) - (fh * startScale) / 2f + bobY
                    val endX = targetTopLeft?.x ?: startX
                    val endY = targetTopLeft?.y ?: startY
                    translationX = lerpFloat(startX, endX, p)
                    translationY = lerpFloat(startY, endY, p)
                    scaleX = flyingScale
                    scaleY = flyingScale
                    alpha = if (flyingTitleSize == IntSize.Zero || (!isLoading && state.showRealHeroTitle)) 0f else 1f
                },
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = onBg,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = onBg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .widthIn(max = maxWidth - 64.dp)
                    .onSizeChanged { subtitleSize = it }
                    .graphicsLayer {
                        val p = if (isLoading) 0f else state.progress
                        val subAlpha = if (isLoading) 1f else (1f - (p / 0.30f).coerceIn(0f, 1f))
                        val titleHeightPx = flyingTitleSize.height.toFloat() * 1.14f
                        translationX = (maxContainerWidthPx - subtitleSize.width) / 2f
                        translationY = (maxContainerHeightPx / 2f - centerOffsetYPx) + titleHeightPx / 2f + subtitleGapPx
                        alpha = if (subtitleSize == IntSize.Zero) 0f else 0.70f * subAlpha
                    },
            )
        }
    }
}
