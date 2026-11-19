package com.lastwave.app.ui.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ComicSplashWaveTest {

    @Test
    fun earthOrbRadius_emergesAfterSystemHandoffAndHoldsBeforeEqualizerBridge() {
        assertThat(splashEarthOrbRadiusScale(0f)).isEqualTo(0f)
        assertThat(splashEarthOrbRadiusScale(0.04f)).isEqualTo(0f)

        val peakScale = splashEarthOrbRadiusScale(0.145f)
        assertThat(peakScale).isWithin(0.01f).of(1.0f)
        assertThat(splashEarthOrbRadiusScale(0.280f)).isWithin(0.01f).of(1.0f)
        assertThat(splashEarthOrbRadiusScale(0.340f)).isLessThan(0.6f)
        assertThat(splashEarthOrbRadiusScale(0.390f)).isEqualTo(0f)
    }

    @Test
    fun headphonesAndAuroraFlip_stayBoundedAcrossNormalizedTimeline() {
        assertThat(splashHeadphonesEnvelope(0f)).isEqualTo(0f)
        assertThat(splashHeadphonesEnvelope(0.18f)).isGreaterThan(0.9f)
        assertThat(splashHeadphonesEnvelope(0.32f)).isEqualTo(0f)

        assertThat(splashAuroraBlobProgress(0.62f)).isEqualTo(0f)
        val midFlip = splashAuroraBlobProgress(0.712f)
        assertThat(midFlip).isGreaterThan(0.4f)
        assertThat(midFlip).isLessThan(0.6f)
        assertThat(splashAuroraBlobProgress(0.80f)).isEqualTo(1f)
    }
}
