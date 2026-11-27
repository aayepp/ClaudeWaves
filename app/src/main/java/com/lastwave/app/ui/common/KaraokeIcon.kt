package com.lastwave.app.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Stage ball-head karaoke microphone with sparkles for Lyrics Mode.
 * Uses pure monochrome SolidColor fills so [androidx.compose.material3.Icon] tinting
 * works cleanly across Light, Dark, AMOLED, Comic Pop, Sakuga, and Glass themes.
 */
val KaraokeIcon: ImageVector by lazy {
    val fill = SolidColor(Color.Black)
    ImageVector.Builder(
        name = "KaraokeIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Upper dome of tilted ball-head grille (center (15.8, 8.2), r = 4.5, gap along equator)
        path(fill = fill, pathFillType = PathFillType.NonZero) {
            moveTo(13.07f, 4.62f)
            arcTo(
                horizontalEllipseRadius = 4.5f,
                verticalEllipseRadius = 4.5f,
                theta = 0f,
                isMoreThanHalf = false,
                isPositiveArc = true,
                x1 = 19.38f,
                y1 = 10.93f,
            )
            close()
        }
        // Lower dome of tilted ball-head grille
        path(fill = fill, pathFillType = PathFillType.NonZero) {
            moveTo(18.53f, 11.78f)
            arcTo(
                horizontalEllipseRadius = 4.5f,
                verticalEllipseRadius = 4.5f,
                theta = 0f,
                isMoreThanHalf = false,
                isPositiveArc = true,
                x1 = 12.22f,
                y1 = 5.47f,
            )
            close()
        }
        // Tapered diagonal handheld grip with rounded bottom cap
        path(fill = fill, pathFillType = PathFillType.NonZero) {
            moveTo(11.4f, 9.9f)
            lineTo(4.49f, 17.89f)
            arcTo(
                horizontalEllipseRadius = 1.15f,
                verticalEllipseRadius = 1.15f,
                theta = 0f,
                isMoreThanHalf = false,
                isPositiveArc = false,
                x1 = 6.11f,
                y1 = 19.51f,
            )
            lineTo(14.1f, 12.6f)
            close()
        }
        // Vocal-removal sparkles around mic
        path(fill = fill) { addSparkle(cx = 6.2f, cy = 6.0f, r = 2.7f) }
        path(fill = fill) { addSparkle(cx = 3.8f, cy = 11.2f, r = 1.6f) }
        path(fill = fill) { addSparkle(cx = 19.2f, cy = 16.6f, r = 1.9f) }
    }.build()
}

private fun PathBuilder.addSparkle(cx: Float, cy: Float, r: Float) {
    moveTo(cx, cy - r)
    quadTo(cx, cy, cx + r, cy)
    quadTo(cx, cy, cx, cy + r)
    quadTo(cx, cy, cx - r, cy)
    quadTo(cx, cy, cx, cy - r)
    close()
}
