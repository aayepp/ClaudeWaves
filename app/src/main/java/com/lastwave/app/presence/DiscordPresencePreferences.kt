package com.lastwave.app.presence

import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lastwave.app.data.local.readSafely
import com.lastwave.app.data.local.recoverPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class DiscordActivityType(val id: String, val code: Int, val label: String, val prefix: String) {
    LISTENING("listening", 2, "Listening", "Listening to"),
    PLAYING("playing", 0, "Playing", "Playing"),
    WATCHING("watching", 3, "Watching", "Watching"),
    ;

    companion object {
        fun fromId(raw: String?): DiscordActivityType =
            entries.firstOrNull { it.id == raw } ?: LISTENING
    }
}

@Immutable
data class DiscordSettings(
    val enabled: Boolean = true,
    val token: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val showAudioQuality: Boolean = true,
    val showListenButton: Boolean = true,
    val showAppButton: Boolean = true,
    val activityType: DiscordActivityType = DiscordActivityType.LISTENING,
    val presenceStatus: String = "online",
) {
    val isSignedIn: Boolean get() = token.isNotBlank()
}

/**
 * Local-only settings and account cache for Discord Rich Presence.
 *
 * The token stays on this device inside DataStore and is only ever sent directly
 * to Discord's own API/Gateway (`discord.com` / `gateway.discord.gg`).
 */
@Singleton
class DiscordPresencePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val ENABLED = booleanPreferencesKey("lw_discord_rich_presence")
        val TOKEN = stringPreferencesKey("lw_discord_token")
        val USER_ID = stringPreferencesKey("lw_discord_user_id")
        val USERNAME = stringPreferencesKey("lw_discord_username")
        val DISPLAY_NAME = stringPreferencesKey("lw_discord_display_name")
        val AVATAR_URL = stringPreferencesKey("lw_discord_avatar_url")
        val SHOW_AUDIO_QUALITY = booleanPreferencesKey("lw_discord_show_quality")
        val SHOW_LISTEN_BUTTON = booleanPreferencesKey("lw_discord_show_listen_btn")
        val SHOW_APP_BUTTON = booleanPreferencesKey("lw_discord_show_app_btn")
        val ACTIVITY_TYPE = stringPreferencesKey("lw_discord_activity_type")
        val PRESENCE_STATUS = stringPreferencesKey("lw_discord_presence_status")
    }

    val settings: Flow<DiscordSettings> = dataStore.data
        .recoverPreferences("DiscordPresencePreferences")
        .map { prefs ->
            DiscordSettings(
                enabled = prefs.readSafely(Keys.ENABLED) ?: true,
                token = prefs.readSafely(Keys.TOKEN).orEmpty().trim(),
                userId = prefs.readSafely(Keys.USER_ID).orEmpty(),
                username = prefs.readSafely(Keys.USERNAME).orEmpty(),
                displayName = prefs.readSafely(Keys.DISPLAY_NAME).orEmpty(),
                avatarUrl = prefs.readSafely(Keys.AVATAR_URL).orEmpty(),
                showAudioQuality = prefs.readSafely(Keys.SHOW_AUDIO_QUALITY) ?: true,
                showListenButton = prefs.readSafely(Keys.SHOW_LISTEN_BUTTON) ?: true,
                showAppButton = prefs.readSafely(Keys.SHOW_APP_BUTTON) ?: true,
                activityType = DiscordActivityType.fromId(prefs.readSafely(Keys.ACTIVITY_TYPE)),
                presenceStatus = prefs.readSafely(Keys.PRESENCE_STATUS)
                    ?.takeIf { it in VALID_STATUSES } ?: "online",
            )
        }

    val enabled: Flow<Boolean> = settings.map { it.enabled }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ENABLED] = enabled }
    }

    suspend fun saveAccount(
        token: String,
        userId: String,
        username: String,
        displayName: String,
        avatarUrl: String?,
    ) {
        dataStore.edit {
            it[Keys.TOKEN] = token.trim()
            it[Keys.USER_ID] = userId
            it[Keys.USERNAME] = username
            it[Keys.DISPLAY_NAME] = displayName
            it[Keys.AVATAR_URL] = avatarUrl.orEmpty()
            it[Keys.ENABLED] = true
        }
    }

    suspend fun clearAccount() {
        dataStore.edit {
            it.remove(Keys.TOKEN)
            it.remove(Keys.USER_ID)
            it.remove(Keys.USERNAME)
            it.remove(Keys.DISPLAY_NAME)
            it.remove(Keys.AVATAR_URL)
        }
    }

    suspend fun setShowAudioQuality(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_AUDIO_QUALITY] = show }
    }

    suspend fun setShowListenButton(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_LISTEN_BUTTON] = show }
    }

    suspend fun setShowAppButton(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_APP_BUTTON] = show }
    }

    suspend fun setActivityType(type: DiscordActivityType) {
        dataStore.edit { it[Keys.ACTIVITY_TYPE] = type.id }
    }

    suspend fun setPresenceStatus(status: String) {
        if (status !in VALID_STATUSES) return
        dataStore.edit { it[Keys.PRESENCE_STATUS] = status }
    }

    private companion object {
        val VALID_STATUSES = setOf("online", "idle", "dnd")
    }
}
