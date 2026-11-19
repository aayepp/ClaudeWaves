package com.lastwave.app.ui.theme

import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.indication
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** True while the Comic Pop theme is active. Every comic modifier below is a no-op otherwise. */
val LocalComicTheme = staticCompositionLocalOf { false }

/** Comic Pop's own yellow. Fixed meanings (Hi-Res tier, output routes, burst lines) use it as is. */
val ComicGold = Color(0xFFFFD60A)

/** The skin's spot colour: [ComicGold], or the user's accent when they picked one with Comic Pop on. */
val LocalComicAccent = staticCompositionLocalOf { ComicGold }
val ComicAccent: Color
    @Composable @ReadOnlyComposable get() = LocalComicAccent.current
val ComicInk = Color(0xFF111111)
val ComicCyan = Color(0xFF3DD6F5)
val ComicPink = Color(0xFFFF4D8D)
val ComicMint = Color(0xFF5CE6A0)

/** Below this a 2.5dp line would swallow the element (dots, progress bars, dividers). */
private val MinInkedSize = 24.dp

/** A square-cornered or rectangular shape is a full-bleed pane, not a panel to ink. */
private fun Shape.isPane(density: Density): Boolean {
    if (this === RectangleShape) return true
    if (this !is CornerBasedShape) return false
    val probe = Size(100f, 100f)
    return topStart.toPx(probe, density) == 0f && topEnd.toPx(probe, density) == 0f &&
        bottomStart.toPx(probe, density) == 0f && bottomEnd.toPx(probe, density) == 0f
}

/**
 * Comic panel: a thick ink outline and a hard, unblurred drop shadow.
 * The shadow is ink on paper in light mode and pink on the dark page, where
 * an ink shadow would vanish. [pressed] (0..1) slides the panel onto its
 * shadow, the way a printed button is pushed down.
 */
@Composable
fun Modifier.comicPanel(
    shape: Shape,
    offset: Dp = 4.dp,
    stroke: Dp = 2.5.dp,
    translucent: Boolean = false,
    pressed: () -> Float = { 0f },
): Modifier {
    if (!LocalComicTheme.current) return this
    val line = MaterialTheme.colorScheme.outline
    val shadow = if (LocalIsDarkTheme.current) ComicPink else ComicInk
    // Outline, shadow path and stroke are rebuilt only when the size changes, not on every frame of a scroll.
    return drawWithCache {
        val outline = if (size.minDimension >= MinInkedSize.toPx()) shape.createOutline(size, layoutDirection, this) else null
        val off = offset.toPx()
        // Only cut the panel silhouette out of its shadow for translucent surfaces; opaque panels paint over it directly
        // without allocating a Vulkan StencilAttachment clip path on every frame.
        val cutOut = if (translucent && outline != null && off > 0f) Path().apply { addOutline(outline) } else null
        val ink = Stroke(stroke.toPx())
        onDrawWithContent {
            if (outline == null) {
                drawContent()
                return@onDrawWithContent
            }
            if (off > 0f) {
                if (cutOut != null) {
                    clipPath(cutOut, ClipOp.Difference) {
                        translate(off, off) { drawOutline(outline, shadow) }
                    }
                } else {
                    translate(off, off) { drawOutline(outline, shadow) }
                }
            }
            val push = off * pressed().coerceIn(0f, 1f)
            translate(push, push) {
                this@onDrawWithContent.drawContent()
                drawOutline(outline, line, style = ink)
            }
        }
    }
}

/** Ink outline only, for the many small surfaces inside a panel. Full-bleed panes are left alone. */
@Composable
fun Modifier.comicInk(shape: Shape, fill: Color, stroke: Dp = 2.dp): Modifier {
    if (!LocalComicTheme.current || fill.alpha < 0.08f) return this
    val line = MaterialTheme.colorScheme.outline
    return drawWithCache {
        val outline = if (size.minDimension >= MinInkedSize.toPx() && !shape.isPane(this)) {
            shape.createOutline(size, layoutDirection, this)
        } else null
        val ink = Stroke(stroke.toPx())
        onDrawWithContent {
            drawContent()
            if (outline != null) drawOutline(outline, line, style = ink)
        }
    }
}

private data class HalftoneKey(val widthPx: Int, val radiusQ: Int, val colorBits: ULong)

private object HalftoneBrushCache {
    private val cache = object : LinkedHashMap<HalftoneKey, ShaderBrush>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<HalftoneKey, ShaderBrush>?): Boolean = size > 16
    }

    @Synchronized
    fun getOrCreate(widthPx: Int, radiusPx: Float, color: Color, build: () -> ShaderBrush): ShaderBrush {
        val key = HalftoneKey(widthPx, (radiusPx * 10f).roundToInt(), color.value)
        return cache.getOrPut(key, build)
    }
}

/**
 * A halftone screen as one repeating tile shared across cards via [HalftoneBrushCache].
 */
private fun Density.halftoneBrush(step: Dp, radius: Dp, color: Color): ShaderBrush {
    val width = step.roundToPx().coerceAtLeast(2)
    val r = radius.toPx()
    return HalftoneBrushCache.getOrCreate(width, r, color) {
        val rowHeight = (width * 0.87f).roundToInt().coerceAtLeast(1)
        val tile = ImageBitmap(width, rowHeight * 2)
        val canvas = Canvas(tile)
        val paint = Paint().apply { this.color = color; isAntiAlias = true }
        // Row one sits mid-tile; row two is offset half a step, so it straddles the tile's side edges.
        canvas.drawCircle(Offset(width / 2f, rowHeight / 2f), r, paint)
        canvas.drawCircle(Offset(0f, rowHeight * 1.5f), r, paint)
        canvas.drawCircle(Offset(width.toFloat(), rowHeight * 1.5f), r, paint)
        ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    }
}

/** Halftone dot screen printed over the content. */
@Composable
fun Modifier.comicHalftone(): Modifier {
    if (!LocalComicTheme.current) return this
    val dot = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    return drawWithCache {
        val screen = halftoneBrush(9.dp, 1.8.dp, dot)
        onDrawWithContent {
            drawContent()
            drawRect(screen)
        }
    }
}

/** Feed section headings: the comic display face under Comic Pop, the app's heavy sans otherwise. */
@Composable
fun sectionTitleStyle(): TextStyle =
    if (LocalComicTheme.current) MaterialTheme.typography.titleLarge.copy(fontSize = 23.sp)
    else MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, letterSpacing = (-0.4).sp)

/** Ink colour for nodes that draw outside composition and cannot read the colour scheme. */
val LocalComicLine = staticCompositionLocalOf { ComicInk }

/**
 * Inks the outline of whatever is clipped to [shape]: artwork, avatars, tiles,
 * chips. Reads the theme at draw time, so it works from plain (non-composable)
 * modifier chains.
 */
fun Modifier.comicOutline(shape: Shape): Modifier = this then ComicOutlineElement(shape)

private data class ComicOutlineElement(val shape: Shape) : ModifierNodeElement<ComicOutlineNode>() {
    override fun create() = ComicOutlineNode(shape)
    override fun update(node: ComicOutlineNode) {
        node.shape = shape
    }
}

private class ComicOutlineNode(var shape: Shape) :
    Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private var cachedFor: Shape? = null
    private var cachedSize = Size.Unspecified
    private var cached: Outline? = null
    private var cachedStrokePx = -1f
    private var cachedStroke = Stroke(0f)

    override fun ContentDrawScope.draw() {
        drawContent()
        if (!currentValueOf(LocalComicTheme)) return
        val current = shape
        if (cachedFor != current || cachedSize != size) {
            cachedFor = current
            cachedSize = size
            // Only rounded panels: a custom viewport clip or a square pane is not a panel edge.
            cached = if (current is CornerBasedShape && size.minDimension >= MinInkedSize.toPx() && !current.isPane(this)) {
                current.createOutline(size, layoutDirection, this)
            } else null
        }
        cached?.let {
            val strokePx = 2.dp.toPx()
            if (strokePx != cachedStrokePx) {
                cachedStrokePx = strokePx
                cachedStroke = Stroke(strokePx)
            }
            drawOutline(it, currentValueOf(LocalComicLine), style = cachedStroke)
        }
    }
}

/** The indication for a clickable that had turned feedback off: the comic pop, or still nothing. */
@Composable
fun comicIndicationOrNull(): Indication? = if (LocalComicTheme.current) ComicIndication else null

private val ComicNightPage = Color(0xFF3B2A8C)
private val ComicPaperPage = Color(0xFFFFF6DC)

/**
 * The two colours Now Playing paints with outside the colour scheme: its ink
 * (text, icons, glass tints) and the shade it lays over artwork. They were
 * hard-coded white and black, which pinned the player to a dark look whatever
 * the theme. Snapshot state rather than a composition local, because much of
 * the player reads them from draw code.
 */
object PlayerPalette {
    var ink by mutableStateOf(Color.White)
        internal set
    var shade by mutableStateOf(Color.Black)
        internal set
}

/**
 * Now Playing follows the app's light or dark mode on every theme: dark keeps white on
 * black, light uses the theme's own ink and page (Comic Pop's ink on cream paper).
 */
@Composable
fun NowPlayingPaletteScope(content: @Composable () -> Unit) {
    val light = !LocalIsDarkTheme.current
    val comic = LocalComicTheme.current
    val ink = when {
        !light -> Color.White
        comic -> ComicInk
        else -> MaterialTheme.colorScheme.onSurface
    }
    val shade = when {
        !light -> Color.Black
        comic -> ComicPaperPage
        else -> MaterialTheme.colorScheme.background
    }
    DisposableEffect(ink, shade) {
        PlayerPalette.ink = ink
        PlayerPalette.shade = shade
        onDispose {
            PlayerPalette.ink = Color.White
            PlayerPalette.shade = Color.Black
        }
    }
    content()
}

/**
 * Flat comic page with a halftone screen: purple at night, cream paper by day.
 * [strength] fades it, so it can sit behind the cover on Now Playing and over
 * the whole backdrop on the lyrics and queue tabs.
 */
@Composable
fun Modifier.comicNightWash(strength: () -> Float = { 1f }): Modifier {
    if (!LocalComicTheme.current) return this
    val dark = LocalIsDarkTheme.current
    val page = if (dark) ComicNightPage else ComicPaperPage
    val dot = if (dark) Color.White.copy(alpha = 0.07f) else ComicInk.copy(alpha = 0.08f)
    return drawWithCache {
        val screen = halftoneBrush(11.dp, 2.2.dp, dot)
        onDrawWithContent {
            drawContent()
            val amount = strength().coerceIn(0f, 1f)
            if (amount > 0.001f) {
                drawRect(page.copy(alpha = 0.92f * amount))
                drawRect(screen, alpha = amount)
            }
        }
    }
}

/** Comic press feedback on a component that owns its own [interactionSource] (Material buttons, cards). */
@Composable
fun Modifier.comicPress(interactionSource: InteractionSource): Modifier =
    if (LocalComicTheme.current) indication(interactionSource, ComicIndication) else this

/**
 * Press feedback for the Comic Pop theme: the element squashes under the
 * finger, springs back past its size, and throws a ring of action lines from
 * the touch point on release.
 */
object ComicIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = ComicIndicationNode(interactionSource)
    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = 0x0C011C
}

private const val PRESS_SCALE = 0.93f
private const val BURST_LINES = 8

private class ComicIndicationNode(private val source: InteractionSource) : Modifier.Node(), DrawModifierNode {
    private val squash = Animatable(1f)
    /** 0 at release, 1 when the action lines have flown out; idle at 1. */
    private val burst = Animatable(1f)
    private var origin = Offset.Unspecified

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        origin = interaction.pressPosition
                        launch { squash.animateTo(PRESS_SCALE, spring(stiffness = 1200f)) }
                    }
                    is PressInteraction.Release -> {
                        launch {
                            // A quick tap releases before the squash lands: start the bounce from pressed anyway.
                            squash.snapTo(minOf(squash.value, 0.95f))
                            squash.animateTo(1f, spring(dampingRatio = 0.32f, stiffness = 520f))
                        }
                        launch {
                            burst.snapTo(0f)
                            burst.animateTo(1f, tween(360, easing = FastOutSlowInEasing))
                        }
                    }
                    is PressInteraction.Cancel -> launch { squash.animateTo(1f, spring()) }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        // Wide rows squash half as far: 7% of a full-width row is a lurch, not a pop.
        val depth = if (size.width > 280.dp.toPx()) 0.5f else 1f
        val factor = 1f - (1f - squash.value) * depth
        if (factor != 1f) scale(factor) { this@draw.drawContent() } else drawContent()

        val progress = burst.value
        if (progress >= 1f) return
        val centre = if (origin.isSpecified) origin else center
        val inner = 16.dp.toPx() + 30.dp.toPx() * progress
        val length = 14.dp.toPx() * (1f - progress)
        for (line in 0 until BURST_LINES) {
            val angle = (line * 360f / BURST_LINES + 22.5f) * (Math.PI / 180.0)
            val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
            val start = centre + direction * inner
            val end = centre + direction * (inner + length)
            val popColor = when (line % 3) {
                0 -> ComicGold
                1 -> ComicCyan
                else -> ComicPink
            }
            drawLine(ComicInk, start, end, strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
            drawLine(popColor, start, end, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}
