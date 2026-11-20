package com.lastwave.app.data.local

import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Full strength refracts and shows busy content through nav chrome; 70% keeps it legible. */
const val DEFAULT_LIQUID_GLASS_INTENSITY = 0.82f

enum class AccentMode(val storageValue: String) {
    MANUAL("manual"),
    DYNAMIC("dynamic"),
    MONOCHROME("monochrome"),
    COMIC("comic");

    companion object {
        fun fromStorage(value: String?): AccentMode =
            entries.firstOrNull { it.storageValue == value } ?: DYNAMIC
    }
}

enum class ThemeMode(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: LIGHT
    }
}

data class ThemePrefs(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val accentColor: String = "#E03030",
    val accentLight: String = "#FF6060",
    val accentMode: AccentMode = AccentMode.DYNAMIC,
    val amoled: Boolean = false,
    /** iOS-style liquid-glass materials. Enabled by default. */
    val liquidGlass: Boolean = true,
    /** 0..1 strength of the glass blur, refraction and sheet translucency. */
    val liquidGlassIntensity: Float = DEFAULT_LIQUID_GLASS_INTENSITY,
    /** Optional compact Material 3 floating dock instead of the standard bottom tabs. */
    val md3NavBar: Boolean = false,
    /** Whether bottom navigation tabs are completely hidden. */
    val hideNavTabs: Boolean = false,
    /** Haptic feedback on taps, buttons, toggles and gestures across the app. */
    val uiHaptics: Boolean = true,
    /** Preferred display refresh rate in Hz; 0 asks for the panel's fastest. */
    val refreshRate: Int = 0,
    /** Dedicated LastWave Signature Theme inspired by the app logo icon (#C9FB00 & #012226). */
    val lastWaveTheme: Boolean = false,
    /** Comic Pop skin (ink, halftone, flat panels). Independent of [accentMode] so a user can
     *  keep the skin and pick their own accent; [AccentMode.COMIC] is the skin's own palette. */
    val comicTheme: Boolean = false,
    val appIcon: AppIcon = AppIcon.DEFAULT,
)

@Singleton
class ThemePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("lw_themeMode")
        val ACCENT_COLOR = stringPreferencesKey("lw_accent")
        val ACCENT_LIGHT = stringPreferencesKey("lw_accentLight")
        val ACCENT_MODE = stringPreferencesKey("lw_accentMode")
        val AMOLED = booleanPreferencesKey("lw_amoled")
        val LIQUID_GLASS = booleanPreferencesKey("lw_liquidGlass")
        val LIQUID_GLASS_INTENSITY = floatPreferencesKey("lw_liquidGlassIntensity")
        val MD3_NAV_BAR = booleanPreferencesKey("lw_md3NavBar")
        val HIDE_NAV_TABS = booleanPreferencesKey("lw_hideNavTabs")
        val UI_HAPTICS = booleanPreferencesKey("lw_ui_haptics")
        val REFRESH_RATE = intPreferencesKey("lw_refresh_rate")
        val LASTWAVE_THEME = booleanPreferencesKey("lw_lastwave_theme")
        val COMIC_THEME = booleanPreferencesKey("lw_comic_theme")
        val COMIC_ACCENT_MODE = stringPreferencesKey("lw_comic_accentMode")
        val COMIC_ACCENT_COLOR = stringPreferencesKey("lw_comic_accent")
        val COMIC_ACCENT_LIGHT = stringPreferencesKey("lw_comic_accentLight")
        val NORMAL_ACCENT_MODE = stringPreferencesKey("lw_normal_accentMode")
        val NORMAL_ACCENT_COLOR = stringPreferencesKey("lw_normal_accent")
        val NORMAL_ACCENT_LIGHT = stringPreferencesKey("lw_normal_accentLight")
        val APP_ICON = stringPreferencesKey("lw_app_icon")
        val LIGHT_DYNAMIC_DEFAULTED = booleanPreferencesKey("lw_light_dynamic_defaulted")
    }

    val prefs: Flow<ThemePrefs> = dataStore.data
        .recoverPreferences("ThemePreferences")
        .map { p ->
            val accentMode = AccentMode.fromStorage(p.readSafely(Keys.ACCENT_MODE))
            ThemePrefs(
                themeMode = ThemeMode.fromStorage(p.readSafely(Keys.THEME_MODE)),
                accentColor = p.readSafely(Keys.ACCENT_COLOR) ?: "#E03030",
                accentLight = p.readSafely(Keys.ACCENT_LIGHT) ?: "#FF6060",
                accentMode = accentMode,
                amoled = p.readSafely(Keys.AMOLED) ?: false,
                liquidGlass = p.readSafely(Keys.LIQUID_GLASS) ?: true,
                liquidGlassIntensity = (p.readSafely(Keys.LIQUID_GLASS_INTENSITY) ?: DEFAULT_LIQUID_GLASS_INTENSITY).coerceIn(0f, 1f),
                md3NavBar = p.readSafely(Keys.MD3_NAV_BAR) ?: false,
                hideNavTabs = p.readSafely(Keys.HIDE_NAV_TABS) ?: false,
                uiHaptics = p.readSafely(Keys.UI_HAPTICS) ?: true,
                refreshRate = p.readSafely(Keys.REFRESH_RATE) ?: 0,
                lastWaveTheme = p.readSafely(Keys.LASTWAVE_THEME) ?: false,
                // Before the skin had its own key, Comic Pop was only an accent mode.
                comicTheme = p.readSafely(Keys.COMIC_THEME) ?: (accentMode == AccentMode.COMIC),
                appIcon = AppIcon.fromId(p.readSafely(Keys.APP_ICON)),
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.storageValue }
    }

    suspend fun setManualAccent(color: String, light: String) {
        dataStore.edit {
            it[Keys.ACCENT_COLOR] = color
            it[Keys.ACCENT_LIGHT] = light
            it[Keys.ACCENT_MODE] = AccentMode.MANUAL.storageValue
            it[Keys.LASTWAVE_THEME] = false
            if (it[Keys.COMIC_THEME] == true) {
                it[Keys.COMIC_ACCENT_MODE] = AccentMode.MANUAL.storageValue
                it[Keys.COMIC_ACCENT_COLOR] = color
                it[Keys.COMIC_ACCENT_LIGHT] = light
            } else {
                it[Keys.NORMAL_ACCENT_MODE] = AccentMode.MANUAL.storageValue
                it[Keys.NORMAL_ACCENT_COLOR] = color
                it[Keys.NORMAL_ACCENT_LIGHT] = light
            }
        }
    }

    suspend fun setMode(mode: AccentMode) {
        dataStore.edit {
            it[Keys.ACCENT_MODE] = mode.storageValue
            if (mode != AccentMode.MANUAL) {
                it[Keys.LASTWAVE_THEME] = false
            }
            if (it[Keys.COMIC_THEME] == true) {
                it[Keys.COMIC_ACCENT_MODE] = mode.storageValue
            } else {
                it[Keys.NORMAL_ACCENT_MODE] = mode.storageValue
            }
        }
    }

    /**
     * Remembers Comic Pop's last picked accent (and the normal theme's last picked accent)
     * so toggling Comic Pop off and back on restores the user's chosen palette instead of resetting.
     */
    suspend fun setComicTheme(enabled: Boolean) {
        dataStore.edit {
            val wasComic = it[Keys.COMIC_THEME] == true
            val currentMode = AccentMode.fromStorage(it[Keys.ACCENT_MODE])
            it[Keys.COMIC_THEME] = enabled
            if (enabled) {
                if (!wasComic && currentMode != AccentMode.COMIC) {
                    it[Keys.NORMAL_ACCENT_MODE] = currentMode.storageValue
                    it[Keys.ACCENT_COLOR]?.let { c -> it[Keys.NORMAL_ACCENT_COLOR] = c }
                    it[Keys.ACCENT_LIGHT]?.let { l -> it[Keys.NORMAL_ACCENT_LIGHT] = l }
                }
                val savedComicMode = it[Keys.COMIC_ACCENT_MODE]?.let(AccentMode::fromStorage) ?: AccentMode.COMIC
                it[Keys.ACCENT_MODE] = savedComicMode.storageValue
                if (savedComicMode == AccentMode.MANUAL) {
                    it[Keys.COMIC_ACCENT_COLOR]?.let { c -> it[Keys.ACCENT_COLOR] = c }
                    it[Keys.COMIC_ACCENT_LIGHT]?.let { l -> it[Keys.ACCENT_LIGHT] = l }
                }
                it[Keys.LASTWAVE_THEME] = false
            } else {
                if (wasComic) {
                    it[Keys.COMIC_ACCENT_MODE] = currentMode.storageValue
                    it[Keys.ACCENT_COLOR]?.let { c -> it[Keys.COMIC_ACCENT_COLOR] = c }
                    it[Keys.ACCENT_LIGHT]?.let { l -> it[Keys.COMIC_ACCENT_LIGHT] = l }
                }
                val savedNormalMode = it[Keys.NORMAL_ACCENT_MODE]?.let(AccentMode::fromStorage)
                if (savedNormalMode != null && savedNormalMode != AccentMode.COMIC) {
                    it[Keys.ACCENT_MODE] = savedNormalMode.storageValue
                    if (savedNormalMode == AccentMode.MANUAL) {
                        it[Keys.NORMAL_ACCENT_COLOR]?.let { c -> it[Keys.ACCENT_COLOR] = c }
                        it[Keys.NORMAL_ACCENT_LIGHT]?.let { l -> it[Keys.ACCENT_LIGHT] = l }
                    }
                } else if (currentMode == AccentMode.COMIC) {
                    it[Keys.ACCENT_MODE] = AccentMode.MONOCHROME.storageValue
                }
            }
        }
    }

    suspend fun setAmoled(enabled: Boolean) {
        dataStore.edit { it[Keys.AMOLED] = enabled }
    }

    suspend fun setLiquidGlass(enabled: Boolean) {
        dataStore.edit { it[Keys.LIQUID_GLASS] = enabled }
    }

    suspend fun setLiquidGlassIntensity(intensity: Float) {
        dataStore.edit { it[Keys.LIQUID_GLASS_INTENSITY] = intensity.coerceIn(0f, 1f) }
    }

    suspend fun setMd3NavBar(enabled: Boolean) {
        dataStore.edit { it[Keys.MD3_NAV_BAR] = enabled }
    }

    suspend fun setHideNavTabs(enabled: Boolean) {
        dataStore.edit { it[Keys.HIDE_NAV_TABS] = enabled }
    }

    suspend fun setRefreshRate(hz: Int) {
        dataStore.edit { it[Keys.REFRESH_RATE] = hz }
    }

    suspend fun setUiHaptics(enabled: Boolean) {
        dataStore.edit { it[Keys.UI_HAPTICS] = enabled }
    }

    suspend fun setLastWaveTheme(enabled: Boolean) {
        dataStore.edit {
            if (enabled && it[Keys.COMIC_THEME] == true) {
                it[Keys.COMIC_ACCENT_MODE] = AccentMode.fromStorage(it[Keys.ACCENT_MODE]).storageValue
                it[Keys.ACCENT_COLOR]?.let { c -> it[Keys.COMIC_ACCENT_COLOR] = c }
                it[Keys.ACCENT_LIGHT]?.let { l -> it[Keys.COMIC_ACCENT_LIGHT] = l }
            }
            it[Keys.LASTWAVE_THEME] = enabled
            if (enabled) {
                it[Keys.COMIC_THEME] = false
                it[Keys.ACCENT_COLOR] = "#C9FB00"
                it[Keys.ACCENT_LIGHT] = "#C9FB00"
                it[Keys.ACCENT_MODE] = AccentMode.MANUAL.storageValue
            }
        }
    }

    suspend fun setAppIcon(icon: AppIcon) {
        dataStore.edit { it[Keys.APP_ICON] = icon.id }
    }

    /** Old builds force-wrote Comic Pop into every install, so the new
     *  light + dynamic default never reached them. Move them over once;
     *  later Settings choices stick. */
    suspend fun ensureLightDynamicDefaulted() {
        dataStore.edit {
            if (it[Keys.LIGHT_DYNAMIC_DEFAULTED] != true) {
                it[Keys.LIGHT_DYNAMIC_DEFAULTED] = true
                it[Keys.THEME_MODE] = ThemeMode.LIGHT.storageValue
                it[Keys.ACCENT_MODE] = AccentMode.DYNAMIC.storageValue
                it[Keys.LASTWAVE_THEME] = false
            }
        }
    }
}
