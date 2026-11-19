package com.lastwave.app.ui.feed

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastwave.app.data.feed.FeedMix
import com.lastwave.app.data.feed.FeedArtist
import com.lastwave.app.data.feed.FeedData
import com.lastwave.app.data.feed.FeedQuickTile
import com.lastwave.app.data.feed.FeedRepository
import com.lastwave.app.data.generate.GeneratedTrack
import com.lastwave.app.data.generate.youtubeVideoIdOrNull
import com.lastwave.app.data.local.DEFAULT_HOME_CHART_COUNTRIES
import com.lastwave.app.data.local.SessionPreferences
import com.lastwave.app.data.local.SettingsPreferences
import com.lastwave.app.data.model.RecentTrack
import com.lastwave.app.data.music.ChartTimeWindow
import com.lastwave.app.data.music.CountryChart
import com.lastwave.app.data.music.HomeChip
import com.lastwave.app.data.music.HomeShelf
import com.lastwave.app.data.music.HomeShelfItem
import com.lastwave.app.data.music.InnerTubeMusicApi
import com.lastwave.app.data.music.YouTubeMusicTrack
import com.lastwave.app.data.music.YouTubePlaylistSummary
import com.lastwave.app.playback.MusicPlayer
import com.lastwave.app.playback.PlayableTrack
import com.lastwave.app.playback.toPlayableTrack
import com.lastwave.app.data.music.toPlayableTrack
import com.lastwave.app.ui.settings.toPlayableTrack
import com.lastwave.app.data.recommend.RecommendationRepository
import com.lastwave.app.data.ytmusic.YtMusicAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class FeedUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val launchingRadio: String? = null,
    val error: String? = null,
    val avatarUrl: String? = null,
    val feedData: FeedData = FeedData(),
    val selectedChip: HomeChip? = null,
    val isChipLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val pagedShelves: List<HomeShelf> = emptyList(),
    val chipShelves: List<HomeShelf> = emptyList(),
    val chipContinuationToken: String? = null,
    val defaultContinuationToken: String? = null,
    val chipBackgroundUrl: String? = null,
) {
    /** True when a mood/activity chip is active; synthetic sections are hidden in this mode. */
    val isChipActive: Boolean get() = selectedChip != null
    val homeChips: List<HomeChip> get() = feedData.homeChips
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
    private val repository: FeedRepository,
    private val sessionPreferences: SessionPreferences,
    private val settingsPreferences: SettingsPreferences,
    private val musicPlayer: MusicPlayer,
    private val innerTube: InnerTubeMusicApi,
    private val recommendationRepository: RecommendationRepository,
    private val ytAuth: YtMusicAuthManager,
    private val homeRepository: com.lastwave.app.data.repository.HomeRepository,
    private val discoverRepository: com.lastwave.app.data.discover.DiscoverRepository,
    private val genresRepository: com.lastwave.app.data.generate.GenresRepository,
    downloadedTrackDao: com.lastwave.app.data.local.db.DownloadedTrackDao,
    playlistRepository: com.lastwave.app.data.playlist.PlaylistRepository,
    songPlayStatsDao: com.lastwave.app.data.local.db.SongPlayStatsDao,
) : ViewModel() {

    /** Locally played songs (newest first), so Continue Listening updates as tracks play. */
    val recentPlayedStats: StateFlow<List<com.lastwave.app.data.local.db.SongPlayStatsEntity>> =
        songPlayStatsDao.observeRecentlyPlayed(20)
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Favourites unplayed for a month, for the optional "Rediscover" section. */
    val rediscoverTracks: StateFlow<List<YouTubeMusicTrack>> =
        songPlayStatsDao.observeForgotten(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000, 15)
            .map { list ->
                list.map { YouTubeMusicTrack(videoId = it.videoId.orEmpty(), title = it.title, artist = it.artist, artworkUrl = it.artworkUrl) }
            }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Newest downloads, for the optional "Downloads" Home section. */
    val homeDownloads: StateFlow<List<com.lastwave.app.data.local.db.DownloadedTrackEntity>> =
        downloadedTrackDao.getAll()
            .map { list -> list.sortedByDescending { it.downloadedAtMillis }.take(20) }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Songs from the device's Local Songs hub, for the optional "Local Music" section. */
    val homeLocalSongs: StateFlow<List<GeneratedTrack>> =
        playlistRepository.playlists
            .map { playlists ->
                playlists.firstOrNull { it.mode == com.lastwave.app.data.playlist.LOCAL_SONGS_MODE }
                    ?.tracks?.take(20).orEmpty()
            }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun playHomeDownloads(tracks: List<com.lastwave.app.data.local.db.DownloadedTrackEntity>, index: Int) {
        if (tracks.isEmpty()) return
        musicPlayer.playQueue(
            tracks = tracks.map { it.toPlayableTrack() },
            startIndex = index.coerceIn(0, tracks.lastIndex),
            sourceLabel = "Downloads",
        )
    }

    fun playHomeLocalSongs(tracks: List<GeneratedTrack>, index: Int) {
        if (tracks.isEmpty()) return
        musicPlayer.playQueue(
            tracks = tracks.map { it.toPlayableTrack() },
            startIndex = index.coerceIn(0, tracks.lastIndex),
            sourceLabel = "Local Songs",
        )
    }

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
    private val _discoveryTracks = MutableStateFlow<List<GeneratedTrack>>(emptyList())
    val discoveryTracks: StateFlow<List<GeneratedTrack>> = _discoveryTracks.asStateFlow()

    private val _topGenres = MutableStateFlow<List<String>>(emptyList())
    val topGenres: StateFlow<List<String>> = _topGenres.asStateFlow()

    /** Ids of Home sections the user hid in Settings → Home sections. */
    val hiddenHomeSections: StateFlow<Set<String>> = settingsPreferences.settings
        .map { it.hiddenHomeSections }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val homeSectionsOrder: StateFlow<List<String>> = settingsPreferences.settings
        .map { it.homeSectionsOrder }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    internal fun saveHomeOrder(order: List<HomeBlock>) {
        viewModelScope.launch { settingsPreferences.setHomeSectionsOrder(order.map { it.id }) }
    }

    internal fun toggleHomeBlock(block: HomeBlock) {
        viewModelScope.launch { settingsPreferences.toggleHomeSectionHidden(block.id) }
    }

    internal fun dismissOnboardingInvite() {
        viewModelScope.launch { settingsPreferences.setOnboardingCompleted(true) }
    }

    /** Podcast surfaces are hidden the moment the user turns them off. */
    val podcastsEnabled: StateFlow<Boolean> = settingsPreferences.settings
        .map { it.podcastsEnabled }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val homeChartCountries: StateFlow<List<String>> = settingsPreferences.settings
        .map { it.homeChartCountries }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DEFAULT_HOME_CHART_COUNTRIES)

    val chartTimeWindow: StateFlow<ChartTimeWindow> = settingsPreferences.settings
        .map { it.chartTimeWindow }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChartTimeWindow.VIRAL)

    fun selectChartTimeWindow(window: ChartTimeWindow) {
        viewModelScope.launch { settingsPreferences.setChartTimeWindow(window) }
    }

    fun focusHomeChartCountry(country: String) {
        viewModelScope.launch {
            val current = settingsPreferences.settings.first().homeChartCountries
            val next = (listOf(country) + current.filterNot { it == country }).take(3)
            settingsPreferences.setHomeChartCountries(next)
        }
    }

    private var feedJob: Job? = null
    private var chipJob: Job? = null
    private var loadMoreJob: Job? = null
    private var requestGeneration: Long = 0L
    private var lastLoadedMillis: Long = 0L

    init {
        loadDiscovery()
        loadTopGenres()
        viewModelScope.launch {
            // The podcast shelf and the chart slides are part of the feed, so
            // changing any of these settings has to re-run it: the cached copy
            // still holds the old ones.
            settingsPreferences.settings
                .map { Triple(it.podcastsEnabled, it.homeChartCountries, it.chartTimeWindow) }
                .distinctUntilChanged()
                .drop(1)
                .collect { loadFeed() }
        }
        viewModelScope.launch {
            ytAuth.awaitLoadedConnection()
            combine(ytAuth.connection, sessionPreferences.session) { connection, session ->
                connection to session.username
            }.distinctUntilChanged().collect { (connection, _) ->
                feedJob?.cancel()
                val cached = repository.getCachedFeed()
                _uiState.value = FeedUiState(
                    isLoading = cached == null,
                    feedData = cached ?: FeedData(
                        isYtConnected = connection.isConnected,
                        ytAccountName = connection.accountName.takeIf { connection.isConnected },
                    ),
                )
                loadFeed()
            }
        }
        viewModelScope.launch {
            val stats = runCatching { homeRepository.fetchStats().getOrNull() }.getOrNull()
            if (stats != null) {
                _uiState.update { it.copy(avatarUrl = stats.avatarUrl) }
            }
        }
    }

    fun loadDiscovery(forceRotate: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cached = discoverRepository.allowedCachedFeed()
                val tracks = if (forceRotate) {
                    val batch = discoverRepository.nextBatch(8)
                    if (batch.isNotEmpty()) batch else cached.shuffled().take(8)
                } else if (cached.isNotEmpty()) {
                    cached.take(8)
                } else {
                    discoverRepository.nextBatch(8)
                }
                _discoveryTracks.value = tracks
            } catch (_: Exception) {
                // Non-destructive fallback if offline/failed
            }
        }
    }

    fun loadFeed() = fetchFeed(refreshing = false)

    fun refresh() {
        loadDiscovery(forceRotate = true)
        loadTopGenres()
        fetchFeed(refreshing = true)
    }

    fun loadTopGenres() {
        viewModelScope.launch(Dispatchers.IO) {
            val stats = runCatching { genresRepository.fetchGenreStats("overall") }.getOrNull().orEmpty()
            val list = if (stats.isNotEmpty()) {
                stats.map { it.name.trim() }.filter { it.isNotBlank() }
            } else {
                listOf("Pop", "Rock", "Hip-Hop", "Indie", "Electronic", "R&B", "Metal", "Jazz", "Classical", "Lo-Fi", "Country", "Alternative")
            }
            _topGenres.value = list
        }
    }

    fun onVisible() {
        if (feedJob?.isActive == true) return
        // ON_START fires on every return from a pushed screen (artist, album,
        // playlist...). Reloading the whole feed each time kept the tabs in
        // a near-constant loading state and hammered the network — only
        // refresh when there's nothing yet or the data is stale.
        val hasContent = _uiState.value.feedData.quickPicks.isNotEmpty() ||
            _uiState.value.feedData.topArtists.isNotEmpty() ||
            _uiState.value.feedData.newReleases.isNotEmpty()
        val stale = System.currentTimeMillis() - lastLoadedMillis > STALE_AFTER_MILLIS
        if (!hasContent || stale) refresh()
    }

    fun playInfiniteRadio() {
        val feed = _uiState.value.feedData
        val ytSeed = (feed.quickPicks + feed.ytLikedSongs + feed.freshFinds).randomOrNull()
        if (ytSeed != null) {
            playTrack(ytSeed, "Infinite Radio")
            return
        }
        val discovery = _discoveryTracks.value
        if (discovery.isNotEmpty()) {
            musicPlayer.playDiscoverQueue(discovery.map { it.toPlayableTrack() }, discovery.indices.random())
        }
    }

    fun shuffleHeroWave(tracks: List<YouTubeMusicTrack>) {
        val playableYt = tracks.filter { it.videoId.isNotBlank() }
        if (playableYt.isNotEmpty()) {
            shuffleTracksQueue(playableYt, "Wave Mix")
            return
        }
        val discovery = _discoveryTracks.value
        if (discovery.isNotEmpty()) {
            musicPlayer.playDiscoverQueue(discovery.shuffled().map { it.toPlayableTrack() }, 0)
        }
    }

    fun playMix(mix: FeedMix) = playTrack(mix.seed, mix.title)

    private fun fetchFeed(refreshing: Boolean) {
        feedJob?.cancel()
        chipJob?.cancel()
        loadMoreJob?.cancel()
        requestGeneration++
        feedJob = viewModelScope.launch {
            val self = coroutineContext[Job]
            _uiState.update {
                it.copy(
                    isLoading = !refreshing || it.isLoading,
                    isRefreshing = refreshing,
                    error = null,
                    selectedChip = if (refreshing) null else it.selectedChip,
                    isChipLoading = if (refreshing) false else it.isChipLoading,
                    isLoadingMore = if (refreshing) false else it.isLoadingMore,
                    pagedShelves = if (refreshing) emptyList() else it.pagedShelves,
                    chipShelves = if (refreshing) emptyList() else it.chipShelves,
                    chipContinuationToken = if (refreshing) null else it.chipContinuationToken,
                    defaultContinuationToken = if (refreshing) null else it.defaultContinuationToken,
                    chipBackgroundUrl = if (refreshing) null else it.chipBackgroundUrl,
                )
            }
            try {
                val connection = ytAuth.awaitLoadedConnection()
                val username = sessionPreferences.session.value.username.takeIf(String::isNotBlank)
                val data = repository.loadFeed(username) { update ->
                    ensureActive()
                    if (ytAuth.connection.value == connection &&
                        sessionPreferences.session.value.username.takeIf(String::isNotBlank) == username
                    ) {
                        _uiState.update {
                            it.copy(
                                feedData = update,
                                defaultContinuationToken = update.homeContinuationToken ?: it.defaultContinuationToken,
                                isLoading = false,
                            )
                        }
                    }
                }
                ensureActive()
                if (ytAuth.connection.value != connection ||
                    sessionPreferences.session.value.username.takeIf(String::isNotBlank) != username
                ) return@launch
                val stats = homeRepository.fetchStats().getOrNull()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        feedData = data,
                        defaultContinuationToken = data.homeContinuationToken,
                        error = null,
                        avatarUrl = stats?.avatarUrl ?: it.avatarUrl,
                    )
                }
                lastLoadedMillis = System.currentTimeMillis()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                ensureActive()
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = appContext.getString(com.lastwave.app.R.string.feed_update_error))
                }
            } finally {
                // Every exit above the success path — cancellation mid-load
                // and the changed-identity guard — used to leave isRefreshing
                // true, and with it a pull-to-refresh spinner that spins over
                // fully-loaded content. Only the job that still owns the state
                // may clear it, so a newer feed job's flags are never reset.
                if (feedJob === self) {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
            }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    /**
     * Toggles the selected mood/activity [chip].
     * Tapping the already selected chip returns to the default view.
     * Selecting a new chip cancels any in-flight requests, hides synthetic sections,
     * and displays only the chip's YTM carousel shelves.
     */
    fun onHomeChipSelected(chip: HomeChip?) = selectChip(chip)

    fun selectChip(chip: HomeChip?) {
        val currentSelected = _uiState.value.selectedChip
        chipJob?.cancel()
        loadMoreJob?.cancel()
        val currentGen = ++requestGeneration
        if (chip == null || chip == currentSelected) {
            _uiState.update {
                it.copy(
                    selectedChip = null,
                    isChipLoading = false,
                    isLoadingMore = false,
                    chipShelves = emptyList(),
                    chipContinuationToken = null,
                    chipBackgroundUrl = null,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                selectedChip = chip,
                isChipLoading = true,
                isLoadingMore = false,
                chipShelves = emptyList(),
                chipContinuationToken = null,
                chipBackgroundUrl = null,
                error = null,
            )
        }

        chipJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val page = innerTube.fetchHomeChip(chip)
                ensureActive()
                if (requestGeneration != currentGen || _uiState.value.selectedChip != chip) return@launch
                _uiState.update {
                    it.copy(
                        isChipLoading = false,
                        chipShelves = page.shelves,
                        chipContinuationToken = page.continuationToken,
                        chipBackgroundUrl = page.backgroundUrl,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                ensureActive()
                if (requestGeneration == currentGen) {
                    _uiState.update {
                        it.copy(
                            selectedChip = null,
                            isChipLoading = false,
                            chipBackgroundUrl = null,
                            error = appContext.getString(com.lastwave.app.R.string.feed_update_error),
                        )
                    }
                }
            }
        }
    }

    /**
     * Loads the next page of shelves when scrolling near the end of Home.
     * Enforces in-flight guard (one request at a time), stale response guard
     * (ignores completed loads if chip or feed changed), and dedupe guard
     * (never appends a shelf with matching title + first item id).
     */
    fun loadMoreHomeShelves() {
        val state = _uiState.value
        if (state.isLoadingMore || state.isChipLoading || state.isLoading || state.isRefreshing) return

        val isChipActive = state.selectedChip != null
        val token = if (isChipActive) {
            state.chipContinuationToken
        } else {
            state.defaultContinuationToken ?: state.feedData.homeContinuationToken
        }
        if (token.isNullOrBlank()) return

        val currentGen = requestGeneration
        val activeChip = state.selectedChip
        _uiState.update { it.copy(isLoadingMore = true) }

        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val nextPage = innerTube.fetchHomeContinuation(token)
                ensureActive()
                if (requestGeneration != currentGen || _uiState.value.selectedChip != activeChip) return@launch

                val existingShelves = if (isChipActive) {
                    _uiState.value.chipShelves
                } else {
                    _uiState.value.feedData.nativeShelves + _uiState.value.pagedShelves
                }
                val existingKeys = existingShelves.map(::shelfFingerprint).toSet()

                val newShelves = nextPage.shelves.filter { shelf ->
                    shelf.items.isNotEmpty() && shelfFingerprint(shelf) !in existingKeys
                }

                _uiState.update { current ->
                    if (isChipActive) {
                        current.copy(
                            isLoadingMore = false,
                            chipShelves = current.chipShelves + newShelves,
                            chipContinuationToken = nextPage.continuationToken,
                        )
                    } else {
                        current.copy(
                            isLoadingMore = false,
                            pagedShelves = current.pagedShelves + newShelves,
                            defaultContinuationToken = nextPage.continuationToken,
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                ensureActive()
                if (requestGeneration == currentGen) {
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            error = appContext.getString(com.lastwave.app.R.string.feed_update_error),
                        )
                    }
                }
            }
        }
    }

    private fun shelfFingerprint(shelf: HomeShelf): String {
        val firstId = when (val item = shelf.items.firstOrNull()) {
            is HomeShelfItem.Track -> item.track.videoId
            is HomeShelfItem.Playlist -> item.playlist.id
            is HomeShelfItem.PodcastEpisode -> item.videoId
            is HomeShelfItem.PodcastShow -> item.browseId
            null -> ""
        }
        return "${shelf.title.trim().lowercase()}_$firstId"
    }

    fun playPodcastEpisode(
        episode: HomeShelfItem.PodcastEpisode,
        queue: List<HomeShelfItem.PodcastEpisode> = listOf(episode),
        sourceLabel: String = "Podcasts",
    ) {
        val tracks = queue.map { it.toPlayableTrack() }
        val index = queue.indexOfFirst { it.videoId == episode.videoId }.coerceAtLeast(0)
        musicPlayer.playPodcastQueue(
            tracks = tracks,
            startIndex = index,
            startPositionMs = 0L,
            sourceLabel = episode.showName?.takeIf { it.isNotBlank() } ?: sourceLabel,
        )
    }

    fun playTrack(track: YouTubeMusicTrack, sourceLabel: String = "Feed") {
        musicPlayer.play(track.toPlayableTrack(), sourceLabel = sourceLabel, startRadio = true)
    }

    fun shuffleTracksQueue(tracks: List<YouTubeMusicTrack>, sourceLabel: String = "Feed") {
        if (tracks.isEmpty()) return
        val shuffled = tracks.shuffled()
        musicPlayer.playQueue(
            shuffled.map { it.toPlayableTrack() },
            startIndex = 0,
            sourceLabel = "$sourceLabel Shuffle",
            enableRadioContinuation = true,
        )
    }

    fun playTracksQueue(tracks: List<YouTubeMusicTrack>, startIndex: Int = 0, sourceLabel: String = "Feed") {
        if (tracks.isEmpty()) return
        val playable = tracks.map { it.toPlayableTrack() }
        musicPlayer.playQueue(
            playable,
            startIndex = startIndex.coerceIn(0, playable.lastIndex.coerceAtLeast(0)),
            sourceLabel = sourceLabel,
            enableRadioContinuation = true,
        )
    }

    fun playRecentQueue(tracks: List<RecentTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val playable = tracks.map {
            PlayableTrack(
                title = it.name,
                artist = it.artist.displayName,
                album = it.album.displayName,
                artworkUrl = it.artworkUrl,
                videoId = GeneratedTrack(it.name, it.artist.displayName, it.artworkUrl, it.url).youtubeVideoIdOrNull(),
            )
        }
        musicPlayer.playQueue(
            playable,
            startIndex = startIndex.coerceIn(0, playable.lastIndex.coerceAtLeast(0)),
            sourceLabel = "Jump Back In",
            enableRadioContinuation = true,
        )
    }

    fun playGeneratedQueue(tracks: List<GeneratedTrack>, startIndex: Int = 0, sourceLabel: String = "Heavy Rotation") {
        if (tracks.isEmpty()) return
        val playable = tracks.map {
            PlayableTrack(
                title = it.name,
                artist = it.artist,
                album = it.album,
                artworkUrl = it.artworkUrl,
                videoId = it.youtubeVideoIdOrNull(),
            )
        }
        musicPlayer.playQueue(
            playable,
            startIndex = startIndex.coerceIn(0, playable.lastIndex.coerceAtLeast(0)),
            sourceLabel = sourceLabel,
            enableRadioContinuation = true,
        )
    }

    fun playArtistRadio(artist: FeedArtist) {
        viewModelScope.launch {
            val seeds = try {
                innerTube.searchSongs("${artist.name} songs", limit = 5, prefetchStreams = false)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                emptyList()
            }
            val seed = seeds.firstOrNull {
                it.artist.contains(artist.name, ignoreCase = true) || artist.name.contains(it.artist, ignoreCase = true)
            } ?: seeds.firstOrNull()
            val related = seed?.let {
                try {
                    recommendationRepository.relatedFor(
                        seedVideoId = it.videoId,
                        title = it.title,
                        artist = it.artist,
                        limit = 25,
                        durationSeconds = it.durationSeconds,
                    )
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    emptyList()
                }
            }.orEmpty()
            val songs = (listOfNotNull(seed) + related).distinctBy(YouTubeMusicTrack::videoId)
            if (songs.isNotEmpty()) {
                playTracksQueue(songs, startIndex = 0, sourceLabel = "${artist.name} Radio")
            }
        }
    }

    fun playDiscoveryQuery(title: String, query: String) {
        if (_uiState.value.launchingRadio != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(launchingRadio = title) }
            try {
                // 1. Fetch real genre discovery tracks matching this exact genre from genresRepository
                val genreTracks = try {
                    genresRepository.discoverMore(query)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    emptyList()
                }

                if (genreTracks.isNotEmpty()) {
                    val playable = genreTracks.map { it.toPlayableTrack() }
                    musicPlayer.playQueue(
                        playable,
                        startIndex = 0,
                        sourceLabel = title,
                        enableRadioContinuation = true,
                    )
                } else {
                    // Fallback to exact search query for this genre
                    val searchResults = try {
                        innerTube.searchSongs("$query songs", limit = 25, prefetchStreams = false)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        emptyList()
                    }
                    if (searchResults.isNotEmpty()) {
                        playTracksQueue(searchResults, sourceLabel = title)
                    }
                }
            } finally {
                _uiState.update { it.copy(launchingRadio = null) }
            }
        }
    }

    fun playPlaylistSummary(summary: YouTubePlaylistSummary) {
        viewModelScope.launch {
            val playable = if (summary.id.startsWith("MPRE")) {
                runCatching {
                    innerTube.fetchAlbumPage(summary.id, summary.title, summary.author.orEmpty())
                }.getOrNull()?.tracks.orEmpty()
            } else {
                runCatching { innerTube.fetchPlaylist(summary.id, maxTracks = 100) }
                    .getOrNull()?.tracks.orEmpty().map { it.toPlayableTrack() }
            }
            if (playable.isNotEmpty()) {
                musicPlayer.playQueue(playable, startIndex = 0, sourceLabel = summary.title)
            }
        }
    }

    fun playCountryChart(chart: CountryChart) {
        if (chart.timeWindow == ChartTimeWindow.MONTHLY && chart.top.isNotEmpty()) {
            musicPlayer.playQueue(
                tracks = chart.top.map { it.toPlayableTrack() },
                startIndex = 0,
                sourceLabel = "${chart.timeWindow.label} · ${chart.playlist.title}",
            )
            return
        }
        viewModelScope.launch {
            val tracks = runCatching { innerTube.fetchPlaylist(chart.playlist.id, maxTracks = 100) }
                .getOrNull()?.tracks.orEmpty().ifEmpty { chart.top }
            if (tracks.isNotEmpty()) {
                musicPlayer.playQueue(
                    tracks = tracks.map { it.toPlayableTrack() },
                    startIndex = 0,
                    sourceLabel = chart.playlist.title,
                )
            }
        }
    }

    fun playPlaylistById(playlistId: String, title: String) {
        viewModelScope.launch {
            val result = runCatching { innerTube.fetchPlaylist(playlistId, maxTracks = 100) }
                .getOrNull()
            val tracks = result?.tracks.orEmpty()
            if (tracks.isNotEmpty()) {
                val playable = tracks.map { it.toPlayableTrack() }
                musicPlayer.playQueue(playable, startIndex = 0, sourceLabel = title)
            }
        }
    }

    fun openDiscoverMixPlaylist(tile: FeedQuickTile? = null, onOpen: (String) -> Unit) {
        viewModelScope.launch {
            var playlistId = tile?.playlistId
            if (playlistId.isNullOrBlank()) {
                val feed = _uiState.value.feedData
                val found = feed.ytSuggestedPlaylists.firstOrNull { it.title.contains("Discover Mix", ignoreCase = true) }
                    ?: feed.nativeShelves.flatMap { it.items }.filterIsInstance<com.lastwave.app.data.music.HomeShelfItem.Playlist>()
                        .map { it.playlist }.firstOrNull { it.title.contains("Discover Mix", ignoreCase = true) }
                playlistId = found?.id
            }
            if (playlistId.isNullOrBlank()) {
                val mixes = runCatching { innerTube.fetchHomeMixes() }.getOrDefault(emptyList())
                playlistId = mixes.firstOrNull { it.title.contains("Discover Mix", ignoreCase = true) }?.id
            }
            if (playlistId.isNullOrBlank()) {
                val search = runCatching { innerTube.searchPlaylists("Discover Mix") }.getOrDefault(emptyList())
                playlistId = search.firstOrNull { it.title.contains("Discover Mix", ignoreCase = true) }?.id
                    ?: search.firstOrNull()?.id
            }
            onOpen(playlistId?.ifBlank { "RDMPO" } ?: "RDMPO")
        }
    }

    fun playDiscoverMix(tile: FeedQuickTile? = null) {
        viewModelScope.launch {
            var playlistId = tile?.playlistId
            if (playlistId.isNullOrBlank()) {
                val feed = _uiState.value.feedData
                val found = feed.ytSuggestedPlaylists.firstOrNull { it.title.contains("Discover Mix", ignoreCase = true) }
                    ?: feed.nativeShelves.flatMap { it.items }.filterIsInstance<com.lastwave.app.data.music.HomeShelfItem.Playlist>()
                        .map { it.playlist }.firstOrNull { it.title.contains("Discover Mix", ignoreCase = true) }
                playlistId = found?.id
            }
            if (playlistId.isNullOrBlank()) {
                val mixes = runCatching { innerTube.fetchHomeMixes() }.getOrDefault(emptyList())
                playlistId = mixes.firstOrNull { it.title.contains("Discover Mix", ignoreCase = true) }?.id
            }
            if (playlistId.isNullOrBlank()) {
                val search = runCatching { innerTube.searchPlaylists("Discover Mix") }.getOrDefault(emptyList())
                playlistId = search.firstOrNull { it.title.contains("Discover Mix", ignoreCase = true) }?.id
                    ?: search.firstOrNull()?.id
            }
            if (!playlistId.isNullOrBlank()) {
                playPlaylistById(playlistId, "Discover Mix")
            } else {
                playInfiniteRadio()
            }
        }
    }

    fun playMyMix(tile: FeedQuickTile? = null) {
        viewModelScope.launch {
            val playlistId = tile?.playlistId
            val tracks = if (!playlistId.isNullOrBlank()) {
                runCatching { innerTube.fetchPlaylist(playlistId, maxTracks = 100) }.getOrNull()?.tracks.orEmpty()
            } else emptyList()
            val queue = tracks.ifEmpty {
                val recents = _uiState.value.feedData.ytRecentSongs
                val picks = _uiState.value.feedData.quickPicks
                val liked = _uiState.value.feedData.ytLikedSongs
                (recents + liked + picks).distinctBy { it.videoId }.shuffled()
            }
            if (queue.isNotEmpty()) {
                val playable = queue.map { it.toPlayableTrack() }
                musicPlayer.playQueue(playable, startIndex = 0, sourceLabel = "My Mix")
            }
        }
    }

    fun handleQuickTileClick(tile: FeedQuickTile) {
        val videoId = tile.actionVideoId ?: return
        musicPlayer.play(
            PlayableTrack(
                title = tile.title,
                artist = tile.subtitle ?: "",
                artworkUrl = tile.artworkUrl,
                videoId = videoId.takeIf(String::isNotBlank),
            ),
            sourceLabel = "Quick Picks",
        )
    }

    fun warmQuickPicks(tracks: List<YouTubeMusicTrack>) {
        musicPlayer.warmLikelyPlays(tracks.map { it.toPlayableTrack() })
    }

    private fun YouTubeMusicTrack.toPlayableTrack(): PlayableTrack = PlayableTrack(
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artworkUrl,
        videoId = videoId.takeIf(String::isNotBlank),
        durationMs = durationSeconds?.takeIf { it > 0 }?.times(1_000L),
    )

    private companion object {
        const val STALE_AFTER_MILLIS = 90_000L
    }
}
