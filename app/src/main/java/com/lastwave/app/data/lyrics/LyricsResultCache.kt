package com.lastwave.app.data.lyrics

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Lyrics by request key: memory first, then one JSON file per song, so a song played
 * before (or the next song, fetched ahead) shows its lyrics at once, even after a restart
 * or offline. Only found lyrics are stored; a miss is always asked again.
 */
internal class LyricsResultCache(cacheDir: File) {
    private val memory = ConcurrentHashMap<String, LyricsResult>()
    private val dir = File(cacheDir, "lyrics")
    private val json = Json { ignoreUnknownKeys = true }

    operator fun get(key: String): LyricsResult? = memory[key] ?: runCatching {
        val file = fileFor(key).takeIf { it.isFile } ?: return null
        json.decodeFromString(StoredLyrics.serializer(), file.readText()).toResult()
            .also { remember(key, it) }
    }.getOrNull()

    operator fun set(key: String, result: LyricsResult) {
        remember(key, result)
        if (result !is LyricsResult.Success) return
        runCatching {
            dir.mkdirs()
            fileFor(key).writeText(json.encodeToString(StoredLyrics.serializer(), StoredLyrics.of(result)))
            trimDisk()
        }
    }

    // Bounded: a long session must not grow memory without limit. Eviction is coarse;
    // a dropped entry comes back from disk.
    private fun remember(key: String, result: LyricsResult) {
        if (memory.size > 320) runCatching { memory.keys.take(64).forEach(memory::remove) }
        memory[key] = result
    }

    private fun trimDisk() {
        val files = dir.listFiles() ?: return
        if (files.size <= MAX_FILES) return
        files.sortedBy { it.lastModified() }.take(files.size - MAX_FILES).forEach { it.delete() }
    }

    private fun fileFor(key: String): File {
        val hash = MessageDigest.getInstance("SHA-1").digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return File(dir, "$hash.json")
    }

    @Serializable
    private data class StoredLyrics(
        val lines: List<LyricLine>,
        val isSynced: Boolean,
        val isWordSynced: Boolean,
        val plainLyrics: String?,
        val isInstrumental: Boolean,
        val source: String?,
    ) {
        fun toResult() = LyricsResult.Success(lines, isSynced, isWordSynced, plainLyrics, isInstrumental, source)

        companion object {
            fun of(r: LyricsResult.Success) =
                StoredLyrics(r.lines, r.isSynced, r.isWordSynced, r.plainLyrics, r.isInstrumental, r.source)
        }
    }

    private companion object {
        const val MAX_FILES = 600
    }
}
