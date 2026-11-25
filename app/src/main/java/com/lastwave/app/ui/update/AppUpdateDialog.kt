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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.LocalLiquidGlass
import com.lastwave.app.ui.theme.comicPanel
import com.lastwave.app.ui.theme.glassCardBorder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

@Immutable
internal data class DialogLayoutMetrics(
    val isCompactLandscape: Boolean,
    val maxDialogHeight: Dp,
)

@Composable
internal fun rememberDialogLayoutMetrics(): DialogLayoutMetrics {
    val config = LocalConfiguration.current
    val isCompactLandscape = config.screenHeightDp < 520 ||
        (config.screenWidthDp > config.screenHeightDp && config.screenHeightDp < 640)
    val maxDialogHeight = (config.screenHeightDp * 0.88f).dp.coerceAtLeast(260.dp)
    return DialogLayoutMetrics(
        isCompactLandscape = isCompactLandscape,
        maxDialogHeight = maxDialogHeight,
    )
}

@HiltViewModel
class AppUpdateDialogViewModel @Inject constructor(
    val manager: AppUpdateManager,
) : ViewModel() {
    val info = manager.updateInfo
}

/**
 * Adaptive in-app update dialog: switches between a compact vertical layout in portrait
 * and a 2-column split panel in Android Player / landscape so CTA buttons stay pinned.
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

    val versionBadge = if (info.currentVersion.isNotBlank()) {
        "v${info.currentVersion}  →  v${info.latestVersion}"
    } else {
        "v${info.latestVersion}"
    }
    val dateLabel = formatReleaseDate(info.publishedAt)?.let { "•  $it" }
    val packageLabel = info.downloadFileName?.let { name ->
        if (info.downloadSizeBytes > 0) "$name  •  ${formatBytes(info.downloadSizeBytes)}" else name
    }
    val notesStyle = releaseNotesStyle()
    val notes = remember(info.releaseNotes, notesStyle) {
        releaseNotesText(
            info.releaseNotes.ifBlank { "No release notes were published for this version." },
            notesStyle,
        )
    }

    Dialog(
        onDismissRequest = { onClose(skipThisVersion) },
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
                    .widthIn(max = 720.dp)
                    .height(metrics.maxDialogHeight.coerceAtMost(420.dp))
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
                        icon = Icons.Filled.SystemUpdate,
                        title = "Update Arrived",
                        badgeText = versionBadge,
                        subtitleText = dateLabel,
                        footerChipText = packageLabel,
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
                        if (info.releaseTitle.isNotBlank() && !info.releaseTitle.contains(info.latestVersion)) {
                            Text(
                                text = info.releaseTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        ReleaseNotesCard(
                            notes = notes,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                        UpdateStatusSection(info = info)
                        UpdateActionFooter(
                            info = info,
                            skipThisVersion = skipThisVersion,
                            onToggleSkip = { skipThisVersion = !skipThisVersion },
                            onUpdate = onUpdate,
                            onCancelDownload = onCancelDownload,
                            onClose = onClose,
                            inlineSkipCheckbox = true,
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    UpdateHeroHeader(
                        icon = Icons.Filled.SystemUpdate,
                        title = "Update Arrived",
                        badgeText = versionBadge,
                        subtitleText = dateLabel,
                        surfaceColor = dialogSurface,
                        maxHeight = 175.dp,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(start = 22.dp, end = 22.dp, top = 4.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (info.releaseTitle.isNotBlank() && !info.releaseTitle.contains(info.latestVersion)) {
                            Text(
                                text = info.releaseTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        ReleaseNotesCard(
                            notes = notes,
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        )
                        packageLabel?.let { label ->
                            PackageMetaChip(label = label)
                        }
                        UpdateStatusSection(info = info)
                        UpdateActionFooter(
                            info = info,
                            skipThisVersion = skipThisVersion,
                            onToggleSkip = { skipThisVersion = !skipThisVersion },
                            onUpdate = onUpdate,
                            onCancelDownload = onCancelDownload,
                            onClose = onClose,
                            inlineSkipCheckbox = false,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseNotesCard(
    notes: AnnotatedString,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
}

@Composable
private fun PackageMetaChip(label: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun UpdateStatusSection(info: UpdateInfo) {
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun UpdateActionFooter(
    info: UpdateInfo,
    skipThisVersion: Boolean,
    onToggleSkip: () -> Unit,
    onUpdate: () -> Unit,
    onCancelDownload: () -> Unit,
    onClose: (skipThisVersion: Boolean) -> Unit,
    inlineSkipCheckbox: Boolean,
) {
    if (!info.isDownloading && !inlineSkipCheckbox) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onToggleSkip)
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Checkbox(checked = skipThisVersion, onCheckedChange = null)
            Text(
                text = "Don't show again for this version",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!info.isDownloading && inlineSkipCheckbox) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleSkip)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Checkbox(checked = skipThisVersion, onCheckedChange = null)
                Text(
                    text = "Don't show again",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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

/**
 * Split-pane left hero card used in landscape / Android Player mode so the banner art,
 * version pill, and APK info stay visible beside the scrollable notes and sticky footer.
 */
@Composable
internal fun UpdateSplitHeroPane(
    icon: ImageVector,
    title: String,
    badgeText: String,
    subtitleText: String? = null,
    footerChipText: String? = null,
    surfaceColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.claudewaves_update_banner),
            contentDescription = "ClaudeWaves update preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to surfaceColor.copy(alpha = 0.08f),
                        0.45f to surfaceColor.copy(alpha = 0.56f),
                        0.82f to surfaceColor.copy(alpha = 0.94f),
                        1.00f to surfaceColor,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                if (!subtitleText.isNullOrBlank()) {
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!footerChipText.isNullOrBlank()) {
                PackageMetaChip(label = footerChipText)
            }
        }
    }
}

/**
 * Bounded 16:9 hero banner with a bottom gradient melting into [surfaceColor].
 */
@Composable
internal fun UpdateHeroHeader(
    icon: ImageVector,
    title: String,
    badgeText: String,
    subtitleText: String? = null,
    surfaceColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    maxHeight: Dp = 175.dp,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .heightIn(max = maxHeight),
    ) {
        Image(
            painter = painterResource(R.drawable.claudewaves_update_banner),
            contentDescription = "ClaudeWaves update preview",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.00f to Color.Transparent,
                        0.40f to Color.Transparent,
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
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp),
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
