package com.lastwave.app.data.download

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SmartOfflineCacheSelectionTest {

    @Test
    fun selectSmartOfflineCandidates_deduplicatesRespectsLimitAndSkipsExisting() {
        val topPlayed = listOf(
            SmartCacheCandidate(title = "Song A", artist = "Artist 1"),
            SmartCacheCandidate(title = "Song B", artist = "Artist 2"),
            SmartCacheCandidate(title = "  ", artist = "Ignored"),
        )
        val liked = listOf(
            SmartCacheCandidate(title = "song a", artist = "ARTIST 1"), // duplicate of topPlayed
            SmartCacheCandidate(title = "Song C", artist = "Artist 3"),
            SmartCacheCandidate(title = "Song D", artist = "Artist 4"),
        )
        val existing = setOf(TrackDownloadManager.makeDownloadKey("Song B", "Artist 2"))

        val selected = selectSmartOfflineCandidates(
            topPlayed = topPlayed,
            liked = liked,
            existingKeys = existing,
            limit = 3,
        )

        // Pool capped at 3 unique songs: A, B, C -> B already downloaded, so queue A and C only.
        assertThat(selected.map { it.title }).containsExactly("Song A", "Song C").inOrder()
        assertThat(selectSmartOfflineCandidates(topPlayed, liked, existing, limit = 0)).isEmpty()
    }
}
