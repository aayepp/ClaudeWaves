package com.lastwave.app.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * Process-cached hardware capability tier used to shed expensive GPU/recomposition
 * work on entry-level and constrained-RAM devices (e.g. 2–4 GB phones, Unisoc/PowerVR
 * and entry MediaTek SoCs such as Realme RMX3231) without changing functionality.
 */
object DevicePerformanceTier {
    // 4 GB physical RAM devices typically report ~3.6–4.0 GiB totalMem.
    private const val LOW_TIER_RAM_BYTES = 4_500_000_000L

    private val ENTRY_SOC_TOKENS = listOf(
        "sc9863", "sp9863", "sc9832", "sp9832", "ums312",
        "mt6739", "mt6761", "mt6762", "mt6765", "mt6580",
        "msm8917", "msm8937", "qm215",
    )

    @Volatile
    private var cachedLowTier: Boolean? = null

    fun isLowTier(context: Context): Boolean {
        cachedLowTier?.let { return it }
        val detected = detectLowTier(context.applicationContext ?: context)
        cachedLowTier = detected
        return detected
    }

    private fun detectLowTier(context: Context): Boolean {
        val am = runCatching {
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        }.getOrNull()
        if (am?.isLowRamDevice == true) return true

        val totalMem = runCatching {
            val info = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(info)
            info.totalMem
        }.getOrDefault(0L)
        if (totalMem in 1L..LOW_TIER_RAM_BYTES) return true

        if (Runtime.getRuntime().availableProcessors() <= 4) return true

        val socFingerprint = buildString {
            append(Build.HARDWARE.orEmpty())
            append(' ')
            append(Build.BOARD.orEmpty())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                append(' ')
                append(Build.SOC_MODEL.orEmpty())
            }
        }.lowercase()
        return ENTRY_SOC_TOKENS.any { token -> socFingerprint.contains(token) }
    }
}
