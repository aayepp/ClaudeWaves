package com.lastwave.app.presence

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.lastwave.app.data.artwork.ArtworkNormalizer
import com.lastwave.app.data.artwork.ArtworkRepository
import com.lastwave.app.data.artwork.DeferredArtworkLookup
import com.lastwave.app.playback.MusicPlayer
import com.lastwave.app.playback.MusicPlayerState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient

/**
 * Publishes what is playing to Discord as a Rich Presence activity.
 *
 * Two transports, selected automatically:
 * 1. **Discord Gateway WebSocket** ([DiscordGatewayTransport]) when the user has
 *    signed in with their Discord account in Settings → Discord. Works reliably on
 *    every Android device even when the Discord app is closed, and includes a
 *    "Listen on YouTube Music" link + Hi-Res quality line + live artwork.
 * 2. **Local Android IPC** ([DiscordIpcTransport]) fallback when signed out.
 *
 * Playback is never touched, delayed, or gated by anything in here: every
 * failure is swallowed and every frame is fire-and-forget.
 */
@Singleton
class DiscordPresenceManager @Inject constructor(
    private val musicPlayer: MusicPlayer,
    private val preferences: DiscordPresencePreferences,
    private val artworkRepository: ArtworkRepository,
    private val okHttpClient: OkHttpClient,
    private val applicationScope: CoroutineScope,
    @ApplicationContext private val context: Context,
) {
    private val lock = Mutex()

    private var ipcTransport: DiscordTransport? = null
    private var gatewayTransport: DiscordGatewayTransport? = null
    private var gatewayToken: String = ""
    private var gatewayStatusJob: Job? = null
    private var started = false

    @Volatile private var latestSettings = DiscordSettings()
    private var lastAttemptMs = 0L
    private var lastKey = ""
    private var lastPushMs = 0L

    private val _connectionStatus = MutableStateFlow(DiscordConnectionStatus.SIGNED_OUT)
    val connectionStatus: StateFlow<DiscordConnectionStatus> = _connectionStatus.asStateFlow()

    private val deferredArtwork = DeferredArtworkLookup(
        scope = applicationScope,
        source = "discord",
        hasMemoryHit = { key ->
            artworkRepository.resolved.value[key]
                ?.startsWith("http://") == true ||
                artworkRepository.resolved.value[key]?.startsWith("https://") == true
        },
        resolve = { name, artist ->
            runCatching { artworkRepository.resolve(name, artist) }
        },
    )
    private var observer: Job? = null
    private var heartbeat: Job? = null

    @Synchronized
    fun start() {
        if (started) return
        started = true
        observer = applicationScope.launch(Dispatchers.Default) {
            try {
                combine(
                    musicPlayer.state,
                    preferences.settings,
                    artworkRepository.resolved,
                ) { state, settings, resolved ->
                    Triple(state, settings, resolved)
                }.collect { (state, settings, resolved) ->
                    latestSettings = settings
                    evaluate(state, settings, resolved)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Log.e(TAG, "Discord presence observer stopped", error)
            }
        }
        heartbeat = applicationScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(DiscordPresence.PUSH_INTERVAL_MS)
                evaluate(musicPlayer.state.value, latestSettings, artworkRepository.resolved.value)
            }
        }
    }

    /** Re-evaluates immediately (settings toggle, login, or explicit refresh). */
    fun refresh() {
        applicationScope.launch(Dispatchers.Default) {
            val settings = runCatching { preferences.settings.first() }.getOrDefault(latestSettings)
            latestSettings = settings
            evaluate(musicPlayer.state.value, settings, artworkRepository.resolved.value, force = true)
        }
    }

    /** Validates a Discord token via `/users/@me`, saves the account locally, and connects immediately. */
    suspend fun signInWithToken(rawToken: String): Result<DiscordUserProfile> {
        val clean = rawToken.trim().removeSurrounding("\"")
        val result = DiscordGatewayTransport.fetchCurrentUser(okHttpClient, clean)
        result.onSuccess { profile ->
            preferences.saveAccount(
                token = clean,
                userId = profile.id,
                username = profile.username,
                displayName = profile.displayName,
                avatarUrl = profile.avatarUrl,
            )
            refresh()
        }
        return result
    }

    /** Clears active presence, tears down Gateway socket, and wipes local token & profile cache. */
    suspend fun signOut() {
        lock.withLock {
            clearGatewayQuiet()
            closeGatewayLocked()
            lastKey = ""
        }
        preferences.clearAccount()
        _connectionStatus.value = DiscordConnectionStatus.SIGNED_OUT
    }

    private suspend fun evaluate(
        state: MusicPlayerState,
        settings: DiscordSettings,
        resolvedArt: Map<String, String>,
        force: Boolean = false,
    ) {
        val now = SystemClock.elapsedRealtime()
        lock.withLock {
            if (!settings.isSignedIn) {
                closeGatewayLocked()
                _connectionStatus.value = DiscordConnectionStatus.SIGNED_OUT
                evaluateIpcLocked(state, settings.enabled, resolvedArt, now)
                return
            }

            // Signed in: prefer Discord Gateway WebSocket and drop any idle IPC bind.
            closeIpcLocked()
            if (!settings.enabled || !DiscordPresence.isConfigured()) {
                deferredArtwork.cancel()
                clearGatewayQuiet(settings.presenceStatus)
                closeGatewayLocked()
                _connectionStatus.value = DiscordConnectionStatus.DISCONNECTED
                return
            }

            if (gatewayToken != settings.token) {
                closeGatewayLocked()
                lastKey = ""
                lastAttemptMs = 0L
            }

            val track = state.current
            val playing = track != null && track.title.isNotBlank() && DiscordPresence.isPlayingForPresence(state)
            if (!playing || track == null) {
                deferredArtwork.cancel()
                clearGatewayQuiet(settings.presenceStatus)
                if (gatewayTransport == null && _connectionStatus.value == DiscordConnectionStatus.SIGNED_OUT) {
                    _connectionStatus.value = DiscordConnectionStatus.DISCONNECTED
                }
                return
            }

            val effectiveState = withResolvedArtwork(state, resolvedArt)
            val key = DiscordPresence.gatewaySignature(effectiveState, settings)
            if (!force && key == lastKey && now - lastPushMs < DiscordPresence.PUSH_INTERVAL_MS && gatewayTransport?.isConnectingOrReady == true) {
                return
            }

            val gw = ensureGatewayLocked(settings, now, force) ?: return
            val coverUrl = DiscordPresence.largeImage(effectiveState.current?.artworkUrl)
            val largeAsset = DiscordGatewayTransport.resolveExternalAsset(okHttpClient, settings.token, coverUrl)
                ?: DiscordGatewayTransport.resolveExternalAsset(okHttpClient, settings.token, DiscordPresence.DISCORD_LOGO_URL)
            val smallAsset = DiscordGatewayTransport.resolveExternalAsset(okHttpClient, settings.token, DiscordPresence.DISCORD_LOGO_URL)

            val nowWallMs = System.currentTimeMillis()
            val activity = DiscordPresence.buildGatewayActivity(
                state = effectiveState,
                settings = settings,
                nowMs = nowWallMs,
                resolvedLargeImage = largeAsset,
                resolvedSmallImage = smallAsset,
            )
            val frame = DiscordPresence.gatewayPresenceFrame(
                activity = activity,
                status = settings.presenceStatus,
                nowMs = nowWallMs,
            )
            val sent = gw.sendPresence(frame, settings.presenceStatus)
            if (!sent) {
                Log.w(TAG, "Discord Gateway send failed; will reconnect")
                closeGatewayLocked()
                lastAttemptMs = SystemClock.elapsedRealtime()
                return
            }
            lastKey = key
            lastPushMs = now
            Log.d(TAG, "Discord Gateway presence queued/pushed: ${track.title}")
        }
    }

    private fun ensureGatewayLocked(
        settings: DiscordSettings,
        now: Long,
        force: Boolean = false,
    ): DiscordGatewayTransport? {
        val existing = gatewayTransport
        if (existing != null) {
            if (existing.status.value == DiscordConnectionStatus.AUTH_FAILED) {
                _connectionStatus.value = DiscordConnectionStatus.AUTH_FAILED
                return null
            }
            if (existing.isConnectingOrReady) return existing
            if (!force && now - lastAttemptMs < DiscordPresence.RETRY_INTERVAL_MS) return null
            lastAttemptMs = now
            existing.connect()
            return existing
        }
        if (!force && now - lastAttemptMs < DiscordPresence.RETRY_INTERVAL_MS) return null
        lastAttemptMs = now
        gatewayToken = settings.token
        val fresh = DiscordGatewayTransport(
            okHttpClient = okHttpClient,
            token = settings.token,
            scope = applicationScope,
            initialStatus = settings.presenceStatus,
        )
        gatewayTransport = fresh
        gatewayStatusJob?.cancel()
        gatewayStatusJob = applicationScope.launch(Dispatchers.Default) {
            fresh.status.collect { st ->
                _connectionStatus.value = st
            }
        }
        fresh.connect()
        return fresh
    }

    private suspend fun evaluateIpcLocked(
        state: MusicPlayerState,
        enabled: Boolean,
        resolvedArt: Map<String, String>,
        now: Long,
    ) {
        if (!enabled || !DiscordPresence.isConfigured()) {
            deferredArtwork.cancel()
            clearIpcQuiet()
            return
        }
        val track = state.current
        if (track == null || track.title.isBlank()) {
            deferredArtwork.cancel()
            clearIpcQuiet()
            return
        }
        val effectiveState = withResolvedArtwork(state, resolvedArt)
        val key = DiscordPresence.signature(effectiveState)
        if (key == lastKey && now - lastPushMs < DiscordPresence.PUSH_INTERVAL_MS) {
            return
        }
        val active = ensureIpcConnectedLocked(now) ?: return
        val sent = runCatching {
            active.setActivity(DiscordPresence.buildActivity(effectiveState, System.currentTimeMillis()))
        }.onFailure {
            Log.w(TAG, "Discord IPC push failed", it)
        }.getOrDefault(false)
        if (!sent) {
            closeIpcLocked()
            lastAttemptMs = SystemClock.elapsedRealtime()
            return
        }
        lastKey = key
        lastPushMs = now
    }

    private fun withResolvedArtwork(
        state: MusicPlayerState,
        resolvedArt: Map<String, String>,
    ): MusicPlayerState {
        val track = state.current ?: return state
        val ownArt = track.artworkUrl?.trim()
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        val pipelineArt = resolvedArt[ArtworkNormalizer.cacheKey(track.title, track.artist)]
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        if (ownArt == null && pipelineArt == null) {
            deferredArtwork.request(track.title, track.artist)
        }
        return if (pipelineArt != null && pipelineArt != track.artworkUrl) {
            state.copy(current = track.copy(artworkUrl = pipelineArt))
        } else {
            state
        }
    }

    private suspend fun ensureIpcConnectedLocked(now: Long): DiscordTransport? {
        val existing = ipcTransport
        if (existing != null && existing.isOpen) return existing
        if (existing != null && !existing.isOpen) {
            closeIpcLocked()
        }
        if (now - lastAttemptMs < DiscordPresence.RETRY_INTERVAL_MS) return null
        lastAttemptMs = now
        val fresh = DiscordIpcTransport.connectOrNull(context)
        return fresh?.also { ipcTransport = it }
    }

    private fun clearIpcQuiet() {
        lastKey = ""
        val active = ipcTransport ?: return
        runCatching { active.clearActivity() }
            .onFailure { Log.d(TAG, "Discord IPC clear ignored", it) }
    }

    private fun closeIpcLocked() {
        runCatching { ipcTransport?.close() }
        ipcTransport = null
    }

    private fun clearGatewayQuiet(status: String = latestSettings.presenceStatus) {
        lastKey = ""
        val active = gatewayTransport ?: return
        runCatching { active.clearPresence(status) }
            .onFailure { Log.d(TAG, "Discord Gateway clear ignored", it) }
    }

    private fun closeGatewayLocked() {
        gatewayStatusJob?.cancel()
        gatewayStatusJob = null
        runCatching { gatewayTransport?.close() }
        gatewayTransport = null
        gatewayToken = ""
    }

    /** Best-effort clear + release. Never throws. */
    suspend fun shutdown() {
        heartbeat?.cancel()
        heartbeat = null
        observer?.cancel()
        observer = null
        lock.withLock {
            clearIpcQuiet()
            closeIpcLocked()
            clearGatewayQuiet()
            closeGatewayLocked()
        }
        started = false
    }

    private companion object {
        const val TAG = "DiscordPresence"
    }
}
