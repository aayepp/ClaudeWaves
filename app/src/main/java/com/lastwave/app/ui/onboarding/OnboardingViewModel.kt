package com.lastwave.app.ui.onboarding

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastwave.app.data.addon.AddonClient
import com.lastwave.app.data.local.AccentMode
import com.lastwave.app.data.local.HttpAddonConfig
import com.lastwave.app.data.local.MiscSettings
import com.lastwave.app.data.local.SettingsPreferences
import com.lastwave.app.data.spotify.SpotifyPreferences
import com.lastwave.app.data.local.ThemeMode
import com.lastwave.app.data.lossless.LosslessMusicApi
import com.lastwave.app.data.repository.ThemeRepository
import com.lastwave.app.data.repository.ThemeUiState
import com.lastwave.app.data.ytmusic.YtMusicAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** A website that generates a personal addon link; it is not an addon itself. */
internal data class AddonGenerator(val name: String, val url: String)

internal val ONBOARDING_ADDON_GENERATORS = listOf(
    AddonGenerator(name = "Palv", url = "https://addon.palv.org/"),
    AddonGenerator(name = "Radha", url = "https://radha-50047163825.development.catalystappsail.in/"),
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsPreferences: SettingsPreferences,
    private val themeRepository: ThemeRepository,
    ytAuthManager: YtMusicAuthManager,
    spotifyPreferences: SpotifyPreferences,
    private val losslessMusicApi: LosslessMusicApi,
) : ViewModel() {

    val settings: StateFlow<MiscSettings> = settingsPreferences.settings
        .catch { emit(MiscSettings()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MiscSettings())

    val themeState: StateFlow<ThemeUiState> = themeRepository.uiState

    val ytConnected: StateFlow<Boolean> = ytAuthManager.connection
        .map { it.isConnected }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val spotifyConnected: StateFlow<Boolean> = spotifyPreferences.isConnected
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val httpAddons: StateFlow<List<HttpAddonConfig>> = settingsPreferences.httpAddons

    fun saveProfileName(name: String) {
        viewModelScope.launch {
            settingsPreferences.setCustomProfileName(name)
        }
    }

    fun saveProfileAvatar(uri: Uri) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                runCatching {
                    appContext.filesDir.listFiles()
                        ?.filter { it.name.startsWith("profile_avatar_") && it.name.endsWith(".jpg") }
                        ?.forEach { it.delete() }
                    val out = File(appContext.filesDir, "profile_avatar_${System.currentTimeMillis()}.jpg")
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        out.outputStream().use { output -> input.copyTo(output) }
                    } ?: return@runCatching null
                    "file://${out.absolutePath}"
                }.getOrNull()
            }
            if (savedPath != null) {
                settingsPreferences.setCustomProfileAvatarUri(savedPath)
            }
        }
    }

    fun clearProfileAvatar() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    appContext.filesDir.listFiles()
                        ?.filter { it.name.startsWith("profile_avatar_") && it.name.endsWith(".jpg") }
                        ?.forEach { it.delete() }
                }
            }
            settingsPreferences.setCustomProfileAvatarUri(null)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themeRepository.setThemeMode(mode) }
    }

    fun setAmoled(enabled: Boolean) {
        viewModelScope.launch { themeRepository.setAmoled(enabled) }
    }

    fun setComicTheme(enabled: Boolean) {
        viewModelScope.launch { themeRepository.setComicTheme(enabled) }
    }

    fun setLiquidGlass(enabled: Boolean) {
        viewModelScope.launch { themeRepository.setLiquidGlass(enabled) }
    }

    fun setManualAccent(color: Color) {
        viewModelScope.launch {
            themeRepository.setMode(AccentMode.MANUAL)
            themeRepository.setManualAccent(color)
        }
    }

    fun importCustomAddon(name: String, rawUrl: String): Boolean {
        val cleaned = rawUrl.trim()
        if (cleaned.isEmpty()) return false
        val withScheme = if ("://" in cleaned) cleaned else "https://$cleaned"
        val normalized = AddonClient.normalizeBase(withScheme)
        val label = name.trim().ifBlank { Uri.parse(normalized).host ?: "Custom Addon" }
        viewModelScope.launch {
            settingsPreferences.addHttpAddon(normalized, label)
            settingsPreferences.setAddonEnabled(true)
            losslessMusicApi.invalidateCredentialsCache()
        }
        return true
    }

    fun removeAddon(id: String) {
        viewModelScope.launch {
            settingsPreferences.removeHttpAddon(id)
            losslessMusicApi.invalidateCredentialsCache()
        }
    }

    fun setAudioPreset(preferLossless: Boolean, quality: Int) {
        viewModelScope.launch {
            settingsPreferences.setPreferLosslessStreaming(preferLossless)
            settingsPreferences.setWifiStreamingQuality(quality)
            settingsPreferences.setCellularStreamingQuality(if (preferLossless) 256 else quality)
        }
    }

    fun setAutomixEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setCrossfadeEnabled(enabled) }
    }

    fun setSmartOfflineCacheEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsPreferences.setSmartOfflineCacheEnabled(enabled) }
    }

    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            settingsPreferences.setOnboardingCompleted(true)
            onDone()
        }
    }
}
