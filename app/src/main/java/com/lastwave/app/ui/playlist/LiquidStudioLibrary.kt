package com.lastwave.app.ui.playlist

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.lastwave.app.data.playlist.SavedPlaylist
import com.lastwave.app.data.playlist.SpotifyUserPlaylist
import com.lastwave.app.data.playlist.isYouTubeOnly
import com.lastwave.app.ui.comic.Surface
import com.lastwave.app.ui.comic.clipOnly
import com.lastwave.app.ui.common.PlaylistCover
import com.lastwave.app.ui.theme.ComicAccent
import com.lastwave.app.ui.theme.ComicInk
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.comicIndicationOrNull
import com.lastwave.app.ui.theme.comicPanel
import com.lastwave.app.ui.theme.glassCardBorder
import com.lastwave.app.ui.theme.glassCardColor
import kotlinx.coroutines.launch
import kotlin.math.abs

private sealed interface LiquidShowcaseEntry {
    val stableKey: String

    data class Room(val playlist: SavedPlaylist) : LiquidShowcaseEntry {
        override val stableKey: String = "room_${playlist.id}"
    }

    data class Spotify(val playlist: SpotifyUserPlaylist) : LiquidShowcaseEntry {
        override val stableKey: String = "sp_${playlist.id}"
    }
}

private val ShowcaseCardShape = RoundedCornerShape(24.dp)

/**
 * Animation #2: Library 3D Cover Flow Carousel + 3D 180-deg Y-axis Card Flip Quick-Preview.
 * Sized proportionally so Header + Segments + Cover Flow + 2x2 Quick Vault fill 1 phone viewport cleanly.
 */
@Composable
internal fun LiquidStudioLibraryCoverFlow(
    roomPlaylists: List<SavedPlaylist>,
    spotifyPlaylists: List<SpotifyUserPlaylist>,
    onOpenRoom: (Long) -> Unit,
    onPlayRoomTrack: (SavedPlaylist, Int) -> Unit,
    onOpenSpotify: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = remember(spotifyPlaylists, roomPlaylists) {
        (spotifyPlaylists.map(LiquidShowcaseEntry::Spotify) + roomPlaylists.map(LiquidShowcaseEntry::Room))
            .take(12)
    }
    if (entries.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { entries.size })
    var flippedKey by remember { mutableStateOf<String?>(null) }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        flippedKey = null
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardSide = minOf(maxWidth * 0.60f, 212.dp).coerceAtLeast(156.dp)
            val horizPad = ((maxWidth - cardSide) / 2f).coerceAtLeast(24.dp)

            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = horizPad),
                pageSpacing = 10.dp,
                key = { idx -> entries[idx].stableKey },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardSide),
            ) { page ->
                val item = entries[page]
                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val absOffset = abs(pageOffset).coerceIn(0f, 1.5f)
                val closeness = (1f - absOffset).coerceIn(0f, 1f)
                val isCentered = absOffset < 0.25f
                val isFlipped = isCentered && flippedKey == item.stableKey

                val flipAngle by animateFloatAsState(
                    targetValue = if (isFlipped) 180f else 0f,
                    animationSpec = spring(dampingRatio = 0.76f, stiffness = 340f),
                    label = "libraryCardFlipY",
                )

                val coverFlowYaw = (-pageOffset * 32f).coerceIn(-40f, 40f)
                val combinedYaw = coverFlowYaw + flipAngle
                val showBackFace = flipAngle > 90f

                Box(
                    modifier = Modifier
                        .size(cardSide)
                        .zIndex(10f - absOffset)
                        .graphicsLayer {
                            cameraDistance = 15f * density
                            rotationY = combinedYaw
                            val sc = lerp(0.84f, 1f, closeness)
                            scaleX = sc
                            scaleY = sc
                            alpha = lerp(0.68f, 1f, closeness)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (!showBackFace) {
                        ShowcaseFrontCard(
                            entry = item,
                            isCentered = isCentered,
                            onClick = {
                                if (isCentered) {
                                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    flippedKey = item.stableKey
                                } else {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    scope.launch { pagerState.animateScrollToPage(page) }
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationY = 180f },
                        ) {
                            ShowcaseBackCard(
                                entry = item,
                                onFlipBack = {
                                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    flippedKey = null
                                },
                                onPlayAll = {
                                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    when (item) {
                                        is LiquidShowcaseEntry.Room -> onPlayRoomTrack(item.playlist, 0)
                                        is LiquidShowcaseEntry.Spotify -> {
                                            val routeId = if (item.playlist.id == "liked") "sp_liked" else "sp_${item.playlist.id}"
                                            onOpenSpotify(routeId)
                                        }
                                    }
                                },
                                onPlayRoomTrack = { idx ->
                                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    if (item is LiquidShowcaseEntry.Room) {
                                        onPlayRoomTrack(item.playlist, idx)
                                    }
                                },
                                onOpen = {
                                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    when (item) {
                                        is LiquidShowcaseEntry.Room -> onOpenRoom(item.playlist.id)
                                        is LiquidShowcaseEntry.Spotify -> {
                                            val routeId = if (item.playlist.id == "liked") "sp_liked" else "sp_${item.playlist.id}"
                                            onOpenSpotify(routeId)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }

        // Sleek carousel page dots
        if (entries.size > 1) {
            val dotCount = entries.size.coerceAtMost(7)
            val activeDot = if (entries.size <= 7) {
                pagerState.currentPage
            } else {
                ((pagerState.currentPage.toFloat() / entries.lastIndex) * (dotCount - 1)).toInt().coerceIn(0, dotCount - 1)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(10.dp),
            ) {
                repeat(dotCount) { idx ->
                    val selected = idx == activeDot
                    val w by animateDpAsState(if (selected) 16.dp else 5.dp, spring(stiffness = 500f), label = "libDotW")
                    val c by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        tween(180),
                        label = "libDotC",
                    )
                    Box(
                        modifier = Modifier
                            .height(5.dp)
                            .width(w)
                            .clip(CircleShape)
                            .background(c),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowcaseFrontCard(
    entry: LiquidShowcaseEntry,
    isCentered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val comic = LocalComicTheme.current
    val title = when (entry) {
        is LiquidShowcaseEntry.Room -> entry.playlist.title
        is LiquidShowcaseEntry.Spotify -> entry.playlist.name
    }
    val countLabel = when (entry) {
        is LiquidShowcaseEntry.Room -> {
            val c = entry.playlist.remoteTrackCount ?: entry.playlist.tracks.size
            if (entry.playlist.isYouTubeOnly) "YT · $c" else "$c tracks"
        }
        is LiquidShowcaseEntry.Spotify -> {
            val c = entry.playlist.trackCount
            if (c != null) "Spotify · $c" else "Spotify"
        }
    }

    Box(
        modifier = modifier
            .comicPanel(ShowcaseCardShape, offset = 3.dp)
            .clipOnly(ShowcaseCardShape)
            .border(
                if (comic) BorderStroke(0.dp, Color.Transparent)
                else glassCardBorder() ?: BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                ShowcaseCardShape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = comicIndicationOrNull(),
                onClick = onClick,
            ),
    ) {
        when (entry) {
            is LiquidShowcaseEntry.Room -> {
                PlaylistCover(
                    playlist = entry.playlist,
                    cornerRadius = 24.dp,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            is LiquidShowcaseEntry.Spotify -> {
                val pl = entry.playlist
                if (pl.id == "liked") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(Color(0xFF1DB954), Color(0xFF191414)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                    }
                } else if (!pl.artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = pl.artworkUrl,
                        contentDescription = pl.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
            }
        }

        // Bottom frosted title scrim pill
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = countLabel,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.80f),
                        maxLines = 1,
                    )
                }
                if (isCentered) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flip,
                            contentDescription = "Flip for quick preview",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShowcaseBackCard(
    entry: LiquidShowcaseEntry,
    onFlipBack: () -> Unit,
    onPlayAll: () -> Unit,
    onPlayRoomTrack: (Int) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val comic = LocalComicTheme.current
    val isDark = LocalIsDarkTheme.current
    val cardColor = when {
        comic -> MaterialTheme.colorScheme.surface
        isDark -> glassCardColor(MaterialTheme.colorScheme.surfaceContainerHigh)
        else -> glassCardColor(MaterialTheme.colorScheme.surface)
    }
    val border = if (comic) null else glassCardBorder() ?: BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.44f else 0.55f),
    )

    val title = when (entry) {
        is LiquidShowcaseEntry.Room -> entry.playlist.title
        is LiquidShowcaseEntry.Spotify -> entry.playlist.name
    }

    Surface(
        shape = ShowcaseCardShape,
        color = cardColor,
        border = border,
        modifier = modifier.comicPanel(ShowcaseCardShape, offset = 3.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .clickable(onClick = onFlipBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Flip back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                when (entry) {
                    is LiquidShowcaseEntry.Room -> {
                        val preview = entry.playlist.tracks.take(4)
                        if (preview.isEmpty()) {
                            Text(
                                text = "Open playlist to load tracks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                        } else {
                            preview.forEachIndexed { idx, track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onPlayRoomTrack(idx) }
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Text(
                                        text = "${idx + 1}.",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = track.name,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                    is LiquidShowcaseEntry.Spotify -> {
                        Text(
                            text = entry.playlist.description?.takeIf(String::isNotBlank) ?: "Synced playlist • Tap Open to browse tracks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(CircleShape)
                        .background(if (comic) ComicAccent else MaterialTheme.colorScheme.primary)
                        .clickable(onClick = onPlayAll),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = if (comic) ComicInk else MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = "Play",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (comic) ComicInk else MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f))
                        .clickable(onClick = onOpen),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "Open",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}
