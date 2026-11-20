package com.lastwave.app.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.data.music.*
import com.lastwave.app.ui.feed.cards.*
import kotlinx.coroutines.flow.collect

/** Only endpoint-qualified community and daily shelves enter this presentation path. */
@Composable
internal fun CommunityDiscoverShelf(
    shelf: HomeShelf,
    onPlayTrack: (YouTubeMusicTrack, String) -> Unit,
    onPlayTracksQueue: (List<YouTubeMusicTrack>, Int, String) -> Unit,
    onPlayPlaylist: (YouTubePlaylistSummary) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onTrackMenu: (YouTubeMusicTrack) -> Unit,
    onTrackLongClick: (YouTubeMusicTrack) -> Unit,
    onSeeAllShelf: ((HomeShelf) -> Unit)? = null,
    onHeaderLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    fallback: @Composable (HomeShelfItem) -> Unit,
) {
    val daily = isDailyDiscoverShelf(shelf)
    val tracks = remember(shelf.items) { shelf.items.filterIsInstance<HomeShelfItem.Track>().map { it.track } }
    val moreId = shelf.moreBrowseId?.removePrefix("VL")
        ?.takeIf { it.startsWith("PL") || it.startsWith("MPRE") || it.startsWith("RD") || it.startsWith("OLAK") }
    val seeAllAction: (() -> Unit)? = when {
        moreId != null -> { { onOpenPlaylist(moreId) } }
        onSeeAllShelf != null && shelf.items.size > 1 -> { { onSeeAllShelf(shelf) } }
        else -> null
    }
    val cleanSubtitle = remember(shelf.subtitle, shelf.strapline) {
        formatShelfSubtitle(shelf.subtitle, shelf.strapline)
    }
    Column(modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        SectionHeader(
            title = shelf.title,
            subtitle = cleanSubtitle,
            onSeeAll = seeAllAction,
            onShuffle = if (tracks.isNotEmpty()) { { onPlayTracksQueue(tracks.shuffled(), 0, shelf.title) } } else null,
            onLongClick = onHeaderLongClick,
        )
        Spacer(Modifier.height(12.dp))
        // Same single-column shelf on every theme (DESIGN.md 3a).
        FeedMediaShelfRow(
            items = shelf.items,
            columns = 1,
            topPadding = 0.dp,
        ) { index, item ->
            val width = com.lastwave.app.ui.feed.cards.LocalSymmetricCardWidth.current ?: 320.dp
            when {
                isCommunityPlaylistItem(item) -> {
                    val playlist = item as HomeShelfItem.Playlist
                    CommunityPlaylistShelfItem(playlist, visible = true, width = width,
                        onOpen = { onOpenPlaylist(playlist.playlist.id) },
                        onPlay = { onPlayPlaylist(playlist.playlist) },
                        onSong = { onPlayTrack(it, playlist.playlist.title) },
                        onMenu = onTrackMenu)
                }
                daily && item is HomeShelfItem.Track -> ShelfFeaturedCard(
                    title = item.track.title,
                    artist = listOfNotNull(item.track.artist.takeIf(String::isNotBlank), item.cardData?.playsText).joinToString(" • "),
                    artworkUrl = item.cardData?.largeArtworkUrl ?: item.track.artworkUrl,
                    caption = item.caption,
                    onClick = { onPlayTrack(item.track, shelf.title) },
                    onLongClick = { onTrackLongClick(item.track) },
                    onMenuClick = { onTrackMenu(item.track) },
                    cardWidth = width, isVisible = true,
                )
                else -> fallback(item)
            }
        }
    }
}

@Composable
private fun CommunityPlaylistShelfItem(
    item: HomeShelfItem.Playlist,
    visible: Boolean,
    width: androidx.compose.ui.unit.Dp,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onSong: (YouTubeMusicTrack) -> Unit,
    onMenu: (YouTubeMusicTrack) -> Unit,
    model: PlaylistPreviewViewModel = hiltViewModel(),
) {
    val loader by model.loader.collectAsStateWithLifecycle()
    var state by remember(loader, item.playlist.id) { mutableStateOf<PlaylistPreviewState>(PlaylistPreviewState.Loading) }
    LaunchedEffect(loader, item.playlist.id, visible) {
        if (visible) loader.load(item.playlist.id).collect { state = it }
    }
    val preview = (state as? PlaylistPreviewState.Ready)?.preview
    val subtitle = listOfNotNull(
        item.playlist.author?.takeIf(String::isNotBlank), item.cardData?.viewsText,
        preview?.songCount?.let { if (it == 1) "1 song" else "$it songs" },
    ).joinToString(" • ")
    ShelfCommunityPlaylistCard(
        playlistId = item.playlist.id,
        title = item.playlist.title, subtitle = subtitle,
        collageUrls = preview?.collageArtworkUrls.orEmpty(), songs = preview?.songs.orEmpty(),
        artworkUrl = item.playlist.artworkUrl, onClick = onOpen, onPlayClick = onPlay,
        onSongClick = onSong, onSongMenuClick = onMenu, cardWidth = width,
        isLoading = state is PlaylistPreviewState.Loading && visible,
        previewFailed = state is PlaylistPreviewState.Failed,
        songPlayCounts = preview?.songPlayCounts.orEmpty(),
    )
}
