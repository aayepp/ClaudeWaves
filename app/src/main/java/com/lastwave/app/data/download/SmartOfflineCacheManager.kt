package com.lastwave.app.data.download

import com.lastwave.app.data.generate.isLocalAudioPath
import com.lastwave.app.data.generate.youtubeVideoIdOrNull
import com.lastwave.app.data.local.SMART_OFFLINE_DEFAULT_LIMIT
import com.lastwave.app.data.local.SettingsPreferences
import com.lastwave.app.data.local.db.DownloadedTrackDao
import com.lastwave.app.data.network.NetworkMonitor
import com.lastwave.app.data.playlist.PlaylistRepository
import com.lastwave.app.data.repository.SongPlayStatsRepository
import com.lastwave.app.diagnostics.AppLog
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SmartCacheCandidate(
    val title: String,
    val artist: String,
    val album: String? = null,
    val artworkUrl: String? = null,
    val videoId: String? = null,
    val durationMs: Long? = null,
)

/**
 * Pure selection logic for Smart Offline Mix: merges top-played tracks and
 * recent Liked Songs in priority order up to [limit] unique slots, excluding
 * anything already cached locally.
 */
internal fun selectSmartOfflineCandidates(
    topPlayed: List<SmartCacheCandidate>,
    liked: List<SmartCacheCandidate>,
    existingKeys: Set<String>,
    limit: Int,
): List<SmartCacheCandidate> {
    if (limit <= 0) return emptyList()
    val pool = ArrayList<SmartCacheCandidate>(limit)
    val seenPoolKeys = HashSet<String>()
    for (candidate in topPlayed + liked) {
        if (candidate.title.isBlank() || candidate.artist.isBlank()) continue
        val key = TrackDownloadManager.makeDownloadKey(candidate.title, candidate.artist)
        if (!seenPoolKeys.add(key)) continue
        pool += candidate
        if (pool.size >= limit) break
    }
    return pool.filter { candidate ->
        TrackDownloadManager.makeDownloadKey(candidate.title, candidate.artist) !in existingKeys
    }
}

/**
 * Automatically caches favorite tracks (top-played + recent Liked Songs) when
 * connected to an unmetered non-cellular network (Wi-Fi / Ethernet).
 */
@Singleton
class SmartOfflineCacheManager @Inject constructor(
    private val settingsPreferences: SettingsPreferences,
    private val networkMonitor: NetworkMonitor,
    private val songPlayStatsRepository: SongPlayStatsRepository,
    private val playlistRepository: PlaylistRepository,
    private val downloadedTrackDao: DownloadedTrackDao,
    private val downloadManager: TrackDownloadManager,
    private val applicationScope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)
    private val syncMutex = Mutex()
    @Volatile private var lastAutoSyncAtMs = 0L
    @Volatile private var lastAutoSyncLimit = SMART_OFFLINE_DEFAULT_LIMIT

    fun start() {
        if (!started.compareAndSet(false, true)) return
        applicationScope.launch(Dispatchers.IO) {
            delay(STARTUP_SETTLE_MS)
            try {
                combine(
                    settingsPreferences.settings
                        .map { it.smartOfflineCacheEnabled to it.smartOfflineCacheLimit }
                        .distinctUntilChanged(),
                    networkMonitor.activeRoute
                        .map { it.network != null && !it.isCellular && it.isUnmetered }
                        .distinctUntilChanged(),
                ) { (enabled, limit), onUnmeteredWifi ->
                    Triple(enabled, limit, onUnmeteredWifi)
                }.collect { (enabled, limit, onUnmeteredWifi) ->
                    if (enabled && onUnmeteredWifi) {
                        syncNow(limit = limit, force = false)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                AppLog.e(TAG, "Smart Offline Mix observer stopped", error)
            }
        }
    }

    /** Queues missing Smart Offline Mix tracks now and returns the count queued. */
    suspend fun syncNow(limit: Int? = null, force: Boolean = true): Int = syncMutex.withLock {
        val prefs = runCatching { settingsPreferences.settings.first() }.getOrNull()
        val effectiveLimit = limit ?: prefs?.smartOfflineCacheLimit ?: SMART_OFFLINE_DEFAULT_LIMIT
        if (!force) {
            if (prefs?.smartOfflineCacheEnabled != true) return@withLock 0
            val route = networkMonitor.activeRoute.value
            if (route.network == null || route.isCellular || !route.isUnmetered) return@withLock 0
            val now = System.currentTimeMillis()
            if (now - lastAutoSyncAtMs < AUTO_SYNC_COOLDOWN_MS && lastAutoSyncLimit == effectiveLimit) {
                return@withLock 0
            }
            lastAutoSyncAtMs = now
            lastAutoSyncLimit = effectiveLimit
        }

        val topPlayed = runCatching {
            songPlayStatsRepository.mostPlayedSince(days = 60, limit = effectiveLimit)
        }.getOrDefault(emptyList()).map { stat ->
            SmartCacheCandidate(
                title = stat.title,
                artist = stat.artist,
                artworkUrl = stat.artworkUrl,
                videoId = stat.videoId,
            )
        }

        val liked = runCatching {
            playlistRepository.getLikedSongs()?.tracks.orEmpty()
        }.getOrDefault(emptyList())
            .asReversed() // Recently liked appear closer to top of auto-cache after most-played
            .filter { !isLocalAudioPath(it.url) }
            .map { track ->
                SmartCacheCandidate(
                    title = track.name,
                    artist = track.artist,
                    album = track.album,
                    artworkUrl = track.artworkUrl,
                    videoId = track.youtubeVideoIdOrNull(),
                    durationMs = track.durationMs,
                )
            }

        val existingKeys = runCatching {
            downloadedTrackDao.getAllList()
                .map { entity ->
                    entity.trackKey.ifBlank {
                        TrackDownloadManager.makeDownloadKey(entity.title, entity.artist)
                    }
                }
                .toSet()
        }.getOrDefault(emptySet())

        val toQueue = selectSmartOfflineCandidates(
            topPlayed = topPlayed,
            liked = liked,
            existingKeys = existingKeys,
            limit = effectiveLimit,
        )
        for (candidate in toQueue) {
            downloadManager.downloadTrack(
                title = candidate.title,
                artist = candidate.artist,
                album = candidate.album,
                artworkUrl = candidate.artworkUrl,
                videoId = candidate.videoId,
                durationMs = candidate.durationMs,
                silent = true,
            )
            delay(ENQUEUE_YIELD_MS)
        }
        toQueue.size
    }

    private companion object {
        const val TAG = "SmartOfflineCache"
        const val STARTUP_SETTLE_MS = 6_000L
        const val AUTO_SYNC_COOLDOWN_MS = 15 * 60 * 1000L
        const val ENQUEUE_YIELD_MS = 120L
    }
}
