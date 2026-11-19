package com.lastwave.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.os.SystemClock
import com.lastwave.app.data.download.TrackDownloadManager
import com.lastwave.app.diagnostics.AppLog
import com.lastwave.app.diagnostics.Diagnostics
import com.lastwave.app.playback.MusicPlayer
import com.lastwave.app.playback.stopAndFlushForIdleReset
import com.lastwave.app.util.AppVisibility
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pure policy helpers for determining when the app has been inactive long enough
 * to warrant a fresh cold start on the next open.
 */
object IdleColdStartPolicy {
    const val IDLE_COLD_START_TIMEOUT_MS: Long = 3L * 60L * 1000L

    fun hasOngoingWork(
        isPlaying: Boolean,
        isBuffering: Boolean,
        isCrossfading: Boolean,
        hasActiveDownloads: Boolean,
    ): Boolean = isPlaying || isBuffering || isCrossfading || hasActiveDownloads

    fun remainingIdleTimeoutMs(
        lastActivityElapsedMs: Long,
        nowElapsedMs: Long,
        isWorkActive: Boolean,
        timeoutMs: Long = IDLE_COLD_START_TIMEOUT_MS,
    ): Long {
        if (isWorkActive) return timeoutMs
        val elapsed = (nowElapsedMs - lastActivityElapsedMs).coerceAtLeast(0L)
        return (timeoutMs - elapsed).coerceAtLeast(0L)
    }

    fun isIdleExpired(
        lastActivityElapsedMs: Long,
        nowElapsedMs: Long,
        isWorkActive: Boolean,
        timeoutMs: Long = IDLE_COLD_START_TIMEOUT_MS,
    ): Boolean = remainingIdleTimeoutMs(
        lastActivityElapsedMs = lastActivityElapsedMs,
        nowElapsedMs = nowElapsedMs,
        isWorkActive = isWorkActive,
        timeoutMs = timeoutMs,
    ) == 0L

    fun isRecentsHistoryLaunch(intent: Intent?): Boolean {
        if (intent == null) return true
        return (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
    }

    fun shouldForwardExternalIntentOnColdRestart(intent: Intent?): Boolean {
        if (isRecentsHistoryLaunch(intent)) return false
        val target = intent ?: return false
        val isStandardLauncherMain = target.action == Intent.ACTION_MAIN &&
            target.hasCategory(Intent.CATEGORY_LAUNCHER)
        return !isStandardLauncherMain
    }

    fun resolveCleanLauncherIntent(context: Context): Intent {
        val launcher = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.let { Intent(it) }
            ?: Intent(context, MainShellActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
        launcher.setPackage(context.packageName)
        launcher.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        return launcher
    }

    fun resolveCurrentProcessName(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName()
        }
        return runCatching {
            FileInputStream("/proc/self/cmdline").use { input ->
                val buffer = ByteArray(128)
                val read = input.read(buffer)
                if (read > 0) String(buffer, 0, read, Charsets.UTF_8).trimEnd('\u0000')
                else context.packageName
            }
        }.getOrDefault(context.packageName)
    }

    fun isHelperRestartProcess(
        context: Context,
        processName: String = resolveCurrentProcessName(context),
    ): Boolean = processName.startsWith("${context.packageName}:")
}

/**
 * Coordinates the 3-minute inactivity lifecycle:
 * - While music is playing/buffering/crossfading or offline downloads are running,
 *   the app is considered active and never times out.
 * - While backgrounded with no ongoing playback/downloads, once 3 minutes elapse
 *   since the last user interaction or playback/download completion:
 *   1. If the background timer fires, idle paused playback state is persisted,
 *      [MusicPlaybackService] stops cleanly, a pending cold-reset marker is saved,
 *      and the idle process exits so the next launch starts completely fresh.
 *   2. If the OS froze the background process before the timer ran, reopening
 *      [MainShellActivity] sees `elapsedRealtime - lastActivity >= 3 min` and
 *      immediately hands off to [IdleRestartActivity] (`:coldstart`) to spawn
 *      a fresh process from Android Splash + `ComicSplash`.
 */
@Singleton
class IdleColdStartCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val applicationScope: CoroutineScope,
    private val musicPlayer: dagger.Lazy<MusicPlayer>,
    private val trackDownloadManager: dagger.Lazy<TrackDownloadManager>,
) {
    private val started = AtomicBoolean(false)
    private val lastActivityElapsedMs = AtomicLong(SystemClock.elapsedRealtime())
    private val playbackActive = AtomicBoolean(false)
    private val downloadsActive = AtomicBoolean(false)
    private val shuttingDownOrRestarting = AtomicBoolean(false)
    private var timerJob: Job? = null

    fun start() {
        if (!started.compareAndSet(false, true)) return
        lastActivityElapsedMs.set(SystemClock.elapsedRealtime())

        applicationScope.launch(Dispatchers.Main.immediate) {
            AppVisibility.visibleFlow.collect { visible ->
                if (visible) {
                    timerJob?.cancel()
                    timerJob = null
                } else {
                    scheduleBackgroundCheckIfNeeded()
                }
            }
        }

        applicationScope.launch {
            delay(OBSERVER_WARMUP_DELAY_MS)
            launch(Dispatchers.Main.immediate) {
                runCatching {
                    musicPlayer.get().state
                        .map { it.isPlaying || it.isBuffering || it.isCrossfading }
                        .distinctUntilChanged()
                        .collect { active ->
                            val wasActive = playbackActive.getAndSet(active)
                            if (active) {
                                timerJob?.cancel()
                                timerJob = null
                                lastActivityElapsedMs.set(SystemClock.elapsedRealtime())
                            } else if (wasActive) {
                                lastActivityElapsedMs.set(SystemClock.elapsedRealtime())
                                if (!AppVisibility.isVisible) {
                                    scheduleBackgroundCheckIfNeeded()
                                }
                            }
                        }
                }
            }
            launch(Dispatchers.Main.immediate) {
                runCatching {
                    trackDownloadManager.get().downloads
                        .map { map -> map.values.any { !it.isFinished && it.error == null } }
                        .distinctUntilChanged()
                        .collect { active ->
                            val wasActive = downloadsActive.getAndSet(active)
                            if (active) {
                                timerJob?.cancel()
                                timerJob = null
                                lastActivityElapsedMs.set(SystemClock.elapsedRealtime())
                            } else if (wasActive) {
                                lastActivityElapsedMs.set(SystemClock.elapsedRealtime())
                                if (!AppVisibility.isVisible) {
                                    scheduleBackgroundCheckIfNeeded()
                                }
                            }
                        }
                }
            }
        }
    }

    fun noteUserActivity() {
        if (shuttingDownOrRestarting.get()) return
        lastActivityElapsedMs.set(SystemClock.elapsedRealtime())
        if (!AppVisibility.isVisible) {
            scheduleBackgroundCheckIfNeeded()
        }
    }

    fun noteForegroundVisible() {
        if (shuttingDownOrRestarting.get()) return
        timerJob?.cancel()
        timerJob = null
        lastActivityElapsedMs.set(SystemClock.elapsedRealtime())
    }

    /**
     * Checks when [MainShellActivity] returns to foreground whether >= 3 minutes
     * have passed without playback/downloads/activity. When expired, restarts the
     * app into a brand-new OS process via [IdleRestartActivity] and returns `true`.
     */
    fun checkAndTriggerColdRestartOnReturn(activity: Activity, incomingIntent: Intent?): Boolean {
        if (shuttingDownOrRestarting.get()) return true
        val workActive = isOngoingWorkCurrentlyActive()
        if (workActive) {
            lastActivityElapsedMs.set(SystemClock.elapsedRealtime())
            return false
        }
        val expired = IdleColdStartPolicy.isIdleExpired(
            lastActivityElapsedMs = lastActivityElapsedMs.get(),
            nowElapsedMs = SystemClock.elapsedRealtime(),
            isWorkActive = false,
        )
        if (!expired) return false
        triggerColdRestart(activity, incomingIntent)
        return true
    }

    private fun isOngoingWorkCurrentlyActive(): Boolean {
        val playerState = runCatching { musicPlayer.get().state.value }.getOrNull()
        val livePlayback = playerState?.let {
            it.isPlaying || it.isBuffering || it.isCrossfading
        } ?: playbackActive.get()
        val liveDownloads = runCatching {
            trackDownloadManager.get().downloads.value.values.any { !it.isFinished && it.error == null }
        }.getOrDefault(downloadsActive.get())
        return IdleColdStartPolicy.hasOngoingWork(
            isPlaying = livePlayback,
            isBuffering = false,
            isCrossfading = false,
            hasActiveDownloads = liveDownloads,
        )
    }

    private fun scheduleBackgroundCheckIfNeeded() {
        if (shuttingDownOrRestarting.get()) return
        if (AppVisibility.isVisible) return
        if (playbackActive.get() || downloadsActive.get()) return
        timerJob?.cancel()
        val remainingMs = IdleColdStartPolicy.remainingIdleTimeoutMs(
            lastActivityElapsedMs = lastActivityElapsedMs.get(),
            nowElapsedMs = SystemClock.elapsedRealtime(),
            isWorkActive = false,
        )
        timerJob = applicationScope.launch(Dispatchers.Default) {
            if (remainingMs > 0L) {
                delay(remainingMs)
            }
            if (!AppVisibility.isVisible && !isOngoingWorkCurrentlyActive()) {
                val expired = IdleColdStartPolicy.isIdleExpired(
                    lastActivityElapsedMs = lastActivityElapsedMs.get(),
                    nowElapsedMs = SystemClock.elapsedRealtime(),
                    isWorkActive = false,
                )
                if (expired) {
                    performBackgroundIdleShutdown()
                } else {
                    withContext(Dispatchers.Main.immediate) {
                        scheduleBackgroundCheckIfNeeded()
                    }
                }
            }
        }
    }

    private suspend fun performBackgroundIdleShutdown() {
        withContext(Dispatchers.Main.immediate) {
            if (AppVisibility.isVisible || isOngoingWorkCurrentlyActive()) return@withContext
            if (!shuttingDownOrRestarting.compareAndSet(false, true)) return@withContext
            AppLog.i(TAG, "Idle >= 3 min in background without active playback; exiting for next cold start")
            runCatching { musicPlayer.get().stopAndFlushForIdleReset() }
            writeColdRestartPending(context, pending = true)
            Diagnostics.markCleanShutdown(context)
            AppLog.flush()
            Process.killProcess(Process.myPid())
        }
    }

    private fun triggerColdRestart(activity: Activity, incomingIntent: Intent?) {
        if (!shuttingDownOrRestarting.compareAndSet(false, true)) return
        timerJob?.cancel()
        timerJob = null
        AppLog.i(TAG, "Returning after >= 3 min idle; triggering cold restart")
        runCatching { musicPlayer.get().stopAndFlushForIdleReset() }
        writeColdRestartPending(context, pending = false)

        val targetIntent = if (IdleColdStartPolicy.shouldForwardExternalIntentOnColdRestart(incomingIntent)) {
            Intent(incomingIntent).apply {
                setPackage(context.packageName)
                flags = (flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY.inv()) or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        } else {
            IdleColdStartPolicy.resolveCleanLauncherIntent(context)
        }

        val trampoline = Intent(activity, IdleRestartActivity::class.java).apply {
            putExtra(IdleRestartActivity.EXTRA_TARGET_INTENT, targetIntent)
            putExtra(IdleRestartActivity.EXTRA_OLD_PID, Process.myPid())
            addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        Diagnostics.markCleanShutdown(context)
        AppLog.flush()
        activity.startActivity(trampoline)
        disableTransition(activity)
        activity.finishAffinity()
        Process.killProcess(Process.myPid())
    }

    companion object {
        private const val TAG = "IdleColdStart"
        private const val PREFS_NAME = "lastwave_idle_lifecycle"
        private const val KEY_COLD_RESTART_PENDING = "cold_restart_pending"
        private const val OBSERVER_WARMUP_DELAY_MS = 2_500L

        internal fun disableTransition(activity: Activity) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
                    activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
                } else {
                    @Suppress("DEPRECATION")
                    activity.overridePendingTransition(0, 0)
                }
            }
        }

        private fun writeColdRestartPending(context: Context, pending: Boolean) {
            runCatching {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_COLD_RESTART_PENDING, pending)
                    .commit()
            }
        }

        fun consumeColdRestartPending(context: Context): Boolean = runCatching {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_COLD_RESTART_PENDING, false)) return@runCatching false
            prefs.edit().putBoolean(KEY_COLD_RESTART_PENDING, false).commit()
            true
        }.getOrDefault(false)
    }
}
