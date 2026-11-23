package com.lastwave.app.ui.update

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.R
import com.lastwave.app.data.update.AppUpdateManager
import com.lastwave.app.data.update.UpdateInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

@HiltViewModel
class AppUpdateDialogViewModel @Inject constructor(
    val manager: AppUpdateManager,
) : ViewModel() {
    val info = manager.updateInfo
}

/**
 * The in-app update dialog, as on desktop: opens by itself when a new release
 * is found on launch, and from Settings' "Update Ready". Update now opens
 * the APK download in the browser, as before. Hosted once at the app root.
 */
@Composable
fun AppUpdateDialogHost(viewModel: AppUpdateDialogViewModel = hiltViewModel()) {
    val info by viewModel.info.collectAsStateWithLifecycle()
    if (!info.showDialog || !info.isUpdateAvailable) return
    val context = LocalContext.current
    val manager = viewModel.manager
    AppUpdateDialog(
        info = info,
        onUpdate = { manager.downloadAndInstallUpdate(context) },
        onCancelDownload = { manager.cancelDownload() },
        onClose = { skipThisVersion ->
            if (skipThisVersion) manager.dismissUpdate(info.latestVersion)
            manager.hideDialog()
        },
    )
}

@Composable
private fun AppUpdateDialog(
    info: UpdateInfo,
    onUpdate: () -> Unit,
    onCancelDownload: () -> Unit,
    onClose: (skipThisVersion: Boolean) -> Unit,
) {
    var skipThisVersion by rememberSaveable { mutableStateOf(false) }
    Dialog(
        onDismissRequest = { onClose(skipThisVersion) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val dialogSurface = MaterialTheme.colorScheme.surfaceContainerHigh
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = dialogSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 560.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                val versionBadge = if (info.currentVersion.isNotBlank()) {
                    "v${info.currentVersion}  →  v${info.latestVersion}"
                } else {
                    "v${info.latestVersion}"
                }
                val dateLabel = formatReleaseDate(info.publishedAt)?.let { "•  $it" }
                UpdateHeroHeader(
                    icon = Icons.Filled.SystemUpdate,
                    title = "Update Arrived",
                    badgeText = versionBadge,
                    subtitleText = dateLabel,
                    surfaceColor = dialogSurface,
                )

                Column(
                    modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 4.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (info.releaseTitle.isNotBlank() && !info.releaseTitle.contains(info.latestVersion)) {
                        Text(
                            text = info.releaseTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    // Release notes card
                    val notesStyle = releaseNotesStyle()
                    val notes = remember(info.releaseNotes, notesStyle) {
                        releaseNotesText(
                            info.releaseNotes.ifBlank { "No release notes were published for this version." },
                            notesStyle,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "RELEASE NOTES",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.0.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            )
                            Text(
                                text = notes,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
                            )
                        }
                    }

                    // Package badge pill
                    info.downloadFileName?.let { name ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
                        ) {
                            Text(
                                text = if (info.downloadSizeBytes > 0) {
                                    "📦  $name  •  ${formatBytes(info.downloadSizeBytes)}"
                                } else {
                                    "📦  $name"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }

                    if (info.isDownloading) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            val progress = info.downloadProgress
                            if (progress != null) {
                                LinearProgressIndicator(
                                    progress = { (progress / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            val sizeProgress = if (info.totalBytes > 0L) {
                                "${formatBytes(info.downloadedBytes)} / ${formatBytes(info.totalBytes)}"
                            } else {
                                formatBytes(info.downloadedBytes)
                            }
                            Text(
                                text = if (progress != null) {
                                    "Downloading update... $progress% ($sizeProgress)"
                                } else {
                                    "Downloading update... ($sizeProgress)"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        info.downloadError?.let { errorMsg ->
                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { skipThisVersion = !skipThisVersion }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Checkbox(
                                checked = skipThisVersion,
                                onCheckedChange = null,
                            )
                            Text(
                                text = "Don't show again for this version",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (info.isDownloading) {
                            TextButton(onClick = onCancelDownload) { Text("Cancel") }
                            Button(onClick = { onClose(false) }) { Text("Hide") }
                        } else {
                            TextButton(onClick = { onClose(skipThisVersion) }) { Text("Ignore") }
                            Button(onClick = onUpdate) {
                                Text(
                                    text = if (info.downloadedApkPath != null) "Install update" else "Update now",
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full 16:9 uncropped hero banner with a bottom gradient melting into [surfaceColor]
 * so the 3-phone illustration + ClaudeWaves wordmark stay completely visible.
 */
@Composable
internal fun UpdateHeroHeader(
    icon: ImageVector,
    title: String,
    badgeText: String,
    subtitleText: String? = null,
    surfaceColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
    ) {
        Image(
            painter = painterResource(R.drawable.claudewaves_update_banner),
            contentDescription = "ClaudeWaves update preview",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.42f to Color.Transparent,
                        0.76f to surfaceColor.copy(alpha = 0.88f),
                        1.00f to surfaceColor,
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        )
                    }
                    if (!subtitleText.isNullOrBlank()) {
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

private fun formatReleaseDate(isoInstant: String): String? = runCatching {
    Instant.parse(isoInstant)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}.getOrNull()
