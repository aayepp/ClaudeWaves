@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.lastwave.app.ui.player

import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.drawscope.scale
import com.lastwave.app.playback.AudioQualityTier
import com.lastwave.app.ui.theme.comicIndicationOrNull
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.comicPanel
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
import com.lastwave.app.data.playlist.isLikedIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.combinedClickable
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lastwave.app.ui.common.PredictiveBackScreen
import com.lastwave.app.playback.haptics.MusicHapticsConfig
import com.lastwave.app.playback.haptics.MusicHapticsEngineMode
import com.lastwave.app.playback.haptics.MusicHapticsIntensity
import com.lastwave.app.playback.haptics.MusicHapticsMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.lerp
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import com.lastwave.app.ui.common.playingMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import com.lastwave.app.ui.common.adaptiveContentWidth
import com.lastwave.app.ui.common.isTabletOrWideScreen
import com.lastwave.app.ui.theme.LocalMd3NavBar
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.FavoriteBorder
import com.lastwave.app.ui.comic.AlertDialog
import com.lastwave.app.ui.comic.Button
import androidx.compose.material3.ButtonDefaults
import com.lastwave.app.ui.comic.Switch
import androidx.compose.material3.SwitchDefaults
import com.lastwave.app.ui.comic.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import com.lastwave.app.ui.comic.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import com.lastwave.app.ui.comic.IconButton
import androidx.compose.material3.MaterialTheme
import com.lastwave.app.ui.comic.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import com.lastwave.app.ui.comic.Surface
import androidx.compose.material3.Text
import com.lastwave.app.ui.comic.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import com.lastwave.app.ui.comic.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.zIndex
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import androidx.media3.common.Player
import com.lastwave.app.data.generate.GeneratedTrack
import com.lastwave.app.data.lyrics.LyricsRepository
import com.lastwave.app.data.lyrics.LyricsResult
import com.lastwave.app.data.lyrics.LyricsSyncType
import kotlinx.coroutines.flow.update
import com.lastwave.app.data.playlist.PlaylistRepository
import com.lastwave.app.data.playlist.SavedPlaylist
import com.lastwave.app.data.playlist.LIKED_SONGS_MODE
import com.lastwave.app.playback.MusicPlayer
import com.lastwave.app.playback.MusicPlayerState
import com.lastwave.app.playback.PlaybackChromeState
import com.lastwave.app.playback.formatSampleRateKHz
import com.lastwave.app.playback.withoutProgress
import com.lastwave.app.playback.isSpatialAudioCodec
import com.lastwave.app.playback.qualityBadgeLabel
import com.lastwave.app.playback.SignalPathReport
import com.lastwave.app.playback.PlaybackProgressState
import androidx.hilt.navigation.compose.hiltViewModel
import com.lastwave.app.data.artwork.ArtworkNormalizer
import com.lastwave.app.playback.PlayableTrack
import com.lastwave.app.playback.toGeneratedTrack
import com.lastwave.app.playback.resolve.MetadataLog
import com.lastwave.app.ui.common.ArtworkImage
import com.lastwave.app.ui.common.ArtworkViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import androidx.compose.material.icons.filled.Timer
import com.lastwave.app.ui.common.DirectionalTrackContent
import com.lastwave.app.ui.common.ExpressiveInlineLoadingIndicator
import com.lastwave.app.ui.common.ExpressiveMotion
import com.lastwave.app.ui.common.PlaylistCover
import com.lastwave.app.ui.common.rememberTrackStepDirection
import com.lastwave.app.ui.common.TrackContextMenuSheet
import com.lastwave.app.ui.common.TrackMenuCapabilities
import com.lastwave.app.ui.common.TrackMenuTarget
import com.lastwave.app.ui.common.GroupGap
import com.lastwave.app.ui.common.TopCurvedSectionClipShape
import com.lastwave.app.ui.common.groupPositionFor
import com.lastwave.app.ui.common.groupShape
import com.lastwave.app.ui.theme.LocalLiquidGlass
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.LiquidGlassSurface
import com.lastwave.app.ui.theme.liquidGlass
import com.lastwave.app.ui.theme.liquidGlassChrome
import com.lastwave.app.ui.theme.liquidGlassContainerColor
import com.lastwave.app.ui.theme.liquidGlassSource
import com.lastwave.app.ui.theme.waterDropJiggle
import com.lastwave.app.ui.theme.isLiquidGlassBackdropSupported
import com.lastwave.app.ui.theme.LocalLiquidGlassBackdrop
import com.lastwave.app.ui.theme.LocalLiquidGlassOverlayBackdrop
import com.lastwave.app.ui.theme.LiquidGlassPreset
import com.lastwave.app.ui.theme.Backdrop
import com.lastwave.app.ui.theme.BackdropBoundsHolder
import com.lastwave.app.ui.theme.ComicInk
import com.lastwave.app.ui.theme.ComicPink
import com.lastwave.app.ui.theme.buildMiniPlayerPillCrownPath
import com.lastwave.app.ui.theme.miniPlayerLiquidGlass
import com.lastwave.app.ui.theme.rememberBackdropUseLightInk
import com.lastwave.app.ui.theme.rememberGlassInteraction
import com.lastwave.app.ui.theme.rememberLayerBackdrop
import com.lastwave.app.ui.theme.trackBackdropBounds
import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.scale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.IntBuffer
import kotlin.time.Duration.Companion.seconds
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt


@Composable
private fun MiniPlayerQualityMicroTag(
    qualityTier: AudioQualityTier,
    isDark: Boolean,
    bursting: Boolean = false,
) {
    val scale by animateFloatAsState(
        targetValue = if (bursting) 1.35f else 1f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 500f),
        label = "qualityTagBurst",
    )
    val isHiRes = qualityTier == AudioQualityTier.HI_RES
    val label = if (isHiRes) {
        stringResource(com.lastwave.app.R.string.lossless_badge_hires)
    } else {
        stringResource(com.lastwave.app.R.string.lossless_badge_cd)
    }
    val icon = if (isHiRes) QualityGlyphs.HiResLossless else QualityGlyphs.Lossless
    val contentColor = when {
        isHiRes && isDark -> Color(0xFFFFD60A)
        isHiRes -> Color(0xFF946800)
        isDark -> Color(0xFF5AE8DC)
        else -> Color(0xFF006D65)
    }
    val containerColor = when {
        isHiRes -> Color(0xFFFFD60A).copy(alpha = if (isDark) 0.16f else 0.24f)
        isDark -> Color(0xFF5AE8DC).copy(alpha = 0.14f)
        else -> Color(0xFF00897B).copy(alpha = 0.13f)
    }
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(4.dp),
        color = containerColor,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(10.dp),
            )
            Text(
                text = label,
                color = contentColor,
                fontSize = 8.5.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MiniPlayerArtistRow(
    artist: String,
    trackKey: String?,
    qualityTier: AudioQualityTier,
    color: Color,
    style: TextStyle,
    marqueeActive: Boolean,
    isDark: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = artist,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .playingMarquee(marqueeActive),
        )
        if (qualityTier != AudioQualityTier.STANDARD) {
            Spacer(Modifier.width(6.dp))
            MiniPlayerQualityMicroTag(
                qualityTier = qualityTier,
                isDark = isDark,
                bursting = rememberTierUpgradeBurst(trackKey, qualityTier) != null,
            )
        }
    }
}

/**
 * Play / pause on a card that flips over: each change turns it half a turn about its
 * vertical axis, always the same way, showing the new symbol on the far side. Past 90°
 * the far face is drawn mirrored back so it never reads backwards.
 */
@Composable
internal fun AnimatedPlayPauseIcon(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val startedPlaying = remember { isPlaying }
    var flips by remember { mutableIntStateOf(0) }
    var shown by remember { mutableStateOf(isPlaying) }
    LaunchedEffect(isPlaying) {
        if (shown != isPlaying) {
            shown = isPlaying
            flips++
        }
    }
    val angle by animateFloatAsState(flips * 180f, spring(dampingRatio = 0.7f, stiffness = 260f), label = "playPauseFlip")
    val color = LocalContentColor.current
    val description = stringResource(com.lastwave.app.R.string.player_play_pause_cd)
    Canvas(
        modifier
            .fillMaxSize()
            .semantics { contentDescription = description }
            .graphicsLayer {
                rotationY = angle
                cameraDistance = 12f * density
            },
    ) {
        // Which face is toward the viewer, and whether it is the mirrored back.
        val half = kotlin.math.floor((angle + 90f) / 180f).toInt()
        val playingFace = if (half % 2 == 0) startedPlaying else !startedPlaying
        val unit = size.minDimension / 24f
        val origin = Offset((size.width - unit * 24f) / 2f, (size.height - unit * 24f) / 2f)
        scale(scaleX = if (half % 2 == 0) 1f else -1f, scaleY = 1f) {
            if (playingFace) {
                drawRect(color, origin + Offset(6 * unit, 5 * unit), Size(4 * unit, 14 * unit))
                drawRect(color, origin + Offset(14 * unit, 5 * unit), Size(4 * unit, 14 * unit))
            } else {
                drawPath(
                    Path().apply {
                        moveTo(origin.x + 8 * unit, origin.y + 5 * unit)
                        lineTo(origin.x + 19 * unit, origin.y + 12 * unit)
                        lineTo(origin.x + 8 * unit, origin.y + 19 * unit)
                        close()
                    },
                    color,
                )
            }
        }
    }
}

@Composable
internal fun MiniPlayer(
    state: PlaybackChromeState,
    progressState: StateFlow<PlaybackProgressState>,
    onExpand: () -> Unit,
    onToggle: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    isLiked: Boolean = false,
    onToggleLiked: () -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp,
    backdrop: Backdrop? = LocalLiquidGlassBackdrop.current,
    /** Root bounds of the visible card (after padding), for the open/close morph. */
    onCardBounds: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null,
    pull: MiniPlayerPull? = null,
    /** 0 = full card, 1 = slim card docked beside the collapsed nav pill. Read in layout/draw only. */
    compactProgress: () -> Float = { 0f },
    adaptiveInk: Boolean = true,
    defaultUseLightInk: Boolean = LocalIsDarkTheme.current,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val track = state.current ?: return
    val windowActive = rememberPlayerWindowActive()
    // Haptics for every mini-player control and swipe gesture.
    val miniHaptic = LocalHapticFeedback.current
    @Suppress("NAME_SHADOWING")
    val onToggle: () -> Unit = { miniHaptic.performHapticFeedback(HapticFeedbackType.ContextClick); onToggle() }
    // The next icon nudges right as it skips, like Now Playing's.
    val nextNudge = remember { Animatable(0f) }
    val nudgeScope = rememberCoroutineScope()
    @Suppress("NAME_SHADOWING")
    val onNext: () -> Unit = {
        miniHaptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        onNext()
        nudgeScope.launch { nextNudge.kick() }
    }
    @Suppress("NAME_SHADOWING")
    val onPrevious: () -> Unit = { miniHaptic.performHapticFeedback(HapticFeedbackType.ContextClick); onPrevious() }
    @Suppress("NAME_SHADOWING")
    val onExpand: () -> Unit = { miniHaptic.performHapticFeedback(HapticFeedbackType.ContextClick); onExpand() }
    @Suppress("NAME_SHADOWING")
    val onClose: () -> Unit = { miniHaptic.performHapticFeedback(HapticFeedbackType.GestureEnd); onClose() }
    // MiniPlayer: single glass card sampling sibling feed content.
    // Luminance loop (5x5 avg, 0.3..0.8, tween 500, 1s).
    val liquidGlass = LocalLiquidGlass.current
    val isGlass = liquidGlass && isLiquidGlassBackdropSupported() && backdrop != null
    var artworkTintRaw by remember(track.videoId, track.artworkUrl) { mutableStateOf<Color?>(null) }
    LaunchedEffect(isGlass, track.videoId, track.artworkUrl) {
        if (!isGlass) {
            artworkTintRaw = null
            return@LaunchedEffect
        }
        val url = ArtworkNormalizer.upscaleYoutubeArtwork(track.artworkUrl)?.takeIf { it.isNotBlank() }
            ?: return@LaunchedEffect
        val cached = ambientPaletteCache.get(url)
        if (cached != null && cached.first != Color.Transparent) {
            artworkTintRaw = cached.first
            return@LaunchedEffect
        }
        withContext(Dispatchers.Default) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .size(96)
                    .build()
                val bitmap = ((context.imageLoader.execute(request) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
                    ?: return@runCatching
                val palette = Palette.from(bitmap).clearFilters().generate()
                val primary = (palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch)?.let { Color(it.rgb) }
                if (primary != null) {
                    artworkTintRaw = primary
                    val secondary = (palette.lightVibrantSwatch ?: palette.darkVibrantSwatch ?: palette.mutedSwatch)?.let { Color(it.rgb) }
                    val tertiary = (palette.darkVibrantSwatch ?: palette.darkMutedSwatch)?.let { Color(it.rgb) }
                    ambientPaletteCache.put(
                        url,
                        Triple(primary, secondary ?: Color.Transparent, tertiary ?: Color.Transparent),
                    )
                }
            }
        }
    }
    val miniGlassTint by animateColorAsState(
        targetValue = artworkTintRaw ?: Color.Transparent,
        animationSpec = tween(550),
        label = "miniGlassTint",
    )
    val resolvedMiniTint = if (artworkTintRaw != null) miniGlassTint else Color.Unspecified
    val artFlight = LocalArtworkFlight.current
    val luminance = 0.5f // Fixed luminance to avoid heavy GPU readback which causes stuttering
    val miniBackdropBounds = remember { BackdropBoundsHolder() }
    val miniUseLightInk = rememberBackdropUseLightInk(
        backdrop = backdrop,
        bounds = miniBackdropBounds,
        enabled = isGlass && adaptiveInk,
        scrimBaseAlpha = 0.28f,
        tintColor = resolvedMiniTint,
        // Skip the opaque cover art on the left so sampling focuses on the text & control area.
        cropStartX = 0.20f,
        cropEndX = 0.96f,
        defaultUseLightInk = defaultUseLightInk,
    )
    val miniGlassInk by animateColorAsState(
        targetValue = if (miniUseLightInk) Color.White else Color.Black,
        animationSpec = tween(com.lastwave.app.ui.theme.ADAPTIVE_INK_FADE_MS),
        label = "miniGlassInk",
    )
    val barInteraction = remember { MutableInteractionSource() }
    val miniTrackKey = remember(track.videoId, track.title, track.artist) {
        track.videoId?.takeIf(String::isNotBlank) ?: "${track.title}|${track.artist}"
    }
    val stepDirection = rememberTrackStepDirection(
        trackKey = miniTrackKey,
        queueIndex = state.currentIndex,
        queueSize = state.queueSize,
    )
    // Drag offsets live in Animatables read only in the graphicsLayer below, so a drag
    // moves the card without recomposing the whole mini player on every pointer event.
    val dragScope = rememberCoroutineScope()
    val dragX = remember(track.videoId, track.title) { Animatable(0f) }
    val dragY = remember(track.videoId, track.title) { Animatable(0f) }
    val swipeWobble = remember { Animatable(1f) }
    var swipeTiltDir by remember { mutableFloatStateOf(0f) }
    fun settleDrag() {
        val releaseDragX = dragX.value
        dragScope.launch { dragX.animateTo(0f, ExpressiveMotion.spatialDefault()) }
        dragScope.launch { dragY.animateTo(0f, ExpressiveMotion.spatialDefault()) }
        if (abs(releaseDragX) > 12f) {
            swipeTiltDir = if (releaseDragX < 0f) -2.2f else 2.2f
            dragScope.launch {
                swipeWobble.snapTo(0f)
                swipeWobble.animateTo(1f, tween(durationMillis = 560, easing = LinearEasing))
            }
        }
    }
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    // An upward drag hands the gesture to [pull] (the sheet follows the finger). Positions are
    // taken in root coordinates: the card itself rides up with the sheet, so local deltas would
    // shrink to nothing under the finger.
    val pullGesture = remember { MiniPullGesture() }
    // One floating pill on every screen and theme. The glass variant used to go
    // full-bleed on screens without a nav bar (song lists, album, playlist), which
    // made it wider than everywhere else.
    val shape = RoundedCornerShape(100)
    val dropController = LocalMiniPlayerDropController.current
    val isComic = LocalComicTheme.current
    val positionedModifier = modifier
        .windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
        )
        .padding(horizontal = 16.dp)
        .padding(bottom = bottomPadding)
        .widthIn(max = 680.dp)
        .fillMaxWidth()
    Box(
        modifier = positionedModifier
            .then(
                if (onCardBounds != null) {
                    Modifier.onGloballyPositioned { onCardBounds(it.boundsInRoot()) }
                } else {
                    Modifier
                },
            )
            .graphicsLayer {
                val sw = swipeWobble.value
                val swipeBounce = if (sw in 0.001f..0.999f) sin(sw * 4.0f * PI.toFloat()) * exp(-2.8f * sw) * 0.72f else 0f
                val swipeTilt = if (sw in 0.001f..0.999f) swipeTiltDir * sin(sw * 3.0f * PI.toFloat()) * exp(-3.0f * sw) else 0f
                val bounce = (dropController?.miniBounce ?: 0f) + swipeBounce
                val wobbleTilt = (dropController?.miniWobbleTilt ?: 0f) + swipeTilt
                val impactX = if (abs(dropController?.miniBounce ?: 0f) > 0.001f) {
                    dropController?.impactXFraction ?: 0.5f
                } else if (swipeTiltDir < 0f) {
                    0.25f
                } else {
                    0.75f
                }
                translationX = dragX.value
                translationY = dragY.value.coerceAtLeast(0f) + (8.dp.toPx() * bounce)
                alpha = (1f - (abs(dragX.value) + dragY.value.coerceAtLeast(0f)) / (threshold * 4f)).coerceIn(0.55f, 1f)
                if (abs(bounce) > 0.001f || abs(wobbleTilt) > 0.001f) {
                    transformOrigin = TransformOrigin(
                        pivotFractionX = impactX.coerceIn(0.15f, 0.85f),
                        pivotFractionY = 0.65f,
                    )
                    scaleX = 1f + 0.068f * bounce
                    scaleY = 1f - 0.115f * bounce
                    rotationZ = wobbleTilt
                } else {
                    transformOrigin = TransformOrigin.Center
                    scaleX = 1f
                    scaleY = 1f
                    rotationZ = 0f
                }
            }
            .onGloballyPositioned { pullGesture.coords = it }
            .pointerInput(track.videoId, track.title, pull) {
                detectDragGestures(
                    onDragStart = { pullGesture.reset() },
                    onDragCancel = {
                        if (pullGesture.active) pull?.release(0f)
                        pullGesture.reset()
                        settleDrag()
                    },
                    onDragEnd = {
                        if (pullGesture.active) {
                            pull?.release(pullGesture.velocity.calculateVelocity().y)
                        } else {
                            when {
                                dragY.value > threshold -> onClose()
                                dragY.value < -threshold -> onExpand()
                                dragX.value < -threshold -> onNext()
                                dragX.value > threshold -> onPrevious()
                            }
                        }
                        pullGesture.reset()
                        settleDrag()
                    },
                ) { change, amount ->
                    change.consume()
                    if (pull != null && pullGesture.claims(amount, dragX.value, dragY.value)) {
                        pullGesture.track(change)?.let(pull::drag)
                        return@detectDragGestures
                    }
                    val x = dragX.value + amount.x
                    val y = dragY.value + amount.y
                    if (abs(x) > abs(y)) dragScope.launch(start = CoroutineStart.UNDISPATCHED) { dragX.snapTo(x) }
                    else dragScope.launch(start = CoroutineStart.UNDISPATCHED) { dragY.snapTo(y) }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val m3SurfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh
        val isDark = LocalIsDarkTheme.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .waterDropJiggle(amplitude = 0.036f, tiltDegrees = 1.8f, dipDp = 3.dp),
        ) {
        if (isGlass) {
            val glassInteraction = rememberGlassInteraction()
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .miniPlayerLiquidGlass(
                        backdrop = backdrop,
                        luminanceAnimation = luminance,
                        interaction = glassInteraction,
                        tintColor = resolvedMiniTint,
                        crownHeightPx = { dropController?.crownHeightPx ?: 0f },
                        impactXFraction = { dropController?.impactXFraction ?: 0.5f },
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .trackBackdropBounds(miniBackdropBounds)
                    .clip(shape)
                    .pointerInput(glassInteraction) { glassInteraction.detectPress(this) }
                    .clickable(interactionSource = barInteraction, indication = comicIndicationOrNull(), onClick = onExpand),
            ) {
                Box(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.graphicsLayer {
                        alpha = (1f - (dropController?.queueBannerProgress ?: 0f)).coerceIn(0f, 1f)
                    },
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp)
                            .collapseVerticalPadding(8.dp, 2.5.dp, compactProgress),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DirectionalTrackContent(
                            trackKey = miniTrackKey,
                            track = track,
                            stepDirection = stepDirection,
                            slideFraction = 0.16f,
                            modifier = Modifier.weight(1f),
                            label = "miniGlassTrackSlide",
                        ) { activeTrack ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.collapseSquare(44.dp, 40.dp, compactProgress).artworkFlightThumb(artFlight)) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        tonalElevation = 2.dp,
                                        modifier = Modifier.fillMaxSize(),
                                    ) {
                                        PlayerArtwork(
                                            track = activeTrack,
                                            modifier = Modifier.fillMaxSize(),
                                            corner = 10.dp,
                                        )
                                    }
                                }
                                val miniArtistColor = miniGlassInk.copy(alpha = 0.72f)
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(
                                        activeTrack.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = miniGlassInk,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth().playingMarquee(state.isPlaying && windowActive),
                                    )
                                    MiniPlayerArtistRow(
                                        artist = activeTrack.artist,
                                        trackKey = activeTrack.videoId ?: "${activeTrack.title}|${activeTrack.artist}",
                                        qualityTier = state.qualityTier,
                                        color = miniArtistColor,
                                        style = MaterialTheme.typography.bodySmall,
                                        marqueeActive = state.isPlaying && windowActive,
                                        isDark = miniUseLightInk,
                                    )
                                }
                            }
                        }
                        // Inner controls sit plain on the glass card (no nested glass).
                        MiniPlayerFavoriteButton(
                            isLiked = isLiked,
                            tintColor = miniGlassInk,
                            useLightInk = miniUseLightInk,
                            onClick = {
                                miniHaptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onToggleLiked()
                            },
                        )
                        Surface(
                            onClick = onToggle,
                            shape = CircleShape,
                            color = Color.Transparent,
                            contentColor = miniGlassInk,
                            modifier = Modifier.size(44.dp).collapseScale(0.85f, compactProgress),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (state.isBuffering) {
                                    ExpressiveInlineLoadingIndicator(
                                        size = 24.dp,
                                        color = androidx.compose.material3.LocalContentColor.current,
                                        strokeWidth = 2.5.dp,
                                    )
                                } else {
                                    AnimatedPlayPauseIcon(state.isPlaying, Modifier.size(27.dp))
                                }
                            }
                        }
                        IconButton(
                            onClick = onNext,
                            enabled = state.queueSize > 1,
                            modifier = Modifier.size(44.dp),
                        ) {
                            Icon(
                                Icons.Filled.SkipNext,
                                "Next",
                                tint = miniGlassInk.copy(alpha = if (state.queueSize > 1) 1f else 0.38f),
                                modifier = Modifier.graphicsLayer { translationX = 6.dp.toPx() * nextNudge.value },
                            )
                        }
                    }
                    MiniPlayerProgress(progressState)
                }
                MiniPlayerImpactAndQueueOverlay(
                    dropController = dropController,
                    textColor = miniGlassInk,
                    isGlass = true,
                    isComic = false,
                    modifier = Modifier.matchParentSize(),
                )
                }
            }
        } else {
            // Material 3 / AMOLED / Comic Pop Mini-Player
            val miniCardShape = RoundedCornerShape(20.dp)
            val artShape = RoundedCornerShape(10.dp)
            val comicLineColor = MaterialTheme.colorScheme.outline
            val comicShadowColor = if (isDark) ComicPink else ComicInk
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val unifiedPath = buildMiniPlayerPillCrownPath(
                            width = size.width,
                            pillHeight = size.height,
                            pillTop = 0f,
                            cornerRadiusPx = 20.dp.toPx(),
                            crownHeightPx = dropController?.crownHeightPx ?: 0f,
                            impactXFraction = dropController?.impactXFraction ?: 0.5f,
                            densityPx = density,
                        )
                        if (isComic) {
                            val off = 4.dp.toPx()
                            translate(off, off) {
                                drawPath(path = unifiedPath, color = comicShadowColor)
                            }
                            drawPath(path = unifiedPath, color = m3SurfaceColor)
                            drawPath(path = unifiedPath, color = comicLineColor, style = Stroke(width = 2.5.dp.toPx()))
                        } else {
                            translate(0f, 3.dp.toPx()) {
                                drawPath(path = unifiedPath, color = Color.Black.copy(alpha = if (isDark) 0.34f else 0.14f))
                            }
                            drawPath(path = unifiedPath, color = m3SurfaceColor)
                            val rimHighlight = Color.White.copy(alpha = if (isDark) 0.16f else 0.48f)
                            drawPath(
                                path = unifiedPath,
                                brush = Brush.verticalGradient(
                                    0f to rimHighlight,
                                    0.6f to Color.Transparent,
                                ),
                                style = Stroke(width = 0.9.dp.toPx()),
                            )
                        }
                    },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(miniCardShape)
                    .clickable(interactionSource = barInteraction, indication = comicIndicationOrNull(), onClick = onExpand),
            ) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                Box(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = (1f - (dropController?.queueBannerProgress ?: 0f)).coerceIn(0f, 1f)
                        },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DirectionalTrackContent(
                            trackKey = miniTrackKey,
                            track = track,
                            stepDirection = stepDirection,
                            slideFraction = 0.16f,
                            modifier = Modifier.weight(1f),
                            label = "miniTrackSlide",
                        ) { activeTrack ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .artworkFlightThumb(artFlight)
                                        .clip(artShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    PlayerArtwork(
                                        track = activeTrack,
                                        modifier = Modifier.fillMaxSize(),
                                        corner = 10.dp,
                                    )
                                }

                                // Typography Hierarchy: 15.sp bold title (-0.2.sp letterSpacing) + 13.sp medium artist (0.78 alpha) + 1.dp space
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp),
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        text = activeTrack.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.2).sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth().playingMarquee(state.isPlaying && windowActive),
                                    )
                                    Spacer(Modifier.height(1.dp))
                                    MiniPlayerArtistRow(
                                        artist = activeTrack.artist,
                                        trackKey = activeTrack.videoId ?: "${activeTrack.title}|${activeTrack.artist}",
                                        qualityTier = state.qualityTier,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        marqueeActive = state.isPlaying && windowActive,
                                        isDark = LocalIsDarkTheme.current,
                                    )
                                }
                            }
                        }

                        MiniPlayerFavoriteButton(
                            isLiked = isLiked,
                            tintColor = MaterialTheme.colorScheme.onSurface,
                            onClick = {
                                miniHaptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                onToggleLiked()
                            },
                        )

                        Surface(
                            onClick = onToggle,
                            shape = CircleShape,
                            color = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (state.isBuffering) {
                                    ExpressiveInlineLoadingIndicator(
                                        size = 20.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    AnimatedPlayPauseIcon(state.isPlaying, Modifier.size(22.dp))
                                }
                            }
                        }

                        IconButton(
                            onClick = onNext,
                            enabled = state.queueSize > 1,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                Icons.Filled.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(22.dp).graphicsLayer { translationX = 6.dp.toPx() * nextNudge.value },
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.queueSize > 1) 1f else 0.38f),
                            )
                        }
                    }
                    MiniPlayerProgress(
                        progressState = progressState,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                MiniPlayerImpactAndQueueOverlay(
                    dropController = dropController,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    isGlass = false,
                    isComic = isComic,
                    modifier = Modifier.matchParentSize(),
                )
                }
                }
            }
        }
        }
    }
}

@Composable
private fun MiniPlayerImpactAndQueueOverlay(
    dropController: MiniPlayerDropController?,
    textColor: Color,
    isGlass: Boolean,
    isComic: Boolean,
    modifier: Modifier = Modifier,
) {
    if (dropController == null) return
    val ripple = dropController.impactRipple
    val queueMorph = dropController.queueBannerProgress
    val accent = MaterialTheme.colorScheme.primary

    if (ripple in 0.01f..0.99f) {
        Canvas(modifier) {
            val impactX = (dropController.impactXFraction * size.width).coerceIn(16.dp.toPx(), size.width - 16.dp.toPx())
            val maxR = size.width * 0.55f
            val r = androidx.compose.ui.util.lerp(10.dp.toPx(), maxR, ripple)
            val fade = (1f - ripple) * (1f - ripple)
            drawCircle(
                color = (if (isGlass) Color(0xFF9CEEFF) else accent).copy(alpha = 0.58f * fade),
                radius = r,
                center = Offset(impactX, 4.dp.toPx()),
                style = Stroke(width = androidx.compose.ui.util.lerp(3.4.dp.toPx(), 1.0.dp.toPx(), ripple)),
            )
        }
    }

    if (queueMorph > 0.01f) {
        val bannerBg = when {
            isComic -> MaterialTheme.colorScheme.primaryContainer
            isGlass -> Color(0xFF3EE6B4).copy(alpha = 0.22f * queueMorph)
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f)
        }
        val bannerContent = when {
            isComic || !isGlass -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> textColor
        }
        Box(
            modifier = modifier
                .graphicsLayer { alpha = queueMorph.coerceIn(0f, 1f) }
                .background(bannerBg),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = if (isGlass && !isComic) Color(0xFF72F5CC) else bannerContent,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = "Added to Queue",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = bannerContent,
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerFavoriteButton(
    isLiked: Boolean,
    tintColor: Color,
    useLightInk: Boolean = LocalIsDarkTheme.current,
    onClick: () -> Unit,
) {
    val activeTint = if (useLightInk) LikedHeartRedOnDark else LikedHeartRed
    val tapMotion = rememberLikedTapMotion(isLiked, initialScale = 0.6f)
    IconButton(
        onClick = onClick,
        modifier = Modifier.likeSparkles(tapMotion, activeTint).size(40.dp),
    ) {
        Icon(
            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isLiked) "Remove favourite" else "Add to favourites",
            tint = if (isLiked) activeTint else tintColor.copy(alpha = 0.72f),
            modifier = Modifier
                .size(20.dp)
                .likeNudgeHeart(active = !isLiked)
                .graphicsLayer {
                    scaleX = tapMotion.scale
                    scaleY = tapMotion.scale
                    rotationZ = tapMotion.rotationZ
                },
        )
    }
}

@Composable
internal fun MiniPlayerProgress(
    progressState: StateFlow<PlaybackProgressState>,
    modifier: Modifier = Modifier,
) {
    val comic = LocalComicTheme.current
    val progressFractionState = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(progressState) {
        progressState.collect { s ->
            val p = if (s.durationMs > 0) s.positionMs.toFloat() / s.durationMs else 0f
            progressFractionState.floatValue = p
        }
    }
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(if (comic) 3.5.dp else 3.dp)
            .clip(CircleShape)
            .background(
                if (comic) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
                else MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        // Scale an already measured layer instead of changing its width and
        // forcing the mini-player through measure/layout on every ticker tick.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = progressFractionState.floatValue.coerceIn(0f, 1f)
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
fun PlayingWaveBars(
    modifier: Modifier = Modifier,
    waveColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    shape: Shape = CircleShape,
) {
    val active = com.lastwave.app.ui.common.LocalAmbientAnimationsActive.current &&
        rememberPlayerWindowActive()
    val first: androidx.compose.runtime.State<Float>
    val second: androidx.compose.runtime.State<Float>
    val third: androidx.compose.runtime.State<Float>
    if (active) {
        val transition = rememberInfiniteTransition(label = "miniArtworkWave")
        first = transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 480, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "miniWaveFirst",
        )
        second = transition.animateFloat(
            initialValue = 0.95f,
            targetValue = 0.30f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 640, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "miniWaveSecond",
        )
        third = transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 530, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "miniWaveThird",
        )
    } else {
        first = remember { mutableFloatStateOf(0.25f) }
        second = remember { mutableFloatStateOf(0.95f) }
        third = remember { mutableFloatStateOf(0.35f) }
    }

    val content: @Composable () -> Unit = {
        // Fixed small badge content so it can never expand to fill the cover.
        // Outer Box is 26x22 (16+5+5, 12+5+5) â€” always tiny, bottom-end aligned by caller.
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .padding(horizontal = 5.dp, vertical = 5.dp)
                .size(16.dp, 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Canvas(
                Modifier.fillMaxSize(),
            ) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            val barCount = 3
            val barWidth = (size.width / 5.2f).coerceAtLeast(1.5f)
            val barGap = barWidth * 0.9f
            val totalContentWidth = barCount * barWidth + (barCount - 1) * barGap
            val startX = ((size.width - totalContentWidth) / 2f).coerceAtLeast(0f)

            repeat(barCount) { index ->
                val fraction = when (index) {
                    0 -> first.value
                    1 -> second.value
                    else -> third.value
                }
                val barHeight = (size.height * fraction).coerceIn(minOf(barWidth, size.height), size.height)
                drawRoundRect(
                    color = waveColor,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        x = startX + index * (barWidth + barGap),
                        y = size.height - barHeight,
                    ),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }
            }
        }
    }

    if (containerColor == Color.Transparent) {
        Box(
            modifier = modifier.defaultMinSize(minWidth = 24.dp, minHeight = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    } else {
        Surface(
            shape = shape,
            color = containerColor,
            modifier = modifier.defaultMinSize(minWidth = 26.dp, minHeight = 22.dp),
        ) {
            content()
        }
    }
}

/** The sheet's side of a mini player pull: [drag] by a root-space delta, [release] with px/s. */
interface MiniPlayerPull {
    fun drag(deltaY: Float)
    fun release(velocityY: Float)
}

private class MiniPullGesture {
    var coords: androidx.compose.ui.layout.LayoutCoordinates? = null
    val velocity = androidx.compose.ui.input.pointer.util.VelocityTracker()
    var active = false
        private set
    private var lastRootY = Float.NaN

    fun reset() {
        active = false
        lastRootY = Float.NaN
        velocity.resetTracking()
    }

    /** The first upward, mostly vertical move of an untouched card starts a pull. */
    fun claims(amount: Offset, dragX: Float, dragY: Float): Boolean {
        if (!active && dragX == 0f && dragY == 0f && amount.y < 0f && abs(amount.y) > abs(amount.x)) active = true
        return active
    }

    /** Root-space Y delta since the last event, or null for the first one. */
    fun track(change: androidx.compose.ui.input.pointer.PointerInputChange): Float? {
        val root = coords?.takeIf { it.isAttached }?.localToRoot(change.position) ?: return null
        velocity.addPosition(change.uptimeMillis, root)
        val delta = if (lastRootY.isNaN()) null else root.y - lastRootY
        lastRootY = root.y
        return delta
    }
}
