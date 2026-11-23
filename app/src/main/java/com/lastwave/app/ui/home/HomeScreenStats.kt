package com.lastwave.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme

@Composable
internal fun LocalStatsInlineNotice(
    isOfflineFallback: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val isComic = LocalComicTheme.current
    val hairline = if (isComic) {
        MaterialTheme.colorScheme.outline
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.36f else 0.52f)
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isOfflineFallback) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                        ),
                )
                Text(
                    text = if (isOfflineFallback) {
                        "Last.fm unreachable · Showing local plays"
                    } else {
                        "Local plays · Connect Last.fm to sync scrobbles"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = if (isOfflineFallback) "Retry" else "Settings",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
        HorizontalDivider(thickness = if (isComic) 1.5.dp else 0.75.dp, color = hairline)
    }
}

@Composable
internal fun TelemetryMetricGrid(
    scrobbles: Long,
    trackCount: Long,
    artistCount: Long,
    albumCount: Long,
    headlineLabel: String,
    onSelectTracks: () -> Unit,
    onSelectArtists: () -> Unit,
    onSelectAlbums: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val isComic = LocalComicTheme.current
    val dividerThickness = if (isComic) 1.5.dp else 0.75.dp
    val dividerColor = if (isComic) {
        MaterialTheme.colorScheme.outline
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.36f else 0.52f)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = dividerThickness, color = dividerColor)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetricCell(
                value = formatCount(scrobbles),
                label = headlineLabel,
                modifier = Modifier.weight(1f),
            )
            VerticalDivider(modifier = Modifier.height(34.dp), thickness = dividerThickness, color = dividerColor)
            MetricCell(
                value = formatCount(trackCount),
                label = "Tracks",
                onClick = onSelectTracks,
                modifier = Modifier.weight(1f),
            )
            VerticalDivider(modifier = Modifier.height(34.dp), thickness = dividerThickness, color = dividerColor)
            MetricCell(
                value = formatCount(artistCount),
                label = "Artists",
                onClick = onSelectArtists,
                modifier = Modifier.weight(1f),
            )
            VerticalDivider(modifier = Modifier.height(34.dp), thickness = dividerThickness, color = dividerColor)
            MetricCell(
                value = formatCount(albumCount),
                label = "Albums",
                onClick = onSelectAlbums,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider(thickness = dividerThickness, color = dividerColor)
    }
}

@Composable
private fun MetricCell(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun TelemetryGenreRow(
    tags: List<String>,
    onOpenGenres: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val isComic = LocalComicTheme.current
    val dividerColor = if (isComic) {
        MaterialTheme.colorScheme.outline
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.36f else 0.52f)
    }
    val summary = if (tags.isNotEmpty()) {
        tags.take(4).joinToString(" · ") { tag -> tag.replaceFirstChar { it.uppercase() } }
    } else {
        "Top tags & genres"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clickable(onClick = onOpenGenres)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Genres",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open Genres",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        HorizontalDivider(thickness = if (isComic) 1.5.dp else 0.75.dp, color = dividerColor)
    }
}

@Composable
internal fun TelemetryActivityChart(
    bins: List<DailyActivityBin>,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val isComic = LocalComicTheme.current
    val displayBins = if (bins.size == 7) {
        bins
    } else {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").map { DailyActivityBin(it, 0, false) }
    }
    val maxPlays = displayBins.maxOfOrNull { it.plays }?.coerceAtLeast(1) ?: 1
    val hasActivity = displayBins.any { it.plays > 0 }
    val gridColor = if (isComic) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.22f else 0.35f)
    }
    val mutedBarColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDark) 0.34f else 0.25f)
    val peakBarColor = MaterialTheme.colorScheme.primary
    val barShape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)

    Column(modifier = modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp),
        ) {
            Column(
                modifier = Modifier.matchParentSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                repeat(3) {
                    HorizontalDivider(thickness = 0.6.dp, color = gridColor)
                }
            }
            if (!hasActivity) {
                Text(
                    text = "Last 7 days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp),
                )
            }
            Row(
                modifier = Modifier.matchParentSize().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                displayBins.forEach { bin ->
                    val fraction = if (hasActivity && bin.plays > 0) {
                        (bin.plays.toFloat() / maxPlays.toFloat()).coerceIn(0.14f, 1f)
                    } else {
                        0.06f
                    }
                    val fillColor = if (bin.isPeak) peakBarColor else mutedBarColor
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.76f)
                                .fillMaxHeight(fraction)
                                .clip(barShape)
                                .background(fillColor)
                                .then(
                                    if (isComic && bin.plays > 0) {
                                        Modifier.border(1.2.dp, MaterialTheme.colorScheme.outline, barShape)
                                    } else {
                                        Modifier
                                    },
                                ),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            displayBins.forEach { bin ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = bin.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (bin.isPeak) FontWeight.Bold else FontWeight.Normal,
                        color = if (bin.isPeak) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun formatCount(value: Long): String = if (value <= 0) "—" else "%,d".format(value)
