package com.lastwave.app.ui.feed

import com.lastwave.app.data.music.chartRegionName
import androidx.compose.foundation.layout.WindowInsets
import androidx.activity.compose.BackHandler
import com.lastwave.app.playback.toPlayableTrack
import com.lastwave.app.ui.theme.LocalComicTheme
import androidx.compose.material.icons.filled.Download
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Radio
import com.lastwave.app.data.lossless.LosslessProbeTarget
import com.lastwave.app.data.lossless.LosslessListTier
import com.lastwave.app.data.lossless.losslessTrackKey
import com.lastwave.app.ui.common.LocalLosslessBadgeViewModel
import com.lastwave.app.ui.common.ObserveLosslessProbeTargets
import com.lastwave.app.ui.common.ObserveVisibleLosslessProbes
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.SharedFlow
import com.lastwave.app.ui.navigation.FEED_TAP_DEBOUNCE_MS
import com.lastwave.app.ui.navigation.FeedTapAction
import com.lastwave.app.ui.navigation.resolveFeedTapAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import com.lastwave.app.data.generate.youtubeVideoIdOrNull
import com.lastwave.app.data.local.HomeSection
import com.lastwave.app.data.local.DEFAULT_SECTION_ORDER
import com.lastwave.app.data.local.db.SongPlayStatsEntity
import com.lastwave.app.data.model.FriendEntry
import com.lastwave.app.data.model.RecentTrack
import com.lastwave.app.data.music.YouTubeMusicTrack
import com.lastwave.app.data.music.YouTubePlaylistSummary
import com.lastwave.app.data.music.HomeChip
import com.lastwave.app.data.music.HomeShelf
import com.lastwave.app.data.music.HomeShelfItem
import com.lastwave.app.playback.MediaType
import com.lastwave.app.playback.PlayableTrack
import com.lastwave.app.ui.common.ArtworkImage
import com.lastwave.app.ui.common.ExpressiveHeader
import com.lastwave.app.ui.common.fadeBelow
import com.lastwave.app.ui.common.ExpressiveShuffleButton
import com.lastwave.app.ui.common.HeaderActionIcon
import com.lastwave.app.ui.common.TrackContextMenuSheet
import com.lastwave.app.ui.common.TrackMiniTrayData
import com.lastwave.app.ui.common.TrackMiniTraySheet
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
import com.lastwave.app.ui.theme.comicPanel
import androidx.compose.material.icons.filled.Close



@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onOpenSettings: () -> Unit,
    onOpenOnboarding: () -> Unit = {},
    onOpenSearch: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenGenres: () -> Unit = onOpenDiscover,
    onOpenFeedPlaylist: (String) -> Unit,
    onOpenFeedSection: (String) -> Unit = {},
    onOpenPlaylist: (Long) -> Unit = {},
    onOpenGenerator: () -> Unit = {},
    onOpenFriends: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenFriendProfile: (username: String, displayName: String?, avatarUrl: String?) -> Unit = { _, _, _ -> },
    onOpenNewReleases: () -> Unit = {},
    onOpenPodcasts: () -> Unit = {},
    onOpenPodcastShow: (String) -> Unit = {},
    onOpenYouTubeLogin: () -> Unit = {},
    feedTapEvents: SharedFlow<Unit>? = null,
    viewModel: FeedViewModel = hiltViewModel(),
    artistAlbumNavigator: ArtistAlbumNavigator = hiltViewModel<ArtistAlbumNavBridgeFeed>().navigator,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = state.selectedChip != null) {
        viewModel.selectChip(null)
    }
    val discoveryTracks by viewModel.discoveryTracks.collectAsStateWithLifecycle()
    val recentPlayedStats by viewModel.recentPlayedStats.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) viewModel.onVisible()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val musicPlayer = LocalMusicPlayer.current
    val context = LocalContext.current
    val playbackState by musicPlayer.listPlaybackState.collectAsStateWithLifecycle()
    val fullPlaybackState by musicPlayer.stateWithoutProgress.collectAsStateWithLifecycle()
    var menuTrack by remember { mutableStateOf<YouTubeMusicTrack?>(null) }
    // Long-press (deep press) mini tray â€” 3-dot buttons keep the full sheet.
    var miniTrayTrack by remember { mutableStateOf<TrackMiniTrayData?>(null) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    // Sections hidden via Settings â†’ Home sections.
    val hiddenSections by viewModel.hiddenHomeSections.collectAsStateWithLifecycle()
    val homeSectionsOrder by viewModel.homeSectionsOrder.collectAsStateWithLifecycle()
    val homeBlocks = remember(homeSectionsOrder) { resolveHomeBlocks(homeSectionsOrder) }
    val homeDownloads by viewModel.homeDownloads.collectAsStateWithLifecycle()
    val homeLocalSongs by viewModel.homeLocalSongs.collectAsStateWithLifecycle()
    val rediscoverTracks by viewModel.rediscoverTracks.collectAsStateWithLifecycle()
    val losslessBadges = LocalLosslessBadgeViewModel.current?.badges?.collectAsStateWithLifecycle()?.value.orEmpty()
    var rearrangingHome by rememberSaveable { mutableStateOf(false) }
    if (rearrangingHome) {
        HomeLayoutSheet(
            order = homeBlocks,
            hidden = hiddenSections,
            onReorder = viewModel::saveHomeOrder,
            onToggle = viewModel::toggleHomeBlock,
            onDismiss = { rearrangingHome = false },
        )
    }
    val podcastsEnabled by viewModel.podcastsEnabled.collectAsStateWithLifecycle()
    val homeChartCountries by viewModel.homeChartCountries.collectAsStateWithLifecycle()
    val chartTimeWindow by viewModel.chartTimeWindow.collectAsStateWithLifecycle()
    val topGenres by viewModel.topGenres.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val hasFeedContent = state.isChipActive || with(state.feedData) {
        nativeShelves.any { it.items.isNotEmpty() } ||
            quickTiles.isNotEmpty() || mixes.isNotEmpty() || topArtists.isNotEmpty() ||
            quickPicks.isNotEmpty() || jumpBackIn.isNotEmpty() || recentAlbums.isNotEmpty() ||
            heavyRotation.isNotEmpty() || ytLikedSongs.isNotEmpty() || ytRecentSongs.isNotEmpty() ||
            (becauseYouListenTo?.items?.isNotEmpty() == true) || freshFinds.isNotEmpty() ||
            spotlight != null || charts.isNotEmpty() || newReleases.isNotEmpty() || friends.isNotEmpty() ||
            podcastShows.isNotEmpty()
    }
    val showSynthetic = !state.isChipActive
    val listState = rememberLazyListState()
    var lastFeedTapMillis by remember { mutableLongStateOf(0L) }
    var isFeedTapRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(state.isRefreshing) {
        if (!state.isRefreshing) {
            isFeedTapRefreshing = false
        }
    }

    LaunchedEffect(feedTapEvents) {
        feedTapEvents?.collect {
            val now = System.currentTimeMillis()
            val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
            val isChipSelected = state.selectedChip != null
            val isRefreshing = state.isRefreshing || isFeedTapRefreshing

            val action = resolveFeedTapAction(
                isChipSelected = isChipSelected,
                isAtTop = isAtTop,
                isOnHomeFeed = true,
                isRefreshing = isRefreshing,
                nowMs = now,
                lastTapMs = lastFeedTapMillis,
                debounceMs = FEED_TAP_DEBOUNCE_MS,
            )

            when (action) {
                FeedTapAction.DeselectChipAndRefresh -> {
                    lastFeedTapMillis = now
                    isFeedTapRefreshing = true
                    viewModel.selectChip(null)
                    listState.animateScrollToItem(0)
                    viewModel.refresh()
                }
                FeedTapAction.ScrollTopAndRefresh -> {
                    lastFeedTapMillis = now
                    isFeedTapRefreshing = true
                    listState.animateScrollToItem(0)
                    viewModel.refresh()
                }
                FeedTapAction.RefreshOnly -> {
                    lastFeedTapMillis = now
                    isFeedTapRefreshing = true
                    viewModel.refresh()
                }
                FeedTapAction.NavigateToFeed,
                FeedTapAction.Ignore -> { /* ignored */ }
            }
        }
    }
    val shouldLoadMore by remember(listState) {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && last >= total - 3
        }
    }
    LaunchedEffect(shouldLoadMore, state.pagedShelves.size, state.chipShelves.size) {
        if (shouldLoadMore && !state.isLoading && !state.isLoadingMore && !state.isRefreshing && !state.isChipLoading) {
            viewModel.loadMoreHomeShelves()
        }
    }
    val customProfile = com.lastwave.app.ui.theme.LocalUserProfile.current
    val effectiveAvatarUrl = customProfile.avatarUri ?: state.avatarUrl
    val baseGreeting = remember {
        when (java.time.LocalTime.now().hour) {
            in 5..11 -> context.getString(R.string.feed_greeting_morning)
            in 12..16 -> context.getString(R.string.feed_greeting_afternoon)
            in 17..21 -> context.getString(R.string.feed_greeting_evening)
            else -> context.getString(R.string.feed_greeting_night)
        }
    }
    val greeting = if (customProfile.name.isNotBlank()) "$baseGreeting, ${customProfile.name}" else baseGreeting
    val formattedDate = remember {
        try {
            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d"))
        } catch (_: Exception) { "" }
    }
    LaunchedEffect(state.error, hasFeedContent) {
        val error = state.error ?: return@LaunchedEffect
        if (hasFeedContent) {
            val result = snackbarHostState.showSnackbar(error, actionLabel = context.getString(R.string.feed_retry), withDismissAction = true)
            viewModel.dismissError()
            if (result == SnackbarResult.ActionPerformed) viewModel.refresh()
        }
    }
    val isPodcastsActive = state.selectedChip?.title.equals("Podcasts", ignoreCase = true)
    val podcastHeroScrollOffset by remember(listState) {
        derivedStateOf {
            val firstItem = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            if (firstItem == null || firstItem.index > 0) {
                10000f
            } else {
                listState.firstVisibleItemScrollOffset.toFloat()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (isPodcastsActive) {
            com.lastwave.app.ui.feed.cards.PodcastHeroBackground(
                backgroundUrl = state.chipBackgroundUrl,
                scrollOffsetY = { podcastHeroScrollOffset },
            )
        }

        // Skip full-screen offscreen LayerBackdrop capture on Feed (both default Home and mood chips):
        // recording the whole LazyColumn into an extra GraphicsLayer doubled GPU frame cost every scroll frame.
        var topBarHeightPx by remember { androidx.compose.runtime.mutableIntStateOf(0) }
        val topBarHeight = if (state.isChipActive) {
            with(androidx.compose.ui.platform.LocalDensity.current) { topBarHeightPx.toDp() }
        } else {
            0.dp
        }
        val listTopInset = 0.dp
        val pullState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()

        val config = LocalConfiguration.current
        val isLandscapeWide = config.screenWidthDp >= 720 && config.screenWidthDp > config.screenHeightDp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentWidth(maxWidth = 1160.dp),
        ) {
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refresh,
                state = pullState,
                indicator = {
                    androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = state.isRefreshing,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = listTopInset),
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topBarHeight),
            ) {
                // No full-screen skeleton: greeting, search, quick access and chips need no
                // network, and each data section stays out until its data arrives.
                run {
                    val isComic = LocalComicTheme.current
                    val ytNewReleasesShelf = remember(state.feedData.nativeShelves) {
                        state.feedData.nativeShelves.firstOrNull {
                            it.title.contains("new release", ignoreCase = true)
                        }
                    }
                    val combinedNewReleases = remember(state.feedData.newReleases, ytNewReleasesShelf) {
                        val fromYt = ytNewReleasesShelf?.items?.mapNotNull { item ->
                            when (item) {
                                is HomeShelfItem.Playlist -> item.playlist
                                is HomeShelfItem.Track -> YouTubePlaylistSummary(
                                    id = item.track.videoId,
                                    title = item.track.title,
                                    author = item.track.artist,
                                    trackCountText = null,
                                    artworkUrl = item.track.artworkUrl,
                                )
                                else -> null
                            }
                        }.orEmpty()
                        (state.feedData.newReleases + fromYt).distinctBy { it.id.ifBlank { it.title } }
                    }

                    val ytQuickPicksShelf = remember(state.feedData.nativeShelves) {
                        state.feedData.nativeShelves.firstOrNull {
                            it.title.contains("quick pick", ignoreCase = true) || it.title.contains("picked for you", ignoreCase = true)
                        }
                    }
                    val combinedQuickPicks = remember(state.feedData.quickPicks, ytQuickPicksShelf) {
                        val fromYt = ytQuickPicksShelf?.items?.filterIsInstance<HomeShelfItem.Track>()?.map { it.track }.orEmpty()
                        (state.feedData.quickPicks + fromYt).distinctBy { it.videoId.ifBlank { it.title } }
                    }

                    val heroArtworks = remember(
                        combinedQuickPicks,
                        state.feedData.freshFinds,
                        state.feedData.ytRecentSongs,
                        combinedNewReleases,
                        discoveryTracks,
                    ) {
                        (combinedQuickPicks.mapNotNull { it.artworkUrl } +
                            state.feedData.freshFinds.mapNotNull { it.artworkUrl } +
                            state.feedData.ytRecentSongs.mapNotNull { it.artworkUrl } +
                            combinedNewReleases.mapNotNull { it.artworkUrl } +
                            discoveryTracks.mapNotNull { it.artworkUrl })
                            .distinct()
                    }

                    val featuredSlides = remember(
                        heroArtworks,
                        state.feedData.ytSuggestedPlaylists,
                        state.feedData.countryCharts,
                        combinedNewReleases,
                    ) {
                        fun releaseSlide(release: YouTubePlaylistSummary, slot: Int) = HomeFeaturedItem(
                            id = "featured_rel_${release.id}_$slot",
                            category = "NEW RELEASE",
                            title = release.title,
                            description = release.author?.let(ArtistHelper::primaryArtist)?.takeIf { it.isNotBlank() }
                                ?: "Freshly dropped tracks to stream now.",
                            artworkUrl = release.artworkUrl ?: heroArtworks.getOrNull(slot),
                            onPlay = { viewModel.playPlaylistSummary(release) },
                            onOpen = {
                                artistAlbumNavigator.openAlbum(
                                    title = release.title,
                                    artist = release.author?.let(ArtistHelper::primaryArtist) ?: "",
                                    browseId = release.id,
                                )
                            },
                        )
                        val charts = state.feedData.countryCharts
                        if (charts.isNotEmpty()) {
                            return@remember charts.map { chart ->
                                val first = chart.top.firstOrNull()
                                HomeFeaturedItem(
                                    id = "featured_chart_${chart.country}_${chart.timeWindow.id}",
                                    category = "${chart.timeWindow.badgeLabel} · ${chartRegionName(chart.country).uppercase()}",
                                    title = first?.let { "#1 ${it.title}" } ?: chart.playlist.title,
                                    description = listOfNotNull(
                                        first?.artist,
                                        chart.top.drop(1).take(2).mapIndexed { i, t -> "#${i + 2} ${t.title}" }
                                            .joinToString(" · ").ifBlank { null },
                                    ).joinToString("\n"),
                                    artworkUrl = first?.artworkUrl ?: chart.playlist.artworkUrl,
                                    onPlay = { viewModel.playCountryChart(chart) },
                                    onOpen = { onOpenFeedPlaylist(chart.playlist.id) },
                                )
                            } + combinedNewReleases.take(1).map { releaseSlide(it, charts.size) }
                        }
                        val list = mutableListOf<HomeFeaturedItem>()
                        list += HomeFeaturedItem(
                            id = "featured_todays_hits",
                            category = "FEATURED PLAYLIST",
                            title = "Today's Hits",
                            description = "The biggest tracks right now, all in one playlist.",
                            artworkUrl = heroArtworks.firstOrNull(),
                            onPlay = viewModel::playInfiniteRadio,
                            onOpen = onOpenDiscover,
                        )
                        state.feedData.ytSuggestedPlaylists.take(2).forEachIndexed { idx, summary ->
                            list += HomeFeaturedItem(
                                id = "featured_pl_${summary.id}_$idx",
                                category = "PICKED FOR YOU",
                                title = summary.title,
                                description = summary.author?.takeIf { it.isNotBlank() } ?: "Curated playlist ready to play.",
                                artworkUrl = summary.artworkUrl ?: heroArtworks.getOrNull(idx + 1),
                                onPlay = { viewModel.playPlaylistSummary(summary) },
                                onOpen = { onOpenFeedPlaylist(summary.id) },
                            )
                        }
                        if (list.size < 3) {
                            combinedNewReleases.take(3 - list.size).forEach { release ->
                                list += releaseSlide(release, list.size)
                            }
                        }
                        if (list.size == 1 && heroArtworks.size > 1) {
                            list += HomeFeaturedItem(
                                id = "featured_discover_mix",
                                category = "DISCOVER WAVE",
                                title = "Fresh Finds",
                                description = "Handpicked sounds tuned to your listening vibe.",
                                artworkUrl = heroArtworks.getOrNull(1),
                                onPlay = { viewModel.playDiscoveryQuery("Fresh Finds", "indie pop") },
                                onOpen = onOpenDiscover,
                            )
                        }
                        list.toList()
                    }

                    val currentVideoId = fullPlaybackState.current?.videoId
                    val currentTitleNorm = fullPlaybackState.current?.title?.trim()?.lowercase().orEmpty()
                    val currentArtistNorm = fullPlaybackState.current?.artist?.trim()?.lowercase().orEmpty()

                    fun isMatchingPlayingTrack(videoId: String?, title: String, artist: String): Boolean {
                        if (!videoId.isNullOrBlank() && !currentVideoId.isNullOrBlank()) {
                            return videoId == currentVideoId
                        }
                        return currentTitleNorm.isNotEmpty() &&
                            title.trim().lowercase() == currentTitleNorm &&
                            artist.trim().lowercase() == currentArtistNorm
                    }

                    val recommendedTracks = remember(
                        state.feedData.freshFinds,
                        combinedQuickPicks,
                        state.feedData.ytLikedSongs,
                        state.feedData.charts,
                        discoveryTracks,
                        isLandscapeWide,
                    ) {
                        // Landscape fills the column beside Made For You to its height
                        // (see RecommendedFillColumn), so it may need a few more rows.
                        val poolSize = if (isLandscapeWide) 20 else 14
                        val personalPool = (state.feedData.freshFinds + combinedQuickPicks + state.feedData.ytLikedSongs)
                            .distinctBy { it.videoId.ifBlank { "${it.title}_${it.artist}" } }
                        val chartPool = state.feedData.charts
                            .distinctBy { it.videoId.ifBlank { "${it.title}_${it.artist}" } }
                        val interleaved = buildList(personalPool.size + chartPool.size) {
                            var p = 0
                            var c = 0
                            while (p < personalPool.size || c < chartPool.size) {
                                repeat(2) { if (p < personalPool.size) add(personalPool[p++]) }
                                if (c < chartPool.size) add(chartPool[c++])
                            }
                        }
                        val pool = interleaved
                            .distinctBy { it.videoId.ifBlank { "${it.title}_${it.artist}" } }
                            .take(poolSize)
                        pool.ifEmpty {
                            discoveryTracks.take(poolSize).map {
                                val playable = it.toPlayableTrack()
                                YouTubeMusicTrack(
                                    videoId = playable.videoId.orEmpty(),
                                    title = playable.title,
                                    artist = playable.artist,
                                    artworkUrl = playable.artworkUrl,
                                )
                            }
                        }
                    }

                    val continueTracks = remember(
                        fullPlaybackState.current,
                        recentPlayedStats,
                        state.feedData.ytRecentSongs,
                        state.feedData.jumpBackIn,
                        discoveryTracks,
                        recommendedTracks,
                    ) {
                        buildContinueListeningTracks(
                            currentTrack = fullPlaybackState.current,
                            recentPlayedStats = recentPlayedStats,
                            ytRecentSongs = state.feedData.ytRecentSongs,
                            jumpBackIn = state.feedData.jumpBackIn,
                            discoveryTracks = discoveryTracks,
                            recommendedTracks = recommendedTracks,
                        )
                    }

                    val continueItems = remember(
                        continueTracks,
                        currentVideoId,
                        currentTitleNorm,
                    ) {
                        continueTracks.mapIndexed { idx, track ->
                            val isActive = isMatchingPlayingTrack(track.videoId, track.title, track.artist)
                            val stableKey = track.videoId?.takeIf(String::isNotBlank)
                                ?: "${track.title.trim()}_${ArtistHelper.primaryArtist(track.artist)}"
                            HomeContinueItem(
                                id = "cont_$stableKey",
                                title = track.title,
                                artist = ArtistHelper.primaryArtist(track.artist),
                                artworkUrl = track.artworkUrl,
                                progressState = if (isActive) musicPlayer.progressState else null,
                                onClick = {
                                    musicPlayer.playQueue(
                                        continueTracks,
                                        startIndex = idx,
                                        sourceLabel = "Continue Listening",
                                        enableRadioContinuation = true,
                                    )
                                },
                                onLongClick = {
                                    miniTrayTrack = TrackMiniTrayData(
                                        title = track.title,
                                        artist = track.artist,
                                        album = track.album,
                                        artworkUrl = track.artworkUrl,
                                        videoId = track.videoId?.takeIf(String::isNotBlank),
                                        sourceLabel = "Continue Listening",
                                        onPlay = {
                                            musicPlayer.playQueue(
                                                continueTracks,
                                                startIndex = idx,
                                                sourceLabel = "Continue Listening",
                                                enableRadioContinuation = true,
                                            )
                                        },
                                    )
                                },
                            )
                        }
                    }

                    val madeForYouItems = remember(
                        state.feedData.mixes,
                        state.feedData.ytSuggestedPlaylists,
                        heroArtworks,
                    ) {
                        val list = mutableListOf<HomePlaylistItem>()
                        state.feedData.mixes.forEachIndexed { idx, mix ->
                            list += HomePlaylistItem(
                                id = "mix_${mix.seed.videoId}_$idx",
                                title = mix.title,
                                caption = "Based on ${mix.seed.title}",
                                artworkUrl = mix.seed.artworkUrl ?: heroArtworks.getOrNull(idx),
                                accentProgress = (0.78f - idx * 0.1f).coerceIn(0.35f, 0.85f),
                                showIndicatorIcon = idx == 0,
                                onClick = { viewModel.playMix(mix) },
                            )
                        }
                        if (list.size < 4) {
                            state.feedData.ytSuggestedPlaylists.forEachIndexed { idx, summary ->
                                if (list.size >= 6) return@forEachIndexed
                                if (list.none { it.title.equals(summary.title, ignoreCase = true) }) {
                                    list += HomePlaylistItem(
                                        id = "sug_${summary.id}_$idx",
                                        title = summary.title,
                                        artworkUrl = summary.artworkUrl ?: heroArtworks.getOrNull(list.size),
                                        accentProgress = 0.62f,
                                        showIndicatorIcon = list.isEmpty(),
                                        onClick = { viewModel.playPlaylistSummary(summary) },
                                    )
                                }
                            }
                        }
                        val defaults = listOf(
                            Triple("My Mix", "favorites hits") { viewModel.playInfiniteRadio() },
                            Triple("Chill Mix", "chill lofi") { viewModel.playDiscoveryQuery("Chill Mix", "chill") },
                            Triple("Focus Mix", "focus instrumental") { viewModel.playDiscoveryQuery("Focus Mix", "focus") },
                            Triple("Feels Mix", "acoustic indie") { viewModel.playDiscoveryQuery("Feels Mix", "indie") },
                        )
                        defaults.forEachIndexed { idx, (label, _, action) ->
                            if (list.size < 4 && list.none { it.title.equals(label, ignoreCase = true) }) {
                                val cardIndex = list.size
                                list += HomePlaylistItem(
                                    id = "default_mix_$label",
                                    title = label,
                                    artworkUrl = heroArtworks.getOrNull(cardIndex),
                                    accentProgress = (0.76f - idx * 0.12f).coerceIn(0.32f, 0.84f),
                                    showIndicatorIcon = cardIndex == 0,
                                    onClick = action,
                                )
                            }
                        }
                        list.toList()
                    }

                    // ── Optional Home sections (off by default; switched on in Edit home) ──
                    val newReleaseCards = remember(combinedNewReleases) {
                        combinedNewReleases.take(15).mapIndexed { idx, release ->
                            HomePlaylistItem(
                                id = "nr_${release.id}_$idx",
                                title = release.title,
                                caption = release.author?.let(ArtistHelper::primaryArtist),
                                artworkUrl = release.artworkUrl,
                                showProgress = false,
                                onClick = {
                                    artistAlbumNavigator.openAlbum(
                                        title = release.title,
                                        artist = release.author?.let(ArtistHelper::primaryArtist) ?: "",
                                        browseId = release.id,
                                    )
                                },
                            )
                        }
                    }
                    val yourMixCards = remember(state.feedData.nativeShelves) {
                        state.feedData.nativeShelves
                            .filter { it.title.contains("mix", ignoreCase = true) }
                            .flatMap { it.items }
                            .filterIsInstance<HomeShelfItem.Playlist>()
                            .map { it.playlist }
                            .distinctBy { it.id }
                            .take(15)
                            .mapIndexed { idx, mix ->
                                HomePlaylistItem(
                                    id = "ym_${mix.id}_$idx",
                                    title = mix.title,
                                    caption = mix.author,
                                    artworkUrl = mix.artworkUrl,
                                    showProgress = false,
                                    onClick = { viewModel.playPlaylistSummary(mix) },
                                )
                            }
                    }
                    val suggestedPlaylistCards = remember(state.feedData.ytSuggestedPlaylists) {
                        state.feedData.ytSuggestedPlaylists.take(15).mapIndexed { idx, summary ->
                            HomePlaylistItem(
                                id = "sp_${summary.id}_$idx",
                                title = summary.title,
                                caption = summary.author,
                                artworkUrl = summary.artworkUrl,
                                showProgress = false,
                                onClick = { onOpenFeedPlaylist(summary.id) },
                            )
                        }
                    }
                    val jumpBackInCards = remember(state.feedData.recentAlbums) {
                        state.feedData.recentAlbums.take(15).mapIndexed { idx, album ->
                            HomePlaylistItem(
                                id = "jb_${album.browseId ?: album.title}_$idx",
                                title = album.title,
                                caption = ArtistHelper.primaryArtist(album.artist),
                                artworkUrl = album.artworkUrl,
                                showProgress = false,
                                onClick = { artistAlbumNavigator.openAlbum(album.title, album.artist, album.browseId) },
                            )
                        }
                    }
                    val countryChartCards = remember(state.feedData.countryCharts) {
                        state.feedData.countryCharts.mapIndexed { idx, chart ->
                            HomePlaylistItem(
                                id = "cc_${chart.country}_$idx",
                                title = "Top ${chartRegionName(chart.country)}",
                                caption = chart.top.firstOrNull()?.let { "${chart.timeWindow.shortLabel} · #1 ${it.title}" },
                                artworkUrl = chart.top.firstOrNull()?.artworkUrl ?: chart.playlist.artworkUrl,
                                showProgress = false,
                                onClick = { onOpenFeedPlaylist(chart.playlist.id) },
                            )
                        }
                    }
                    val downloadCards = remember(homeDownloads) {
                        homeDownloads.mapIndexed { idx, track ->
                            HomePlaylistItem(
                                id = "dl_${track.id}",
                                title = track.title,
                                caption = ArtistHelper.primaryArtist(track.artist),
                                artworkUrl = track.artworkUrl,
                                showProgress = false,
                                onClick = { viewModel.playHomeDownloads(homeDownloads, idx) },
                            )
                        }
                    }
                    val localSongCards = remember(homeLocalSongs) {
                        homeLocalSongs.mapIndexed { idx, track ->
                            HomePlaylistItem(
                                id = "ls_${track.url.ifBlank { track.name }}_$idx",
                                title = track.name,
                                caption = ArtistHelper.primaryArtist(track.artist),
                                artworkUrl = track.artworkUrl,
                                showProgress = false,
                                onClick = { viewModel.playHomeLocalSongs(homeLocalSongs, idx) },
                            )
                        }
                    }
                    val onRepeatCards = remember(state.feedData.heavyRotation) {
                        val tracks = state.feedData.heavyRotation.take(15)
                        tracks.mapIndexed { idx, track ->
                            HomePlaylistItem(
                                id = "or_${track.name}_$idx",
                                title = track.name,
                                caption = track.playcount?.let { "$it plays" } ?: ArtistHelper.primaryArtist(track.artist),
                                artworkUrl = track.artworkUrl,
                                showProgress = false,
                                onClick = { viewModel.playGeneratedQueue(tracks, idx, "On Repeat") },
                            )
                        }
                    }
                    val quickPickTracks = remember(combinedQuickPicks) { combinedQuickPicks.take(12) }
                    val favouriteArtists = remember(state.feedData.topArtists) { state.feedData.topArtists.take(15) }

                    fun trackCards(prefix: String, tracks: List<YouTubeMusicTrack>, label: String) =
                        tracks.mapIndexed { idx, track ->
                            HomePlaylistItem(
                                id = "${prefix}_${track.videoId}_$idx",
                                title = track.title,
                                caption = ArtistHelper.primaryArtist(track.artist),
                                artworkUrl = track.artworkUrl,
                                showProgress = false,
                                onClick = { viewModel.playTracksQueue(tracks, idx, label) },
                            )
                        }
                    val becauseSection = state.feedData.becauseYouListenTo
                    val becauseCards = remember(becauseSection) {
                        trackCards("by", becauseSection?.items.orEmpty().take(15), becauseSection?.title.orEmpty())
                    }
                    // Probe every Home pool; only songs confirmed Hi-Res make the shelf.
                    val hiResPool = remember(state.feedData, combinedQuickPicks) {
                        with(state.feedData) {
                            (combinedQuickPicks + charts + freshFinds + ytLikedSongs + ytRecentSongs + becauseYouListenTo?.items.orEmpty())
                                .distinctBy { it.videoId.ifBlank { it.title } }
                                .take(60)
                        }
                    }
                    ObserveLosslessProbeTargets(
                        remember(hiResPool) { hiResPool.map { LosslessProbeTarget(it.title, it.artist, it.album, it.durationSeconds) } },
                    )
                    val hiResTracks = remember(hiResPool, losslessBadges) {
                        hiResPool.filter { losslessBadges[losslessTrackKey(it.title, it.artist)] == LosslessListTier.HI_RES }
                    }
                    val hiResCards = remember(hiResTracks) {
                        trackCards("hr", hiResTracks.take(15), "Hi-Res Picks")
                    }
                    val rediscoverCards = remember(rediscoverTracks) { trackCards("rd", rediscoverTracks, "Rediscover") }
                    val podcastCards = remember(state.feedData.podcastShows) {
                        state.feedData.podcastShows.take(15).map { show ->
                            HomePlaylistItem(
                                id = "pc_${show.showId}",
                                title = show.title,
                                caption = show.publisher,
                                artworkUrl = show.artworkUrl,
                                showProgress = false,
                                onClick = { onOpenPodcastShow(show.showId) },
                            )
                        }
                    }

                    val visibleHomeBlocks = homeBlocks.filterNot { isHomeBlockHidden(it, hiddenSections) }

                    /** A Home section item the user can hold to move, hide or rearrange. */
                    fun androidx.compose.foundation.lazy.LazyListScope.blockItem(
                        block: HomeBlock,
                        key: String,
                        contentType: String,
                        content: @Composable () -> Unit,
                    ) = item(key = key, contentType = contentType) {
                        HomeBlockHold(
                            block = block,
                            canMoveUp = visibleHomeBlocks.firstOrNull() != block,
                            canMoveDown = visibleHomeBlocks.lastOrNull() != block,
                            onMove = { up -> viewModel.saveHomeOrder(moveHomeBlock(homeBlocks, hiddenSections, block, up)) },
                            onHide = { viewModel.toggleHomeBlock(block) },
                            onRearrange = { rearrangingHome = true },
                            content = content,
                        )
                    }

                    fun openSection(payload: FeedSectionPayload) {
                        onOpenFeedSection(FeedSectionDetailStore.put(payload))
                    }

                    /** One optional section; empty data shows nothing at all. */
                    val renderOptionalBlock: androidx.compose.foundation.lazy.LazyListScope.(HomeBlock) -> Unit = { block ->
                        fun cardShelf(
                            key: String,
                            title: String,
                            subtitle: String?,
                            cards: List<HomePlaylistItem>,
                            onSeeAll: (() -> Unit)? = null,
                            onShuffle: (() -> Unit)? = null,
                        ) {
                            if (cards.isEmpty()) return
                            blockItem(block, key, "optional_card_shelf") {
                                Spacer(modifier = Modifier.height(24.dp))
                                SectionHeader(
                                    title = title,
                                    subtitle = subtitle,
                                    onSeeAll = onSeeAll ?: {
                                        openSection(FeedSectionPayload.Cards(title = title, subtitle = subtitle, items = cards))
                                    },
                                    onShuffle = onShuffle,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                MadeForYouRow(items = cards)
                            }
                        }
                        when (block) {
                            HomeBlock.NEW_RELEASES -> cardShelf(
                                "opt_new_releases", "New Releases", "Fresh albums and singles", newReleaseCards,
                                onSeeAll = onOpenNewReleases,
                            )
                            HomeBlock.YOUR_MIXES -> cardShelf("opt_your_mixes", "Your Mixes", "Mixed for you on YouTube Music", yourMixCards)
                            HomeBlock.SUGGESTED_PLAYLISTS -> cardShelf("opt_suggested", "Suggested Playlists", "From your YouTube Music account", suggestedPlaylistCards)
                            HomeBlock.JUMP_BACK_IN -> cardShelf("opt_jump_back_in", "Jump Back In", "Albums you played recently", jumpBackInCards)
                            HomeBlock.COUNTRY_CHARTS -> cardShelf("opt_country_charts", "Country Charts", "What's topping the charts", countryChartCards)
                            HomeBlock.DOWNLOADS -> cardShelf(
                                "opt_downloads", "Downloads", "Ready to play offline", downloadCards,
                                onSeeAll = onOpenDownloads,
                            )
                            HomeBlock.LOCAL_MUSIC -> cardShelf(
                                "opt_local_music", "Local Music", "Songs on this device", localSongCards,
                                onSeeAll = {
                                    openSection(
                                        FeedSectionPayload.Cards(
                                            title = "Local Music",
                                            subtitle = "Songs on this device",
                                            items = localSongCards,
                                            onPlayAll = { viewModel.playHomeLocalSongs(homeLocalSongs, 0) },
                                            onShuffle = { viewModel.playHomeLocalSongs(homeLocalSongs.shuffled(), 0) },
                                        ),
                                    )
                                },
                                onShuffle = { viewModel.playHomeLocalSongs(homeLocalSongs.shuffled(), 0) },
                            )
                            HomeBlock.ON_REPEAT -> cardShelf(
                                "opt_on_repeat", "On Repeat", "Your most played lately", onRepeatCards,
                                onSeeAll = {
                                    openSection(
                                        FeedSectionPayload.Cards(
                                            title = "On Repeat",
                                            subtitle = "Your most played lately",
                                            items = onRepeatCards,
                                            onPlayAll = { viewModel.playGeneratedQueue(state.feedData.heavyRotation, 0, "On Repeat") },
                                            onShuffle = { viewModel.playGeneratedQueue(state.feedData.heavyRotation.shuffled(), 0, "On Repeat") },
                                        ),
                                    )
                                },
                                onShuffle = { viewModel.playGeneratedQueue(state.feedData.heavyRotation.shuffled(), 0, "On Repeat") },
                            )
                            HomeBlock.BECAUSE_YOU_LISTEN -> {
                                val title = becauseSection?.title.orEmpty()
                                val items = becauseSection?.items.orEmpty()
                                cardShelf(
                                    "opt_because", title, becauseSection?.subtitle, becauseCards,
                                    onSeeAll = {
                                        openSection(
                                            FeedSectionPayload.Tracks(
                                                title = title,
                                                subtitle = becauseSection?.subtitle,
                                                tracks = items,
                                                onPlayTrack = { idx -> viewModel.playTracksQueue(items, idx, title) },
                                                onShuffle = { viewModel.shuffleTracksQueue(items, title) },
                                            ),
                                        )
                                    },
                                    onShuffle = { viewModel.shuffleTracksQueue(items, title) },
                                )
                            }
                            HomeBlock.HI_RES_PICKS -> cardShelf(
                                "opt_hi_res", "Hi-Res Picks", "Up to 24-bit / 192 kHz", hiResCards,
                                onSeeAll = {
                                    openSection(
                                        FeedSectionPayload.Tracks(
                                            title = "Hi-Res Picks",
                                            subtitle = "Up to 24-bit / 192 kHz",
                                            tracks = hiResTracks,
                                            onPlayTrack = { idx -> viewModel.playTracksQueue(hiResTracks, idx, "Hi-Res Picks") },
                                            onShuffle = { viewModel.shuffleTracksQueue(hiResTracks, "Hi-Res Picks") },
                                        ),
                                    )
                                },
                                onShuffle = { viewModel.shuffleTracksQueue(hiResTracks, "Hi-Res Picks") },
                            )
                            HomeBlock.REDISCOVER -> cardShelf(
                                "opt_rediscover", "Rediscover", "Favourites you haven't played in a while", rediscoverCards,
                                onSeeAll = {
                                    openSection(
                                        FeedSectionPayload.Tracks(
                                            title = "Rediscover",
                                            subtitle = "Favourites you haven't played in a while",
                                            tracks = rediscoverTracks,
                                            onPlayTrack = { idx -> viewModel.playTracksQueue(rediscoverTracks, idx, "Rediscover") },
                                            onShuffle = { viewModel.shuffleTracksQueue(rediscoverTracks, "Rediscover") },
                                        ),
                                    )
                                },
                                onShuffle = { viewModel.shuffleTracksQueue(rediscoverTracks, "Rediscover") },
                            )
                            HomeBlock.PODCASTS -> cardShelf(
                                "opt_podcasts", state.feedData.podcastShelfTitle ?: "Podcasts", null, podcastCards,
                                onSeeAll = onOpenPodcasts,
                            )
                            HomeBlock.QUICK_PICKS -> if (quickPickTracks.isNotEmpty()) {
                                blockItem(block, "opt_quick_picks", "optional_quick_picks") {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    SectionHeader(
                                        title = "Quick Picks",
                                        subtitle = "Tap and play",
                                        onSeeAll = {
                                            openSection(
                                                FeedSectionPayload.Tracks(
                                                    title = "Quick Picks",
                                                    subtitle = "Tap and play",
                                                    tracks = combinedQuickPicks,
                                                    onPlayTrack = { idx -> viewModel.playTracksQueue(combinedQuickPicks, idx, "Quick Picks") },
                                                    onShuffle = { viewModel.shuffleTracksQueue(combinedQuickPicks, "Quick Picks") },
                                                ),
                                            )
                                        },
                                        onShuffle = { viewModel.shuffleTracksQueue(combinedQuickPicks, "Quick Picks") },
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    QuickPicksRows(
                                        tracks = quickPickTracks,
                                        currentPlayingVideoId = currentVideoId,
                                        isPlaying = fullPlaybackState.isPlaying,
                                        onTrackClick = { idx -> viewModel.playTracksQueue(quickPickTracks, idx, "Quick Picks") },
                                        onMenuClick = { track -> menuTrack = track },
                                    )
                                }
                            }
                            HomeBlock.FAVOURITE_ARTISTS -> if (favouriteArtists.isNotEmpty()) {
                                blockItem(block, "opt_favourite_artists", "optional_artists") {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    SectionHeader(
                                        title = "Favourite Artists",
                                        subtitle = "The artists you play most",
                                        onSeeAll = {
                                            openSection(
                                                FeedSectionPayload.Artists(
                                                    title = "Favourite Artists",
                                                    subtitle = "The artists you play most",
                                                    artists = state.feedData.topArtists,
                                                    onOpenArtist = { name, browseId -> artistAlbumNavigator.openArtist(name, browseId) },
                                                ),
                                            )
                                        },
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    androidx.compose.foundation.lazy.LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(favouriteArtists.size) { i ->
                                            val artist = favouriteArtists[i]
                                            ArtistAvatarCard(
                                                artist = artist,
                                                onClick = { artistAlbumNavigator.openArtist(artist.name, artist.browseId) },
                                                isTop = i == 0,
                                            )
                                        }
                                    }
                                }
                            }
                            else -> Unit
                        }
                    }

                    val usingDiscoverFallback = remember(
                        state.feedData.freshFinds,
                        combinedQuickPicks,
                        state.feedData.ytLikedSongs,
                        state.feedData.charts,
                        discoveryTracks,
                    ) {
                        state.feedData.freshFinds.isEmpty() &&
                            combinedQuickPicks.isEmpty() &&
                            state.feedData.ytLikedSongs.isEmpty() &&
                            state.feedData.charts.isEmpty() &&
                            discoveryTracks.isNotEmpty()
                    }
                    val discoverQueue = remember(usingDiscoverFallback, discoveryTracks) {
                        if (usingDiscoverFallback) discoveryTracks.map { it.toPlayableTrack() } else emptyList()
                    }

                    val recommendedItems = remember(
                        recommendedTracks,
                        playbackState,
                        currentVideoId,
                        currentTitleNorm,
                        usingDiscoverFallback,
                        discoverQueue,
                    ) {
                        recommendedTracks.mapIndexed { idx, track ->
                            val isActiveList = isMatchingPlayingTrack(track.videoId, track.title, track.artist)
                            val isPlaying = isActiveList && playbackState.isPlaying
                            val playAction = {
                                if (usingDiscoverFallback && idx < discoverQueue.size) {
                                    musicPlayer.playDiscoverQueue(discoverQueue, idx)
                                } else {
                                    viewModel.playTracksQueue(recommendedTracks, idx, "Recommended for You")
                                }
                            }
                            HomeRecommendedItem(
                                id = "${track.videoId}_${track.title}_$idx",
                                track = track,
                                isCurrent = isActiveList,
                                isPlaying = isPlaying,
                                onClick = playAction,
                                onMenuClick = { menuTrack = track },
                                onLongClick = {
                                    miniTrayTrack = TrackMiniTrayData(
                                        title = track.title,
                                        artist = track.artist,
                                        album = track.album,
                                        artworkUrl = track.artworkUrl,
                                        videoId = track.videoId.takeIf(String::isNotBlank),
                                        sourceLabel = "Recommended for You",
                                        onPlay = playAction,
                                    )
                                },
                            )
                        }
                    }

                    val onShuffleRecommended = remember(
                        usingDiscoverFallback,
                        discoverQueue,
                        recommendedTracks,
                        viewModel,
                        musicPlayer,
                    ) {
                        {
                            if (usingDiscoverFallback && discoverQueue.isNotEmpty()) {
                                musicPlayer.playDiscoverQueue(discoverQueue.shuffled(), 0)
                            } else if (recommendedTracks.isNotEmpty()) {
                                viewModel.shuffleTracksQueue(recommendedTracks, "Recommended for You")
                            }
                        }
                    }

                    var selectedLocalMood by rememberSaveable { mutableStateOf<String?>(null) }
                    val moodLabels = remember(state.homeChips) {
                        val ytMoods = state.homeChips
                            .map { it.title }
                            .filterNot { it.equals("Podcasts", ignoreCase = true) }
                        ytMoods.ifEmpty { listOf("Relax", "Workout", "Focus", "Commute", "Energise") }
                    }
                    val onSelectMood = remember(state.homeChips, viewModel) {
                        { label: String ->
                            val norm = label.lowercase()
                            val matchingChip = state.homeChips.firstOrNull { chip ->
                                val chipNorm = chip.title.lowercase()
                                chipNorm == norm || (norm == "energise" && chipNorm == "energize")
                            }
                            if (matchingChip != null) {
                                selectedLocalMood = null
                                viewModel.onHomeChipSelected(matchingChip)
                            } else {
                                val nextMood = if (selectedLocalMood.equals(label, ignoreCase = true)) null else label
                                selectedLocalMood = nextMood
                                if (nextMood != null) {
                                    viewModel.playDiscoveryQuery("$nextMood Mix", nextMood.lowercase())
                                }
                            }
                        }
                    }

                    val quickAccessEntries = remember(onOpenDiscover, onOpenFeedPlaylist, state.homeChips) {
                        listOf(
                            HomeQuickAccessEntry(
                                id = "discover",
                                label = "Discover",
                                icon = Icons.Filled.Explore,
                                highlighted = true,
                                onClick = onOpenDiscover,
                            ),
                            HomeQuickAccessEntry(
                                id = "podcasts",
                                label = "Podcasts",
                                icon = Icons.Filled.Mic,
                                highlighted = false,
                                onClick = {
                                    val chip = state.homeChips.firstOrNull {
                                        it.title.equals("Podcasts", ignoreCase = true)
                                    } ?: HomeChip(title = "Podcasts", params = "ggMPOg1mZWVkX3BvZGNhc3Rz")
                                    viewModel.onHomeChipSelected(chip)
                                },
                            ),
                            HomeQuickAccessEntry(
                                id = "radio",
                                label = "Radio",
                                icon = Icons.Filled.Radio,
                                highlighted = false,
                                onClick = viewModel::playInfiniteRadio,
                            ),
                            HomeQuickAccessEntry(
                                id = "favourites",
                                label = "Favourites",
                                icon = Icons.Filled.Favorite,
                                highlighted = false,
                                onClick = { onOpenFeedPlaylist("yt_liked") },
                            ),
                        )
                    }

                val recommendedProbeKeyToTarget = remember(recommendedItems) {
                    recommendedItems.mapIndexed { idx, item ->
                        "rec_${item.id}_$idx" to LosslessProbeTarget(
                            title = item.track.title,
                            artist = item.track.artist,
                            durationSeconds = item.track.durationSeconds?.takeIf { it > 0 },
                            album = item.track.album,
                        )
                    }.toMap()
                }
                ObserveVisibleLosslessProbes(
                    listState = listState,
                    keyToTarget = recommendedProbeKeyToTarget,
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .safeHorizontalContentPadding(),
                    contentPadding = PaddingValues(
                        bottom = FloatingNavDefaults.contentBottomPadding(),
                        top = if (state.isChipActive) 12.dp + listTopInset else 0.dp,
                    ),
                    verticalArrangement = Arrangement.Top,
                ) {
                    if (state.isChipActive) {
                        if (state.isChipLoading) {
                            item(key = "chip_loading_skeleton", contentType = "shelf") {
                                HomeContainerSection {
                                    Column(verticalArrangement = Arrangement.spacedBy(CardTokens.sectionGap)) {
                                        com.lastwave.app.ui.feed.cards.ShelfSquareSkeletonRow(
                                            cardWidth = com.lastwave.app.ui.feed.cards.rememberSquareCardWidth(),
                                        )
                                        com.lastwave.app.ui.feed.cards.ShelfWideSkeletonRow(
                                            cardWidth = com.lastwave.app.ui.feed.cards.rememberWideCardWidth(),
                                        )
                                    }
                                }
                            }
                        } else if (state.chipShelves.isEmpty()) {
                            item(key = "chip_empty_state", contentType = "shelf") {
                                HomeContainerSection {
                                    PodcastGuestEmptyState(
                                        onLoginClick = onOpenYouTubeLogin,
                                    )
                                }
                            }
                        } else {
                            for ((shelfIndex, shelf) in state.chipShelves.withIndex()) {
                                if (shelf.items.isEmpty()) continue
                                item(key = "chip_shelf_${shelfIndex}_${shelf.title}", contentType = "shelf") {
                                    HomeContainerSection {
                                        HomeShelfCard(
                                            shelf = shelf,
                                            onPlayTrack = { track, title -> viewModel.playTrack(track, title) },
                                            onPlayTracksQueue = { tracks, idx, title -> viewModel.playTracksQueue(tracks, idx, title) },
                                            onShuffleTracksQueue = { tracks, title -> viewModel.shuffleTracksQueue(tracks, title) },
                                            onOpenFeedPlaylist = onOpenFeedPlaylist,
                                            onOpenAlbum = { title, artist, id -> artistAlbumNavigator.openAlbum(title, artist, id) },
                                            onTrackLongClick = { tray -> miniTrayTrack = tray },
                                            onPlayPlaylist = viewModel::playPlaylistSummary,
                                            onTrackMenu = { menuTrack = it },
                                            onOpenPodcastShow = onOpenPodcastShow,
                                            onPlayPodcastEpisode = { ep, q, title -> viewModel.playPodcastEpisode(ep, q, title) },
                                            onSeeAllShelf = { targetShelf ->
                                                openSection(
                                                    FeedSectionPayload.Shelf(
                                                        title = targetShelf.title,
                                                        subtitle = targetShelf.subtitle,
                                                        items = targetShelf.items,
                                                        onPlayTrack = { track, title -> viewModel.playTrack(track, title) },
                                                        onPlayTracksQueue = { tracks, idx, title -> viewModel.playTracksQueue(tracks, idx, title) },
                                                        onShuffleTracksQueue = { tracks, title -> viewModel.shuffleTracksQueue(tracks, title) },
                                                        onOpenFeedPlaylist = onOpenFeedPlaylist,
                                                        onOpenAlbum = { title, artist, id -> artistAlbumNavigator.openAlbum(title, artist, id) },
                                                        onOpenPodcastShow = onOpenPodcastShow,
                                                        onPlayPodcastEpisode = { ep, q, title -> viewModel.playPodcastEpisode(ep, q, title) },
                                                    ),
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        item(key = "home_header", contentType = "home_header") {
                            HomeHeader(
                                avatarUrl = effectiveAvatarUrl,
                                hasNotificationBadge = combinedNewReleases.isNotEmpty(),
                                musicPlayer = musicPlayer,
                                onOpenDownloads = onOpenDownloads,
                                onOpenNotifications = { showNotificationsDialog = true },
                                onOpenSettings = onOpenSettings,
                                onOpenSearch = onOpenSearch,
                                showCenterSearch = isLandscapeWide,
                            )
                        }

                        if (isLandscapeWide) {
                            item(key = "hero_split", contentType = "hero_split") {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        GreetingSection(horizontalPadding = 0.dp)
                                        Spacer(modifier = Modifier.height(14.dp))
                                        QuickAccessRow(
                                            items = quickAccessEntries,
                                            horizontalPadding = 0.dp,
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        HomeMoodChipsRow(
                                            moodLabels = moodLabels,
                                            selectedMood = selectedLocalMood,
                                            onSelectAll = {
                                                selectedLocalMood = null
                                                viewModel.selectChip(null)
                                            },
                                            onSelectMood = onSelectMood,
                                            horizontalPadding = 0.dp,
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1.05f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        ChartQuickChipsRow(
                                            selectedWindow = chartTimeWindow,
                                            selectedCountries = homeChartCountries,
                                            onSelectWindow = viewModel::selectChartTimeWindow,
                                            onSelectCountry = viewModel::focusHomeChartCountry,
                                            horizontalPadding = 0.dp,
                                        )
                                        FeaturedPlaylistCarousel(
                                            slides = featuredSlides,
                                            horizontalPadding = 0.dp,
                                        )
                                    }
                                }
                            }
                            if (continueItems.isNotEmpty()) {
                                blockItem(HomeBlock.CONTINUE_LISTENING, "continue_listening", "continue_listening") {
                                    Spacer(modifier = Modifier.height(22.dp))
                                    SectionHeader(
                                        title = "Continue Listening",
                                        onSeeAll = {
                                            if (state.feedData.ytRecentSongs.isNotEmpty()) {
                                                onOpenFeedPlaylist("yt_recent")
                                            } else {
                                                onOpenDiscover()
                                            }
                                        },
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ContinueListeningRow(items = continueItems)
                                }
                            }

                            if (madeForYouItems.isNotEmpty() || recommendedItems.isNotEmpty()) {
                                item(key = "lower_bento_split", contentType = "lower_bento_split") {
                                    if (recommendedItems.isNotEmpty()) {
                                        ObserveLosslessProbeTargets(
                                            remember(recommendedItems) {
                                                recommendedItems.map { item ->
                                                    LosslessProbeTarget(
                                                        title = item.track.title,
                                                        artist = item.track.artist,
                                                        durationSeconds = item.track.durationSeconds?.takeIf { it > 0 },
                                                        album = item.track.album,
                                                    )
                                                }
                                            },
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    if (madeForYouItems.isNotEmpty() && recommendedItems.isNotEmpty()) {
                                        val recPairs = recommendedItems.chunked(2)
                                        RecommendedFillColumn(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp),
                                            left = {
                                                Column {
                                                    SectionHeader(
                                                        title = "Made For You",
                                                        subtitle = "Personalised mixes based on your taste",
                                                        onSeeAll = onOpenGenerator,
                                                        horizontalPadding = 0.dp,
                                                    )
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    MadeForYouTwoByTwoGrid(items = madeForYouItems)
                                                }
                                            },
                                            rightHeader = {
                                                Column {
                                                    SectionHeader(
                                                        title = "Recommended for You",
                                                        // Same two-line header as Made For You beside it,
                                                        // so both titles and their content start level.
                                                        subtitle = "Songs picked from your listening",
                                                        onSeeAll = onOpenDiscover,
                                                        onShuffle = onShuffleRecommended,
                                                        horizontalPadding = 0.dp,
                                                    )
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                }
                                            },
                                            rowCount = recPairs.size,
                                            row = { index -> RecommendedPairRow(recPairs[index]) },
                                        )
                                    } else Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        if (madeForYouItems.isNotEmpty()) {
                                            Column(modifier = Modifier.weight(0.88f)) {
                                                SectionHeader(
                                                    title = "Made For You",
                                                    subtitle = "Personalised mixes based on your taste",
                                                    onSeeAll = onOpenGenerator,
                                                    horizontalPadding = 0.dp,
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                MadeForYouTwoByTwoGrid(items = madeForYouItems)
                                            }
                                        }
                                        if (recommendedItems.isNotEmpty()) {
                                            Column(modifier = Modifier.weight(1.12f)) {
                                                SectionHeader(
                                                    title = "Recommended for You",
                                                    // Same two-line header as Made For You beside it,
                                                    // so both titles and their content start level.
                                                    subtitle = "Songs picked from your listening",
                                                    onSeeAll = onOpenDiscover,
                                                    onShuffle = onShuffleRecommended,
                                                    horizontalPadding = 0.dp,
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    recommendedItems.chunked(2).forEach { pair ->
                                                        RecommendedPairRow(pair)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            // Shelf sections in the user's order; the fixed blocks above render nothing here.
                            homeBlocks
                                .filterNot { isHomeBlockHidden(it, hiddenSections) }
                                .forEach { renderOptionalBlock(it) }
                        } else {
                            item(key = "home_greeting", contentType = "home_greeting") {
                                Spacer(modifier = Modifier.height(10.dp))
                                GreetingSection()
                            }

                            item(key = "home_search_bar", contentType = "home_search_bar") {
                                Spacer(modifier = Modifier.height(14.dp))
                                HomeSearchBarPill(onClick = onOpenSearch)
                            }

                            if (!customProfile.onboardingCompleted) {
                                item(key = "onboarding_invite", contentType = "onboarding_invite") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OnboardingInviteCard(
                                        onStart = onOpenOnboarding,
                                        onDismiss = viewModel::dismissOnboardingInvite,
                                    )
                                }
                            }

                            run {
                                visibleHomeBlocks.forEach { block ->
                                    when (block) {
                                        HomeBlock.QUICK_ACCESS -> blockItem(block, "quick_access", "quick_access") {
                                            Spacer(modifier = Modifier.height(14.dp))
                                            QuickAccessRow(items = quickAccessEntries)
                                        }
                                        HomeBlock.MOOD_CHIPS -> blockItem(block, "home_mood_chips", "home_mood_chips") {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            HomeMoodChipsRow(
                                                moodLabels = moodLabels,
                                                selectedMood = selectedLocalMood,
                                                onSelectAll = {
                                                    selectedLocalMood = null
                                                    viewModel.selectChip(null)
                                                },
                                                onSelectMood = onSelectMood,
                                            )
                                        }
                                        HomeBlock.TOP_CHARTS -> blockItem(block, "featured_carousel", "featured_carousel") {
                                            Spacer(modifier = Modifier.height(14.dp))
                                            ChartQuickChipsRow(
                                                selectedWindow = chartTimeWindow,
                                                selectedCountries = homeChartCountries,
                                                onSelectWindow = viewModel::selectChartTimeWindow,
                                                onSelectCountry = viewModel::focusHomeChartCountry,
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            FeaturedPlaylistCarousel(slides = featuredSlides)
                                        }
                                        HomeBlock.CONTINUE_LISTENING -> if (continueItems.isNotEmpty()) {
                                            blockItem(block, "continue_listening", "continue_listening") {
                                                Spacer(modifier = Modifier.height(22.dp))
                                                SectionHeader(
                                                    title = "Continue Listening",
                                                    onSeeAll = {
                                                        if (state.feedData.ytRecentSongs.isNotEmpty()) {
                                                            onOpenFeedPlaylist("yt_recent")
                                                        } else {
                                                            onOpenDiscover()
                                                        }
                                                    },
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                ContinueListeningRow(items = continueItems)
                                            }
                                        }
                                        HomeBlock.MADE_FOR_YOU -> if (madeForYouItems.isNotEmpty()) {
                                            blockItem(block, "made_for_you", "made_for_you") {
                                                Spacer(modifier = Modifier.height(24.dp))
                                                SectionHeader(
                                                    title = "Made For You",
                                                    subtitle = "Personalised mixes based on your taste",
                                                    onSeeAll = onOpenGenerator,
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                MadeForYouRow(items = madeForYouItems)
                                            }
                                        }
                                        HomeBlock.RECOMMENDED -> if (recommendedItems.isNotEmpty()) {
                                            blockItem(block, "recommended_header", "recommended_header") {
                                                Spacer(modifier = Modifier.height(24.dp))
                                                SectionHeader(
                                                    title = "Recommended for You",
                                                    onSeeAll = onOpenDiscover,
                                                    onShuffle = onShuffleRecommended,
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                            itemsIndexed(
                                                items = recommendedItems,
                                                key = { idx, item -> "rec_${item.id}_$idx" },
                                                contentType = { _, _ -> "recommended_row" },
                                            ) { _, item ->
                                                RecommendedSongRow(item = item)
                                            }
                                        }
                                        else -> renderOptionalBlock(block)
                                    }
                                }
                            }
                        }

                        item(key = "feed_footer", contentType = "feed_footer") {
                            Spacer(modifier = Modifier.height(12.dp))
                            FeedFooter(lastUpdatedMillis = state.feedData.lastUpdatedMillis)
                        }
                    }

                    if (state.isChipActive && state.isLoadingMore) {
                        item(key = "loading_more_indicator", contentType = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                ExpressiveInlineLoadingIndicator()
                            }
                        }
                    }
                }
            }
        }

            // Unified header shown in chip sub-views (e.g. Workout, Relax, Podcasts).
            // PinnedChipsBar sits inside ExpressiveHeader.bottomContent so the header's
            // bottom edge/fade never overlaps the chip pills on any theme.
            if (state.isChipActive) {
                val headerBg = MaterialTheme.colorScheme.background
                ExpressiveHeader(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .onSizeChanged { if (it.height != topBarHeightPx) topBarHeightPx = it.height }
                        .then(if (isPodcastsActive) Modifier else Modifier.fadeBelow(headerBg)),
                    title = stringResource(R.string.feed_home),
                    containerColor = if (isPodcastsActive) Color.Transparent else Color.Unspecified,
                    backdrop = null,
                    leadingContent = {
                        HeaderActionIcon(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            onClick = { viewModel.selectChip(null) },
                        )
                    },
                    actions = {
                        HeaderActionIcon(Icons.Filled.Search, "Search", onOpenSearch)
                        IconButton(onClick = onOpenSettings) {
                            com.lastwave.app.ui.home.ProfileAvatar(avatarUrl = effectiveAvatarUrl, modifier = Modifier.size(38.dp))
                        }
                    },
                    bottomContent = if (state.homeChips.isNotEmpty()) {
                        {
                            PinnedChipsBar(
                                chips = state.homeChips,
                                selectedChip = state.selectedChip,
                                onChipClick = viewModel::onHomeChipSelected,
                                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
                            )
                        }
                    } else null,
                )
            }
    }
        // Cards scrolling under the clock and battery icons are softened by PlayerHost's
        // EdgeFade; a solid strip here drew a hard line and left that blur nothing to blur.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
                .adaptiveContentWidth(maxWidth = 600.dp)
                .safeHorizontalContentPadding()
                .padding(bottom = FloatingNavDefaults.contentBottomPadding()),
        )
    }


    menuTrack?.let { track ->
        TrackContextMenuSheet(
            target = TrackMenuTarget.Track(track.title, track.artist, ""),
            capabilities = TrackMenuCapabilities(showCopyActions = false, showDeleteScrobble = false),
            playableTrack = PlayableTrack(
                title = track.title,
                artist = track.artist,
                album = track.album,
                artworkUrl = track.artworkUrl,
                videoId = track.videoId.takeIf(String::isNotBlank),
            ),
            playbackSourceLabel = "Home",
            onDismiss = { menuTrack = null },
        )
    }

    miniTrayTrack?.let { tray ->
        TrackMiniTraySheet(
            data = tray,
            onDismiss = { miniTrayTrack = null },
        )
    }

    if (showNotificationsDialog) {
        HomeNotificationsDialog(
            newReleaseCount = state.feedData.newReleases.size,
            onOpenNewReleases = {
                showNotificationsDialog = false
                onOpenNewReleases()
            },
            onOpenDownloads = {
                showNotificationsDialog = false
                onOpenDownloads()
            },
            onDismiss = { showNotificationsDialog = false },
        )
    }
}

/** Two recommended songs side by side; a lone last song keeps the half width. */
@Composable
private fun RecommendedPairRow(pair: List<HomeRecommendedItem>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        pair.forEach { rec ->
            RecommendedSongRow(
                item = rec,
                horizontalPadding = 0.dp,
                compact = true,
                modifier = Modifier.weight(1f),
            )
        }
        if (pair.size == 1) {
            Spacer(Modifier.weight(1f))
        }
    }
}

/**
 * Landscape split: [left] (Made For You) beside a header and rows of songs. The left
 * side is measured first, then only as many rows as fit its height are shown, and the
 * spacing between them grows a little so both columns end on the same line. Card
 * sizes follow the screen width, so a fixed song count could not line up everywhere.
 */
@Composable
private fun RecommendedFillColumn(
    left: @Composable () -> Unit,
    rightHeader: @Composable () -> Unit,
    rowCount: Int,
    row: @Composable (Int) -> Unit,
    modifier: Modifier = Modifier,
    leftWeight: Float = 0.88f,
    rightWeight: Float = 1.12f,
    gap: Dp = 20.dp,
    rowSpacing: Dp = 4.dp,
    maxExtraSpacing: Dp = 12.dp,
    minRows: Int = 3,
) {
    androidx.compose.ui.layout.SubcomposeLayout(modifier) { constraints ->
        val gapPx = gap.roundToPx()
        val totalWidth = constraints.maxWidth
        val leftWidth = ((totalWidth - gapPx) * leftWeight / (leftWeight + rightWeight)).toInt()
        val rightWidth = totalWidth - gapPx - leftWidth
        val leftConstraints = androidx.compose.ui.unit.Constraints.fixedWidth(leftWidth)
        val rightConstraints = androidx.compose.ui.unit.Constraints.fixedWidth(rightWidth)

        val leftPlaceables = subcompose("left", left).map { it.measure(leftConstraints) }
        val leftHeight = leftPlaceables.maxOfOrNull { it.height } ?: 0
        val headerPlaceables = subcompose("header", rightHeader).map { it.measure(rightConstraints) }
        val headerHeight = headerPlaceables.sumOf { it.height }

        val spacingPx = rowSpacing.roundToPx()
        val rows = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var contentHeight = headerHeight
        for (index in 0 until rowCount) {
            val placeable = subcompose("row_$index") { row(index) }
                .map { it.measure(rightConstraints) }
                .firstOrNull() ?: break
            val next = contentHeight + (if (rows.isEmpty()) 0 else spacingPx) + placeable.height
            // Keep a row while it ends no more than half a row past the left column.
            if (rows.size >= minRows && next > leftHeight + placeable.height / 2) break
            rows += placeable
            contentHeight = next
        }

        // Spread any shortfall over the gaps so the last row meets the left edge's bottom.
        val shortfall = leftHeight - contentHeight
        val extraPerGap = if (shortfall > 0 && rows.size > 1) {
            minOf(shortfall / (rows.size - 1), maxExtraSpacing.roundToPx())
        } else 0
        val rightHeight = contentHeight + extraPerGap * (rows.size - 1).coerceAtLeast(0)

        layout(totalWidth, maxOf(leftHeight, rightHeight)) {
            var y = 0
            leftPlaceables.forEach { it.placeRelative(0, y); y += it.height }
            val rightX = leftWidth + gapPx
            y = 0
            headerPlaceables.forEach { it.placeRelative(rightX, y); y += it.height }
            rows.forEachIndexed { index, placeable ->
                if (index > 0) y += spacingPx + extraPerGap
                placeable.placeRelative(rightX, y)
                y += placeable.height
            }
        }
    }
}

/**
 * Builds up to [limit] tracks for "Continue Listening": actually listened tracks
 * (the active song, local play stats, YouTube Music history, and jump-back-in)
 * lead in order, and Discover / Recommended songs fill any remaining slots until
 * the user has listened to enough distinct songs to own the full row.
 */
internal fun buildContinueListeningTracks(
    currentTrack: PlayableTrack?,
    recentPlayedStats: List<SongPlayStatsEntity>,
    ytRecentSongs: List<YouTubeMusicTrack>,
    jumpBackIn: List<RecentTrack>,
    discoveryTracks: List<GeneratedTrack>,
    recommendedTracks: List<YouTubeMusicTrack>,
    limit: Int = 10,
): List<PlayableTrack> {
    val result = ArrayList<PlayableTrack>(limit)
    val seenVideoIds = HashSet<String>()
    val seenTrackKeys = HashSet<String>()

    fun tryAdd(track: PlayableTrack) {
        if (result.size >= limit) return
        if (track.title.isBlank()) return
        if (track.mediaType == MediaType.PODCAST_EPISODE ||
            !track.showId.isNullOrBlank() ||
            track.artist.equals("Podcast", ignoreCase = true)
        ) return
        val vid = track.videoId?.trim()?.takeIf { it.isNotBlank() }
        val key = "${track.title.trim().lowercase()}|${ArtistHelper.primaryArtist(track.artist).trim().lowercase()}"
        if (vid != null && vid in seenVideoIds) return
        if (key in seenTrackKeys) return
        if (vid != null) seenVideoIds += vid
        seenTrackKeys += key
        result += track
    }

    currentTrack?.let(::tryAdd)
    recentPlayedStats.forEach { stat ->
        tryAdd(
            PlayableTrack(
                title = stat.title,
                artist = stat.artist,
                artworkUrl = stat.artworkUrl,
                videoId = stat.videoId?.takeIf(String::isNotBlank),
            ),
        )
    }
    ytRecentSongs.forEach { tryAdd(it.toPlayableTrack()) }
    jumpBackIn.forEach { rec ->
        val vid = GeneratedTrack(rec.name, rec.artist.displayName, rec.artworkUrl, rec.url).youtubeVideoIdOrNull()
        tryAdd(
            PlayableTrack(
                title = rec.name,
                artist = rec.artist.displayName,
                album = rec.album.displayName.takeIf(String::isNotBlank),
                artworkUrl = rec.artworkUrl,
                videoId = vid,
            ),
        )
    }
    discoveryTracks.forEach { tryAdd(it.toPlayableTrack()) }
    recommendedTracks.forEach { tryAdd(it.toPlayableTrack()) }
    return result
}

@Composable
private fun OnboardingInviteCard(
    onStart: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    androidx.compose.material3.Surface(
        shape = shape,
        color = com.lastwave.app.ui.theme.glassCardColor(),
        border = com.lastwave.app.ui.theme.glassCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .comicPanel(shape, offset = 3.dp)
            .clickable(onClick = onStart),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Set up profile & explore new features",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Photo, theme, Hi-Res addons & interactive gesture tour",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            androidx.compose.material3.FilledTonalButton(
                onClick = onStart,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text("Start", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
