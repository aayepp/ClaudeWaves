package com.lastwave.app.ui.settings

import com.lastwave.app.R
import com.lastwave.app.data.local.EQ_MAX_GAIN_DB
import com.lastwave.app.data.local.EqualizerPreferences
import com.lastwave.app.data.local.EqualizerPresets
import com.lastwave.app.data.local.EqualizerSettings
import com.lastwave.app.playback.NativeAudioEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shared 15-band equalizer state/preview controller used by both Settings and
 * the Now Playing EQ sheet so band drags preview immediately and persist cleanly.
 */
internal class EqualizerController(
    private val equalizerPreferences: EqualizerPreferences,
    private val audioEngine: dagger.Lazy<NativeAudioEngine>,
    private val scope: CoroutineScope,
    val settings: StateFlow<EqualizerSettings>,
    private val logTag: String,
    private val onFailure: (Int) -> Unit,
    private val runAction: (action: String, block: suspend () -> Unit) -> Unit,
) {
    private var immediateEnabled = false
    private var immediateGains = EqualizerSettings().gainsDb.toFloatArray()
    private val previews = Channel<Pair<Boolean, FloatArray>>(Channel.CONFLATED)

    init {
        scope.launch {
            for ((enabled, gains) in previews) {
                try {
                    withContext(Dispatchers.Default) { audioEngine.get().setEqualizer(enabled, gains) }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    android.util.Log.e(logTag, "Equalizer preview failed", error)
                    onFailure(R.string.toast_eq_fail)
                } catch (error: LinkageError) {
                    android.util.Log.e(logTag, "Equalizer unavailable", error)
                    onFailure(R.string.toast_eq_missing)
                }
            }
        }
        scope.launch {
            settings.collect {
                immediateEnabled = it.enabled
                immediateGains = it.gainsDb.toFloatArray()
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        immediateEnabled = enabled
        previews.trySend(enabled to immediateGains.copyOf())
        runAction("update the equalizer") { equalizerPreferences.setEnabled(enabled) }
    }

    fun applyPreset(name: String) {
        (EqualizerPresets.byName(name) ?: settings.value.userPresets.firstOrNull { it.name == name })?.let { preset ->
            immediateEnabled = true
            immediateGains = preset.gainsDb.toFloatArray()
            previews.trySend(true to immediateGains.copyOf())
            runAction("apply the equalizer preset") { equalizerPreferences.applyPreset(preset) }
        }
    }

    fun previewBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in immediateGains.indices || !gainDb.isFinite()) return
        immediateGains = immediateGains.copyOf().also {
            it[bandIndex] = gainDb.coerceIn(-EQ_MAX_GAIN_DB, EQ_MAX_GAIN_DB)
        }
        previews.trySend(immediateEnabled to immediateGains.copyOf())
    }

    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex !in immediateGains.indices) return
        previewBandGain(bandIndex, gainDb)
        runAction("update the equalizer band") { equalizerPreferences.setBandGain(bandIndex, gainDb) }
    }

    /** Saves the current curve as one of the listener's own named presets (max 20). */
    fun saveUserPreset(name: String) =
        runAction("save the equalizer preset") { equalizerPreferences.saveUserPreset(name) }

    fun renameUserPreset(oldName: String, newName: String) =
        runAction("rename the equalizer preset") { equalizerPreferences.renameUserPreset(oldName, newName) }

    fun deleteUserPreset(name: String) =
        runAction("delete the equalizer preset") { equalizerPreferences.deleteUserPreset(name) }
}
