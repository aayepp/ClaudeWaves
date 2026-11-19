package com.lastwave.app.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastwave.app.data.artwork.ArtworkRepository
import com.lastwave.app.data.discover.DiscoverRepository
import com.lastwave.app.data.generate.GeneratedTrack
import com.lastwave.app.data.local.DEFAULT_HOME_CHART_COUNTRIES
import com.lastwave.app.data.local.DISCOVER_REGION_AUTO
import com.lastwave.app.data.local.SettingsPreferences
import com.lastwave.app.data.music.CHART_COUNTRY_CHOICES
import com.lastwave.app.data.music.ChartTimeWindow
import com.lastwave.app.data.naming.PlaylistNamer
import com.lastwave.app.data.playlist.PlaylistRepository
import com.lastwave.app.data.ytmusic.YtMusicAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.compose.runtime.Immutable

@Immutable
data class DiscoverUiState(
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val tracks: List<GeneratedTrack> = emptyList(),
    val error: String? = null,
    val isRefreshing: Boolean = false,
    val saveResultMessage: String? = null,
    val isYtConnected: Boolean = false,
    val endReached: Boolean = false,
    val selectedRegion: String = DISCOVER_REGION_AUTO,
    val chartTimeWindow: ChartTimeWindow = ChartTimeWindow.VIRAL,
    val regionChoices: List<String> = buildDiscoverRegionChoices(DEFAULT_HOME_CHART_COUNTRIES),
    val chartTrackCount: Int = 0,
)

internal fun buildDiscoverRegionChoices(homeChartCountries: List<String>): List<String> = buildList {
    add(DISCOVER_REGION_AUTO)
    addAll(homeChartCountries.filter { it.isNotBlank() })
    add("ZZ")
    addAll(CHART_COUNTRY_CHOICES)
}.distinct()

/** Faithful port of discover.js (§7): infinite-scroll feed, pull-to-
 *  refresh, Surprise Me, and Save As Playlist (order-preserving snapshot
 *  of exactly what's currently rendered, with duplicate-signature
 *  detection). */
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: DiscoverRepository,
    private val playlistRepository: PlaylistRepository,
    private val artworkRepository: ArtworkRepository,
    private val ytMusicAuth: YtMusicAuthManager,
    private val settingsPreferences: SettingsPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            ytMusicAuth.connection.collect { conn ->
                _uiState.update { it.copy(isYtConnected = conn.isConnected) }
            }
        }
        viewModelScope.launch {
            settingsPreferences.settings
                .map { it.homeChartCountries to it.chartTimeWindow }
                .distinctUntilChanged()
                .collect { (countries, window) ->
                    _uiState.update {
                        it.copy(
                            regionChoices = buildDiscoverRegionChoices(countries),
                            chartTimeWindow = window,
                        )
                    }
                }
        }
        viewModelScope.launch {
            repository.feed.collect { feed ->
                val state = _uiState.value
                if (feed.isNotEmpty() || (!state.isLoading && !state.isRefreshing && state.tracks.isNotEmpty())) {
                    _uiState.update {
                        it.copy(
                            isLoading = if (feed.isNotEmpty()) false else it.isLoading,
                            tracks = feed,
                            chartTrackCount = repository.chartTrackCount,
                        )
                    }
                }
            }
        }
        loadInitial()
    }

    fun loadInitial() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val settings = runCatching { settingsPreferences.settings.first() }.getOrNull()
            val region = settings?.discoverChartRegion?.takeIf { it.isNotBlank() } ?: DISCOVER_REGION_AUTO
            val window = settings?.chartTimeWindow ?: ChartTimeWindow.VIRAL
            val choices = buildDiscoverRegionChoices(settings?.homeChartCountries ?: DEFAULT_HOME_CHART_COUNTRIES)
            val staleOrChanged = repository.isStale(region, window)
            val cached = if (repository.currentRegion == region && repository.currentTimeWindow == window) {
                repository.allowedCachedFeed()
            } else emptyList()

            if (cached.isNotEmpty() && !staleOrChanged) {
                _uiState.update {
                    it.copy(
                        selectedRegion = region,
                        chartTimeWindow = window,
                        regionChoices = choices,
                        isLoading = false,
                        error = null,
                        tracks = cached,
                        chartTrackCount = repository.chartTrackCount,
                    )
                }
            } else if (cached.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        selectedRegion = region,
                        chartTimeWindow = window,
                        regionChoices = choices,
                        isLoading = false,
                        isRefreshing = true,
                        error = null,
                        tracks = cached,
                        chartTrackCount = repository.chartTrackCount,
                    )
                }
                try {
                    val batch = repository.refreshBatch(INITIAL_BATCH_SIZE, region, window)
                    val feed = repository.getCachedFeed()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            tracks = feed,
                            chartTrackCount = repository.chartTrackCount,
                        )
                    }
                    artworkRepository.enrichBatch(batch.take(INITIAL_BATCH_SIZE).map { it.name to it.artist })
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
                return@launch
            } else {
                _uiState.update {
                    it.copy(
                        selectedRegion = region,
                        chartTimeWindow = window,
                        regionChoices = choices,
                        isLoading = true,
                        isRefreshing = false,
                        error = null,
                        endReached = false,
                    )
                }
            }

            try {
                val needed = (INITIAL_BATCH_SIZE - cached.size).coerceAtLeast(0)
                val batch = if (needed > 0) repository.nextBatch(needed) else emptyList()
                val feed = repository.getCachedFeed()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        tracks = feed,
                        chartTrackCount = repository.chartTrackCount,
                    )
                }
                artworkRepository.enrichBatch((cached + batch).take(INITIAL_BATCH_SIZE).map { it.name to it.artist })
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                if (_uiState.value.tracks.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = e.message ?: "Couldn't load recommendations",
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                }
            }
        }
    }

    fun selectRegion(region: String) {
        val window = _uiState.value.chartTimeWindow
        if (region == _uiState.value.selectedRegion &&
            _uiState.value.tracks.isNotEmpty() &&
            !repository.isStale(region, window)
        ) return
        reloadChartSelection(region, window, persistRegion = true, persistWindow = false)
    }

    fun selectTimeWindow(window: ChartTimeWindow) {
        val region = _uiState.value.selectedRegion
        if (window == _uiState.value.chartTimeWindow &&
            _uiState.value.tracks.isNotEmpty() &&
            !repository.isStale(region, window)
        ) return
        reloadChartSelection(region, window, persistRegion = false, persistWindow = true)
    }

    private fun reloadChartSelection(
        region: String,
        window: ChartTimeWindow,
        persistRegion: Boolean,
        persistWindow: Boolean,
    ) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedRegion = region,
                    chartTimeWindow = window,
                    isLoading = true,
                    isRefreshing = false,
                    isLoadingMore = false,
                    tracks = emptyList(),
                    chartTrackCount = 0,
                    error = null,
                    endReached = false,
                )
            }
            try {
                if (persistRegion) settingsPreferences.setDiscoverChartRegion(region)
                if (persistWindow) settingsPreferences.setChartTimeWindow(window)
                repository.resetForChart(region, window)
                val batch = repository.nextBatch(INITIAL_BATCH_SIZE)
                val feed = repository.getCachedFeed()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tracks = feed,
                        chartTrackCount = repository.chartTrackCount,
                    )
                }
                artworkRepository.enrichBatch(batch.map { it.name to it.artist })
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Couldn't load chart") }
            }
        }
    }

    fun refreshIfStale() {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing) return
        if (repository.isStale(state.selectedRegion, state.chartTimeWindow)) {
            refresh()
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoading || _uiState.value.isRefreshing || _uiState.value.isLoadingMore || _uiState.value.endReached) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val more = repository.nextBatch(PAGE_SIZE)
                // An empty batch means the pool is exhausted — stop
                // auto-paging so sitting at the bottom doesn't re-trigger
                // loadMore on every size change in an endless network storm.
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        endReached = more.isEmpty(),
                        tracks = repository.getCachedFeed(),
                        chartTrackCount = repository.chartTrackCount,
                    )
                }
                artworkRepository.enrichBatch(more.map { it.name to it.artist })
            } catch (cancellation: CancellationException) {
                _uiState.update { it.copy(isLoadingMore = false) }
                throw cancellation
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun refresh() {
        loadJob?.cancel()
        val region = _uiState.value.selectedRegion
        val window = _uiState.value.chartTimeWindow
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null, endReached = false) }
            try {
                val batch = repository.refreshBatch(INITIAL_BATCH_SIZE, region, window)
                val feed = repository.getCachedFeed()
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        tracks = feed,
                        chartTrackCount = repository.chartTrackCount,
                    )
                }
                artworkRepository.enrichBatch(batch.map { it.name to it.artist })
            } catch (cancellation: CancellationException) {
                _uiState.update { it.copy(isRefreshing = false) }
                throw cancellation
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false, error = if (it.tracks.isEmpty()) e.message else null) }
            }
        }
    }

    fun surpriseMe() {
        loadJob?.cancel()
        val region = _uiState.value.selectedRegion
        val window = _uiState.value.chartTimeWindow
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isRefreshing = false, error = null, endReached = false) }
            try {
                repository.resetForChart(region, window)
                val batch = repository.nextBatch(INITIAL_BATCH_SIZE)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        tracks = repository.getCachedFeed(),
                        chartTrackCount = repository.chartTrackCount,
                    )
                }
                artworkRepository.enrichBatch(batch.map { it.name to it.artist })
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    /** §7.3 Save As Playlist: snapshots exactly what's currently rendered,
     *  no re-fetch. Order-preserving duplicate-signature guard. */
    fun saveAsPlaylist() {
        val tracks = _uiState.value.tracks
        if (tracks.isEmpty()) return
        viewModelScope.launch {
            // Every step here reads the playlist table. A failed read must
            // surface as "try again" — save() aborts rather than lose its
            // dedupe check, which used to stack a same-title sibling.
            try {
                val signature = playlistRepository.discoverSignature(tracks)
                val existing = playlistRepository.findByDiscoverSignature(signature)
                if (existing != null) {
                    _uiState.update { it.copy(saveResultMessage = "Already saved as \"${existing.title}\"") }
                    return@launch
                }
                val title = PlaylistNamer.generateUniqueName(playlistRepository.titles())
                val subtitle = PlaylistNamer.subtitleFor("discover")
                playlistRepository.save(title, subtitle, "discover", tracks, discoverSignature = signature)
                _uiState.update { it.copy(saveResultMessage = "Saved as \"$title\"") }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                _uiState.update { it.copy(saveResultMessage = "Couldn't save playlist. Try again.") }
            }
        }
    }

    fun dismissSaveResult() = _uiState.update { it.copy(saveResultMessage = null) }
    fun dismissError() = _uiState.update { it.copy(error = null) }

    private companion object {
        const val INITIAL_BATCH_SIZE = 16
        const val PAGE_SIZE = 12
    }
}
