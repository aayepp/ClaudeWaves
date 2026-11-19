package com.lastwave.app.ui.common

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Drag-to-reorder for a LazyColumn. The dragged item is drawn where the finger
 * put it (start + drag − where the list placed it now), so swaps, neighbour
 * animations and edge auto-scroll never make it jump.
 */
@Stable
class LazyListReorder internal constructor(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    private val edgePx: Float,
    private val canDropOn: (Any) -> Boolean,
    private val onMove: (from: Any, to: Any) -> Unit,
    private val haptics: HapticFeedback,
    private val onEnd: () -> Unit,
) {
    var draggingKey by mutableStateOf<Any?>(null)
        private set
    private var startTop = 0f
    private var delta by mutableFloatStateOf(0f)
    private var autoScroll: Job? = null

    private fun dragged(): LazyListItemInfo? =
        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggingKey }

    /** Translation for the dragged item; read it in graphicsLayer. */
    val offset: Float
        get() = dragged()?.let { startTop + delta - it.offset } ?: 0f

    fun start(key: Any) {
        val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        draggingKey = key
        startTop = info.offset.toFloat()
        delta = 0f
    }

    fun drag(dy: Float) {
        if (draggingKey == null) return
        delta += dy
        swapIfPastNeighbour()
        if (autoScroll?.isActive != true && edgeStep() != 0f) {
            autoScroll = scope.launch {
                while (draggingKey != null) {
                    val step = edgeStep()
                    if (step == 0f) break
                    listState.scrollBy(step)
                    swapIfPastNeighbour()
                    withFrameNanos { }
                }
            }
        }
    }

    fun end() {
        if (draggingKey == null) return
        autoScroll?.cancel()
        draggingKey = null
        delta = 0f
        onEnd()
    }

    /**
     * Swap once the dragged centre crosses a neighbour's centre. Comparing against
     * the neighbour's centre (not its whole span) keeps unequal heights from
     * swapping back and forth on the same spot.
     */
    private fun swapIfPastNeighbour() {
        val info = dragged() ?: return
        val visible = listState.layoutInfo.visibleItemsInfo
        val centre = startTop + delta + info.size / 2f
        val next = visible.firstOrNull { it.index == info.index + 1 }
        val prev = visible.firstOrNull { it.index == info.index - 1 }
        val target = when {
            next != null && canDropOn(next.key) && centre > next.offset + next.size / 2f -> next
            prev != null && canDropOn(prev.key) && centre < prev.offset + prev.size / 2f -> prev
            else -> return
        }
        // LazyColumn keeps its first visible item in place by key; moving that item
        // would drag the whole list along with it.
        if (info.index == listState.firstVisibleItemIndex || target.index == listState.firstVisibleItemIndex) {
            listState.requestScrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        }
        onMove(info.key, target.key)
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    /** Pixels to scroll this frame: grows the deeper the item sits in an edge zone. */
    private fun edgeStep(): Float {
        val info = dragged() ?: return 0f
        val layout = listState.layoutInfo
        val top = startTop + delta
        val bottom = top + info.size
        val bottomEdge = layout.viewportEndOffset - layout.afterContentPadding - edgePx
        val topEdge = edgePx
        val depth = when {
            bottom > bottomEdge && listState.canScrollForward -> bottom - bottomEdge
            top < topEdge && listState.canScrollBackward -> top - topEdge
            else -> 0f
        }
        return (depth / 4f).coerceIn(-24f, 24f)
    }
}

@Composable
fun rememberLazyListReorder(
    listState: LazyListState,
    canDropOn: (Any) -> Boolean,
    onMove: (from: Any, to: Any) -> Unit,
    onEnd: () -> Unit,
): LazyListReorder {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val edgePx = with(LocalDensity.current) { 56.dp.toPx() }
    val drop by rememberUpdatedState(canDropOn)
    val move by rememberUpdatedState(onMove)
    val end by rememberUpdatedState(onEnd)
    return remember(listState) {
        LazyListReorder(
            listState = listState,
            scope = scope,
            edgePx = edgePx,
            canDropOn = { drop(it) },
            onMove = { from, to -> move(from, to) },
            haptics = haptics,
            onEnd = { end() },
        )
    }
}

/** Put on a drag handle: dragging it moves the item keyed [key]. */
fun Modifier.reorderHandle(reorder: LazyListReorder, key: Any): Modifier =
    pointerInput(reorder, key) {
        detectDragGestures(
            onDragStart = { reorder.start(key) },
            onDrag = { change, amount ->
                change.consume()
                reorder.drag(amount.y)
            },
            onDragEnd = reorder::end,
            onDragCancel = reorder::end,
        )
    }
