package com.lastwave.app.ui.update

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.data.update.AppUpdateManager
import com.lastwave.app.data.update.BroadcastManager
import com.lastwave.app.data.update.BroadcastMessage
import com.lastwave.app.data.update.BroadcastType
import com.lastwave.app.data.update.formatBroadcastDate
import com.lastwave.app.ui.comic.Button
import com.lastwave.app.ui.comic.TextButton
import com.lastwave.app.ui.theme.ComicAccent
import com.lastwave.app.ui.theme.ComicInk
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.comicPanel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BroadcastDialogViewModel @Inject constructor(
    val manager: BroadcastManager,
    val updateManager: AppUpdateManager,
) : ViewModel()

/**
 * Root host for owner announcements polled from GitHub Issues (`broadcast` label).
 * Waits until splash dismissal and yields while the APK update dialog is on screen.
 */
@Composable
fun BroadcastDialogHost(
    splashDismissed: Boolean,
    viewModel: BroadcastDialogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.manager.state.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateManager.updateInfo.collectAsStateWithLifecycle()
    val updateDialogVisible = updateInfo.showDialog && updateInfo.isUpdateAvailable
    val active = uiState.active ?: return
    if (!splashDismissed || updateDialogVisible || !uiState.showDialog) return

    BroadcastDialog(
        message = active,
        onDismiss = { viewModel.manager.dismiss(active.id) },
    )
}

@Composable
private fun BroadcastDialog(
    message: BroadcastMessage,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val isComic = LocalComicTheme.current
    val cardShape = RoundedCornerShape(if (isComic) 20.dp else 28.dp)
    val accent = when (message.type) {
        BroadcastType.INFO -> MaterialTheme.colorScheme.primary
        BroadcastType.WARNING -> MaterialTheme.colorScheme.error
        BroadcastType.MAINTENANCE -> MaterialTheme.colorScheme.tertiary
        BroadcastType.UPDATE -> MaterialTheme.colorScheme.secondary
    }
    val icon: ImageVector = when (message.type) {
        BroadcastType.INFO -> Icons.Filled.Campaign
        BroadcastType.WARNING -> Icons.Filled.Warning
        BroadcastType.MAINTENANCE -> Icons.Filled.Build
        BroadcastType.UPDATE -> Icons.Filled.NewReleases
    }
    val iconContainer = when {
        isComic && message.type == BroadcastType.WARNING -> MaterialTheme.colorScheme.errorContainer
        isComic -> ComicAccent
        else -> accent.copy(alpha = 0.16f)
    }
    val iconTint = when {
        isComic && message.type == BroadcastType.WARNING -> MaterialTheme.colorScheme.onErrorContainer
        isComic -> ComicInk
        else -> accent
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = cardShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = if (isComic) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 520.dp)
                .comicPanel(cardShape),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = iconContainer,
                        border = if (isComic) BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else null,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = message.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val dateText = formatBroadcastDate(message.publishedAt).takeIf { it.isNotBlank() }
                        val subtitle = listOfNotNull(dateText, message.type.label).joinToString(" • ")
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (message.message.isNotBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = message.message,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val actionUrl = message.actionUrl
                    val buttonColors = if (isComic) {
                        ButtonDefaults.buttonColors(containerColor = ComicAccent, contentColor = ComicInk)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                    if (!actionUrl.isNullOrBlank()) {
                        TextButton(onClick = onDismiss) {
                            Text("Tutup")
                        }
                        Button(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(actionUrl)))
                                }
                                onDismiss()
                            },
                            colors = buttonColors,
                        ) {
                            Text(
                                text = message.actionLabel?.ifBlank { null } ?: "Info Lanjut",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            colors = buttonColors,
                        ) {
                            Text("Faham", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
