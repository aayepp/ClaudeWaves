package com.lastwave.app.ui.update

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.R
import com.lastwave.app.data.update.AppChangelogManager
import com.lastwave.app.data.update.AppUpdateManager
import com.lastwave.app.data.update.ChangelogEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChangelogViewModel @Inject constructor(
    val changelogManager: AppChangelogManager,
    val updateManager: AppUpdateManager,
) : ViewModel()

/**
 * Root host for both the full changelog viewer (opened from Settings -> About)
 * and the one-time post-update "What's New" popup. Delays the automatic popup
 * until splash dismissal and skips showing while an online update dialog is up.
 */
@Composable
fun ChangelogHost(
    splashDismissed: Boolean,
    viewModel: ChangelogViewModel = hiltViewModel(),
) {
    val state by viewModel.changelogManager.state.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateManager.updateInfo.collectAsStateWithLifecycle()
    val updateDialogVisible = updateInfo.showDialog && updateInfo.isUpdateAvailable

    if (state.showFullChangelog) {
        ChangelogViewerDialog(
            currentVersion = state.currentVersion,
            entries = state.entries,
            onDismiss = viewModel.changelogManager::closeChangelogViewer,
        )
    } else if (splashDismissed && !updateDialogVisible && state.showWhatsNewPopup && state.whatsNewEntry != null) {
        WhatsNewDialog(
            entry = state.whatsNewEntry!!,
            onDismiss = viewModel.changelogManager::dismissWhatsNew,
            onViewAll = viewModel.changelogManager::openFullChangelogFromWhatsNew,
        )
    }
}

@Composable
private fun WhatsNewDialog(
    entry: ChangelogEntry,
    onDismiss: () -> Unit,
    onViewAll: () -> Unit,
) {
    val notesStyle = releaseNotesStyle()
    val notes = remember(entry.bodyMarkdown, notesStyle) {
        releaseNotesText(entry.bodyMarkdown, notesStyle)
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 560.dp),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("What's New in v${entry.version}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        entry.date?.let { date ->
                            Text(
                                text = "Updated $date",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                ) {
                    Image(
                        painter = painterResource(R.drawable.claudewaves_update_banner),
                        contentDescription = "ClaudeWaves update preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Column(
                    Modifier
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onViewAll) { Text("All Versions") }
                    Button(onClick = onDismiss) { Text("Got it") }
                }
            }
        }
    }
}

@Composable
private fun ChangelogViewerDialog(
    currentVersion: String,
    entries: List<ChangelogEntry>,
    onDismiss: () -> Unit,
) {
    val notesStyle = releaseNotesStyle()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 560.dp),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Changelog", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Installed: v$currentVersion",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (entries.isEmpty()) {
                    Text(
                        text = "No release notes available for this build.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(items = entries, key = { it.version }) { entry ->
                            val notes = remember(entry.version, entry.bodyMarkdown, notesStyle) {
                                releaseNotesText(entry.bodyMarkdown, notesStyle)
                            }
                            val isCurrent = entry.version.removePrefix("v")
                                .equals(currentVersion.removePrefix("v"), ignoreCase = true)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = if (isCurrent) "v${entry.version} (Current)" else "v${entry.version}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                    entry.date?.let { date ->
                                        Text(
                                            text = date,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Text(
                                    text = notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(4.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}
