package com.lastwave.app.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.lastwave.app.playback.MusicPlayer
import com.lastwave.app.playback.MusicPlayerState
import com.lastwave.app.presence.DiscordActivityType
import com.lastwave.app.presence.DiscordConnectionStatus
import com.lastwave.app.presence.DiscordPresence
import com.lastwave.app.presence.DiscordPresenceManager
import com.lastwave.app.presence.DiscordPresencePreferences
import com.lastwave.app.presence.DiscordSettings
import com.lastwave.app.ui.comic.AlertDialog
import com.lastwave.app.ui.comic.Button
import com.lastwave.app.ui.comic.FilledTonalButton
import com.lastwave.app.ui.comic.OutlinedButton
import com.lastwave.app.ui.comic.Surface
import com.lastwave.app.ui.comic.TextButton
import com.lastwave.app.ui.common.ArtworkImage
import com.lastwave.app.ui.common.ConnectedButtonGroup
import com.lastwave.app.ui.common.ConnectedButtonItem
import com.lastwave.app.ui.common.ExpressiveHeader
import com.lastwave.app.ui.common.GlassHeaderLayout
import com.lastwave.app.ui.common.adaptiveContentWidth
import com.lastwave.app.ui.theme.glassCardBorder
import com.lastwave.app.ui.theme.glassCardColor
import com.lastwave.app.ui.theme.pageBackground
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class DiscordSettingsViewModel @Inject constructor(
    private val preferences: DiscordPresencePreferences,
    private val presenceManager: DiscordPresenceManager,
    musicPlayer: MusicPlayer,
) : ViewModel() {

    val settings: StateFlow<DiscordSettings> = preferences.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = DiscordSettings(),
    )

    val connectionStatus: StateFlow<DiscordConnectionStatus> = presenceManager.connectionStatus
    val playerState: StateFlow<MusicPlayerState> = musicPlayer.state

    private val _signingIn = MutableStateFlow(false)
    val signingIn: StateFlow<Boolean> = _signingIn.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setEnabled(enabled)
            presenceManager.refresh()
        }
    }

    fun setShowAudioQuality(show: Boolean) {
        viewModelScope.launch {
            preferences.setShowAudioQuality(show)
            presenceManager.refresh()
        }
    }

    fun setShowListenButton(show: Boolean) {
        viewModelScope.launch {
            preferences.setShowListenButton(show)
            presenceManager.refresh()
        }
    }

    fun setShowAppButton(show: Boolean) {
        viewModelScope.launch {
            preferences.setShowAppButton(show)
            presenceManager.refresh()
        }
    }

    fun setActivityType(type: DiscordActivityType) {
        viewModelScope.launch {
            preferences.setActivityType(type)
            presenceManager.refresh()
        }
    }

    fun setPresenceStatus(status: String) {
        viewModelScope.launch {
            preferences.setPresenceStatus(status)
            presenceManager.refresh()
        }
    }

    fun signInWithToken(token: String, onSuccess: () -> Unit) {
        if (_signingIn.value) return
        viewModelScope.launch {
            _signingIn.value = true
            _error.value = null
            val res = withContext(NonCancellable) {
                presenceManager.signInWithToken(token)
            }
            _signingIn.value = false
            res.onSuccess {
                onSuccess()
            }.onFailure { err ->
                _error.value = err.message ?: "Failed to verify Discord token"
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                presenceManager.signOut()
            }
            _error.value = null
        }
    }

    fun clearError() {
        _error.value = null
    }
}

@Composable
fun DiscordSettingsScreen(
    onBack: () -> Unit,
    onOpenLogin: () -> Unit,
    viewModel: DiscordSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val signingIn by viewModel.signingIn.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showTokenDialog by rememberSaveable { mutableStateOf(false) }
    var showSignOutConfirm by rememberSaveable { mutableStateOf(false) }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pageBackground(),
        contentAlignment = Alignment.TopCenter,
    ) {
        GlassHeaderLayout(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentWidth(maxWidth = 720.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            header = { headerModifier, headerBackdrop ->
                ExpressiveHeader(
                    modifier = headerModifier,
                    backdrop = headerBackdrop,
                    title = "Discord Rich Presence",
                    subtitle = if (settings.isSignedIn) {
                        "Connected as @${settings.username}"
                    } else {
                        "Share what you play on Discord"
                    },
                    onBack = onBack,
                )
            },
        ) { headerInset ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = headerInset + 12.dp,
                    bottom = 32.dp + navBottom,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Account & Connection Status Card
                item(key = "discord_account") {
                    DiscordAccountCard(
                        settings = settings,
                        status = connectionStatus,
                        signingIn = signingIn,
                        onSignIn = onOpenLogin,
                        onPasteToken = {
                            viewModel.clearError()
                            showTokenDialog = true
                        },
                        onSignOut = { showSignOutConfirm = true },
                    )
                }

                // 2. Live Discord Activity Card Preview
                item(key = "discord_preview") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel("Live Card Preview")
                        DiscordPresencePreviewCard(
                            settings = settings,
                            playerState = playerState,
                        )
                    }
                }

                // 3. Behavior & Display Controls
                item(key = "discord_controls") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel("Presence Options")
                        SettingsGroup(rowCount = 4) { index, position ->
                            when (index) {
                                0 -> SettingsToggleCard(
                                    icon = Icons.Filled.Public,
                                    iconContainer = MaterialTheme.colorScheme.primaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    title = "Enable Discord Rich Presence",
                                    subtitle = if (settings.enabled) {
                                        "Publishes your playing track while music is active"
                                    } else {
                                        "Paused — nothing is shown on your Discord profile"
                                    },
                                    checked = settings.enabled,
                                    onCheckedChange = viewModel::setEnabled,
                                    position = position,
                                )
                                1 -> SettingsToggleCard(
                                    icon = Icons.Filled.GraphicEq,
                                    iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    title = "Show audio quality",
                                    subtitle = "Display Hi-Res Lossless, bit depth & sample rate on the card",
                                    checked = settings.showAudioQuality,
                                    onCheckedChange = viewModel::setShowAudioQuality,
                                    position = position,
                                )
                                2 -> SettingsToggleCard(
                                    icon = Icons.Filled.Link,
                                    iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    title = "Listen on YouTube Music button",
                                    subtitle = "Adds a direct YouTube Music link for the current track",
                                    checked = settings.showListenButton,
                                    onCheckedChange = viewModel::setShowListenButton,
                                    position = position,
                                )
                                3 -> SettingsToggleCard(
                                    icon = Icons.Filled.SmartButton,
                                    iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    title = "Get ClaudeWaves button",
                                    subtitle = "Shows a link to ClaudeWaves releases below the track",
                                    checked = settings.showAppButton,
                                    onCheckedChange = viewModel::setShowAppButton,
                                    position = position,
                                )
                            }
                        }
                    }
                }

                // 4. Activity Type & Online Status Pickers
                item(key = "discord_style") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel("Activity Style")
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = glassCardColor(),
                            border = glassCardBorder(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Activity label",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    val types = DiscordActivityType.entries
                                    ConnectedButtonGroup(
                                        items = types.map { type ->
                                            ConnectedButtonItem(
                                                label = type.label,
                                                icon = when (type) {
                                                    DiscordActivityType.LISTENING -> Icons.Filled.Headphones
                                                    DiscordActivityType.PLAYING -> Icons.Filled.PlayArrow
                                                    DiscordActivityType.WATCHING -> Icons.Filled.Visibility
                                                },
                                            )
                                        },
                                        selectedIndex = types.indexOf(settings.activityType).coerceAtLeast(0),
                                        onSelect = { idx -> viewModel.setActivityType(types[idx]) },
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Discord status while playing",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    val statuses = listOf("online" to "Online", "idle" to "Idle", "dnd" to "DND")
                                    ConnectedButtonGroup(
                                        items = statuses.map { ConnectedButtonItem(label = it.second) },
                                        selectedIndex = statuses.indexOfFirst { it.first == settings.presenceStatus }
                                            .coerceAtLeast(0),
                                        onSelect = { idx -> viewModel.setPresenceStatus(statuses[idx].first) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTokenDialog) {
        var tokenInput by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!signingIn) showTokenDialog = false },
            title = { Text("Paste Discord Token") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your token stays on this device and connects directly to Discord's gateway.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = {
                            tokenInput = it
                            viewModel.clearError()
                        },
                        label = { Text("Discord user token") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = error != null,
                        supportingText = error?.let { msg -> { Text(msg) } },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = tokenInput.isNotBlank() && !signingIn,
                    onClick = {
                        viewModel.signInWithToken(tokenInput) {
                            showTokenDialog = false
                            Toast.makeText(context, "Connected to Discord", Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    if (signingIn) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Connect")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !signingIn,
                    onClick = { showTokenDialog = false },
                ) { Text("Cancel") }
            },
        )
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Disconnect Discord?") },
            text = { Text("Your Discord token will be removed from this device and your playing status will be cleared.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        viewModel.signOut()
                        Toast.makeText(context, "Disconnected from Discord", Toast.LENGTH_SHORT).show()
                    },
                ) { Text("Disconnect") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun DiscordAccountCard(
    settings: DiscordSettings,
    status: DiscordConnectionStatus,
    signingIn: Boolean,
    onSignIn: () -> Unit,
    onPasteToken: () -> Unit,
    onSignOut: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = glassCardColor(),
        border = glassCardBorder(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (settings.isSignedIn) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (settings.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = settings.avatarUrl,
                                contentDescription = settings.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = settings.displayName.ifBlank { settings.username.ifBlank { "Discord User" } },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (settings.username.isNotBlank()) {
                            Text(
                                text = "@${settings.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        val (badgeText, badgeColor) = when {
                            !settings.enabled -> "Paused" to MaterialTheme.colorScheme.onSurfaceVariant
                            status == DiscordConnectionStatus.CONNECTED -> "Live on Discord" to MaterialTheme.colorScheme.primary
                            status == DiscordConnectionStatus.CONNECTING -> "Connecting to gateway…" to MaterialTheme.colorScheme.secondary
                            status == DiscordConnectionStatus.AUTH_FAILED -> "Session expired — sign in again" to MaterialTheme.colorScheme.error
                            else -> "Ready — plays when music starts" to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor),
                            )
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = badgeColor,
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (status == DiscordConnectionStatus.AUTH_FAILED) {
                        Button(onClick = onSignIn, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sign in again")
                        }
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Disconnect")
                    }
                }
            } else {
                Text(
                    text = "Connect Discord",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Sign in to Discord to display your currently playing songs, Hi-Res audio quality, album cover and YouTube Music link live on your Discord profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Your token stays strictly on this device and connects only to Discord's gateway. Third-party gateway presence is outside Discord's official API — use at your own discretion.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            lineHeight = 16.sp,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick = onSignIn,
                        enabled = !signingIn,
                        modifier = Modifier.weight(1.3f),
                    ) {
                        Icon(Icons.Filled.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sign in", maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = onPasteToken,
                        enabled = !signingIn,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Token", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscordPresencePreviewCard(
    settings: DiscordSettings,
    playerState: MusicPlayerState,
) {
    val track = playerState.current
    val title = track?.title?.takeIf { it.isNotBlank() } ?: "BbY WOW"
    val artist = track?.artist?.takeIf { it.isNotBlank() } ?: "KAROL G"
    val quality = if (track != null) {
        DiscordPresence.qualityLine(playerState)
    } else {
        "Hi-Res Lossless \u00B7 24-bit \u00B7 96 kHz"
    }
    val hasWatchUrl = track == null || DiscordPresence.youtubeWatchUrl(track.videoId) != null
    val speed = playerState.speed.takeIf { it.isFinite() && it > 0.1f } ?: 1.0f
    val displayTitle = if (track != null && kotlin.math.abs(speed - 1.0f) >= 0.01f) {
        "$title [${String.format(java.util.Locale.US, "%.2fx", speed)}]"
    } else {
        title
    }
    val subtitle = if (settings.showAudioQuality && quality.isNotEmpty()) {
        "$artist \u00B7 $quality"
    } else {
        artist
    }
    val totalMs = if (track != null) DiscordPresence.durationMs(playerState) else 225_000L
    val posMs = if (track != null && totalMs > 0L) playerState.positionMs.coerceIn(0L, totalMs) else 64_000L
    val progress = if (totalMs > 0L) (posMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f) else 0.36f

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = glassCardColor(MaterialTheme.colorScheme.surfaceContainerHighest),
        border = glassCardBorder(),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${settings.activityType.prefix} ClaudeWaves".uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = settings.presenceStatus.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    if (track != null) {
                        ArtworkImage(
                            name = track.title,
                            artist = track.artist,
                            embeddedUrl = track.artworkUrl,
                            fallbackIcon = Icons.Filled.Headphones,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Headphones,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    // Simulated Discord timeline bar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = formatPreviewDuration(posMs),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                        }
                        Text(
                            text = formatPreviewDuration(totalMs),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            AnimatedVisibility(visible = (settings.showListenButton && hasWatchUrl) || settings.showAppButton) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (settings.showListenButton && hasWatchUrl) {
                        PreviewCardButton(label = DiscordPresence.GATEWAY_BUTTON_1)
                    }
                    if (settings.showAppButton) {
                        PreviewCardButton(label = DiscordPresence.DEFAULT_BUTTON_2)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewCardButton(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatPreviewDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSec / 60L
    val secs = totalSec % 60L
    return String.format(java.util.Locale.US, "%d:%02d", mins, secs)
}
