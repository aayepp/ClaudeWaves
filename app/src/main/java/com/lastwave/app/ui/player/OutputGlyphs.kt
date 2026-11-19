package com.lastwave.app.ui.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Filled glyphs for the now-playing output button, one per route the player
 * can tell apart (24x24 grid, single colour, tinted by the Icon). Android
 * reports Bluetooth and wired routes without the device's form factor, so
 * every Bluetooth route reads as earbuds and every wired one as an IEM.
 */
internal object OutputGlyphs {

    private val ink = SolidColor(Color.Black)

    private fun glyph(name: String, fill: String, stroke: String? = null) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(PathParser().parsePathString(fill).toNodes(), pathFillType = PathFillType.EvenOdd, fill = ink)
            if (stroke != null) {
                addPath(
                    PathParser().parsePathString(stroke).toNodes(),
                    stroke = ink,
                    strokeLineWidth = 1.9f,
                    strokeLineCap = StrokeCap.Round,
                )
            }
        }.build()

    /** Phone with its speaker grille, sound leaving to the right. */
    val PhoneSpeaker: ImageVector by lazy {
        glyph(
            "PhoneSpeaker",
            "M6.5 2h7A2.5 2.5 0 0 1 16 4.5v15a2.5 2.5 0 0 1-2.5 2.5h-7A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" +
                "M10 14a2.4 2.4 0 1 0 0 4.8a2.4 2.4 0 0 0 0-4.8z" +
                "M10 6.5a1.2 1.2 0 1 0 0 2.4a1.2 1.2 0 0 0 0-2.4z",
            "M18.2 9.2a3.8 3.8 0 0 1 0 5.6M20.6 7a7 7 0 0 1 0 10",
        )
    }

    /** A pair of stemmed earbuds. */
    val Earbuds: ImageVector by lazy {
        glyph(
            "Earbuds",
            "M8 3a3.5 3.5 0 0 0-3.5 3.5V9a3.5 3.5 0 0 0 2.3 3.3v7.2a1.2 1.2 0 0 0 2.4 0v-7.2A3.5 3.5 0 0 0 11.5 9V6.5A3.5 3.5 0 0 0 8 3z" +
                "M16 3a3.5 3.5 0 0 1 3.5 3.5V9a3.5 3.5 0 0 1-2.3 3.3v7.2a1.2 1.2 0 0 1-2.4 0v-7.2A3.5 3.5 0 0 1 12.5 9V6.5A3.5 3.5 0 0 1 16 3z",
        )
    }

    /** An in-ear monitor shell with its cable and plug. */
    val Iem: ImageVector by lazy {
        glyph(
            "Iem",
            "M5.5 3.6c3.4-1.7 8-0.6 9.1 2.8c1 3.2-1.2 6.6-4.6 7.1c-3.3 0.5-5.9-1.3-6.4-4.5c-0.4-2.4 0.1-4.4 1.9-5.4z" +
                "M17 19.6h2a0.7 0.7 0 0 1 0.7 0.7v1.6a0.7 0.7 0 0 1-0.7 0.7h-2a0.7 0.7 0 0 1-0.7-0.7v-1.6a0.7 0.7 0 0 1 0.7-0.7z",
            "M10.2 13.6c0.2 2.6 2 3.6 4 3.6c2.2 0 3.6 1 3.6 3",
        )
    }

    /** A dongle DAC: body with a volume knob, then the USB plug. */
    val UsbDac: ImageVector by lazy {
        glyph(
            "UsbDac",
            "M4.5 6.5h10a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2h-10a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z" +
                "M9.5 9.3a2.7 2.7 0 1 0 0 5.4a2.7 2.7 0 0 0 0-5.4z" +
                "M16.5 11h2.5v2h-2.5z" +
                "M19.7 9.5h1.6a0.7 0.7 0 0 1 0.7 0.7v3.6a0.7 0.7 0 0 1-0.7 0.7h-1.6a0.7 0.7 0 0 1-0.7-0.7v-3.6a0.7 0.7 0 0 1 0.7-0.7z",
        )
    }
}
