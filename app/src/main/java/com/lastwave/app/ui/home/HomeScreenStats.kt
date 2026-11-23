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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
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
    selectedDayKey: String? = null,
    onSelectDay: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val isDark = LocalIsDarkTheme.current
    val isComic = LocalComicTheme.current
    val displayBins = if (bins.size == 7) {
        bins
    } else {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").map {
            DailyActivityBin(dayKey = "", dayLabel = it, fullDateLabel = it, plays = 0, isPeak = false)
        }
    }
    val selectedBin = remember(displayBins, selectedDayKey) {
        selectedDayKey?.let { key -> displayBins.firstOrNull { it.dayKey == key } }
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

    Column(modifier = modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (selectedBin != null) {
                val playsWord = if (selectedBin.plays == 1) "play" else "plays"
                Text(
                    text = "${selectedBin.fullDateLabel} · ${selectedBin.plays} $playsWord · ~${formatDayEstimate(selectedBin.plays)}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontFeatureSettings = "tnum",
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                val clearShape = RoundedCornerShape(50)
                Box(
                    modifier = Modifier
                        .heightIn(min = 28.dp)
                        .clip(clearShape)
                        .background(
                            if (isComic) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.16f else 0.12f)
                            },
                        )
                        .border(
                            width = if (isComic) 1.5.dp else 0.75.dp,
                            color = if (isComic) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary.copy(alpha = 0.36f),
                            shape = clearShape,
                        )
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelectDay(null)
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isComic) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (hasActivity) "Last 7 days · tap bar" else "Last 7 days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(114.dp),
        ) {
            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(top = 16.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                repeat(3) {
                    HorizontalDivider(thickness = 0.6.dp, color = gridColor)
                }
            }
            Row(
                modifier = Modifier.matchParentSize().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                displayBins.forEach { bin ->
                    val isSelected = selectedBin != null && bin.dayKey == selectedBin.dayKey
                    val hasAnySelection = selectedBin != null
                    val fraction = if (hasActivity && bin.plays > 0) {
                        (bin.plays.toFloat() / maxPlays.toFloat()).coerceIn(0.14f, 1f)
                    } else {
                        0.06f
                    }
                    val fillColor = when {
                        isSelected -> peakBarColor
                        hasAnySelection -> mutedBarColor.copy(alpha = if (isDark) 0.40f else 0.34f)
                        bin.isPeak -> peakBarColor
                        else -> mutedBarColor
                    }
                    val canTap = bin.dayKey.isNotBlank()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = canTap) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onSelectDay(bin.dayKey)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier.height(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSelected) {
                                Text(
                                    text = bin.plays.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontFeatureSettings = "tnum",
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.76f)
                                    .fillMaxHeight(fraction)
                                    .clip(barShape)
                                    .background(fillColor)
                                    .then(
                                        if (isComic && (isSelected || (!hasAnySelection && bin.plays > 0))) {
                                            Modifier.border(
                                                width = if (isSelected) 1.8.dp else 1.2.dp,
                                                color = MaterialTheme.colorScheme.outline,
                                                shape = barShape,
                                            )
                                        } else {
                                            Modifier
                                        },
                                    ),
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = bin.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected || (!hasAnySelection && bin.isPeak)) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                hasAnySelection -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f)
                                bin.isPeak -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

private fun formatDayEstimate(plays: Int): String {
    val minutes = (plays * 210) / 60
    return when {
        minutes <= 0 -> "0m"
        minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
        else -> "${minutes}m"
    }
}

private fun formatCount(value: Long): String = if (value <= 0) "—" else "%,d".format(value)
