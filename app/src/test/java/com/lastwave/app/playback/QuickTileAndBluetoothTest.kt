package com.lastwave.app.playback

import android.media.AudioDeviceInfo
import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class QuickTileAndBluetoothTest {

    @Test
    fun resolveTilePresentation_handlesEmptyPlayingBufferingAndPausedStates() {
        val empty = PlaybackQuickTileService.resolveTilePresentation(PlaybackChromeState())
        assertThat(empty.label).isEqualTo("ClaudeWaves")
        assertThat(empty.subtitle).isEqualTo("Tap to play")
        assertThat(empty.isActive).isFalse()

        val track = PlayableTrack(title = "Nightcall", artist = "Kavinsky")
        val playing = PlaybackQuickTileService.resolveTilePresentation(
            PlaybackChromeState(current = track, isPlaying = true),
        )
        assertThat(playing.label).isEqualTo("Nightcall")
        assertThat(playing.subtitle).isEqualTo("Kavinsky")
        assertThat(playing.isActive).isTrue()

        val buffering = PlaybackQuickTileService.resolveTilePresentation(
            PlaybackChromeState(current = track, isPlaying = false, isBuffering = true),
        )
        assertThat(buffering.subtitle).isEqualTo("Loading...")
        assertThat(buffering.isActive).isTrue()

        val pausedNoArtist = PlaybackQuickTileService.resolveTilePresentation(
            PlaybackChromeState(current = PlayableTrack(title = "Track", artist = ""), isPlaying = false),
        )
        assertThat(pausedNoArtist.subtitle).isEqualTo("Paused")
        assertThat(pausedNoArtist.isActive).isFalse()
    }

    @Test
    fun isBluetoothAudioSinkType_respectsSdkGuardsAndFiltersNonBluetooth() {
        assertThat(isBluetoothAudioSinkType(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, Build.VERSION_CODES.N))
            .isTrue()
        assertThat(isBluetoothAudioSinkType(AudioDeviceInfo.TYPE_HEARING_AID, Build.VERSION_CODES.O))
            .isFalse()
        assertThat(isBluetoothAudioSinkType(AudioDeviceInfo.TYPE_HEARING_AID, Build.VERSION_CODES.P))
            .isTrue()
        assertThat(isBluetoothAudioSinkType(AudioDeviceInfo.TYPE_BLE_HEADSET, Build.VERSION_CODES.R))
            .isFalse()
        assertThat(isBluetoothAudioSinkType(AudioDeviceInfo.TYPE_BLE_HEADSET, Build.VERSION_CODES.S))
            .isTrue()
        assertThat(isBluetoothAudioSinkType(AudioDeviceInfo.TYPE_BLE_SPEAKER, Build.VERSION_CODES.S))
            .isTrue()
        assertThat(isBluetoothAudioSinkType(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, Build.VERSION_CODES.UPSIDE_DOWN_CAKE))
            .isFalse()
        assertThat(isBluetoothAudioSinkType(AudioDeviceInfo.TYPE_WIRED_HEADSET, Build.VERSION_CODES.UPSIDE_DOWN_CAKE))
            .isFalse()
    }
}
