package com.lastwave.app.ui.feed

import androidx.compose.ui.res.painterResource
import com.lastwave.app.data.lossless.LosslessProbeTarget
import com.lastwave.app.ui.common.ObserveLosslessProbeTargets
import com.lastwave.app.ui.common.TrackSubtitleWithBadge
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.animation.AnimatedVisibility
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.ComicAccent
import com.lastwave.app.ui.theme.ComicInk
import com.lastwave.app.ui.theme.comicHalftone
import com.lastwave.app.ui.theme.comicPanel
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import com.lastwave.app.ui.player.DropRowBoundsHolder
import com.lastwave.app.ui.player.LiquidDropKind
import com.lastwave.app.ui.player.LocalMiniPlayerDropController
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.activity.compose.BackHandler
import com.lastwave.app.playback.toPlayableTrack
import androidx.compose.material.icons.filled.Download
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Mic
import com.lastwave.app.data.music.toYouTubeMusicTrack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import com.lastwave.app.ui.comic.AlertDialog
import com.lastwave.app.ui.comic.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import com.lastwave.app.ui.comic.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.lastwave.app.ui.comic.Surface
import androidx.compose.material3.Text
import com.lastwave.app.ui.comic.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.layout.onSizeChanged
import com.lastwave.app.ui.theme.liquidGlassSource
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.SharedFlow
import com.lastwave.app.ui.navigation.FEED_TAP_DEBOUNCE_MS
import com.lastwave.app.ui.navigation.FeedTapAction
import com.lastwave.app.ui.navigation.resolveFeedTapAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import com.lastwave.app.ui.comic.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import com.lastwave.app.R
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lastwave.app.ui.feed.cards.CardTokens
import com.lastwave.app.ui.feed.cards.CardTextArea
import com.lastwave.app.ui.feed.cards.rememberSquareCardWidth
import com.lastwave.app.ui.feed.cards.rememberWideCardWidth
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lastwave.app.data.feed.FeedAlbum
import com.lastwave.app.data.feed.FeedArtist
import com.lastwave.app.data.feed.FeedQuickTile
import com.lastwave.app.data.feed.FeedSpotlight
import com.lastwave.app.util.ArtistHelper
import com.lastwave.app.data.generate.GeneratedTrack
import com.lastwave.app.data.local.HomeSection
import com.lastwave.app.data.local.DEFAULT_SECTION_ORDER
import com.lastwave.app.data.model.FriendEntry
import com.lastwave.app.data.model.RecentTrack
import com.lastwave.app.data.music.YouTubeMusicTrack
import com.lastwave.app.data.music.YouTubePlaylistSummary
import com.lastwave.app.data.music.HomeChip
import com.lastwave.app.data.music.HomeShelf
import com.lastwave.app.data.music.HomeShelfItem
import com.lastwave.app.playback.PlayableTrack
import com.lastwave.app.ui.common.ArtworkImage
import com.lastwave.app.ui.common.ExpressiveHeader
import com.lastwave.app.ui.common.ExpressiveShuffleButton
import com.lastwave.app.ui.common.HeaderActionIcon
import com.lastwave.app.ui.common.TrackContextMenuSheet
import com.lastwave.app.ui.common.TrackMiniTrayData
import com.lastwave.app.ui.common.TrackMiniTraySheet
import com.lastwave.app.ui.common.TopCurvedSectionClipShape
import com.lastwave.app.ui.common.WindowSizeClass
import com.lastwave.app.ui.common.rememberWindowSizeClass
import com.lastwave.app.ui.common.TrackMenuCapabilities
import com.lastwave.app.ui.common.TrackMenuTarget
import com.lastwave.app.ui.common.ExpressiveInlineLoadingIndicator
import com.lastwave.app.ui.common.adaptiveContentWidth
import com.lastwave.app.ui.common.safeHorizontalContentPadding
import com.lastwave.app.ui.navigation.ArtistAlbumNavigator
import com.lastwave.app.ui.player.LocalMusicPlayer
import com.lastwave.app.ui.player.PlayingWaveBars
import com.lastwave.app.ui.shell.FloatingNavDefaults
import com.lastwave.app.ui.podcast.PodcastShowRow



internal fun String.displayName(): String =
    split(" ", "-", "_").filter { it.isNotBlank() }.joinToString(" ") {
        it.replaceFirstChar { c -> c.uppercase() }
    }

internal fun relativeTime(context: android.content.Context, uts: String?): String? {
    val epoch = uts?.toLongOrNull() ?: return null
    val now = System.currentTimeMillis() / 1000
    val diff = (now - epoch).coerceAtLeast(0)
    return when {
        diff < 3600 -> context.getString(R.string.feed_ago_min, (diff / 60).coerceAtLeast(1).toInt())
        diff < 86400 -> context.getString(R.string.feed_ago_hour, (diff / 3600).toInt())
        diff < 86400 * 7 -> context.getString(R.string.feed_ago_day, (diff / 86400).toInt())
        diff < 86400 * 30 -> context.getString(R.string.feed_ago_week, (diff / (86400 * 7)).toInt())
        else -> context.getString(R.string.feed_ago_month, (diff / (86400 * 30)).toInt())
    }
}

@Composable
internal fun AudioWavesBrandMark(
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val wavesBrush = remember {
        Brush.linearGradient(
            listOf(
                Color(0xFF0ECAE8),
                Color(0xFF267CFF),
                Color(0xFFB05CFF),
                Color(0xFFFA4C6E),
            ),
        )
    }
    val subtitleBrush = remember {
        Brush.linearGradient(
            listOf(
                Color(0xFF82E8F8),
                Color(0xFF8AB4FF),
                Color(0xFFCCA0FF),
                Color(0xFFFF98AE),
            ),
        )
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // The launcher icon itself, so Home and the home screen show the same mark.
        Surface(
            shape = CircleShape,
            color = colorResource(R.color.ic_launcher_wave_background),
            modifier = Modifier
                .size(if (subtitle != null) 38.dp else 32.dp)
                .comicPanel(CircleShape, offset = 2.dp),
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_launcher_wave_badge),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(verticalArrangement = Arrangement.Center) {
            // The build's own name, two-tone like the logo: its "waves" in the 4-color brand gradient.
            val name = stringResource(R.string.app_name)
            val split = name.lastIndexOf("waves", ignoreCase = true).takeIf { it > 0 } ?: name.length
            Text(
                text = buildAnnotatedString {
                    append(name.take(split))
                    withStyle(SpanStyle(brush = wavesBrush)) { append(name.drop(split)) }
                },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 21.sp,
                    letterSpacing = (-0.5).sp,
                ),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(brush = subtitleBrush),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun HomeSearchPill(
    onOpenSearch: () -> Unit,
    onOpenVibe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val comic = LocalComicTheme.current
    val haptics = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            onOpenSearch()
        },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
        border = if (comic) null else BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
        ),
        modifier = modifier
            .height(44.dp)
            .comicPanel(CircleShape, offset = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.search_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
            Text(
                text = "Search songs, artists, albums...",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Surface(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onOpenVibe()
                },
                shape = CircleShape,
                color = if (comic) ComicAccent else MaterialTheme.colorScheme.primaryContainer,
                border = if (comic) BorderStroke(1.5.dp, ComicInk) else null,
                modifier = Modifier.height(32.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = if (comic) ComicInk else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = "Vibe",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (comic) ComicInk else MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
internal fun InfiniteRadioHero(
    quickPicks: List<YouTubeMusicTrack>,
    onPlay: () -> Unit,
    onShuffle: () -> Unit = onPlay,
    topArtists: List<FeedArtist> = emptyList(),
    horizontalPadding: Dp = 16.dp,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val comic = LocalComicTheme.current
    val heroArt = quickPicks.firstOrNull { !it.artworkUrl.isNullOrBlank() }?.artworkUrl
    val heroShape = RoundedCornerShape(22.dp)
    val waveMixTitle = remember {
        when (java.time.LocalTime.now().hour) {
            in 5..11 -> "Your Morning Wave"
            in 12..16 -> "Your Afternoon Wave"
            in 17..21 -> "Your Evening Wave"
            else -> "Your Night Wave"
        }
    }
    val mixSubtitle = remember(quickPicks, topArtists) {
        val artistNames = (
            quickPicks.map { ArtistHelper.primaryArtist(it.artist) } +
                topArtists.map { ArtistHelper.primaryArtist(it.name) }
            )
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(3)
        if (artistNames.isNotEmpty()) {
            "${artistNames.joinToString(" · ")} · Infinite mix"
        } else {
            "An endless station shaped by your listening"
        }
    }
    val primaryWaveColor = if (comic) ComicAccent else MaterialTheme.colorScheme.primary
    val secondaryWaveColor = MaterialTheme.colorScheme.tertiary

    Surface(
        shape = heroShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .comicPanel(heroShape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            if (!heroArt.isNullOrBlank()) {
                AsyncImage(
                    model = heroArt,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.24f),
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                            ),
                        ),
                    )
                    .comicHalftone(),
            )
            // Static audio waveform signature in the upper right quadrant — reinforces the AudioWaves player identity cleanly.
            Canvas(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 14.dp, end = 14.dp)
                    .width(92.dp)
                    .height(36.dp),
            ) {
                val barCount = 15
                val stepX = size.width / barCount
                val centerY = size.height / 2f
                val amplitudes = floatArrayOf(0.28f, 0.45f, 0.72f, 0.52f, 0.92f, 0.64f, 0.98f, 0.58f, 0.84f, 0.48f, 0.76f, 0.42f, 0.60f, 0.34f, 0.22f)
                for (i in 0 until barCount) {
                    val h = (size.height * amplitudes[i]).coerceAtLeast(4f)
                    val x = i * stepX + stepX * 0.25f
                    drawRoundRect(
                        color = primaryWaveColor.copy(alpha = 0.72f),
                        topLeft = Offset(x, centerY - h / 2f),
                        size = Size(stepX * 0.48f, h),
                        cornerRadius = CornerRadius(4f, 4f),
                    )
                }
                val wavePath = Path()
                val segments = 32
                for (s in 0..segments) {
                    val t = s.toFloat() / segments
                    val x = t * size.width
                    val envelope = sin(t * PI).toFloat()
                    val y = centerY + sin(t * 4f * PI).toFloat() * (size.height * 0.34f) * envelope
                    if (s == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
                }
                drawPath(
                    path = wavePath,
                    color = secondaryWaveColor.copy(alpha = 0.6f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (comic) ComicAccent else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    border = if (comic) BorderStroke(1.5.dp, ComicInk) else null,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = if (comic) ComicInk else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            "LIVE RADIO · INFINITE MIX",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.9.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (comic) ComicInk else MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = waveMixTitle,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 23.sp,
                            letterSpacing = (-0.4).sp,
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = mixSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onPlay()
                        },
                        enabled = quickPicks.isNotEmpty(),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (comic) ComicAccent else MaterialTheme.colorScheme.primary,
                            contentColor = if (comic) ComicInk else MaterialTheme.colorScheme.onPrimary,
                        ),
                        border = if (comic) BorderStroke(1.5.dp, ComicInk) else null,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Stream Now", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                    }
                    Surface(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onShuffle()
                        },
                        enabled = quickPicks.isNotEmpty(),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                        border = BorderStroke(
                            1.5.dp,
                            if (comic) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                        ),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                Icons.Filled.Shuffle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = "Shuffle",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TasteStrip(
    tags: List<String>,
    launching: String?,
    onTagClick: (String) -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FeedSectionHeader(
            title = stringResource(R.string.feed_your_sound),
            subtitle = stringResource(R.string.feed_your_sound_sub),
            onLongClick = onLongClick,
        )
        // Justified flow on every theme (DESIGN.md 3a).
        JustifiedChipFlowLayout(
            horizontalGap = 8.dp,
            verticalGap = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 6.dp, bottom = 2.dp),
        ) {
            tags.take(10).forEach { tag ->
                TasteTagPill(
                    tag = tag,
                    isLoading = launching?.equals(tag.displayName(), ignoreCase = true) == true,
                    onClick = { onTagClick(tag) },
                )
            }
        }
    }
}

@Composable
private fun TasteTagPill(
    tag: String,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
        modifier = Modifier.comicPanel(CircleShape, offset = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        ) {
            if (!LocalComicTheme.current) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
            Text(
                tag.displayName(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isLoading) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.feed_start_radio_cd),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/**
 * Packs pills into horizontal rows within [maxWidth] and distributes each row's remaining width
 * evenly across its pills so every row starts and ends flush with the container's 16dp grid.
 */
@Composable
internal fun JustifiedChipFlowLayout(
    horizontalGap: Dp,
    verticalGap: Dp,
    modifier: Modifier = Modifier,
    balanceRows: Boolean = true,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        if (measurables.isEmpty()) {
            return@Layout layout(constraints.minWidth, 0) {}
        }
        val maxWidth = constraints.maxWidth
        val gapX = horizontalGap.roundToPx()
        val gapY = verticalGap.roundToPx()

        val items = measurables.mapIndexed { index, measurable ->
            val natural = measurable.maxIntrinsicWidth(constraints.maxHeight).coerceAtMost(maxWidth)
            index to natural
        }

        val rows = mutableListOf<MutableList<Pair<Int, Int>>>()
        var currentRow = mutableListOf<Pair<Int, Int>>()
        var currentWidth = 0
        for (entry in items) {
            val itemWidth = entry.second
            val nextWidth = if (currentRow.isEmpty()) itemWidth else currentWidth + gapX + itemWidth
            if (currentRow.isNotEmpty() && nextWidth > maxWidth) {
                rows.add(currentRow)
                currentRow = mutableListOf(entry)
                currentWidth = itemWidth
            } else {
                currentRow.add(entry)
                currentWidth = nextWidth
            }
        }
        if (currentRow.isNotEmpty()) rows.add(currentRow)

        if (balanceRows && rows.size >= 2) {
            // Pull trailing pills down when the final row is sparse so stretched pills stay visually proportional.
            var moved = true
            while (moved) {
                moved = false
                for (i in rows.lastIndex downTo 1) {
                    val prev = rows[i - 1]
                    val curr = rows[i]
                    if (prev.size - curr.size >= 2) {
                        val candidate = prev.last()
                        val currWidthWithCandidate = curr.sumOf { it.second } + gapX * curr.size + candidate.second
                        if (currWidthWithCandidate <= maxWidth) {
                            prev.removeAt(prev.lastIndex)
                            curr.add(0, candidate)
                            moved = true
                        }
                    }
                }
            }
        }

        val placeableRows = rows.map { row ->
            val usedNatural = row.sumOf { it.second } + gapX * (row.size - 1)
            val extra = (maxWidth - usedNatural).coerceAtLeast(0)
            val perItemExtra = extra / row.size
            val remainder = extra % row.size
            row.mapIndexed { colIndex, (index, naturalWidth) ->
                val targetWidth = (naturalWidth + perItemExtra + if (colIndex < remainder) 1 else 0)
                    .coerceAtMost(maxWidth)
                measurables[index].measure(
                    Constraints(
                        minWidth = targetWidth,
                        maxWidth = targetWidth,
                        minHeight = 0,
                        maxHeight = constraints.maxHeight,
                    ),
                )
            }
        }

        val rowHeights = placeableRows.map { row -> row.maxOf { it.height } }
        val totalHeight = rowHeights.sum() + gapY * (placeableRows.size - 1)
        val layoutHeight = constraints.constrainHeight(totalHeight)

        layout(maxWidth, layoutHeight) {
            var y = 0
            placeableRows.forEachIndexed { rowIdx, row ->
                var x = 0
                val rowH = rowHeights[rowIdx]
                row.forEach { placeable ->
                    placeable.placeRelative(x, y + (rowH - placeable.height) / 2)
                    x += placeable.width + gapX
                }
                y += rowH + gapY
            }
        }
    }
}

@Composable
internal fun PinnedChipsBar(
    chips: List<HomeChip>,
    selectedChip: HomeChip?,
    onChipClick: (HomeChip?) -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
) {
    if (chips.isEmpty()) return

    // Every theme (DESIGN.md 3a): measure exactly the chips that fit inside 16.dp..16.dp and stretch the row edge-to-edge with a More tray for extras.
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val measureStyle = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
    var overflowExpanded by remember { mutableStateOf(false) }

    val rowMaxWidth = (
        androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp.coerceAtMost(1160.dp) -
            horizontalPadding * 2 - 3.dp
        ).coerceAtLeast(120.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = 4.dp)
            .padding(end = 3.dp, bottom = 2.dp),
    ) {
        val gapDp = 8.dp
        val chipPadDp = 30.dp
        val chevronReserveDp = 20.dp
        val chipWidths = remember(chips, density, measureStyle) {
            chips.map { chip ->
                with(density) { textMeasurer.measure(chip.title, measureStyle).size.width.toDp() } + chipPadDp
            }
        }
        val allChipWidth = remember(density, measureStyle) {
            with(density) { textMeasurer.measure("All", measureStyle).size.width.toDp() } + chipPadDp
        }
        val moreIdleWidth = remember(density, measureStyle) {
            with(density) { textMeasurer.measure("More", measureStyle).size.width.toDp() } + chipPadDp + chevronReserveDp
        }

        val splitIndex = remember(chips, selectedChip, rowMaxWidth, chipWidths, allChipWidth, moreIdleWidth) {
            val allTotal = allChipWidth + chipWidths.fold(0.dp) { acc, w -> acc + gapDp + w }
            if (allTotal <= rowMaxWidth) {
                chips.size
            } else {
                val activeWidth = selectedChip?.let { sel ->
                    with(density) { textMeasurer.measure(sel.title, measureStyle).size.width.toDp() } + chipPadDp + chevronReserveDp
                } ?: 0.dp
                val reservedToggle = maxOf(moreIdleWidth, activeWidth)
                var used = allChipWidth
                var count = 0
                for (w in chipWidths) {
                    val nextUsed = used + gapDp + w
                    if (nextUsed + gapDp + reservedToggle <= rowMaxWidth) {
                        used = nextUsed
                        count++
                    } else {
                        break
                    }
                }
                count
            }
        }

        val visibleChips = remember(chips, splitIndex) { chips.take(splitIndex) }
        val overflowChips = remember(chips, splitIndex) { chips.drop(splitIndex) }
        val activeOverflow = overflowChips.firstOrNull {
            selectedChip == it || selectedChip?.title.equals(it.title, ignoreCase = true)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            JustifiedChipFlowLayout(
                horizontalGap = gapDp,
                verticalGap = 8.dp,
                balanceRows = false,
                modifier = Modifier.fillMaxWidth(),
            ) {
                MoodChip(
                    label = "All",
                    selected = selectedChip == null,
                    onClick = {
                        overflowExpanded = false
                        onChipClick(null)
                    },
                )
                visibleChips.forEach { chip ->
                    val isSelected = selectedChip == chip || selectedChip?.title.equals(chip.title, ignoreCase = true)
                    MoodChip(
                        label = chip.title,
                        selected = isSelected,
                        onClick = {
                            overflowExpanded = false
                            onChipClick(chip)
                        },
                    )
                }
                if (overflowChips.isNotEmpty()) {
                    MoodChip(
                        label = activeOverflow?.title ?: "More",
                        selected = activeOverflow != null || overflowExpanded,
                        trailingIcon = if (overflowExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        onClick = { overflowExpanded = !overflowExpanded },
                    )
                }
            }
            AnimatedVisibility(visible = overflowExpanded && overflowChips.isNotEmpty()) {
                JustifiedChipFlowLayout(
                    horizontalGap = gapDp,
                    verticalGap = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                ) {
                    overflowChips.forEach { chip ->
                        val isSelected = selectedChip == chip || selectedChip?.title.equals(chip.title, ignoreCase = true)
                        MoodChip(
                            label = chip.title,
                            selected = isSelected,
                            onClick = {
                                overflowExpanded = false
                                onChipClick(chip)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun MoodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    trailingIcon: ImageVector? = null,
    horizontalPadding: Dp = 14.dp,
) {
    val haptics = LocalHapticFeedback.current
    val comic = LocalComicTheme.current
    val background by androidx.compose.animation.animateColorAsState(
        targetValue = when {
            !selected -> MaterialTheme.colorScheme.surfaceContainerHigh
            comic -> ComicAccent
            else -> MaterialTheme.colorScheme.primaryContainer
        },
        label = "moodChipBackground",
    )
    val textColor by androidx.compose.animation.animateColorAsState(
        targetValue = when {
            !selected -> MaterialTheme.colorScheme.onSurfaceVariant
            comic -> ComicInk
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        },
        label = "moodChipText",
    )
    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = CircleShape,
        color = background,
        modifier = Modifier.height(32.dp).comicPanel(CircleShape, offset = 2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxHeight().padding(horizontal = horizontalPadding),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
internal fun ExploreByVibeSection(
    chips: List<HomeChip>,
    selectedChip: HomeChip?,
    onChipClick: (HomeChip?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (chips.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FeedSectionHeader(
            title = "Explore by vibe",
            subtitle = "Tap a mood to filter your feed",
        )
        PinnedChipsBar(
            chips = chips,
            selectedChip = selectedChip,
            onChipClick = onChipClick,
        )
    }
}

@Composable
internal fun QuickTilesGrid(
    tiles: List<FeedQuickTile>,
    onTileClick: (FeedQuickTile) -> Unit,
    wideBento: Boolean = false,
    modifier: Modifier = Modifier,
) {
    when {
        wideBento -> {
            val rows = remember(tiles) { tiles.take(4).chunked(2) }
            Column(
                modifier = modifier.height(160.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rows.forEach { rowTiles ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowTiles.forEach { tile ->
                            QuickTileCard(
                                tile = tile,
                                onClick = { onTileClick(tile) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                compactHeight = Dp.Unspecified,
                            )
                        }
                        if (rowTiles.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        else -> {
            val pages = remember(tiles) { tiles.chunked(2) }
            val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 2.dp),
            ) {
                HorizontalPager(
                    state = pagerState,
                    pageSpacing = 12.dp,
                    // Asymmetric 15.dp outer + 1.dp inner padding keeps cards on the 16.dp grid without clipping comic strokes.
                    contentPadding = PaddingValues(horizontal = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                ) { pageIndex ->
                    val rowTiles = pages[pageIndex]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowTiles.forEach { tile ->
                            QuickTileCard(
                                tile = tile,
                                onClick = { onTileClick(tile) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rowTiles.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                if (pages.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(pages.size) { dotIndex ->
                            val active = pagerState.currentPage == dotIndex
                            Box(
                                modifier = Modifier
                                    .size(width = if (active) 16.dp else 6.dp, height = 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun QuickTileCard(
    tile: FeedQuickTile,
    modifier: Modifier = Modifier,
    compactHeight: Dp = 64.dp,
    onClick: () -> Unit,
) {
    val tileShape = RoundedCornerShape(16.dp)

    val isYtLikedTile = tile.collection == "yt_liked" || tile.playlistId == "yt_liked"
    val isLocalLikedTile = tile.isLiked && !isYtLikedTile
    val isLikedTile = isYtLikedTile || isLocalLikedTile
    val isMixTile = tile.collection == "mix" || tile.collection == "discover_mix" || tile.title.contains("Discover Mix", ignoreCase = true)
    val isMyMixTile = tile.collection == "my_mix" || tile.title.equals("My Mix", ignoreCase = true)
    val isDiscoverTile = tile.collection == "radio" || tile.title.equals("Discover", ignoreCase = true)
    val isNewReleasesTile = tile.collection == "new_releases"
    /** Shows a glyph rather than a cover. */
    val iconTile = isLikedTile || isMixTile || isMyMixTile || isDiscoverTile || isNewReleasesTile

    val tileColor = when {
        isLikedTile -> MaterialTheme.colorScheme.primaryContainer
        isMixTile -> MaterialTheme.colorScheme.secondaryContainer
        isMyMixTile -> MaterialTheme.colorScheme.surfaceVariant
        isDiscoverTile -> MaterialTheme.colorScheme.tertiaryContainer
        isNewReleasesTile -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
    }
    val finalSubtitle = if (isNewReleasesTile) "Fresh drops" else tile.subtitle ?: if (tile.actionVideoId != null) "Track" else "Playlist"

    @Composable
    fun TileGlyph(iconSize: Dp) {
        when {
            isYtLikedTile -> Icon(Icons.Filled.ThumbUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(iconSize))
            isLocalLikedTile -> Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(iconSize))
            isMixTile -> Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(iconSize))
            isMyMixTile -> Icon(Icons.Filled.AllInclusive, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(iconSize))
            isDiscoverTile -> Icon(Icons.Filled.Explore, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(iconSize))
            isNewReleasesTile -> Icon(Icons.Filled.NewReleases, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(iconSize))
            else -> ArtworkImage(
                name = tile.title,
                artist = tile.subtitle ?: "",
                embeddedUrl = tile.artworkUrl,
                fallbackIcon = Icons.Filled.Album,
                preferEmbedded = tile.actionVideoId == null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    Surface(
        onClick = onClick,
        shape = tileShape,
        color = tileColor,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.10f),
        ),
        modifier = modifier
            .then(if (compactHeight != Dp.Unspecified) Modifier.height(compactHeight) else Modifier)
            .comicPanel(tileShape),
    ) {
        val denseBento = compactHeight == Dp.Unspecified
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (denseBento) 10.dp else 12.dp, vertical = if (denseBento) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (denseBento) 8.dp else 10.dp),
        ) {
            Box(
                modifier = if (iconTile) Modifier.size(if (denseBento) 24.dp else 28.dp) else Modifier.size(if (denseBento) 36.dp else 40.dp).clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                TileGlyph(if (denseBento) 22.dp else 26.dp)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = if (denseBento) 13.sp else 13.5.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = androidx.compose.material3.LocalContentColor.current,
                )
                if (finalSubtitle.isNotEmpty()) {
                    Text(
                        text = finalSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = androidx.compose.material3.LocalContentColor.current.copy(alpha = 0.78f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun QuickPicksRows(
    tracks: List<YouTubeMusicTrack>,
    currentPlayingVideoId: String?,
    isPlaying: Boolean,
    onTrackClick: (Int) -> Unit,
    onMenuClick: (YouTubeMusicTrack) -> Unit,
    onLongClick: (YouTubeMusicTrack, Int) -> Unit = { _, _ -> },
) {
    val columns = remember(tracks) { tracks.chunked(3) }
    if (columns.isEmpty()) return

    ObserveLosslessProbeTargets(
        remember(tracks) {
            tracks.map { track ->
                LosslessProbeTarget(
                    title = track.title,
                    artist = track.artist,
                    durationSeconds = track.durationSeconds?.takeIf { it > 0 },
                    album = track.album,
                )
            }
        },
    )

    // Paged columns of three on every theme (DESIGN.md 3a).
    val pagerState = rememberPagerState(pageCount = { columns.size })
    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { pageIdx ->
            val column = columns[pageIdx]
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                column.forEachIndexed { rowIndex, track ->
                    val overallIndex = pageIdx * 3 + rowIndex
                    QuickPickTrackRow(
                        track = track,
                        isCurrent = track.videoId.isNotBlank() && track.videoId == currentPlayingVideoId,
                        isPlaying = isPlaying,
                        horizontalPadding = 14.dp,
                        verticalPadding = 6.dp,
                        onClick = { onTrackClick(overallIndex) },
                        onLongClick = { onLongClick(track, overallIndex) },
                        onMenuClick = { onMenuClick(track) },
                    )
                }
            }
        }
        if (columns.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(columns.size.coerceAtMost(6)) { dotIndex ->
                    val active = pagerState.currentPage == dotIndex
                    Box(
                        modifier = Modifier
                            .size(width = if (active) 16.dp else 6.dp, height = 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f),
                            ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickPickTrackRow(
    track: YouTubeMusicTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    horizontalPadding: Dp,
    verticalPadding: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val dropController = LocalMiniPlayerDropController.current
    val thumbOffsetPx = with(LocalDensity.current) { (horizontalPadding + 24.dp).toPx() }
    val rowBounds = remember { DropRowBoundsHolder() }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
        border = null,
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { if (it.isAttached) rowBounds.rect = it.boundsInRoot() }
            .combinedClickable(
                onClick = {
                    dropController?.triggerFromBounds(
                        title = track.title,
                        artist = ArtistHelper.primaryArtist(track.artist),
                        artworkUrl = track.artworkUrl,
                        rowBoundsInRoot = rowBounds.rect,
                        kind = LiquidDropKind.PLAY,
                        thumbOffsetPx = thumbOffsetPx,
                    )
                    onClick()
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(modifier = Modifier.size(48.dp)) {
                ArtworkImage(
                    name = track.title,
                    artist = ArtistHelper.primaryArtist(track.artist),
                    embeddedUrl = track.artworkUrl,
                    fallbackIcon = Icons.Filled.MusicNote,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                )
                if (isCurrent && isPlaying) {
                    PlayingWaveBars(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(2.dp)
                            .size(24.dp, 18.dp),
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 2.dp),
                )
                TrackSubtitleWithBadge(
                    subtitle = ArtistHelper.primaryArtist(track.artist),
                    title = track.title,
                    artist = track.artist,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    compactBadge = true,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.feed_more_cd, track.title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
