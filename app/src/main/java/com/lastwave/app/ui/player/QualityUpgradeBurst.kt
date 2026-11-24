package com.lastwave.app.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.lastwave.app.playback.AudioQualityTier
import com.lastwave.app.ui.theme.ComicInk
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/** How long an upgrade burst stays on screen. */
private const val BURST_MS = 1400

private fun AudioQualityTier.burstFill(): Color = if (this == AudioQualityTier.HI_RES) Color(0xFFFFD60A) else Color(0xFF3DD6F5)

private fun AudioQualityTier.burstLabel(): String = if (this == AudioQualityTier.HI_RES) "HI-RES!" else "LOSSLESS!"

/**
 * The mini player and Now Playing both watch for an upgrade, but the phone
 * should tap only once for it.
 */
private object TierBurstLatch {
    private var last: String? = null
    fun claim(key: String): Boolean {
        if (key == last) return false
        last = key
        return true
    }
}

/**
 * The tier a song just climbed to, held for [BURST_MS]; null otherwise.
 * Fast start opens on a lossy stream and switches up once the better one is
 * ready: this marks that moment. A song that opens lossless sets the baseline
 * without bursting, and a downgrade never bursts.
 */
@Composable
internal fun rememberTierUpgradeBurst(trackKey: String?, tier: AudioQualityTier?): AudioQualityTier? {
    var baseline by remember(trackKey) { mutableStateOf<AudioQualityTier?>(null) }
    var burst by remember(trackKey) { mutableStateOf<AudioQualityTier?>(null) }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(trackKey, tier) {
        if (trackKey == null || tier == null) return@LaunchedEffect
        val from = baseline
        baseline = tier
        if (from == null || tier <= from || tier == AudioQualityTier.STANDARD) return@LaunchedEffect
        burst = tier
        if (TierBurstLatch.claim("$trackKey|$tier")) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        try {
            delay(BURST_MS.toLong() + 100L)
        } finally {
            if (burst == tier) burst = null
        }
    }
    return burst
}

/**
 * The upgrade moment on Now Playing: a stamp that pops over the quality pill
 * with action lines flying out, then fades back to the pill itself.
 */
@Composable
internal fun TierUpgradeBurst(
    tier: AudioQualityTier?,
    trackKey: String? = null,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(1f) }
    var shown by remember { mutableStateOf<AudioQualityTier?>(null) }
    LaunchedEffect(trackKey, tier) {
        if (tier == null) {
            shown = null
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        shown = tier
        progress.snapTo(0f)
        try {
            progress.animateTo(1f, tween(BURST_MS, easing = LinearEasing))
        } finally {
            shown = null
        }
    }
    val current = shown ?: return
    val p = progress.value
    if (p >= 1f) return

    // Pop past full size, settle, then fade over the last fifth.
    val scale = when {
        p < 0.15f -> 0.4f + 0.75f * (p / 0.15f)
        p < 0.3f -> 1.15f - 0.15f * ((p - 0.15f) / 0.15f)
        else -> 1f
    }
    val alpha = if (p > 0.8f) 1f - (p - 0.8f) / 0.2f else 1f
    val fill = current.burstFill()
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = modifier
            .wrapContentSize(unbounded = true)
            .graphicsLayer { this.alpha = alpha }
            .drawBehind {
                val fly = (p / 0.5f).coerceIn(0f, 1f)
                if (fly >= 1f) return@drawBehind
                val inner = size.minDimension * 0.55f + 34.dp.toPx() * fly
                val length = 14.dp.toPx() * (1f - fly)
                for (line in 0 until 8) {
                    val angle = Math.toRadians(line * 45.0 + 22.5)
                    val direction = Offset(cos(angle).toFloat() * 1.6f, sin(angle).toFloat())
                    val start = center + direction * inner
                    val end = center + direction * (inner + length)
                    drawLine(ComicInk, start, end, strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
                    drawLine(fill, start, end, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = current.burstLabel(),
            style = MaterialTheme.typography.titleLarge,
            color = ComicInk,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = -5f
                }
                .background(fill, shape)
                .border(2.5.dp, ComicInk, shape)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}
