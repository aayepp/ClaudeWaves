package com.lastwave.app.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lastwave.app.data.update.DailyActivePoint
import com.lastwave.app.data.update.GitHubReleaseStats
import com.lastwave.app.data.update.LiveUserStats
import com.lastwave.app.data.update.StatBucket
import com.lastwave.app.ui.comic.Surface
import com.lastwave.app.ui.comic.TextButton
import com.lastwave.app.ui.theme.ComicAccent
import com.lastwave.app.ui.theme.glassCardColor
import java.util.Locale

/** Accent palette for multi-segment charts, all from the theme so every theme stays legible. */
@Composable
private fun chartPalette(isComic: Boolean): List<Color> {
    val c = MaterialTheme.colorScheme
    return listOf(if (isComic) ComicAccent else c.primary, c.tertiary, c.secondary, c.error, c.outline)
}

@Composable
private fun BentoTile(
    isComic: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = glassCardColor(MaterialTheme.colorScheme.surfaceContainer),
        border = devCardBorder(isComic),
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}

@Composable
private fun TileLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

private val Tabular = TextStyle(fontFeatureSettings = "tnum")

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MetricTile(
    label: String,
    value: String,
    note: String?,
    isComic: Boolean,
    info: DevMetricInfo,
    onHoldMetric: (DevMetricInfo) -> Unit,
    modifier: Modifier = Modifier,
    noteColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val haptic = LocalHapticFeedback.current
    BentoTile(
        isComic = isComic,
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(
                onClick = { onHoldMetric(info) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onHoldMetric(info)
                },
            ),
    ) {
        TileLabel(label)
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.merge(Tabular),
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
        if (note != null) {
            Text(note, style = MaterialTheme.typography.labelSmall, color = noteColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Loading, error or the live Bento block (hero + four metric tiles). */
@Composable
internal fun LiveStatsBento(
    live: LiveUserStats?,
    error: String?,
    isLoading: Boolean,
    isComic: Boolean,
    onHoldMetric: (DevMetricInfo) -> Unit,
    onTestPing: () -> Unit,
    onRetry: () -> Unit,
) {
    when {
        live == null && isLoading -> BentoTile(isComic, Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
            }
        }
        live == null -> BentoTile(isComic, Modifier.fillMaxWidth()) {
            Text(
                text = error ?: "Gagal memuat turun data statistik",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = onRetry, enabled = !isLoading) { Text("Cuba lagi") }
        }
        else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroTile(live, isComic, onHoldMetric, onTestPing, isLoading)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("DAU hari ini", formatStatsNumber(live.dauToday), "24 jam: ${formatStatsNumber(live.active24h)}",
                    isComic, DevMetricInfo.DAU, onHoldMetric, Modifier.weight(1f))
                MetricTile("Install", formatStatsNumber(live.totalInstalls),
                    "+${formatStatsNumber(live.newToday)} hari ini · +${formatStatsNumber(live.new7d)} 7h",
                    isComic, DevMetricInfo.INSTALLS, onHoldMetric, Modifier.weight(1f),
                    noteColor = if (live.newToday > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("WAU", formatStatsNumber(live.wau7d), "aktif 7 hari", isComic, DevMetricInfo.WAU, onHoldMetric, Modifier.weight(1f))
                MetricTile("MAU", formatStatsNumber(live.mau30d), "aktif 30 hari", isComic, DevMetricInfo.MAU, onHoldMetric, Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroTile(
    live: LiveUserStats,
    isComic: Boolean,
    onHoldMetric: (DevMetricInfo) -> Unit,
    onTestPing: () -> Unit,
    isLoading: Boolean,
) {
    val haptic = LocalHapticFeedback.current
    BentoTile(
        isComic = isComic,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .combinedClickable(
                onClick = { onHoldMetric(DevMetricInfo.ONLINE_15M) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onHoldMetric(DevMetricInfo.ONLINE_15M)
                },
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TileLabel("Aktif sekarang")
            Box(
                Modifier.size(7.dp).clip(CircleShape).background(
                    if (live.online15m > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                ),
            )
            Text("LIVE · 15m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onTestPing, enabled = !isLoading) {
                Text("Test Ping", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(
            text = formatStatsNumber(live.online15m),
            style = MaterialTheme.typography.displaySmall.merge(Tabular),
            fontWeight = FontWeight.Black,
        )
        if (live.dailyTrend14d.size >= 2) {
            TrendLine(live.dailyTrend14d, isComic, Modifier.fillMaxWidth().height(72.dp))
            val peak = live.dailyTrend14d.maxOf { it.dau }
            Text(
                text = "Active users 14 hari · puncak ${formatStatsNumber(peak)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TrendLine(points: List<DailyActivePoint>, isComic: Boolean, modifier: Modifier) {
    val accent = if (isComic) ComicAccent else MaterialTheme.colorScheme.primary
    val ring = MaterialTheme.colorScheme.surface
    val maxDau = points.maxOf { it.dau }.coerceAtLeast(1)
    val peakIndex = points.indexOfFirst { it.dau == maxDau }
    // A line reads a 14-day trend at a glance; bars made every day compete equally.
    Canvas(modifier) {
        val inset = 5.dp.toPx()
        val chartH = size.height - inset * 2
        val stepX = (size.width - inset * 2) / (points.size - 1)
        val coords = points.mapIndexed { i, p -> Offset(inset + i * stepX, inset + chartH * (1f - p.dau.toFloat() / maxDau)) }
        val line = Path().apply {
            moveTo(coords.first().x, coords.first().y)
            for (k in 1 until coords.size) {
                val midX = (coords[k - 1].x + coords[k].x) / 2f
                cubicTo(midX, coords[k - 1].y, midX, coords[k].y, coords[k].x, coords[k].y)
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(coords.last().x, size.height)
            lineTo(coords.first().x, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(accent.copy(alpha = 0.28f), accent.copy(alpha = 0f))))
        drawPath(line, accent, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        listOf(peakIndex, coords.lastIndex).distinct().forEach { k ->
            drawCircle(ring, radius = 5.dp.toPx(), center = coords[k])
            drawCircle(accent, radius = 3.5.dp.toPx(), center = coords[k])
        }
    }
}

/** Country of the app's active users (from heartbeats; GitHub gives no download geography). */
@Composable
internal fun CountriesTile(buckets: List<StatBucket>, isComic: Boolean) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val total = buckets.sumOf { it.count }.coerceAtLeast(1)
    val top = buckets.maxOf { it.count }.coerceAtLeast(1)
    val accent = if (isComic) ComicAccent else MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    BentoTile(isComic, Modifier.fillMaxWidth()) {
        Row {
            TileLabel("Negara user", Modifier.weight(1f))
            Text("${buckets.size} negara", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        (if (expanded) buckets else buckets.take(6)).forEach { bucket ->
            val code = bucket.label.trim().uppercase(Locale.ROOT)
            val isIso = code.length == 2 && code.all { it in 'A'..'Z' }
            val name = if (isIso) Locale("", code).getDisplayCountry(Locale("ms")).ifBlank { code } else bucket.label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Text(if (isIso) flagOf(code) else "🌐", modifier = Modifier.width(24.dp))
                Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(112.dp))
                Canvas(Modifier.weight(1f).height(8.dp)) {
                    val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
                    drawRoundRect(track, cornerRadius = r)
                    drawRoundRect(accent, size = Size(size.width * bucket.count / top, size.height), cornerRadius = r)
                }
                Text("${bucket.count * 100 / total}%", style = MaterialTheme.typography.labelLarge.merge(Tabular), fontWeight = FontWeight.Bold, modifier = Modifier.width(38.dp))
                Text(formatStatsNumber(bucket.count), style = MaterialTheme.typography.labelSmall.merge(Tabular), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (buckets.size > 6) {
            TextButton(onClick = { expanded = !expanded }, modifier = Modifier.align(Alignment.End)) {
                Text(if (expanded) "Tunjuk 6 teratas" else "Lihat semua ›", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun flagOf(iso: String): String =
    iso.map { Character.toChars(0x1F1E6 + (it - 'A')) }.joinToString("") { String(it) }

@Composable
internal fun VersionsTile(buckets: List<StatBucket>, isComic: Boolean) {
    val palette = chartPalette(isComic)
    val shown = buckets.take(4)
    val total = buckets.sumOf { it.count }.coerceAtLeast(1)
    BentoTile(isComic, Modifier.fillMaxWidth()) {
        TileLabel("Versi app")
        Canvas(Modifier.fillMaxWidth().height(12.dp).padding(vertical = 0.dp)) {
            val gap = 2.dp.toPx()
            var x = 0f
            val rest = total - shown.sumOf { it.count }
            (shown.map { it.count } + listOf(rest).filter { it > 0 }).forEachIndexed { i, count ->
                val w = size.width * count / total - gap
                if (w > 0f) drawRoundRect(palette[i.coerceAtMost(palette.lastIndex)], topLeft = Offset(x, 0f), size = Size(w, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                x += w + gap
            }
        }
        Spacer(Modifier.height(4.dp))
        LegendRow(shown.map { it.label to it.count * 100 / total }, palette)
    }
}

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun LegendRow(entries: List<Pair<String, Int>>, palette: List<Color>) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        entries.forEachIndexed { i, (label, pct) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(palette[i.coerceAtMost(palette.lastIndex)]))
                Text("$label $pct%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun AndroidTile(buckets: List<StatBucket>, isComic: Boolean, modifier: Modifier) {
    val palette = chartPalette(isComic)
    val shown = buckets.take(4)
    val total = buckets.sumOf { it.count }.coerceAtLeast(1)
    BentoTile(isComic, modifier) {
        TileLabel("Android")
        Canvas(Modifier.size(92.dp).align(Alignment.CenterHorizontally).padding(6.dp)) {
            val stroke = 12.dp.toPx()
            var start = -90f
            shown.forEachIndexed { i, bucket ->
                val sweep = 360f * bucket.count / total
                drawArc(palette[i], start, (sweep - 3f).coerceAtLeast(0.5f), useCenter = false,
                    topLeft = Offset(stroke / 2, stroke / 2), size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Butt))
                start += sweep
            }
        }
        LegendRow(shown.map { it.label to it.count * 100 / total }, palette)
    }
}

@Composable
internal fun ReleaseTile(gh: GitHubReleaseStats?, error: String?, isLoading: Boolean, isComic: Boolean, modifier: Modifier) {
    val accent = if (isComic) ComicAccent else MaterialTheme.colorScheme.primary
    val muted = accent.copy(alpha = 0.35f)
    BentoTile(isComic, modifier) {
        TileLabel("Download APK")
        when {
            gh == null && isLoading -> CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            gh == null -> Text(error ?: "Tiada data GitHub", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            else -> {
                Text(formatStatsNumber(gh.totalDownloads), style = MaterialTheme.typography.headlineSmall.merge(Tabular), fontWeight = FontWeight.ExtraBold)
                Text(
                    text = "★ ${formatStatsNumber(gh.stars)} · ${gh.latestTag ?: "-"}: ${formatStatsNumber(gh.latestDownloads)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Oldest to newest, newest highlighted: download pace per release at a glance.
                val recent = gh.releases.take(6).reversed()
                val maxDl = recent.maxOfOrNull { it.downloads }?.coerceAtLeast(1L) ?: 1L
                if (recent.isNotEmpty()) {
                    Canvas(Modifier.fillMaxWidth().height(46.dp).padding(top = 6.dp)) {
                        val gap = 4.dp.toPx()
                        val w = (size.width - gap * (recent.size - 1)) / recent.size
                        recent.forEachIndexed { i, r ->
                            val h = (size.height * r.downloads / maxDl).coerceAtLeast(3.dp.toPx())
                            drawRoundRect(if (i == recent.lastIndex) accent else muted, topLeft = Offset(i * (w + gap), size.height - h),
                                size = Size(w, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                        }
                    }
                }
            }
        }
    }
}
