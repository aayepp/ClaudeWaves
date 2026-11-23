package com.lastwave.app.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.data.lyrics.LyricLine
import com.lastwave.app.playback.PlaybackProgressState
import com.lastwave.app.ui.theme.PlayerPalette
import kotlinx.coroutines.flow.StateFlow

/** Height the three preview lines take. */
internal val LYRIC_PREVIEW_HEIGHT = 78.dp

/** The same three lines in smaller type, for short landscape panes (car head units). */
internal val LYRIC_PREVIEW_COMPACT_HEIGHT = 60.dp
private const val PREVIEW_LINES = 3

/**
 * Stacks lines top-down from the current one and stops at the first that would not fit
 * the given height, then centres what fits. A wrapped line pushes the last dim line out
 * whole instead of the block growing or a line being half clipped.
 */
@Composable
private fun FittingLines(gap: Dp, content: @Composable () -> Unit) {
    Layout(content, Modifier.fillMaxWidth()) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val free = constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
        val placed = mutableListOf<Placeable>()
        var used = 0
        for (measurable in measurables) {
            val p = measurable.measure(free)
            val next = used + (if (placed.isEmpty()) 0 else gapPx) + p.height
            // The current line always shows, even if it alone is taller than the box.
            if (placed.isNotEmpty() && constraints.hasBoundedHeight && next > constraints.maxHeight) break
            placed += p
            used = next
        }
        val height = if (constraints.hasBoundedHeight) constraints.maxHeight else used
        layout(constraints.maxWidth, height) {
            var y = ((height - used) / 2).coerceAtLeast(0)
            for (p in placed) {
                p.placeRelative((constraints.maxWidth - p.width) / 2, y)
                y += p.height + gapPx
            }
        }
    }
}

/**
 * Three synced lyric lines in Now Playing: previous, current (bright), and next
 * dimmed, stepping up as the song moves on. Tap opens Lyrics. The block is centred
 * both ways in the space it is given, so it sits centered between artwork and controls.
 */
@Composable
internal fun NowPlayingLyricsPreview(
    lines: List<LyricLine>,
    progressState: StateFlow<PlaybackProgressState>,
    onOpenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val progress by progressState.collectAsStateWithLifecycle()
    // Recompose on a new line, not on every ~60 ms position tick.
    val current by remember(lines) {
        derivedStateOf { lines.indexOfLast { it.timeMs <= progress.positionMs } }
    }
    val currentStyle = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall
    val otherStyle = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
    Box(
        modifier = modifier.clickable(onClick = onOpenLyrics),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (slideInVertically(tween(280)) { it / 4 } + fadeIn(tween(220))) togetherWith
                    (slideOutVertically(tween(280)) { -it / 4 } + fadeOut(tween(180)))
            },
            label = "nowPlayingLyricsPreview",
        ) { active ->
            val first = when {
                lines.size <= PREVIEW_LINES -> 0
                active <= 0 -> 0
                active >= lines.lastIndex -> (lines.size - PREVIEW_LINES).coerceAtLeast(0)
                else -> active - 1
            }
            FittingLines(gap = if (compact) 1.dp else 3.dp) {
                for (i in first until minOf(first + PREVIEW_LINES, lines.size)) {
                    val line = lines.getOrNull(i) ?: continue
                    val isCurrent = i == active
                    val base = if (isCurrent) currentStyle else otherStyle
                    BasicText(
                        text = line.text.ifBlank { "♪" },
                        style = base.copy(
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = PlayerPalette.ink.copy(alpha = if (isCurrent) 1f else 0.48f),
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = base.fontSize * 0.85f,
                            maxFontSize = base.fontSize,
                            stepSize = 0.5.sp,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
