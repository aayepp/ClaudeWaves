package com.lastwave.app.ui.common

import com.lastwave.app.ui.theme.ComicPink
import com.lastwave.app.ui.theme.ComicMint
import com.lastwave.app.ui.theme.ComicGold
import com.lastwave.app.ui.theme.ComicInk
import com.lastwave.app.ui.theme.LocalComicTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.R
import com.lastwave.app.data.lossless.LosslessAvailabilityRepository
import com.lastwave.app.data.lossless.LosslessListTier
import com.lastwave.app.data.lossless.LosslessProbeTarget
import com.lastwave.app.data.lossless.losslessTrackKey
import com.lastwave.app.ui.player.QualityGlyphs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@HiltViewModel
class LosslessBadgeViewModel @Inject constructor(
    private val repository: LosslessAvailabilityRepository,
) : ViewModel() {
    val badges: StateFlow<Map<String, LosslessListTier>> = repository.badges
    val downloadedKeys: StateFlow<Set<String>> = repository.downloadedKeys
    val secondaryBadges: StateFlow<Map<String, LosslessListTier>> = repository.secondaryBadges
    val prefersAtmos: StateFlow<Boolean> = repository.prefersAtmos

    fun queueProbe(targets: List<LosslessProbeTarget>) {
        repository.queueProbe(targets)
    }
}

val LocalLosslessBadgeViewModel = staticCompositionLocalOf<LosslessBadgeViewModel?> { null }

/**
 * Compact inline badges for a song: its HI-RES / LOSSLESS / ATMOS tier (from
 * downloaded files, live playback results and lazy viewport probes) and a
 * downloaded mark when the app has it on this phone.
 */
@Composable
fun LosslessTrackBadge(
    title: String,
    artist: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val viewModel = LocalLosslessBadgeViewModel.current ?: return
    val key = remember(title, artist) { losslessTrackKey(title, artist) }
    if (key.isBlank()) return

    val tier by remember(viewModel, key) {
        viewModel.badges
            .map { map -> map[key]?.takeIf { it != LosslessListTier.NONE } }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = viewModel.badges.value[key]?.takeIf { it != LosslessListTier.NONE })
    val downloaded by remember(viewModel, key) {
        viewModel.downloadedKeys.map { key in it }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = key in viewModel.downloadedKeys.value)
    // "HI-RES, ATMOS": what plays, then the other version that exists. With Atmos as the
    // streaming quality the order flips and the stereo tier is drawn as an outline.
    val second by remember(viewModel, key) {
        viewModel.secondaryBadges.map { it[key] }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = viewModel.secondaryBadges.value[key])
    val prefersAtmos by viewModel.prefersAtmos.collectAsStateWithLifecycle()

    if (tier == null && !downloaded) return
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tier?.let { QualityTierBadge(tier = it, compact = compact) }
        second?.takeIf { it != tier }?.let { QualityTierBadge(tier = it, compact = compact, outlined = prefersAtmos) }
        if (downloaded) DownloadedMark(compact)
    }
}

/**
 * "On this phone": the song plays offline. Accent colour on the theme's own
 * surfaces; over artwork (the player queue) it follows that backdrop instead.
 */
@Composable
private fun DownloadedMark(compact: Boolean) {
    val tint = when (LocalBadgeOnDarkBackdrop.current) {
        true -> Color.White.copy(alpha = 0.9f)
        false -> Color.Black.copy(alpha = 0.75f)
        null -> MaterialTheme.colorScheme.primary
    }
    Icon(
        imageVector = Icons.Filled.DownloadDone,
        contentDescription = stringResource(R.string.track_downloaded_cd),
        tint = tint,
        modifier = Modifier.size(if (compact) 12.dp else 13.dp),
    )
}

/**
 * Whether the surface behind quality badges is dark, when it isn't the theme's
 * own (the player queue sits on artwork). Null follows the theme.
 */
val LocalBadgeOnDarkBackdrop = compositionLocalOf<Boolean?> { null }

/** The HI-RES / LOSSLESS / ATMOS pill itself, shared by song lists and the signal path. */
@Composable
fun QualityTierBadge(
    tier: LosslessListTier,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    large: Boolean = false,
    /** Outline only: a version that exists but isn't the one that plays. */
    outlined: Boolean = false,
) {
    val isDark = LocalBadgeOnDarkBackdrop.current ?: (MaterialTheme.colorScheme.surface.luminance() < 0.5f)

    val label: String
    val icon: ImageVector
    val containerColor: Color
    val contentColor: Color
    when (tier) {
        LosslessListTier.HI_RES -> {
            label = stringResource(R.string.lossless_badge_hires)
            icon = QualityGlyphs.HiResLossless
            contentColor = if (isDark) Color(0xFFFFD60A) else Color(0xFF946800)
            containerColor = Color(0xFFFFD60A).copy(alpha = if (isDark) 0.16f else 0.24f)
        }
        LosslessListTier.LOSSLESS -> {
            label = stringResource(R.string.lossless_badge_cd)
            icon = QualityGlyphs.Lossless
            contentColor = if (isDark) Color(0xFF5AE8DC) else Color(0xFF006D65)
            containerColor = if (isDark) {
                Color(0xFF5AE8DC).copy(alpha = 0.14f)
            } else {
                Color(0xFF00897B).copy(alpha = 0.13f)
            }
        }
        LosslessListTier.ATMOS -> {
            label = stringResource(R.string.lossless_badge_atmos)
            icon = QualityGlyphs.Spatial
            // Fixed hues like the other tiers, so the dark/light pick holds on any backdrop.
            contentColor = if (isDark) Color(0xFFD7BBFF) else Color(0xFF5B2A99)
            containerColor = Color(0xFF9C6BFF).copy(alpha = if (isDark) 0.18f else 0.14f)
        }
        LosslessListTier.NONE -> return
    }
    if (LocalComicTheme.current) {
        // Comic Pop: solid comic colour and ink, like its buttons, instead of a faint tint.
        val comicFill = when (tier) {
            LosslessListTier.HI_RES -> ComicGold
            LosslessListTier.LOSSLESS -> ComicMint
            else -> ComicPink
        }
        if (outlined) {
            // Comic colours read on the night page; ink reads on paper.
            val line = if (isDark) comicFill else ComicInk
            QualityBadge(label, icon, line, Color.Transparent, modifier, compact, large, outline = line)
        } else {
            QualityBadge(label, icon, ComicInk, comicFill, modifier, compact, large)
        }
        return
    }
    if (outlined) {
        QualityBadge(label, icon, contentColor, Color.Transparent, modifier, compact, large, outline = contentColor.copy(alpha = 0.6f))
    } else {
        QualityBadge(label, icon, contentColor, containerColor, modifier, compact, large)
    }
}

/**
 * The badge shape every quality shares: song lists use the small sizes, Now
 * Playing the [large] one (also for lossy streams, which lists never badge).
 */
@Composable
fun QualityBadge(
    label: String,
    icon: ImageVector,
    contentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    large: Boolean = false,
    outline: Color? = null,
) {
    // Comic Pop inks every solid badge's edge, as it does its buttons.
    val inked = LocalComicTheme.current && containerColor.alpha >= 0.99f
    val strokeWidth = if (large) 1.5.dp else 1.dp
    Surface(
        shape = RoundedCornerShape(if (large) 6.dp else 4.dp),
        color = containerColor,
        border = when {
            outline != null -> BorderStroke(strokeWidth, outline)
            inked -> BorderStroke(strokeWidth, ComicInk)
            else -> null
        },
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                when {
                    large -> 4.dp
                    compact -> 2.5.dp
                    else -> 3.dp
                },
            ),
            modifier = Modifier.padding(
                horizontal = when {
                    large -> 7.dp
                    compact -> 4.dp
                    else -> 5.dp
                },
                vertical = when {
                    large -> 3.5.dp
                    compact -> 1.dp
                    else -> 1.5.dp
                },
            ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(
                    when {
                        large -> 15.dp
                        compact -> 10.dp
                        else -> 11.dp
                    },
                ),
            )
            Text(
                text = label,
                color = contentColor,
                fontSize = when {
                    large -> 12.sp
                    compact -> 8.5.sp
                    else -> 9.sp
                },
                lineHeight = when {
                    large -> 14.sp
                    compact -> 10.sp
                    else -> 11.sp
                },
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                maxLines = 1,
            )
        }
    }
}

/**
 * Renders a track's single-line subtitle (artist/album metadata) alongside its
 * inline [LosslessTrackBadge] so trailing icons and durations stay uncluttered.
 */
@Composable
fun TrackSubtitleWithBadge(
    subtitle: String,
    title: String,
    artist: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    compactBadge: Boolean = false,
    prefix: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        prefix?.invoke()
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = style,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        LosslessTrackBadge(
            title = title,
            artist = artist,
            compact = compactBadge,
        )
    }
}

/**
 * Watches visible item keys in [listState], waits briefly for scroll momentum
 * to settle, and queues metadata-only probes for visible tracks in [keyToTarget].
 */
@Composable
fun ObserveVisibleLosslessProbes(
    listState: LazyListState,
    keyToTarget: Map<String, LosslessProbeTarget>,
) {
    val viewModel = LocalLosslessBadgeViewModel.current ?: return
    val latestMap by rememberUpdatedState(keyToTarget)
    val visibleKeys by remember(listState) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? String }
        }
    }
    LaunchedEffect(visibleKeys, keyToTarget) {
        if (visibleKeys.isEmpty() || latestMap.isEmpty()) return@LaunchedEffect
        delay(280L)
        val targets = visibleKeys.mapNotNull { latestMap[it] }
        if (targets.isNotEmpty()) {
            viewModel.queueProbe(targets)
        }
    }
}

/**
 * Queues lossless metadata probes for a visible non-LazyColumn batch (horizontal
 * shelves, stacked home cards) after a brief settle delay.
 */
@Composable
fun ObserveLosslessProbeTargets(targets: List<LosslessProbeTarget>) {
    val viewModel = LocalLosslessBadgeViewModel.current ?: return
    val distinctTargets = remember(targets) {
        targets.filter { it.title.isNotBlank() && it.artist.isNotBlank() }
            .distinctBy { losslessTrackKey(it.title, it.artist) }
    }
    LaunchedEffect(distinctTargets) {
        if (distinctTargets.isEmpty()) return@LaunchedEffect
        delay(280L)
        viewModel.queueProbe(distinctTargets)
    }
}
