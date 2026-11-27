package com.lastwave.app.ui.settings

import android.app.Activity
import android.app.KeyguardManager
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lastwave.app.data.update.AppStatsManager
import com.lastwave.app.data.update.BroadcastManager
import com.lastwave.app.data.update.BroadcastMessage
import com.lastwave.app.data.update.BroadcastType
import com.lastwave.app.data.update.DevLockState
import com.lastwave.app.data.update.DeveloperFeedbackSnapshot
import com.lastwave.app.data.update.DeveloperStatsSnapshot
import com.lastwave.app.data.update.FeedbackCategory
import com.lastwave.app.data.update.UserFeedbackItem
import com.lastwave.app.data.update.formatBroadcastDate
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
)

@HiltViewModel
class DeveloperConsoleViewModel @Inject constructor(
    private val manager: BroadcastManager,
    private val statsManager: AppStatsManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        DeveloperConsoleUiState(
            hasToken = manager.hasOwnerToken(),
            ownerLogin = manager.savedOwnerLogin(),
            lockState = manager.getDevLockState(),
        ),
    )
    internal val uiState: StateFlow<DeveloperConsoleUiState> = _uiState.asStateFlow()

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
                stats = null,
                isLoadingStats = false,
                pingFeedback = null,
                feedback = null,
                isLoadingFeedback = false,
                feedbackBusyId = null,
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
                _uiState.update {
                    it.copy(
                        hasToken = manager.hasOwnerToken(),
                        ownerLogin = manager.savedOwnerLogin(),
                        isUnlocked = false,
                        lockState = manager.getDevLockState(),
                        isBusy = false,
                        authError = err.message ?: "Gagal sahkan token",
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
                stats = null,
                isLoadingStats = false,
                pingFeedback = null,
                feedback = null,
                isLoadingFeedback = false,
                feedbackBusyId = null,
                authError = null,
                broadcastStatus = null,
                broadcastStatusIsError = false,
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
            }
        }
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
            if (_uiState.value.isUnlocked) {
                _uiState.update {
                    it.copy(
                        stats = snapshot,
                        isLoadingStats = false,
                        pingFeedback = if (ok) "Ping peranti berjaya direkod" else "Gagal hantar ping ke Worker",
                    )
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
        onSuccess: () -> Unit,
    ) {
        if (!_uiState.value.isUnlocked) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, broadcastStatus = null, broadcastStatusIsError = false) }
            val result = manager.sendBroadcast(
                title = title,
                message = message,
                type = type,
                imageUrl = imageUrl.ifBlank { null },
                actionLabel = actionLabel.ifBlank { null },
                actionUrl = actionUrl.ifBlank { null },
                notify = notify,
                closePrevious = true,
            )
            result.onSuccess { created ->
                val open = manager.fetchOpenBroadcasts().getOrDefault(listOf(created))
                _uiState.update {
                    it.copy(
                        openBroadcasts = open,
                        isBusy = false,
                        broadcastStatus = "Broadcast dihantar (#${created.issueNumber})",
                        broadcastStatusIsError = false,
                    )
                }
                onSuccess()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        broadcastStatus = err.message ?: "Gagal hantar broadcast",
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
            result.onSuccess {
                val open = manager.fetchOpenBroadcasts().getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        openBroadcasts = open,
                        isBusy = false,
                        broadcastStatus = "Broadcast #$issueNumber ditamatkan",
                        broadcastStatusIsError = false,
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        broadcastStatus = err.message ?: "Gagal tamatkan broadcast",
                        broadcastStatusIsError = true,
                    )
                }
            }
        }
    }
}

internal enum class DevMetricInfo(
    val title: String,
    val description: String,
) {
    ONLINE_15M(
        title = "Online (15m)",
        description = "Bilangan peranti unik yang menghantar ping dalam 15 minit terakhir. Menunjukkan anggaran pengguna sedang aktif membuka app sekarang.",
    ),
    NEW_TODAY(
        title = "Pengguna Baru Hari Ini",
        description = "Bilangan peranti unik yang pertama kali membuka app hari ini (berdasarkan ID rawak tanpa identiti peribadi).",
    ),
    DAU(
        title = "DAU — Daily Active Users",
        description = "Jumlah peranti unik yang aktif menggunakan app pada hari ini. Berguna untuk melihat trafik harian app.",
    ),
    WAU(
        title = "WAU — Weekly Active Users",
        description = "Jumlah peranti unik yang membuka app sekurang-kurangnya sekali dalam 7 hari terakhir.",
    ),
    MAU(
        title = "MAU — Monthly Active Users",
        description = "Jumlah peranti unik yang aktif dalam tempoh 30 hari terakhir (penanda aras jumlah pengguna aktif bulanan).",
    ),
    INSTALLS(
        title = "Installs — Jumlah Peranti",
        description = "Jumlah keseluruhan peranti unik yang pernah membuka versi app dengan statistik aktif sejak dilancarkan.",
    ),
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

    var showPatLogin by rememberSaveable { mutableStateOf(false) }
    var deviceAuthFallbackMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var awaitingDeviceCredential by remember { mutableStateOf(false) }
    var showSignOutConfirm by rememberSaveable { mutableStateOf(false) }
    var activeMetricHelp by remember { mutableStateOf<DevMetricInfo?>(null) }
    var previewMessage by remember { mutableStateOf<BroadcastMessage?>(null) }

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
                "Sahkan cap jari, wajah, atau PIN peranti",
            )
        } else {
            null
        }
        if (intent != null) {
            deviceAuthFallbackMessage = null
            awaitingDeviceCredential = true
            deviceUnlockLauncher.launch(intent)
        } else {
            deviceAuthFallbackMessage = "Peranti tiada kunci skrin (PIN/Cap Jari). Sahkan semula guna PAT."
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
            title = { Text("Log Keluar Developer?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Token @${ui.ownerLogin ?: "aayepp"} akan dipadam dan kad Developer Console akan disembunyikan daripada Settings.",
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
                    Text("Log Keluar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Batal")
                }
            },
        )
    }

    activeMetricHelp?.let { info ->
        val live = ui.stats?.liveStats
        val valueLine = when (info) {
            DevMetricInfo.ONLINE_15M -> live?.let { "Semasa: ${formatStatsNumber(it.online15m)} pengguna" }
            DevMetricInfo.NEW_TODAY -> live?.let { "Hari ini: +${formatStatsNumber(it.newToday)} peranti" }
            DevMetricInfo.DAU -> live?.let {
                "Hari ini: ${formatStatsNumber(it.dauToday)} • 24j: ${formatStatsNumber(it.active24h)}"
            }
            DevMetricInfo.WAU -> live?.let {
                "7 hari: ${formatStatsNumber(it.wau7d)} • Baru 7h: +${formatStatsNumber(it.new7d)}"
            }
            DevMetricInfo.MAU -> live?.let { "30 hari: ${formatStatsNumber(it.mau30d)} peranti unik" }
            DevMetricInfo.INSTALLS -> live?.let { "Sepanjang masa: ${formatStatsNumber(it.totalInstalls)} peranti" }
        }
        AlertDialog(
            onDismissRequest = { activeMetricHelp = null },
            title = { Text(info.title, fontWeight = FontWeight.Bold) },
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
                        text = info.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { activeMetricHelp = null }) {
                    Text("Faham", fontWeight = FontWeight.Bold)
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
                bottomClearance = bottomScrollClearance,
                onBack = onBack,
                onRefresh = viewModel::refreshAll,
                onLockNow = viewModel::lockSession,
                onTestPing = viewModel::sendTestPing,
                onHoldMetric = { activeMetricHelp = it },
                onRefreshFeedback = viewModel::refreshFeedback,
                onToggleFeedbackResolved = viewModel::toggleFeedbackResolved,
                onDeleteFeedback = viewModel::deleteFeedback,
                onCloseBroadcast = viewModel::closeBroadcast,
                onSendBroadcast = viewModel::sendBroadcast,
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
private fun Cooldown24hContent(
    unlockAtWallMs: Long,
    onExpired: () -> Unit,
    topInset: androidx.compose.ui.unit.Dp,
    bottomInset: androidx.compose.ui.unit.Dp,
    isComic: Boolean,
) {
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
    val remainingText = if (hours > 0) {
        "$hours jam $minutes minit"
    } else {
        "${minutes.coerceAtLeast(1)} minit"
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
            text = "Akses Dikunci",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Cuba semula dalam $remainingText",
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
                text = "3 percubaan gagal direkodkan. Selepas tempoh tamat, hanya ada 1 percubaan terakhir sebelum akses dikunci selamanya.",
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
) {
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
                Text("Guna Cap Jari / PIN", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onSwitchTokenOrSignOut) {
                Text("Tukar Token / Log Keluar")
            }
        }
        Text(
            text = "Auto-kunci apabila keluar halaman",
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
) {
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
            text = "Sahkan token pemilik untuk buka konsol",
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
                                    contentDescription = "Tampal token",
                                )
                            }
                            IconButton(onClick = { revealToken = !revealToken }) {
                                Icon(
                                    imageVector = if (revealToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "Papar atau sembunyikan token",
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
                        "Percubaan terakhir — gagal akan kunci akses secara kekal"
                    lockState.remainingAttempts in 1..2 ->
                        "Baki ${lockState.remainingAttempts} percubaan sebelum dikunci 24 jam"
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
                        Text("Sahkan & Buka", fontWeight = FontWeight.Bold)
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
                                Text("Batal")
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
                                Text("Log Keluar")
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = "Disahkan di server untuk @aayepp · Tiada rahsia dalam APK",
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
    bottomClearance: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLockNow: () -> Unit,
    onTestPing: () -> Unit,
    onHoldMetric: (DevMetricInfo) -> Unit,
    onRefreshFeedback: () -> Unit,
    onToggleFeedbackResolved: (UserFeedbackItem) -> Unit,
    onDeleteFeedback: (Long) -> Unit,
    onCloseBroadcast: (Int) -> Unit,
    onSendBroadcast: (
        title: String,
        message: String,
        type: BroadcastType,
        imageUrl: String,
        actionLabel: String,
        actionUrl: String,
        notify: Boolean,
        onSuccess: () -> Unit,
    ) -> Unit,
    onPreviewBroadcast: (BroadcastMessage) -> Unit,
    onHideShortcut: () -> Unit,
    onRequestSignOut: () -> Unit,
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    // Category (0 all, 1 bug, 2 suggestion, 3 feedback) and status (0 open, 1 resolved, 2 all) filter together.
    var inboxCategory by rememberSaveable { mutableIntStateOf(0) }
    var inboxStatus by rememberSaveable { mutableIntStateOf(0) }
    var selectedType by rememberSaveable { mutableStateOf(BroadcastType.INFO) }
    var title by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var imageUrl by rememberSaveable { mutableStateOf("") }
    var actionLabel by rememberSaveable { mutableStateOf("") }
    var actionUrl by rememberSaveable { mutableStateOf("") }
    var notify by rememberSaveable { mutableStateOf(true) }

    val statsListState = rememberLazyListState()
    val inboxListState = rememberLazyListState()
    val broadcastListState = rememberLazyListState()

    val unreadCount = ui.feedback?.unreadCount ?: 0
    val tabItems = remember(unreadCount) {
        listOf(
            ConnectedButtonItem(label = "Ringkasan"),
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
                        HeaderActionIcon(
                            icon = Icons.Filled.Refresh,
                            contentDescription = "Muat semula",
                            onClick = onRefresh,
                        )
                        HeaderActionIcon(
                            icon = Icons.Filled.Lock,
                            contentDescription = "Kunci konsol",
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
                            onHoldMetric = onHoldMetric,
                            onTestPing = onTestPing,
                            onRetry = onRefresh,
                        )
                    }
                    if (live != null && live.byCountry.isNotEmpty()) {
                        item(key = "stats_countries") { CountriesTile(live.byCountry, isComic) }
                    }
                    if (live != null && live.byVersion.isNotEmpty()) {
                        item(key = "stats_versions") { VersionsTile(live.byVersion, isComic) }
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
                                        text = "Dikemas kini ${formatClockTime(fetchedAt)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    item(key = "stats_session_strip") {
                        SessionSecurityStrip(
                            ownerLogin = ui.ownerLogin ?: "aayepp",
                            isComic = isComic,
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
                    val filtered = when (inboxStatus) {
                        0 -> inCategory.filter { !it.isResolved }
                        1 -> inCategory.filter { it.isResolved }
                        else -> inCategory
                    }

                    item(key = "inbox_summary") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.forEachIndexed { idx, category ->
                                InboxCountTile(
                                    category = category,
                                    count = category?.let { c -> allItems.count { it.category == c } } ?: allItems.size,
                                    selected = inboxCategory == idx,
                                    isComic = isComic,
                                    onClick = { inboxCategory = idx },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }

                    item(key = "inbox_status") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "Belum selesai · ${inCategory.count { !it.isResolved }}",
                                "Selesai · ${inCategory.count { it.isResolved }}",
                                "Semua",
                            ).forEachIndexed { idx, label ->
                                FilterChip(
                                    selected = inboxStatus == idx,
                                    onClick = { inboxStatus = idx },
                                    label = { Text(label, fontWeight = if (inboxStatus == idx) FontWeight.Bold else FontWeight.Medium) },
                                    shape = CircleShape,
                                )
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
                                            Text("Cuba lagi")
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
                                            text = "Tiada mesej dalam kategori ini",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            items(filtered, key = { "fb_${it.id}" }) { item ->
                                InboxFeedbackCard(
                                    item = item,
                                    isComic = isComic,
                                    isBusy = ui.feedbackBusyId == item.id,
                                    onToggleResolved = { onToggleFeedbackResolved(item) },
                                    onDelete = { onDeleteFeedback(item.id) },
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
                            onCloseBroadcast = onCloseBroadcast,
                        )
                    }

                    item(key = "broadcast_composer") {
                        NewBroadcastSection(
                            selectedType = selectedType,
                            onSelectType = { selectedType = it },
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
                            notify = notify,
                            onNotifyChange = { notify = it },
                            statusMessage = ui.broadcastStatus,
                            statusIsError = ui.broadcastStatusIsError,
                            isBusy = ui.isBusy,
                            isComic = isComic,
                            onPreview = {
                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                onPreviewBroadcast(
                                    BroadcastMessage(
                                        id = "preview",
                                        issueNumber = 0,
                                        title = title.trim(),
                                        message = message.trim(),
                                        type = selectedType,
                                        publishedAt = today,
                                        imageUrl = imageUrl.trim().ifBlank { null },
                                        actionLabel = actionLabel.trim().ifBlank { null },
                                        actionUrl = actionUrl.trim().ifBlank { null },
                                        notify = notify,
                                    ),
                                )
                            },
                            onSend = {
                                onSendBroadcast(
                                    title,
                                    message,
                                    selectedType,
                                    imageUrl,
                                    actionLabel,
                                    actionUrl,
                                    notify,
                                ) {
                                    title = ""
                                    message = ""
                                    imageUrl = ""
                                    actionLabel = ""
                                    actionUrl = ""
                                }
                            },
                        )
                    }

                    item(key = "broadcast_session_strip") {
                        SessionSecurityStrip(
                            ownerLogin = ui.ownerLogin ?: "aayepp",
                            isComic = isComic,
                            onHideShortcut = onHideShortcut,
                            onSignOut = onRequestSignOut,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxFeedbackCard(
    item: UserFeedbackItem,
    isComic: Boolean,
    isBusy: Boolean,
    onToggleResolved: () -> Unit,
    onDelete: () -> Unit,
) {
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
        FeedbackCategory.SUGGESTION -> "Cadangan"
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
                                    text = "Selesai",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF30D158),
                                )
                            }
                        }
                    }
                }
                Text(
                    text = formatRelativeSec(item.createdAtSec),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            Text(
                text = item.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (item.isResolved) 0.72f else 0.9f),
            )

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onToggleResolved,
                    enabled = !isBusy,
                ) {
                    Text(
                        text = if (item.isResolved) "Buka Semula" else "Tandakan Selesai",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                val clipboard = LocalClipboardManager.current
                TextButton(
                    onClick = {
                        // One paste carries everything needed to reproduce the report elsewhere.
                        clipboard.setText(
                            AnnotatedString(
                                "[$catLabel] ${item.title}\n${item.body}" +
                                    (if (metaParts.isNotEmpty()) "\n" + metaParts.joinToString(" · ") else ""),
                            ),
                        )
                    },
                ) {
                    Text("Salin", fontWeight = FontWeight.SemiBold)
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
                    Text("Padam", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun InboxCountTile(
    category: FeedbackCategory?,
    count: Int,
    selected: Boolean,
    isComic: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (label, icon, tint) = when (category) {
        null -> Triple("Semua", Icons.Filled.Inbox, MaterialTheme.colorScheme.onSurface)
        FeedbackCategory.BUG -> Triple("Bug", Icons.Filled.BugReport, MaterialTheme.colorScheme.error)
        FeedbackCategory.SUGGESTION -> Triple("Cadangan", Icons.Filled.Lightbulb, MaterialTheme.colorScheme.tertiary)
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

private fun formatRelativeSec(epochSec: Long): String {
    if (epochSec <= 0L) return ""
    val diffSec = (System.currentTimeMillis() / 1000L - epochSec).coerceAtLeast(0L)
    return when {
        diffSec < 60L -> "Baru saja"
        diffSec < 3600L -> "${diffSec / 60L}m lalu"
        diffSec < 86400L -> "${diffSec / 3600L}j lalu"
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(epochSec * 1000L))
    }
}

@Composable
private fun ActiveBroadcastsSection(
    openBroadcasts: List<BroadcastMessage>,
    isBusy: Boolean,
    isComic: Boolean,
    onCloseBroadcast: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Active Broadcasts",
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
                    text = "Tiada broadcast aktif",
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
                                contentDescription = item.type.label,
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
                                Text(
                                    text = "Aktif · #${item.issueNumber}$datePart",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                            TextButton(
                                onClick = { onCloseBroadcast(item.issueNumber) },
                                enabled = !isBusy,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Text("Tamat", fontWeight = FontWeight.SemiBold)
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
    selectedType: BroadcastType,
    onSelectType: (BroadcastType) -> Unit,
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
    notify: Boolean,
    onNotifyChange: (Boolean) -> Unit,
    statusMessage: String?,
    statusIsError: Boolean,
    isBusy: Boolean,
    isComic: Boolean,
    onPreview: () -> Unit,
    onSend: () -> Unit,
) {
    val fieldShape = RoundedCornerShape(14.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "New Broadcast",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
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
                                    text = type.label,
                                    fontWeight = if (selectedType == type) FontWeight.Bold else FontWeight.Medium,
                                )
                            },
                            shape = CircleShape,
                            colors = chipColors,
                        )
                    }
                }

                val broadcastInputStyle = androidx.compose.material3.LocalTextStyle.current
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Tajuk") },
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
                    label = { Text("Mesej (Markdown)") },
                    supportingText = { Text("Sokong Markdown: **tebal**, - senarai, # tajuk") },
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
                    label = { Text("URL Gambar Banner (pilihan)") },
                    placeholder = { Text("Kosong = banner default ClaudeWaves") },
                    singleLine = true,
                    shape = fieldShape,
                    modifier = Modifier.fillMaxWidth(),
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Pautan (pilihan)",
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
                            placeholder = { Text("URL") },
                            singleLine = true,
                            shape = fieldShape,
                            modifier = Modifier.weight(0.58f),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Notifikasi Android",
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
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Hantar Broadcast", fontWeight = FontWeight.Bold)
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
    onHideShortcut: () -> Unit,
    onSignOut: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = glassCardColor(MaterialTheme.colorScheme.surfaceContainer),
        border = devCardBorder(isComic),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
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
                text = "@$ownerLogin · Auto-kunci bila keluar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onHideShortcut) {
                Text("Sembunyi", fontWeight = FontWeight.SemiBold)
            }
            TextButton(
                onClick = onSignOut,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Log Keluar", fontWeight = FontWeight.SemiBold)
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
