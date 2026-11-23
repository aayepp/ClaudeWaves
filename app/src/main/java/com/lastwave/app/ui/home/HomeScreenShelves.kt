package com.lastwave.app.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip as uiClip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lastwave.app.data.repository.HomeAlbum
import com.lastwave.app.data.repository.HomeArtistItem
import com.lastwave.app.data.repository.HomeTrack
import com.lastwave.app.ui.comic.clip
import com.lastwave.app.ui.common.ArtworkImage
import com.lastwave.app.ui.common.LocalAmbientAnimationsActive
import com.lastwave.app.ui.common.TrackSubtitleWithBadge
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.comicPanel

private val TelemetryArtShape = RoundedCornerShape(10.dp)
private val TelemetryNowPlayingShape = RoundedCornerShape(14.dp)

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
internal fun TrackRow(
    track: HomeTrack,
    badge: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenuClick: () -> Unit,
    rank: Int? = null,
    showDivider: Boolean = true,
) {
    val isNowPlaying = track.isNowPlaying
    val isDark = LocalIsDarkTheme.current
    val isComic = LocalComicTheme.current
    val secondaryTextColor = if (isNowPlaying) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val rowBase = if (isNowPlaying) {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .comicPanel(TelemetryNowPlayingShape, offset = 2.dp)
            .uiClip(TelemetryNowPlayingShape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.24f else 0.42f))
            .then(
                if (isComic) Modifier
                else Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), TelemetryNowPlayingShape),
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = rowBase
                .heightIn(min = 60.dp)
                .padding(vertical = 8.dp, horizontal = if (isNowPlaying) 10.dp else 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (rank != null && !isNowPlaying) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(26.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(TelemetryArtShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                ArtworkImage(
                    name = track.name,
                    artist = com.lastwave.app.util.ArtistHelper.primaryArtist(track.artist),
                    embeddedUrl = track.artworkUrl,
                    fallbackIcon = if (isNowPlaying) Icons.Filled.GraphicEq else Icons.Filled.MusicNote,
                    modifier = Modifier.fillMaxSize(),
                )
                if (isNowPlaying) {
                    com.lastwave.app.ui.player.PlayingWaveBars(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp).size(20.dp, 16.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val displayArtist = com.lastwave.app.util.ArtistHelper.primaryArtist(track.artist)
                if (displayArtist.isNotBlank() || track.name.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    TrackSubtitleWithBadge(
                        subtitle = displayArtist,
                        title = track.name,
                        artist = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isNowPlaying) MaterialTheme.colorScheme.onSurfaceVariant else secondaryTextColor,
                    )
                }
            }
            if (isNowPlaying) {
                Spacer(Modifier.width(8.dp))
                val active = LocalAmbientAnimationsActive.current
                val dotAlpha: State<Float> = if (active) {
                    val infiniteTransition = rememberInfiniteTransition(label = "nowPlayingPulse")
                    infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "dotAlpha",
                    )
                } else {
                    remember { mutableFloatStateOf(1f) }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .graphicsLayer { alpha = dotAlpha.value }
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                    Text(
                        text = "LIVE",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else if (badge != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            com.lastwave.app.ui.common.OverflowMenuButton(onClick = onMenuClick)
        }
        if (showDivider && !isNowPlaying) {
            HorizontalDivider(
                thickness = if (isComic) 1.dp else 0.5.dp,
                color = if (isComic) {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.25f else 0.42f)
                },
            )
        }
    }
}

@Composable
internal fun AlbumRow(
    album: HomeAlbum,
    badge: String?,
    onClick: () -> Unit,
    rank: Int? = null,
    showDivider: Boolean = true,
) {
    val isDark = LocalIsDarkTheme.current
    val isComic = LocalComicTheme.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (rank != null) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(26.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(TelemetryArtShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                ArtworkImage(
                    name = album.name,
                    artist = album.artist,
                    embeddedUrl = album.artworkUrl,
                    fallbackIcon = Icons.Filled.Album,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    album.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val displayArtist = com.lastwave.app.util.ArtistHelper.primaryArtist(album.artist)
                if (displayArtist.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        displayArtist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (badge != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = if (isComic) 1.dp else 0.5.dp,
                color = if (isComic) {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.25f else 0.42f)
                },
            )
        }
    }
}

@Composable
internal fun ArtistRow(
    artist: HomeArtistItem,
    badge: String?,
    onClick: () -> Unit,
    rank: Int? = null,
    showDivider: Boolean = true,
) {
    val isDark = LocalIsDarkTheme.current
    val isComic = LocalComicTheme.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (rank != null) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(26.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                ArtworkImage(
                    name = "artist:${artist.name}",
                    artist = artist.name,
                    embeddedUrl = artist.artworkUrl,
                    fallbackIcon = Icons.Filled.People,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                artist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (badge != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = if (isComic) 1.dp else 0.5.dp,
                color = if (isComic) {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.25f else 0.42f)
                },
            )
        }
    }
}
