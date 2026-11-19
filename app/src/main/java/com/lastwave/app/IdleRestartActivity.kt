package com.lastwave.app

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process

/**
 * Lightweight helper Activity running in `:coldstart` so the main app process
 * can terminate completely and relaunch cleanly from Android SplashScreen +
 * `LastWaveApplication.onCreate()` without OEM Task/Process reuse.
 */
class IdleRestartActivity : Activity() {

    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile
    private var cancelled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IdleColdStartCoordinator.disableTransition(this)

        val rawTarget = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_TARGET_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_TARGET_INTENT)
        }
        val targetIntent = rawTarget?.takeIf { candidate ->
            val pkg = candidate.component?.packageName ?: candidate.`package`
            pkg == null || pkg == packageName
        } ?: IdleColdStartPolicy.resolveCleanLauncherIntent(this)

        val oldPid = intent.getIntExtra(EXTRA_OLD_PID, -1)

        Thread({
            waitForProcessExit(oldPid)
            if (cancelled) return@Thread
            mainHandler.post {
                if (isFinishing || isDestroyed) return@post
                runCatching { startActivity(targetIntent) }
                IdleColdStartCoordinator.disableTransition(this)
                finish()
                mainHandler.post { Process.killProcess(Process.myPid()) }
            }
        }, "lastwave-cold-restart").apply {
            isDaemon = true
            start()
        }
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Ignore accidental back presses during the <150ms cold restart handoff.
    }

    override fun onDestroy() {
        cancelled = true
        super.onDestroy()
    }

    private fun waitForProcessExit(oldPid: Int) {
        if (oldPid <= 0) {
            Thread.sleep(MIN_HANDOFF_MS)
            return
        }
        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        var waitedMs = 0L
        Thread.sleep(MIN_HANDOFF_MS)
        waitedMs += MIN_HANDOFF_MS
        while (waitedMs < MAX_WAIT_MS && !cancelled) {
            val alive = runCatching {
                am?.runningAppProcesses?.any { it.pid == oldPid } == true
            }.getOrDefault(false)
            if (!alive) break
            if (waitedMs >= FORCE_SIGNAL_AT_MS) {
                runCatching { Process.killProcess(oldPid) }
            }
            Thread.sleep(POLL_STEP_MS)
            waitedMs += POLL_STEP_MS
        }
    }

    companion object {
        const val EXTRA_TARGET_INTENT = "com.lastwave.app.extra.IDLE_COLD_TARGET_INTENT"
        const val EXTRA_OLD_PID = "com.lastwave.app.extra.IDLE_COLD_OLD_PID"
        private const val MIN_HANDOFF_MS = 60L
        private const val POLL_STEP_MS = 20L
        private const val FORCE_SIGNAL_AT_MS = 220L
        private const val MAX_WAIT_MS = 500L
    }
}
