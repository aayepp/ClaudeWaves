package com.lastwave.app.data.music

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.Test

class ChartSelectionAndMonthlyMergeTest {

    private fun newApi(): InnerTubeMusicApi = InnerTubeMusicApi(
        http = mockk(relaxed = true),
        streamExtractor = mockk(relaxed = true),
        innerTubeXExtractor = mockk(relaxed = true),
        ytAuth = mockk(relaxed = true),
        settingsPreferences = mockk(relaxed = true),
        appContext = mockk(relaxed = true),
    )

    private fun track(id: String, title: String = "Track $id") = YouTubeMusicTrack(
        videoId = id,
        title = title,
        artist = "Artist $id",
    )

    @Test
    fun parsePlaylistRenderersRespectsAllowedPrefixesForChartsAndReleases() {
        val api = newApi()
        val sample = Json.parseToJsonElement(
            """
            {
              "items": [
                {
                  "musicTwoRowItemRenderer": {
                    "title": { "runs": [{ "text": "Trending 20 MY" }] },
                    "navigationEndpoint": { "browseEndpoint": { "browseId": "VLOLAK5uy_abc123" } }
                  }
                },
                {
                  "musicTwoRowItemRenderer": {
                    "title": { "runs": [{ "text": "Fresh Album" }] },
                    "navigationEndpoint": { "browseEndpoint": { "browseId": "MPREb_xyz789" } }
                  }
                },
                {
                  "musicTwoRowItemRenderer": {
                    "title": { "runs": [{ "text": "Top 100 Malaysia" }] },
                    "navigationEndpoint": { "browseEndpoint": { "browseId": "VLPL4fGSI1pDJn" } }
                  }
                }
              ]
            }
            """.trimIndent(),
        )

        assertThat(api.parsePlaylistRenderers(sample).map { it.id })
            .containsExactly("PL4fGSI1pDJn")
        assertThat(api.parsePlaylistRenderers(sample, CHART_PLAYLIST_PREFIXES).map { it.id })
            .containsExactly("OLAK5uy_abc123", "PL4fGSI1pDJn").inOrder()
        assertThat(api.parsePlaylistRenderers(sample, RELEASE_PLAYLIST_PREFIXES).map { it.id })
            .containsExactly("OLAK5uy_abc123", "MPREb_xyz789", "PL4fGSI1pDJn").inOrder()
    }

    @Test
    fun selectChartPlaylistsSkipsLivePerformancesAndPicksCorrectWindows() {
        val korea = listOf(
            YouTubePlaylistSummary("PL_live", "Top 100 Live Performances - South Korea"),
            YouTubePlaylistSummary("PL_daily", "Daily Top Music Videos - South Korea"),
            YouTubePlaylistSummary("PL_weekly", "Top 100 Music Videos South Korea"),
            YouTubePlaylistSummary("OLAK5uy_trend", "Trending 20 South Korea"),
        )

        val (viralPrimary, viralSecondary) = selectChartPlaylists(korea, ChartTimeWindow.VIRAL)!!
        assertThat(viralPrimary.id).isEqualTo("OLAK5uy_trend")
        assertThat(viralSecondary).isNull()

        val (weeklyPrimary, weeklySecondary) = selectChartPlaylists(korea, ChartTimeWindow.WEEKLY)!!
        assertThat(weeklyPrimary.id).isEqualTo("PL_weekly")
        assertThat(weeklySecondary).isNull()

        val (monthlyPrimary, monthlySecondary) = selectChartPlaylists(korea, ChartTimeWindow.MONTHLY)!!
        assertThat(monthlyPrimary.id).isEqualTo("PL_weekly")
        assertThat(monthlySecondary?.id).isEqualTo("PL_daily")
    }

    @Test
    fun selectChartPlaylistsHandlesLocalizedThreePlListAndGlobalFallback() {
        val localizedKr = listOf(
            YouTubePlaylistSummary("PL_1", "인기 라이브 TOP 100"),
            YouTubePlaylistSummary("PL_2", "일간 인기 뮤직 비디오"),
            YouTubePlaylistSummary("PL_3", "주간 TOP 100"),
        )
        assertThat(selectChartPlaylists(localizedKr, ChartTimeWindow.VIRAL)?.first?.id)
            .isEqualTo("PL_2")
        assertThat(selectChartPlaylists(localizedKr, ChartTimeWindow.WEEKLY)?.first?.id)
            .isEqualTo("PL_3")

        val global = listOf(
            YouTubePlaylistSummary("PL_daily_zz", "Daily Top Music Videos - Global"),
            YouTubePlaylistSummary("PL_weekly_zz", "Top 100 Music Videos Global"),
        )
        assertThat(selectChartPlaylists(global, ChartTimeWindow.VIRAL)?.first?.id)
            .isEqualTo("PL_daily_zz")
        val (monthlyPrimary, monthlySecondary) = selectChartPlaylists(global, ChartTimeWindow.MONTHLY)!!
        assertThat(monthlyPrimary.id).isEqualTo("PL_weekly_zz")
        assertThat(monthlySecondary?.id).isEqualTo("PL_daily_zz")
    }

    @Test
    fun mergeMonthlyChartTracksPromotesSustainedConsensusOverOneDaySpikes() {
        val weekly = listOf(track("spike"), track("steady_a"), track("steady_b"))
        val daily = listOf(track("viral_only"), track("steady_b"), track("steady_a"))

        val merged = mergeMonthlyChartTracks(weekly, daily, top = 4)

        assertThat(merged.map { it.videoId })
            .containsExactly("steady_a", "steady_b", "spike", "viral_only")
            .inOrder()
    }
}
