package com.lastwave.app.ui.feed

import com.google.common.truth.Truth.assertThat
import com.lastwave.app.data.generate.GeneratedTrack
import com.lastwave.app.data.local.db.SongPlayStatsEntity
import com.lastwave.app.data.model.ArtistRef
import com.lastwave.app.data.model.ImageDto
import com.lastwave.app.data.model.RecentTrack
import com.lastwave.app.data.music.YouTubeMusicTrack
import com.lastwave.app.playback.MediaType
import com.lastwave.app.playback.PlayableTrack
import org.junit.Test

class ContinueListeningTest {

    @Test
    fun mergesSourcesInPriorityOrderAndRespectsLimit() {
        val result = buildContinueListeningTracks(
            currentTrack = PlayableTrack(title = "Current Song", artist = "Artist A", videoId = "v1"),
            recentPlayedStats = listOf(
                SongPlayStatsEntity(trackKey = "local|artist b", title = "Local Song", artist = "Artist B", videoId = "v2"),
            ),
            ytRecentSongs = listOf(
                YouTubeMusicTrack(videoId = "v3", title = "YT Recent", artist = "Artist C"),
            ),
            jumpBackIn = listOf(
                RecentTrack(
                    name = "Scrobble Track",
                    artist = ArtistRef(name = "Artist D"),
                    image = listOf(ImageDto(url = "https://img.example/d.jpg", size = "large")),
                ),
            ),
            discoveryTracks = listOf(
                GeneratedTrack(name = "Discovery Track", artist = "Artist E", durationMs = 210_000L),
            ),
            recommendedTracks = listOf(
                YouTubeMusicTrack(videoId = "v6", title = "Recommended Track", artist = "Artist F"),
                YouTubeMusicTrack(videoId = "v7", title = "Overflow Track", artist = "Artist G"),
            ),
            limit = 6,
        )

        assertThat(result.map { it.title }).containsExactly(
            "Current Song",
            "Local Song",
            "YT Recent",
            "Scrobble Track",
            "Discovery Track",
            "Recommended Track",
        ).inOrder()
    }

    @Test
    fun deduplicatesByVideoIdAndNormalizedPrimaryArtistTitle() {
        val result = buildContinueListeningTracks(
            currentTrack = PlayableTrack(title = "Midnight City", artist = "M83 ft. Guest", videoId = "vid_city"),
            recentPlayedStats = listOf(
                // Same videoId under slightly different metadata must be dropped.
                SongPlayStatsEntity(trackKey = "other|m83", title = "Other Label", artist = "M83", videoId = "vid_city"),
                SongPlayStatsEntity(trackKey = "unique|alpha", title = "Unique Local", artist = "Alpha", videoId = "vid_local"),
            ),
            ytRecentSongs = listOf(
                // Different videoId but same normalized title + primary artist ("m83").
                YouTubeMusicTrack(videoId = "vid_city_alt", title = " midnight city ", artist = "m83"),
                YouTubeMusicTrack(videoId = "vid_yt", title = "Starlight", artist = "Muse"),
            ),
            jumpBackIn = listOf(
                RecentTrack(name = "starlight", artist = ArtistRef(name = "Muse feat. Live")),
            ),
            discoveryTracks = listOf(
                GeneratedTrack(name = "Unique Local", artist = "Alpha"),
            ),
            recommendedTracks = listOf(
                YouTubeMusicTrack(videoId = "vid_local", title = "Duplicate Video", artist = "Beta"),
            ),
        )

        assertThat(result.map { it.title }).containsExactly(
            "Midnight City",
            "Unique Local",
            "Starlight",
        ).inOrder()
    }

    @Test
    fun filtersBlankTitlesAndPodcastEpisodesAcrossSources() {
        val result = buildContinueListeningTracks(
            currentTrack = PlayableTrack(
                title = "Tech Talk Ep 1",
                artist = "Host",
                videoId = "pod_1",
                mediaType = MediaType.PODCAST_EPISODE,
            ),
            recentPlayedStats = listOf(
                SongPlayStatsEntity(trackKey = "blank", title = "   ", artist = "Nobody"),
                SongPlayStatsEntity(trackKey = "pod", title = "Daily News", artist = "Podcast", videoId = "pod_2"),
                SongPlayStatsEntity(trackKey = "song", title = "Real Song", artist = "Real Artist", videoId = "real_1"),
            ),
            ytRecentSongs = listOf(
                YouTubeMusicTrack(videoId = "pod_3", title = "Show Episode", artist = "Podcast"),
            ),
            jumpBackIn = listOf(
                RecentTrack(name = "", artist = ArtistRef(name = "Ghost")),
                RecentTrack(name = "Lastfm Ep", artist = ArtistRef(name = "Podcast")),
            ),
            discoveryTracks = listOf(
                GeneratedTrack(name = "   ", artist = "Blank Discovery"),
            ),
            recommendedTracks = listOf(
                YouTubeMusicTrack(videoId = "real_2", title = "Another Song", artist = "Real Artist 2"),
            ),
        )

        assertThat(result.map { it.title }).containsExactly(
            "Real Song",
            "Another Song",
        ).inOrder()

        // A track carrying a showId must also be rejected even if mediaType was SONG.
        val withShowId = buildContinueListeningTracks(
            currentTrack = PlayableTrack(title = "Episode", artist = "Host", showId = "MPSPPL123"),
            recentPlayedStats = emptyList(),
            ytRecentSongs = emptyList(),
            jumpBackIn = emptyList(),
            discoveryTracks = emptyList(),
            recommendedTracks = emptyList(),
        )
        assertThat(withShowId).isEmpty()
    }
}
