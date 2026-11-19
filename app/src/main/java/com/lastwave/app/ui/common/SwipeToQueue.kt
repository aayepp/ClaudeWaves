package com.lastwave.app.ui.common

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.lastwave.app.playback.PlayableTrack
import com.lastwave.app.ui.player.LocalMusicPlayer
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Swipe a song row toward the end edge to add it to the queue, like Spotify.
 * The row springs back either way; past the threshold a tick confirms it will
 * queue on release. [track] is built only when the swipe commits.
 */
@Composable
fun SwipeToQueue(
    track: (() -> PlayableTrack)?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    content: @Composable () -> Unit,
) {
    val player = LocalMusicPlayer.current
    if (track == null) {
        Box(modifier) { content() }
        return
    }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val dir = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val thresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val currentTrack by rememberUpdatedState(track)

    Box(
        modifier.pointerInput(dir, thresholdPx) {
            var armed = false
            detectHorizontalDragGestures(
                onDragStart = { armed = false },
                onHorizontalDrag = { change, amount ->
                    val pulled = offset.value
                    // Past the threshold the row resists, so it can't fly off.
                    val step = amount * dir * if (pulled > thresholdPx) 0.35f else 1f
                    if (pulled + step <= 0f && pulled <= 0f) return@detectHorizontalDragGestures
                    change.consume()
                    val next = (pulled + step).coerceAtLeast(0f)
                    scope.launch { offset.snapTo(next) }
                    if ((next >= thresholdPx) != armed) {
                        armed = next >= thresholdPx
                        if (armed) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    }
                },
                onDragEnd = {
                    if (armed) {
                        player.addToQueue(currentTrack())
                        Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
                    }
                    scope.launch { offset.animateTo(0f) }
                },
                onDragCancel = { scope.launch { offset.animateTo(0f) } },
            )
        },
    ) {
        val progress = (offset.value / thresholdPx).coerceIn(0f, 1f)
        if (offset.value > 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f + 0.65f * progress)),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .size(24.dp)
                        .graphicsLayer {
                            val s = 0.6f + 0.4f * progress
                            scaleX = s
                            scaleY = s
                        },
                )
            }
        }
        Box(Modifier.offset { IntOffset((offset.value * dir).roundToInt(), 0) }) { content() }
    }
}
