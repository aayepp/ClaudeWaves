package com.lastwave.app.ui.playlist

import com.lastwave.app.ui.common.sortedForDisplay
import com.lastwave.app.data.generate.GeneratedTrack

// Keep source indices through sorting/filtering: a song may occur more than once.
internal fun playlistTrackDisplayIndices(
    tracks: List<GeneratedTrack>,
    folderTracks: List<GeneratedTrack>,
    sort: PlaylistTrackSort,
    ascending: Boolean,
    query: String,
    // YouTube's Liked Music arrives newest-first; every other playlist appends new songs.
    newestFirst: Boolean = false,
): List<Int> {
    val visible = folderTracks.toHashSet()
    val sorted = tracks.indices.filter { tracks[it] in visible }.sortedForDisplay(
        sort,
        ascending = ascending,
        title = { tracks[it].name },
        artist = { tracks[it].artist },
        durationMs = { tracks[it].durationMs },
        newestFirst = newestFirst,
        addedAtMillis = { tracks[it].addedAtMillis },
    )
    if (query.isBlank()) return sorted
    val clean = query.lowercase()
    return sorted.filter { tracks[it].name.lowercase().contains(clean) || tracks[it].artist.lowercase().contains(clean) }
}

internal fun selectedPlaylistTrackIndex(tracks: List<GeneratedTrack>, sourceIndex: Int, selected: GeneratedTrack): Int? {
    fun GeneratedTrack.matchesSelected() = this == selected || (selected.url.isNotBlank() && url == selected.url)
    if (tracks.getOrNull(sourceIndex)?.matchesSelected() == true) return sourceIndex
    return tracks.indexOf(selected).takeIf { it >= 0 }
        ?: tracks.indexOfFirst { it.matchesSelected() }.takeIf { it >= 0 }
}
