package com.lastwave.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

private const val ATLAS_W = 32
private const val ATLAS_H = 16
private const val STANDALONE_W = 16
private const val STANDALONE_H = 8
private const val INITIAL_SAMPLE_DELAY_MS = 80L
private const val SAMPLE_INTERVAL_MS = 220L
// True WCAG 2.1 crossover between white and black ink is L = sqrt(1.05 * 0.05) - 0.05 ~= 0.179.
private const val DARK_TO_LIGHT_THRESHOLD = 0.165f
private const val LIGHT_TO_DARK_THRESHOLD = 0.215f
internal const val ADAPTIVE_INK_FADE_MS = 120

/** Plain holder so layout updates do not recompose the chrome on every scroll frame. */
internal class BackdropBoundsHolder {
    var rect: Rect? = null
}

internal fun Modifier.trackBackdropBounds(holder: BackdropBoundsHolder): Modifier =
    onGloballyPositioned { coords ->
        if (coords.isAttached) {
            holder.rect = coords.boundsInRoot()
        }
    }

/**
 * Shared coordinator that batches all adaptive-ink targets reading the same [LayerBackdrop]
 * into ONE tiny 32x16 GPU readback per tick instead of N independent RenderThread stalls.
 */
internal class BackdropContrastCoordinator(
    val backdrop: LayerBackdrop,
) {
    internal class Target(
        val bounds: BackdropBoundsHolder,
        var scrimBaseAlpha: Float,
        var tintColor: Color,
        var cropStartX: Float,
        var cropEndX: Float,
        var cropStartY: Float,
        var cropEndY: Float,
        var isVisible: () -> Boolean,
        var onLuminance: (Float) -> Unit,
    )

    private val targets = mutableListOf<Target>()

    fun register(target: Target) {
        targets.add(target)
    }

    fun unregister(target: Target) {
        targets.remove(target)
    }

    fun snapshotTargets(): List<Target> = if (targets.isEmpty()) emptyList() else targets.toList()
}

internal val LocalBackdropContrastCoordinator =
    staticCompositionLocalOf<BackdropContrastCoordinator?> { null }

/**
 * Hosts the single shared sampling loop for [backdrop] (installed once in PlayerHost).
 * Pauses automatically when [paused] is true (e.g. full player covering the screen),
 * during sheet motion, or when the app is backgrounded.
 */
@Composable
internal fun rememberBackdropContrastCoordinator(
    backdrop: LayerBackdrop?,
    paused: () -> Boolean = { false },
): BackdropContrastCoordinator? {
    if (backdrop == null || !isLiquidGlassBackdropSupported()) return null
    val isDark = LocalIsDarkTheme.current
    val coordinator = remember(backdrop) { BackdropContrastCoordinator(backdrop) }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val sampleLayer = rememberGraphicsLayer()
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestPaused by rememberUpdatedState(paused)

    LaunchedEffect(coordinator, isDark, lifecycleOwner) {
        val pixels = IntArray(ATLAS_W * ATLAS_H)
        delay(INITIAL_SAMPLE_DELAY_MS)
        while (true) {
            val active = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
                !latestPaused() &&
                !GlassMotion.isSheetMoving()
            val sourceLayer = backdrop.graphicsLayer
            if (active && !sourceLayer.isReleased && sourceLayer.size.width > 0 && sourceLayer.size.height > 0) {
                val activeTargets = coordinator.snapshotTargets()
                var unionLeft = Float.POSITIVE_INFINITY
                var unionTop = Float.POSITIVE_INFINITY
                var unionRight = Float.NEGATIVE_INFINITY
                var unionBottom = Float.NEGATIVE_INFINITY
                var anyValid = false
                for (t in activeTargets) {
                    if (!t.isVisible()) continue
                    val r = t.bounds.rect ?: continue
                    if (r.width < 4f || r.height < 4f) continue
                    val sl = r.left + r.width * t.cropStartX
                    val sr = r.left + r.width * t.cropEndX
                    val st = r.top + r.height * t.cropStartY
                    val sb = r.top + r.height * t.cropEndY
                    if (sr - sl < 2f || sb - st < 2f) continue
                    unionLeft = min(unionLeft, sl)
                    unionTop = min(unionTop, st)
                    unionRight = max(unionRight, sr)
                    unionBottom = max(unionBottom, sb)
                    anyValid = true
                }
                if (anyValid) {
                    val unionW = (unionRight - unionLeft).coerceAtLeast(4f)
                    val unionH = (unionBottom - unionTop).coerceAtLeast(4f)
                    val captured = try {
                        sampleLayer.record(
                            density = density,
                            layoutDirection = layoutDirection,
                            size = IntSize(ATLAS_W, ATLAS_H),
                        ) {
                            withTransform({
                                scale(
                                    scaleX = ATLAS_W / unionW,
                                    scaleY = ATLAS_H / unionH,
                                    pivot = Offset.Zero,
                                )
                                translate(left = -unionLeft, top = -unionTop)
                            }) {
                                drawLayer(sourceLayer)
                            }
                        }
                        sampleLayer.toImageBitmap().readPixels(pixels)
                        true
                    } catch (e: Throwable) {
                        currentCoroutineContext().ensureActive()
                        false
                    }
                    if (captured) {
                        for (t in activeTargets) {
                            if (!t.isVisible()) continue
                            val r = t.bounds.rect ?: continue
                            if (r.width < 4f || r.height < 4f) continue
                            val sl = r.left + r.width * t.cropStartX
                            val sr = r.left + r.width * t.cropEndX
                            val st = r.top + r.height * t.cropStartY
                            val sb = r.top + r.height * t.cropEndY
                            val x0 = (((sl - unionLeft) / unionW) * ATLAS_W).toInt().coerceIn(0, ATLAS_W - 1)
                            val x1 = (((sr - unionLeft) / unionW) * ATLAS_W).toInt().coerceIn(x0 + 1, ATLAS_W)
                            val y0 = (((st - unionTop) / unionH) * ATLAS_H).toInt().coerceIn(0, ATLAS_H - 1)
                            val y1 = (((sb - unionTop) / unionH) * ATLAS_H).toInt().coerceIn(y0 + 1, ATLAS_H)
                            val lum = computeRegionWcagLuminance(
                                pixels = pixels,
                                stride = ATLAS_W,
                                x0 = x0,
                                x1 = x1,
                                y0 = y0,
                                y1 = y1,
                                isDark = isDark,
                                scrimBaseAlpha = t.scrimBaseAlpha,
                                tintColor = t.tintColor,
                            )
                            if (lum != null) t.onLuminance(lum)
                        }
                    }
                }
            }
            delay(SAMPLE_INTERVAL_MS)
        }
    }

    return coordinator
}

/**
 * Returns whether foreground text/icons over [backdrop] at [bounds] should use light (white) ink.
 *
 * Uses the shared [LocalBackdropContrastCoordinator] when available so all chrome elements
 * share one tiny GPU readback per tick. Returns [defaultUseLightInk] with zero GPU work when
 * Liquid Glass is disabled or unsupported (Comic Pop, glass off, Android < 12).
 */
@Composable
internal fun rememberBackdropUseLightInk(
    backdrop: Backdrop?,
    bounds: BackdropBoundsHolder,
    enabled: Boolean,
    scrimBaseAlpha: Float = 0.28f,
    tintColor: Color = Color.Unspecified,
    cropStartX: Float = 0.04f,
    cropEndX: Float = 0.96f,
    cropStartY: Float = 0.15f,
    cropEndY: Float = 0.85f,
    isVisible: () -> Boolean = { true },
    defaultUseLightInk: Boolean = LocalIsDarkTheme.current,
): Boolean {
    val isDark = LocalIsDarkTheme.current
    val layerBackdrop = if (enabled && isLiquidGlassBackdropSupported()) backdrop as? LayerBackdrop else null
    if (layerBackdrop == null) return defaultUseLightInk

    var useLightInk by remember(defaultUseLightInk, layerBackdrop) { mutableStateOf(defaultUseLightInk) }
    val applyLuminance = remember(defaultUseLightInk, layerBackdrop) {
        { lum: Float ->
            if (useLightInk && lum > LIGHT_TO_DARK_THRESHOLD) {
                useLightInk = false
            } else if (!useLightInk && lum < DARK_TO_LIGHT_THRESHOLD) {
                useLightInk = true
            }
        }
    }

    val coordinator = LocalBackdropContrastCoordinator.current
    if (coordinator != null && coordinator.backdrop === layerBackdrop) {
        val target = remember(coordinator, bounds) {
            BackdropContrastCoordinator.Target(
                bounds = bounds,
                scrimBaseAlpha = scrimBaseAlpha,
                tintColor = tintColor,
                cropStartX = cropStartX,
                cropEndX = cropEndX,
                cropStartY = cropStartY,
                cropEndY = cropEndY,
                isVisible = isVisible,
                onLuminance = applyLuminance,
            )
        }
        target.scrimBaseAlpha = scrimBaseAlpha
        target.tintColor = tintColor
        target.cropStartX = cropStartX
        target.cropEndX = cropEndX
        target.cropStartY = cropStartY
        target.cropEndY = cropEndY
        target.isVisible = isVisible
        target.onLuminance = applyLuminance

        DisposableEffect(coordinator, target) {
            coordinator.register(target)
            onDispose { coordinator.unregister(target) }
        }
        return useLightInk
    }

    // Fallback standalone sampler only when a caller passes a custom LayerBackdrop outside PlayerHost.
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val sampleLayer = rememberGraphicsLayer()
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestTint by rememberUpdatedState(tintColor)
    val latestVisible by rememberUpdatedState(isVisible)

    LaunchedEffect(layerBackdrop, isDark, scrimBaseAlpha, cropStartX, cropEndX, cropStartY, cropEndY, lifecycleOwner) {
        val pixels = IntArray(STANDALONE_W * STANDALONE_H)
        delay(INITIAL_SAMPLE_DELAY_MS)
        while (true) {
            val active = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
                latestVisible() &&
                !GlassMotion.isSheetMoving()
            val rect = bounds.rect
            val sourceLayer = layerBackdrop.graphicsLayer
            if (
                active &&
                rect != null &&
                rect.width >= 4f &&
                rect.height >= 4f &&
                !sourceLayer.isReleased &&
                sourceLayer.size.width > 0 &&
                sourceLayer.size.height > 0
            ) {
                val sampleLeft = rect.left + rect.width * cropStartX
                val sampleTop = rect.top + rect.height * cropStartY
                val sampleW = (rect.width * (cropEndX - cropStartX)).coerceAtLeast(4f)
                val sampleH = (rect.height * (cropEndY - cropStartY)).coerceAtLeast(4f)
                val lum = try {
                    sampleLayer.record(
                        density = density,
                        layoutDirection = layoutDirection,
                        size = IntSize(STANDALONE_W, STANDALONE_H),
                    ) {
                        withTransform({
                            scale(
                                scaleX = STANDALONE_W / sampleW,
                                scaleY = STANDALONE_H / sampleH,
                                pivot = Offset.Zero,
                            )
                            translate(left = -sampleLeft, top = -sampleTop)
                        }) {
                            drawLayer(sourceLayer)
                        }
                    }
                    sampleLayer.toImageBitmap().readPixels(pixels)
                    computeRegionWcagLuminance(
                        pixels = pixels,
                        stride = STANDALONE_W,
                        x0 = 0,
                        x1 = STANDALONE_W,
                        y0 = 0,
                        y1 = STANDALONE_H,
                        isDark = isDark,
                        scrimBaseAlpha = scrimBaseAlpha,
                        tintColor = latestTint,
                    )
                } catch (e: Throwable) {
                    currentCoroutineContext().ensureActive()
                    null
                }
                if (lum != null) applyLuminance(lum)
            }
            delay(SAMPLE_INTERVAL_MS)
        }
    }

    return useLightInk
}

private fun computeRegionWcagLuminance(
    pixels: IntArray,
    stride: Int,
    x0: Int,
    x1: Int,
    y0: Int,
    y1: Int,
    isDark: Boolean,
    scrimBaseAlpha: Float,
    tintColor: Color,
): Float? {
    val scrimAlpha = LiquidGlassIntensity.tint(scrimBaseAlpha).coerceIn(0f, 1f)
    val scrimChannel = if (isDark) 0f else 1f
    val invScrim = 1f - scrimAlpha

    val hasTint = tintColor.isSpecified && tintColor.alpha > 0f
    val washAlpha = if (hasTint) {
        ((if (isDark) 0.14f else 0.10f) * lerp(0.65f, 1.15f, LiquidGlassIntensity.value)).coerceIn(0f, 1f)
    } else 0f
    val invWash = 1f - washAlpha
    val tintR = if (hasTint) tintColor.red * washAlpha else 0f
    val tintG = if (hasTint) tintColor.green * washAlpha else 0f
    val tintB = if (hasTint) tintColor.blue * washAlpha else 0f

    val height = (y1 - y0).coerceAtLeast(1)
    val midStart = y0 + height / 4
    val midEnd = y1 - height / 4

    var weightedLumSum = 0f
    var weightSum = 0f
    for (y in y0 until y1) {
        val rowWeight = if (y in midStart until midEnd) 2f else 1f
        val rowOffset = y * stride
        for (x in x0 until x1) {
            val px = pixels[rowOffset + x]
            val a = (px ushr 24) and 0xFF
            if (a == 0) continue

            var r = ((px ushr 16) and 0xFF) / 255f
            var g = ((px ushr 8) and 0xFF) / 255f
            var b = (px and 0xFF) / 255f

            r = r * invScrim + scrimChannel * scrimAlpha
            g = g * invScrim + scrimChannel * scrimAlpha
            b = b * invScrim + scrimChannel * scrimAlpha

            if (hasTint) {
                r = r * invWash + tintR
                g = g * invWash + tintG
                b = b * invWash + tintB
            }

            val lum = 0.2126f * srgbToLinear(r) + 0.7152f * srgbToLinear(g) + 0.0722f * srgbToLinear(b)
            weightedLumSum += lum * rowWeight
            weightSum += rowWeight
        }
    }
    if (weightSum <= 0f) return null
    return weightedLumSum / weightSum
}

private fun srgbToLinear(c: Float): Float =
    if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
