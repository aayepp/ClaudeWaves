package com.lastwave.app.playback

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.lastwave.app.data.local.SettingsPreferences
import com.lastwave.app.diagnostics.AppLog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Returns true when [deviceType] represents a Bluetooth headset/speaker/hearing-aid sink. */
internal fun isBluetoothAudioSinkType(
    deviceType: Int,
    sdkInt: Int = Build.VERSION.SDK_INT,
): Boolean {
    if (deviceType == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) return true
    if (sdkInt >= Build.VERSION_CODES.P && deviceType == AudioDeviceInfo.TYPE_HEARING_AID) return true
    if (sdkInt >= Build.VERSION_CODES.S) {
        if (deviceType == AudioDeviceInfo.TYPE_BLE_HEADSET ||
            deviceType == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
            deviceType == AudioDeviceInfo.TYPE_BLE_BROADCAST
        ) {
            return true
        }
    }
    return false
}

/**
 * Listens for Bluetooth audio output attachments via [AudioDeviceCallback]
 * (requires no `BLUETOOTH_CONNECT` runtime permission) and resumes playback
 * automatically when enabled in Settings.
 */
@Singleton
class BluetoothAutoResumeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsPreferences: SettingsPreferences,
    private val musicPlayer: dagger.Lazy<MusicPlayer>,
    private val applicationScope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)
    private val knownDeviceIds = HashSet<Int>()
    private var debounceResumeJob: Job? = null

    fun start() {
        if (!started.compareAndSet(false, true)) return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

        // Seed currently attached Bluetooth sinks so registration itself never auto-plays.
        runCatching {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .filter { it.isSink && isBluetoothAudioSinkType(it.type) }
                .forEach { knownDeviceIds.add(it.id) }
        }

        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                val newlyAttached = addedDevices.any { device ->
                    device.isSink &&
                        isBluetoothAudioSinkType(device.type) &&
                        knownDeviceIds.add(device.id)
                }
                if (!newlyAttached) return
                debounceResumeJob?.cancel()
                debounceResumeJob = applicationScope.launch(Dispatchers.IO) {
                    delay(RESUME_DEBOUNCE_MS)
                    val enabled = runCatching {
                        settingsPreferences.settings.first().autoResumeBluetooth
                    }.getOrDefault(false)
                    if (!enabled) return@launch
                    val player = runCatching { musicPlayer.get() }.getOrNull() ?: return@launch
                    val chrome = player.chromeState.value
                    if (chrome.isPlaying || chrome.isBuffering) return@launch
                    runCatching { player.resumeOrShuffleMix() }
                        .onFailure { AppLog.w(TAG, "Bluetooth auto-resume failed: ${it.message}") }
                }
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                removedDevices.forEach { device -> knownDeviceIds.remove(device.id) }
            }
        }

        runCatching {
            audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        }.onFailure {
            AppLog.w(TAG, "Could not register AudioDeviceCallback: ${it.message}")
        }
    }

    private companion object {
        const val TAG = "BtAutoResume"
        const val RESUME_DEBOUNCE_MS = 1_200L
    }
}
