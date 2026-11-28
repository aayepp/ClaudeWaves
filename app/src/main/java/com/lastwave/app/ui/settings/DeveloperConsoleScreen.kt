package com.lastwave.app.ui.settings

import android.app.Activity
import android.app.KeyguardManager
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import com.lastwave.app.ui.theme.sakugaTypingInput
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lastwave.app.data.update.AppStatsManager
import com.lastwave.app.data.update.BroadcastDisplayMode
import com.lastwave.app.data.update.BroadcastManager
import com.lastwave.app.data.update.BroadcastMessage
import com.lastwave.app.data.update.BroadcastType
import com.lastwave.app.data.update.DevLockState
import com.lastwave.app.data.update.DeveloperFeedbackSnapshot
import com.lastwave.app.data.update.DeveloperStatsSnapshot
import com.lastwave.app.data.update.FeedbackCategory
import com.lastwave.app.data.update.UserFeedbackItem
import com.lastwave.app.data.update.formatBroadcastDate
import com.lastwave.app.diagnostics.AppLog
import com.lastwave.app.diagnostics.CrashRecorder
import com.lastwave.app.ui.player.LocalMiniPlayerScrollClearance
import com.lastwave.app.ui.comic.AlertDialog
import com.lastwave.app.ui.comic.Button
import com.lastwave.app.ui.comic.FilterChip
import com.lastwave.app.ui.comic.OutlinedButton
import com.lastwave.app.ui.comic.Surface
import com.lastwave.app.ui.comic.Switch
import com.lastwave.app.ui.comic.TextButton
import com.lastwave.app.ui.common.ConnectedButtonGroup
import com.lastwave.app.ui.common.ConnectedButtonItem
import com.lastwave.app.ui.common.ExpressiveHeader
import com.lastwave.app.ui.common.GlassHeaderLayout
import com.lastwave.app.ui.common.HeaderActionIcon
import com.lastwave.app.ui.common.adaptiveContentWidth
import com.lastwave.app.ui.theme.ComicAccent
import com.lastwave.app.ui.theme.ComicInk
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.glassCardBorder
import com.lastwave.app.ui.theme.glassCardColor
import com.lastwave.app.ui.theme.pageBackground
import com.lastwave.app.ui.update.BroadcastDialog
import com.lastwave.app.ui.update.HomeBroadcastTickerPill
import com.lastwave.app.ui.update.accent
import com.lastwave.app.ui.update.icon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

internal data class DeveloperConsoleUiState(
    val hasToken: Boolean = false,
    val ownerLogin: String? = null,
    val isUnlocked: Boolean = false,
    val lockState: DevLockState = DevLockState.Open(remainingAttempts = 3, isFinalChanceAfterCooldown = false),
    val openBroadcasts: List<BroadcastMessage> = emptyList(),
    val broadcastHistory: List<BroadcastMessage> = emptyList(),
    val isBusy: Boolean = false,
    val authError: String? = null,
    val broadcastStatus: String? = null,
    val broadcastStatusIsError: Boolean = false,
    val stats: DeveloperStatsSnapshot? = null,
    val isLoadingStats: Boolean = false,
    val pingFeedback: String? = null,
    val feedback: DeveloperFeedbackSnapshot? = null,
    val isLoadingFeedback: Boolean = false,
    val feedbackBusyId: Long? = null,
    val creatingIssueIds: Set<Long> = emptySet(),
    val createdIssues: Map<Long, Pair<Int, String>> = emptyMap(),
    val issueErrorById: Map<Long, String> = emptyMap(),
    val devLanguage: DevLanguage = DevLanguage.BM,
    val translateTarget: DevLanguage = DevLanguage.BM,
    val autoTranslateEnabled: Boolean = false,
    val translations: Map<Pair<Long, DevLanguage>, FeedbackTranslation> = emptyMap(),
    val translatingKeys: Set<Pair<Long, DevLanguage>> = emptySet(),
)

@HiltViewModel
class DeveloperConsoleViewModel @Inject constructor(
    private val manager: BroadcastManager,
    private val statsManager: AppStatsManager,
) : ViewModel() {
    private val initialLang = DevLanguage.fromCode(manager.getDevLanguageCode())
    private val _uiState = MutableStateFlow(
        DeveloperConsoleUiState(
            hasToken = manager.hasOwnerToken(),
            ownerLogin = manager.savedOwnerLogin(),
            lockState = manager.getDevLockState(),
            devLanguage = initialLang,
            translateTarget = initialLang,
        ),
    )
    internal val uiState: StateFlow<DeveloperConsoleUiState> = _uiState.asStateFlow()

    internal fun setDevLanguage(lang: DevLanguage) {
        manager.setDevLanguageCode(lang.code)
        _uiState.update { it.copy(devLanguage = lang, translateTarget = lang) }
        if (_uiState.value.autoTranslateEnabled) {
            translatePendingFeedback(_uiState.value.feedback?.items.orEmpty(), lang)
        }
    }

    internal fun setTranslateTarget(lang: DevLanguage) {
        _uiState.update { it.copy(translateTarget = lang) }
        if (_uiState.value.autoTranslateEnabled) {
            translatePendingFeedback(_uiState.value.feedback?.items.orEmpty(), lang)
        }
    }

    fun refreshLockState() {
        _uiState.update {
            it.copy(
                hasToken = manager.hasOwnerToken(),
                ownerLogin = manager.savedOwnerLogin(),
                lockState = manager.getDevLockState(),
            )
        }
    }

    fun unlockAfterDeviceAuth() {
        if (!manager.hasOwnerToken()) return
        _uiState.update {
            it.copy(
                hasToken = true,
                ownerLogin = manager.savedOwnerLogin(),
                isUnlocked = true,
                lockState = manager.getDevLockState(),
                authError = null,
            )
        }
        refreshAll()
    }

    fun lockSession() {
        _uiState.update {
            it.copy(
                isUnlocked = false,
                openBroadcasts = emptyList(),
                broadcastHistory = emptyList(),
                stats = null,
                isLoadingStats = false,
                pingFeedback = null,
                feedback = null,
                isLoadingFeedback = false,
                feedbackBusyId = null,
                creatingIssueIds = emptySet(),
                authError = null,
                lockState = manager.getDevLockState(),
            )
        }
    }

    fun verifyAndSignIn(rawToken: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, authError = null) }
            val result = manager.saveAndVerifyOwnerToken(rawToken)
            result.onSuccess { login ->
                _uiState.update {
                    it.copy(
                        hasToken = true,
                        ownerLogin = login,
                        isUnlocked = true,
                        lockState = manager.getDevLockState(),
                        isBusy = false,
                        authError = null,
                    )
                }
                refreshAll()
                onSuccess()
            }.onFailure { err ->
                val bm = _uiState.value.devLanguage == DevLanguage.BM
                _uiState.update {
                    it.copy(
                        hasToken = manager.hasOwnerToken(),
                        ownerLogin = manager.savedOwnerLogin(),
                        isUnlocked = false,
                        lockState = manager.getDevLockState(),
                        isBusy = false,
                        authError = err.message ?: if (bm) "Gagal sahkan token" else "Failed to verify token",
                    )
                }
            }
        }
    }

    fun hideShortcutFromSettings() {
        manager.hideDeveloperShortcut()
    }

    fun signOut() {
        manager.clearOwnerToken()
        _uiState.update {
            it.copy(
                hasToken = false,
                ownerLogin = null,
                isUnlocked = false,
                openBroadcasts = emptyList(),
                broadcastHistory = emptyList(),
                stats = null,
                isLoadingStats = false,
                pingFeedback = null,
                feedback = null,
                isLoadingFeedback = false,
                feedbackBusyId = null,
                creatingIssueIds = emptySet(),
                createdIssues = emptyMap(),
                issueErrorById = emptyMap(),
                authError = null,
                broadcastStatus = null,
                broadcastStatusIsError = false,
                autoTranslateEnabled = false,
                translations = emptyMap(),
                translatingKeys = emptySet(),
                lockState = manager.getDevLockState(),
            )
        }
    }

    fun refreshAll() {
        if (!_uiState.value.isUnlocked) return
        viewModelScope.launch {
            manager.fetchOpenBroadcasts().onSuccess { list ->
                if (_uiState.value.isUnlocked) {
                    _uiState.update { it.copy(openBroadcasts = list) }
                }
            }
            manager.fetchBroadcastHistory(limit = 10).onSuccess { hist ->
                if (_uiState.value.isUnlocked) {
                    _uiState.update { it.copy(broadcastHistory = hist) }
                }
            }
        }
        refreshStats()
        refreshFeedback()
    }

    fun refreshStats() {
        if (!_uiState.value.isUnlocked) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStats = true) }
            val snapshot = statsManager.fetchDeveloperStats()
            if (_uiState.value.isUnlocked) {
                _uiState.update { it.copy(stats = snapshot, isLoadingStats = false) }
            }
        }
    }

    fun refreshFeedback() {
        if (!_uiState.value.isUnlocked) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFeedback = true) }
            val snapshot = statsManager.fetchDeveloperFeedback()
            if (_uiState.value.isUnlocked) {
                _uiState.update { it.copy(feedback = snapshot, isLoadingFeedback = false) }
                val state = _uiState.value
                if (state.autoTranslateEnabled) {
                    translatePendingFeedback(snapshot.items, state.translateTarget)
                }
            }
        }
    }

    fun toggleAutoTranslate() {
        if (!_uiState.value.isUnlocked) return
        val next = !_uiState.value.autoTranslateEnabled
        _uiState.update { it.copy(autoTranslateEnabled = next) }
        if (next) {
            val state = _uiState.value
            translatePendingFeedback(state.feedback?.items.orEmpty(), state.translateTarget)
        }
    }

    internal fun translateFeedback(
        item: UserFeedbackItem,
        targetLang: DevLanguage = _uiState.value.translateTarget,
    ) {
        if (!_uiState.value.isUnlocked) return
        val state = _uiState.value
        val key = item.id to targetLang
        if (state.translations.containsKey(key) || key in state.translatingKeys) return
        viewModelScope.launch {
            _uiState.update { it.copy(translatingKeys = it.translatingKeys + key) }
            val result = FeedbackTranslator.translate(item.title, item.body, targetLang)
            _uiState.update { cur ->
                cur.copy(
                    translatingKeys = cur.translatingKeys - key,
                    translations = if (result != null && cur.isUnlocked) {
                        cur.translations + (key to result)
                    } else {
                        cur.translations
                    },
                )
            }
        }
    }

    private fun translatePendingFeedback(items: List<UserFeedbackItem>, targetLang: DevLanguage) {
        val state = _uiState.value
        val candidates = items
            .filter {
                val key = it.id to targetLang
                !state.translations.containsKey(key) && key !in state.translatingKeys
            }
            .take(12)
        if (candidates.isEmpty()) return
        candidates.forEach { translateFeedback(it, targetLang) }
    }

    fun toggleFeedbackResolved(item: UserFeedbackItem) {
        if (!_uiState.value.isUnlocked) return
        val nextResolved = !item.isResolved
        viewModelScope.launch {
            _uiState.update { it.copy(feedbackBusyId = item.id) }
            val res = statsManager.setFeedbackResolved(item.id, nextResolved)
            if (_uiState.value.isUnlocked) {
                res.onSuccess {
                    _uiState.update { state ->
                        val snap = state.feedback
                        if (snap == null) state.copy(feedbackBusyId = null)
                        else {
                            val updatedItems = snap.items.map { row ->
                                if (row.id == item.id) {
                                    row.copy(status = if (nextResolved) "resolved" else "new")
                                } else row
                            }
                            state.copy(
                                feedbackBusyId = null,
                                feedback = snap.copy(
                                    items = updatedItems,
                                    unreadCount = updatedItems.count { !it.isResolved },
                                ),
                            )
                        }
                    }
                }.onFailure {
                    _uiState.update { it.copy(feedbackBusyId = null) }
                }
            }
        }
    }

    fun deleteFeedback(id: Long) {
        if (!_uiState.value.isUnlocked) return
        viewModelScope.launch {
            _uiState.update { it.copy(feedbackBusyId = id) }
            val res = statsManager.deleteFeedback(id)
            if (_uiState.value.isUnlocked) {
                res.onSuccess {
                    _uiState.update { state ->
                        val snap = state.feedback
                        if (snap == null) state.copy(feedbackBusyId = null)
                        else {
                            val updatedItems = snap.items.filterNot { it.id == id }
                            state.copy(
                                feedbackBusyId = null,
                                feedback = snap.copy(
                                    items = updatedItems,
                                    unreadCount = updatedItems.count { !it.isResolved },
                                    totalCount = (snap.totalCount - 1).coerceAtLeast(updatedItems.size),
                                ),
                            )
                        }
                    }
                }.onFailure {
                    _uiState.update { it.copy(feedbackBusyId = null) }
                }
            }
        }
    }

    fun sendTestPing() {
        if (!_uiState.value.isUnlocked) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStats = true, pingFeedback = null) }
            val ok = statsManager.sendHeartbeatIfDue(force = true)
            val snapshot = statsManager.fetchDeveloperStats()
            val bm = _uiState.value.devLanguage == DevLanguage.BM
            if (_uiState.value.isUnlocked) {
                _uiState.update {
                    it.copy(
                        stats = snapshot,
                        isLoadingStats = false,
                        pingFeedback = when {
                            ok && bm -> "Ping peranti berjaya direkod"
                            ok -> "Device ping recorded"
                            bm -> "Gagal hantar ping ke Worker"
                            else -> "Failed to send ping to Worker"
                        },
                    )
                }
            }
        }
    }

    internal fun createGitHubIssue(
        item: UserFeedbackItem,
        translation: FeedbackTranslation?,
        digest: BugTriageDigest,
    ) {
        if (!_uiState.value.isUnlocked) return
        val cur = _uiState.value
        if (item.id in cur.creatingIssueIds || cur.createdIssues.containsKey(item.id)) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    creatingIssueIds = it.creatingIssueIds + item.id,
                    issueErrorById = it.issueErrorById - item.id,
                )
            }
            val issueTitle = translation?.translatedTitle ?: item.title
            val issueBody = BugTriageAnalyzer.buildAiAgentPrompt(item, translation, digest, DevLanguage.EN)
            val labels = listOf(
                if (item.category == FeedbackCategory.BUG) "bug" else "enhancement",
                "from-app",
            )
            val res = manager.createGitHubIssue(
                title = issueTitle,
                body = issueBody,
                labels = labels,
            )
            if (_uiState.value.isUnlocked) {
                res.onSuccess { created ->
                    _uiState.update { state ->
                        state.copy(
                            creatingIssueIds = state.creatingIssueIds - item.id,
                            createdIssues = state.createdIssues + (item.id to created),
                        )
                    }
                }.onFailure { err ->
                    _uiState.update { state ->
                        state.copy(
                            creatingIssueIds = state.creatingIssueIds - item.id,
                            issueErrorById = state.issueErrorById + (item.id to (err.message ?: "Failed")),
                        )
                    }
                }
            }
        }
    }

    fun sendBroadcast(
        title: String,
        message: String,
        type: BroadcastType,
        imageUrl: String,
        actionLabel: String,
        actionUrl: String,
        notify: Boolean,
        displayMode: BroadcastDisplayMode,
        minVersion: String,
        maxVersion: String,
        expiryDays: Int?,
        onSuccess: () -> Unit,
    ) {
        if (!_uiState.value.isUnlocked) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, broadcastStatus = null, broadcastStatusIsError = false) }
            val expiresAt = expiryDays?.let { System.currentTimeMillis() + it * 86_400_000L }
            val result = manager.sendBroadcast(
                title = title,
                message = message,
                type = type,
                imageUrl = imageUrl.ifBlank { null },
                actionLabel = actionLabel.ifBlank { null },
                actionUrl = actionUrl.ifBlank { null },
                notify = notify,
                displayMode = displayMode,
                minVersion = minVersion.ifBlank { null },
                maxVersion = maxVersion.ifBlank { null },
                expiresAtEpochMs = expiresAt,
                closePrevious = true,
            )
            val bm = _uiState.value.devLanguage == DevLanguage.BM
            result.onSuccess { created ->
                val open = manager.fetchOpenBroadcasts().getOrDefault(listOf(created))
                val hist = manager.fetchBroadcastHistory(10).getOrDefault(_uiState.value.broadcastHistory)
                _uiState.update {
                    it.copy(
                        openBroadcasts = open,
                        broadcastHistory = hist,
                        isBusy = false,
                        broadcastStatus = if (bm) "Broadcast dihantar (#${created.issueNumber})" else "Broadcast sent (#${created.issueNumber})",
                        broadcastStatusIsError = false,
                    )
                }
                onSuccess()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        broadcastStatus = err.message ?: if (bm) "Gagal hantar broadcast" else "Failed to send broadcast",
                        broadcastStatusIsError = true,
                    )
                }
            }
        }
    }

    fun updateBroadcast(
        issueNumber: Int,
        title: String,
        message: String,
        type: BroadcastType,
        imageUrl: String,
        actionLabel: String,
        actionUrl: String,
        notify: Boolean,
        displayMode: BroadcastDisplayMode,
        minVersion: String,
        maxVersion: String,
        expiryDays: Int?,
        onSuccess: () -> Unit,
    ) {
        if (!_uiState.value.isUnlocked) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, broadcastStatus = null, broadcastStatusIsError = false) }
            val expiresAt = expiryDays?.let { System.currentTimeMillis() + it * 86_400_000L }
            val result = manager.updateBroadcast(
                issueNumber = issueNumber,
                title = title,
                message = message,
                type = type,
                imageUrl = imageUrl.ifBlank { null },
                actionLabel = actionLabel.ifBlank { null },
                actionUrl = actionUrl.ifBlank { null },
                notify = notify,
                displayMode = displayMode,
                minVersion = minVersion.ifBlank { null },
                maxVersion = maxVersion.ifBlank { null },
                expiresAtEpochMs = expiresAt,
            )
            val bm = _uiState.value.devLanguage == DevLanguage.BM
            result.onSuccess {
                val open = manager.fetchOpenBroadcasts().getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        openBroadcasts = open,
                        isBusy = false,
                        broadcastStatus = if (bm) "Broadcast #$issueNumber dikemas kini" else "Broadcast #$issueNumber updated",
                        broadcastStatusIsError = false,
                    )
                }
                onSuccess()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        broadcastStatus = err.message ?: if (bm) "Gagal kemas kini broadcast" else "Failed to update broadcast",
                        broadcastStatusIsError = true,
                    )
                }
            }
        }
    }

    fun closeBroadcast(issueNumber: Int) {
        if (!_uiState.value.isUnlocked) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, broadcastStatus = null, broadcastStatusIsError = false) }
            val result = manager.closeBroadcast(issueNumber)
            val bm = _uiState.value.devLanguage == DevLanguage.BM
            result.onSuccess {
                val open = manager.fetchOpenBroadcasts().getOrDefault(emptyList())
                val hist = manager.fetchBroadcastHistory(10).getOrDefault(_uiState.value.broadcastHistory)
                _uiState.update {
                    it.copy(
                        openBroadcasts = open,
                        broadcastHistory = hist,
                        isBusy = false,
                        broadcastStatus = if (bm) "Broadcast #$issueNumber ditamatkan" else "Broadcast #$issueNumber ended",
                        broadcastStatusIsError = false,
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        broadcastStatus = err.message ?: if (bm) "Gagal tamatkan broadcast" else "Failed to end broadcast",
                        broadcastStatusIsError = true,
                    )
                }
            }
        }
    }
}

internal enum class DevMetricInfo(
    private val titleBm: String,
    private val titleEn: String,
    private val descBm: String,
    private val descEn: String,
) {
    ONLINE_15M(
        titleBm = "Online (15m)",
        titleEn = "Online (15m)",
        descBm = "Bilangan peranti unik yang menghantar ping dalam 15 minit terakhir. Menunjukkan anggaran pengguna sedang aktif membuka app sekarang.",
        descEn = "Unique devices that sent a heartbeat within the last 15 minutes. Estimates how many users currently have the app open.",
    ),
    NEW_TODAY(
        titleBm = "Pengguna Baru Hari Ini",
        titleEn = "New Users Today",
        descBm = "Bilangan peranti unik yang pertama kali membuka app hari ini (berdasarkan ID rawak tanpa identiti peribadi).",
        descEn = "Unique devices opening the app for the first time today (random install ID, zero personal identity).",
    ),
    DAU(
        titleBm = "DAU — Daily Active Users",
        titleEn = "DAU — Daily Active Users",
        descBm = "Jumlah peranti unik yang aktif menggunakan app pada hari ini. Berguna untuk melihat trafik harian app.",
        descEn = "Total unique devices active in the app today. Useful for tracking daily traffic.",
    ),
    WAU(
        titleBm = "WAU — Weekly Active Users",
        titleEn = "WAU — Weekly Active Users",
        descBm = "Jumlah peranti unik yang membuka app sekurang-kurangnya sekali dalam 7 hari terakhir.",
        descEn = "Unique devices active at least once during the past 7 days.",
    ),
    MAU(
        titleBm = "MAU — Monthly Active Users",
        titleEn = "MAU — Monthly Active Users",
        descBm = "Jumlah peranti unik yang aktif dalam tempoh 30 hari terakhir (penanda aras jumlah pengguna aktif bulanan).",
        descEn = "Unique devices active during the past 30 days (monthly active user benchmark).",
    ),
    INSTALLS(
        titleBm = "Installs — Jumlah Peranti",
        titleEn = "Installs — Total Devices",
        descBm = "Jumlah keseluruhan peranti unik yang pernah membuka versi app dengan statistik aktif sejak dilancarkan.",
        descEn = "Total unique devices that have opened app versions with telemetry enabled.",
    ),
    STICKINESS(
        titleBm = "Stickiness (DAU / MAU)",
        titleEn = "Stickiness (DAU / MAU)",
        descBm = "Nisbah pengguna aktif harian (DAU) berbanding pengguna aktif 30 hari (MAU). Lebih tinggi bermakna pengguna kerap kembali setiap hari.",
        descEn = "Ratio of daily active users (DAU) to monthly active users (MAU). Higher means stronger daily retention.",
    ),
    ADOPTION(
        titleBm = "Adoptasi Versi",
        titleEn = "Version Adoption",
        descBm = "Peratusan peranti aktif yang sudah kemas kini ke versi app semasa berbanding versi lama.",
        descEn = "Percentage of active devices already running the current app version vs older releases.",
    );

    fun title(lang: DevLanguage): String = if (lang == DevLanguage.BM) titleBm else titleEn
    fun description(lang: DevLanguage): String = if (lang == DevLanguage.BM) descBm else descEn
}

@Composable
fun DeveloperConsoleScreen(
    onBack: () -> Unit,
    viewModel: DeveloperConsoleViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isComic = LocalComicTheme.current
    val lang = ui.devLanguage
    val bm = lang == DevLanguage.BM

    var showPatLogin by rememberSaveable { mutableStateOf(false) }
    var deviceAuthFallbackMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var awaitingDeviceCredential by remember { mutableStateOf(false) }
    var showSignOutConfirm by rememberSaveable { mutableStateOf(false) }
    var activeMetricHelp by remember { mutableStateOf<DevMetricInfo?>(null) }
    var previewMessage by remember { mutableStateOf<BroadcastMessage?>(null) }
    var zoomedScreenshot by remember { mutableStateOf<String?>(null) }
    var pendingDeleteFeedback by remember { mutableStateOf<UserFeedbackItem?>(null) }
    var showLocalLogsDialog by rememberSaveable { mutableStateOf(false) }
    val currentAppVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
        }.getOrDefault("")
    }

    val deviceUnlockLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        awaitingDeviceCredential = false
        if (result.resultCode == Activity.RESULT_OK) {
            deviceAuthFallbackMessage = null
            showPatLogin = false
            viewModel.unlockAfterDeviceAuth()
        }
    }

    fun launchDeviceUnlock() {
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        @Suppress("DEPRECATION")
        val intent = if (km?.isDeviceSecure == true) {
            km.createConfirmDeviceCredentialIntent(
                "Unlock Developer Console",
                if (bm) "Sahkan cap jari, wajah, atau PIN peranti" else "Confirm fingerprint, face, or device PIN",
            )
        } else {
            null
        }
        if (intent != null) {
            deviceAuthFallbackMessage = null
            awaitingDeviceCredential = true
            deviceUnlockLauncher.launch(intent)
        } else {
            deviceAuthFallbackMessage = if (bm) {
                "Peranti tiada kunci skrin (PIN/Cap Jari). Sahkan semula guna PAT."
            } else {
                "Device has no screen lock (PIN/Biometric). Re-verify using your PAT."
            }
            showPatLogin = true
        }
    }

    DisposableEffect(lifecycleOwner, ui.isUnlocked, awaitingDeviceCredential) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && ui.isUnlocked && !awaitingDeviceCredential) {
                viewModel.lockSession()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (ui.lockState is DevLockState.PermanentlyLocked) {
        LaunchedEffect(Unit) { onBack() }
        Box(modifier = Modifier.fillMaxSize().pageBackground())
        return
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text(if (bm) "Log Keluar Developer?" else "Sign Out Developer?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (bm) {
                        "Token @${ui.ownerLogin ?: "aayepp"} akan dipadam dan kad Developer Console akan disembunyikan daripada Settings."
                    } else {
                        "Token for @${ui.ownerLogin ?: "aayepp"} will be cleared and the Developer Console card will be hidden from Settings."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirm = false
                        showPatLogin = false
                        deviceAuthFallbackMessage = null
                        viewModel.signOut()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(if (bm) "Log Keluar" else "Sign Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text(if (bm) "Batal" else "Cancel")
                }
            },
        )
    }

    activeMetricHelp?.let { info ->
        val live = ui.stats?.liveStats
        val valueLine = when (info) {
            DevMetricInfo.ONLINE_15M -> live?.let {
                if (bm) "Semasa: ${formatStatsNumber(it.online15m)} pengguna" else "Now: ${formatStatsNumber(it.online15m)} users"
            }
            DevMetricInfo.NEW_TODAY -> live?.let {
                if (bm) "Hari ini: +${formatStatsNumber(it.newToday)} peranti" else "Today: +${formatStatsNumber(it.newToday)} devices"
            }
            DevMetricInfo.DAU -> live?.let {
                if (bm) {
                    "Hari ini: ${formatStatsNumber(it.dauToday)} • 24j: ${formatStatsNumber(it.active24h)}"
                } else {
                    "Today: ${formatStatsNumber(it.dauToday)} • 24h: ${formatStatsNumber(it.active24h)}"
                }
            }
            DevMetricInfo.WAU -> live?.let {
                if (bm) {
                    "7 hari: ${formatStatsNumber(it.wau7d)} • Baru 7h: +${formatStatsNumber(it.new7d)}"
                } else {
                    "7 days: ${formatStatsNumber(it.wau7d)} • New 7d: +${formatStatsNumber(it.new7d)}"
                }
            }
            DevMetricInfo.MAU -> live?.let {
                if (bm) "30 hari: ${formatStatsNumber(it.mau30d)} peranti unik" else "30 days: ${formatStatsNumber(it.mau30d)} unique devices"
            }
            DevMetricInfo.INSTALLS -> live?.let {
                if (bm) "Sepanjang masa: ${formatStatsNumber(it.totalInstalls)} peranti" else "All time: ${formatStatsNumber(it.totalInstalls)} devices"
            }
            DevMetricInfo.STICKINESS -> live?.let {
                val pct = if (it.mau30d > 0) it.dauToday * 100 / it.mau30d else 0
                "DAU/MAU: $pct% (${formatStatsNumber(it.dauToday)} / ${formatStatsNumber(it.mau30d)})"
            }
            DevMetricInfo.ADOPTION -> live?.let {
                val total = it.byVersion.sumOf { b -> b.count }.coerceAtLeast(1)
                val clean = currentAppVersion.removePrefix("v").trim()
                val match = it.byVersion.firstOrNull { b -> b.label.removePrefix("v").trim() == clean } ?: it.byVersion.firstOrNull()
                match?.let { b -> "${b.label}: ${b.count * 100 / total}% (${formatStatsNumber(b.count)} / ${formatStatsNumber(total)})" }
            }
        }
        AlertDialog(
            onDismissRequest = { activeMetricHelp = null },
            title = { Text(info.title(lang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (valueLine != null) {
                        Text(
                            text = valueLine,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = info.description(lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { activeMetricHelp = null }) {
                    Text(if (bm) "Faham" else "Got it", fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    previewMessage?.let { msg ->
        BroadcastDialog(
            message = msg,
            onDismiss = { previewMessage = null },
        )
    }

    zoomedScreenshot?.let { shotSource ->
        ScreenshotZoomDialog(
            source = shotSource,
            lang = lang,
            onDismiss = { zoomedScreenshot = null },
        )
    }

    pendingDeleteFeedback?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteFeedback = null },
            title = { Text(if (bm) "Padam Laporan?" else "Delete Report?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (bm) {
                        "Laporan #${target.id} (\"${target.title}\") akan dipadam secara kekal dari Inbox."
                    } else {
                        "Report #${target.id} (\"${target.title}\") will be permanently deleted from Inbox."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFeedback(target.id)
                        pendingDeleteFeedback = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(if (bm) "Padam" else "Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteFeedback = null }) {
                    Text(if (bm) "Batal" else "Cancel")
                }
            },
        )
    }

    if (showLocalLogsDialog) {
        LocalDiagnosticsDialog(
            lang = lang,
            onDismiss = { showLocalLogsDialog = false },
        )
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomScrollClearance = navBottom + LocalMiniPlayerScrollClearance.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pageBackground(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (!ui.isUnlocked) {
            GlassHeaderLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .adaptiveContentWidth(maxWidth = 720.dp)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                header = { headerModifier, _ ->
                    ExpressiveHeader(
                        modifier = headerModifier,
                        title = "",
                        onBack = onBack,
                        containerColor = Color.Transparent,
                        actions = {
                            DevLanguagePillToggle(
                                language = lang,
                                isComic = isComic,
                                onSelectLanguage = viewModel::setDevLanguage,
                            )
                        },
                    )
                },
            ) { headerInset ->
                when (val lock = ui.lockState) {
                    is DevLockState.Cooldown24h -> Cooldown24hContent(
                        unlockAtWallMs = lock.unlockAtWallMs,
                        onExpired = viewModel::refreshLockState,
                        topInset = headerInset,
                        bottomInset = bottomScrollClearance,
                        isComic = isComic,
                        lang = lang,
                    )
                    is DevLockState.Open -> {
                        if (ui.hasToken && !showPatLogin) {
                            ReturningUnlockContent(
                                ownerLogin = ui.ownerLogin ?: "aayepp",
                                onUseBiometricOrPin = { launchDeviceUnlock() },
                                onSwitchTokenOrSignOut = {
                                    deviceAuthFallbackMessage = null
                                    showPatLogin = true
                                },
                                topInset = headerInset,
                                bottomInset = bottomScrollClearance,
                                isComic = isComic,
                                lang = lang,
                            )
                        } else {
                            PatLoginContent(
                                lockState = lock,
                                hasSavedToken = ui.hasToken,
                                isBusy = ui.isBusy,
                                errorMessage = deviceAuthFallbackMessage ?: ui.authError,
                                onVerify = { token ->
                                    deviceAuthFallbackMessage = null
                                    viewModel.verifyAndSignIn(token) {
                                        showPatLogin = false
                                    }
                                },
                                onCancelSwitch = if (ui.hasToken) {
                                    {
                                        deviceAuthFallbackMessage = null
                                        showPatLogin = false
                                    }
                                } else {
                                    null
                                },
                                onRequestSignOut = if (ui.hasToken) {
                                    { showSignOutConfirm = true }
                                } else {
                                    null
                                },
                                topInset = headerInset,
                                bottomInset = bottomScrollClearance,
                                isComic = isComic,
                                lang = lang,
                            )
                        }
                    }
                    DevLockState.PermanentlyLocked -> Unit
                }
            }
        } else {
            UnlockedConsoleContent(
                ui = ui,
                isComic = isComic,
                currentAppVersion = currentAppVersion,
                bottomClearance = bottomScrollClearance,
                onBack = onBack,
                onRefresh = viewModel::refreshAll,
                onLockNow = viewModel::lockSession,
                onSelectDevLanguage = viewModel::setDevLanguage,
                onSelectTranslateTarget = viewModel::setTranslateTarget,
                onTestPing = viewModel::sendTestPing,
                onHoldMetric = { activeMetricHelp = it },
                onOpenLocalLogs = { showLocalLogsDialog = true },
                onRefreshFeedback = viewModel::refreshFeedback,
                onToggleAutoTranslate = viewModel::toggleAutoTranslate,
                onTranslateFeedback = { item -> viewModel.translateFeedback(item) },
                onOpenScreenshot = { zoomedScreenshot = it },
                onToggleFeedbackResolved = viewModel::toggleFeedbackResolved,
                onRequestDeleteFeedback = { item -> pendingDeleteFeedback = item },
                onCreateGitHubIssue = { item, tr, digest -> viewModel.createGitHubIssue(item, tr, digest) },
                onCloseBroadcast = viewModel::closeBroadcast,
                onSendBroadcast = viewModel::sendBroadcast,
                onUpdateBroadcast = viewModel::updateBroadcast,
                onPreviewBroadcast = { previewMessage = it },
                onHideShortcut = {
                    viewModel.hideShortcutFromSettings()
                    onBack()
                },
                onRequestSignOut = { showSignOutConfirm = true },
            )
        }
    }
}

@Composable
private fun DevLanguagePillToggle(
    language: DevLanguage,
    isComic: Boolean,
    onSelectLanguage: (DevLanguage) -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = if (isComic) ComicAccent else glassCardColor(MaterialTheme.colorScheme.surfaceContainerHigh),
        border = devCardBorder(isComic),
        modifier = Modifier.padding(end = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DevLanguage.entries.forEach { option ->
                val selected = language == option
                Surface(
                    onClick = { if (!selected) onSelectLanguage(option) },
                    shape = CircleShape,
                    color = if (selected) {
                        if (isComic) ComicInk else MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                ) {
                    Text(
                        text = option.shortLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) {
                            if (isComic) Color.White else MaterialTheme.colorScheme.onPrimary
                        } else {
                            if (isComic) ComicInk else MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Cooldown24hContent(
    unlockAtWallMs: Long,
    onExpired: () -> Unit,
    topInset: androidx.compose.ui.unit.Dp,
    bottomInset: androidx.compose.ui.unit.Dp,
    isComic: Boolean,
    lang: DevLanguage,
) {
    val bm = lang == DevLanguage.BM
    var nowMs by remember(unlockAtWallMs) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(unlockAtWallMs) {
        while (true) {
            val current = System.currentTimeMillis()
            nowMs = current
            val left = unlockAtWallMs - current
            if (left <= 0L) {
                onExpired()
                break
            }
            delay(left.coerceAtMost(30_000L).coerceAtLeast(1_000L))
        }
    }
    val remainingMs = (unlockAtWallMs - nowMs).coerceAtLeast(0L)
    val totalMinutes = ((remainingMs + 59_999L) / 60_000L).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val remainingText = if (bm) {
        if (hours > 0) "$hours jam $minutes minit" else "${minutes.coerceAtLeast(1)} minit"
    } else {
        if (hours > 0) "$hours hr $minutes min" else "${minutes.coerceAtLeast(1)} min"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = topInset + 32.dp,
                bottom = bottomInset + 32.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        DevGateIconBadge(
            shape = RoundedCornerShape(18.dp),
            isComic = isComic,
            tint = MaterialTheme.colorScheme.error,
            icon = Icons.Filled.Lock,
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = if (bm) "Akses Dikunci" else "Access Locked",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (bm) "Cuba semula dalam $remainingText" else "Try again in $remainingText",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = glassCardColor(MaterialTheme.colorScheme.surfaceContainer),
            border = devCardBorder(isComic),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (bm) {
                    "3 percubaan gagal direkodkan. Selepas tempoh tamat, hanya ada 1 percubaan terakhir sebelum akses dikunci selamanya."
                } else {
                    "3 failed attempts recorded. After cooldown ends, 1 final attempt remains before permanent lockout."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(18.dp),
            )
        }
    }
}

@Composable
private fun ReturningUnlockContent(
    ownerLogin: String,
    onUseBiometricOrPin: () -> Unit,
    onSwitchTokenOrSignOut: () -> Unit,
    topInset: androidx.compose.ui.unit.Dp,
    bottomInset: androidx.compose.ui.unit.Dp,
    isComic: Boolean,
    lang: DevLanguage,
) {
    val bm = lang == DevLanguage.BM
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = topInset + 24.dp,
                bottom = bottomInset + 24.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DevGateIconBadge(
                shape = CircleShape,
                isComic = isComic,
                tint = if (isComic) ComicInk else MaterialTheme.colorScheme.primary,
                icon = Icons.Filled.Fingerprint,
                badgeSize = 72.dp,
                iconSize = 36.dp,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Unlock Console",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Signed in as @$ownerLogin",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onUseBiometricOrPin,
                shape = CircleShape,
                colors = devPrimaryButtonColors(isComic),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(if (bm) "Guna Cap Jari / PIN" else "Use Biometric / PIN", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onSwitchTokenOrSignOut) {
                Text(if (bm) "Tukar Token / Log Keluar" else "Switch Token / Sign Out")
            }
        }
        Text(
            text = if (bm) "Auto-kunci apabila keluar halaman" else "Auto-locks when leaving this screen",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun PatLoginContent(
    lockState: DevLockState.Open,
    hasSavedToken: Boolean,
    isBusy: Boolean,
    errorMessage: String?,
    onVerify: (String) -> Unit,
    onCancelSwitch: (() -> Unit)?,
    onRequestSignOut: (() -> Unit)?,
    topInset: androidx.compose.ui.unit.Dp,
    bottomInset: androidx.compose.ui.unit.Dp,
    isComic: Boolean,
    lang: DevLanguage,
) {
    val bm = lang == DevLanguage.BM
    val context = LocalContext.current
    var tokenInput by rememberSaveable { mutableStateOf("") }
    var revealToken by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(
                start = 24.dp,
                end = 24.dp,
                top = topInset + 24.dp,
                bottom = bottomInset + 28.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        DevGateIconBadge(
            shape = RoundedCornerShape(18.dp),
            isComic = isComic,
            tint = if (isComic) ComicInk else MaterialTheme.colorScheme.onSurface,
            icon = Icons.Filled.Lock,
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Developer Access",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (bm) "Sahkan token pemilik untuk buka konsol" else "Verify owner token to open console",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = glassCardColor(MaterialTheme.colorScheme.surfaceContainer),
            border = devCardBorder(isComic),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val patTextStyle = androidx.compose.material3.LocalTextStyle.current
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("GitHub Owner PAT") },
                    placeholder = { Text("github_pat_...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = if (revealToken) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (tokenInput.isNotBlank() && !isBusy) onVerify(tokenInput)
                        },
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clipText = clipboard?.primaryClip?.getItemAt(0)
                                        ?.coerceToText(context)?.toString()?.trim()
                                    if (!clipText.isNullOrEmpty()) {
                                        tokenInput = clipText
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentPaste,
                                    contentDescription = if (bm) "Tampal token" else "Paste token",
                                )
                            }
                            IconButton(onClick = { revealToken = !revealToken }) {
                                Icon(
                                    imageVector = if (revealToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (bm) "Papar atau sembunyikan token" else "Show or hide token",
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .sakugaTypingInput(
                            text = tokenInput,
                            textStyle = patTextStyle,
                            isPassword = !revealToken,
                            leadingPadding = 16.dp,
                        ),
                )

                val noticeText = when {
                    lockState.isFinalChanceAfterCooldown ->
                        if (bm) "Percubaan terakhir — gagal akan kunci akses secara kekal" else "Final attempt — failure locks access permanently"
                    lockState.remainingAttempts in 1..2 ->
                        if (bm) "Baki ${lockState.remainingAttempts} percubaan sebelum dikunci 24 jam" else "${lockState.remainingAttempts} attempt(s) left before 24h lock"
                    else -> null
                }
                if (noticeText != null) {
                    Text(
                        text = noticeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (lockState.isFinalChanceAfterCooldown || lockState.remainingAttempts == 1) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Button(
                    onClick = { onVerify(tokenInput) },
                    enabled = tokenInput.isNotBlank() && !isBusy,
                    shape = CircleShape,
                    colors = devPrimaryButtonColors(isComic),
                    contentPadding = PaddingValues(vertical = 14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = if (isComic) ComicInk else MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(if (bm) "Sahkan & Buka" else "Verify & Open", fontWeight = FontWeight.Bold)
                    }
                }

                if (hasSavedToken && (onCancelSwitch != null || onRequestSignOut != null)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (onCancelSwitch != null) {
                            TextButton(onClick = onCancelSwitch, enabled = !isBusy) {
                                Text(if (bm) "Batal" else "Cancel")
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }
                        if (onRequestSignOut != null) {
                            TextButton(
                                onClick = onRequestSignOut,
                                enabled = !isBusy,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Text(if (bm) "Log Keluar" else "Sign Out")
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = if (bm) "Disahkan di server untuk @aayepp · Tiada rahsia dalam APK" else "Server-verified for @aayepp · Zero secrets in APK",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnlockedConsoleContent(
    ui: DeveloperConsoleUiState,
    isComic: Boolean,
    currentAppVersion: String,
    bottomClearance: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLockNow: () -> Unit,
    onSelectDevLanguage: (DevLanguage) -> Unit,
    onSelectTranslateTarget: (DevLanguage) -> Unit,
    onTestPing: () -> Unit,
    onHoldMetric: (DevMetricInfo) -> Unit,
    onOpenLocalLogs: () -> Unit,
    onRefreshFeedback: () -> Unit,
    onToggleAutoTranslate: () -> Unit,
    onTranslateFeedback: (UserFeedbackItem) -> Unit,
    onOpenScreenshot: (String) -> Unit,
    onToggleFeedbackResolved: (UserFeedbackItem) -> Unit,
    onRequestDeleteFeedback: (UserFeedbackItem) -> Unit,
    onCreateGitHubIssue: (UserFeedbackItem, FeedbackTranslation?, BugTriageDigest) -> Unit,
    onCloseBroadcast: (Int) -> Unit,
    onSendBroadcast: (
        title: String,
        message: String,
        type: BroadcastType,
        imageUrl: String,
        actionLabel: String,
        actionUrl: String,
        notify: Boolean,
        displayMode: BroadcastDisplayMode,
        minVersion: String,
        maxVersion: String,
        expiryDays: Int?,
        onSuccess: () -> Unit,
    ) -> Unit,
    onUpdateBroadcast: (
        issueNumber: Int,
        title: String,
        message: String,
        type: BroadcastType,
        imageUrl: String,
        actionLabel: String,
        actionUrl: String,
        notify: Boolean,
        displayMode: BroadcastDisplayMode,
        minVersion: String,
        maxVersion: String,
        expiryDays: Int?,
        onSuccess: () -> Unit,
    ) -> Unit,
    onPreviewBroadcast: (BroadcastMessage) -> Unit,
    onHideShortcut: () -> Unit,
    onRequestSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val lang = ui.devLanguage
    val bm = lang == DevLanguage.BM
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    // Category (0 all, 1 bug, 2 suggestion, 3 feedback) and status (0 open, 1 resolved, 2 all) filter together.
    var inboxCategory by rememberSaveable { mutableIntStateOf(0) }
    var inboxStatus by rememberSaveable { mutableIntStateOf(0) }
    var inboxSearchQuery by rememberSaveable { mutableStateOf("") }
    // Priority filter: 0 all, 1 high, 2 medium, 3 low.
    var inboxPriorityFilter by rememberSaveable { mutableIntStateOf(0) }
    var inboxCurrentVersionOnly by rememberSaveable { mutableStateOf(false) }

    var editingIssueNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedType by rememberSaveable { mutableStateOf(BroadcastType.INFO) }
    var selectedDisplayMode by rememberSaveable { mutableStateOf(BroadcastDisplayMode.POPUP) }
    var title by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var imageUrl by rememberSaveable { mutableStateOf("") }
    var actionLabel by rememberSaveable { mutableStateOf("") }
    var actionUrl by rememberSaveable { mutableStateOf("") }
    var minVersion by rememberSaveable { mutableStateOf("") }
    var maxVersion by rememberSaveable { mutableStateOf("") }
    var expiryDays by rememberSaveable { mutableStateOf<Int?>(null) }
    var notify by rememberSaveable { mutableStateOf(true) }

    fun loadBroadcastIntoComposer(msg: BroadcastMessage, asEdit: Boolean) {
        editingIssueNumber = if (asEdit) msg.issueNumber else null
        selectedType = msg.type
        selectedDisplayMode = msg.displayMode
        title = msg.title
        message = msg.message
        imageUrl = msg.imageUrl.orEmpty()
        actionLabel = msg.actionLabel.orEmpty()
        actionUrl = msg.actionUrl.orEmpty()
        minVersion = msg.minVersion.orEmpty()
        maxVersion = msg.maxVersion.orEmpty()
        expiryDays = msg.expiresAtEpochMs?.let { exp ->
            val remDays = ((exp - System.currentTimeMillis()) / 86_400_000L).toInt()
            when {
                remDays <= 1 -> 1
                remDays <= 3 -> 3
                else -> 7
            }
        }
        notify = msg.notify
    }

    fun clearBroadcastComposer() {
        editingIssueNumber = null
        title = ""
        message = ""
        imageUrl = ""
        actionLabel = ""
        actionUrl = ""
        minVersion = ""
        maxVersion = ""
        expiryDays = null
    }

    val statsListState = rememberLazyListState()
    val inboxListState = rememberLazyListState()
    val broadcastListState = rememberLazyListState()

    val unreadCount = ui.feedback?.unreadCount ?: 0
    val tabItems = remember(unreadCount, lang) {
        listOf(
            ConnectedButtonItem(label = if (bm) "Ringkasan" else "Overview"),
            ConnectedButtonItem(label = if (unreadCount > 0) "Inbox ($unreadCount)" else "Inbox"),
            ConnectedButtonItem(label = "Broadcast"),
        )
    }

    GlassHeaderLayout(
        modifier = Modifier
            .fillMaxSize()
            .adaptiveContentWidth(maxWidth = 720.dp)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        header = { headerModifier, headerBackdrop ->
            Column(
                modifier = headerModifier.then(
                    if (headerBackdrop == null) {
                        Modifier.background(MaterialTheme.colorScheme.background)
                    } else Modifier,
                ),
            ) {
                ExpressiveHeader(
                    backdrop = headerBackdrop,
                    title = "Developer",
                    onBack = onBack,
                    actions = {
                        DevLanguagePillToggle(
                            language = lang,
                            isComic = isComic,
                            onSelectLanguage = onSelectDevLanguage,
                        )
                        HeaderActionIcon(
                            icon = Icons.Filled.Refresh,
                            contentDescription = if (bm) "Muat semula" else "Refresh",
                            onClick = onRefresh,
                        )
                        HeaderActionIcon(
                            icon = Icons.Filled.Lock,
                            contentDescription = if (bm) "Kunci konsol" else "Lock console",
                            onClick = onLockNow,
                        )
                    },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
                ) {
                    ConnectedButtonGroup(
                        items = tabItems,
                        selectedIndex = selectedTabIndex,
                        onSelect = { selectedTabIndex = it },
                    )
                }
            }
        },
    ) { headerInset ->
        val listState = when (selectedTabIndex) {
            0 -> statsListState
            1 -> inboxListState
            else -> broadcastListState
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = headerInset + 10.dp,
                bottom = 32.dp + bottomClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (selectedTabIndex) {
                0 -> {
                    val live = ui.stats?.liveStats
                    item(key = "stats_live") {
                        LiveStatsBento(
                            live = live,
                            error = ui.stats?.liveError,
                            isLoading = ui.isLoadingStats,
                            isComic = isComic,
                            lang = lang,
                            currentAppVersion = currentAppVersion,
                            onHoldMetric = onHoldMetric,
                            onTestPing = onTestPing,
                            onRetry = onRefresh,
                        )
                    }
                    if (live != null && live.byCountry.isNotEmpty()) {
                        item(key = "stats_countries") { CountriesTile(live.byCountry, isComic, lang) }
                    }
                    if (live != null && live.byVersion.isNotEmpty()) {
                        item(key = "stats_versions") { VersionsTile(live.byVersion, isComic, lang) }
                    }
                    item(key = "stats_android_release") {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (live != null && live.byAndroid.isNotEmpty()) {
                                AndroidTile(live.byAndroid, isComic, Modifier.weight(1f))
                            }
                            ReleaseTile(
                                gh = ui.stats?.githubStats,
                                error = ui.stats?.githubError,
                                isLoading = ui.isLoadingStats,
                                isComic = isComic,
                                lang = lang,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    val fetchedAt = ui.stats?.fetchedAtMs ?: 0L
                    if (fetchedAt > 0L || !ui.pingFeedback.isNullOrBlank()) {
                        item(key = "stats_footer") {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                ui.pingFeedback?.let { feedback ->
                                    Text(
                                        text = feedback,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                                if (fetchedAt > 0L) {
                                    Text(
                                        text = if (bm) "Dikemas kini ${formatClockTime(fetchedAt)}" else "Updated ${formatClockTime(fetchedAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    item(key = "stats_local_logs") {
                        val crashCount = remember(ui.stats?.fetchedAtMs) {
                            runCatching { CrashRecorder.crashFiles(context).size }.getOrDefault(0)
                        }
                        val logLines = remember(ui.stats?.fetchedAtMs) {
                            runCatching { AppLog.tail?.snapshot(200)?.size ?: 0 }.getOrDefault(0)
                        }
                        LocalDiagnosticsTile(
                            crashCount = crashCount,
                            logLineCount = logLines,
                            isComic = isComic,
                            lang = lang,
                            onOpenLogs = onOpenLocalLogs,
                        )
                    }

                    item(key = "stats_session_strip") {
                        SessionSecurityStrip(
                            ownerLogin = ui.ownerLogin ?: "aayepp",
                            isComic = isComic,
                            lang = lang,
                            onSelectLanguage = onSelectDevLanguage,
                            onHideShortcut = onHideShortcut,
                            onSignOut = onRequestSignOut,
                        )
                    }
                }

                1 -> {
                    val snap = ui.feedback
                    val allItems = snap?.items.orEmpty()
                    val categories = listOf(null, FeedbackCategory.BUG, FeedbackCategory.SUGGESTION, FeedbackCategory.FEEDBACK)
                    val inCategory = categories[inboxCategory]?.let { c -> allItems.filter { it.category == c } } ?: allItems
                    val byStatus = when (inboxStatus) {
                        0 -> inCategory.filter { !it.isResolved }
                        1 -> inCategory.filter { it.isResolved }
                        else -> inCategory
                    }
                    val queryTrim = inboxSearchQuery.trim().lowercase()
                    val filtered = byStatus.filter { item ->
                        val verOk = !inboxCurrentVersionOnly ||
                            currentAppVersion.isBlank() ||
                            item.appVersion.equals(currentAppVersion, ignoreCase = true)
                        if (!verOk) return@filter false
                        if (inboxPriorityFilter != 0) {
                            val d = BugTriageAnalyzer.analyze(item, item.title, item.body, lang)
                            val wanted = when (inboxPriorityFilter) {
                                1 -> BugPriority.HIGH
                                2 -> BugPriority.MEDIUM
                                else -> BugPriority.LOW
                            }
                            if (d.priority != wanted) return@filter false
                        }
                        if (queryTrim.isNotEmpty()) {
                            val hay = "${item.title} ${item.body} ${item.deviceModel} ${item.appVersion} ${item.contact}".lowercase()
                            if (!hay.contains(queryTrim)) return@filter false
                        }
                        true
                    }

                    item(key = "inbox_translate_bar") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = glassCardColor(MaterialTheme.colorScheme.surfaceContainer),
                            border = devCardBorder(isComic),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Translate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (bm) "Auto-Terjemah Laporan" else "Auto-Translate Reports",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = if (bm) {
                                                "Terjemah laporan pengguna ke bahasa pilihan secara automatik"
                                            } else {
                                                "Automatically translate user reports into your chosen language"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Switch(
                                        checked = ui.autoTranslateEnabled,
                                        onCheckedChange = { onToggleAutoTranslate() },
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = if (bm) "Bahasa Sasaran:" else "Target Language:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    DevLanguage.entries.forEach { targetOption ->
                                        val selected = ui.translateTarget == targetOption
                                        FilterChip(
                                            selected = selected,
                                            onClick = { onSelectTranslateTarget(targetOption) },
                                            label = {
                                                Text(
                                                    text = targetOption.displayName,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                )
                                            },
                                            shape = CircleShape,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "inbox_summary") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.forEachIndexed { idx, category ->
                                InboxCountTile(
                                    category = category,
                                    count = category?.let { c -> allItems.count { it.category == c } } ?: allItems.size,
                                    selected = inboxCategory == idx,
                                    isComic = isComic,
                                    lang = lang,
                                    onClick = { inboxCategory = idx },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    item(key = "inbox_status") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    if (bm) "Belum selesai · ${inCategory.count { !it.isResolved }}" else "Open · ${inCategory.count { !it.isResolved }}",
                                    if (bm) "Selesai · ${inCategory.count { it.isResolved }}" else "Resolved · ${inCategory.count { it.isResolved }}",
                                    if (bm) "Semua" else "All",
                                ).forEachIndexed { idx, label ->
                                    FilterChip(
                                        selected = inboxStatus == idx,
                                        onClick = { inboxStatus = idx },
                                        label = { Text(label, fontWeight = if (inboxStatus == idx) FontWeight.Bold else FontWeight.Medium) },
                                        shape = CircleShape,
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = inboxSearchQuery,
                                onValueChange = { inboxSearchQuery = it },
                                placeholder = { Text(if (bm) "Cari laporan, model, atau versi..." else "Search reports, device, or version...") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = if (inboxSearchQuery.isNotEmpty()) {
                                    {
                                        IconButton(onClick = { inboxSearchQuery = "" }) {
                                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                } else null,
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                listOf(
                                    0 to if (bm) "Semua Prioriti" else "All Priorities",
                                    1 to if (bm) "Tinggi" else "High",
                                    2 to if (bm) "Sederhana" else "Medium",
                                    3 to if (bm) "Ringan" else "Low",
                                ).forEach { (pIdx, pLabel) ->
                                    FilterChip(
                                        selected = inboxPriorityFilter == pIdx,
                                        onClick = { inboxPriorityFilter = pIdx },
                                        label = { Text(pLabel, style = MaterialTheme.typography.labelSmall) },
                                        shape = CircleShape,
                                    )
                                }
                                if (currentAppVersion.isNotBlank()) {
                                    FilterChip(
                                        selected = inboxCurrentVersionOnly,
                                        onClick = { inboxCurrentVersionOnly = !inboxCurrentVersionOnly },
                                        label = { Text("v$currentAppVersion", style = MaterialTheme.typography.labelSmall) },
                                        shape = CircleShape,
                                    )
                                }
                            }
                        }
                    }

                    when {
                        ui.isLoadingFeedback && snap == null -> {
                            item(key = "inbox_loading") {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = glassCardColor(MaterialTheme.colorScheme.surfaceContainer),
                                    border = devCardBorder(isComic),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
                                    }
                                }
                            }
                        }
                        snap?.error != null && allItems.isEmpty() -> {
                            item(key = "inbox_error") {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = glassCardColor(MaterialTheme.colorScheme.surfaceContainer),
                                    border = devCardBorder(isComic),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = snap.error,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                        TextButton(onClick = onRefreshFeedback) {
                                            Text(if (bm) "Cuba lagi" else "Retry")
                                        }
                                    }
                                }
                            }
                        }
                        filtered.isEmpty() -> {
                            item(key = "inbox_empty") {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = glassCardColor(MaterialTheme.colorScheme.surfaceContainer),
                                    border = devCardBorder(isComic),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(28.dp),
                                        )
                                        Text(
                                            text = if (bm) "Tiada mesej dalam kategori ini" else "No messages in this category",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            items(filtered, key = { "fb_${it.id}" }) { item ->
                                val trKey = item.id to ui.translateTarget
                                InboxFeedbackCard(
                                    item = item,
                                    isComic = isComic,
                                    lang = lang,
                                    translateTarget = ui.translateTarget,
                                    isBusy = ui.feedbackBusyId == item.id,
                                    translation = ui.translations[trKey],
                                    isTranslating = trKey in ui.translatingKeys,
                                    isCreatingIssue = item.id in ui.creatingIssueIds,
                                    createdIssue = ui.createdIssues[item.id],
                                    issueError = ui.issueErrorById[item.id],
                                    onTranslate = { onTranslateFeedback(item) },
                                    onOpenScreenshot = onOpenScreenshot,
                                    onToggleResolved = { onToggleFeedbackResolved(item) },
                                    onDelete = { onRequestDeleteFeedback(item) },
                                    onCreateGitHubIssue = { digest -> onCreateGitHubIssue(item, ui.translations[trKey], digest) },
                                )
                            }
                        }
                    }
                }

                else -> {
                    item(key = "broadcast_active") {
                        ActiveBroadcastsSection(
                            openBroadcasts = ui.openBroadcasts,
                            isBusy = ui.isBusy,
                            isComic = isComic,
                            lang = lang,
                            onEditBroadcast = { loadBroadcastIntoComposer(it, asEdit = true) },
                            onCloseBroadcast = onCloseBroadcast,
                        )
                    }

                    item(key = "broadcast_composer") {
                        NewBroadcastSection(
                            editingIssueNumber = editingIssueNumber,
                            onCancelEdit = { clearBroadcastComposer() },
                            selectedType = selectedType,
                            onSelectType = { selectedType = it },
                            selectedDisplayMode = selectedDisplayMode,
                            onSelectDisplayMode = { selectedDisplayMode = it },
                            title = title,
                            onTitleChange = { title = it },
                            message = message,
                            onMessageChange = { message = it },
                            imageUrl = imageUrl,
                            onImageUrlChange = { imageUrl = it },
                            actionLabel = actionLabel,
                            onActionLabelChange = { actionLabel = it },
                            actionUrl = actionUrl,
                            onActionUrlChange = { actionUrl = it },
                            minVersion = minVersion,
                            onMinVersionChange = { minVersion = it },
                            maxVersion = maxVersion,
                            onMaxVersionChange = { maxVersion = it },
                            expiryDays = expiryDays,
                            onExpiryDaysChange = { expiryDays = it },
                            notify = notify,
                            onNotifyChange = { notify = it },
                            statusMessage = ui.broadcastStatus,
                            statusIsError = ui.broadcastStatusIsError,
                            isBusy = ui.isBusy,
                            isComic = isComic,
                            lang = lang,
                            onApplyTemplate = { tTitle, tMsg, tType, tMode, tLabel, tUrl ->
                                title = tTitle
                                message = tMsg
                                selectedType = tType
                                selectedDisplayMode = tMode
                                actionLabel = tLabel
                                actionUrl = tUrl
                            },
                            onPreview = {
                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                onPreviewBroadcast(
                                    BroadcastMessage(
                                        id = "preview",
                                        issueNumber = editingIssueNumber ?: 0,
                                        title = title.trim(),
                                        message = message.trim(),
                                        type = selectedType,
                                        publishedAt = today,
                                        imageUrl = imageUrl.trim().ifBlank { null },
                                        actionLabel = actionLabel.trim().ifBlank { null },
                                        actionUrl = actionUrl.trim().ifBlank { null },
                                        notify = notify,
                                        displayMode = selectedDisplayMode,
                                        minVersion = minVersion.trim().ifBlank { null },
                                        maxVersion = maxVersion.trim().ifBlank { null },
                                        expiresAtEpochMs = expiryDays?.let { System.currentTimeMillis() + it * 86_400_000L },
                                    ),
                                )
                            },
                            onSend = {
                                val editId = editingIssueNumber
                                if (editId != null) {
                                    onUpdateBroadcast(
                                        editId,
                                        title,
                                        message,
                                        selectedType,
                                        imageUrl,
                                        actionLabel,
                                        actionUrl,
                                        notify,
                                        selectedDisplayMode,
                                        minVersion,
                                        maxVersion,
                                        expiryDays,
                                    ) {
                                        clearBroadcastComposer()
                                    }
                                } else {
                                    onSendBroadcast(
                                        title,
                                        message,
                                        selectedType,
                                        imageUrl,
                                        actionLabel,
                                        actionUrl,
                                        notify,
                                        selectedDisplayMode,
                                        minVersion,
                                        maxVersion,
                                        expiryDays,
                                    ) {
                                        clearBroadcastComposer()
                                    }
                                }
                            },
                        )
                    }

                    if (ui.broadcastHistory.isNotEmpty()) {
                        item(key = "broadcast_history") {
                            BroadcastHistorySection(
                                history = ui.broadcastHistory,
                                isComic = isComic,
                                lang = lang,
                                onReuse = { loadBroadcastIntoComposer(it, asEdit = false) },
                            )
                        }
                    }

                    item(key = "broadcast_session_strip") {
                        SessionSecurityStrip(
                            ownerLogin = ui.ownerLogin ?: "aayepp",
                            isComic = isComic,
                            lang = lang,
                            onSelectLanguage = onSelectDevLanguage,
                            onHideShortcut = onHideShortcut,
                            onSignOut = onRequestSignOut,
                        )
                    }
                }
            }
        }
    }
}

private fun BroadcastType.localizedLabel(lang: DevLanguage): String = when (this) {
    BroadcastType.INFO -> "Info"
    BroadcastType.WARNING -> if (lang == DevLanguage.BM) "Amaran" else "Warning"
    BroadcastType.MAINTENANCE -> if (lang == DevLanguage.BM) "Selenggara" else "Maintenance"
    BroadcastType.UPDATE -> "Update"
}

private fun BroadcastDisplayMode.localizedLabel(lang: DevLanguage): String = when (this) {
    BroadcastDisplayMode.POPUP -> if (lang == DevLanguage.BM) "Popup Sahaja" else "Popup Only"
    BroadcastDisplayMode.TICKER -> if (lang == DevLanguage.BM) "Top Home Bergerak" else "Scrolling Top Banner"
    BroadcastDisplayMode.BOTH -> if (lang == DevLanguage.BM) "Kedua-dua" else "Both"
}

@Composable
private fun InboxFeedbackCard(
    item: UserFeedbackItem,
    isComic: Boolean,
    lang: DevLanguage,
    translateTarget: DevLanguage,
    isBusy: Boolean,
    translation: FeedbackTranslation?,
    isTranslating: Boolean,
    isCreatingIssue: Boolean,
    createdIssue: Pair<Int, String>?,
    issueError: String?,
    onTranslate: () -> Unit,
    onOpenScreenshot: (String) -> Unit,
    onToggleResolved: () -> Unit,
    onDelete: () -> Unit,
    onCreateGitHubIssue: (BugTriageDigest) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bm = lang == DevLanguage.BM
    var showOriginal by rememberSaveable(item.id, translateTarget.code) { mutableStateOf(false) }
    var showAssistant by rememberSaveable(item.id) {
        mutableStateOf(item.category == FeedbackCategory.BUG && !item.isResolved)
    }
    var copiedPrompt by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(copiedPrompt) {
        if (copiedPrompt) {
            delay(2000L)
            copiedPrompt = false
        }
    }

    val hasTranslated = translation != null && !translation.alreadyTargetLang
    val showingTranslated = hasTranslated && !showOriginal
    val displayTitle = if (showingTranslated) translation.translatedTitle else item.title
    val displayBody = if (showingTranslated) translation.translatedBody else item.body
    val digest = remember(item.id, item.title, item.body, displayTitle, displayBody, lang) {
        BugTriageAnalyzer.analyze(item, displayTitle, displayBody, lang)
    }

    val catBg = when (item.category) {
        FeedbackCategory.BUG -> MaterialTheme.colorScheme.errorContainer
        FeedbackCategory.SUGGESTION -> MaterialTheme.colorScheme.primaryContainer
        FeedbackCategory.FEEDBACK -> MaterialTheme.colorScheme.secondaryContainer
    }
    val catFg = when (item.category) {
        FeedbackCategory.BUG -> MaterialTheme.colorScheme.onErrorContainer
        FeedbackCategory.SUGGESTION -> MaterialTheme.colorScheme.onPrimaryContainer
        FeedbackCategory.FEEDBACK -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val catLabel = when (item.category) {
        FeedbackCategory.BUG -> "Bug"
        FeedbackCategory.SUGGESTION -> if (bm) "Cadangan" else "Feature"
        FeedbackCategory.FEEDBACK -> "Feedback"
    }
    val catIcon = when (item.category) {
        FeedbackCategory.BUG -> Icons.Filled.BugReport
        FeedbackCategory.SUGGESTION -> Icons.Filled.Lightbulb
        FeedbackCategory.FEEDBACK -> Icons.Filled.ChatBubbleOutline
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = glassCardColor(
            if (item.isResolved) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
        border = devCardBorder(isComic),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (!item.isResolved) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = catBg,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(catIcon, contentDescription = null, tint = catFg, modifier = Modifier.size(13.dp))
                            Text(
                                text = catLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = catFg,
                            )
                        }
                    }
                    if (showingTranslated) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                        ) {
                            Text(
                                text = "${translation.sourceLang} → ${translation.targetLang.shortLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                    if (item.isResolved) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF30D158).copy(alpha = 0.18f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF30D158),
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = if (bm) "Selesai" else "Resolved",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF30D158),
                                )
                            }
                        }
                    }
                }
                Text(
                    text = formatRelativeSec(item.createdAtSec, lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = displayBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (item.isResolved) 0.72f else 0.9f),
            )

            if (item.screenshots.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        item.screenshots.take(5).forEach { shot ->
                            FeedbackScreenshotThumbnail(
                                source = shot,
                                onClick = { onOpenScreenshot(shot) },
                            )
                        }
                    }
                    Text(
                        text = if (bm) {
                            "${item.screenshots.size} Screenshot · Tekan gambar untuk zoom"
                        } else {
                            "${item.screenshots.size} Screenshot(s) · Tap image to zoom"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            val clipboard = LocalClipboardManager.current

            if (showAssistant) {
                val prioColor = when (digest.priority) {
                    BugPriority.HIGH -> MaterialTheme.colorScheme.error
                    BugPriority.MEDIUM -> MaterialTheme.colorScheme.tertiary
                    BugPriority.LOW -> MaterialTheme.colorScheme.primary
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.24f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    text = if (bm) "Pembantu Bug (Assistant)" else "Bug Assistant",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = prioColor.copy(alpha = 0.16f),
                            ) {
                                Text(
                                    text = digest.priority.label(lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = prioColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(
                            text = if (bm) "Apa berlaku: ${digest.plainExplanation}" else "What happened: ${digest.plainExplanation}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (bm) "Punca mungkin: ${digest.likelyCause}" else "Likely cause: ${digest.likelyCause}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (digest.steps.isNotEmpty()) {
                            Text(
                                text = (if (bm) "Langkah ulang: " else "Steps: ") +
                                    digest.steps.take(3).joinToString(" -> "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = (if (bm) "Fail disyaki: " else "Suspect files: ") +
                                digest.suspectFiles.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        OutlinedButton(
                            onClick = {
                                clipboard.setText(
                                    AnnotatedString(
                                        BugTriageAnalyzer.buildAiAgentPrompt(item, translation, digest, lang),
                                    ),
                                )
                                copiedPrompt = true
                            },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = if (copiedPrompt) Icons.Filled.CheckCircle else Icons.Filled.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when {
                                    copiedPrompt && bm -> "Prompt AI Agent Disalin!"
                                    copiedPrompt -> "AI Agent Prompt Copied!"
                                    bm -> "Salin Prompt Untuk AI Agent"
                                    else -> "Copy Prompt for AI Agent"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val prompt = BugTriageAnalyzer.buildAiAgentPrompt(item, translation, digest, lang)
                                    scope.launch {
                                        shareBugPack(context, prompt, item.screenshots).onFailure {
                                            Toast.makeText(
                                                context,
                                                if (bm) "Gagal kongsi bug pack" else "Failed to share bug pack",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    }
                                },
                                shape = CircleShape,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (bm) "Kongsi" else "Share Pack",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            }
                            if (createdIssue != null) {
                                val (num, url) = createdIssue
                                OutlinedButton(
                                    onClick = {
                                        if (url.isNotBlank()) {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            }
                                        }
                                    },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = "Issue #$num",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onCreateGitHubIssue(digest) },
                                    enabled = !isCreatingIssue,
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    if (isCreatingIssue) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Filled.BugReport, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            text = if (bm) "Buka Issue" else "GitHub Issue",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                        if (!issueError.isNullOrBlank()) {
                            Text(
                                text = issueError,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            val metaParts = buildList {
                if (item.contact.isNotBlank()) add(item.contact)
                if (item.appVersion.isNotBlank()) add("v${item.appVersion}")
                if (item.androidApi > 0) add("API ${item.androidApi}")
                if (item.deviceModel.isNotBlank()) add(item.deviceModel)
            }
            if (metaParts.isNotEmpty()) {
                Text(
                    text = metaParts.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(
                    onClick = {
                        when {
                            isTranslating -> Unit
                            hasTranslated -> showOriginal = !showOriginal
                            else -> onTranslate()
                        }
                    },
                    enabled = !isTranslating,
                ) {
                    Text(
                        text = when {
                            isTranslating -> if (bm) "Menterjemah..." else "Translating..."
                            showingTranslated -> if (bm) "Lihat Asal" else "Show Original"
                            hasTranslated -> if (bm) "Terjemah ${translateTarget.shortLabel}" else "Translate ${translateTarget.shortLabel}"
                            translation?.alreadyTargetLang == true -> if (bm) "Sudah ${translateTarget.shortLabel}" else "Already ${translateTarget.shortLabel}"
                            else -> if (bm) "Terjemah ${translateTarget.shortLabel}" else "Translate ${translateTarget.shortLabel}"
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                TextButton(onClick = { showAssistant = !showAssistant }) {
                    Text(
                        text = if (showAssistant) {
                            if (bm) "Tutup Assistant" else "Hide Assistant"
                        } else {
                            "Assistant"
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                TextButton(
                    onClick = onToggleResolved,
                    enabled = !isBusy,
                ) {
                    Text(
                        text = if (item.isResolved) {
                            if (bm) "Buka Semula" else "Reopen"
                        } else {
                            if (bm) "Selesai" else "Resolve"
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                TextButton(
                    onClick = onDelete,
                    enabled = !isBusy,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (bm) "Padam" else "Delete", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FeedbackScreenshotThumbnail(
    source: String,
    onClick: () -> Unit,
) {
    val modifier = Modifier
        .size(width = 68.dp, height = 52.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        .clickable(onClick = onClick)

    if (source.startsWith("data:image/", ignoreCase = true)) {
        val bitmap by produceState<ImageBitmap?>(initialValue = null, source) {
            value = decodeDataUriBitmap(source)
        }
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = "Screenshot",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }
    } else {
        AsyncImage(
            model = source,
            contentDescription = "Screenshot",
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

@Composable
private fun ScreenshotZoomDialog(
    source: String,
    lang: DevLanguage,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bm = lang == DevLanguage.BM
    var scale by remember(source) { mutableStateOf(1f) }
    var offset by remember(source) { mutableStateOf(Offset.Zero) }
    var isWorking by remember(source) { mutableStateOf(false) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = nextScale
        offset = if (nextScale > 1.05f) offset + panChange else Offset.Zero
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                        .pointerInput(source) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.05f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.2f
                                    }
                                },
                            )
                        }
                        .transformable(state = transformState),
                ) {
                    val imgModifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                    if (source.startsWith("data:image/", ignoreCase = true)) {
                        val bitmap by produceState<ImageBitmap?>(initialValue = null, source) {
                            value = decodeDataUriBitmap(source)
                        }
                        val bmp = bitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = "Screenshot",
                                contentScale = ContentScale.Fit,
                                modifier = imgModifier,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    } else {
                        AsyncImage(
                            model = source,
                            contentDescription = "Screenshot",
                            contentScale = ContentScale.Fit,
                            modifier = imgModifier,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (bm) "Cubit untuk zoom · Ketik 2x reset" else "Pinch to zoom · Double-tap reset",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        enabled = !isWorking,
                        onClick = {
                            scope.launch {
                                isWorking = true
                                val res = saveScreenshotToGallery(context, source)
                                isWorking = false
                                val msg = res.fold(
                                    onSuccess = { if (bm) "Disimpan ke Galeri" else "Saved to Pictures" },
                                    onFailure = { it.message ?: if (bm) "Gagal simpan" else "Save failed" },
                                )
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (bm) "Simpan" else "Save", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        enabled = !isWorking,
                        onClick = {
                            scope.launch {
                                isWorking = true
                                val res = shareScreenshot(context, source)
                                isWorking = false
                                res.onFailure {
                                    Toast.makeText(
                                        context,
                                        if (bm) "Gagal kongsi gambar" else "Share failed",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                }
                            }
                        },
                        shape = CircleShape,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (bm) "Kongsi" else "Share", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalDiagnosticsDialog(
    lang: DevLanguage,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val bm = lang == DevLanguage.BM
    var refreshKey by remember { mutableIntStateOf(0) }
    val diagnosticsText = remember(refreshKey) {
        buildString {
            val crashes = runCatching { CrashRecorder.crashFiles(context) }.getOrDefault(emptyList())
            appendLine("=== CRASH FILES (${crashes.size}) ===")
            val latest = crashes.firstOrNull()
            if (latest != null) {
                appendLine(latest.name)
                appendLine(runCatching { latest.readText(Charsets.UTF_8).take(1800) }.getOrDefault("(unreadable)"))
            } else {
                appendLine(if (bm) "(Tiada fail crash direkod)" else "(No crash files recorded)")
            }
            appendLine()
            appendLine("=== RECENT LOG TAIL ===")
            val tail = runCatching { AppLog.tail?.snapshot(40).orEmpty() }.getOrDefault(emptyList())
            if (tail.isEmpty()) {
                appendLine(if (bm) "(Log memori kosong)" else "(In-memory log tail empty)")
            } else {
                tail.forEach { appendLine(it.toString()) }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (bm) "Log & Crash Peranti" else "Device Logs & Crashes",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = diagnosticsText,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    clipboard.setText(AnnotatedString(diagnosticsText))
                    Toast.makeText(
                        context,
                        if (bm) "Log disalin" else "Logs copied",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
            ) {
                Text(if (bm) "Salin" else "Copy", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        runCatching { CrashRecorder.deleteAll(context) }
                        refreshKey++
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(if (bm) "Padam Crash" else "Clear Crashes")
                }
                TextButton(onClick = onDismiss) {
                    Text(if (bm) "Tutup" else "Close")
                }
            }
        },
    )
}

@Composable
private fun InboxCountTile(
    category: FeedbackCategory?,
    count: Int,
    selected: Boolean,
    isComic: Boolean,
    lang: DevLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bm = lang == DevLanguage.BM
    val (label, icon, tint) = when (category) {
        null -> Triple(if (bm) "Semua" else "All", Icons.Filled.Inbox, MaterialTheme.colorScheme.onSurface)
        FeedbackCategory.BUG -> Triple("Bug", Icons.Filled.BugReport, MaterialTheme.colorScheme.error)
        FeedbackCategory.SUGGESTION -> Triple(if (bm) "Cadangan" else "Feature", Icons.Filled.Lightbulb, MaterialTheme.colorScheme.tertiary)
        FeedbackCategory.FEEDBACK -> Triple("Feedback", Icons.Filled.ChatBubbleOutline, MaterialTheme.colorScheme.primary)
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = glassCardColor(
            if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = if (selected && !isComic) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else devCardBorder(isComic),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp),
        ) {
            Text(
                text = formatStatsNumber(count),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (category == null) MaterialTheme.colorScheme.onSurface else tint,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

private fun formatRelativeSec(epochSec: Long, lang: DevLanguage): String {
    if (epochSec <= 0L) return ""
    val bm = lang == DevLanguage.BM
    val diffSec = (System.currentTimeMillis() / 1000L - epochSec).coerceAtLeast(0L)
    return when {
        diffSec < 60L -> if (bm) "Baru saja" else "Just now"
        diffSec < 3600L -> if (bm) "${diffSec / 60L}m lalu" else "${diffSec / 60L}m ago"
        diffSec < 86400L -> if (bm) "${diffSec / 3600L}j lalu" else "${diffSec / 3600L}h ago"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(epochSec * 1000L))
    }
}

@Composable
private fun ActiveBroadcastsSection(
    openBroadcasts: List<BroadcastMessage>,
    isBusy: Boolean,
    isComic: Boolean,
    lang: DevLanguage,
    onEditBroadcast: (BroadcastMessage) -> Unit,
    onCloseBroadcast: (Int) -> Unit,
) {
    val bm = lang == DevLanguage.BM
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (bm) "Broadcast Aktif" else "Active Broadcasts",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (openBroadcasts.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = glassCardColor(MaterialTheme.colorScheme.surfaceContainer),
                border = devCardBorder(isComic),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (bm) "Tiada broadcast aktif" else "No active broadcasts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                openBroadcasts.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = glassCardColor(MaterialTheme.colorScheme.surfaceContainer),
                        border = devCardBorder(isComic),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = item.type.icon(),
                                contentDescription = item.type.localizedLabel(lang),
                                tint = if (isComic) ComicInk else item.type.accent(),
                                modifier = Modifier.size(22.dp),
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                val datePart = item.publishedAt.takeIf { it.isNotBlank() }?.let {
                                    " · ${formatBroadcastDate(it)}"
                                }.orEmpty()
                                val statusPrefix = if (bm) "Aktif" else "Active"
                                Text(
                                    text = "$statusPrefix · #${item.issueNumber} · ${item.displayMode.localizedLabel(lang)}$datePart",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                            TextButton(
                                onClick = { onEditBroadcast(item) },
                                enabled = !isBusy,
                            ) {
                                Text(if (bm) "Edit" else "Edit", fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(
                                onClick = { onCloseBroadcast(item.issueNumber) },
                                enabled = !isBusy,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Text(if (bm) "Tamat" else "End", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewBroadcastSection(
    editingIssueNumber: Int?,
    onCancelEdit: () -> Unit,
    selectedType: BroadcastType,
    onSelectType: (BroadcastType) -> Unit,
    selectedDisplayMode: BroadcastDisplayMode,
    onSelectDisplayMode: (BroadcastDisplayMode) -> Unit,
    title: String,
    onTitleChange: (String) -> Unit,
    message: String,
    onMessageChange: (String) -> Unit,
    imageUrl: String,
    onImageUrlChange: (String) -> Unit,
    actionLabel: String,
    onActionLabelChange: (String) -> Unit,
    actionUrl: String,
    onActionUrlChange: (String) -> Unit,
    minVersion: String,
    onMinVersionChange: (String) -> Unit,
    maxVersion: String,
    onMaxVersionChange: (String) -> Unit,
    expiryDays: Int?,
    onExpiryDaysChange: (Int?) -> Unit,
    notify: Boolean,
    onNotifyChange: (Boolean) -> Unit,
    statusMessage: String?,
    statusIsError: Boolean,
    isBusy: Boolean,
    isComic: Boolean,
    lang: DevLanguage,
    onApplyTemplate: (
        title: String,
        message: String,
        type: BroadcastType,
        displayMode: BroadcastDisplayMode,
        actionLabel: String,
        actionUrl: String,
    ) -> Unit,
    onPreview: () -> Unit,
    onSend: () -> Unit,
) {
    val bm = lang == DevLanguage.BM
    val fieldShape = RoundedCornerShape(14.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = when {
                    editingIssueNumber != null && bm -> "Edit Broadcast #$editingIssueNumber"
                    editingIssueNumber != null -> "Edit Broadcast #$editingIssueNumber"
                    bm -> "Broadcast Baru"
                    else -> "New Broadcast"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (editingIssueNumber != null) {
                TextButton(onClick = onCancelEdit, enabled = !isBusy) {
                    Text(if (bm) "Batal Edit" else "Cancel Edit", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = glassCardColor(MaterialTheme.colorScheme.surfaceContainer),
            border = devCardBorder(isComic),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (editingIssueNumber == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (bm) "Templat Pantas" else "Quick Templates",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(
                                (if (bm) "Update Baru" else "New Update") to {
                                    onApplyTemplate(
                                        if (bm) "Versi Baru Kini Tersedia!" else "New Version Available!",
                                        if (bm) "Kemas kini ke versi terkini untuk pembetulan audio dan kestabilan." else "Update to the latest version for audio fixes and stability.",
                                        BroadcastType.UPDATE,
                                        BroadcastDisplayMode.BOTH,
                                        if (bm) "Kemas Kini" else "Update Now",
                                        "claudewaves://update",
                                    )
                                },
                                (if (bm) "Selenggara" else "Maintenance") to {
                                    onApplyTemplate(
                                        if (bm) "Penyelenggaraan Server" else "Server Maintenance",
                                        if (bm) "Sebahagian ciri carian/stream mungkin perlahan buat sementara waktu." else "Some search/streaming features may be temporarily slower.",
                                        BroadcastType.MAINTENANCE,
                                        BroadcastDisplayMode.TICKER,
                                        "",
                                        "",
                                    )
                                },
                                "Hotfix" to {
                                    onApplyTemplate(
                                        if (bm) "Penting: Sila Kemas Kini" else "Important: Please Update",
                                        if (bm) "Kami telah membaiki isu main balik. Sila pasang versi terbaru." else "We fixed an urgent playback issue. Please install the latest build.",
                                        BroadcastType.WARNING,
                                        BroadcastDisplayMode.POPUP,
                                        if (bm) "Semak Update" else "Check Update",
                                        "claudewaves://update",
                                    )
                                },
                            ).forEach { (tplLabel, applyFn) ->
                                FilterChip(
                                    selected = false,
                                    onClick = applyFn,
                                    label = { Text(tplLabel, style = MaterialTheme.typography.labelSmall) },
                                    shape = CircleShape,
                                )
                            }
                        }
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val chipColors = if (isComic) {
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ComicAccent,
                            selectedLabelColor = ComicInk,
                        )
                    } else {
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    BroadcastType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { onSelectType(type) },
                            leadingIcon = {
                                Icon(
                                    imageVector = type.icon(),
                                    contentDescription = null,
                                    tint = if (isComic) ComicInk else type.accent(),
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            label = {
                                Text(
                                    text = type.localizedLabel(lang),
                                    fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Medium,
                                )
                            },
                            shape = CircleShape,
                            colors = chipColors,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (bm) "Gaya Paparan Dalam App" else "In-App Display Style",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        BroadcastDisplayMode.entries.forEach { mode ->
                            val selected = selectedDisplayMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectDisplayMode(mode) },
                                label = {
                                    Text(
                                        text = mode.localizedLabel(lang),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    )
                                },
                                shape = CircleShape,
                            )
                        }
                    }
                }

                val broadcastInputStyle = androidx.compose.material3.LocalTextStyle.current
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(if (bm) "Tajuk" else "Title") },
                    singleLine = true,
                    shape = fieldShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .sakugaTypingInput(
                            text = title,
                            textStyle = broadcastInputStyle,
                            leadingPadding = 16.dp,
                        ),
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    label = { Text(if (bm) "Mesej (Markdown)" else "Message (Markdown)") },
                    supportingText = {
                        Text(if (bm) "Sokong Markdown: **tebal**, - senarai, # tajuk" else "Supports Markdown: **bold**, - list, # heading")
                    },
                    minLines = 3,
                    maxLines = 6,
                    shape = fieldShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .sakugaTypingInput(
                            text = message,
                            textStyle = broadcastInputStyle,
                            leadingPadding = 16.dp,
                        ),
                )

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = onImageUrlChange,
                    label = { Text(if (bm) "URL Gambar Banner (pilihan)" else "Banner Image URL (optional)") },
                    placeholder = { Text(if (bm) "Kosong = banner default ClaudeWaves" else "Blank = default ClaudeWaves banner") },
                    singleLine = true,
                    shape = fieldShape,
                    modifier = Modifier.fillMaxWidth(),
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (bm) "Pautan (pilihan)" else "Action Link (optional)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = actionLabel,
                            onValueChange = onActionLabelChange,
                            placeholder = { Text("Label") },
                            singleLine = true,
                            shape = fieldShape,
                            modifier = Modifier.weight(0.42f),
                        )
                        OutlinedTextField(
                            value = actionUrl,
                            onValueChange = onActionUrlChange,
                            placeholder = { Text("https://... / claudewaves://...") },
                            singleLine = true,
                            shape = fieldShape,
                            modifier = Modifier.weight(0.58f),
                        )
                    }
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(
                            Triple("Update App", if (bm) "Kemas Kini" else "Update", "claudewaves://update"),
                            Triple("Feedback", if (bm) "Hantar Laporan" else "Feedback", "claudewaves://feedback"),
                            Triple("Audio", if (bm) "Tetapan Audio" else "Audio Settings", "claudewaves://settings/audio"),
                            Triple(if (bm) "Tema" else "Themes", if (bm) "Lihat Tema" else "Themes", "claudewaves://themes"),
                        ).forEach { (chipTitle, ctaLabel, deepUrl) ->
                            FilterChip(
                                selected = actionUrl.equals(deepUrl, ignoreCase = true),
                                onClick = {
                                    onActionLabelChange(ctaLabel)
                                    onActionUrlChange(deepUrl)
                                },
                                label = { Text(chipTitle, style = MaterialTheme.typography.labelSmall) },
                                shape = CircleShape,
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (bm) "Sasaran Versi App (pilihan)" else "Target App Version (optional)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = minVersion,
                            onValueChange = onMinVersionChange,
                            placeholder = { Text(if (bm) "Min (cth: 1.0.50)" else "Min (e.g. 1.0.50)") },
                            singleLine = true,
                            shape = fieldShape,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = maxVersion,
                            onValueChange = onMaxVersionChange,
                            placeholder = { Text(if (bm) "Max (cth: 1.0.59)" else "Max (e.g. 1.0.59)") },
                            singleLine = true,
                            shape = fieldShape,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (bm) "Tempoh Tamat Automatik" else "Auto-Expiry",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(
                            null to if (bm) "Tiada had" else "No limit",
                            1 to if (bm) "24 jam" else "24h",
                            3 to if (bm) "3 hari" else "3 days",
                            7 to if (bm) "7 hari" else "7 days",
                        ).forEach { (days, label) ->
                            FilterChip(
                                selected = expiryDays == days,
                                onClick = { onExpiryDaysChange(days) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                shape = CircleShape,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (bm) "Notifikasi Android" else "Android Notification",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Switch(
                        checked = notify,
                        onCheckedChange = onNotifyChange,
                    )
                }

                statusMessage?.let { feedback ->
                    Text(
                        text = feedback,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (statusIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }

                if (selectedDisplayMode != BroadcastDisplayMode.POPUP && title.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (bm) "Preview Top Home Bergerak" else "Scrolling Top Banner Preview",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        HomeBroadcastTickerPill(
                            message = BroadcastMessage(
                                id = "preview_ticker",
                                issueNumber = 0,
                                title = title.trim(),
                                message = message.trim(),
                                type = selectedType,
                                publishedAt = "",
                                actionLabel = actionLabel.trim().ifBlank { null },
                                actionUrl = actionUrl.trim().ifBlank { null },
                                notify = notify,
                                displayMode = selectedDisplayMode,
                            ),
                            onClick = onPreview,
                            onDismiss = {},
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onPreview,
                        enabled = title.isNotBlank() && message.isNotBlank() && !isBusy,
                        shape = CircleShape,
                        modifier = Modifier.weight(0.38f),
                    ) {
                        Text("Preview", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onSend,
                        enabled = title.isNotBlank() && message.isNotBlank() && !isBusy,
                        shape = CircleShape,
                        colors = devPrimaryButtonColors(isComic),
                        modifier = Modifier.weight(0.62f),
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = if (isComic) ComicInk else MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(
                                imageVector = if (editingIssueNumber != null) Icons.Filled.Edit else Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = when {
                                    editingIssueNumber != null && bm -> "Kemas Kini"
                                    editingIssueNumber != null -> "Update Broadcast"
                                    bm -> "Hantar Broadcast"
                                    else -> "Send Broadcast"
                                },
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BroadcastHistorySection(
    history: List<BroadcastMessage>,
    isComic: Boolean,
    lang: DevLanguage,
    onReuse: (BroadcastMessage) -> Unit,
) {
    val bm = lang == DevLanguage.BM
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (bm) "Sejarah Broadcast Lepas" else "Past Broadcasts",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            history.forEach { item ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = glassCardColor(MaterialTheme.colorScheme.surfaceContainerLow),
                    border = devCardBorder(isComic),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = item.type.icon(),
                            contentDescription = item.type.localizedLabel(lang),
                            tint = if (isComic) ComicInk else item.type.accent(),
                            modifier = Modifier.size(20.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            val datePart = item.publishedAt.takeIf { it.isNotBlank() }?.let { " · ${formatBroadcastDate(it)}" }.orEmpty()
                            Text(
                                text = "#${item.issueNumber} · ${item.displayMode.localizedLabel(lang)}$datePart",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                        TextButton(onClick = { onReuse(item) }) {
                            Text(if (bm) "Guna Semula" else "Reuse", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSecurityStrip(
    ownerLogin: String,
    isComic: Boolean,
    lang: DevLanguage,
    onSelectLanguage: (DevLanguage) -> Unit,
    onHideShortcut: () -> Unit,
    onSignOut: () -> Unit,
) {
    val bm = lang == DevLanguage.BM
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = glassCardColor(MaterialTheme.colorScheme.surfaceContainer),
        border = devCardBorder(isComic),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (bm) "Bahasa Dev Mode" else "Dev Mode Language",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DevLanguage.entries.forEach { option ->
                        val selected = lang == option
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectLanguage(option) },
                            label = {
                                Text(
                                    text = option.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                )
                            },
                            shape = CircleShape,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "@$ownerLogin · " + if (bm) "Auto-kunci bila keluar" else "Auto-lock on exit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onHideShortcut) {
                    Text(if (bm) "Sembunyi" else "Hide", fontWeight = FontWeight.SemiBold)
                }
                TextButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(if (bm) "Log Keluar" else "Sign Out", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DevGateIconBadge(
    shape: androidx.compose.ui.graphics.Shape,
    isComic: Boolean,
    tint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeSize: androidx.compose.ui.unit.Dp = 56.dp,
    iconSize: androidx.compose.ui.unit.Dp = 26.dp,
) {
    Surface(
        shape = shape,
        color = if (isComic) ComicAccent else glassCardColor(MaterialTheme.colorScheme.surfaceContainerHigh),
        border = devCardBorder(isComic),
        modifier = Modifier.size(badgeSize),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
internal fun devCardBorder(isComic: Boolean): BorderStroke? {
    if (isComic) return null
    return glassCardBorder() ?: BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
}

@Composable
private fun devPrimaryButtonColors(isComic: Boolean) = if (isComic) {
    ButtonDefaults.buttonColors(containerColor = ComicAccent, contentColor = ComicInk)
} else {
    ButtonDefaults.buttonColors()
}

internal fun formatStatsNumber(value: Number): String =
    NumberFormat.getIntegerInstance(Locale.US).format(value.toLong())

private fun formatClockTime(timeMs: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timeMs))
