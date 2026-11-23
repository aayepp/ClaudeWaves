package com.lastwave.app.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.lastwave.app.R
import com.lastwave.app.data.lossless.LosslessProbeTarget
import com.lastwave.app.data.repository.HomeSortMode
import com.lastwave.app.data.repository.HomeTrack
import com.lastwave.app.ui.common.ObserveVisibleLosslessProbes
import com.lastwave.app.ui.common.TrackMiniTrayData
import com.lastwave.app.ui.common.TrackMiniTraySheet
import com.lastwave.app.ui.common.adaptiveContentWidth
import com.lastwave.app.ui.common.safeHorizontalContentPadding
import com.lastwave.app.ui.shell.FloatingNavDefaults

/**
 * Audiophile Telemetry Strip Stats screen (Stats 3):
 *  1. Compact telemetry top bar — "Stats", live monospace duration pill, Share, Friends, Settings avatar.
 *  2. Swiss 4-column hairline metric grid (Plays/Scrobbles, Tracks, Artists, Albums) + genre link row.
 *  3. 7-day listening activity bar chart.
 *  4. Tab strip (Tracks / Artists / Albums / History + timeframe selector) and ranked/history list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenGenres: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenDownloads: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    artistAlbumNavigator: com.lastwave.app.ui.navigation.ArtistAlbumNavigator = hiltViewModel<ArtistAlbumNavBridgeHome>().navigator,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val jamViewModel: com.lastwave.app.ui.jam.LastWaveJamViewModel = hiltViewModel()
    var showJamSheet by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.pollWhileActive()
        }
    }

    var menuTrack by remember { mutableStateOf<HomeTrack?>(null) }
    var showRecapShare by rememberSaveable { mutableStateOf(false) }

    val customProfile = com.lastwave.app.ui.theme.LocalUserProfile.current
    val displayUsername = when {
        uiState.isViewingFriend -> uiState.viewingUsername
        customProfile.name.isNotBlank() -> customProfile.name
        uiState.username.isNotBlank() -> uiState.username
        uiState.isLocalStatsMode -> "Guest"
        else -> uiState.username
    }
    val hasRecapData = !uiState.isLoading && (
        uiState.allTracks.isNotEmpty() || uiState.topTracksOverall.isNotEmpty() || uiState.topArtists.isNotEmpty()
    )

    Scaffold(
        // Transparent so the page wash / background image shows; Scaffold's own fill hid it.
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                StatsTelemetryTopBar(
                    displayUsername = displayUsername,
                    isViewingFriend = uiState.isViewingFriend,
                    showShare = hasRecapData,
                    avatarUrl = customProfile.avatarUri ?: uiState.stats?.avatarUrl,
                    onShare = { showRecapShare = true },
                    onOpenFriends = onOpenFriends,
                    onOpenSettings = onOpenSettings,
                    viewModel = viewModel,
                    modifier = Modifier.adaptiveContentWidth(maxWidth = 860.dp),
                )
            }
        },
    ) { scaffoldPadding ->
        if (uiState.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(scaffoldPadding).safeHorizontalContentPadding(),
                contentAlignment = Alignment.Center,
            ) {
                com.lastwave.app.ui.common.ExpressiveLoadingIndicator(message = "Loading your listening history")
            }
            return@Scaffold
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier
                    .fillMaxSize()
                    .adaptiveContentWidth(maxWidth = 860.dp)
                    .safeHorizontalContentPadding(),
            ) {
                val listState = rememberLazyListState()
                LaunchedEffect(listState, uiState.allTracks.size) {
                    snapshotFlowNearEnd(listState) { viewModel.loadNextPage() }
                }

                val rows by viewModel.rows.collectAsStateWithLifecycle()
                val playbackQueue = remember(rows) {
                    rows.mapNotNull { row ->
                        (row as? HomeRow.Track)?.track?.let { track ->
                            com.lastwave.app.playback.PlayableTrack(
                                title = track.name,
                                artist = track.artist,
                                artworkUrl = track.artworkUrl,
                                videoId = track.videoId,
                            )
                        }
                    }
                }
                val playbackIndexByRow = remember(rows) {
                    var nextPlaybackIndex = 0
                    IntArray(rows.size) { rowIndex ->
                        if (rows[rowIndex] is HomeRow.Track) nextPlaybackIndex++ else -1
                    }
                }
                val isRankedMode = uiState.sortMode != HomeSortMode.RECENT
                val rankByRow = remember(rows, isRankedMode) {
                    var nextRank = 1
                    IntArray(rows.size) { rowIndex ->
                        if (!isRankedMode) {
                            -1
                        } else {
                            when (val row = rows[rowIndex]) {
                                is HomeRow.Track -> if (row.track.isNowPlaying) -1 else nextRank++
                                is HomeRow.Album, is HomeRow.Artist -> nextRank++
                                is HomeRow.DateHeader -> -1
                            }
                        }
                    }
                }
                val rowKeys = remember(rows) {
                    val seen = HashMap<String, Int>(rows.size)
                    Array(rows.size) { i ->
                        val base = when (val row = rows[i]) {
                            is HomeRow.DateHeader -> "date_${row.label}"
                            is HomeRow.Track -> if (row.track.isNowPlaying) {
                                "nowplaying_${row.track.key}"
                            } else {
                                "track_${row.track.key}_${row.track.timestampMillis}"
                            }
                            is HomeRow.Album -> "album_${row.album.artist}_${row.album.name}"
                            is HomeRow.Artist -> "artist_${row.artist.name}"
                        }
                        val n = seen[base] ?: 0
                        seen[base] = n + 1
                        if (n == 0) base else "${base}#$n"
                    }
                }
                val musicPlayer = com.lastwave.app.ui.player.LocalMusicPlayer.current
                var miniTrayRowIndex by remember { mutableStateOf<Int?>(null) }

                val probeKeyToTarget = remember(rows, rowKeys) {
                    rows.mapIndexedNotNull { index, row ->
                        val track = (row as? HomeRow.Track)?.track ?: return@mapIndexedNotNull null
                        val key = rowKeys.getOrNull(index) ?: return@mapIndexedNotNull null
                        key to LosslessProbeTarget(
                            title = track.name,
                            artist = track.artist,
                        )
                    }.toMap()
                }
                ObserveVisibleLosslessProbes(listState = listState, keyToTarget = probeKeyToTarget)

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 0.dp,
                        bottom = FloatingNavDefaults.contentBottomPadding(),
                    ),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (uiState.isLocalStatsMode && !uiState.isViewingFriend) {
                        item(key = "local_notice", contentType = "local_notice") {
                            LocalStatsInlineNotice(
                                isOfflineFallback = uiState.isLastFmConnected,
                                onClick = if (uiState.isLastFmConnected) viewModel::refresh else onOpenSettings,
                            )
                        }
                    }

                    uiState.stats?.let { stats ->
                        item(key = "telemetry_metrics", contentType = "telemetry_metrics") {
                            TelemetryMetricGrid(
                                scrobbles = stats.scrobbles,
                                trackCount = stats.trackCount,
                                artistCount = stats.artistCount,
                                albumCount = stats.albumCount,
                                headlineLabel = if (uiState.isLocalStatsMode && !uiState.isViewingFriend) "Plays" else "Scrobbles",
                                onSelectTracks = { viewModel.setSortMode(HomeSortMode.MOST_PLAYED) },
                                onSelectArtists = { viewModel.setSortMode(HomeSortMode.TOP_ARTISTS) },
                                onSelectAlbums = { viewModel.setSortMode(HomeSortMode.TOP_ALBUMS) },
                            )
                        }
                    }

                    item(key = "telemetry_genres", contentType = "telemetry_genres") {
                        TelemetryGenreRow(
                            tags = uiState.topTags,
                            onOpenGenres = onOpenGenres,
                        )
                    }

                    item(key = "telemetry_activity", contentType = "telemetry_activity") {
                        TelemetryActivityChart(bins = uiState.dailyActivity7d)
                    }

                    item(key = "telemetry_tabs", contentType = "telemetry_tabs") {
                        TelemetryTabStrip(
                            sortMode = uiState.sortMode,
                            onSortModeChange = viewModel::setSortMode,
                        )
                    }

                    itemsIndexed(
                        rows,
                        key = { index, _ -> rowKeys.getOrNull(index) ?: index.toString() },
                        contentType = { _, row ->
                            when (row) {
                                is HomeRow.DateHeader -> "date"
                                is HomeRow.Track -> "track"
                                is HomeRow.Album -> "album"
                                is HomeRow.Artist -> "artist"
                            }
                        },
                    ) { rowIndex, row ->
                        val rank = rankByRow.getOrNull(rowIndex)?.takeIf { it > 0 }
                        val showDivider = rowIndex < rows.lastIndex && rows[rowIndex + 1] !is HomeRow.DateHeader
                        Box {
                            when (row) {
                                is HomeRow.DateHeader -> DateHeaderRow(row.label)
                                is HomeRow.Track -> TrackRow(
                                    track = row.track,
                                    badge = row.badge,
                                    rank = rank,
                                    showDivider = showDivider,
                                    onClick = {
                                        musicPlayer.playQueue(
                                            tracks = playbackQueue,
                                            startIndex = playbackIndexByRow.getOrElse(rowIndex) { 0 },
                                            sourceLabel = "Home",
                                        )
                                    },
                                    onLongClick = { miniTrayRowIndex = rowIndex },
                                    onMenuClick = { menuTrack = row.track },
                                )
                                is HomeRow.Album -> AlbumRow(
                                    album = row.album,
                                    badge = row.badge,
                                    rank = rank,
                                    showDivider = showDivider,
                                    onClick = {
                                        artistAlbumNavigator.openAlbum(
                                            title = row.album.name,
                                            artist = row.album.artist,
                                            browseId = "",
                                        )
                                    },
                                )
                                is HomeRow.Artist -> ArtistRow(
                                    artist = row.artist,
                                    badge = row.badge,
                                    rank = rank,
                                    showDivider = showDivider,
                                    onClick = {
                                        artistAlbumNavigator.openArtist(
                                            name = row.artist.name,
                                            browseId = "",
                                        )
                                    },
                                )
                            }
                        }
                    }

                    if (rows.isEmpty()) {
                        item(key = "empty", contentType = "empty") {
                            Box(
                                Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(stringResource(R.string.home_no_tracks), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                miniTrayRowIndex?.let { trayIndex ->
                    (rows.getOrNull(trayIndex) as? HomeRow.Track)?.let { trayRow ->
                        TrackMiniTraySheet(
                            data = TrackMiniTrayData(
                                title = trayRow.track.name,
                                artist = trayRow.track.artist,
                                artworkUrl = trayRow.track.artworkUrl,
                                sourceLabel = "Home",
                                onPlay = {
                                    musicPlayer.playQueue(
                                        tracks = playbackQueue,
                                        startIndex = playbackIndexByRow.getOrElse(trayIndex) { 0 },
                                        sourceLabel = "Home",
                                    )
                                },
                            ),
                            onDismiss = { miniTrayRowIndex = null },
                        )
                    }
                }
            }
        }
    }

    menuTrack?.let { track ->
        com.lastwave.app.ui.common.TrackContextMenuSheet(
            target = com.lastwave.app.ui.common.TrackMenuTarget.Track(track.name, track.artist, track.artworkUrl.orEmpty()),
            capabilities = com.lastwave.app.ui.common.TrackMenuCapabilities(showCopyActions = true, showDeleteScrobble = true),
            playbackSourceLabel = "Home",
            onDismiss = { menuTrack = null },
        )
    }

    if (showJamSheet) {
        com.lastwave.app.ui.jam.LastWaveJamSheet(
            viewModel = jamViewModel,
            onDismiss = { showJamSheet = false },
        )
    }

    if (showRecapShare) {
        RecapShareDialog(
            uiState = uiState,
            onDismiss = { showRecapShare = false },
        )
    }
}

internal suspend fun snapshotFlowNearEnd(listState: LazyListState, onNearEnd: () -> Unit) {
    snapshotFlow {
        val info = listState.layoutInfo
        val total = info.totalItemsCount
        val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
        total > 0 && lastVisible >= total - 5
    }.collect { isNear -> if (isNear) onNearEnd() }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class ArtistAlbumNavBridgeHome @javax.inject.Inject constructor(
    val navigator: com.lastwave.app.ui.navigation.ArtistAlbumNavigator
) : androidx.lifecycle.ViewModel()
