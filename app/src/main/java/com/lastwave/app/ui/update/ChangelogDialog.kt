package com.lastwave.app.ui.update

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.data.update.AppChangelogManager
import com.lastwave.app.data.update.AppUpdateManager
import com.lastwave.app.data.update.ChangelogEntry
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.LocalLiquidGlass
import com.lastwave.app.ui.theme.comicPanel
import com.lastwave.app.ui.theme.glassCardBorder
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
    val metrics = rememberDialogLayoutMetrics()
    val isComic = LocalComicTheme.current
    val isGlass = LocalLiquidGlass.current && !isComic
    val isDark = LocalIsDarkTheme.current
    val corner = if (isComic) 20.dp else 28.dp
    val cardShape = RoundedCornerShape(corner)
    val dialogSurface = if (isGlass) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (isDark) 0.90f else 0.94f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val cardBorder = when {
        isComic -> null
        isGlass -> glassCardBorder()
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = cardShape,
            color = dialogSurface,
            border = cardBorder,
            modifier = if (metrics.isCompactLandscape) {
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .widthIn(max = 700.dp)
                    .height(metrics.maxDialogHeight.coerceAtMost(400.dp))
                    .comicPanel(cardShape)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .widthIn(max = 520.dp)
                    .heightIn(max = metrics.maxDialogHeight)
                    .comicPanel(cardShape)
            },
        ) {
            if (metrics.isCompactLandscape) {
                Row(modifier = Modifier.fillMaxSize()) {
                    UpdateSplitHeroPane(
                        icon = Icons.Filled.AutoAwesome,
                        title = "What's New",
                        badgeText = "v${entry.version}",
                        subtitleText = entry.date?.let { "•  Updated $it" },
                        surfaceColor = dialogSurface,
                        modifier = Modifier.weight(0.42f).fillMaxHeight(),
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)),
                    )
                    Column(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
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
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    UpdateHeroHeader(
                        icon = Icons.Filled.AutoAwesome,
                        title = "What's New",
                        badgeText = "v${entry.version}",
                        subtitleText = entry.date?.let { "•  Updated $it" },
                        surfaceColor = dialogSurface,
                        maxHeight = 175.dp,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
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
    }
}

@Composable
private fun ChangelogViewerDialog(
    currentVersion: String,
    entries: List<ChangelogEntry>,
    onDismiss: () -> Unit,
) {
    val notesStyle = releaseNotesStyle()
    val metrics = rememberDialogLayoutMetrics()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .widthIn(max = 600.dp)
                .heightIn(max = metrics.maxDialogHeight),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            .weight(1f, fill = false),
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
