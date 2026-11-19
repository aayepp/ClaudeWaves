package com.lastwave.app.data.addon

import com.lastwave.app.data.local.HttpAddonConfig
import com.lastwave.app.data.local.SettingsPreferences
import com.lastwave.app.data.lossless.NativeSecrets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared singleton health cache for configured HTTP addons.
 * Runs automatically once at app launch (from LastWaveApplication) and can be
 * refreshed on demand from Settings › Provider Modules.
 */
@Singleton
class AddonHealthMonitor @Inject constructor(
    private val settingsPreferences: SettingsPreferences,
    private val nativeSecrets: NativeSecrets,
) {
    private val _healthMap = MutableStateFlow<Map<String, AddonHealth>>(emptyMap())
    val healthMap: StateFlow<Map<String, AddonHealth>> = _healthMap.asStateFlow()

    private val _checkingIds = MutableStateFlow<Set<String>>(emptySet())
    val checkingIds: StateFlow<Set<String>> = _checkingIds.asStateFlow()

    fun record(id: String, health: AddonHealth) {
        _healthMap.update { it + (id to health) }
    }

    fun remove(id: String) {
        _healthMap.update { it - id }
    }

    suspend fun checkOne(addon: HttpAddonConfig): AddonHealth {
        if (addon.url.isBlank()) return AddonHealth.Rejected("Addon URL cannot be empty")
        _checkingIds.update { it + addon.id }
        return try {
            val health = AddonClient(addon.url, nativeSecrets = nativeSecrets).health()
            _healthMap.update { it + (addon.id to health) }
            health
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            val err = AddonHealth.Unreachable(t.message ?: "Connection failed")
            _healthMap.update { it + (addon.id to err) }
            err
        } finally {
            _checkingIds.update { it - addon.id }
        }
    }

    /** Checks all configured HTTP addons in parallel; returns (onlineCount, totalCount). */
    suspend fun checkAll(): Pair<Int, Int> {
        val addons = runCatching { settingsPreferences.settings.first().httpAddons }
            .getOrDefault(settingsPreferences.httpAddons.value)
            .filter { it.url.isNotBlank() }
        if (addons.isEmpty()) return 0 to 0
        var online = 0
        coroutineScope {
            addons.map { addon ->
                async {
                    val health = checkOne(addon)
                    if (health is AddonHealth.Ok) online++
                }
            }.forEach { it.await() }
        }
        return online to addons.size
    }
}
