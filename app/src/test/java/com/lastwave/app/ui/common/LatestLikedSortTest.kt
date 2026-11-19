package com.lastwave.app.ui.common

import com.lastwave.app.data.generate.GeneratedTrack
import com.lastwave.app.data.generate.fillMissingAddedAtFrom
import com.lastwave.app.data.generate.mergePreservingAddedAt
import com.lastwave.app.ui.playlist.PlaylistTrackSort
import com.lastwave.app.ui.playlist.playlistTrackDisplayIndices
import org.junit.Assert.assertEquals
import org.junit.Test

class LatestLikedSortTest {

    private fun track(
        name: String,
        artist: String = "Artist",
        addedAtMillis: Long? = null,
        url: String = "https://music.youtube.com/watch?v=$name",
    ) = GeneratedTrack(
        name = name,
        artist = artist,
        url = url,
        addedAtMillis = addedAtMillis,
    )

    @Test
    fun latestLikedSortsTimestampedNewestFirstAboveLegacyTracks() {
        val legacyOlder = track("Legacy Older", addedAtMillis = null)
        val legacyNewer = track("Legacy Newer", addedAtMillis = null)
        val spotifyMid = track("Spotify Mid", addedAtMillis = 2_000L)
        val spotifyOld = track("Spotify Old", addedAtMillis = 1_000L)
        val tappedNow = track("Heart Tapped", addedAtMillis = 5_000L)

        val tracks = listOf(legacyOlder, legacyNewer, spotifyOld, spotifyMid, tappedNow)

        val newestFirstOrder = tracks.sortedForDisplay(
            sort = PlaylistTrackSort.LATEST_LIKED,
            ascending = false,
            title = { it.name },
            artist = { it.artist },
            durationMs = { it.durationMs },
            newestFirst = false,
            addedAtMillis = { it.addedAtMillis },
        ).map { it.name }

        assertEquals(
            listOf("Heart Tapped", "Spotify Mid", "Spotify Old", "Legacy Newer", "Legacy Older"),
            newestFirstOrder,
        )

        val oldestFirstOrder = tracks.sortedForDisplay(
            sort = PlaylistTrackSort.LATEST_LIKED,
            ascending = true,
            title = { it.name },
            artist = { it.artist },
            durationMs = { it.durationMs },
            newestFirst = false,
            addedAtMillis = { it.addedAtMillis },
        ).map { it.name }

        assertEquals(newestFirstOrder.reversed(), oldestFirstOrder)
    }

    @Test
    fun latestLikedWithoutAnyTimestampsDegradesGracefullyToDateAddedOrder() {
        val tracks = listOf(
            track("YT Newest Arrived", addedAtMillis = null),
            track("YT Middle Arrived", addedAtMillis = null),
            track("YT Oldest Arrived", addedAtMillis = null),
        )

        val result = tracks.sortedForDisplay(
            sort = PlaylistTrackSort.LATEST_LIKED,
            ascending = false,
            title = { it.name },
            artist = { it.artist },
            durationMs = { it.durationMs },
            newestFirst = true,
            addedAtMillis = { it.addedAtMillis },
        ).map { it.name }

        assertEquals(
            listOf("YT Newest Arrived", "YT Middle Arrived", "YT Oldest Arrived"),
            result,
        )
    }

    @Test
    fun playlistTrackDisplayIndicesAppliesLatestLikedWithSearchFilter() {
        val tracks = listOf(
            track("First Liked", artist = "Coldplay", addedAtMillis = 100L),
            track("Skip Me", artist = "Other", addedAtMillis = 300L),
            track("Latest Match", artist = "Coldplay", addedAtMillis = 400L),
        )

        val indices = playlistTrackDisplayIndices(
            tracks = tracks,
            folderTracks = tracks,
            sort = PlaylistTrackSort.LATEST_LIKED,
            ascending = false,
            query = "cold",
        )

        assertEquals(listOf(2, 0), indices)
    }

    @Test
    fun mergePreservingAddedAtBackfillsNullTimestampsAndKeepsExisting() {
        val local = listOf(
            track("Song A", artist = "Artist A", addedAtMillis = 9_000L),
            track("Song B", artist = "Artist B", addedAtMillis = null),
        )
        val remote = listOf(
            track("Song A", artist = "Artist A", addedAtMillis = 1_000L),
            track("Song B", artist = "Artist B", addedAtMillis = 4_000L),
            track("Song C", artist = "Artist C", addedAtMillis = 6_000L),
        )

        val backfilled = local.fillMissingAddedAtFrom(remote)
        assertEquals(9_000L, backfilled[0].addedAtMillis)
        assertEquals(4_000L, backfilled[1].addedAtMillis)

        val merged = local.mergePreservingAddedAt(remote)
        assertEquals(3, merged.size)
        assertEquals(9_000L, merged.first { it.name == "Song A" }.addedAtMillis)
        assertEquals(4_000L, merged.first { it.name == "Song B" }.addedAtMillis)
        assertEquals(6_000L, merged.first { it.name == "Song C" }.addedAtMillis)
    }
}
