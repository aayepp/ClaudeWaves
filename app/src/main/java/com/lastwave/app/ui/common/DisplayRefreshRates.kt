package com.lastwave.app.ui.common

import android.view.Display
import kotlin.math.abs
import kotlin.math.roundToInt

// Ported from BitChord (GPL-3.0), ui/performance/DisplayRefreshRates.kt.

private const val MIN_SELECTABLE_REFRESH_RATE = 50
private const val MAX_SELECTABLE_REFRESH_RATE = 240

/** "Auto" in Settings: ask for the panel's fastest rate, as the app always did. */
const val REFRESH_RATE_AUTO = 0

/** Refresh rates this display can actually select, rounded for labels (60, 90, 120). */
fun Display?.selectableRefreshRates(): List<Int> {
    val fallback = this?.refreshRate?.roundToInt()?.coerceAtLeast(MIN_SELECTABLE_REFRESH_RATE) ?: 60
    return this?.supportedModes
        ?.asSequence()
        ?.map { it.refreshRate.roundToInt() }
        ?.filter { it in MIN_SELECTABLE_REFRESH_RATE..MAX_SELECTABLE_REFRESH_RATE }
        ?.distinct()
        ?.sorted()
        ?.toList()
        ?.ifEmpty { listOf(fallback) }
        ?: listOf(fallback)
}

/** Maps a saved rate to one this display has, so a backup restored on another phone still resolves. */
fun Display?.resolveRefreshRate(preferred: Int): Int =
    selectableRefreshRates().minBy { abs(it - preferred) }
