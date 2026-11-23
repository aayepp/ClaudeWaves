package com.lastwave.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip as uiClip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.R
import com.lastwave.app.data.repository.HomeSortMode
import com.lastwave.app.ui.comic.DropdownMenuItem
import com.lastwave.app.ui.comic.IconButton
import com.lastwave.app.ui.comic.Surface
import com.lastwave.app.ui.common.ArtworkImage
import com.lastwave.app.ui.common.ExpressiveHeader
import com.lastwave.app.ui.common.HeaderActionIcon
import com.lastwave.app.ui.common.safeHorizontalContentPadding
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.comicPanel
import com.lastwave.app.ui.theme.glassCardColor

private enum class TelemetryPrimaryTab(val label: String) {
    TRACKS("Tracks"),
    ARTISTS("Artists"),
    ALBUMS("Albums"),
    HISTORY("History"),
}

private val DurationBadgeShape = RoundedCornerShape(10.dp)

@Composable
internal fun StatsTelemetryTopBar(
    displayUsername: String,
    isViewingFriend: Boolean,
    showShare: Boolean,
    avatarUrl: String?,
    onShare: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    ExpressiveHeader(
        title = "Stats",
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxWidth().safeHorizontalContentPadding(),
        leadingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Stats",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                LiveListenDurationPill(viewModel = viewModel)
                if (isViewingFriend && displayUsername.isNotBlank()) {
                    Surface(
                        onClick = onOpenFriends,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = displayUsername,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        },
        actions = {
            if (showShare) {
                HeaderActionIcon(Icons.Filled.Share, "Share Recap", onShare)
            }
            HeaderActionIcon(Icons.Filled.People, stringResource(R.string.home_switch_profile_cd), onOpenFriends)
            IconButton(onClick = onOpenSettings) {
                ProfileAvatar(avatarUrl = avatarUrl, modifier = Modifier.size(36.dp))
            }
        },
    )
}

@Composable
internal fun LiveListenDurationPill(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listenElapsedSeconds by viewModel.listenElapsedSeconds.collectAsStateWithLifecycle()
    val totalSeconds = (uiState.stats?.timerBaseSeconds ?: 0) + listenElapsedSeconds.toLong()
    val isPlaying = uiState.nowPlaying != null
    val isComic = LocalComicTheme.current
    val isDark = LocalIsDarkTheme.current

    Surface(
        shape = DurationBadgeShape,
        color = if (isComic) {
            MaterialTheme.colorScheme.surface
        } else {
            glassCardColor(MaterialTheme.colorScheme.surfaceContainerHigh)
        },
        border = if (isComic) null else BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.45f else 0.6f),
        ),
        modifier = Modifier.comicPanel(DurationBadgeShape, offset = 2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        ) {
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .uiClip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Text(
                text = formatTimer(totalSeconds),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontFeatureSettings = "tnum",
                ),
                fontWeight = FontWeight.SemiBold,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

internal fun formatTimer(totalSeconds: Long): String {
    if (totalSeconds <= 0) return "--"
    val d = totalSeconds / 86400
    val h = (totalSeconds % 86400) / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return when {
        d > 0 -> "${d}d ${h}h ${m}m"
        h > 0 -> "${h}h ${m}m ${s}s"
        else -> "${m}m ${s}s"
    }
}

@Composable
fun ProfileAvatar(avatarUrl: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = if (LocalComicTheme.current) {
            modifier.comicPanel(CircleShape, offset = 2.dp)
        } else {
            modifier
                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .padding(2.dp)
        },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                ArtworkImage(
                    name = "profile",
                    artist = "avatar",
                    embeddedUrl = avatarUrl,
                    fallbackIcon = Icons.Filled.AccountCircle,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
internal fun TelemetryTabStrip(
    sortMode: HomeSortMode,
    onSortModeChange: (HomeSortMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val isDark = LocalIsDarkTheme.current
    val isComic = LocalComicTheme.current
    var lastTrackMode by rememberSaveable { mutableStateOf(HomeSortMode.MOST_PLAYED) }
    LaunchedEffect(sortMode) {
        if (sortMode == HomeSortMode.MOST_PLAYED || sortMode == HomeSortMode.LAST_30_DAYS || sortMode == HomeSortMode.LAST_7_DAYS) {
            lastTrackMode = sortMode
        }
    }
    val activeTab = when (sortMode) {
        HomeSortMode.TOP_ARTISTS -> TelemetryPrimaryTab.ARTISTS
        HomeSortMode.TOP_ALBUMS -> TelemetryPrimaryTab.ALBUMS
        HomeSortMode.RECENT -> TelemetryPrimaryTab.HISTORY
        HomeSortMode.MOST_PLAYED, HomeSortMode.LAST_7_DAYS, HomeSortMode.LAST_30_DAYS -> TelemetryPrimaryTab.TRACKS
    }
    var periodMenuOpen by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                TelemetryPrimaryTab.entries.forEach { tab ->
                    val selected = tab == activeTab
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                val target = when (tab) {
                                    TelemetryPrimaryTab.TRACKS -> lastTrackMode
                                    TelemetryPrimaryTab.ARTISTS -> HomeSortMode.TOP_ARTISTS
                                    TelemetryPrimaryTab.ALBUMS -> HomeSortMode.TOP_ALBUMS
                                    TelemetryPrimaryTab.HISTORY -> HomeSortMode.RECENT
                                }
                                onSortModeChange(target)
                            }
                            .padding(top = 10.dp),
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(if (selected) 28.dp else 0.dp)
                                .height(2.dp)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(1.dp),
                                ),
                        )
                    }
                }
            }

            if (activeTab == TelemetryPrimaryTab.TRACKS) {
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .uiClip(RoundedCornerShape(10.dp))
                            .clickable { periodMenuOpen = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = when (lastTrackMode) {
                                HomeSortMode.LAST_7_DAYS -> "7d"
                                HomeSortMode.LAST_30_DAYS -> "30d"
                                else -> "All"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontFeatureSettings = "tnum",
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            Icons.Filled.ExpandMore,
                            contentDescription = "Timeframe",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = periodMenuOpen,
                        onDismissRequest = { periodMenuOpen = false },
                        shape = RoundedCornerShape(18.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        PeriodOption("All Time", lastTrackMode == HomeSortMode.MOST_PLAYED) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSortModeChange(HomeSortMode.MOST_PLAYED)
                            periodMenuOpen = false
                        }
                        PeriodOption("Last 30 Days", lastTrackMode == HomeSortMode.LAST_30_DAYS) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSortModeChange(HomeSortMode.LAST_30_DAYS)
                            periodMenuOpen = false
                        }
                        PeriodOption("Last 7 Days", lastTrackMode == HomeSortMode.LAST_7_DAYS) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSortModeChange(HomeSortMode.LAST_7_DAYS)
                            periodMenuOpen = false
                        }
                    }
                }
            }
        }
        HorizontalDivider(
            thickness = if (isComic) 1.5.dp else 0.75.dp,
            color = if (isComic) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.35f else 0.5f),
        )
    }
}

@Composable
private fun PeriodOption(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        },
        trailingIcon = {
            if (active) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        },
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
    )
}

@Composable
internal fun DateHeaderRow(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
    )
}
