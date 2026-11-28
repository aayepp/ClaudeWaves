package com.lastwave.app.ui.shell

import kotlin.math.roundToInt

/** The lens follows the finger continuously, stopping at the first/last tab centre. */
internal fun liquidNavigationPosition(x: Float, width: Float, itemCount: Int): Float {
    if (itemCount <= 1 || width <= 0f || !x.isFinite() || !width.isFinite()) return 0f
    return (x / (width / itemCount) - 0.5f).coerceIn(0f, (itemCount - 1).toFloat())
}

/** Physical finger position without quantizing to a tab or clamping at its centre. */
internal fun liquidNavigationRawPosition(x: Float, width: Float, itemCount: Int): Float {
    if (itemCount <= 1 || width <= 0f || !x.isFinite() || !width.isFinite()) return 0f
    return x / (width / itemCount) - 0.5f
}

/** Small elastic travel beyond the end tabs, with no magnetic regions between them. */
internal fun liquidNavigationRubberBand(position: Float, itemCount: Int): Float {
    if (itemCount <= 1 || !position.isFinite()) return 0f
    val last = (itemCount - 1).toFloat()
    val limit = 0.18f
    return when {
        position < 0f -> -limit * (1f - 1f / (1f - position / limit))
        position > last -> last + limit * (1f - 1f / (1f + (position - last) / limit))
        else -> position
    }
}

/** Convert a physical left-to-right lens position into the row's logical tab index. */
internal fun liquidNavigationIndex(position: Float, itemCount: Int, isRtl: Boolean): Int {
    if (itemCount <= 1) return 0
    val physicalIndex = if (position.isFinite()) position.roundToInt().coerceIn(0, itemCount - 1) else 0
    return if (isRtl) itemCount - 1 - physicalIndex else physicalIndex
}

internal fun liquidNavigationVisualIndex(index: Int, itemCount: Int, isRtl: Boolean): Float {
    if (itemCount <= 1) return 0f
    val logicalIndex = index.coerceIn(0, itemCount - 1)
    return (if (isRtl) itemCount - 1 - logicalIndex else logicalIndex).toFloat()
}

/** Computes physical left-to-right slot origins and widths from logical slot weights. */
internal fun liquidNavigationSlotMetrics(
    slotWeights: List<Float>,
    totalWidth: Float,
    isRtl: Boolean,
): Pair<FloatArray, FloatArray> {
    val n = slotWeights.size
    if (n == 0 || totalWidth <= 0f || !totalWidth.isFinite()) {
        return FloatArray(n) to FloatArray(n)
    }
    val ordered = FloatArray(n) { i ->
        val logicalIdx = if (isRtl) n - 1 - i else i
        slotWeights[logicalIdx].takeIf { it.isFinite() && it > 0f } ?: 1f
    }
    val weightSum = ordered.sum().coerceAtLeast(1f)
    val lefts = FloatArray(n)
    val widths = FloatArray(n)
    var x = 0f
    for (i in 0 until n) {
        val w = totalWidth * ordered[i] / weightSum
        lefts[i] = x
        widths[i] = w
        x += w
    }
    return lefts to widths
}

/** Maps a physical finger X coordinate into continuous slot space for weighted slots. */
internal fun liquidNavigationRawPositionWeighted(
    x: Float,
    slotLefts: FloatArray,
    slotWidths: FloatArray,
): Float {
    val n = minOf(slotLefts.size, slotWidths.size)
    if (n <= 1 || !x.isFinite()) return 0f
    for (i in 0 until n) {
        val left = slotLefts[i]
        val w = slotWidths[i]
        if (w <= 0f) continue
        if (x < left + w || i == n - 1) {
            return i - 0.5f + (x - left) / w
        }
    }
    return 0f
}

/** Interpolates left edge and width for the pill at continuous visual [position]. */
internal fun liquidNavigationContinuousLeftAndWidth(
    position: Float,
    slotLefts: FloatArray,
    slotWidths: FloatArray,
): Pair<Float, Float> {
    val n = minOf(slotLefts.size, slotWidths.size)
    if (n == 0) return 0f to 0f
    if (n == 1 || !position.isFinite()) return slotLefts[0] to slotWidths[0]
    val p = position
    return when {
        p <= 0f -> (slotLefts[0] + p * slotWidths[0]) to slotWidths[0]
        p >= n - 1 -> {
            val last = n - 1
            (slotLefts[last] + (p - last) * slotWidths[last]) to slotWidths[last]
        }
        else -> {
            val idx = p.toInt().coerceIn(0, n - 2)
            val frac = p - idx
            val left = slotLefts[idx] + (slotLefts[idx + 1] - slotLefts[idx]) * frac
            val width = slotWidths[idx] + (slotWidths[idx + 1] - slotWidths[idx]) * frac
            left to width
        }
    }
}
