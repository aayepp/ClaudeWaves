package com.lastwave.app.ui.common

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
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
import kotlin.math.min
import kotlin.math.sin

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
    val ringColor = remember(onBg, primary, isDark, isComic, comicAccent) {
        when {
            isComic -> comicAccent
            isDark -> lerpColor(Color.White, primary, 0.25f)
            else -> lerpColor(onBg, primary, 0.45f)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "detailSonarPulse")
    val pulseCycleState: State<Float> = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
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
            .zIndex(5f),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val p = if (isLoading) 0f else state.progress
            val ringFade = if (isLoading) 1f else (1f - (p / 0.58f).coerceIn(0f, 1f))
            if (ringFade <= 0.01f) return@Canvas
            val ringExpand = if (isLoading) 1f else 1f + 0.88f * p
            val pulseCycle = pulseCycleState.value
            val cx = size.width / 2f
            val cy = size.height / 2f - 18.dp.toPx()
            val minRadius = 44.dp.toPx()
            val maxRadius = min(size.width, size.height) * 0.37f
            val ringCount = 5
            for (i in 0 until ringCount) {
                val phase = (pulseCycle + i / ringCount.toFloat()) % 1f
                val radius = lerpFloat(minRadius, maxRadius, phase) * ringExpand
                val envelope = sin(phase * PI).toFloat()
                if (envelope <= 0.01f) continue
                if (isComic) {
                    drawCircle(
                        color = ringColor.copy(alpha = 0.30f * envelope * ringFade),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.5.dp.toPx()),
                    )
                } else {
                    val baseWidth = lerpFloat(10.dp.toPx(), 18.dp.toPx(), phase)
                    val peakAlpha = (if (isDark) 0.14f else 0.11f) * envelope * ringFade
                    for (layer in 0 until 3) {
                        val layerWidth = baseWidth * (1f + layer * 0.65f)
                        val layerAlpha = peakAlpha / (1f + layer * 1.15f)
                        drawCircle(
                            color = ringColor.copy(alpha = layerAlpha),
                            radius = radius,
                            center = Offset(cx, cy),
                            style = Stroke(width = layerWidth),
                        )
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
                    val breath = 1f + 0.022f * sin(pulseCycleState.value * 2f * PI).toFloat() * (1f - p)
                    val flyingScale = lerpFloat(1.14f, 1f, p) * breath
                    val targetTopLeft = state.heroTitleTopLeft
                    transformOrigin = TransformOrigin(0f, 0f)
                    val fw = flyingTitleSize.width.toFloat()
                    val fh = flyingTitleSize.height.toFloat()
                    val startScale = 1.14f * breath
                    val startX = (maxContainerWidthPx - fw * startScale) / 2f
                    val startY = (maxContainerHeightPx / 2f - centerOffsetYPx) - (fh * startScale) / 2f
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
