package com.lastwave.app.ui.feed

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.R
import com.lastwave.app.data.feed.FeedArtist
import com.lastwave.app.data.lossless.LosslessProbeTarget
import com.lastwave.app.data.music.HomeShelfItem
import com.lastwave.app.data.music.YouTubeMusicTrack
import com.lastwave.app.playback.PlayableTrack
import com.lastwave.app.ui.comic.Button
import com.lastwave.app.ui.comic.FilledTonalButton
import com.lastwave.app.ui.comic.IconButton
import com.lastwave.app.ui.comic.clip
import com.lastwave.app.ui.common.ArtworkImage
import com.lastwave.app.ui.common.ExpressiveGroupTrackRow
import com.lastwave.app.ui.common.ExpressiveHeader
import com.lastwave.app.ui.common.GlassHeaderLayout
import com.lastwave.app.ui.common.GroupGap
import com.lastwave.app.ui.common.HeaderActionIcon
import com.lastwave.app.ui.common.ObserveVisibleLosslessProbes
import com.lastwave.app.ui.common.TrackContextMenuSheet
import com.lastwave.app.ui.common.TrackMenuCapabilities
import com.lastwave.app.ui.common.TrackMenuTarget
import com.lastwave.app.ui.common.adaptiveContentWidth
import com.lastwave.app.ui.common.groupPositionFor
import com.lastwave.app.ui.common.safeDrawingBottomPadding
import com.lastwave.app.ui.common.safeHorizontalContentPadding
import com.lastwave.app.ui.player.LocalMiniPlayerScrollClearance
import com.lastwave.app.ui.player.LocalMusicPlayer
import com.lastwave.app.ui.player.PlayingWaveBars
import com.lastwave.app.util.ArtistHelper
import java.util.LinkedHashMap
import java.util.concurrent.atomic.AtomicLong

/** In-memory payload passed when opening "See All" on a Feed section. */
internal sealed interface FeedSectionPayload {
    val title: String
    val subtitle: String?

    data class Cards(
        override val title: String,
        override val subtitle: String? = null,
        val items: List<HomePlaylistItem>,
        val onPlayAll: (() -> Unit)? = null,
        val onShuffle: (() -> Unit)? = null,
    ) : FeedSectionPayload

    data class Tracks(
        override val title: String,
        override val subtitle: String? = null,
        val tracks: List<YouTubeMusicTrack>,
        val onPlayTrack: (Int) -> Unit,
        val onShuffle: () -> Unit,
    ) : FeedSectionPayload

    data class Artists(
        override val title: String,
        override val subtitle: String? = null,
        val artists: List<FeedArtist>,
        val onOpenArtist: (name: String, browseId: String?) -> Unit,
    ) : FeedSectionPayload

    data class Shelf(
        override val title: String,
        override val subtitle: String? = null,
        val items: List<HomeShelfItem>,
        val onPlayTrack: (YouTubeMusicTrack, String) -> Unit,
        val onPlayTracksQueue: (List<YouTubeMusicTrack>, Int, String) -> Unit,
        val onShuffleTracksQueue: (List<YouTubeMusicTrack>, String) -> Unit,
        val onOpenFeedPlaylist: (String) -> Unit,
        val onOpenAlbum: (title: String, artist: String, browseId: String) -> Unit,
        val onOpenPodcastShow: (String) -> Unit,
        val onPlayPodcastEpisode: (HomeShelfItem.PodcastEpisode, List<HomeShelfItem.PodcastEpisode>, String) -> Unit,
    ) : FeedSectionPayload
}

/** Bounded cache so navigating Feed -> See All -> Album/Artist -> back keeps the section intact. */
internal object FeedSectionDetailStore {
    private const val MAX_ENTRIES = 8
    private val seq = AtomicLong(0)
    private val lock = Any()
    private val entries = object : LinkedHashMap<String, FeedSectionPayload>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FeedSectionPayload>?): Boolean =
            size > MAX_ENTRIES
    }

    fun put(payload: FeedSectionPayload): String {
        val id = "sec_${seq.incrementAndGet()}"
        synchronized(lock) { entries[id] = payload }
        return id
    }

    fun get(id: String): FeedSectionPayload? = synchronized(lock) { entries[id] }
}

@Composable
fun FeedSectionDetailScreen(
    sectionId: String,
    onBack: () -> Unit,
) {
    val payload = remember(sectionId) { FeedSectionDetailStore.get(sectionId) }
    if (payload == null) {
        LaunchedEffect(sectionId) { onBack() }
        return
    }

    val shuffleAction: (() -> Unit)? = when (payload) {
        is FeedSectionPayload.Cards -> payload.onShuffle
        is FeedSectionPayload.Tracks -> payload.onShuffle
        is FeedSectionPayload.Artists -> null
        is FeedSectionPayload.Shelf -> {
            val shelfTracks = remember(payload.items) {
                payload.items.filterIsInstance<HomeShelfItem.Track>().map { it.track }
            }
            if (shelfTracks.isNotEmpty()) {
                { payload.onShuffleTracksQueue(shelfTracks, payload.title) }
            } else null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        GlassHeaderLayout(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentWidth(maxWidth = 960.dp),
            header = { headerModifier, headerBackdrop ->
                ExpressiveHeader(
                    modifier = headerModifier,
                    backdrop = headerBackdrop,
                    title = payload.title,
                    subtitle = payload.subtitle,
                    onBack = onBack,
                    actions = {
                        shuffleAction?.let { shuffle ->
                            HeaderActionIcon(
                                icon = Icons.Filled.Shuffle,
                                contentDescription = stringResource(R.string.feed_shuffle),
                                onClick = shuffle,
                            )
                        }
                    },
                )
            },
        ) { headerInset ->
            val bottomPadding = 24.dp + LocalMiniPlayerScrollClearance.current + safeDrawingBottomPadding()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeHorizontalContentPadding(),
            ) {
                when (payload) {
                    is FeedSectionPayload.Tracks -> TrackSectionList(
                        title = payload.title,
                        tracks = payload.tracks,
                        onPlayTrack = payload.onPlayTrack,
                        onShuffle = payload.onShuffle,
                        topPadding = 10.dp + headerInset,
                        bottomPadding = bottomPadding,
                    )

                    is FeedSectionPayload.Cards -> CardSectionGrid(
                        items = payload.items,
                        onPlayAll = payload.onPlayAll,
                        onShuffle = payload.onShuffle,
                        topPadding = 12.dp + headerInset,
                        bottomPadding = bottomPadding,
                    )

                    is FeedSectionPayload.Artists -> ArtistSectionGrid(
                        artists = payload.artists,
                        onOpenArtist = payload.onOpenArtist,
                        topPadding = 12.dp + headerInset,
                        bottomPadding = bottomPadding,
                    )

                    is FeedSectionPayload.Shelf -> {
                        val allTracks = remember(payload.items) {
                            payload.items.isNotEmpty() && payload.items.all { it is HomeShelfItem.Track }
                        }
                        if (allTracks) {
                            val tracks = remember(payload.items) {
                                payload.items.filterIsInstance<HomeShelfItem.Track>().map { it.track }
                            }
                            TrackSectionList(
                                title = payload.title,
                                tracks = tracks,
                                onPlayTrack = { idx -> payload.onPlayTracksQueue(tracks, idx, payload.title) },
                                onShuffle = { payload.onShuffleTracksQueue(tracks, payload.title) },
                                topPadding = 10.dp + headerInset,
                                bottomPadding = bottomPadding,
                            )
                        } else {
                            val episodes = remember(payload.items) {
                                payload.items.filterIsInstance<HomeShelfItem.PodcastEpisode>()
                            }
                            val tracks = remember(payload.items) {
                                payload.items.filterIsInstance<HomeShelfItem.Track>().map { it.track }
                            }
                            val cards = remember(payload.items) {
                                payload.items.mapIndexed { idx, item ->
                                    when (item) {
                                        is HomeShelfItem.Track -> HomePlaylistItem(
                                            id = "sh_t_${item.track.videoId}_$idx",
                                            title = item.track.title,
                                            caption = ArtistHelper.primaryArtist(item.track.artist),
                                            artworkUrl = item.cardData?.largeArtworkUrl ?: item.track.artworkUrl,
                                            showProgress = false,
                                            onClick = { payload.onPlayTrack(item.track, payload.title) },
                                        )
                                        is HomeShelfItem.Playlist -> HomePlaylistItem(
                                            id = "sh_p_${item.playlist.id}_$idx",
                                            title = item.playlist.title,
                                            caption = item.playlist.author,
                                            artworkUrl = item.playlist.artworkUrl,
                                            showProgress = false,
                                            onClick = {
                                                val isAlbum = item.itemType.equals("album", ignoreCase = true) ||
                                                    item.playlist.id.startsWith("MPRE") ||
                                                    item.playlist.id.startsWith("OLAK")
                                                if (isAlbum) {
                                                    payload.onOpenAlbum(item.playlist.title, item.playlist.author.orEmpty(), item.playlist.id)
                                                } else {
                                                    payload.onOpenFeedPlaylist(item.playlist.id)
                                                }
                                            },
                                        )
                                        is HomeShelfItem.PodcastShow -> HomePlaylistItem(
                                            id = "sh_ps_${item.browseId}_$idx",
                                            title = item.title,
                                            caption = item.author,
                                            artworkUrl = item.artworkUrl,
                                            showProgress = false,
                                            onClick = { payload.onOpenPodcastShow(item.browseId) },
                                        )
                                        is HomeShelfItem.PodcastEpisode -> HomePlaylistItem(
                                            id = "sh_pe_${item.videoId}_$idx",
                                            title = item.title,
                                            caption = item.showName ?: item.subtitle,
                                            artworkUrl = item.artworkUrl,
                                            showProgress = false,
                                            onClick = { payload.onPlayPodcastEpisode(item, episodes, payload.title) },
                                        )
                                    }
                                }
                            }
                            CardSectionGrid(
                                items = cards,
                                onPlayAll = if (tracks.isNotEmpty()) {
                                    { payload.onPlayTracksQueue(tracks, 0, payload.title) }
                                } else null,
                                onShuffle = if (tracks.isNotEmpty()) {
                                    { payload.onShuffleTracksQueue(tracks, payload.title) }
                                } else null,
                                topPadding = 12.dp + headerInset,
                                bottomPadding = bottomPadding,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayShuffleActionRow(
    onPlayAll: (() -> Unit)?,
    onShuffle: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (onPlayAll == null && onShuffle == null) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onPlayAll != null) {
            Button(
                onClick = onPlayAll,
                modifier = Modifier.weight(1f),
                shape = CircleShape,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.feed_play_all), fontWeight = FontWeight.Bold)
            }
        }
        if (onShuffle != null) {
            FilledTonalButton(
                onClick = onShuffle,
                modifier = Modifier.weight(1f),
                shape = CircleShape,
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.feed_shuffle), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TrackSectionList(
    title: String,
    tracks: List<YouTubeMusicTrack>,
    onPlayTrack: (Int) -> Unit,
    onShuffle: () -> Unit,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    val musicPlayer = LocalMusicPlayer.current
    val playbackState by musicPlayer.listPlaybackState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var menuTrack by remember { mutableStateOf<YouTubeMusicTrack?>(null) }

    val probeKeyToTarget = remember(tracks) {
        tracks.mapIndexed { index, track ->
            "${track.videoId}:$index" to LosslessProbeTarget(
                title = track.title,
                artist = track.artist,
                durationSeconds = track.durationSeconds?.takeIf { it > 0 },
                album = track.album,
            )
        }.toMap()
    }
    ObserveVisibleLosslessProbes(listState = listState, keyToTarget = probeKeyToTarget)

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(GroupGap),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (tracks.isNotEmpty()) {
            item(key = "controls_header") {
                PlayShuffleActionRow(
                    onPlayAll = { onPlayTrack(0) },
                    onShuffle = onShuffle,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
        itemsIndexed(
            items = tracks,
            key = { index, track -> "${track.videoId}:$index" },
            contentType = { _, _ -> "section_track" },
        ) { index, track ->
            val isPlayingThis = playbackState.isPlaying &&
                (playbackState.current?.videoId == track.videoId ||
                    (playbackState.current?.title.equals(track.title, ignoreCase = true) &&
                        playbackState.current?.artist.equals(track.artist, ignoreCase = true)))

            val subtitle = listOfNotNull(
                track.artist.takeIf(String::isNotBlank),
                track.album?.takeIf(String::isNotBlank),
            ).joinToString(" \u2022 ")

            ExpressiveGroupTrackRow(
                title = track.title,
                subtitle = subtitle,
                position = groupPositionFor(index, tracks.size),
                isPlaying = isPlayingThis,
                onClick = { onPlayTrack(index) },
                onLongClick = { menuTrack = track },
                badgeTitle = track.title,
                badgeArtist = track.artist,
                dropArtworkUrl = track.artworkUrl,
                modifier = Modifier.padding(horizontal = 16.dp),
                leading = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    ) {
                        ArtworkImage(
                            name = track.title,
                            artist = track.artist,
                            embeddedUrl = track.artworkUrl,
                            fallbackIcon = Icons.Filled.MusicNote,
                            decodeSizePx = 160,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (isPlayingThis) {
                            PlayingWaveBars(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(2.dp)
                                    .size(24.dp, 18.dp),
                            )
                        }
                    }
                },
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        track.durationSeconds?.let { duration ->
                            Text(
                                text = "%d:%02d".format(duration / 60, duration % 60),
                                style = MaterialTheme.typography.labelSmall,
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
        val trackIndex = tracks.indexOfFirst { it.videoId == track.videoId }
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
            playbackSourceLabel = title,
            onPlayInLastWave = {
                if (trackIndex >= 0) onPlayTrack(trackIndex)
            },
            onDismiss = { menuTrack = null },
        )
    }
}

@Composable
private fun CardSectionGrid(
    items: List<HomePlaylistItem>,
    onPlayAll: (() -> Unit)?,
    onShuffle: (() -> Unit)?,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 148.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topPadding,
            bottom = bottomPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (onPlayAll != null || onShuffle != null) {
            item(key = "controls_header", span = { GridItemSpan(maxLineSpan) }) {
                PlayShuffleActionRow(
                    onPlayAll = onPlayAll,
                    onShuffle = onShuffle,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        items(items = items, key = { it.id }) { card ->
            Column(modifier = Modifier.fillMaxWidth()) {
                PlaylistArtCard(item = card)
                if (!card.caption.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = card.caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistSectionGrid(
    artists: List<FeedArtist>,
    onOpenArtist: (name: String, browseId: String?) -> Unit,
    topPadding: androidx.compose.ui.unit.Dp,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 112.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topPadding,
            bottom = bottomPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(
            items = artists,
            key = { index, artist -> "${artist.browseId ?: artist.name}:$index" },
        ) { index, artist ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                ArtistAvatarCard(
                    artist = artist,
                    onClick = { onOpenArtist(artist.name, artist.browseId) },
                    isTop = index == 0,
                )
            }
        }
    }
}
