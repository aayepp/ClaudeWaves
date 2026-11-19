package com.lastwave.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import kotlin.math.pow

private const val SAMPLE_W = 16
private const val SAMPLE_H = 8
private const val INITIAL_SAMPLE_DELAY_MS = 40L
private const val SAMPLE_INTERVAL_MS = 100L
// True WCAG 2.1 crossover between white and black ink is L = sqrt(1.05 * 0.05) - 0.05 ~= 0.179.
// Tight hysteresis around 0.179 catches subtle background changes without flickering.
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
 * Samples a 16x8 center-weighted downscale of [backdrop] behind the cropped text/icon zone
 * of [bounds] at a throttled cadence, computes true WCAG 2.1 linear relative luminance
 * after compositing the glass scrim, and returns whether foreground ink should be light.
 *
 * Returns [defaultUseLightInk] with zero GPU work when Liquid Glass is disabled or unsupported
 * (Comic Pop, glass off, Android < 12).
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
    defaultUseLightInk: Boolean = LocalIsDarkTheme.current,
): Boolean {
    val isDark = LocalIsDarkTheme.current
    val layerBackdrop = if (enabled && isLiquidGlassBackdropSupported()) backdrop as? LayerBackdrop else null
    if (layerBackdrop == null) return defaultUseLightInk

    var useLightInk by remember(defaultUseLightInk, layerBackdrop) { mutableStateOf(defaultUseLightInk) }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val sampleLayer = rememberGraphicsLayer()
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestTint by rememberUpdatedState(tintColor)

    LaunchedEffect(layerBackdrop, isDark, scrimBaseAlpha, cropStartX, cropEndX, cropStartY, cropEndY, lifecycleOwner) {
        val pixels = IntArray(SAMPLE_W * SAMPLE_H)
        delay(INITIAL_SAMPLE_DELAY_MS)
        while (true) {
            val active = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
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
                        size = IntSize(SAMPLE_W, SAMPLE_H),
                    ) {
                        withTransform({
                            scale(
                                scaleX = SAMPLE_W / sampleW,
                                scaleY = SAMPLE_H / sampleH,
                                pivot = Offset.Zero,
                            )
                            translate(left = -sampleLeft, top = -sampleTop)
                        }) {
                            drawLayer(sourceLayer)
                        }
                    }
                    sampleLayer.toImageBitmap().readPixels(pixels)
                    computeEffectiveWcagLuminance(
                        pixels = pixels,
                        isDark = isDark,
                        scrimBaseAlpha = scrimBaseAlpha,
                        tintColor = latestTint,
                    )
                } catch (e: Throwable) {
                    currentCoroutineContext().ensureActive()
                    null
                }
                if (lum != null) {
                    if (useLightInk && lum > LIGHT_TO_DARK_THRESHOLD) {
                        useLightInk = false
                    } else if (!useLightInk && lum < DARK_TO_LIGHT_THRESHOLD) {
                        useLightInk = true
                    }
                }
            }
            delay(SAMPLE_INTERVAL_MS)
        }
    }

    return useLightInk
}

private fun computeEffectiveWcagLuminance(
    pixels: IntArray,
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

    var weightedLumSum = 0f
    var weightSum = 0f
    for (y in 0 until SAMPLE_H) {
        // Middle rows where text/icon strokes sit carry 2x weight so small changes behind glyphs register fast.
        val rowWeight = if (y in 2..5) 2f else 1f
        val rowOffset = y * SAMPLE_W
        for (x in 0 until SAMPLE_W) {
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
