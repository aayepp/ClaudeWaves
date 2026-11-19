package com.lastwave.app.playback

import com.lastwave.app.data.generate.GeneratedTrack
import com.lastwave.app.data.generate.LocalAudioFormats

internal fun PlayableTrack.toGeneratedTrack() = GeneratedTrack(
    name = title,
    artist = artist,
    artworkUrl = artworkUrl,
    album = album,
    trackNumber = trackNumber,
    durationMs = durationMs,
    // Only pure device-local files (no videoId) keep their storage path/URI;
    // downloaded YouTube tracks keep their watch URL so likes, playlists, and
    // sync treat them identically to their streaming counterparts.
    url = if (videoId.isNullOrBlank()) {
        LocalAudioFormats.importedPathOrNull(playbackUrl).orEmpty()
    } else {
        "https://music.youtube.com/watch?v=$videoId"
    },
)
