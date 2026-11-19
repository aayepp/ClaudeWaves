package com.lastwave.app.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MiscSettingsDefaultsTest {

    @Test
    fun miscSettings_newFeatureDefaultsAreSafeAndDisabled() {
        val defaults = MiscSettings()
        assertThat(defaults.nowPlayingDownloadButtonEnabled).isFalse()
        assertThat(defaults.smartOfflineCacheEnabled).isFalse()
        assertThat(defaults.smartOfflineCacheLimit).isEqualTo(SMART_OFFLINE_DEFAULT_LIMIT)
        assertThat(defaults.autoResumeBluetooth).isFalse()
        assertThat(SMART_OFFLINE_LIMIT_CHOICES).containsExactly(20, 25, 35, 50).inOrder()
    }
}
