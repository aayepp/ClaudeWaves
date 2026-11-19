package com.lastwave.app.ui.common

import com.google.common.truth.Truth.assertThat
import com.lastwave.app.ui.playlist.PlaylistTrackSort
import org.junit.Test

class TrackSortTest {

    private data class Row(
        val title: String,
        val artist: String,
        val durationMs: Long?,
    )

    private val appendRows = listOf(
        Row(title = "Oldest", artist = "Charlie", durationMs = 240_000L),
        Row(title = "Middle", artist = "alpha", durationMs = null),
        Row(title = "Newest", artist = "Bravo", durationMs = 120_000L),
    )

    @Test
    fun dateAddedRespectsAppendOrderedSources() {
        val oldestFirst = appendRows.sortedForDisplay(
            sort = PlaylistTrackSort.DATE_ADDED,
            ascending = true,
            title = { it.title },
            artist = { it.artist },
            durationMs = { it.durationMs },
            newestFirst = false,
        )
        val newestFirst = appendRows.sortedForDisplay(
            sort = PlaylistTrackSort.DATE_ADDED,
            ascending = false,
            title = { it.title },
            artist = { it.artist },
            durationMs = { it.durationMs },
            newestFirst = false,
        )

        assertThat(oldestFirst.map { it.title }).containsExactly("Oldest", "Middle", "Newest").inOrder()
        assertThat(newestFirst.map { it.title }).containsExactly("Newest", "Middle", "Oldest").inOrder()
    }

    @Test
    fun dateAddedRespectsNewestFirstRemoteSources() {
        val remoteNewestFirstRows = listOf(
            Row(title = "Newest", artist = "Bravo", durationMs = 120_000L),
            Row(title = "Middle", artist = "alpha", durationMs = null),
            Row(title = "Oldest", artist = "Charlie", durationMs = 240_000L),
        )

        val newestFirst = remoteNewestFirstRows.sortedForDisplay(
            sort = PlaylistTrackSort.DATE_ADDED,
            ascending = false,
            title = { it.title },
            artist = { it.artist },
            durationMs = { it.durationMs },
            newestFirst = true,
        )
        val oldestFirst = remoteNewestFirstRows.sortedForDisplay(
            sort = PlaylistTrackSort.DATE_ADDED,
            ascending = true,
            title = { it.title },
            artist = { it.artist },
            durationMs = { it.durationMs },
            newestFirst = true,
        )

        assertThat(newestFirst.map { it.title }).containsExactly("Newest", "Middle", "Oldest").inOrder()
        assertThat(oldestFirst.map { it.title }).containsExactly("Oldest", "Middle", "Newest").inOrder()
    }

    @Test
    fun nameArtistAndPlayTimeSortDeterministicallyWithNullsLast() {
        val byArtistAsc = appendRows.sortedForDisplay(
            sort = PlaylistTrackSort.ARTIST,
            ascending = true,
            title = { it.title },
            artist = { it.artist },
            durationMs = { it.durationMs },
        )
        assertThat(byArtistAsc.map { it.title }).containsExactly("Middle", "Newest", "Oldest").inOrder()

        val byNameDesc = appendRows.sortedForDisplay(
            sort = PlaylistTrackSort.NAME,
            ascending = false,
            title = { it.title },
            artist = { it.artist },
            durationMs = { it.durationMs },
        )
        assertThat(byNameDesc.map { it.title }).containsExactly("Oldest", "Newest", "Middle").inOrder()

        val byPlayTimeAsc = appendRows.sortedForDisplay(
            sort = PlaylistTrackSort.PLAY_TIME,
            ascending = true,
            title = { it.title },
            artist = { it.artist },
            durationMs = { it.durationMs },
        )
        assertThat(byPlayTimeAsc.map { it.title }).containsExactly("Newest", "Oldest", "Middle").inOrder()

        val byPlayTimeDesc = appendRows.sortedForDisplay(
            sort = PlaylistTrackSort.PLAY_TIME,
            ascending = false,
            title = { it.title },
            artist = { it.artist },
            durationMs = { it.durationMs },
        )
        assertThat(byPlayTimeDesc.map { it.title }).containsExactly("Oldest", "Newest", "Middle").inOrder()
    }
}
