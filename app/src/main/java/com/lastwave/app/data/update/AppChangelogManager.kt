package com.lastwave.app.data.update

import android.content.Context
import com.lastwave.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class ChangelogUiState(
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val entries: List<ChangelogEntry> = emptyList(),
    val whatsNewEntry: ChangelogEntry? = null,
    val showWhatsNewPopup: Boolean = false,
    val showFullChangelog: Boolean = false,
)

private const val CHANGELOG_PREFS = "lastwave_changelog_prefs"
private const val KEY_LAST_SEEN_VERSION_CODE = "last_seen_changelog_version_code"
private const val CHANGELOG_ASSET_NAME = "CHANGELOG.md"

/**
 * Loads the bundled `CHANGELOG.md` asset and controls both the one-time
 * post-update "What's New" popup and the on-demand Settings changelog viewer.
 */
@Singleton
class AppChangelogManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs by lazy {
        context.getSharedPreferences(CHANGELOG_PREFS, Context.MODE_PRIVATE)
    }

    private val _state = MutableStateFlow(ChangelogUiState())
    val state: StateFlow<ChangelogUiState> = _state.asStateFlow()

    init {
        scope.launch { loadInitial() }
    }

    private fun loadInitial() {
        val entries = readBundledEntries()
        val currentEntry = findChangelogEntryForVersion(entries, BuildConfig.VERSION_NAME)
        val lastSeenCode = prefs.getInt(KEY_LAST_SEEN_VERSION_CODE, -1)
        val shouldShowPopup = currentEntry != null && lastSeenCode < BuildConfig.VERSION_CODE
        _state.update {
            it.copy(
                currentVersion = BuildConfig.VERSION_NAME,
                entries = entries,
                whatsNewEntry = currentEntry,
                showWhatsNewPopup = shouldShowPopup,
            )
        }
    }

    private fun readBundledEntries(): List<ChangelogEntry> =
        runCatching {
            context.assets.open(CHANGELOG_ASSET_NAME).bufferedReader(Charsets.UTF_8).use {
                parseChangelogMarkdown(it.readText())
            }
        }.getOrDefault(emptyList())

    fun openChangelog() {
        if (_state.value.entries.isEmpty()) {
            scope.launch {
                val entries = readBundledEntries()
                _state.update {
                    it.copy(
                        entries = entries,
                        whatsNewEntry = it.whatsNewEntry ?: findChangelogEntryForVersion(entries, BuildConfig.VERSION_NAME),
                        showFullChangelog = true,
                    )
                }
            }
        } else {
            _state.update { it.copy(showFullChangelog = true) }
        }
    }

    fun closeChangelogViewer() {
        _state.update { it.copy(showFullChangelog = false) }
    }

    fun dismissWhatsNew() {
        prefs.edit().putInt(KEY_LAST_SEEN_VERSION_CODE, BuildConfig.VERSION_CODE).apply()
        _state.update { it.copy(showWhatsNewPopup = false) }
    }

    fun openFullChangelogFromWhatsNew() {
        prefs.edit().putInt(KEY_LAST_SEEN_VERSION_CODE, BuildConfig.VERSION_CODE).apply()
        _state.update { it.copy(showWhatsNewPopup = false, showFullChangelog = true) }
    }
}
