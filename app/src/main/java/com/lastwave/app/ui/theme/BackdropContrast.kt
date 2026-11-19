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

private const val SAMPLE_W = 8
private const val SAMPLE_H = 4
private const val INITIAL_SAMPLE_DELAY_MS = 60L
private const val SAMPLE_INTERVAL_MS = 200L
private const val DARK_TO_LIGHT_THRESHOLD = 0.42f
private const val LIGHT_TO_DARK_THRESHOLD = 0.56f

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
 * Samples a tiny 8x4 downscale of [backdrop] behind [bounds] at a throttled cadence
 * and returns whether foreground text/icons should use light (white) ink for legibility.
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

    LaunchedEffect(layerBackdrop, isDark, scrimBaseAlpha, lifecycleOwner) {
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
                val lum = try {
                    sampleLayer.record(
                        density = density,
                        layoutDirection = layoutDirection,
                        size = IntSize(SAMPLE_W, SAMPLE_H),
                    ) {
                        withTransform({
                            scale(
                                scaleX = SAMPLE_W / rect.width,
                                scaleY = SAMPLE_H / rect.height,
                                pivot = Offset.Zero,
                            )
                            translate(left = -rect.left, top = -rect.top)
                        }) {
                            drawLayer(sourceLayer)
                        }
                    }
                    sampleLayer.toImageBitmap().readPixels(pixels)
                    computeEffectiveLuminance(
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

private fun computeEffectiveLuminance(
    pixels: IntArray,
    isDark: Boolean,
    scrimBaseAlpha: Float,
    tintColor: Color,
): Float? {
    var sumR = 0f
    var sumG = 0f
    var sumB = 0f
    var count = 0
    for (px in pixels) {
        val a = (px ushr 24) and 0xFF
        if (a == 0) continue
        sumR += ((px ushr 16) and 0xFF) / 255f
        sumG += ((px ushr 8) and 0xFF) / 255f
        sumB += (px and 0xFF) / 255f
        count++
    }
    if (count == 0) return null

    var r = sumR / count
    var g = sumG / count
    var b = sumB / count

    // Composite the frosted glass scrim so the threshold matches what sits behind the text.
    val scrimAlpha = LiquidGlassIntensity.tint(scrimBaseAlpha).coerceIn(0f, 1f)
    val scrimChannel = if (isDark) 0f else 1f
    val invScrim = 1f - scrimAlpha
    r = r * invScrim + scrimChannel * scrimAlpha
    g = g * invScrim + scrimChannel * scrimAlpha
    b = b * invScrim + scrimChannel * scrimAlpha

    if (tintColor.isSpecified && tintColor.alpha > 0f) {
        val washAlpha = ((if (isDark) 0.14f else 0.10f) * lerp(0.65f, 1.15f, LiquidGlassIntensity.value))
            .coerceIn(0f, 1f)
        val invWash = 1f - washAlpha
        r = r * invWash + tintColor.red * washAlpha
        g = g * invWash + tintColor.green * washAlpha
        b = b * invWash + tintColor.blue * washAlpha
    }

    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
