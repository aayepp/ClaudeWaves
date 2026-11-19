package com.lastwave.app.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.lastwave.app.R
import com.lastwave.app.data.repository.HomeArtistItem
import com.lastwave.app.data.repository.HomeTrack
import com.lastwave.app.ui.player.lyricsshare.ShareCardRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

enum class RecapPeriod(val chipLabel: String, val cardSubtitle: String) {
    LAST_7_DAYS("7 Days", "Last 7 Days"),
    LAST_30_DAYS("30 Days", "Last 30 Days"),
    ALL_TIME("All Time", "All-Time Recap"),
}

data class RecapCardData(
    val displayName: String,
    val period: RecapPeriod,
    val totalPlays: Long,
    val estimatedMinutes: Long,
    val topTracks: List<HomeTrack>,
    val topArtists: List<HomeArtistItem>,
    val heroArtworkUrl: String?,
)

/** Builds pure summary data for the shareable Recap card from [HomeUiState]. */
fun buildRecapCardData(uiState: HomeUiState, period: RecapPeriod): RecapCardData {
    val rawTracks = when (period) {
        RecapPeriod.LAST_7_DAYS -> uiState.topTracks7Days.ifEmpty { uiState.allTracks }
        RecapPeriod.LAST_30_DAYS -> uiState.topTracks30Days.ifEmpty { uiState.topTracksOverall.ifEmpty { uiState.allTracks } }
        RecapPeriod.ALL_TIME -> uiState.topTracksOverall.ifEmpty { uiState.allTracks }
    }
    // Deduplicate case-insensitively while preserving descending rank / summing counts when drawn from recent feed.
    val rankedTracks = rawTracks
        .filter { it.name.isNotBlank() }
        .take(4)

    val rankedArtists = if (period == RecapPeriod.ALL_TIME && uiState.topArtists.isNotEmpty()) {
        uiState.topArtists.take(3)
    } else {
        rawTracks
            .filter { it.artist.isNotBlank() }
            .groupBy { it.artist.trim().lowercase() }
            .values
            .map { group ->
                HomeArtistItem(
                    name = group.first().artist.trim(),
                    playCount = group.sumOf { it.playCount.coerceAtLeast(1).toLong() },
                    artworkUrl = group.firstNotNullOfOrNull { it.artworkUrl },
                )
            }
            .sortedByDescending { it.playCount }
            .ifEmpty { uiState.topArtists }
            .take(3)
    }

    val periodPlays = rawTracks.sumOf { it.playCount.coerceAtLeast(1).toLong() }
    val totalPlays = if (period == RecapPeriod.ALL_TIME) {
        uiState.stats?.scrobbles?.takeIf { it > 0L } ?: periodPlays
    } else {
        periodPlays
    }
    val estimatedMinutes = (totalPlays * 210L) / 60L

    val displayName = when {
        uiState.isViewingFriend -> uiState.viewingUsername
        uiState.username.isNotBlank() -> uiState.username
        else -> "Listener"
    }
    val heroArt = rankedTracks.firstOrNull()?.artworkUrl
        ?: rankedArtists.firstOrNull()?.artworkUrl
        ?: uiState.stats?.avatarUrl

    return RecapCardData(
        displayName = displayName,
        period = period,
        totalPlays = totalPlays,
        estimatedMinutes = estimatedMinutes,
        topTracks = rankedTracks,
        topArtists = rankedArtists,
        heroArtworkUrl = heroArt,
    )
}

internal fun formatListeningMinutes(minutes: Long): String {
    if (minutes <= 0L) return "0 min"
    val hours = minutes / 60L
    val rem = minutes % 60L
    return when {
        hours >= 100 -> "${hours}h"
        hours > 0 -> "${hours}h ${rem}m"
        else -> "${rem}m"
    }
}

object RecapCardRenderer {
    private const val WIDTH = 1080
    private const val HEIGHT = 1350

    /** Renders a 4:5 portrait Recap bitmap on [Dispatchers.Default]. */
    suspend fun render(
        context: Context,
        data: RecapCardData,
        heroArtwork: Bitmap?,
        accentColor: Int,
    ): Bitmap = withContext(Dispatchers.Default) {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Deep obsidian backdrop with luminous accent aura in top-right corner
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(),
                intArrayOf(0xFF0C0E16.toInt(), 0xFF131624.toInt(), 0xFF0A0B10.toInt()),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                WIDTH * 0.85f,
                HEIGHT * 0.14f,
                WIDTH * 0.72f,
                intArrayOf(withAlpha(accentColor, 0x66), withAlpha(accentColor, 0x18), AndroidColor.TRANSPARENT),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), glowPaint)

        val bold = ShareCardRenderer.getTypeface(context, 750)
        val semiBold = ShareCardRenderer.getTypeface(context, 600)
        val regular = ShareCardRenderer.getTypeface(context, 450)
        val marginX = 72f
        val contentW = WIDTH - marginX * 2f

        // Header eyebrow + user tag
        val badgePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold
            textSize = 26f
            letterSpacing = 0.14f
            color = accentColor
        }
        canvas.drawText(data.period.cardSubtitle.uppercase(), marginX, 112f, badgePaint)

        val userPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = semiBold
            textSize = 30f
            color = 0xB8FFFFFF.toInt()
            textAlign = Paint.Align.RIGHT
        }
        val elidedUser = TextUtils.ellipsize("@${data.displayName}", userPaint, contentW * 0.45f, TextUtils.TruncateAt.END)
        canvas.drawText(elidedUser, 0, elidedUser.length, WIDTH - marginX, 112f, userPaint)

        // Hero stats card row (artwork left, total plays + listening time right)
        val heroTop = 152f
        val artSize = 236f
        if (heroArtwork != null) {
            val rounded = roundedCrop(heroArtwork, artSize.roundToInt(), 36f)
            canvas.drawBitmap(rounded, marginX, heroTop, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f
                color = 0x33FFFFFF
            }
            canvas.drawRoundRect(RectF(marginX, heroTop, marginX + artSize, heroTop + artSize), 36f, 36f, border)
        } else {
            val placeholder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = withAlpha(accentColor, 0x33)
            }
            canvas.drawRoundRect(RectF(marginX, heroTop, marginX + artSize, heroTop + artSize), 36f, 36f, placeholder)
        }

        val heroTextX = marginX + artSize + 36f
        val statValuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold
            textSize = 86f
            color = AndroidColor.WHITE
        }
        val playsStr = String.format("%,d", data.totalPlays)
        canvas.drawText(playsStr, heroTextX, heroTop + 98f, statValuePaint)

        val statSubPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = semiBold
            textSize = 32f
            color = 0xD8FFFFFF.toInt()
        }
        canvas.drawText("PLAYS \u2022 ${formatListeningMinutes(data.estimatedMinutes)}", heroTextX, heroTop + 148f, statSubPaint)

        data.topTracks.firstOrNull()?.let { first ->
            val topSongPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = regular
                textSize = 28f
                color = 0xAAFFFFFF.toInt()
            }
            val avail = WIDTH - marginX - heroTextX
            val line = TextUtils.ellipsize("#1 ${first.name} — ${first.artist}", topSongPaint, avail, TextUtils.TruncateAt.END)
            canvas.drawText(line, 0, line.length, heroTextX, heroTop + 206f, topSongPaint)
        }

        // Top Artists section pill strip
        var cursorY = heroTop + artSize + 58f
        val sectionLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold
            textSize = 24f
            letterSpacing = 0.12f
            color = 0x99FFFFFF.toInt()
        }
        if (data.topArtists.isNotEmpty()) {
            canvas.drawText("TOP ARTISTS", marginX, cursorY, sectionLabelPaint)
            cursorY += 24f
            val cardRect = RectF(marginX, cursorY, WIDTH - marginX, cursorY + 196f)
            drawGlassPanel(canvas, cardRect, 28f)
            val rowTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = semiBold
                textSize = 32f
                color = AndroidColor.WHITE
            }
            val rowCountPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = regular
                textSize = 26f
                color = 0xA6FFFFFF.toInt()
                textAlign = Paint.Align.RIGHT
            }
            val rankPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = bold
                textSize = 28f
                color = accentColor
            }
            data.topArtists.forEachIndexed { idx, artist ->
                val y = cursorY + 52f + idx * 58f
                canvas.drawText("${idx + 1}", marginX + 28f, y, rankPaint)
                val name = TextUtils.ellipsize(artist.name, rowTitlePaint, contentW - 220f, TextUtils.TruncateAt.END)
                canvas.drawText(name, 0, name.length, marginX + 74f, y, rowTitlePaint)
                canvas.drawText("${artist.playCount} plays", WIDTH - marginX - 28f, y, rowCountPaint)
            }
            cursorY += 244f
        }

        // Top Tracks section
        if (data.topTracks.isNotEmpty()) {
            canvas.drawText("TOP TRACKS", marginX, cursorY, sectionLabelPaint)
            cursorY += 24f
            val panelH = (data.topTracks.size * 98f) + 32f
            val tracksRect = RectF(marginX, cursorY, WIDTH - marginX, cursorY + panelH)
            drawGlassPanel(canvas, tracksRect, 28f)

            val trackTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = semiBold
                textSize = 31f
                color = AndroidColor.WHITE
            }
            val trackArtistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = regular
                textSize = 25f
                color = 0x9EFFFFFF.toInt()
            }
            val trackPlaysPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = semiBold
                textSize = 25f
                color = accentColor
                textAlign = Paint.Align.RIGHT
            }
            val rankPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = bold
                textSize = 30f
                color = accentColor
            }

            data.topTracks.forEachIndexed { idx, track ->
                val baseY = cursorY + 58f + idx * 98f
                canvas.drawText("#${idx + 1}", marginX + 28f, baseY, rankPaint)
                val title = TextUtils.ellipsize(track.name, trackTitlePaint, contentW - 250f, TextUtils.TruncateAt.END)
                canvas.drawText(title, 0, title.length, marginX + 94f, baseY - 6f, trackTitlePaint)
                val sub = TextUtils.ellipsize(track.artist, trackArtistPaint, contentW - 250f, TextUtils.TruncateAt.END)
                canvas.drawText(sub, 0, sub.length, marginX + 94f, baseY + 26f, trackArtistPaint)
                if (track.playCount > 0) {
                    canvas.drawText("${track.playCount}×", WIDTH - marginX - 28f, baseY + 8f, trackPlaysPaint)
                }
            }
        }

        // Footer brand mark
        val footerY = HEIGHT - 64f
        val logoSize = 54
        ContextCompat.getDrawable(context, R.drawable.ic_notification_wave)?.let { logo ->
            logo.setBounds(
                marginX.roundToInt(),
                (footerY - logoSize + 8f).roundToInt(),
                (marginX + logoSize).roundToInt(),
                (footerY + 8f).roundToInt(),
            )
            logo.colorFilter = PorterDuffColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
            logo.draw(canvas)
        }
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = bold
            textSize = 40f
            color = AndroidColor.WHITE
        }
        canvas.drawText("ClaudeWaves", marginX + logoSize + 16f, footerY - 2f, brandPaint)

        bitmap
    }

    private fun drawGlassPanel(canvas: Canvas, rect: RectF, radius: Float) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x16FFFFFF }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = 0x26FFFFFF
        }
        canvas.drawRoundRect(rect, radius, radius, fill)
        canvas.drawRoundRect(rect, radius, radius, stroke)
    }

    private fun roundedCrop(source: Bitmap, sizePx: Int, radius: Float): Bitmap {
        val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat())
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val minSide = minOf(source.width, source.height)
        val sx = (source.width - minSide) / 2
        val sy = (source.height - minSide) / 2
        canvas.drawBitmap(source, Rect(sx, sy, sx + minSide, sy + minSide), rect, paint)
        return out
    }

    private fun withAlpha(color: Int, alphaByte: Int): Int =
        (color and 0x00FFFFFF) or ((alphaByte and 0xFF) shl 24)
}
