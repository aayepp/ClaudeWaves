package com.lastwave.app.data.discover

import com.google.common.truth.Truth.assertThat
import com.lastwave.app.data.generate.GenerateRepository
import com.lastwave.app.data.generate.GeneratedTrack
import com.lastwave.app.data.generate.TasteProfile
import com.lastwave.app.data.generate.TasteProfileProvider
import com.lastwave.app.data.local.DISCOVER_REGION_AUTO
import com.lastwave.app.data.local.MiscSettings
import com.lastwave.app.data.local.SettingsPreferences
import com.lastwave.app.data.music.ChartTimeWindow
import com.lastwave.app.data.music.CountryChart
import com.lastwave.app.data.music.InnerTubeMusicApi
import com.lastwave.app.data.music.YouTubeMusicTrack
import com.lastwave.app.data.music.YouTubePlaylistSummary
import com.lastwave.app.data.recommend.RecommendationRepository
import com.lastwave.app.data.ytmusic.YtConnection
import com.lastwave.app.data.ytmusic.YtMusicAuthManager
import com.lastwave.app.ui.discover.buildDiscoverRegionChoices
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DiscoverChartRegionTest {

    private val generateRepository: GenerateRepository = mockk(relaxed = true)
    private val tasteProfileProvider: TasteProfileProvider = mockk(relaxed = true)
    private val innerTube: InnerTubeMusicApi = mockk(relaxed = true)
    private val ytAuth: YtMusicAuthManager = mockk(relaxed = true)
    private val recommendationRepository: RecommendationRepository = mockk(relaxed = true)
    private val settingsPreferences: SettingsPreferences = mockk(relaxed = true)

    private lateinit var settingsFlow: MutableStateFlow<MiscSettings>
    private lateinit var repository: DiscoverRepository
    private var fakeNowMs = 1_000_000L

    private fun emptyProfile(recentTracks: List<GeneratedTrack> = emptyList()) = TasteProfile(
        topArtistNames = emptySet(),
        recentArtists = emptySet(),
        topTags = emptySet(),
        topTrackKeys = emptySet(),
        recentTrackKeys = recentTracks.mapTo(mutableSetOf()) { it.key },
        topTracksRaw = emptyList(),
        recentTracksRaw = recentTracks,
        topArtistsRaw = emptyList(),
        builtAtMillis = 0L,
    )

    @Before
    fun setUp() {
        settingsFlow = MutableStateFlow(
            MiscSettings(
                homeChartCountries = listOf("MY", "ID", "ZZ"),
                discoverChartRegion = DISCOVER_REGION_AUTO,
            ),
        )
        every { settingsPreferences.settings } returns settingsFlow
        every { ytAuth.connection } returns MutableStateFlow(YtConnection())
        coEvery { tasteProfileProvider.get() } returns emptyProfile()
        coEvery { generateRepository.savedPlaylistTrackKeys() } returns emptySet()
        coEvery { generateRepository.filterRecommendationExclusions(any()) } answers { firstArg() }
        coEvery { generateRepository.preferPlaylistFreshness(any(), any(), any()) } answers {
            firstArg<List<GeneratedTrack>>().take(secondArg())
        }
        coEvery { generateRepository.fetchLocalFallbackMix(any()) } returns emptyList()
        coEvery { generateRepository.fetchSimilarTracks(any(), any(), any()) } returns emptyList()
        coEvery { generateRepository.fetchSimilarArtistTracks(any(), any()) } returns emptyList()
        coEvery { generateRepository.fetchTagTracks(any(), any()) } returns emptyList()

        repository = DiscoverRepository(
            generateRepository = generateRepository,
            tasteProfileProvider = tasteProfileProvider,
            innerTube = innerTube,
            ytAuth = ytAuth,
            recommendationRepository = recommendationRepository,
            settingsPreferences = settingsPreferences,
        ).apply {
            nowMs = { fakeNowMs }
        }
    }

    private fun chartFor(country: String, vararg titles: String): CountryChart = CountryChart(
        country = country,
        playlist = YouTubePlaylistSummary(id = "PL_$country", title = "$country Top"),
        top = titles.mapIndexed { index, title ->
            YouTubeMusicTrack(
                videoId = "${country}_$index",
                title = title,
                artist = "Artist $index",
                artworkUrl = "https://img/$country/$index.jpg",
            )
        },
    )

    @Test
    fun `explicit country chart works in guest mode and preserves exact chart rank order`() = runTest {
        settingsFlow.value = settingsFlow.value.copy(discoverChartRegion = "MY")
        val heardTrack = GeneratedTrack(name = "No 1 Hit", artist = "Artist 0", artworkUrl = null)
        coEvery { tasteProfileProvider.get() } returns emptyProfile(recentTracks = listOf(heardTrack))
        coEvery { innerTube.fetchCountryChart("MY", 50) } returns chartFor(
            "MY",
            "No 1 Hit",
            "No 2 Hit",
            "No 3 Hit",
        )

        val batch = repository.nextBatch(3)

        assertThat(batch.map { it.name }).containsExactly("No 1 Hit", "No 2 Hit", "No 3 Hit").inOrder()
        assertThat(repository.chartTrackCount).isEqualTo(3)
        assertThat(repository.currentRegion).isEqualTo("MY")
    }

    @Test
    fun `global ZZ chart fetches guest global hits and switches cleanly from country chart`() = runTest {
        settingsFlow.value = settingsFlow.value.copy(discoverChartRegion = "JP")
        coEvery { innerTube.fetchCountryChart("JP", 50) } returns chartFor("JP", "Tokyo Song")
        coEvery { innerTube.fetchCountryChart("ZZ", 50) } returns chartFor("ZZ", "World Anthem", "Global Hit")

        assertThat(repository.nextBatch(1).map { it.name }).containsExactly("Tokyo Song")

        settingsFlow.value = settingsFlow.value.copy(discoverChartRegion = "ZZ")
        val globalBatch = repository.nextBatch(2)

        assertThat(globalBatch.map { it.name }).containsExactly("World Anthem", "Global Hit").inOrder()
        assertThat(repository.currentRegion).isEqualTo("ZZ")
        assertThat(repository.chartTrackCount).isEqualTo(2)
    }

    @Test
    fun `auto For You mode pulls live home chart even when signed out`() = runTest {
        settingsFlow.value = MiscSettings(
            homeChartCountries = listOf("KR", "ZZ"),
            discoverChartRegion = DISCOVER_REGION_AUTO,
        )
        coEvery { innerTube.fetchCountryChart("KR", 50) } returns chartFor("KR", "Seoul Chart #1", "Seoul Chart #2")

        val batch = repository.nextBatch(2)

        assertThat(batch.map { it.name }).containsExactly("Seoul Chart #1", "Seoul Chart #2")
    }

    @Test
    fun `cache goes stale after 15 minutes and refreshBatch loads updated tracks`() = runTest {
        settingsFlow.value = settingsFlow.value.copy(discoverChartRegion = "MY")
        coEvery { innerTube.fetchCountryChart("MY", 50) } returns chartFor("MY", "Morning #1")

        repository.nextBatch(1)
        assertThat(repository.isStale("MY")).isFalse()

        fakeNowMs += 5 * 60_000L
        assertThat(repository.isStale("MY")).isFalse()

        fakeNowMs += 11 * 60_000L
        assertThat(repository.isStale("MY")).isTrue()

        coEvery { innerTube.fetchCountryChart("MY", 50) } returns chartFor("MY", "Evening #1", "Evening #2")
        val refreshed = repository.refreshBatch(2, "MY")

        assertThat(refreshed.map { it.name }).containsExactly("Evening #1", "Evening #2").inOrder()
        assertThat(repository.isStale("MY")).isFalse()
    }

    @Test
    fun `buildDiscoverRegionChoices prioritizes Auto, user home countries, and Global without duplicates`() {
        val choices = buildDiscoverRegionChoices(listOf("JP", "KR"))

        assertThat(choices.take(4)).containsExactly(DISCOVER_REGION_AUTO, "JP", "KR", "ZZ").inOrder()
        assertThat(choices).containsNoDuplicates()
    }

    @Test
    fun `switching chartTimeWindow invalidates cache and loads weekly and monthly charts`() = runTest {
        settingsFlow.value = settingsFlow.value.copy(
            discoverChartRegion = "MY",
            chartTimeWindow = ChartTimeWindow.VIRAL,
        )
        coEvery { innerTube.fetchCountryChart("MY", 50) } returns chartFor("MY", "Viral Hit #1")
        coEvery { innerTube.fetchCountryChart("MY", 50, ChartTimeWindow.WEEKLY) } returns chartFor("MY", "Weekly Hit #1")
        coEvery { innerTube.fetchCountryChart("MY", 50, ChartTimeWindow.MONTHLY) } returns chartFor("MY", "Monthly Hit #1")

        assertThat(repository.nextBatch(1).map { it.name }).containsExactly("Viral Hit #1")
        assertThat(repository.isStale("MY", ChartTimeWindow.WEEKLY)).isTrue()

        settingsFlow.value = settingsFlow.value.copy(chartTimeWindow = ChartTimeWindow.WEEKLY)
        assertThat(repository.nextBatch(1).map { it.name }).containsExactly("Weekly Hit #1")
        assertThat(repository.currentTimeWindow).isEqualTo(ChartTimeWindow.WEEKLY)
        assertThat(repository.isStale("MY", ChartTimeWindow.WEEKLY)).isFalse()

        settingsFlow.value = settingsFlow.value.copy(chartTimeWindow = ChartTimeWindow.MONTHLY)
        assertThat(repository.nextBatch(1).map { it.name }).containsExactly("Monthly Hit #1")
        assertThat(repository.currentTimeWindow).isEqualTo(ChartTimeWindow.MONTHLY)
    }
}
