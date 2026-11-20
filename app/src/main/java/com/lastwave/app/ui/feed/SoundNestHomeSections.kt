package com.lastwave.app.ui.feed

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.lastwave.app.data.music.ChartTimeWindow
import com.lastwave.app.data.music.HomeChip
import com.lastwave.app.data.music.chartRegionName
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.cast.framework.CastButtonFactory
import com.lastwave.app.R
import com.lastwave.app.data.music.YouTubeMusicTrack
import com.lastwave.app.ui.common.TrackSubtitleWithBadge
import com.lastwave.app.playback.MusicPlayer
import com.lastwave.app.playback.PlaybackProgressState
import kotlinx.coroutines.flow.StateFlow
import com.lastwave.app.ui.comic.AlertDialog
import com.lastwave.app.ui.comic.Button
import com.lastwave.app.ui.comic.IconButton
import com.lastwave.app.ui.comic.Surface
import com.lastwave.app.ui.comic.TextButton
import com.lastwave.app.ui.comic.clip
import com.lastwave.app.ui.common.ArtworkImage
import com.lastwave.app.ui.common.expressivePressScale
import com.lastwave.app.ui.theme.sectionTitleStyle
import com.lastwave.app.ui.common.WindowSizeClass
import com.lastwave.app.ui.common.rememberWindowSizeClass
import com.lastwave.app.ui.home.ProfileAvatar
import com.lastwave.app.ui.theme.ComicInk
import com.lastwave.app.ui.theme.ComicAccent
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.comicHalftone
import com.lastwave.app.ui.theme.comicIndicationOrNull
import com.lastwave.app.ui.theme.comicPanel
import com.lastwave.app.ui.theme.comicPress
import java.time.LocalTime

@Immutable
internal data class HomeQuickAccessEntry(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val highlighted: Boolean = false,
    val onClick: () -> Unit,
)

@Immutable
internal data class HomeFeaturedItem(
    val id: String,
    val category: String,
    val title: String,
    val description: String,
    val artworkUrl: String?,
    val onPlay: () -> Unit,
    val onOpen: () -> Unit,
)

@Immutable
internal data class HomeContinueItem(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val progress: Float = 0f,
    val progressState: StateFlow<PlaybackProgressState>? = null,
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null,
)

@Immutable
internal data class HomePlaylistItem(
    val id: String,
    val title: String,
    val artworkUrl: String?,
    val accentProgress: Float = 0.65f,
    val showIndicatorIcon: Boolean = false,
    val caption: String? = null,
    /** The thin accent bar under the title; off for plain albums/playlists. */
    val showProgress: Boolean = true,
    val onClick: () -> Unit,
)

@Immutable
internal data class HomeRecommendedItem(
    val id: String,
    val track: YouTubeMusicTrack,
    val isCurrent: Boolean = false,
    val isPlaying: Boolean = false,
    val onClick: () -> Unit,
    val onMenuClick: () -> Unit,
    val onLongClick: () -> Unit,
)

/** Section A: App identity on the left, optional inline Search in wide landscape, Downloads/Cast/Notifications/Profile on the right. */
@Composable
internal fun HomeHeader(
    avatarUrl: String?,
    hasNotificationBadge: Boolean,
    musicPlayer: MusicPlayer,
    onOpenDownloads: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: (() -> Unit)? = null,
    showCenterSearch: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val comic = LocalComicTheme.current
    val context = LocalContext.current
    val castAvailable = remember(musicPlayer) { musicPlayer.initializeCast() }
    var mediaRouteButton by remember { mutableStateOf<MediaRouteButton?>(null) }

    // Hidden MediaRouteButton lets a theme-matched Compose button trigger the native Cast picker.
    if (castAvailable) {
        AndroidView(
            modifier = Modifier.size(0.dp),
            factory = { ctx ->
                MediaRouteButton(ctx).apply {
                    CastButtonFactory.setUpMediaRouteButton(ctx, this)
                    mediaRouteButton = this
                }
            },
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (showCenterSearch && onOpenSearch != null) {
            AudioWavesBrandMark(subtitle = "Music Streaming")
            HomeSearchBarPill(
                onClick = onOpenSearch,
                compact = true,
                horizontalPadding = 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
            )
        } else {
            AudioWavesBrandMark(
                subtitle = "Music Streaming",
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HeaderRoundIconButton(
                icon = Icons.Filled.Download,
                contentDescription = "Downloads",
                onClick = onOpenDownloads,
            )

            HeaderRoundIconButton(
                icon = Icons.Filled.Cast,
                contentDescription = stringResourceOrFallback(R.string.cast_title, "Cast"),
                onClick = {
                    if (castAvailable) {
                        mediaRouteButton?.performClick()
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.cast_unavailable),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
            )

            Box {
                HeaderRoundIconButton(
                    icon = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    onClick = onOpenNotifications,
                )
                if (hasNotificationBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-5).dp, y = 5.dp)
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (comic) ComicAccent else MaterialTheme.colorScheme.primary)
                            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                    )
                }
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(40.dp),
            ) {
                ProfileAvatar(
                    avatarUrl = avatarUrl,
                    modifier = Modifier
                        .size(38.dp)
                        .comicPanel(CircleShape, offset = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun HeaderRoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .size(38.dp)
            .comicPanel(CircleShape, offset = 2.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Section B: Time-aware greeting headline and concise discovery subtitle. */
@Composable
internal fun GreetingSection(
    horizontalPadding: Dp = 16.dp,
    modifier: Modifier = Modifier,
) {
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning \uD83D\uDC4B"
            in 12..16 -> "Good afternoon \uD83D\uDC4B"
            in 17..21 -> "Good evening \uD83D\uDC4B"
            else -> "Good night \uD83D\uDC4B"
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 26.sp,
                letterSpacing = (-0.5).sp,
            ),
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Discover, stream and enjoy millions of songs",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Quick navigation pill to open full search directly from Home. */
@Composable
internal fun HomeSearchBarPill(
    onClick: () -> Unit,
    compact: Boolean = false,
    horizontalPadding: Dp = 16.dp,
    modifier: Modifier = Modifier,
) {
    val comic = LocalComicTheme.current
    val haptics = LocalHapticFeedback.current
    val pillShape = CircleShape
    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = pillShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (!comic) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
        } else null,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .then(if (comic) Modifier.padding(end = 3.dp, bottom = 3.dp) else Modifier)
            .height(if (compact) 40.dp else 46.dp)
            .comicPanel(pillShape, offset = if (compact) 2.dp else 3.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 14.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = if (comic) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(if (compact) 18.dp else 20.dp),
            )
            Text(
                text = "Search songs, artists, albums...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Section C: Four equal-width Quick Access action tiles (Discover, Podcasts, Radio, Favourites). */
@Composable
internal fun QuickAccessRow(
    items: List<HomeQuickAccessEntry>,
    horizontalPadding: Dp = 16.dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(end = 3.dp, bottom = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { entry ->
            QuickAccessItem(
                entry = entry,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Compact horizontal mood filter pills below Quick Access. */
@Composable
internal fun HomeMoodChipsRow(
    moodLabels: List<String>,
    selectedMood: String?,
    onSelectAll: () -> Unit,
    onSelectMood: (String) -> Unit,
    horizontalPadding: Dp = 16.dp,
    modifier: Modifier = Modifier,
) {
    PinnedChipsBar(
        chips = remember(moodLabels) { moodLabels.map { HomeChip(it) } },
        selectedChip = selectedMood?.let { HomeChip(it) },
        onChipClick = { chip -> if (chip == null) onSelectAll() else onSelectMood(chip.title) },
        horizontalPadding = horizontalPadding,
        modifier = modifier,
    )
}

@Composable
internal fun ChartQuickChipsRow(
    selectedWindow: ChartTimeWindow,
    selectedCountries: List<String>,
    onSelectWindow: (ChartTimeWindow) -> Unit,
    onSelectCountry: (String) -> Unit,
    horizontalPadding: Dp = 16.dp,
    modifier: Modifier = Modifier,
) {
    val countryChoices = remember(selectedCountries) {
        (selectedCountries + listOf("MY", "ID", "KR", "ZZ")).distinct()
    }
    val primaryCountry = selectedCountries.firstOrNull()
    // Justified, flush to the page grid on every theme (DESIGN.md 3a).
    val visibleCountries = remember(countryChoices, primaryCountry) {
        val topTwo = countryChoices.take(2).toMutableList()
        if (primaryCountry != null && primaryCountry !in topTwo && primaryCountry in countryChoices) {
            topTwo[topTwo.lastIndex] = primaryCountry
        }
        topTwo
    }
    val availWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp - horizontalPadding * 2 - 3.dp
    val chipPad = if (availWidth < 430.dp) 7.dp else 10.dp
    val chipGap = if (availWidth < 430.dp) 4.dp else 6.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .padding(end = 3.dp, bottom = 2.dp),
    ) {
        JustifiedChipFlowLayout(
            horizontalGap = chipGap,
            verticalGap = 8.dp,
            balanceRows = true,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ChartTimeWindow.entries.forEach { window ->
                MoodChip(
                    label = window.shortLabel,
                    selected = window == selectedWindow,
                    onClick = { onSelectWindow(window) },
                    horizontalPadding = chipPad,
                )
            }
            visibleCountries.forEach { code ->
                MoodChip(
                    label = if (code == "ZZ") "Global" else chartRegionName(code).ifBlank { code },
                    selected = code == primaryCountry,
                    onClick = { onSelectCountry(code) },
                    horizontalPadding = chipPad,
                )
            }
        }
    }
}

@Composable
internal fun QuickAccessItem(
    entry: HomeQuickAccessEntry,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val comic = LocalComicTheme.current
    val shape = RoundedCornerShape(16.dp)
    val containerColor = when {
        entry.highlighted && comic -> ComicAccent
        entry.highlighted -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        entry.highlighted && comic -> ComicInk
        entry.highlighted -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val cardInteraction = remember { MutableInteractionSource() }
    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            entry.onClick()
        },
        shape = shape,
        color = containerColor,
        border = if (!comic && !entry.highlighted) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        } else null,
        interactionSource = cardInteraction,
        modifier = modifier
            .then(if (!comic) Modifier.expressivePressScale(cardInteraction) else Modifier)
            .heightIn(min = 72.dp)
            .comicPanel(shape, offset = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = entry.label,
                tint = contentColor,
                modifier = Modifier.size(21.dp),
            )
            Text(
                text = entry.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (entry.highlighted) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Section D: HorizontalPager carousel of featured playlists with inline bottom page indicators. */
@Composable
internal fun FeaturedPlaylistCarousel(
    slides: List<HomeFeaturedItem>,
    horizontalPadding: Dp = 16.dp,
    modifier: Modifier = Modifier,
) {
    if (slides.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val comic = LocalComicTheme.current
    val cardShape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
    ) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val slide = slides[page]
            FeaturedPlaylistCard(
                item = slide,
                shape = cardShape,
                modifier = Modifier.padding(end = 4.dp, bottom = 4.dp),
            )
        }

        if (slides.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(slides.size) { index ->
                    val active = pagerState.currentPage == index
                    val dotColor by animateColorAsState(
                        targetValue = when {
                            active && comic -> ComicAccent
                            active -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        },
                        label = "featuredDotColor",
                    )
                    Box(
                        modifier = Modifier
                            .size(width = if (active) 18.dp else 6.dp, height = 6.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                }
            }
        }
    }
}

@Composable
internal fun FeaturedPlaylistCard(
    item: HomeFeaturedItem,
    shape: RoundedCornerShape = RoundedCornerShape(22.dp),
    modifier: Modifier = Modifier,
) {
    val comic = LocalComicTheme.current
    val haptics = LocalHapticFeedback.current
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val accentColor = if (comic) ComicAccent else MaterialTheme.colorScheme.primary

    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            item.onOpen()
        },
        shape = shape,
        color = containerColor,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 172.dp, max = 188.dp)
            .comicPanel(shape),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Right-aligned featured artwork softly faded into the card surface on the left.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.56f)
                    .fillMaxHeight(),
            ) {
                if (!item.artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                            modifier = Modifier.size(56.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0.0f to containerColor,
                                0.35f to containerColor.copy(alpha = 0.72f),
                                1.0f to Color.Transparent,
                            ),
                        ),
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .comicHalftone(),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .fillMaxHeight()
                    .padding(start = 16.dp, top = 16.dp, bottom = 22.dp, end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.0.sp,
                            fontSize = 10.sp,
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            letterSpacing = (-0.4).sp,
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        item.onPlay()
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = if (comic) ComicInk else MaterialTheme.colorScheme.onPrimary,
                    ),
                    border = if (comic) BorderStroke(1.5.dp, ComicInk) else null,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Play",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

/** Shared section header: title (+ optional subtitle) on the left, optional Shuffle pill and "See All ›" action on the right. */
@Composable
internal fun SectionHeader(
    title: String,
    subtitle: String? = null,
    onSeeAll: (() -> Unit)? = null,
    onShuffle: (() -> Unit)? = null,
    horizontalPadding: Dp = 16.dp,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val comic = LocalComicTheme.current
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
                .then(
                    if (onLongClick == null) Modifier
                    else Modifier.pointerInput(onLongClick) {
                        detectTapGestures(onLongPress = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick()
                        })
                    },
                ),
        ) {
            Text(
                text = title,
                style = sectionTitleStyle(),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (onShuffle != null || onSeeAll != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onShuffle != null) {
                    val pillColor = if (comic) ComicAccent else MaterialTheme.colorScheme.primaryContainer
                    val pillContent = if (comic) ComicInk else MaterialTheme.colorScheme.onPrimaryContainer
                    Surface(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onShuffle()
                        },
                        shape = CircleShape,
                        color = pillColor,
                        modifier = Modifier
                            .height(28.dp)
                            .comicPanel(CircleShape, offset = 2.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shuffle,
                                contentDescription = "Shuffle $title",
                                tint = pillContent,
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = "Shuffle",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = pillContent,
                                maxLines = 1,
                            )
                        }
                    }
                }
                if (onSeeAll != null) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSeeAll()
                            }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "See All",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                }
            }
        }
    }
}

private const val CONTINUE_GRID_SIZE = 6

/** Section E: Horizontal list of compact song cards with mini play button and progress bar. */
@Composable
internal fun ContinueListeningRow(
    items: List<HomeContinueItem>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    // Six songs in a fixed grid on every theme: a row of wide cards showed two
    // and hid the rest behind a swipe. Avoid BoxWithConstraints subcomposition during scroll.
    val columns = if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp >= 632.dp) 3 else 2
    Box(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.take(CONTINUE_GRID_SIZE).chunked(columns).forEach { row ->
                // End padding leaves the last card's 2.dp shadow inside the margin.
                Row(Modifier.padding(end = 2.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { CompactSongCard(item = it, modifier = Modifier.weight(1f)) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CompactSongCard(
    item: HomeContinueItem,
    modifier: Modifier = Modifier,
) {
    val comic = LocalComicTheme.current
    val haptics = LocalHapticFeedback.current
    val cardShape = RoundedCornerShape(14.dp)
    val artShape = RoundedCornerShape(10.dp)
    val accentColor = if (comic) ComicAccent else MaterialTheme.colorScheme.primary
    val cardInteraction = remember { MutableInteractionSource() }

    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (!comic) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)) else null,
        modifier = modifier
            .width(224.dp)
            .then(if (comic) Modifier.comicPress(cardInteraction) else Modifier.expressivePressScale(cardInteraction))
            .comicPanel(cardShape, offset = 2.dp)
            .combinedClickable(
                interactionSource = cardInteraction,
                indication = comicIndicationOrNull(),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    item.onClick()
                },
                onLongClick = item.onLongClick?.let {
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(artShape),
            ) {
                ArtworkImage(
                    name = item.title,
                    artist = item.artist,
                    embeddedUrl = item.artworkUrl,
                    fallbackIcon = Icons.Filled.MusicNote,
                    modifier = Modifier.fillMaxSize(),
                )
                Surface(
                    shape = CircleShape,
                    color = if (comic) ComicAccent else Color.Black.copy(alpha = 0.68f),
                    border = if (comic) BorderStroke(1.2.dp, ComicInk) else null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .size(20.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play ${item.title}",
                            tint = if (comic) ComicInk else Color.White,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                ContinueProgressBar(
                    fallbackProgress = item.progress,
                    progressState = item.progressState,
                    accentColor = accentColor,
                )
            }
        }
    }
}

@Composable
private fun ContinueProgressBar(
    fallbackProgress: Float,
    progressState: StateFlow<PlaybackProgressState>?,
    accentColor: Color,
) {
    // Subscribe only in this leaf bar so playback ticks never recompose FeedScreen or sibling cards.
    val fraction = if (progressState != null) {
        val snap by progressState.collectAsStateWithLifecycle()
        if (snap.durationMs > 0L) (snap.positionMs.toFloat() / snap.durationMs.toFloat()).coerceIn(0f, 1f)
        else fallbackProgress
    } else {
        fallbackProgress
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0.08f, 1f))
                .clip(CircleShape)
                .background(accentColor),
        )
    }
}

/** Section F: Horizontal list of square personalized mix cards with overlay titles. */
@Composable
internal fun MadeForYouRow(
    items: List<HomePlaylistItem>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val sizeClass = rememberWindowSizeClass()
    val cardSize = when (sizeClass) {
        WindowSizeClass.COMPACT -> 128.dp
        WindowSizeClass.MEDIUM -> 144.dp
        WindowSizeClass.EXPANDED -> 152.dp
    }
    // Same symmetric shelf on every theme (DESIGN.md 3a).
    Box(modifier) {
        FeedMediaShelfRow(items = items, topPadding = 0.dp, gap = 12.dp, minItemWidth = cardSize) { _, item ->
            PlaylistCard(item = item, cardSize = Dp.Unspecified)
        }
    }
}

/** Two-by-two bento grid of square mix cards for wide landscape tablets. */
@Composable
internal fun MadeForYouTwoByTwoGrid(
    items: List<HomePlaylistItem>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items.take(4).chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowItems.forEach { item ->
                    PlaylistCard(
                        item = item,
                        cardSize = Dp.Unspecified,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun PlaylistCard(
    item: HomePlaylistItem,
    cardSize: Dp = 132.dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.then(if (cardSize != Dp.Unspecified) Modifier.width(cardSize) else Modifier.fillMaxWidth()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PlaylistArtCard(item)
        item.caption?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun PlaylistArtCard(item: HomePlaylistItem) {
    val comic = LocalComicTheme.current
    val haptics = LocalHapticFeedback.current
    val cardShape = RoundedCornerShape(18.dp)
    val accentColor = if (comic) ComicAccent else MaterialTheme.colorScheme.primary

    val cardInteraction = remember { MutableInteractionSource() }

    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            item.onClick()
        },
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        interactionSource = cardInteraction,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!comic) Modifier.expressivePressScale(cardInteraction) else Modifier)
            .aspectRatio(1f)
            .comicPanel(cardShape),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ArtworkImage(
                name = item.title,
                artist = "Mix",
                embeddedUrl = item.artworkUrl,
                fallbackIcon = Icons.AutoMirrored.Filled.QueueMusic,
                decodeSizePx = 360,
                modifier = Modifier.fillMaxSize(),
            )
            // Legible dark bottom fade + subtle halftone accent so artwork stays punchy and labels read crisply.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.08f),
                            0.52f to Color.Black.copy(alpha = 0.25f),
                            1.0f to Color.Black.copy(alpha = 0.84f),
                        ),
                    )
                    .comicHalftone(),
            )

            if (item.showIndicatorIcon) {
                Surface(
                    shape = CircleShape,
                    color = if (comic) ComicAccent else MaterialTheme.colorScheme.primaryContainer,
                    border = if (comic) BorderStroke(1.2.dp, ComicInk) else null,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .size(26.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = if (comic) ComicInk else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            lineHeight = 18.sp,
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        shape = CircleShape,
                        color = accentColor,
                        border = if (comic) BorderStroke(1.2.dp, ComicInk) else null,
                        modifier = Modifier.size(26.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play ${item.title}",
                                tint = if (comic) ComicInk else MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
                if (item.showProgress) Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.24f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(item.accentProgress.coerceIn(0.15f, 1f))
                            .clip(CircleShape)
                            .background(accentColor),
                    )
                }
            }
        }
    }
}

/** Section G: Vertical recommended song row with optional Lossless/Hi-Res pills and overflow menu. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RecommendedSongRow(
    item: HomeRecommendedItem,
    horizontalPadding: Dp = 16.dp,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val comic = LocalComicTheme.current
    val haptics = LocalHapticFeedback.current
    val thumbShape = RoundedCornerShape(10.dp)
    val rowShape = RoundedCornerShape(14.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .clip(rowShape)
            .then(
                if (item.isCurrent) {
                    Modifier.background(
                        if (comic) ComicAccent.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    )
                } else {
                    Modifier
                },
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = comicIndicationOrNull(),
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    item.onClick()
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    item.onLongClick()
                },
            )
            .padding(horizontal = 6.dp, vertical = if (compact) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 46.dp else 50.dp)
                .comicPanel(thumbShape, offset = 2.dp)
                .clip(thumbShape),
        ) {
            ArtworkImage(
                name = item.track.title,
                artist = item.track.artist,
                embeddedUrl = item.track.artworkUrl,
                fallbackIcon = Icons.Filled.MusicNote,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.track.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (item.isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (item.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TrackSubtitleWithBadge(
                subtitle = item.track.artist,
                title = item.track.title,
                artist = item.track.artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                compactBadge = compact,
            )
        }

        IconButton(
            onClick = item.onMenuClick,
            modifier = Modifier.size(34.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Track options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

/** Lightweight activity sheet opened from the Header Notification icon. */
@Composable
internal fun HomeNotificationsDialog(
    newReleaseCount: Int,
    onOpenNewReleases: () -> Unit,
    onOpenDownloads: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = "Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick = {
                        onDismiss()
                        onOpenNewReleases()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NewReleases,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "New Releases",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (newReleaseCount > 0) "$newReleaseCount fresh releases waiting" else "Browse latest albums & singles",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Surface(
                    onClick = {
                        onDismiss()
                        onOpenDownloads()
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Offline Library & Downloads",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Manage downloaded tracks and Lossless cache",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun stringResourceOrFallback(resId: Int, fallback: String): String {
    val ctx = LocalContext.current
    return remember(resId) {
        runCatching { ctx.getString(resId) }.getOrDefault(fallback)
    }
}
