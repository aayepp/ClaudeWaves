package com.lastwave.app.data.lossless

import android.content.Context
import android.util.Log
import com.lastwave.app.data.addon.AddonClient
import com.lastwave.app.data.addon.parseRateFromQualityString
import com.lastwave.app.data.local.SettingsPreferences
import com.lastwave.app.data.local.db.DownloadedTrackDao
import com.lastwave.app.data.local.db.DownloadedTrackEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "LosslessBadgeRepo"
private const val CACHE_FILE_NAME = "lossless_availability_cache_v1.json"
private const val POSITIVE_TTL_MILLIS = 14L * 24 * 60 * 60 * 1000
private const val NEGATIVE_TTL_MILLIS = 3L * 24 * 60 * 60 * 1000
private const val MAX_CACHE_ENTRIES = 2_000
private const val MAX_CONCURRENT_PROBES = 2
private const val PROBE_SPACING_MS = 80L

data class LosslessProbeTarget(
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationSeconds: Int? = null,
) {
    val cacheKey: String get() = losslessTrackKey(title, artist)
}

@Serializable
private data class CachedBadgeEntry(
    val tier: LosslessListTier,
    val checkedAtMillis: Long,
    /** An Atmos version exists besides [tier]; survives playback reporting the stereo tier. */
    val atmos: Boolean = false,
) {
    val hasAtmos: Boolean get() = atmos || tier == LosslessListTier.ATMOS

    /**
     * The stereo version's tier. Search can see only the Atmos one, yet playback at a
     * stereo quality then resolves a stereo stream that is nearly always Hi-Res, so that
     * is assumed until a play reports the real tier.
     */
    val stereoTier: LosslessListTier?
        get() = when (tier) {
            LosslessListTier.ATMOS -> LosslessListTier.HI_RES
            LosslessListTier.NONE -> null
            else -> tier
        }
}

@Serializable
private data class PersistedBadgeCache(
    val configFingerprint: String = "",
    val entries: Map<String, CachedBadgeEntry> = emptyMap(),
)

/**
 * Maintains instant offline/playback lossless badges plus lazy, metadata-only
 * background probes for visible song-list rows. Never calls `/stream/{id}`.
 */
@Singleton
class LosslessAvailabilityRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadedTrackDao: DownloadedTrackDao,
    private val losslessMusicApi: LosslessMusicApi,
    private val settingsPreferences: SettingsPreferences,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private val cacheMutex = Mutex()
    private val probeSemaphore = Semaphore(MAX_CONCURRENT_PROBES)
    private val inFlightKeys: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())

    private val downloadTiers = MutableStateFlow<Map<String, LosslessListTier>>(emptyMap())
    private val probedEntries = MutableStateFlow<Map<String, CachedBadgeEntry>>(emptyMap())
    private val isEnabled: StateFlow<Boolean> = settingsPreferences.settings
        .map { it.showLosslessListBadges }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, true)

    private var diskLoaded = false
    private var activeFingerprint: String = ""
    private var flushJob: Job? = null

    /** Dolby Atmos is the streaming quality on Wi-Fi or mobile data: ATMOS leads the badges. */
    val prefersAtmos: StateFlow<Boolean> = settingsPreferences.settings
        .map {
            it.wifiStreamingQuality == LosslessMusicApi.QUALITY_DOLBY_ATMOS ||
                it.cellularStreamingQuality == LosslessMusicApi.QUALITY_DOLBY_ATMOS
        }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, false)

    /**
     * Reactive map of canonical `title|artist` keys to the leading badge: the stereo
     * tier, or ATMOS when Atmos is the streaming quality. Emits an empty map when the
     * user disables list badges in Settings.
     */
    val badges: StateFlow<Map<String, LosslessListTier>> = combine(
        downloadTiers,
        probedEntries,
        isEnabled,
        prefersAtmos,
    ) { downloaded, probed, enabled, atmosPreferred ->
        if (!enabled) {
            emptyMap()
        } else {
            buildMap {
                for ((key, entry) in probed) {
                    val lead = if (atmosPreferred && entry.hasAtmos) LosslessListTier.ATMOS else entry.stereoTier
                    if (lead != null) put(key, lead)
                }
                // Local downloaded files reflect guaranteed offline quality and override cached probes.
                putAll(downloaded)
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    /**
     * The badge beside the leading one for songs that have an Atmos version: ATMOS
     * next to the stereo tier, or the stereo tier next to ATMOS when Atmos is the
     * streaming quality. Lists read "HI-RES, ATMOS": what plays, and what else exists.
     */
    val secondaryBadges: StateFlow<Map<String, LosslessListTier>> = combine(
        probedEntries,
        isEnabled,
        prefersAtmos,
    ) { probed, enabled, atmosPreferred ->
        if (!enabled) {
            emptyMap()
        } else {
            buildMap {
                for ((key, entry) in probed) {
                    if (!entry.hasAtmos) continue
                    val second = if (atmosPreferred) entry.stereoTier else LosslessListTier.ATMOS
                    if (second != null) put(key, second)
                }
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    /**
     * `title|artist` keys of every song downloaded by the app, for the "on this
     * phone" mark in song lists. Independent of the quality-badge setting.
     */
    val downloadedKeys: StateFlow<Set<String>> = downloadedTrackDao.getAll()
        .map { downloads ->
            downloads.mapNotNullTo(HashSet()) { losslessTrackKey(it.title, it.artist).takeIf(String::isNotBlank) }
        }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    init {
        scope.launch { ensureDiskLoaded() }
        scope.launch {
            downloadedTrackDao.getAll().collect { downloads ->
                downloadTiers.value = buildDownloadTierMap(downloads)
            }
        }
        scope.launch {
            losslessMusicApi.resolvedTrackTiers.collect { event ->
                val key = event.key
                if (key.isBlank()) return@collect
                ensureDiskLoaded()
                var changed = false
                cacheMutex.withLock {
                    val existing = probedEntries.value[key]
                    val hadAtmos = existing?.atmos == true || existing?.tier == LosslessListTier.ATMOS
                    val stereoKnown = existing != null &&
                        existing.tier != LosslessListTier.NONE && existing.tier != LosslessListTier.ATMOS
                    // Playing the Atmos version marks it, without hiding the stereo tier; playing
                    // stereo replaces an Atmos-only tier but keeps the Atmos mark.
                    val next = if (event.tier == LosslessListTier.ATMOS && stereoKnown) {
                        if (existing!!.atmos) return@withLock
                        existing.copy(atmos = true)
                    } else {
                        val merged = mergeTierUpdate(existing?.tier, event) ?: return@withLock
                        CachedBadgeEntry(tier = merged, checkedAtMillis = System.currentTimeMillis(), atmos = hadAtmos)
                    }
                    val updated = probedEntries.value.toMutableMap()
                    updated[key] = next
                    probedEntries.value = updated
                    changed = true
                }
                if (changed) scheduleDiskFlush()
            }
        }
        scope.launch {
            combine(
                settingsPreferences.httpAddons,
                settingsPreferences.addonUrl,
                settingsPreferences.addonEnabled,
            ) { addons, legacyUrl, legacyEnabled ->
                computeFingerprint(addons.filter { it.enabled && it.url.isNotBlank() }.map { it.url }, legacyUrl, legacyEnabled)
            }.distinctUntilChanged().collect { newFingerprint ->
                ensureDiskLoaded()
                var changed = false
                cacheMutex.withLock {
                    if (activeFingerprint.isNotEmpty() && activeFingerprint != newFingerprint) {
                        activeFingerprint = newFingerprint
                        probedEntries.value = emptyMap()
                        changed = true
                    } else if (activeFingerprint.isEmpty()) {
                        activeFingerprint = newFingerprint
                    }
                }
                if (changed) scheduleDiskFlush()
            }
        }
    }

    fun queueProbe(targets: List<LosslessProbeTarget>) {
        if (targets.isEmpty()) return
        if (!isEnabled.value) return
        if (losslessMusicApi.isCoolingDown) return

        scope.launch {
            ensureDiskLoaded()
            val fingerprintNow = currentFingerprint()
            if (fingerprintNow.isBlank()) return@launch

            val now = System.currentTimeMillis()
            val downloaded = downloadTiers.value
            val cached = probedEntries.value

            val candidates = LinkedHashMap<String, LosslessProbeTarget>()
            for (target in targets) {
                val key = target.cacheKey
                if (key.isBlank() || target.artist.isBlank()) continue
                if (key in downloaded) continue
                val cachedEntry = cached[key]
                if (cachedEntry != null && isEntryFresh(cachedEntry, now)) continue
                if (!candidates.containsKey(key) && inFlightKeys.add(key)) {
                    candidates[key] = target
                }
            }
            if (candidates.isEmpty()) return@launch

            for ((key, target) in candidates) {
                scope.launch {
                    try {
                        probeSemaphore.withPermit {
                            currentCoroutineContext().ensureActive()
                            if (!isEnabled.value || losslessMusicApi.isCoolingDown) {
                                return@withPermit
                            }
                            val probe = losslessMusicApi.probeTrackListTier(
                                title = target.title,
                                artist = target.artist,
                                expectedAlbum = target.album,
                                expectedDurationSeconds = target.durationSeconds,
                            )
                            if (probe != null) {
                                cacheMutex.withLock {
                                    val updated = probedEntries.value.toMutableMap()
                                    updated[key] = CachedBadgeEntry(
                                        tier = probe.tier,
                                        checkedAtMillis = System.currentTimeMillis(),
                                        atmos = probe.atmos,
                                    )
                                    probedEntries.value = trimCache(updated)
                                }
                                scheduleDiskFlush()
                            }
                            delay(PROBE_SPACING_MS)
                        }
                    } catch (e: CancellationException) {
                        currentCoroutineContext().ensureActive()
                    } catch (t: Throwable) {
                        Log.w(TAG, "List probe skipped for '${target.title}': ${t.message}")
                    } finally {
                        inFlightKeys.remove(key)
                    }
                }
            }
        }
    }

    private suspend fun ensureDiskLoaded() {
        if (diskLoaded) return
        cacheMutex.withLock {
            if (diskLoaded) return@withLock
            val currentFp = currentFingerprint()
            val file = File(context.filesDir, CACHE_FILE_NAME)
            val persisted = runCatching {
                if (!file.exists()) null
                else json.decodeFromString<PersistedBadgeCache>(file.readText())
            }.getOrNull()

            if (persisted != null && persisted.configFingerprint == currentFp) {
                val now = System.currentTimeMillis()
                val valid = persisted.entries.filterValues { isEntryFresh(it, now) }
                if (probedEntries.value.isEmpty()) {
                    probedEntries.value = trimCache(valid)
                } else {
                    // Preserve newer live entries accumulated before disk load completed.
                    val merged = valid.toMutableMap()
                    merged.putAll(probedEntries.value)
                    probedEntries.value = trimCache(merged)
                }
            }
            activeFingerprint = currentFp
            diskLoaded = true
        }
    }

    private fun scheduleDiskFlush() {
        flushJob?.cancel()
        flushJob = scope.launch {
            delay(1_500L)
            saveCacheToDisk()
        }
    }

    private suspend fun saveCacheToDisk() {
        val snapshot = cacheMutex.withLock {
            PersistedBadgeCache(
                configFingerprint = activeFingerprint.ifBlank { currentFingerprint() },
                entries = trimCache(probedEntries.value),
            )
        }
        runCatching {
            val target = File(context.filesDir, CACHE_FILE_NAME)
            val tmp = File(context.filesDir, "$CACHE_FILE_NAME.tmp")
            tmp.writeText(json.encodeToString(snapshot))
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
        }.onFailure { err ->
            Log.w(TAG, "Failed saving lossless badge cache: ${err.message}")
        }
    }

    private fun currentFingerprint(): String = computeFingerprint(
        addonUrls = settingsPreferences.httpAddons.value
            .filter { it.enabled && it.url.isNotBlank() }
            .map { it.url },
        legacyUrl = settingsPreferences.addonUrl.value,
        legacyEnabled = settingsPreferences.addonEnabled.value,
    )

    private fun computeFingerprint(
        addonUrls: List<String>,
        legacyUrl: String?,
        legacyEnabled: Boolean,
    ): String {
        val normalized = addonUrls
            .map { AddonClient.normalizeBase(it) }
            .filter { it.isNotBlank() }
            .ifEmpty {
                if (legacyEnabled && !legacyUrl.isNullOrBlank()) {
                    listOf(AddonClient.normalizeBase(legacyUrl))
                } else {
                    emptyList()
                }
            }
        return normalized.sorted().joinToString("|")
    }

    private fun isEntryFresh(entry: CachedBadgeEntry, nowMillis: Long): Boolean {
        val ttl = if (entry.tier == LosslessListTier.NONE) NEGATIVE_TTL_MILLIS else POSITIVE_TTL_MILLIS
        return (nowMillis - entry.checkedAtMillis) in 0..ttl
    }

    private fun trimCache(entries: Map<String, CachedBadgeEntry>): Map<String, CachedBadgeEntry> {
        if (entries.size <= MAX_CACHE_ENTRIES) return entries
        return entries.entries
            .sortedByDescending { it.value.checkedAtMillis }
            .take(MAX_CACHE_ENTRIES)
            .associate { it.key to it.value }
    }

    private fun buildDownloadTierMap(downloads: List<DownloadedTrackEntity>): Map<String, LosslessListTier> {
        if (downloads.isEmpty()) return emptyMap()
        val result = LinkedHashMap<String, LosslessListTier>(downloads.size)
        for (entity in downloads) {
            if (!entity.isLossless) continue
            val key = losslessTrackKey(entity.title, entity.artist)
            if (key.isBlank()) continue
            val tier = classifyDownloadFormatBadge(entity.formatBadge)
            val prev = result[key]
            if (prev == null || tier.ordinal < prev.ordinal) {
                result[key] = tier
            }
        }
        return result
    }

    internal companion object {
        /**
         * Returns the tier to store when [event] arrives, or null when the cached
         * entry already matches or should stay protected against capped downgrades.
         */
        internal fun mergeTierUpdate(
            existingTier: LosslessListTier?,
            event: ResolvedTrackTierEvent,
        ): LosslessListTier? {
            if (!event.allowDowngrade &&
                existingTier == LosslessListTier.HI_RES &&
                event.tier == LosslessListTier.LOSSLESS
            ) {
                return null
            }
            if (existingTier == event.tier) return null
            return event.tier
        }

        internal fun classifyDownloadFormatBadge(formatBadge: String): LosslessListTier {
            val badge = formatBadge.uppercase()
            return when {
                badge.contains("ATMOS") || badge.contains("DOLBY") -> LosslessListTier.ATMOS
                badge.contains("HI_RES") ||
                    badge.contains("HI-RES") ||
                    badge.contains("DSD") ||
                    badge.contains("24/") ||
                    badge.contains("24-BIT") ||
                    badge.contains("24BIT") ||
                    badge.contains("24 BIT") ||
                    badge.contains("32/") ||
                    badge.contains("32-BIT") ||
                    (parseRateFromQualityString(formatBadge) ?: 0.0) > 48_000.0 -> LosslessListTier.HI_RES
                else -> LosslessListTier.LOSSLESS
            }
        }
    }
}
