package com.lastwave.app.diagnostics

import android.content.Context

/**
 * Assembles the whole diagnostics subsystem, in the only order that works.
 *
 * Order matters more than it looks:
 *
 * 1. The previous session's [SessionMarker] is read *before* this session
 *    overwrites it, otherwise a clean launch would erase the evidence that the
 *    last one died.
 * 2. [AppLog] installs before anything can report a failure, so the rest of
 *    startup is already covered.
 * 3. [ExitReasonReader] runs after logging exists, because its findings are
 *    themselves crash records.
 *
 * Installed from `Application.attachBaseContext`, which is the earliest point
 * where a Context exists — ContentProviders are created before `onCreate`, and
 * failures in that phase were previously invisible.
 */
object Diagnostics {

    private const val TAG = "Diagnostics"

    @Volatile
    private var watchdog: AnrWatchdog? = null

    fun install(context: Context) {
        runCatching {
            val uncleanSession = SessionMarker.takeUncleanSession(context)
            SessionMarker.markRunning(context)

            AppLog.install(context)
            CrashHandler.install(context)

            AppLog.i(TAG, "Diagnostics starting (pid=${android.os.Process.myPid()})")

            // Off the startup path: the exit-history binder query, and after a crash
            // decoding its tombstone or ANR trace, ran here before the first frame.
            Thread({ recordPreviousSessionEnd(context, uncleanSession) }, "lastwave-exit-reasons").apply {
                isDaemon = true
                start()
            }

            startAnrWatchdog()
        }.onFailure {
            android.util.Log.e(TAG, "Diagnostics unavailable; continuing without it", it)
        }
    }

    private fun recordPreviousSessionEnd(context: Context, uncleanSession: String?) {
        runCatching {
            // A native crash, a low-memory kill, or a system-recorded ANR leave
            // no Java stack trace for the crash handler to see. Android 11+
            // records them itself and this surfaces them as crash files.
            val exitResult = runCatching { ExitReasonReader.recordHistoricalExits(context) }
                .getOrDefault(ExitReasonReader.HistoricalExitsResult())

            if (uncleanSession != null) {
                val previousPid = parsePid(uncleanSession)
                val wasPlaying = uncleanSession.contains("playing=true")
                val wasVisible = uncleanSession.contains("uiVisible=true")
                when {
                    previousPid != null && previousPid in exitResult.recordedCrashPids -> {
                        AppLog.i(TAG, "Previous session pid=$previousPid already recorded via ExitReasonReader; skipping UNKNOWN_TERMINATION")
                    }
                    previousPid != null && previousPid in exitResult.benignExitPids -> {
                        AppLog.i(TAG, "Previous session pid=$previousPid ended via benign OS/user exit; skipping UNKNOWN_TERMINATION")
                    }
                    !wasPlaying && !wasVisible -> {
                        AppLog.i(TAG, "Previous background idle session ended without clean marker ($uncleanSession); skipping UNKNOWN_TERMINATION")
                    }
                    else -> {
                        // Active playback or visible UI stopped unexpectedly with no OS exit record.
                        CrashRecorder.record(
                            type = CrashType.UNKNOWN_TERMINATION,
                            detail = buildString {
                                appendLine("The previous session ended without a clean-shutdown marker.")
                                appendLine("The system may have killed the process, or the app may have been")
                                appendLine("swiped from recents. Recorded marker: $uncleanSession")
                                if (wasPlaying) {
                                    appendLine("Music WAS playing: the system stopped an active playback session.")
                                }
                            },
                        )
                    }
                }
            }
        }.onFailure {
            android.util.Log.e(TAG, "Previous session exit records unavailable", it)
        }
    }

    private fun parsePid(marker: String): Int? =
        Regex("""\bpid=(\d+)\b""").find(marker)?.groupValues?.getOrNull(1)?.toIntOrNull()

    /**
     * Marks the session as ended deliberately. Called when the last activity
     * finishes for good — not on a configuration change.
     */
    fun markCleanShutdown(context: Context) {
        runCatching { SessionMarker.markClean(context) }
    }

    /** Writes everything queued right now. Called when the app goes to the background. */
    fun flush() {
        runCatching { AppLog.flush() }
    }

    private fun startAnrWatchdog() {
        if (watchdog != null) return
        val started = AnrWatchdog { stalledMillis, mainStack ->
            runCatching {
                AppLog.e(TAG, "Main thread stalled for ${stalledMillis}ms")
                CrashRecorder.record(
                    type = CrashType.ANR,
                    detail = buildString {
                        appendLine("Main thread unresponsive for ${stalledMillis}ms.")
                        appendLine()
                        appendLine("--- main thread stack ---")
                        mainStack.forEach { appendLine("  at $it") }
                    },
                )
            }
        }
        watchdog = started
        started.start()
    }
}