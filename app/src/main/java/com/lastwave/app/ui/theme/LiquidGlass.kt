package com.lastwave.app.ui.theme

import android.app.ActivityManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.kyant.backdrop.backdrops.layerBackdrop as nativeBackdrop

/** Shared opt-in flag for Settings > Experimental > Liquid Glass. */
val LocalLiquidGlass = staticCompositionLocalOf { false }

/**
 * The user's glass clarity (Settings › Appearance), 0..1: 0 is frosted and
 * solid, 1 is clear with strong refraction. Read at draw time by every glass
 * recipe, so dragging the slider previews live without recomposing the
 * screens behind it.
 */
object LiquidGlassIntensity {
    var value by mutableFloatStateOf(com.lastwave.app.data.local.DEFAULT_LIQUID_GLASS_INTENSITY)

    /** Scrim from [opaque] (0, frosted) down to a crystal-clear fifth of [alpha] (1, clear). */
    fun tint(alpha: Float, opaque: Float = 0.56f): Float = lerp(opaque, alpha * 0.22f, value)

    /** Vibrancy rises as the glass clears, so colours behind glow through. */
    fun saturation(frosted: Float, clear: Float): Float = lerp(frosted, clear, value)

    /** Blur multiplier: 1.65× frosted at 0, down to ~0.47× (~5.6 dp) at 1 so lens refraction reads crystal clear. */
    fun blur(): Float = 1.65f - 1.18f * value
}

/**
 * Normalized device gravity tilt in [-1f, 1f], read strictly inside draw lambdas
 * so sensor ticks shift glass highlights without triggering recompositions.
 */
internal object GlassTilt {
    var x by mutableFloatStateOf(0f)
        private set
    var y by mutableFloatStateOf(0f)
        private set

    internal fun update(nx: Float, ny: Float) {
        if (abs(nx - x) > 0.012f) x = nx
        if (abs(ny - y) > 0.012f) y = ny
    }

    internal fun reset() {
        x = 0f
        y = 0f
    }
}

/** Registers a single low-power gravity sensor while Liquid Glass is active and the window is resumed. */
@Composable
internal fun GlassTiltEffect(enabled: Boolean) {
    val context = LocalContext.current
    LifecycleResumeEffect(enabled, context) {
        if (!enabled) {
            GlassTilt.reset()
            onPauseOrDispose { GlassTilt.reset() }
        } else {
            val manager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val sensor = manager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
                ?: manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (manager == null || sensor == null) {
                GlassTilt.reset()
                onPauseOrDispose { }
            } else {
                var smoothX = 0f
                var smoothY = 0f
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val targetX = (-event.values[0] / 6.5f).coerceIn(-1f, 1f)
                        val targetY = ((event.values[1] - 4.5f) / 6.5f).coerceIn(-1f, 1f)
                        smoothX += (targetX - smoothX) * 0.14f
                        smoothY += (targetY - smoothY) * 0.14f
                        GlassTilt.update(smoothX, smoothY)
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
                onPauseOrDispose {
                    manager.unregisterListener(listener)
                    GlassTilt.reset()
                }
            }
        }
    }
}

/** Shared toggle for classic Material 3 full-width bottom navigation bar. */
val LocalMd3NavBar = staticCompositionLocalOf { false }

/** Background-only source for surfaces inside the captured scrolling content. */
val LocalLiquidGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** Separate source for overlays; kept for call-site compat, same type as backdrop. */
val LocalLiquidGlassOverlayBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/** Content-brightness hint, kept for compat (native recipe does not need it). */
val LocalLiquidGlassContentBrightness = compositionLocalOf { 0f }

/** Optional subtle artwork/accent tint reflected into glass scrim & specular rim. */
val LocalLiquidGlassTint = compositionLocalOf { Color.Unspecified }

/**
 * Plain nanosecond clock (after Auris' GlassMotion) that marks active Now Playing sheet
 * travel without snapshot-state invalidations, so glass can skip heavy lens passes while
 * the sheet is in flight and automatically resume within 500 ms of settling.
 */
object GlassMotion {
    private const val FREEZE_WINDOW_NS = 500_000_000L
    @Volatile
    private var lastSheetStampNs: Long = Long.MIN_VALUE

    fun stampSheet() {
        lastSheetStampNs = System.nanoTime()
    }

    fun clearSheet() {
        lastSheetStampNs = Long.MIN_VALUE
    }

    fun isSheetMoving(): Boolean {
        val stamp = lastSheetStampNs
        if (stamp == Long.MIN_VALUE) return false
        return (System.nanoTime() - stamp) < FREEZE_WINDOW_NS
    }
}

/** Typealiases for clean, unified Backdrop types (mirrors PlatformBackdrop). */
typealias LayerBackdrop = LayerBackdrop
typealias Backdrop = Backdrop

/** Factory matching Kyant0 Backdrop's official API.
 *
 *  NOTE: the Compose compiler forbids try/catch (and runCatching) around
 *  @Composable invocations, so this stays a direct passthrough. First-frame
 *  safety comes from the [isDeviceGlassCapable] gate at every call site,
 *  and any failure still lands in the crash-guard log + startup trail. */
@Composable
fun rememberLayerBackdrop(
    onDraw: androidx.compose.ui.graphics.drawscope.ContentDrawScope.() -> Unit = { drawContent() },
): LayerBackdrop? = com.kyant.backdrop.backdrops.rememberLayerBackdrop(onDraw = onDraw)

/** Marks a composable as the source layer that sibling glass surfaces refract. */
fun Modifier.layerBackdropCompat(backdrop: LayerBackdrop): Modifier = this.nativeBackdrop(backdrop)

/** Remember a backdrop that draws a flat color + content. */
@Composable
fun rememberBackdrop(color: Color): LayerBackdrop? =
    rememberLayerBackdrop {
        drawRect(color)
        drawContent()
    }

/**
 * Continuous-curvature squircle (G2 superellipse) implementing [CornerBasedShape].
 * Kept from LastWave so DockShape stays a CornerBasedShape (lens-compatible).
 */
class SquircleShape(
    topStart: CornerSize,
    topEnd: CornerSize,
    bottomEnd: CornerSize,
    bottomStart: CornerSize,
) : CornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {

    constructor(radius: Dp) : this(
        CornerSize(radius),
        CornerSize(radius),
        CornerSize(radius),
        CornerSize(radius),
    )

    constructor(percent: Int = 50) : this(
        CornerSize(percent),
        CornerSize(percent),
        CornerSize(percent),
        CornerSize(percent),
    )

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize,
    ): CornerBasedShape = SquircleShape(topStart, topEnd, bottomEnd, bottomStart)

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return Outline.Rectangle(androidx.compose.ui.geometry.Rect.Zero)

        val isLtr = layoutDirection == LayoutDirection.Ltr
        val tl = if (isLtr) topStart else topEnd
        val tr = if (isLtr) topEnd else topStart
        val br = if (isLtr) bottomEnd else bottomStart
        val bl = if (isLtr) bottomStart else bottomEnd

        if (tl <= 0f && tr <= 0f && br <= 0f && bl <= 0f) {
            return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
        }

        val path = createSquirclePath(w, h, tl, tr, br, bl)
        return Outline.Generic(path)
    }
}

fun createSquirclePath(
    w: Float,
    h: Float,
    tlRadius: Float,
    trRadius: Float,
    brRadius: Float,
    blRadius: Float,
): Path {
    val path = Path()
    val maxRadius = minOf(w, h) / 2f
    val tl = tlRadius.coerceIn(0f, maxRadius)
    val tr = trRadius.coerceIn(0f, maxRadius)
    val br = brRadius.coerceIn(0f, maxRadius)
    val bl = blRadius.coerceIn(0f, maxRadius)

    val k = 1.528665f
    var lTl = tl * k
    var lTr = tr * k
    var lBr = br * k
    var lBl = bl * k

    val maxWTop = lTl + lTr
    if (maxWTop > w && maxWTop > 0f) {
        val scale = w / maxWTop
        lTl *= scale
        lTr *= scale
    }
    val maxWBottom = lBl + lBr
    if (maxWBottom > w && maxWBottom > 0f) {
        val scale = w / maxWBottom
        lBl *= scale
        lBr *= scale
    }
    val maxHLeft = lTl + lBl
    if (maxHLeft > h && maxHLeft > 0f) {
        val scale = h / maxHLeft
        lTl *= scale
        lBl *= scale
    }
    val maxHRight = lTr + lBr
    if (maxHRight > h && maxHRight > 0f) {
        val scale = h / maxHRight
        lTr *= scale
        lBr *= scale
    }

    path.moveTo(lTl, 0f)
    path.lineTo(w - lTr, 0f)
    if (lTr > 0.001f) {
        path.cubicTo(
            w - lTr * (1f - 0.712053f), 0f,
            w - lTr * (1f - 0.566789f), lTr * 0.030183f,
            w - lTr * (1f - 0.455953f), lTr * 0.087377f,
        )
        path.cubicTo(
            w - lTr * (1f - 0.345118f), lTr * 0.144571f,
            w - lTr * (1f - 0.242846f), lTr * 0.242846f,
            w - lTr * (1f - 0.144571f), lTr * 0.345118f,
        )
        path.cubicTo(
            w - lTr * (1f - 0.087377f), lTr * 0.455953f,
            w - lTr * 0.030183f, lTr * (1f - 0.566789f),
            w, lTr * (1f - 0.712053f),
        )
        path.lineTo(w, lTr)
    } else {
        path.lineTo(w, 0f)
        path.lineTo(w, lTr)
    }
    path.lineTo(w, h - lBr)
    if (lBr > 0.001f) {
        path.cubicTo(
            w, h - lBr * (1f - 0.712053f),
            w - lBr * 0.030183f, h - lBr * (1f - 0.566789f),
            w - lBr * 0.087377f, h - lBr * (1f - 0.455953f),
        )
        path.cubicTo(
            w - lBr * 0.144571f, h - lBr * (1f - 0.345118f),
            w - lBr * 0.242846f, h - lBr * (1f - 0.242846f),
            w - lBr * 0.345118f, h - lBr * (1f - 0.144571f),
        )
        path.cubicTo(
            w - lBr * 0.455953f, h - lBr * (1f - 0.087377f),
            w - lBr * (1f - 0.566789f), h - lBr * 0.030183f,
            w - lBr * (1f - 0.712053f), h,
        )
        path.lineTo(w - lBr, h)
    } else {
        path.lineTo(w, h)
        path.lineTo(w - lBr, h)
    }
    path.lineTo(lBl, h)
    if (lBl > 0.001f) {
        path.cubicTo(
            lBl * (1f - 0.712053f), h,
            lBl * (1f - 0.566789f), h - lBl * 0.030183f,
            lBl * (1f - 0.455953f), h - lBl * 0.087377f,
        )
        path.cubicTo(
            lBl * (1f - 0.345118f), h - lBl * 0.144571f,
            lBl * (1f - 0.242846f), h - lBl * 0.242846f,
            lBl * (1f - 0.144571f), h - lBl * 0.345118f,
        )
        path.cubicTo(
            lBl * (1f - 0.087377f), h - lBl * 0.455953f,
            lBl * 0.030183f, h - lBl * (1f - 0.566789f),
            0f, h - lBl * (1f - 0.712053f),
        )
        path.lineTo(0f, h - lBl)
    } else {
        path.lineTo(0f, h)
        path.lineTo(0f, h - lBl)
    }
    path.lineTo(0f, lTl)
    if (lTl > 0.001f) {
        path.cubicTo(
            0f, lTl * (1f - 0.712053f),
            lTl * 0.030183f, lTl * (1f - 0.566789f),
            lTl * 0.087377f, lTl * (1f - 0.455953f),
        )
        path.cubicTo(
            lTl * 0.144571f, lTl * (1f - 0.345118f),
            lTl * 0.242846f, lTl * (1f - 0.242846f),
            lTl * 0.345118f, lTl * (1f - 0.144571f),
        )
        path.cubicTo(
            lTl * 0.455953f, lTl * (1f - 0.087377f),
            lTl * (1f - 0.566789f), lTl * 0.030183f,
            lTl * (1f - 0.712053f), 0f,
        )
        path.lineTo(lTl, 0f)
    } else {
        path.lineTo(0f, 0f)
        path.lineTo(lTl, 0f)
    }
    path.close()
    return path
}

/** Keeps glass inside Material's visual bounds while retaining its outer touch target. */
@Composable
fun LiquidGlassSurface(
    onClick: () -> Unit,
    glassModifier: Modifier,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val jiggleFallback = if (!isLiquidGlassBackdropSupported()) Modifier.waterDropJiggle(amplitude = 0.045f) else Modifier
    Surface(
        onClick = onClick,
        modifier = modifier
            .then(jiggleFallback)
            .comicPress(source)
            .comicPanel(shape, offset = 3.dp, translucent = LocalLiquidGlass.current || color.alpha < 0.99f),
        shape = shape,
        color = color,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        border = border,
        interactionSource = source,
        enabled = enabled,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().then(glassModifier),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

// ── Interactive liquid glass core ──────────────────────────────────────────────
// No custom RuntimeShader, no ambient infiniteTransition, no haptics,
// no progressive blur, no always-on chromatic aberration.
// Effect stack is drawInteractiveGlass.

@Composable
fun isDeviceGlassCapable(): Boolean {
    val view = LocalView.current
    if (view.isInEditMode) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
    if (!view.isHardwareAccelerated) return false
    val context = LocalContext.current
    val am = remember(context) {
        runCatching { context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager }.getOrNull()
    }
    if (am?.isLowRamDevice == true) return false
    return true
}

@Composable
fun isLiquidGlassBackdropSupported(): Boolean =
    LocalLiquidGlass.current && isDeviceGlassCapable()

@Composable
fun Modifier.liquidGlassSource(
    backdrop: LayerBackdrop?,
): Modifier {
    if (backdrop == null) return this
    if (!isLiquidGlassBackdropSupported()) return this
    return this.layerBackdrop(backdrop)
}

@Composable
fun liquidGlassContainerColor(
    color: Color,
    enabled: Boolean = LocalLiquidGlass.current,
    backdrop: Backdrop? = LocalLiquidGlassBackdrop.current,
): Color = when {
    enabled && isLiquidGlassBackdropSupported() && backdrop != null ->
        if (color.isSpecified && color.alpha < 0.95f) color else Color.Transparent
    // Comic Pop has no glass, so a faint tint left the artwork showing through
    // the player's buttons: light covers washed out their labels at night.
    LocalComicTheme.current && color.isSpecified && color.alpha in 0.01f..0.95f ->
        color.compositeOver(MaterialTheme.colorScheme.background)
    else -> color
}

@Composable
fun isLiquidGlassEnabled(): Boolean = LocalLiquidGlass.current

/**
 * Press/hold state holder for a single liquid-glass surface.
 * Responsive GlassInteraction: spring press 0→1,
 * observe-only drag so wrapped clicks keep working.
 */
class GlassInteraction(
    private val animationScope: CoroutineScope,
) {
    private val pressSpec = spring(dampingRatio = 0.72f, stiffness = 240f, visibilityThreshold = 0.001f)
    private val jiggleDownSpec = spring(dampingRatio = 0.42f, stiffness = 210f, visibilityThreshold = 0.001f)
    private val jiggleUpSpec = spring(dampingRatio = 0.40f, stiffness = 190f, visibilityThreshold = 0.001f)
    private val pressAnimation = Animatable(0f, 0.001f)
    private val jiggleAnimation = Animatable(0f, 0.001f)
    private val rippleAnimation = Animatable(1f, 0.001f)

    /** 0f at rest, animating to 1f while pressed. Read in draw/effect/layer blocks. */
    val pressProgress: Float get() = pressAnimation.value

    /** Smooth velocity-driven water-droplet wobble (starts from 0 with zero frame-0 snap). */
    val jiggleProgress: Float get() = jiggleAnimation.value

    /** Expanding water ripple ring (0f at touch down → 1f faded out). */
    val rippleProgress: Float get() = rippleAnimation.value

    /** Local-space touch point used as the centre of the press glow & water ripple. */
    var touchPosition by mutableStateOf(Offset.Zero)
        private set

    private fun triggerJiggle(velocity: Float, spec: androidx.compose.animation.core.SpringSpec<Float>) {
        animationScope.launch {
            // Velocity-driven from current position (no snapTo) so motion begins seamlessly from rest.
            jiggleAnimation.animateTo(0f, spec, initialVelocity = velocity)
        }
    }

    private fun triggerRipple() {
        animationScope.launch {
            rippleAnimation.snapTo(0f)
            rippleAnimation.animateTo(1f, tween(durationMillis = 620, easing = FastOutSlowInEasing))
        }
    }

    suspend fun detectPress(pointer: PointerInputScope) =
        with(pointer) {
            inspectDragGestures(
                onDragStart = { down ->
                    touchPosition = down.position
                    animationScope.launch { pressAnimation.animateTo(1f, pressSpec) }
                    triggerJiggle(6.5f, jiggleDownSpec)
                    triggerRipple()
                },
                onDragEnd = {
                    animationScope.launch { pressAnimation.animateTo(0f, pressSpec) }
                    triggerJiggle(-5.5f, jiggleUpSpec)
                },
                onDragCancel = {
                    animationScope.launch { pressAnimation.animateTo(0f, pressSpec) }
                    triggerJiggle(-3.8f, jiggleUpSpec)
                },
            ) { change, _ ->
                touchPosition = change.position
            }
        }
}

@Composable
fun rememberGlassInteraction(): GlassInteraction {
    val scope = rememberCoroutineScope()
    return remember(scope) { GlassInteraction(scope) }
}

/**
 * Water-drop jelly jiggle modifier (volume-preserving X/Y squash & stretch oscillation on
 * press and release). Uses observe-only pointer inspection so clicks & drags stay intact,
 * and works across every theme (Liquid Glass, Light/Dark/AMOLED, and Comic Pop).
 */
@Composable
fun Modifier.waterDropJiggle(amplitude: Float = 0.042f): Modifier {
    val interaction = rememberGlassInteraction()
    val isDark = LocalIsDarkTheme.current
    val isComic = LocalComicTheme.current
    val accent = MaterialTheme.colorScheme.primary
    return this
        .graphicsLayer {
            val wobble = interaction.jiggleProgress
            val press = interaction.pressProgress
            scaleX = 1f + press * (amplitude * 0.45f) + wobble * amplitude
            scaleY = 1f + press * (amplitude * 0.35f) - wobble * (amplitude * 0.88f)
        }
        .drawWithContent {
            drawContent()
            val press = interaction.pressProgress
            if (!isComic && press > 0.01f && size.minDimension > 1f) {
                val touch = interaction.touchPosition
                val spotRadius = (size.minDimension * 0.68f).coerceAtLeast(1f)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (if (isDark) 0.22f else 0.16f) * press),
                            Color.Transparent,
                        ),
                        center = touch,
                        radius = spotRadius,
                    ),
                )
            }
            val ripple = interaction.rippleProgress
            if (ripple < 1f) {
                val touch = interaction.touchPosition
                val maxR = size.minDimension * 0.52f
                val r = lerp(size.minDimension * 0.14f, maxR, ripple)
                val fade = (1f - ripple) * (1f - ripple)
                val ringColor = when {
                    isComic -> accent
                    isDark -> Color.White
                    else -> accent
                }
                drawCircle(
                    color = ringColor.copy(alpha = (if (isDark) 0.34f else 0.28f) * fade),
                    radius = r,
                    center = touch,
                    style = Stroke(width = lerp(2.dp.toPx(), 0.6.dp.toPx(), ripple)),
                )
            }
        }
        .pointerInput(interaction) { interaction.detectPress(this) }
}

/**
 * Shared frosted-glass recipe. Single source of truth for every liquid-glass
 * surface: nav bar, dock, FAB, mini-player, player controls, pills and icon
 * buttons all funnel through [drawInteractiveGlass], so the frost level is
 * defined here once and cannot drift between the two [liquidGlass] overloads.
 */
/** Shared by the backdrop glass and the liquid nav bar so the two engines frost alike. */
internal object GlassRecipe {
    const val BLUR_BASE_DP = 12f
    const val BLUR_BRIGHT_DP = 20f
    const val BLUR_DARK_DP = 6f

    const val BRIGHTNESS_DARK = 0.05f
    const val BRIGHTNESS_LIGHT = 0.07f
    const val CONTRAST = 1.08f
    const val SATURATION_FROSTED = 1.26f
    const val SATURATION_CLEAR = 1.92f

    const val LENS_DIVISOR = 3f
    const val DISPERSION_THRESHOLD = 0.45f

    const val MIN_SCRIM = 0.14f
    const val MAX_SCRIM = 0.48f

    const val PRESS_BLUR_DP = 2.5f
    const val PRESS_REFRACTION_DP = 5f
}

/**
 * Interactive liquid glass drawing modifier.
 * Element MUST be a sibling of the backdrop source, never inside it.
 */
fun Modifier.drawInteractiveGlass(
    isDark: Boolean,
    backdrop: Backdrop,
    luminanceAnimation: Float,
    shape: Shape,
    interaction: GlassInteraction?,
    pressedScale: Float = 1.12f,
    highlight: Highlight = Highlight.Default,
    blurScale: Float = 1f,
    minScrim: Float = GlassRecipe.MIN_SCRIM,
    maxScrim: Float = GlassRecipe.MAX_SCRIM,
    tintColor: Color = Color.Unspecified,
): Modifier =
    this
        .drawBackdrop(
            backdrop = backdrop,
            shape = { lensSafeGlassShape(shape) },
            highlight = { highlight },
            effects = {
                val l = (luminanceAnimation * 2f - 1f).let { sign(it) * it * it }
                val press = interaction?.pressProgress ?: 0f
                val intensity = LiquidGlassIntensity.value
                colorControls(
                    brightness = if (isDark) GlassRecipe.BRIGHTNESS_DARK else GlassRecipe.BRIGHTNESS_LIGHT,
                    contrast = GlassRecipe.CONTRAST,
                    saturation = LiquidGlassIntensity.saturation(
                        GlassRecipe.SATURATION_FROSTED,
                        GlassRecipe.SATURATION_CLEAR,
                    ) + 0.15f * press,
                )
                blur(
                    (
                        if (l > 0f) {
                            lerp(
                                GlassRecipe.BLUR_BASE_DP.dp.toPx(),
                                GlassRecipe.BLUR_BRIGHT_DP.dp.toPx(),
                                l,
                            )
                        } else {
                            lerp(
                                GlassRecipe.BLUR_BASE_DP.dp.toPx(),
                                GlassRecipe.BLUR_DARK_DP.dp.toPx(),
                                -l,
                            )
                        }
                    ) * blurScale * LiquidGlassIntensity.blur() + GlassRecipe.PRESS_BLUR_DP.dp.toPx() * press,
                )
                if (!GlassMotion.isSheetMoving()) {
                    val pressRefractionPx = GlassRecipe.PRESS_REFRACTION_DP.dp.toPx() * press
                    lens(
                        size.minDimension / GlassRecipe.LENS_DIVISOR * intensity + pressRefractionPx,
                        size.minDimension * 0.62f + pressRefractionPx,
                        intensity >= GlassRecipe.DISPERSION_THRESHOLD || press > 0.05f,
                    )
                }
            },
            onDrawBackdrop = { drawBackdrop ->
                drawBackdrop()
            },
            onDrawSurface = {
                val intensity = LiquidGlassIntensity.value
                val press = interaction?.pressProgress ?: 0f
                val darken = lerp(minScrim, maxScrim, ((luminanceAnimation - 0.3f) / 0.5f).coerceIn(0f, 1f))
                drawRect((if (isDark) Color.Black else Color.White).copy(alpha = LiquidGlassIntensity.tint(darken)))
                if (tintColor.isSpecified) {
                    val washAlpha = (if (isDark) 0.14f else 0.10f) * lerp(0.65f, 1.15f, intensity)
                    drawRect(tintColor.copy(alpha = washAlpha))
                }

                // Convex top-left specular catch (shifts subtly with gravity tilt) and lower shade.
                val tiltX = GlassTilt.x
                val tiltY = GlassTilt.y
                val specColor = if (tintColor.isSpecified) {
                    androidx.compose.ui.graphics.lerp(Color.White, tintColor, 0.28f)
                } else {
                    Color.White
                }
                val sheenStart = Offset(
                    size.width * (0.04f + tiltX * 0.26f),
                    size.height * (0.04f + tiltY * 0.20f),
                )
                val sheenEnd = Offset(
                    size.width * (0.72f + tiltX * 0.24f),
                    size.height * (0.85f + tiltY * 0.18f),
                )
                drawRect(
                    brush = Brush.linearGradient(
                        0f to specColor.copy(alpha = if (isDark) lerp(0.09f, 0.20f, intensity) else lerp(0.24f, 0.42f, intensity)),
                        0.46f to Color.Transparent,
                        start = sheenStart,
                        end = sheenEnd,
                    ),
                )
                if ((abs(tiltX) > 0.04f || abs(tiltY) > 0.04f) && size.minDimension > 1f) {
                    val streakX = size.width * (0.5f + tiltX * 0.45f)
                    val streakY = size.height * (0.5f + tiltY * 0.45f)
                    val glintAlpha = ((abs(tiltX) + abs(tiltY)) * 0.5f).coerceIn(0f, 1f) *
                        (if (isDark) 0.16f else 0.24f) * lerp(0.6f, 1.15f, intensity)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(specColor.copy(alpha = glintAlpha), Color.Transparent),
                            center = Offset(streakX, streakY),
                            radius = (size.maxDimension * 0.65f).coerceAtLeast(1f),
                        ),
                        blendMode = BlendMode.Plus,
                    )
                }
                drawRect(
                    brush = Brush.verticalGradient(
                        0.58f to Color.Transparent,
                        1f to Color.Black.copy(alpha = if (isDark) 0.16f else 0.09f),
                    ),
                )

                // Directional inner rim (plus crisp outer dark hairline in Light mode so white
                // glass pops cleanly off bright pages, after Auris' ConvxOuterDarkRim).
                val rimBoost = 1f + 0.34f * press
                val rimOutline = lensSafeGlassShape(shape).createOutline(size, layoutDirection, this)
                if (!isDark) {
                    drawOutline(
                        outline = rimOutline,
                        color = Color.Black.copy(alpha = 0.12f),
                        style = Stroke(width = 1.6.dp.toPx()),
                    )
                }
                val touchRatioX = if (press > 0.01f && size.width > 0f) {
                    ((interaction?.touchPosition?.x ?: (size.width * 0.5f)) / size.width - 0.5f).coerceIn(-0.5f, 0.5f) * press
                } else 0f
                val touchRatioY = if (press > 0.01f && size.height > 0f) {
                    ((interaction?.touchPosition?.y ?: (size.height * 0.5f)) / size.height - 0.5f).coerceIn(-0.5f, 0.5f) * press
                } else 0f
                val rimStart = Offset(
                    size.width * (0.14f + tiltX * 0.22f + touchRatioX * 0.35f).coerceIn(0f, 1f),
                    size.height * (0.12f + tiltY * 0.22f + touchRatioY * 0.35f).coerceIn(0f, 1f),
                )
                val rimEnd = Offset(
                    size.width * (0.88f + tiltX * 0.12f - touchRatioX * 0.25f).coerceIn(0f, 1f),
                    size.height * (0.90f + tiltY * 0.12f - touchRatioY * 0.25f).coerceIn(0f, 1f),
                )
                drawOutline(
                    outline = rimOutline,
                    brush = Brush.linearGradient(
                        0f to specColor.copy(alpha = ((if (isDark) 0.65f else 0.95f) * rimBoost).coerceAtMost(1f)),
                        0.45f to Color.White.copy(alpha = if (isDark) 0.08f else 0.18f),
                        1f to specColor.copy(alpha = ((if (isDark) 0.30f else 0.48f) * rimBoost).coerceAtMost(1f)),
                        start = rimStart,
                        end = rimEnd,
                    ),
                    style = Stroke(width = 1.1.dp.toPx()),
                )

                // Double-wave expanding specular water ripple ring from touch point.
                val ripple = interaction?.rippleProgress ?: 1f
                if (ripple < 1f) {
                    val touch = interaction?.touchPosition ?: Offset(size.width / 2f, size.height / 2f)
                    val maxR = maxOf(size.width, size.height) * 0.95f
                    val r = lerp(size.minDimension * 0.14f, maxR, ripple)
                    val fade = (1f - ripple) * (1f - ripple)
                    val ringAlpha = (if (isDark) 0.55f else 0.78f) * fade
                    drawCircle(
                        color = specColor.copy(alpha = ringAlpha),
                        radius = r,
                        center = touch,
                        style = Stroke(width = lerp(2.4.dp.toPx(), 0.7.dp.toPx(), ripple)),
                        blendMode = BlendMode.Plus,
                    )
                    drawCircle(
                        color = specColor.copy(alpha = ringAlpha * 0.45f),
                        radius = r * 0.72f,
                        center = touch,
                        style = Stroke(width = lerp(1.4.dp.toPx(), 0.45.dp.toPx(), ripple)),
                        blendMode = BlendMode.Plus,
                    )
                }

                // Multi-layer liquid specular spotlight + edge flare tracking the touch point.
                if (press > 0f && size.minDimension > 1f) {
                    val touch = interaction?.touchPosition ?: Offset(size.width / 2f, size.height / 2f)
                    val innerR = (size.minDimension * 0.34f).coerceAtLeast(14.dp.toPx()).coerceAtLeast(1f)
                    val midR = (size.minDimension * 0.72f).coerceAtLeast(1f)
                    val wideR = (size.minDimension * 1.45f).coerceAtLeast(1f)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (if (isDark) 0.40f else 0.48f) * press),
                                specColor.copy(alpha = (if (isDark) 0.14f else 0.20f) * press),
                                Color.Transparent,
                            ),
                            center = touch,
                            radius = innerR,
                        ),
                        blendMode = BlendMode.Plus,
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (if (isDark) 0.24f else 0.30f) * press),
                                Color.Transparent,
                            ),
                            center = touch,
                            radius = midR,
                        ),
                        blendMode = BlendMode.Plus,
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (if (isDark) 0.12f else 0.16f) * press),
                                Color.Transparent,
                            ),
                            center = touch,
                            radius = wideR,
                        ),
                        blendMode = BlendMode.Plus,
                    )
                    drawOutline(
                        outline = rimOutline,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (if (isDark) 0.72f else 0.90f) * press),
                                if (intensity >= GlassRecipe.DISPERSION_THRESHOLD) {
                                    Color(0xFF7BE8FF).copy(alpha = 0.28f * press)
                                } else {
                                    specColor.copy(alpha = 0.22f * press)
                                },
                                Color.Transparent,
                            ),
                            center = touch,
                            radius = (size.minDimension * 0.92f).coerceAtLeast(1f),
                        ),
                        style = Stroke(width = 1.6.dp.toPx()),
                        blendMode = BlendMode.Plus,
                    )
                }
            },
            layerBlock =
                if (interaction != null) {
                    {
                        val baseScale = lerp(1f, pressedScale, interaction.pressProgress)
                        val wideDamp = if (size.width > size.height * 2f) 0.55f else 1f
                        val wobble = interaction.jiggleProgress * wideDamp
                        scaleX = baseScale + wobble * 0.044f
                        scaleY = baseScale - wobble * 0.038f
                    }
                } else {
                    null
                },
        ).then(
            if (interaction != null) {
                Modifier.pointerInput(interaction) { interaction.detectPress(this) }
            } else {
                Modifier
            },
        )

/**
 * Top app bar glass. The pill recipe above is tuned for small floating
 * controls (heavy 18-56% scrim, press scaling); stretched across a
 * full-width bar it read as a flat tinted slab. Bars want what iOS/visionOS
 * bars do: a deep frost so text stays legible over busy covers, boosted
 * saturation so the colours behind glow through (vibrancy), a light tint
 * instead of a scrim and a soft sheen on top. The pane fades into the page
 * at the bottom rather than ending on an edge. Static: no pointer input, no animation, no per-frame
 * state, so it costs one blur pass like the nav bar.
 *
 * Element MUST be a sibling of the backdrop source, never inside it.
 */
@Composable
fun Modifier.headerLiquidGlass(
    backdrop: Backdrop,
    shape: Shape,
    /**
     * Fade the pane in and out. Applied inside the pane's own bounds: a caller's
     * graphicsLayer around it is only header-high, and while its alpha is below 1
     * it clipped the fade strip below the header to a hard edge.
     */
    alpha: (() -> Float)? = null,
): Modifier {
    if (!LocalLiquidGlass.current || !isLiquidGlassBackdropSupported()) return this
    val isDark = LocalIsDarkTheme.current
    val fadePx = with(LocalDensity.current) { HeaderGlassFade.roundToPx() }
    // The pane ends in a fade instead of a rim, shadow and lens edge (those drew a hard
    // line under every header). The fade must sit *below* the header: fading inside it
    // left its lower part (titles, filter chips) over sharp, unblurred content. So the
    // pane is laid out fadePx taller than the header and fades over that extra strip.
    // The fade is a mask over the whole pane in its own layer, not drawn inside kyant's
    // callbacks: kyant records the backdrop padded by the blur radius, so a mask sized
    // from onDrawBackdrop's `size` slid below the pane and the blur ended on a hard cut.
    return this
        .layout { measurable, constraints ->
            val pane = measurable.measure(constraints.offset(vertical = fadePx))
            layout(pane.width, (pane.height - fadePx).coerceAtLeast(0)) { pane.place(0, 0) }
        }
        .graphicsLayer {
            if (alpha != null) this.alpha = alpha()
            compositingStrategy = CompositingStrategy.Offscreen
        }
        .drawWithContent {
            drawContent()
            val fadeFrom = ((size.height - fadePx) / size.height).coerceIn(0f, 1f)
            drawRect(Brush.verticalGradient(fadeFrom to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
        }
        .drawBackdrop(
            backdrop = backdrop,
            shape = { lensSafeGlassShape(shape) },
            highlight = null,
            shadow = null,
            effects = {
                colorControls(
                    brightness = if (isDark) HeaderGlassRecipe.BRIGHTNESS_DARK else HeaderGlassRecipe.BRIGHTNESS_LIGHT,
                    contrast = 1f,
                    saturation = LiquidGlassIntensity.saturation(HeaderGlassRecipe.SATURATION_FROSTED, HeaderGlassRecipe.SATURATION_CLEAR),
                )
                blur(HeaderGlassRecipe.BLUR_DP.dp.toPx() * LiquidGlassIntensity.blur())
            },
            onDrawSurface = {
                drawRect(
                    (if (isDark) Color.Black else Color.White).copy(
                        alpha = LiquidGlassIntensity.tint(if (isDark) HeaderGlassRecipe.TINT_DARK else HeaderGlassRecipe.TINT_LIGHT),
                    ),
                )
                // Sheen: light catching the top of the pane.
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = if (isDark) 0.07f else 0.16f),
                        0.55f to Color.Transparent,
                    ),
                )
            },
        )
        .layout { measurable, constraints ->
            val header = measurable.measure(constraints.offset(vertical = -fadePx))
            layout(header.width, header.height + fadePx) { header.place(0, 0) }
        }
}

private object HeaderGlassRecipe {
    const val BLUR_DP = 24f
    const val SATURATION_FROSTED = 1.3f
    const val SATURATION_CLEAR = 1.9f
    const val BRIGHTNESS_DARK = 0f
    const val BRIGHTNESS_LIGHT = 0.04f
    const val TINT_DARK = 0.30f
    const val TINT_LIGHT = 0.46f
}

/** Height of the strip below a glass header where the pane melts into the page. */
val HeaderGlassFade = 32.dp

/**
 * lens-safe shape guard: kyant's lens effect (used by every interactive
 * glass surface) only accepts corner-based shapes — its SDF shader throws
 * "Only RoundedRectangularShape or CornerBasedShape is supported" for a
 * plain [RectangleShape], which took the render pass down with it (the
 * MD3 navigation bar's glass chrome). Square shapes map to a zero-radius
 * [RoundedCornerShape]: identical rectangle visuals, valid lens input.
 */
internal fun lensSafeGlassShape(shape: Shape): Shape =
    if (shape == RectangleShape) RoundedCornerShape(0.dp) else shape

/** Interactive liquidGlass: static surfaces use fixed mid-luminance. */
@Composable
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    shape: Shape = CircleShape,
    interactive: Boolean = true,
    highlight: Highlight = Highlight.Default,
    forceDark: Boolean = false,
    tintColor: Color = LocalLiquidGlassTint.current,
): Modifier {
    if (!LocalLiquidGlass.current) {
        return this
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f))
    }
    if (!isLiquidGlassBackdropSupported()) {
        return this
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f))
    }
    val isDark = forceDark || LocalIsDarkTheme.current
    val interaction = rememberGlassInteraction()
    return this.drawInteractiveGlass(
        isDark = isDark,
        backdrop = backdrop,
        luminanceAnimation = 0.5f,
        shape = shape,
        interaction = if (interactive) interaction else null,
        highlight = highlight,
        tintColor = tintColor,
    )
}

/** Interactive liquidGlass overload for luminance-sampling surfaces (MiniPlayer, nav capsule). */
@Composable
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    luminanceAnimation: Float,
    shape: Shape = CircleShape,
    interactive: Boolean = true,
    blurScale: Float = 1f,
    minScrim: Float = GlassRecipe.MIN_SCRIM,
    maxScrim: Float = GlassRecipe.MAX_SCRIM,
    forceDark: Boolean = false,
    tintColor: Color = LocalLiquidGlassTint.current,
): Modifier {
    if (!LocalLiquidGlass.current || !isLiquidGlassBackdropSupported()) {
        return this
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f))
    }
    val isDark = forceDark || LocalIsDarkTheme.current
    val interaction = rememberGlassInteraction()
    return this.drawInteractiveGlass(
        isDark = isDark,
        backdrop = backdrop,
        luminanceAnimation = luminanceAnimation,
        shape = shape,
        interaction = if (interactive) interaction else null,
        pressedScale = 1.04f,
        blurScale = blurScale,
        minScrim = minScrim,
        maxScrim = maxScrim,
        tintColor = tintColor,
    )
}

@Composable
fun LiquidGlassContainer(
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    interactive: Boolean = true,
    highlight: Highlight = Highlight.Default,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    if (backdrop == null || !isLiquidGlassBackdropSupported()) {
        Box(
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f)),
            contentAlignment = contentAlignment,
            content = content,
        )
        return
    }
    Box(
        modifier = modifier.liquidGlass(backdrop, shape, interactive, highlight),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/** Floating action pill hosting icon buttons in a liquid glass shell. */
@Composable
fun LiquidGlassActionPill(
    backdrop: Backdrop?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable RowScope.() -> Unit,
) {
    if (backdrop == null || !isLiquidGlassBackdropSupported()) {
        Row(
            modifier = modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.8f)),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        return
    }
    Row(
        modifier = modifier.liquidGlass(backdrop, shape, true, Highlight.Default),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** LiquidGlassIconButton: ImageVector version, small circles use narrow highlight. */
@Composable
fun LiquidGlassIconButton(
    backdrop: Backdrop?,
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(48.dp),
    shape: Shape = CircleShape,
    tint: Color = Color.White,
    interactive: Boolean = true,
    highlight: Highlight = Highlight(width = 1.dp),
) {
    LiquidGlassContainer(
        backdrop = backdrop,
        modifier = modifier,
        shape = shape,
        interactive = interactive,
        highlight = highlight,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp).clickable(onClick = onClick),
        )
    }
}

/** Compat overload: Painter version (existing LastWave call sites). */
@Composable
fun LiquidGlassIconButton(
    backdrop: Backdrop?,
    painter: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.size(48.dp),
    shape: Shape = CircleShape,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    contentDescription: String? = null,
) {
    LiquidGlassContainer(
        backdrop = backdrop,
        modifier = modifier,
        shape = shape,
        interactive = true,
        highlight = Highlight(width = 1.dp),
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp).clickable(onClick = onClick),
        )
    }
}

// ── Compat shims: keep old symbols compiling, route to interactive liquid glass ──

/** Kept for call-site compat; values are ignored — recipe is fixed. */
enum class LiquidGlassPreset {
    BottomNavigation,
    MiniPlayer,
    PlayerControls,
    FloatingControls,
    Header,
    ModalSheet,
    ContextMenu,
    Overlay,
    Card,
}

private fun LiquidGlassPreset.isGlassTarget(): Boolean = when (this) {
    LiquidGlassPreset.BottomNavigation,
    LiquidGlassPreset.MiniPlayer,
    LiquidGlassPreset.PlayerControls,
    LiquidGlassPreset.FloatingControls,
    LiquidGlassPreset.Header -> true
    LiquidGlassPreset.ModalSheet,
    LiquidGlassPreset.ContextMenu,
    LiquidGlassPreset.Overlay,
    LiquidGlassPreset.Card -> false
}

/**
 * Compat shim: old liquidGlassChrome now delegates to drawInteractiveGlass
 * for allowed targets only; all other presets return unmodified (opaque fallback).
 */
@Composable
fun Modifier.liquidGlassChrome(
    shape: Shape,
    enabled: Boolean = true,
    preset: LiquidGlassPreset = LiquidGlassPreset.Card,
    backdrop: Backdrop? = LocalLiquidGlassBackdrop.current,
    interactionSource: MutableInteractionSource? = null,
    exportedBackdrop: LayerBackdrop? = null,
    ambientMotion: Boolean = false,
    contentBrightness: Float = 0f,
    onPointerPosition: ((Offset?) -> Unit)? = null,
    forceDark: Boolean = false,
): Modifier {
    if (!enabled || !preset.isGlassTarget() || backdrop == null || !isLiquidGlassBackdropSupported()) {
        return this
    }
    // Now Playing's controls follow the app's light or dark mode like the rest of the
    // player: forced dark glass under light mode's dark ink hid the icons.
    return this.liquidGlass(
        backdrop = backdrop,
        shape = shape,
        interactive = preset != LiquidGlassPreset.BottomNavigation && preset != LiquidGlassPreset.Header,
        highlight = Highlight.Default,
        forceDark = forceDark,
    )
}

/**
 * Crash-free stub: old BackdropBlur did a full-screen 36dp sibling blur inside
 * its own capture (self-capture loop). Now draws only a veil, no backdrop.
 */
@Composable
fun BackdropBlur(
    radius: Dp,
    modifier: Modifier = Modifier,
    veil: Color = Color.Unspecified,
    veilAlpha: Float = 0.74f,
    content: @Composable BoxScope.() -> Unit,
) {
    val resolvedVeil = if (veil == Color.Unspecified) MaterialTheme.colorScheme.surface else veil
    Box(modifier) {
        Box(Modifier.matchParentSize(), content = content)
        Box(Modifier.matchParentSize().background(resolvedVeil.copy(alpha = veilAlpha)))
    }
}

/**
 * Observe-only drag/press recogniser (Kyant catalog DragGestureInspector).
 * Never consumes events, so glass reacts while wrapped buttons keep their taps.
 */
internal suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val down = awaitFirstDown(requireUnconsumed = false)
        onDragStart(down)
        onDrag(down, Offset.Zero)
        val upEvent = drag(
            pointerId = down.id,
            onDrag = { onDrag(it, it.positionChange()) },
        )
        if (upEvent == null) {
            onDragCancel()
        } else {
            onDragEnd(upEvent)
        }
    }
}

private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): PointerInputChange? {
    val isPointerUp = currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true
    if (isPointerUp) {
        return null
    }
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) ?: return null
        if (change.isConsumed) {
            return null
        }
        if (change.changedToUpIgnoreConsumed()) {
            return change
        }
        onDrag(change)
        pointer = change.id
    }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(pointerId: PointerId): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                return dragEvent
            } else {
                pointer = otherDown.id
            }
        } else {
            val hasDragged = dragEvent.previousPosition != dragEvent.position
            if (hasDragged) {
                return dragEvent
            }
        }
    }
}

/**
 * The lit edge the mini player's glass gets from its specular highlight. Glass chrome without
 * that highlight (the liquid dock, its search and tab circles, the Home circle) gets this so
 * every floating control carries the same rim.
 */
internal fun Modifier.glassRim(shape: Shape = CircleShape): Modifier = border(
    1.dp,
    androidx.compose.ui.graphics.Brush.verticalGradient(
        listOf(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.55f), androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f)),
    ),
    shape,
)
