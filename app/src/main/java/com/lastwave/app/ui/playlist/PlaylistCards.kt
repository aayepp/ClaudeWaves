package com.lastwave.app.ui.playlist

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lastwave.app.R
import com.lastwave.app.data.playlist.LIKED_SONGS_MODE
import com.lastwave.app.data.playlist.LOCAL_SONGS_MODE
import com.lastwave.app.data.playlist.SavedPlaylist
import com.lastwave.app.data.playlist.SpotifyUserPlaylist
import com.lastwave.app.data.playlist.isYouTubeOnly
import com.lastwave.app.ui.comic.DropdownMenuItem
import com.lastwave.app.ui.comic.Surface
import com.lastwave.app.ui.comic.clipOnly
import com.lastwave.app.ui.common.ArtworkImage
import com.lastwave.app.ui.common.ExpressiveInlineLoadingIndicator
import com.lastwave.app.ui.common.OverflowMenuButton
import com.lastwave.app.ui.common.PlaylistCover
import com.lastwave.app.ui.common.TrackSubtitleWithBadge
import com.lastwave.app.ui.player.PlayingWaveBars
import com.lastwave.app.ui.settings.SettingsDuotoneGlyph
import com.lastwave.app.ui.settings.SettingsDuotoneIcon
import com.lastwave.app.ui.settings.SettingsIconMedallion
import com.lastwave.app.ui.settings.SettingsMedallionColors
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.comicIndicationOrNull
import com.lastwave.app.ui.theme.comicPanel
import com.lastwave.app.ui.theme.glassCardBorder
import com.lastwave.app.ui.theme.glassCardColor

private val VaultTileShape = RoundedCornerShape(16.dp)
private val CoverTileShape = RoundedCornerShape(12.dp)
private val InlineTrackArtShape = RoundedCornerShape(10.dp)

@Composable
internal fun LibraryQuickVaultGrid(
    likedCount: Int,
    downloadedCount: Int,
    localCount: Int,
    onOpenLiked: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenLocal: () -> Unit,
    onOpenGenerator: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val roleColors = SettingsMedallionColors.current()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VaultTile(
                glyph = SettingsDuotoneGlyph.LIKED_HEART,
                accent = roleColors.spotify,
                title = "Liked Songs",
                subtitle = likedCount.toString(),
                onClick = onOpenLiked,
                modifier = Modifier.weight(1f),
            )
            VaultTile(
                glyph = SettingsDuotoneGlyph.DOWNLOAD_TRAY,
                accent = roleColors.audio,
                title = "Downloads",
                subtitle = downloadedCount.toString(),
                onClick = onOpenDownloads,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VaultTile(
                glyph = SettingsDuotoneGlyph.LOCAL_FOLDER_NOTE,
                accent = roleColors.library,
                title = "Local Songs",
                subtitle = localCount.toString(),
                onClick = onOpenLocal,
                modifier = Modifier.weight(1f),
            )
            VaultTile(
                glyph = SettingsDuotoneGlyph.SMART_MIX_SPARK,
                accent = roleColors.appearance,
                title = "Smart Mix",
                subtitle = "Generate",
                onClick = onOpenGenerator,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun VaultTile(
    glyph: SettingsDuotoneGlyph,
    accent: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val isComic = LocalComicTheme.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "vaultPress",
    )
    val surfaceColor = when {
        isComic -> MaterialTheme.colorScheme.surface
        isDark -> glassCardColor(MaterialTheme.colorScheme.surfaceContainerHigh)
        else -> glassCardColor(MaterialTheme.colorScheme.surface)
    }
    val border = if (isComic) {
        null
    } else {
        glassCardBorder() ?: BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.42f else 0.55f),
        )
    }

    Row(
        modifier = modifier
            .heightIn(min = 62.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .comicPanel(VaultTileShape, offset = 3.dp)
            .then(if (border != null) Modifier.border(border, VaultTileShape) else Modifier)
            .clipOnly(VaultTileShape)
            .background(surfaceColor)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onClick()
                },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SettingsIconMedallion(accent = accent, size = 34.dp, cornerRadius = 10.dp) { tint ->
            SettingsDuotoneIcon(glyph = glyph, tint = tint, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun PlaylistCoverGridTile(
    playlist: SavedPlaylist,
    isRegenerating: Boolean,
    isPlaying: Boolean,
    playbackSource: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPlay: () -> Unit,
    onTogglePin: () -> Unit,
    onDownload: () -> Unit,
    onMakeLocal: () -> Unit,
    onRegenerate: () -> Unit,
    onEditCover: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val isThisPlaylistPlaying = isPlaying && playbackSource == playlist.title
    var menuExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "coverTileScale",
    )
    val selectedBorderAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(180),
        label = "coverSelectBorder",
    )
    val trackCount = playlist.remoteTrackCount ?: playlist.tracks.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = comicIndicationOrNull(),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .comicPanel(CoverTileShape, offset = 2.dp)
                .then(
                    if (selectedBorderAlpha > 0f) {
                        Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = selectedBorderAlpha),
                            CoverTileShape,
                        )
                    } else {
                        Modifier
                    },
                )
                .clipOnly(CoverTileShape),
        ) {
            PlaylistCover(
                playlist = playlist,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 12.dp,
            )

            if (playlist.isPinned && !isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(22.dp)
                        .clipOnly(CircleShape)
                        .background(Color.Black.copy(alpha = 0.56f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = stringResource(R.string.pl_pinned_cd),
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }

            if (isThisPlaylistPlaying) {
                PlayingWaveBars(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .size(22.dp, 16.dp),
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
            } else if (!isRegenerating) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clipOnly(CircleShape)
                            .background(Color.Black.copy(alpha = 0.48f))
                            .clickable { menuExpanded = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.pl_options_cd),
                            tint = Color.White,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        if (playlist.tracks.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pl_play_cd)) },
                                leadingIcon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                                onClick = { onPlay(); menuExpanded = false },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(if (playlist.isPinned) stringResource(R.string.pl_unpin) else stringResource(R.string.pl_pin)) },
                            leadingIcon = { Icon(Icons.Filled.PushPin, contentDescription = null) },
                            onClick = { onTogglePin(); menuExpanded = false },
                        )
                        val isLocalSongs = playlist.mode == LOCAL_SONGS_MODE
                        if (!isLocalSongs) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pl_download_all)) },
                                leadingIcon = { Icon(Icons.Filled.Download, contentDescription = null) },
                                onClick = { onDownload(); menuExpanded = false },
                            )
                        }
                        if (playlist.isYouTubeOnly) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pl_make_local)) },
                                leadingIcon = { Icon(Icons.Filled.BookmarkAdd, contentDescription = null) },
                                onClick = { onMakeLocal(); menuExpanded = false },
                            )
                        } else {
                            if (playlist.mode != LIKED_SONGS_MODE && !isLocalSongs) {
                                DropdownMenuItem(
                                    text = { Text(if (isRegenerating) stringResource(R.string.pl_regenerating) else stringResource(R.string.pl_regenerate)) },
                                    leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                                    enabled = !isRegenerating,
                                    onClick = { onRegenerate(); menuExpanded = false },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.pl_change_cover)) },
                                leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                                onClick = { onEditCover(); menuExpanded = false },
                            )
                            if (playlist.mode != LIKED_SONGS_MODE) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.common_rename)) },
                                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                                    onClick = { onRename(); menuExpanded = false },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.common_export)) },
                                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                                onClick = { onExport(); menuExpanded = false },
                            )
                        }
                        if (!isLocalSongs) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { onDelete(); menuExpanded = false },
                            )
                        }
                    }
                }
            }

            if (isRegenerating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.62f)),
                    contentAlignment = Alignment.Center,
                ) {
                    ExpressiveInlineLoadingIndicator(size = 24.dp, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (playlist.isYouTubeOnly) "YT · $trackCount" else "$trackCount tracks",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun SpotifyCoverGridTile(
    playlist: SpotifyUserPlaylist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val isLiked = playlist.id == "liked"
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "spCoverScale",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = comicIndicationOrNull(),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onClick()
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .comicPanel(CoverTileShape, offset = 2.dp)
                .clipOnly(CoverTileShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isLiked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF1DB954), Color(0xFF191414)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = Color.White,
                    )
                }
            } else if (!playlist.artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = playlist.artworkUrl,
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = playlist.trackCount?.let { "Spotify · $it" } ?: "Spotify",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun LibraryInlineTrackRow(
    title: String,
    artist: String,
    artworkUrl: String?,
    trailingBadge: String? = null,
    showDivider: Boolean = true,
    onClick: () -> Unit,
    onMenuClick: (() -> Unit)? = null,
) {
    val isDark = LocalIsDarkTheme.current
    val isComic = LocalComicTheme.current
    val dividerColor = if (isComic) {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.25f else 0.42f)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clipOnly(InlineTrackArtShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                ArtworkImage(
                    name = title,
                    artist = artist,
                    embeddedUrl = artworkUrl,
                    fallbackIcon = Icons.Filled.MusicNote,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                TrackSubtitleWithBadge(
                    subtitle = artist,
                    title = title,
                    artist = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!trailingBadge.isNullOrBlank()) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Text(
                        text = trailingBadge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            if (onMenuClick != null) {
                OverflowMenuButton(onClick = onMenuClick)
            }
        }
        if (showDivider) {
            HorizontalDivider(thickness = if (isComic) 1.dp else 0.5.dp, color = dividerColor)
        }
    }
}
