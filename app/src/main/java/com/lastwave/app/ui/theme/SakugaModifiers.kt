package com.lastwave.app.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Volume-preserving squash-and-stretch press feedback for Anime Sakuga surfaces & rows.
 * Plain at rest; squashes wide under finger and overshoots with pink accent wedges on release.
 */
object SakugaIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        SakugaIndicationNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = 0x5A406A
}

private class SakugaIndicationNode(
    private val source: InteractionSource,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private val squashY = Animatable(1f)
    private val wedgeProgress = Animatable(1f)
    private var origin = Offset.Unspecified

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> {
                        origin = interaction.pressPosition
                        launch { squashY.animateTo(0.88f, spring(stiffness = 1400f, dampingRatio = 0.78f)) }
                    }
                    is PressInteraction.Release -> {
                        launch {
                            squashY.snapTo(minOf(squashY.value, 0.90f))
                            squashY.animateTo(1f, spring(dampingRatio = 0.38f, stiffness = 560f))
                        }
                        launch {
                            wedgeProgress.snapTo(0f)
                            wedgeProgress.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 420f))
                        }
                    }
                    is PressInteraction.Cancel -> launch {
                        squashY.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 560f))
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        if (!currentValueOf(LocalSakugaTheme)) {
            drawContent()
            return
        }
        val wideRow = size.width > 280.dp.toPx()
        val sy = if (wideRow) 1f - (1f - squashY.value) * 0.42f else squashY.value
        val sx = 1f / sqrt(sy.coerceIn(0.65f, 1.35f))
        if (sy != 1f) {
            scale(scaleX = sx, scaleY = sy) { this@draw.drawContent() }
        } else {
            drawContent()
        }

        val p = wedgeProgress.value
        if (p < 0.98f) {
            val pal = currentValueOf(LocalSakugaPalette)
            val c = if (origin.isSpecified) origin else center
            val r0 = 10.dp.toPx() + 24.dp.toPx() * p
            val len = 10.dp.toPx() * (1f - p)
            if (len > 0.5f) {
                for (i in 0 until 5) {
                    val a = ((i * 72f + 18f) * (PI / 180.0)).toFloat()
                    val dir = Offset(cos(a), sin(a))
                    drawLine(
                        color = if (i % 2 == 0) pal.accent else pal.line,
                        start = c + dir * r0,
                        end = c + dir * (r0 + len),
                        strokeWidth = 2.6.dp.toPx() * (1f - p * 0.5f),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

/**
 * Port of sakuga-vfx `input/input.js`: focus kicks three accent wedges from the top-right corner,
 * and every typed character bounces the field slightly while kicking up cel dust puffs (and occasional
 * crystal chips) directly beneath the measured caret position.
 *
 * No-op outside Anime Sakuga mode.
 */
@Composable
fun Modifier.sakugaTypingInput(
    text: String,
    textStyle: TextStyle,
    isPassword: Boolean = false,
    leadingPadding: Dp = 16.dp,
): Modifier = if (!LocalSakugaTheme.current) {
    this
} else {
    composed {
        val fx = LocalSakugaFx.current
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        val scope = rememberCoroutineScope()
        val scaleAnim = remember { Animatable(1f) }
        var fieldBounds by remember { mutableStateOf<Rect?>(null) }
        var isFocused by remember { mutableStateOf(false) }
        var initialized by remember { mutableStateOf(false) }
        var lastLen by remember { mutableStateOf(text.length) }

        LaunchedEffect(text) {
            if (!initialized) {
                initialized = true
                lastLen = text.length
                return@LaunchedEffect
            }
            if (text.length == lastLen) return@LaunchedEffect
            lastLen = text.length
            launch {
                scaleAnim.snapTo(0.985f)
                scaleAnim.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 700f))
            }
            val bounds = fieldBounds ?: return@LaunchedEffect
            val leadPx = with(density) { leadingPadding.toPx() }
            val trailPadPx = with(density) { 42.dp.toPx() }
            val dustYOffset = with(density) { 10.dp.toPx() }
            val measuredText = if (isPassword) "•".repeat(text.length) else text
            val measuredPx = if (measuredText.isEmpty()) {
                0f
            } else {
                runCatching {
                    textMeasurer.measure(
                        text = measuredText,
                        style = textStyle,
                        maxLines = 1,
                        softWrap = false,
                    ).size.width.toFloat()
                }.getOrDefault(0f)
            }
            val maxSpan = (bounds.width - leadPx - trailPadPx).coerceAtLeast(0f)
            val caretX = bounds.left + leadPx + measuredPx.coerceIn(0f, maxSpan)
            val caretY = bounds.center.y + dustYOffset
            fx?.emitTypingDust(Offset(caretX, caretY))
        }

        this
            .onGloballyPositioned { coords ->
                if (coords.isAttached) fieldBounds = coords.boundsInRoot()
            }
            .onFocusChanged { focusState ->
                if (focusState.isFocused && !isFocused) {
                    isFocused = true
                    fieldBounds?.let { b ->
                        fx?.emitTypingFocus(Offset(b.right - with(density) { 8.dp.toPx() }, b.top + with(density) { 4.dp.toPx() }))
                    }
                    scope.launch {
                        scaleAnim.snapTo(1.035f)
                        scaleAnim.animateTo(1f, spring(dampingRatio = 0.48f, stiffness = 700f))
                    }
                } else if (!focusState.isFocused) {
                    isFocused = false
                }
            }
            .graphicsLayer {
                val s = scaleAnim.value
                scaleX = s
                scaleY = s
            }
    }
}
