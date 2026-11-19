package com.lastwave.app

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class IdleColdStartPolicyTest {

    @Test
    fun idleTimeout_expiresAtExactThreeMinutesWhenNoWorkActive() {
        val start = 10_000L
        val threeMinutesMs = 3L * 60L * 1000L

        assertThat(
            IdleColdStartPolicy.isIdleExpired(
                lastActivityElapsedMs = start,
                nowElapsedMs = start + threeMinutesMs - 1L,
                isWorkActive = false,
            ),
        ).isFalse()

        assertThat(
            IdleColdStartPolicy.remainingIdleTimeoutMs(
                lastActivityElapsedMs = start,
                nowElapsedMs = start + threeMinutesMs - 500L,
                isWorkActive = false,
            ),
        ).isEqualTo(500L)

        assertThat(
            IdleColdStartPolicy.isIdleExpired(
                lastActivityElapsedMs = start,
                nowElapsedMs = start + threeMinutesMs,
                isWorkActive = false,
            ),
        ).isTrue()
    }

    @Test
    fun ongoingPlaybackOrDownloads_neverExpireIdleTimeout() {
        val start = 1_000L
        val tenMinutesLater = start + 10L * 60L * 1000L

        assertThat(
            IdleColdStartPolicy.hasOngoingWork(
                isPlaying = true,
                isBuffering = false,
                isCrossfading = false,
                hasActiveDownloads = false,
            ),
        ).isTrue()
        assertThat(
            IdleColdStartPolicy.hasOngoingWork(
                isPlaying = false,
                isBuffering = false,
                isCrossfading = false,
                hasActiveDownloads = true,
            ),
        ).isTrue()
        assertThat(
            IdleColdStartPolicy.hasOngoingWork(
                isPlaying = false,
                isBuffering = false,
                isCrossfading = false,
                hasActiveDownloads = false,
            ),
        ).isFalse()

        assertThat(
            IdleColdStartPolicy.isIdleExpired(
                lastActivityElapsedMs = start,
                nowElapsedMs = tenMinutesLater,
                isWorkActive = true,
            ),
        ).isFalse()
    }

    @Test
    fun externalIntentForwarding_distinguishesRecentsLauncherAndShortcuts() {
        assertThat(IdleColdStartPolicy.isRecentsHistoryLaunch(null)).isTrue()
        assertThat(IdleColdStartPolicy.shouldForwardExternalIntentOnColdRestart(null)).isFalse()

        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        assertThat(IdleColdStartPolicy.shouldForwardExternalIntentOnColdRestart(launcher)).isFalse()

        val recentsShortcut = Intent("com.lastwave.app.shortcut.SEARCH").apply {
            addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY)
        }
        assertThat(IdleColdStartPolicy.isRecentsHistoryLaunch(recentsShortcut)).isTrue()
        assertThat(IdleColdStartPolicy.shouldForwardExternalIntentOnColdRestart(recentsShortcut)).isFalse()

        val freshShortcut = Intent("com.lastwave.app.shortcut.SEARCH")
        assertThat(IdleColdStartPolicy.isRecentsHistoryLaunch(freshShortcut)).isFalse()
        assertThat(IdleColdStartPolicy.shouldForwardExternalIntentOnColdRestart(freshShortcut)).isTrue()
    }
}
