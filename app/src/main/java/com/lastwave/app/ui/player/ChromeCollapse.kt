package com.lastwave.app.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Decides compact vs expanded chrome from list scroll deltas. Direction must hold for
 * [thresholdPx] before the state flips, so fling tails and finger jitter never toggle it.
 */
internal class ScrollCollapseTracker(private val thresholdPx: Float) {
    var collapsed = false
        private set
    private var travel = 0f

    /**
     * [consumedY] is what the list (and anything nested in it) scrolled, negative towards
     * the list end. [availableY] is what nobody consumed: positive means the list is
     * already at its top, which always expands.
     */
    fun onScroll(consumedY: Float, availableY: Float): Boolean {
        if (availableY > 0f) return reset()
        if (consumedY == 0f) return collapsed
        if (travel * consumedY < 0f) travel = 0f
        travel += consumedY
        if (abs(travel) >= thresholdPx) {
            collapsed = travel < 0f
            travel = 0f
        }
        return collapsed
    }

    fun reset(): Boolean {
        travel = 0f
        collapsed = false
        return false
    }
}

/**
 * Scroll-driven collapse of the nav bar + mini player. Everything reads the single
 * [progress] (0 expanded, 1 compact) from layout/draw lambdas, never in composition.
 */
@Stable
internal class ChromeCollapseState(thresholdPx: Float) {
    private val tracker = ScrollCollapseTracker(thresholdPx)
    internal val animatable = Animatable(0f)
    internal var collapsed by mutableStateOf(false)
        private set

    val progress: () -> Float = { animatable.value }

    val connection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            collapsed = tracker.onScroll(consumed.y, available.y)
            return Offset.Zero
        }
    }

    fun expand() {
        collapsed = tracker.reset()
    }
}

internal val LocalChromeCollapse = staticCompositionLocalOf<ChromeCollapseState?> { null }

// Critically damped (Auris-style folding bar): crisp response with zero overshoot.
private val ChromeCollapseSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 280f,
    visibilityThreshold = 0.001f,
)

/** [enabled] is false off the main tabs; the chrome then snaps back to expanded. */
@Composable
internal fun rememberChromeCollapseState(enabled: Boolean): ChromeCollapseState {
    val thresholdPx = with(LocalDensity.current) { 20.dp.toPx() }
    val state = remember { ChromeCollapseState(thresholdPx) }
    LaunchedEffect(state, enabled) {
        state.expand()
        if (!enabled) {
            state.animatable.snapTo(0f)
            return@LaunchedEffect
        }
        snapshotFlow { state.collapsed }.collect { collapsed ->
            // A new animateTo preempts the running one and starts from its velocity.
            launch { state.animatable.animateTo(if (collapsed) 1f else 0f, ChromeCollapseSpec) }
        }
    }
    return state
}

// Compact row geometry, shared by the nav pill and the mini player that docks beside it.
internal val CompactPillSize = 56.dp
internal val CompactRowStartMargin = 14.dp
internal val CompactRowBottomMargin = 12.dp
internal val CompactRowGap = 8.dp

private fun navBarFade(progress: Float) = (progress / 0.5f).coerceIn(0f, 1f)

/**
 * The full bar shrinks towards its start edge and fades out over the first half.
 * Once invisible it is left unplaced, so the compact player row under it takes the touches.
 */
internal fun Modifier.collapsingNavBar(progress: () -> Float): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val pivotX = if (layoutDirection == LayoutDirection.Ltr) 0f else 1f
        layout(placeable.width, placeable.height) {
            if (navBarFade(progress()) < 1f) {
                placeable.placeWithLayer(0, 0) {
                    val t = navBarFade(progress())
                    alpha = 1f - t
                    scaleX = lerp(1f, 0.7f, t)
                    scaleY = scaleX
                    transformOrigin = TransformOrigin(pivotX, 1f)
                }
            }
        }
    }

/** Settings' "Home Button on Right": the Home circle and docked tab sit at the right edge. */
internal val LocalHomeOnRight = staticCompositionLocalOf { false }

/** Updates extra horizontal width added while the Home hold pill expands (`0dp .. 84dp`). */
internal val LocalSetHomePillExtraWidth = staticCompositionLocalOf<(Dp) -> Unit> { {} }

/**
 * Positions and sizes the compact nav pill; it grows in as the bar fades and is unplaced while
 * expanded. Callers align it to the bottom start or end per [LocalHomeOnRight].
 */
@Composable
internal fun Modifier.collapsedNavPill(progress: () -> Float): Modifier = this
    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
    .then(
        if (LocalHomeOnRight.current) Modifier.padding(end = CompactRowStartMargin, bottom = CompactRowBottomMargin)
        else Modifier.padding(start = CompactRowStartMargin, bottom = CompactRowBottomMargin),
    )
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            if (progress() > 0f) {
                placeable.placeWithLayer(0, 0) {
                    val t = ((progress() - 0.3f) / 0.5f).coerceIn(0f, 1f)
                    alpha = t
                    scaleX = lerp(0.6f, 1f, t)
                    scaleY = scaleX
                }
            }
        }
    }
    .height(CompactPillSize)
    .widthIn(min = CompactPillSize)

/**
 * Moves the mini player into the collapsed nav row, beside the pill. [lift] is its expanded
 * gap above the bottom inset. Done in layout, not as a draw translation, so the card's touch
 * bounds and the bounds the full player morphs from follow it.
 */
internal fun Modifier.dockBesidePill(
    lift: Dp,
    progress: () -> Float,
    pillOnRight: Boolean = false,
    extraPillWidth: () -> Dp = { 0.dp },
    elastic: Boolean = false,
): Modifier =
    layout { measurable, constraints ->
        val p = progress()
        val extraDp = extraPillWidth()
        val shiftX = ((CompactPillSize + extraDp + CompactRowGap).toPx() * p).roundToInt()
        val shiftY = ((lift - CompactRowBottomMargin).toPx() * p).roundToInt()
        val placeable = measurable.measure(constraints.offset(horizontal = -shiftX))
        // Reporting less height than measured drops the card by shiftY; only the empty
        // bottom padding hangs below the screen edge.
        val height = (placeable.height - shiftY).coerceAtLeast(0)
        val extraFrac = (extraDp.value / 84f).coerceIn(0f, 1f)
        fun androidx.compose.ui.layout.Placeable.PlacementScope.placeDockable(x: Int) {
            if (elastic && extraFrac > 0.001f) {
                placeable.placeRelativeWithLayer(x, 0) {
                    val bounce = kotlin.math.sin(extraFrac * kotlin.math.PI.toFloat())
                    scaleY = 1f - 0.03f * bounce
                }
            } else {
                placeable.placeRelative(x, 0)
            }
        }
        if (!constraints.hasBoundedWidth) {
            layout(placeable.width + shiftX, height) { placeDockable(if (pillOnRight) 0 else shiftX) }
        } else if (pillOnRight) {
            val width = constraints.maxWidth
            val x = minOf((width - placeable.width) / 2, width - placeable.width - shiftX)
            layout(width, height) { placeDockable(x) }
        } else {
            // Take the full width and keep the card centred; only move it when the
            // centred card would sit under the pill. A width-capped card (tablets)
            // already clears the pill, so it no longer jumps sideways on collapse.
            val width = constraints.maxWidth
            val x = maxOf((width - placeable.width) / 2, shiftX)
            layout(width, height) { placeDockable(x) }
        }
    }

/** Draw-phase scale towards [compact]; the control keeps its laid-out touch target. */
internal fun Modifier.collapseScale(compact: Float, progress: () -> Float): Modifier =
    graphicsLayer {
        scaleX = lerp(1f, compact, progress())
        scaleY = scaleX
    }

/** Square box whose side follows [progress]; measured in the layout phase only. */
internal fun Modifier.collapseSquare(expanded: Dp, compact: Dp, progress: () -> Float): Modifier =
    layout { measurable, _ ->
        val side = lerp(expanded.toPx(), compact.toPx(), progress()).roundToInt()
        val placeable = measurable.measure(Constraints.fixed(side, side))
        layout(side, side) { placeable.place(0, 0) }
    }

/** Fixed height that follows [progress]. */
internal fun Modifier.collapseHeight(expanded: Dp, compact: Dp, progress: () -> Float): Modifier =
    layout { measurable, constraints ->
        val height = lerp(expanded.toPx(), compact.toPx(), progress()).roundToInt()
        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
        layout(placeable.width, height) { placeable.place(0, 0) }
    }

/** Equal top and bottom padding that follows [progress]. */
internal fun Modifier.collapseVerticalPadding(expanded: Dp, compact: Dp, progress: () -> Float): Modifier =
    layout { measurable, constraints ->
        val pad = lerp(expanded.toPx(), compact.toPx(), progress()).roundToInt()
        val placeable = measurable.measure(constraints.offset(vertical = -2 * pad))
        layout(placeable.width, constraints.constrainHeight(placeable.height + 2 * pad)) {
            placeable.place(0, pad)
        }
    }
