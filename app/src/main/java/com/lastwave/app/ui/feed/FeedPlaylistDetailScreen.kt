package com.lastwave.app.ui.feed

import com.lastwave.app.ui.theme.pageBackground
import androidx.compose.ui.platform.LocalContext
import com.lastwave.app.playback.toPlayableTrack
import com.lastwave.app.ui.playlist.PlaylistSortMemory
import com.lastwave.app.ui.playlist.PlaylistTrackSort
import com.lastwave.app.ui.common.sortedForDisplay
import com.lastwave.app.ui.common.TrackSortMenu
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.lastwave.app.data.lossless.LosslessProbeTarget
import com.lastwave.app.ui.common.ObserveVisibleLosslessProbes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import com.lastwave.app.ui.comic.Button
import com.lastwave.app.ui.comic.FilledTonalButton
import androidx.compose.material3.Icon
import com.lastwave.app.ui.comic.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lastwave.app.ui.comic.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.lastwave.app.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.data.music.YouTubeMusicTrack
import com.lastwave.app.data.music.YouTubePlaylistResult
import com.lastwave.app.ui.common.ArtworkImage
import com.lastwave.app.ui.common.ExpressiveGroupTrackRow
import com.lastwave.app.ui.common.ExpressiveHeader
import com.lastwave.app.ui.common.GlassHeaderLayout
import com.lastwave.app.ui.common.ExpressiveLoadingIndicator
import com.lastwave.app.ui.common.GroupGap
import com.lastwave.app.ui.common.groupPositionFor
import com.lastwave.app.ui.common.safeDrawingBottomPadding
import com.lastwave.app.ui.common.adaptiveContentWidth
import com.lastwave.app.playback.PlayableTrack
import com.lastwave.app.ui.common.TrackContextMenuSheet
import com.lastwave.app.ui.common.TrackMenuCapabilities
import com.lastwave.app.ui.common.TrackMenuTarget
import com.lastwave.app.ui.common.TrackMiniTrayData
import com.lastwave.app.ui.common.TrackMiniTraySheet
import com.lastwave.app.ui.player.LocalMiniPlayerScrollClearance

@Composable
fun FeedPlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    viewModel: FeedPlaylistDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(playlistId) {
        viewModel.load(playlistId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pageBackground(),
        contentAlignment = Alignment.TopCenter,
    ) {
        val playlist = (state as? FeedPlaylistDetailUiState.Success)?.playlist
        GlassHeaderLayout(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentWidth(maxWidth = 860.dp),
            header = { headerModifier, headerBackdrop ->
        ExpressiveHeader(
            modifier = headerModifier,
            backdrop = headerBackdrop,
            title = playlist?.title?.takeIf(String::isNotBlank) ?: "Playlist",
            subtitle = playlist?.author ?: "YouTube Music",
            onBack = onBack,
        )
            },
        ) { headerInset ->
        Column(Modifier.fillMaxSize()) {
        when (val current = state) {
            FeedPlaylistDetailUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ExpressiveLoadingIndicator(message = stringResource(R.string.fpd_opening))
                }
            }

            is FeedPlaylistDetailUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = current.message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(18.dp))
                    FilledTonalButton(onClick = { viewModel.load(playlistId) }) {
                        Text(stringResource(R.string.fpd_retry))
                    }
                }
            }

            is FeedPlaylistDetailUiState.Success -> {
                // Status rows sit above the list, so they need the inset themselves.
                if (current.isLoadingMore || current.loadError != null) Spacer(Modifier.height(headerInset))
                if (current.isLoadingMore) {
                    Text(stringResource(R.string.fpd_loading_more), modifier = Modifier.padding(horizontal = 16.dp))
                }
                if (current.loadError != null) {
                    FilledTonalButton(onClick = { viewModel.load(playlistId, force = true) }) {
                        Text(stringResource(if (current.playlist.isComplete) R.string.fpd_retry else R.string.fpd_incomplete))
                    }
                }
                PlaylistContent(
                    playlist = current.playlist,
                    onPlay = viewModel::playFrom,
                    onShuffle = viewModel::shuffle,
                    onSave = viewModel::saveToLibrary,
                    isSaving = current.isSaving,
                    savedToLibrary = current.savedToLibrary,
                    saveError = current.saveError,
                    topInset = if (current.isLoadingMore || current.loadError != null) 0.dp else headerInset,
                )
            }
        }
        }
        }
    }
}

@Composable
private fun PlaylistContent(
    playlist: YouTubePlaylistResult,
    onPlay: (List<YouTubeMusicTrack>, Int) -> Unit,
    onShuffle: (List<YouTubeMusicTrack>) -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean,
    savedToLibrary: Boolean,
    saveError: String?,
    topInset: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val context = LocalContext.current
    val sortKey = "feed_${playlist.id}"
    val newestFirst = playlist.id == "yt_liked" ||
        playlist.id == "yt_recent" ||
        playlist.id == "sp_liked" ||
        playlist.id == "spotify:liked"
    val isLikedFeedPlaylist = playlist.id == "yt_liked" ||
        playlist.id == "sp_liked" ||
        playlist.id == "spotify:liked"
    val defaultSort = if (isLikedFeedPlaylist) PlaylistTrackSort.LATEST_LIKED to false else PlaylistTrackSort.CUSTOM to true
    val savedSort = remember(sortKey, defaultSort) { PlaylistSortMemory.load(context, sortKey, defaultSort = defaultSort) }
    var menuTrack by remember { mutableStateOf<YouTubeMusicTrack?>(null) }
    var trackSort by remember(sortKey, defaultSort) { mutableStateOf(savedSort.first) }
    var trackSortAscending by remember(sortKey, defaultSort) { mutableStateOf(savedSort.second) }
    val shown = remember(playlist.tracks, trackSort, trackSortAscending, newestFirst) {
        playlist.tracks.sortedForDisplay(
            sort = trackSort,
            ascending = trackSortAscending,
            title = { it.title },
            artist = { it.artist },
            durationMs = { t -> t.durationSeconds?.let { it * 1000L } },
            newestFirst = newestFirst,
            addedAtMillis = { it.addedAtMillis },
        )
    }
    // Occurrence-disambiguated keys aligned to display order so animateItem() animates sort reorders cleanly.
    val shownKeys = remember(shown) {
        val counts = mutableMapOf<String, Int>()
        shown.map { track ->
            val base = track.videoId.ifBlank { "${track.title}|${track.artist}" }
            val n = counts.getOrDefault(base, 0)
            counts[base] = n + 1
            "$base#$n"
        }
    }
    // Long-press (deep press) opens the mini tray; 3-dot keeps the full sheet.
    var miniTrayIndex by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()
    val probeKeyToTarget = remember(shown, shownKeys) {
        buildMap {
            shown.forEachIndexed { index, track ->
                val key = shownKeys.getOrNull(index) ?: "${track.videoId}:$index"
                put(
                    key,
                    LosslessProbeTarget(
                        title = track.title,
                        artist = track.artist,
                        album = track.album?.takeIf { it.isNotBlank() },
                        durationSeconds = track.durationSeconds?.takeIf { it > 0 },
                    ),
                )
            }
        }
    }
    ObserveVisibleLosslessProbes(listState = listState, keyToTarget = probeKeyToTarget)
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 20.dp + topInset,
            bottom = 24.dp + LocalMiniPlayerScrollClearance.current + safeDrawingBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(GroupGap),
    ) {
        item(key = "hero") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ArtworkImage(
                    name = playlist.title,
                    artist = playlist.author.orEmpty(),
                    embeddedUrl = playlist.artworkUrl ?: playlist.tracks.firstOrNull()?.artworkUrl,
                    fallbackIcon = if (playlist.id == "yt_liked") Icons.Filled.Favorite else Icons.Filled.MusicNote,
                    preferEmbedded = true,
                    modifier = Modifier
                        .size(196.dp)
                        .clip(RoundedCornerShape(26.dp)),
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    text = playlist.title.ifBlank { "Playlist" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        playlist.author?.takeIf(String::isNotBlank),
                        "${playlist.tracks.size} tracks",
                    ).joinToString(" \u2022 "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = { onPlay(shown, 0) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.home_play))
                    }
                    FilledTonalButton(
                        onClick = { onShuffle(shown) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Shuffle, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.feed_shuffle))
                    }
                }
                Spacer(Modifier.height(18.dp))
                FilledTonalButton(
                    onClick = onSave,
                    enabled = !isSaving && !savedToLibrary,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(when {
                        isSaving -> "Saving…"
                        savedToLibrary -> "Saved to library"
                        else -> "Save to library"
                    })
                }
                saveError?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TRACKS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    if (playlist.tracks.size > 1) {
                        TrackSortMenu(
                            sort = trackSort,
                            ascending = trackSortAscending,
                            options = PlaylistTrackSort.entries,
                            onChange = { sort, ascending ->
                                trackSort = sort
                                trackSortAscending = ascending
                                PlaylistSortMemory.save(context, sortKey, sort, ascending)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        itemsIndexed(
            items = shown,
            key = { index, track -> shownKeys.getOrNull(index) ?: "${track.videoId}:$index" },
            contentType = { _, _ -> "feed_track" },
        ) { index, track ->
            ExpressiveGroupTrackRow(
                title = track.title,
                subtitle = track.artistAndAlbum(),
                position = groupPositionFor(index, shown.size),
                onClick = { onPlay(shown, index) },
                onLongClick = { miniTrayIndex = index },
                badgeTitle = track.title,
                badgeArtist = track.artist,
                queueTrack = { track.toPlayableTrack() },
                modifier = Modifier
                    .animateItem()
                    .padding(horizontal = 16.dp),
                leading = {
                    ArtworkImage(
                        name = track.title,
                        artist = track.artist,
                        embeddedUrl = track.artworkUrl,
                        fallbackIcon = Icons.Filled.MusicNote,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                },
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        track.durationSeconds?.let { duration ->
                            Text(
                                text = duration.toDurationLabel(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(
                            onClick = { menuTrack = track },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = stringResource(R.string.fpd_track_options_cd),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
            )
        }
    }

    menuTrack?.let { track ->
        TrackContextMenuSheet(
            target = TrackMenuTarget.Track(track.title, track.artist, ""),
            capabilities = TrackMenuCapabilities(showCopyActions = true, showDeleteScrobble = false),
            playableTrack = PlayableTrack(
                title = track.title,
                artist = track.artist,
                album = track.album,
                artworkUrl = track.artworkUrl,
                videoId = track.videoId.takeIf(String::isNotBlank),
            ),
            playbackSourceLabel = playlist.title.ifBlank { "Mix" },
            onDismiss = { menuTrack = null },
        )
    }

    miniTrayIndex?.let { trayIndex ->
        shown.getOrNull(trayIndex)?.let { track ->
            TrackMiniTraySheet(
                data = TrackMiniTrayData(
                    title = track.title,
                    artist = track.artist,
                    album = track.album,
                    artworkUrl = track.artworkUrl,
                    videoId = track.videoId.takeIf(String::isNotBlank),
                    sourceLabel = playlist.title.ifBlank { "Mix" },
                    onPlay = { onPlay(shown, trayIndex) },
                ),
                onDismiss = { miniTrayIndex = null },
            )
        }
    }
}

private fun YouTubeMusicTrack.artistAndAlbum(): String =
    listOfNotNull(artist.takeIf(String::isNotBlank), album?.takeIf(String::isNotBlank))
        .joinToString(" \u2022 ")
        .ifBlank { "YouTube Music" }

private fun Int.toDurationLabel(): String = "%d:%02d".format(this / 60, this % 60)
