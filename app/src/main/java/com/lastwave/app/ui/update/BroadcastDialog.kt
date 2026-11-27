package com.lastwave.app.ui.update

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lastwave.app.R
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
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.LocalLiquidGlass
import com.lastwave.app.ui.theme.comicPanel
import com.lastwave.app.ui.theme.glassCardBorder
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
internal fun BroadcastDialog(
    message: BroadcastMessage,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val metrics = rememberDialogLayoutMetrics()
    val isComic = LocalComicTheme.current
    val isGlass = LocalLiquidGlass.current && !isComic
    val isDark = LocalIsDarkTheme.current
    val corner = if (isComic) 20.dp else 28.dp
    val cardShape = RoundedCornerShape(corner)
    val bannerClipShape = RoundedCornerShape(topStart = corner, topEnd = corner)
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

    val accent = message.type.accent()
    val icon = message.type.icon()
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
                    Box(modifier = Modifier.weight(0.42f).fillMaxHeight()) {
                        BroadcastBannerArt(
                            message = message,
                            dialogSurface = dialogSurface,
                            isComic = isComic,
                            clipShape = RoundedCornerShape(topStart = corner, bottomStart = corner),
                            modifier = Modifier.fillMaxSize(),
                        )
                        BroadcastTitleHeader(
                            message = message,
                            icon = icon,
                            iconContainer = iconContainer,
                            iconTint = iconTint,
                            isComic = isComic,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        )
                    }
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
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BroadcastBodyScroll(
                            message = message,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                        BroadcastActionFooter(
                            message = message,
                            isComic = isComic,
                            onDismiss = onDismiss,
                            onOpenAction = { url ->
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                                onDismiss()
                            },
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    BroadcastBannerArt(
                        message = message,
                        dialogSurface = dialogSurface,
                        isComic = isComic,
                        clipShape = bannerClipShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .heightIn(max = 175.dp),
                    )
                    if (isComic) {
                        HorizontalDivider(
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(
                                start = 20.dp,
                                end = 20.dp,
                                top = if (isComic) 16.dp else 8.dp,
                                bottom = 18.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BroadcastTitleHeader(
                            message = message,
                            icon = icon,
                            iconContainer = iconContainer,
                            iconTint = iconTint,
                            isComic = isComic,
                        )
                        BroadcastBodyScroll(
                            message = message,
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        )
                        BroadcastActionFooter(
                            message = message,
                            isComic = isComic,
                            onDismiss = onDismiss,
                            onOpenAction = { url ->
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BroadcastBannerArt(
    message: BroadcastMessage,
    dialogSurface: Color,
    isComic: Boolean,
    clipShape: Shape,
    modifier: Modifier = Modifier,
) {
    val defaultBanner = painterResource(R.drawable.claudewaves_update_banner)
    Box(modifier = modifier.clip(clipShape)) {
        val customBannerUrl = message.imageUrl
        if (!customBannerUrl.isNullOrBlank()) {
            AsyncImage(
                model = customBannerUrl,
                contentDescription = message.title,
                contentScale = ContentScale.Crop,
                placeholder = defaultBanner,
                error = defaultBanner,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(
                painter = defaultBanner,
                contentDescription = "ClaudeWaves",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (!isComic) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.00f to Color.Transparent,
                            0.44f to Color.Transparent,
                            0.80f to dialogSurface.copy(alpha = 0.88f),
                            1.00f to dialogSurface,
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun BroadcastTitleHeader(
    message: BroadcastMessage,
    icon: ImageVector,
    iconContainer: Color,
    iconTint: Color,
    isComic: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = iconContainer,
            border = if (isComic) BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else null,
            modifier = Modifier.size(40.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = message.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val dateText = formatBroadcastDate(message.publishedAt).takeIf { it.isNotBlank() }
            val subtitle = listOfNotNull(dateText, message.type.label).joinToString(" • ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BroadcastBodyScroll(
    message: BroadcastMessage,
    modifier: Modifier = Modifier,
) {
    if (message.message.isBlank()) return
    val notesStyle = releaseNotesStyle()
    val renderedBody = remember(message.message, notesStyle) {
        releaseNotesText(message.message, notesStyle)
    }
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = renderedBody,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f),
        )
    }
}

@Composable
private fun BroadcastActionFooter(
    message: BroadcastMessage,
    isComic: Boolean,
    onDismiss: () -> Unit,
    onOpenAction: (String) -> Unit,
) {
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
                Text("Close")
            }
            Button(
                onClick = { onOpenAction(actionUrl) },
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
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Floating top-of-Home marquee broadcast banner (Design 3A): glides smoothly when ambient
 * animations are active, opens the full [BroadcastDialog] on tap, and dismisses cleanly on `X`.
 */
@Composable
fun HomeBroadcastTickerHost(
    modifier: Modifier = Modifier,
    viewModel: BroadcastDialogViewModel = hiltViewModel(),
) {
    val uiState by viewModel.manager.state.collectAsStateWithLifecycle()
    val active = uiState.active
    AnimatedVisibility(
        visible = active != null && uiState.showTicker,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        if (active != null) {
            HomeBroadcastTickerPill(
                message = active,
                onClick = { viewModel.manager.openDialogFromTicker() },
                onDismiss = { viewModel.manager.dismissTicker(active.id) },
            )
        }
    }
}

@Composable
internal fun HomeBroadcastTickerPill(
    message: BroadcastMessage,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isComic = LocalComicTheme.current
    val isGlass = LocalLiquidGlass.current && !isComic
    val isDark = LocalIsDarkTheme.current
    val accent = message.type.accent()
    val ambientActive = com.lastwave.app.ui.common.LocalAmbientAnimationsActive.current
    val containerColor = when {
        isComic -> MaterialTheme.colorScheme.surfaceContainerHigh
        isGlass -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (isDark) 0.84f else 0.90f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val border = when {
        isComic -> null
        isGlass -> glassCardBorder() ?: BorderStroke(1.dp, accent.copy(alpha = 0.45f))
        else -> BorderStroke(1.dp, accent.copy(alpha = 0.42f))
    }
    val tickerText = remember(message.title, message.message) {
        val plainBody = message.message
            .lineSequence()
            .map { it.trim().trimStart('#', '*', '-', '>', ' ').trim() }
            .filter { it.isNotEmpty() }
            .joinToString("  •  ")
        if (plainBody.isNotEmpty()) "${message.title}  —  $plainBody" else message.title
    }

    Surface(
        shape = CircleShape,
        color = containerColor,
        border = border,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(CircleShape)
            .comicPanel(CircleShape)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 7.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = if (isComic) ComicAccent else accent.copy(alpha = 0.18f),
                border = if (isComic) BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline) else null,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = message.type.icon(),
                        contentDescription = message.type.label,
                        tint = if (isComic) ComicInk else accent,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = message.type.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isComic) ComicInk else accent,
                        maxLines = 1,
                    )
                }
            }

            Text(
                text = tickerText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (ambientActive) {
                            Modifier.basicMarquee(iterations = Int.MAX_VALUE, velocity = 42.dp)
                        } else {
                            Modifier
                        },
                    ),
            )

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

/** One icon per broadcast type, shared by the user dialog and the Developer console. */
internal fun BroadcastType.icon(): ImageVector = when (this) {
    BroadcastType.INFO -> Icons.Filled.Campaign
    BroadcastType.WARNING -> Icons.Filled.Warning
    BroadcastType.MAINTENANCE -> Icons.Filled.Build
    BroadcastType.UPDATE -> Icons.Filled.NewReleases
}

@Composable
internal fun BroadcastType.accent(): Color = when (this) {
    BroadcastType.INFO -> MaterialTheme.colorScheme.primary
    BroadcastType.WARNING -> MaterialTheme.colorScheme.error
    BroadcastType.MAINTENANCE -> MaterialTheme.colorScheme.tertiary
    BroadcastType.UPDATE -> MaterialTheme.colorScheme.secondary
}
