package com.lastwave.app.ui.settings

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lastwave.app.data.update.BroadcastManager
import com.lastwave.app.data.update.BroadcastMessage
import com.lastwave.app.data.update.BroadcastType
import com.lastwave.app.ui.comic.Button
import com.lastwave.app.ui.comic.FilterChip
import com.lastwave.app.ui.comic.IconButton
import com.lastwave.app.ui.comic.OutlinedButton
import com.lastwave.app.ui.comic.Surface
import com.lastwave.app.ui.comic.Switch
import com.lastwave.app.ui.comic.TextButton
import com.lastwave.app.ui.theme.ComicAccent
import com.lastwave.app.ui.theme.ComicInk
import com.lastwave.app.ui.theme.LocalComicTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class BroadcastAdminUiState(
    val hasToken: Boolean = false,
    val ownerLogin: String? = null,
    val openBroadcasts: List<BroadcastMessage> = emptyList(),
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val isError: Boolean = false,
)

@HiltViewModel
class BroadcastAdminViewModel @Inject constructor(
    private val manager: BroadcastManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        BroadcastAdminUiState(
            hasToken = manager.hasOwnerToken(),
            ownerLogin = manager.savedOwnerLogin(),
        ),
    )
    internal val uiState: StateFlow<BroadcastAdminUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    hasToken = manager.hasOwnerToken(),
                    ownerLogin = manager.savedOwnerLogin(),
                )
            }
            val result = manager.fetchOpenBroadcasts()
            result.onSuccess { list ->
                _uiState.update { it.copy(openBroadcasts = list) }
            }
        }
    }

    fun saveToken(rawToken: String, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusMessage = null, isError = false) }
            val result = manager.saveAndVerifyOwnerToken(rawToken)
            result.onSuccess { login ->
                val open = manager.fetchOpenBroadcasts().getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        hasToken = true,
                        ownerLogin = login,
                        openBroadcasts = open,
                        isBusy = false,
                        statusMessage = "Token disahkan ($login)",
                        isError = false,
                    )
                }
                onSaved()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusMessage = err.message ?: "Gagal sahkan token",
                        isError = true,
                    )
                }
            }
        }
    }

    fun clearToken() {
        manager.clearOwnerToken()
        _uiState.update {
            it.copy(
                hasToken = false,
                ownerLogin = null,
                statusMessage = "Token dipadam dari peranti ini",
                isError = false,
            )
        }
    }

    fun send(
        title: String,
        message: String,
        type: BroadcastType,
        actionLabel: String,
        actionUrl: String,
        notify: Boolean,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusMessage = null, isError = false) }
            val result = manager.sendBroadcast(
                title = title,
                message = message,
                type = type,
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
                        statusMessage = "Broadcast dihantar (#${created.issueNumber})",
                        isError = false,
                    )
                }
                onSuccess()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusMessage = err.message ?: "Gagal hantar broadcast",
                        isError = true,
                    )
                }
            }
        }
    }

    fun close(issueNumber: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, statusMessage = null, isError = false) }
            val result = manager.closeBroadcast(issueNumber)
            result.onSuccess {
                val open = manager.fetchOpenBroadcasts().getOrDefault(emptyList())
                _uiState.update {
                    it.copy(
                        openBroadcasts = open,
                        isBusy = false,
                        statusMessage = "Broadcast #$issueNumber ditamatkan",
                        isError = false,
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusMessage = err.message ?: "Gagal tamatkan broadcast",
                        isError = true,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun BroadcastAdminSheet(
    onDismiss: () -> Unit,
    viewModel: BroadcastAdminViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val isComic = LocalComicTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showTokenEditor by rememberSaveable { mutableStateOf(!ui.hasToken) }
    var tokenInput by rememberSaveable { mutableStateOf("") }
    var revealToken by rememberSaveable { mutableStateOf(false) }

    var selectedType by rememberSaveable { mutableStateOf(BroadcastType.INFO) }
    var title by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    var actionLabel by rememberSaveable { mutableStateOf("") }
    var actionUrl by rememberSaveable { mutableStateOf("") }
    var notify by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    val fieldShape = RoundedCornerShape(14.dp)
    val panelShape = RoundedCornerShape(14.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            androidx.compose.material3.Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp),
            ) {}
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hantar Broadcast",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "GitHub Issues • ${ui.ownerLogin ?: "aayepp"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                val dotColor = if (ui.hasToken) Color(0xFF30D158) else MaterialTheme.colorScheme.error
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (isComic) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(dotColor),
                        )
                        Text(
                            text = if (ui.hasToken) "Token Aktif" else "Tiada Token",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                IconButton(
                    onClick = { showTokenEditor = !showTokenEditor },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Key,
                        contentDescription = "Tetapan token",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (showTokenEditor || !ui.hasToken) {
                Surface(
                    shape = panelShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = if (isComic) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Fine-Grained PAT GitHub (Issues: Read & Write pada aayepp/ClaudeWaves). Disimpan lokal dalam peranti ini sahaja.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            label = { Text("GitHub Token") },
                            singleLine = true,
                            shape = fieldShape,
                            visualTransformation = if (revealToken) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                androidx.compose.material3.IconButton(onClick = { revealToken = !revealToken }) {
                                    Icon(
                                        imageVector = if (revealToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = "Papar atau sembunyikan token",
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (ui.hasToken) {
                                TextButton(
                                    onClick = { viewModel.clearToken() },
                                    enabled = !ui.isBusy,
                                ) {
                                    Text("Padam")
                                }
                            }
                            Button(
                                onClick = {
                                    viewModel.saveToken(tokenInput) {
                                        tokenInput = ""
                                        showTokenEditor = false
                                    }
                                },
                                enabled = tokenInput.isNotBlank() && !ui.isBusy,
                                colors = adminPrimaryButtonColors(isComic),
                            ) {
                                Text("Sahkan & Simpan", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            ui.openBroadcasts.forEach { item ->
                Surface(
                    shape = panelShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = if (isComic) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF30D158)),
                        )
                        Text(
                            text = "Aktif: ${item.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(
                            onClick = { viewModel.close(item.issueNumber) },
                            enabled = ui.hasToken && !ui.isBusy,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text("Tamat", style = MaterialTheme.typography.labelMedium)
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
                        onClick = { selectedType = type },
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

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tajuk") },
                singleLine = true,
                shape = fieldShape,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Mesej") },
                minLines = 3,
                maxLines = 6,
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
                        onValueChange = { actionLabel = it },
                        placeholder = { Text("Label") },
                        singleLine = true,
                        shape = fieldShape,
                        modifier = Modifier.weight(0.42f),
                    )
                    OutlinedTextField(
                        value = actionUrl,
                        onValueChange = { actionUrl = it },
                        placeholder = { Text("URL") },
                        singleLine = true,
                        shape = fieldShape,
                        modifier = Modifier.weight(0.58f),
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
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
                    onCheckedChange = { notify = it },
                )
            }

            ui.statusMessage?.let { feedback ->
                Text(
                    text = feedback,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (ui.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }

            Button(
                onClick = {
                    viewModel.send(
                        title = title,
                        message = message,
                        type = selectedType,
                        actionLabel = actionLabel,
                        actionUrl = actionUrl,
                        notify = notify,
                    ) {
                        title = ""
                        message = ""
                        actionLabel = ""
                        actionUrl = ""
                    }
                },
                enabled = ui.hasToken && title.isNotBlank() && message.isNotBlank() && !ui.isBusy,
                shape = CircleShape,
                colors = adminPrimaryButtonColors(isComic),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (ui.isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
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

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun adminPrimaryButtonColors(isComic: Boolean) = if (isComic) {
    ButtonDefaults.buttonColors(containerColor = ComicAccent, contentColor = ComicInk)
} else {
    ButtonDefaults.buttonColors()
}
