package com.lastwave.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal enum class SettingsDuotoneGlyph {
    AUDIO_WAVEFORM,
    APPEARANCE_PRISM,
    YOUTUBE_BRAND,
    SPOTIFY_BRAND,
    LASTFM_BRAND,
    LIBRARY_VINYL,
    MODULES_HEX,
    DATA_VAULT,
    CLAUDEWAVES_CREST,
    WIFI_HIRES,
    CELLULAR_BARS,
    HEADPHONES_WAVE,
    DOWNLOAD_TRAY,
    EQ_FADERS,
    USB_DAC_WAVE,
    CROSSFADE_WAVE,
    LIKED_HEART,
    LOCAL_FOLDER_NOTE,
    SMART_MIX_SPARK,
}

@Immutable
internal data class SettingsRolePalette(
    val audio: Color,
    val appearance: Color,
    val youtube: Color,
    val spotify: Color,
    val lastFm: Color,
    val library: Color,
    val modules: Color,
    val data: Color,
    val about: Color,
    val statusConnected: Color,
)

internal object SettingsMedallionColors {
    @Composable
    @ReadOnlyComposable
    fun current(): SettingsRolePalette {
        val isDark = LocalIsDarkTheme.current
        return if (isDark) {
            SettingsRolePalette(
                audio = Color(0xFF7FA8C4),
                appearance = Color(0xFF9B8CF2),
                youtube = Color(0xFFF26B68),
                spotify = Color(0xFF3BD17B),
                lastFm = Color(0xFFE67880),
                library = Color(0xFFEAB364),
                modules = Color(0xFF59C7BE),
                data = Color(0xFFA3B0C2),
                about = MaterialTheme.colorScheme.primary,
                statusConnected = Color(0xFF3BD17B),
            )
        } else {
            SettingsRolePalette(
                audio = Color(0xFF346282),
                appearance = Color(0xFF5A48C2),
                youtube = Color(0xFFC62828),
                spotify = Color(0xFF158446),
                lastFm = Color(0xFFB33942),
                library = Color(0xFFB06D16),
                modules = Color(0xFF1A7A72),
                data = Color(0xFF4D5D73),
                about = MaterialTheme.colorScheme.primary,
                statusConnected = Color(0xFF1B9A52),
            )
        }
    }
}

/** Frosted thin-ring medallion replacing flat solid icon squares across Settings and Library vault tiles. */
@Composable
internal fun SettingsIconMedallion(
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    cornerRadius: Dp = 11.dp,
    content: @Composable BoxScope.(tint: Color) -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val isComic = LocalComicTheme.current
    val shape = RoundedCornerShape(cornerRadius)
    val fillAlpha = when {
        isComic && isDark -> 0.24f
        isComic -> 0.18f
        isDark -> 0.16f
        else -> 0.11f
    }
    val border = if (isComic) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    } else {
        BorderStroke(1.dp, accent.copy(alpha = if (isDark) 0.42f else 0.32f))
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(accent.copy(alpha = fillAlpha))
            .border(border, shape),
        contentAlignment = Alignment.Center,
    ) {
        content(accent)
    }
}

/** Crisp 24×24 virtual-grid duotone vector + brand marks for Settings and Library quick-vault tiles. */
@Composable
internal fun SettingsDuotoneIcon(
    glyph: SettingsDuotoneGlyph,
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp),
) {
    Canvas(modifier = modifier) {
        val unit = size.minDimension / 24f
        val ox = (size.width - unit * 24f) * 0.5f
        val oy = (size.height - unit * 24f) * 0.5f
        val stroke = Stroke(width = 1.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val thinStroke = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val duoFill = tint.copy(alpha = 0.24f)

        fun pt(x: Float, y: Float) = Offset(ox + x * unit, oy + y * unit)

        when (glyph) {
            SettingsDuotoneGlyph.AUDIO_WAVEFORM -> {
                val centerLeft = ox + 10.75f * unit
                val centerTop = oy + 4f * unit
                drawRoundRect(
                    color = duoFill,
                    topLeft = Offset(centerLeft, centerTop),
                    size = Size(2.5f * unit, 16f * unit),
                    cornerRadius = CornerRadius(1.25f * unit, 1.25f * unit),
                )
                val bars = listOf(
                    4f to (9.5f to 14.5f),
                    8f to (6.5f to 17.5f),
                    12f to (4f to 20f),
                    16f to (7f to 17f),
                    20f to (10f to 14f),
                )
                bars.forEach { (x, range) ->
                    drawLine(
                        color = tint,
                        start = pt(x, range.first),
                        end = pt(x, range.second),
                        strokeWidth = 1.85.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }

            SettingsDuotoneGlyph.APPEARANCE_PRISM -> {
                val prism = Path().apply {
                    moveTo(ox + 11.5f * unit, oy + 4.5f * unit)
                    lineTo(ox + 4.2f * unit, oy + 18.2f * unit)
                    lineTo(ox + 18.8f * unit, oy + 18.2f * unit)
                    close()
                }
                drawPath(prism, duoFill, style = Fill)
                drawPath(prism, tint, style = stroke)
                drawLine(tint, pt(3.2f, 11.2f), pt(8.4f, 11.2f), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, pt(14.5f, 10.6f), pt(20.6f, 8.8f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, pt(15.2f, 12.6f), pt(20.8f, 13.4f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(duoFill, radius = 2.1f * unit, center = pt(18.6f, 5.8f))
                drawCircle(tint, radius = 2.1f * unit, center = pt(18.6f, 5.8f), style = thinStroke)
            }

            SettingsDuotoneGlyph.YOUTUBE_BRAND -> {
                val topLeft = pt(3f, 5.5f)
                val boxSize = Size(18f * unit, 13f * unit)
                val corner = CornerRadius(4f * unit, 4f * unit)
                drawRoundRect(duoFill, topLeft = topLeft, size = boxSize, cornerRadius = corner)
                drawRoundRect(tint, topLeft = topLeft, size = boxSize, cornerRadius = corner, style = stroke)
                val play = Path().apply {
                    moveTo(ox + 10.1f * unit, oy + 9.2f * unit)
                    lineTo(ox + 15.6f * unit, oy + 12f * unit)
                    lineTo(ox + 10.1f * unit, oy + 14.8f * unit)
                    close()
                }
                drawPath(play, tint, style = Fill)
            }

            SettingsDuotoneGlyph.SPOTIFY_BRAND -> {
                drawCircle(duoFill, radius = 9.2f * unit, center = pt(12f, 12f))
                drawCircle(tint, radius = 9.2f * unit, center = pt(12f, 12f), style = stroke)
                val arc1 = Path().apply {
                    moveTo(ox + 7.2f * unit, oy + 9.5f * unit)
                    quadraticTo(ox + 12.2f * unit, oy + 7.7f * unit, ox + 17.1f * unit, oy + 10.1f * unit)
                }
                val arc2 = Path().apply {
                    moveTo(ox + 8.1f * unit, oy + 12.3f * unit)
                    quadraticTo(ox + 12.2f * unit, oy + 10.8f * unit, ox + 16.2f * unit, oy + 12.8f * unit)
                }
                val arc3 = Path().apply {
                    moveTo(ox + 9.1f * unit, oy + 15.0f * unit)
                    quadraticTo(ox + 12.2f * unit, oy + 13.9f * unit, ox + 15.1f * unit, oy + 15.4f * unit)
                }
                drawPath(arc1, tint, style = stroke)
                drawPath(arc2, tint, style = Stroke(width = 1.55.dp.toPx(), cap = StrokeCap.Round))
                drawPath(arc3, tint, style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round))
            }

            SettingsDuotoneGlyph.LASTFM_BRAND -> {
                // Scrobble "as" wave motif: closed left "a" loop flowing into crisp "s" curve.
                drawCircle(duoFill, radius = 3.2f * unit, center = pt(7.6f, 12.2f))
                drawCircle(tint, radius = 3.2f * unit, center = pt(7.6f, 12.2f), style = stroke)
                val scrobbleTail = Path().apply {
                    moveTo(ox + 10.8f * unit, oy + 9.4f * unit)
                    lineTo(ox + 10.8f * unit, oy + 14.3f * unit)
                    cubicTo(
                        ox + 10.8f * unit, oy + 15.6f * unit,
                        ox + 12.6f * unit, oy + 15.8f * unit,
                        ox + 13.4f * unit, oy + 14.0f * unit,
                    )
                    cubicTo(
                        ox + 14.3f * unit, oy + 11.4f * unit,
                        ox + 15.4f * unit, oy + 8.8f * unit,
                        ox + 17.6f * unit, oy + 8.8f * unit,
                    )
                    cubicTo(
                        ox + 19.4f * unit, oy + 8.8f * unit,
                        ox + 19.6f * unit, oy + 10.8f * unit,
                        ox + 17.6f * unit, oy + 11.7f * unit,
                    )
                    cubicTo(
                        ox + 15.6f * unit, oy + 12.5f * unit,
                        ox + 19.8f * unit, oy + 13.0f * unit,
                        ox + 19.6f * unit, oy + 15.0f * unit,
                    )
                    cubicTo(
                        ox + 19.4f * unit, oy + 16.4f * unit,
                        ox + 16.4f * unit, oy + 16.2f * unit,
                        ox + 15.6f * unit, oy + 14.9f * unit,
                    )
                }
                drawPath(scrobbleTail, tint, style = stroke)
            }

            SettingsDuotoneGlyph.LIBRARY_VINYL -> {
                // Peeking vinyl disc behind sleeve
                drawCircle(duoFill, radius = 5.4f * unit, center = pt(15.2f, 11.6f))
                drawArc(
                    color = tint,
                    startAngle = -90f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = pt(9.8f, 6.2f),
                    size = Size(10.8f * unit, 10.8f * unit),
                    style = stroke,
                )
                drawCircle(tint, radius = 1.3f * unit, center = pt(15.2f, 11.6f))
                // Front sleeve with corner rounding
                val sleeveLeft = pt(3.8f, 5.2f)
                val sleeveSize = Size(11.4f * unit, 13.6f * unit)
                val radius = CornerRadius(2.4f * unit, 2.4f * unit)
                drawRoundRect(duoFill, topLeft = sleeveLeft, size = sleeveSize, cornerRadius = radius)
                drawRoundRect(tint, topLeft = sleeveLeft, size = sleeveSize, cornerRadius = radius, style = stroke)
                // Embedded music note
                drawCircle(tint, radius = 1.65f * unit, center = pt(8.3f, 14.5f))
                drawLine(tint, pt(9.8f, 14.5f), pt(9.8f, 9.2f), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, pt(9.8f, 9.2f), pt(12.2f, 10.1f), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
            }

            SettingsDuotoneGlyph.MODULES_HEX -> {
                val hex = Path().apply {
                    for (i in 0 until 6) {
                        val angle = (-90.0 + i * 60.0) * PI / 180.0
                        val x = ox + (12f + 7.8f * cos(angle).toFloat()) * unit
                        val y = oy + (12f + 7.8f * sin(angle).toFloat()) * unit
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(hex, duoFill, style = Fill)
                drawPath(hex, tint, style = stroke)
                val nodes = listOf(pt(12f, 7.6f), pt(8.4f, 14.2f), pt(15.6f, 14.2f))
                drawLine(tint, nodes[0], nodes[1], strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, nodes[1], nodes[2], strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, nodes[2], nodes[0], strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
                nodes.forEach { center ->
                    drawCircle(tint, radius = 1.7f * unit, center = center)
                }
            }

            SettingsDuotoneGlyph.DATA_VAULT -> {
                val topRect = Rect(ox + 5f * unit, oy + 4.8f * unit, ox + 19f * unit, oy + 9.2f * unit)
                val topPath = Path().apply { addOval(topRect) }
                drawPath(topPath, duoFill, style = Fill)
                drawPath(topPath, tint, style = stroke)
                // Side walls
                drawLine(tint, pt(5f, 7f), pt(5f, 17f), strokeWidth = 1.75.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, pt(19f, 7f), pt(19f, 17f), strokeWidth = 1.75.dp.toPx(), cap = StrokeCap.Round)
                // Middle & bottom curved rims
                drawArc(
                    color = tint,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = pt(5f, 10f),
                    size = Size(14f * unit, 4.2f * unit),
                    style = stroke,
                )
                drawArc(
                    color = tint,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = pt(5f, 14.8f),
                    size = Size(14f * unit, 4.2f * unit),
                    style = stroke,
                )
            }

            SettingsDuotoneGlyph.CLAUDEWAVES_CREST -> {
                drawCircle(duoFill, radius = 9f * unit, center = pt(12f, 12f))
                drawCircle(tint, radius = 9f * unit, center = pt(12f, 12f), style = stroke)
                val crest = Path().apply {
                    moveTo(ox + 5.6f * unit, oy + 12f * unit)
                    lineTo(ox + 7.8f * unit, oy + 12f * unit)
                    lineTo(ox + 9.4f * unit, oy + 8.3f * unit)
                    lineTo(ox + 11.4f * unit, oy + 15.8f * unit)
                    lineTo(ox + 13.4f * unit, oy + 7.2f * unit)
                    lineTo(ox + 15.2f * unit, oy + 14.4f * unit)
                    lineTo(ox + 16.6f * unit, oy + 12f * unit)
                    lineTo(ox + 18.4f * unit, oy + 12f * unit)
                }
                drawPath(crest, tint, style = stroke)
            }

            SettingsDuotoneGlyph.WIFI_HIRES -> {
                drawCircle(duoFill, radius = 8.2f * unit, center = pt(12f, 14f))
                drawArc(tint, 215f, 110f, false, pt(3.5f, 6.2f), Size(17f * unit, 17f * unit), style = stroke)
                drawArc(tint, 218f, 104f, false, pt(6.5f, 9.5f), Size(11f * unit, 11f * unit), style = stroke)
                drawArc(tint, 222f, 96f, false, pt(9.2f, 12.6f), Size(5.6f * unit, 5.6f * unit), style = stroke)
                drawCircle(tint, radius = 1.65f * unit, center = pt(12f, 17.6f))
            }

            SettingsDuotoneGlyph.CELLULAR_BARS -> {
                val heights = listOf(5f to 18f, 8.8f to 14.5f, 12.6f to 11f, 16.4f to 7.2f)
                heights.forEachIndexed { idx, (x, topY) ->
                    val rectTop = pt(x, topY)
                    val rectSize = Size(2.6f * unit, (19.5f - topY) * unit)
                    val corner = CornerRadius(1.1f * unit, 1.1f * unit)
                    if (idx >= 2) drawRoundRect(duoFill, topLeft = rectTop, size = rectSize, cornerRadius = corner)
                    drawRoundRect(tint, topLeft = rectTop, size = rectSize, cornerRadius = corner, style = thinStroke)
                }
            }

            SettingsDuotoneGlyph.HEADPHONES_WAVE -> {
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = pt(4.6f, 5.2f),
                    size = Size(14.8f * unit, 14.2f * unit),
                    style = stroke,
                )
                val leftCup = CornerRadius(1.8f * unit, 1.8f * unit)
                drawRoundRect(duoFill, pt(3.8f, 12.4f), Size(3.4f * unit, 6.4f * unit), leftCup)
                drawRoundRect(tint, pt(3.8f, 12.4f), Size(3.4f * unit, 6.4f * unit), leftCup, style = stroke)
                drawRoundRect(duoFill, pt(16.8f, 12.4f), Size(3.4f * unit, 6.4f * unit), leftCup)
                drawRoundRect(tint, pt(16.8f, 12.4f), Size(3.4f * unit, 6.4f * unit), leftCup, style = stroke)
                // Acoustic bars inside
                drawLine(tint, pt(10f, 13.8f), pt(10f, 16.4f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, pt(12f, 12.4f), pt(12f, 17.6f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, pt(14f, 13.8f), pt(14f, 16.4f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
            }

            SettingsDuotoneGlyph.DOWNLOAD_TRAY -> {
                val tray = Path().apply {
                    moveTo(ox + 4.8f * unit, oy + 14.5f * unit)
                    lineTo(ox + 4.8f * unit, oy + 17.8f * unit)
                    quadraticTo(ox + 4.8f * unit, oy + 19.2f * unit, ox + 6.4f * unit, oy + 19.2f * unit)
                    lineTo(ox + 17.6f * unit, oy + 19.2f * unit)
                    quadraticTo(ox + 19.2f * unit, oy + 19.2f * unit, ox + 19.2f * unit, oy + 17.8f * unit)
                    lineTo(ox + 19.2f * unit, oy + 14.5f * unit)
                }
                drawRoundRect(duoFill, pt(5.2f, 14.5f), Size(13.6f * unit, 4.4f * unit), CornerRadius(1.8f * unit, 1.8f * unit))
                drawPath(tray, tint, style = stroke)
                drawLine(tint, pt(12f, 4.8f), pt(12f, 15.2f), strokeWidth = 1.75.dp.toPx(), cap = StrokeCap.Round)
                val head = Path().apply {
                    moveTo(ox + 8.2f * unit, oy + 11.4f * unit)
                    lineTo(ox + 12f * unit, oy + 15.4f * unit)
                    lineTo(ox + 15.8f * unit, oy + 11.4f * unit)
                }
                drawPath(head, tint, style = stroke)
            }

            SettingsDuotoneGlyph.EQ_FADERS -> {
                val sliders = listOf(6.5f to 9f, 12f to 15f, 17.5f to 8f)
                sliders.forEach { (x, knobY) ->
                    drawLine(tint, pt(x, 4.8f), pt(x, 19.2f), strokeWidth = 1.65.dp.toPx(), cap = StrokeCap.Round)
                    drawCircle(duoFill, radius = 2.5f * unit, center = pt(x, knobY))
                    drawCircle(tint, radius = 2.15f * unit, center = pt(x, knobY), style = stroke)
                }
            }

            SettingsDuotoneGlyph.USB_DAC_WAVE -> {
                drawRoundRect(duoFill, pt(8f, 4.6f), Size(8f * unit, 4.2f * unit), CornerRadius(1.2f * unit, 1.2f * unit))
                drawRoundRect(tint, pt(8f, 4.6f), Size(8f * unit, 4.2f * unit), CornerRadius(1.2f * unit, 1.2f * unit), style = stroke)
                drawRoundRect(duoFill, pt(6.5f, 8.8f), Size(11f * unit, 6.2f * unit), CornerRadius(2.2f * unit, 2.2f * unit))
                drawRoundRect(tint, pt(6.5f, 8.8f), Size(11f * unit, 6.2f * unit), CornerRadius(2.2f * unit, 2.2f * unit), style = stroke)
                val pulse = Path().apply {
                    moveTo(ox + 5f * unit, oy + 18.2f * unit)
                    lineTo(ox + 8.5f * unit, oy + 18.2f * unit)
                    lineTo(ox + 10.5f * unit, oy + 16.4f * unit)
                    lineTo(ox + 13.5f * unit, oy + 19.8f * unit)
                    lineTo(ox + 15.5f * unit, oy + 18.2f * unit)
                    lineTo(ox + 19f * unit, oy + 18.2f * unit)
                }
                drawPath(pulse, tint, style = thinStroke)
            }

            SettingsDuotoneGlyph.CROSSFADE_WAVE -> {
                val w1 = Path().apply {
                    moveTo(ox + 3.8f * unit, oy + 14.5f * unit)
                    cubicTo(ox + 7.2f * unit, oy + 6.5f * unit, ox + 10.8f * unit, oy + 6.5f * unit, ox + 13.5f * unit, oy + 12f * unit)
                    cubicTo(ox + 16f * unit, oy + 17.5f * unit, ox + 18.5f * unit, oy + 17f * unit, ox + 20.2f * unit, oy + 11.5f * unit)
                }
                val w2 = Path().apply {
                    moveTo(ox + 3.8f * unit, oy + 9.5f * unit)
                    cubicTo(ox + 7.2f * unit, oy + 17.5f * unit, ox + 10.8f * unit, oy + 17.5f * unit, ox + 13.5f * unit, oy + 12f * unit)
                    cubicTo(ox + 16f * unit, oy + 6.5f * unit, ox + 18.5f * unit, oy + 7f * unit, ox + 20.2f * unit, oy + 12.5f * unit)
                }
                drawCircle(duoFill, radius = 5.4f * unit, center = pt(12f, 12f))
                drawPath(w1, tint, style = stroke)
                drawPath(w2, tint, style = thinStroke)
            }

            SettingsDuotoneGlyph.LIKED_HEART -> {
                val heart = Path().apply {
                    moveTo(ox + 12f * unit, oy + 19.2f * unit)
                    cubicTo(ox + 6.2f * unit, oy + 15.2f * unit, ox + 4.2f * unit, oy + 11.6f * unit, ox + 4.2f * unit, oy + 8.8f * unit)
                    cubicTo(ox + 4.2f * unit, oy + 6.3f * unit, ox + 6.2f * unit, oy + 4.6f * unit, ox + 8.5f * unit, oy + 4.6f * unit)
                    cubicTo(ox + 10.1f * unit, oy + 4.6f * unit, ox + 11.3f * unit, oy + 5.5f * unit, ox + 12f * unit, oy + 6.8f * unit)
                    cubicTo(ox + 12.7f * unit, oy + 5.5f * unit, ox + 13.9f * unit, oy + 4.6f * unit, ox + 15.5f * unit, oy + 4.6f * unit)
                    cubicTo(ox + 17.8f * unit, oy + 4.6f * unit, ox + 19.8f * unit, oy + 6.3f * unit, ox + 19.8f * unit, oy + 8.8f * unit)
                    cubicTo(ox + 19.8f * unit, oy + 11.6f * unit, ox + 17.8f * unit, oy + 15.2f * unit, ox + 12f * unit, oy + 19.2f * unit)
                    close()
                }
                drawPath(heart, duoFill, style = Fill)
                drawPath(heart, tint, style = stroke)
            }

            SettingsDuotoneGlyph.LOCAL_FOLDER_NOTE -> {
                val folder = Path().apply {
                    moveTo(ox + 3.8f * unit, oy + 7.2f * unit)
                    quadraticTo(ox + 3.8f * unit, oy + 5.6f * unit, ox + 5.4f * unit, oy + 5.6f * unit)
                    lineTo(ox + 9.4f * unit, oy + 5.6f * unit)
                    lineTo(ox + 11.2f * unit, oy + 7.6f * unit)
                    lineTo(ox + 18.6f * unit, oy + 7.6f * unit)
                    quadraticTo(ox + 20.2f * unit, oy + 7.6f * unit, ox + 20.2f * unit, oy + 9.2f * unit)
                    lineTo(ox + 20.2f * unit, oy + 17.4f * unit)
                    quadraticTo(ox + 20.2f * unit, oy + 19f * unit, ox + 18.6f * unit, oy + 19f * unit)
                    lineTo(ox + 5.4f * unit, oy + 19f * unit)
                    quadraticTo(ox + 3.8f * unit, oy + 19f * unit, ox + 3.8f * unit, oy + 17.4f * unit)
                    close()
                }
                drawPath(folder, duoFill, style = Fill)
                drawPath(folder, tint, style = stroke)
                drawCircle(tint, radius = 1.45f * unit, center = pt(11.3f, 15.2f))
                drawLine(tint, pt(12.6f, 15.2f), pt(12.6f, 10.6f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                drawLine(tint, pt(12.6f, 10.6f), pt(14.8f, 11.4f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
            }

            SettingsDuotoneGlyph.SMART_MIX_SPARK -> {
                fun drawSpark(cx: Float, cy: Float, r: Float, fill: Boolean) {
                    val path = Path().apply {
                        moveTo(ox + cx * unit, oy + (cy - r) * unit)
                        quadraticTo(ox + cx * unit, oy + cy * unit, ox + (cx + r) * unit, oy + cy * unit)
                        quadraticTo(ox + cx * unit, oy + cy * unit, ox + cx * unit, oy + (cy + r) * unit)
                        quadraticTo(ox + cx * unit, oy + cy * unit, ox + (cx - r) * unit, oy + cy * unit)
                        quadraticTo(ox + cx * unit, oy + cy * unit, ox + cx * unit, oy + (cy - r) * unit)
                        close()
                    }
                    if (fill) drawPath(path, duoFill, style = Fill)
                    drawPath(path, tint, style = thinStroke)
                }
                drawSpark(10f, 11.2f, 6f, fill = true)
                drawSpark(17.5f, 16.8f, 3.1f, fill = true)
            }
        }
    }
}
