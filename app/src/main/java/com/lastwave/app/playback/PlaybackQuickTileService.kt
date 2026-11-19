package com.lastwave.app.playback

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.lastwave.app.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Android Quick Settings tile showing the active track and toggling playback
 * (or restoring / shuffling Liked Songs when the queue is empty).
 */
@AndroidEntryPoint
class PlaybackQuickTileService : TileService() {

    @Inject
    lateinit var musicPlayer: dagger.Lazy<MusicPlayer>

    private var tileScope: CoroutineScope? = null
    private var collectorJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        val scope = tileScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            .also { tileScope = it }
        val player = runCatching { musicPlayer.get() }.getOrNull()
        if (player == null) {
            applyTileSnapshot(resolveTilePresentation(PlaybackChromeState()))
            return
        }
        collectorJob?.cancel()
        collectorJob = scope.launch {
            player.chromeState.collect { chrome ->
                applyTileSnapshot(resolveTilePresentation(chrome))
            }
        }
    }

    override fun onStopListening() {
        collectorJob?.cancel()
        collectorJob = null
        super.onStopListening()
    }

    override fun onDestroy() {
        collectorJob?.cancel()
        collectorJob = null
        tileScope?.cancel()
        tileScope = null
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        val player = runCatching { musicPlayer.get() }.getOrNull() ?: return
        val scope = tileScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            .also { tileScope = it }
        val current = player.chromeState.value.current
        if (current != null) {
            player.togglePlayPause()
        } else {
            scope.launch {
                runCatching { player.resumeOrShuffleMix() }
            }
        }
    }

    private fun applyTileSnapshot(presentation: QuickTilePresentation) {
        val tile = qsTile ?: return
        tile.label = presentation.label
        tile.state = if (presentation.isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, R.drawable.ic_notification_wave)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = presentation.subtitle
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = if (presentation.isActive) "Playing" else "Paused"
        }
        runCatching { tile.updateTile() }
    }

    internal data class QuickTilePresentation(
        val label: String,
        val subtitle: String,
        val isActive: Boolean,
    )

    internal companion object {
        fun resolveTilePresentation(chrome: PlaybackChromeState): QuickTilePresentation {
            val track = chrome.current
            if (track == null) {
                return QuickTilePresentation(
                    label = "ClaudeWaves",
                    subtitle = "Tap to play",
                    isActive = false,
                )
            }
            val active = chrome.isPlaying || chrome.isBuffering
            val title = track.title.ifBlank { "ClaudeWaves" }
            val subtitle = when {
                chrome.isBuffering && !chrome.isPlaying -> "Loading..."
                track.artist.isNotBlank() -> track.artist
                active -> "Playing"
                else -> "Paused"
            }
            return QuickTilePresentation(
                label = title,
                subtitle = subtitle,
                isActive = active,
            )
        }
    }
}
