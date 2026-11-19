package com.lastwave.app.ui.home

import com.google.common.truth.Truth.assertThat
import com.lastwave.app.data.repository.HomeArtistItem
import com.lastwave.app.data.repository.HomeStats
import com.lastwave.app.data.repository.HomeTrack
import org.junit.Test

class RecapCardDataTest {

    @Test
    fun buildRecapCardData_aggregatesPeriodsArtistsAndListeningTime() {
        val t1 = HomeTrack("Starboy", "The Weeknd", "https://art/1", null, playCount = 12)
        val t2 = HomeTrack("Die For You", "The Weeknd", null, null, playCount = 8)
        val t3 = HomeTrack("Instant Crush", "Daft Punk", "https://art/3", null, playCount = 10)
        val overallTrack = HomeTrack("Midnight City", "M83", "https://art/overall", null, playCount = 50)

        val state = HomeUiState(
            username = "ayep",
            stats = HomeStats(scrobbles = 400, trackCount = 120, artistCount = 45, albumCount = 30),
            topTracks7Days = listOf(t1, t2, t3),
            topTracksOverall = listOf(overallTrack),
            topArtists = listOf(HomeArtistItem("M83", playCount = 90L)),
        )

        val sevenDay = buildRecapCardData(state, RecapPeriod.LAST_7_DAYS)
        assertThat(sevenDay.displayName).isEqualTo("ayep")
        assertThat(sevenDay.totalPlays).isEqualTo(30L)
        assertThat(sevenDay.estimatedMinutes).isEqualTo((30L * 210L) / 60L)
        assertThat(sevenDay.topArtists.first().name).isEqualTo("The Weeknd")
        assertThat(sevenDay.topArtists.first().playCount).isEqualTo(20L)
        assertThat(sevenDay.heroArtworkUrl).isEqualTo("https://art/1")

        val allTime = buildRecapCardData(state, RecapPeriod.ALL_TIME)
        assertThat(allTime.totalPlays).isEqualTo(400L)
        assertThat(allTime.topTracks).containsExactly(overallTrack)
        assertThat(allTime.topArtists.first().name).isEqualTo("M83")
    }

    @Test
    fun formatListeningMinutes_formatsReadableDurations() {
        assertThat(formatListeningMinutes(0)).isEqualTo("0 min")
        assertThat(formatListeningMinutes(42)).isEqualTo("42m")
        assertThat(formatListeningMinutes(135)).isEqualTo("2h 15m")
        assertThat(formatListeningMinutes(6120)).isEqualTo("102h")
    }
}
