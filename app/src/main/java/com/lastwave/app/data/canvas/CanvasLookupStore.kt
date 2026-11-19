package com.lastwave.app.data.canvas

import android.util.Log
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Canvas lookups that found a clip, kept on disk. Lookups ask Spotify / Apple / Tidal
 * over the network and were remembered only in memory, so offline even a clip already
 * in [CanvasCache] never played: the lookup failed before the cache was reached.
 */
internal class CanvasLookupStore(private val file: File) {
    private val json = Json { ignoreUnknownKeys = true }
    private var entries: LinkedHashMap<String, CanvasArtwork>? = null

    @Synchronized
    fun get(key: String): CanvasArtwork? = load()[key]

    @Synchronized
    fun put(key: String, artwork: CanvasArtwork) {
        val map = load()
        if (map[key] == artwork) return
        map.remove(key)
        map[key] = artwork
        while (map.size > MAX_ENTRIES) map.remove(map.keys.first())
        runCatching { file.writeText(json.encodeToString(map as Map<String, CanvasArtwork>)) }
            .onFailure { Log.w(TAG, "Saving canvas lookups failed: ${it.message}") }
    }

    private fun load(): LinkedHashMap<String, CanvasArtwork> = entries ?: LinkedHashMap(
        runCatching { json.decodeFromString<Map<String, CanvasArtwork>>(file.readText()) }.getOrDefault(emptyMap()),
    ).also { entries = it }

    private companion object {
        const val TAG = "CanvasLookupStore"

        /** Well past what the 150 MB clip cache holds, so the lookup outlives the clip. */
        const val MAX_ENTRIES = 1_000
    }
}
