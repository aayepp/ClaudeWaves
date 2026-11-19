package com.lastwave.app.ui.shell

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate

/** Dock tabs whose icon moves from the inside as the tab is picked. */
internal enum class TabGlyph { HOME, STATS, PLAYLISTS }

/**
 * A dock icon drawn on Material's 24-unit grid so at rest it matches the stock icon,
 * animated by [play] (0 at rest, 1 at the height of the tap motion, read at draw time):
 * Home's door swings shut and open again, Stats' bars dip and rise in turn, Playlists'
 * rows slide in while the note hops.
 */
@Composable
internal fun AnimatedTabGlyph(glyph: TabGlyph, play: () -> Float, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val p = play().coerceIn(-0.3f, 1f)
        val unit = size.minDimension / 24f
        translate((size.width - unit * 24f) / 2f, (size.height - unit * 24f) / 2f) {
            when (glyph) {
                TabGlyph.HOME -> home(unit, p, tint)
                TabGlyph.STATS -> stats(unit, p, tint)
                TabGlyph.PLAYLISTS -> playlists(unit, p, tint)
            }
        }
    }
}

private fun DrawScope.home(u: Float, p: Float, tint: Color) {
    // Material "home": the door is the gap at x 10..14, y 14..20.
    val house = Path().apply {
        moveTo(10 * u, 20 * u); lineTo(10 * u, 14 * u); lineTo(14 * u, 14 * u); lineTo(14 * u, 20 * u)
        lineTo(19 * u, 20 * u); lineTo(19 * u, 12 * u); lineTo(22 * u, 12 * u); lineTo(12 * u, 3 * u)
        lineTo(2 * u, 12 * u); lineTo(5 * u, 12 * u); lineTo(5 * u, 20 * u); close()
    }
    drawPath(house, tint)
    // The door leaf, hinged on the left, swings shut and back open.
    val leaf = 4 * u * p.coerceIn(0f, 1f)
    if (leaf > 0f) drawRect(tint.copy(alpha = tint.alpha * 0.75f), Offset(10 * u, 14 * u), Size(leaf, 6 * u))
}

private fun DrawScope.stats(u: Float, p: Float, tint: Color) {
    // Three bars, tallest in the middle; each dips by its own amount and springs back.
    val bars = listOf(Triple(2.5f, 11f, 1f), Triple(9f, 4f, 0.6f), Triple(15.5f, 8f, 0.85f))
    for ((x, top, give) in bars) {
        val height = (21f - top) * (1f - 0.55f * p * give)
        drawRoundRect(tint, Offset(x * u, (21f - height) * u), Size(6f * u, height * u), CornerRadius(u, u))
    }
}

private fun DrawScope.playlists(u: Float, p: Float, tint: Color) {
    // Material "queue_music": three rows and a note; rows slide in from the left in turn.
    val rows = listOf(Triple(6f, 12f, 0f), Triple(10f, 12f, 0.25f), Triple(14f, 8f, 0.5f))
    for ((y, width, lag) in rows) {
        val shift = -3f * u * (p * (1f - lag)).coerceAtLeast(0f)
        drawRect(tint, Offset(3 * u + shift, y * u), Size(width * u, 2 * u))
    }
    val hop = -2.5f * u * p
    rotate(-12f * p, pivot = Offset(16 * u, 17 * u)) {
        translate(top = hop) {
            drawRect(tint, Offset(17 * u, 6 * u), Size(2 * u, 11 * u))
            drawRect(tint, Offset(17 * u, 6 * u), Size(5 * u, 2 * u))
            drawCircle(tint, radius = 3 * u, center = Offset(16 * u, 17 * u))
        }
    }
}
