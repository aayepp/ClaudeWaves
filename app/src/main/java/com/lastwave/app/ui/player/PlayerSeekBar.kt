@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.lastwave.app.ui.player

import com.lastwave.app.ui.theme.LocalSakugaFx
import com.lastwave.app.ui.theme.LocalSakugaPalette
import com.lastwave.app.ui.theme.LocalSakugaTheme
import com.lastwave.app.ui.theme.PlayerPalette
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
import androidx.compose.animation.core.animateIntAsState
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
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.ui.layout.boundsInRoot
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
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalContext
import com.lastwave.app.ui.comic.clip
import com.lastwave.app.ui.comic.clipOnly
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.hapticfeedback.HapticFeedback
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
import com.lastwave.app.ui.common.ExpressiveInlineLoadingIndicator
import com.lastwave.app.ui.common.ExpressiveMotion
import com.lastwave.app.ui.common.PlaylistCover
import com.lastwave.app.ui.common.TrackContextMenuSheet
import com.lastwave.app.ui.common.TrackMenuCapabilities
import com.lastwave.app.ui.common.TrackMenuTarget
import com.lastwave.app.ui.common.GroupGap
import com.lastwave.app.ui.common.TopCurvedSectionClipShape
import com.lastwave.app.ui.common.groupPositionFor
import com.lastwave.app.ui.common.groupShape
import com.lastwave.app.ui.theme.ComicInk
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalLiquidGlass
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.LiquidGlassIntensity
import com.lastwave.app.ui.theme.LiquidGlassSurface
import com.lastwave.app.ui.theme.liquidGlass
import com.lastwave.app.ui.theme.liquidGlassChrome
import com.lastwave.app.ui.theme.liquidGlassContainerColor
import com.lastwave.app.ui.theme.liquidGlassSource
import com.lastwave.app.ui.theme.isLiquidGlassBackdropSupported
import com.lastwave.app.ui.theme.LocalLiquidGlassBackdrop
import com.lastwave.app.ui.theme.LocalLiquidGlassOverlayBackdrop
import com.lastwave.app.ui.theme.LiquidGlassPreset
import com.lastwave.app.ui.theme.Backdrop
import com.lastwave.app.ui.theme.rememberLayerBackdrop
import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.scale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.IntBuffer
import kotlin.time.Duration.Companion.seconds
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt


internal enum class SeekDirection { REWIND, FORWARD }

@Composable
internal fun PlayerProgressSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    tertiaryColor: Color = MaterialTheme.colorScheme.tertiary,
    wavy: Boolean = false,
    isPlaying: Boolean = false,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
) {
    val dragged by interactionSource.collectIsDraggedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val dropletMorph by animateFloatAsState(
        targetValue = if (dragged || pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 420f),
        label = "playerProgressDroplet",
    )
    val isGlass = LocalLiquidGlass.current
    val isComic = LocalComicTheme.current
    val isDark = LocalIsDarkTheme.current
    val darkBackdrop = PlayerPalette.shade.luminance() < 0.5f
    val primary = remember(primaryColor, darkBackdrop) {
        primaryColor.ensureVisibleOnBackdrop(darkBackdrop, 0.38f)
    }
    val tertiary = remember(tertiaryColor, darkBackdrop) {
        tertiaryColor.ensureVisibleOnBackdrop(darkBackdrop, 0.28f)
    }
    val inactive = PlayerPalette.ink.copy(alpha = if (enabled) 0.24f else 0.14f)
    val range = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
    val fraction = ((value - valueRange.start) / range).coerceIn(0f, 1f)
    val thumbColor = if (enabled) primary else PlayerPalette.ink.copy(alpha = 0.34f)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier.height(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (wavy) WavyProgressTrack(
                fraction = { fraction },
                isPlaying = isPlaying,
                dragging = dragged,
                isTranslucent = true,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                tertiaryColor = tertiaryColor,
                modifier = Modifier.fillMaxSize(),
            ) else Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val centerY = size.height / 2f

                val trackHeightPx = 14.dp.toPx()
                val cornerRadius = CornerRadius(
                    trackHeightPx / 2f,
                    trackHeightPx / 2f,
                )
                val thumbWidthPx = 5.dp.toPx()
                val thumbHeightPx = 38.dp.toPx()
                val dragWidthPx = 30.dp.toPx()
                val dragHeightPx = 24.dp.toPx()
                val currentThumbW = androidx.compose.ui.util.lerp(thumbWidthPx, dragWidthPx, dropletMorph.coerceAtLeast(0f)).coerceAtLeast(2f)
                val thumbClearancePx = 9.dp.toPx()

                val rawThumbCenterX = fraction * width
                val thumbCenterX = if (width > currentThumbW) {
                    rawThumbCenterX.coerceIn(
                        currentThumbW / 2f,
                        width - currentThumbW / 2f,
                    )
                } else {
                    width / 2f
                }

                val activeEndX = (thumbCenterX - thumbClearancePx)
                    .coerceIn(0f, width)
                val inactiveStartX = (thumbCenterX + thumbClearancePx)
                    .coerceIn(0f, width)

                // Thick active track.
                if (activeEndX > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                primary.copy(alpha = if (enabled) 1f else 0.42f),
                                tertiary.copy(alpha = if (enabled) 0.95f else 0.36f),
                            ),
                            startX = 0f,
                            endX = activeEndX,
                        ),
                        topLeft = Offset(
                            0f,
                            centerY - trackHeightPx / 2f,
                        ),
                        size = Size(activeEndX, trackHeightPx),
                        cornerRadius = cornerRadius,
                    )
                }

                // Thick inactive track.
                if (inactiveStartX < width) {
                    drawRoundRect(
                        color = inactive,
                        topLeft = Offset(
                            inactiveStartX,
                            centerY - trackHeightPx / 2f,
                        ),
                        size = Size(
                            width - inactiveStartX,
                            trackHeightPx,
                        ),
                        cornerRadius = cornerRadius,
                    )
                }

                // Endpoint marker.
                val endpointX = width - trackHeightPx / 2f
                if (inactiveStartX < endpointX) {
                    drawCircle(
                        color = thumbColor.copy(alpha = 0.90f),
                        radius = 2.dp.toPx(),
                        center = Offset(endpointX, centerY),
                    )
                }

                drawLiquidGlassDropletThumb(
                    center = Offset(thumbCenterX, centerY),
                    restWidthPx = thumbWidthPx,
                    restHeightPx = thumbHeightPx,
                    dragWidthPx = dragWidthPx,
                    dragHeightPx = dragHeightPx,
                    dragMorph = dropletMorph,
                    thumbColor = thumbColor,
                    isGlass = isGlass,
                    isComic = isComic,
                    isDark = isDark,
                )
            }

            // Transparent Material Slider preserves seeking and accessibility.
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                enabled = enabled,
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxSize(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                    disabledThumbColor = Color.Transparent,
                    disabledActiveTrackColor = Color.Transparent,
                    disabledInactiveTrackColor = Color.Transparent,
                    disabledActiveTickColor = Color.Transparent,
                    disabledInactiveTickColor = Color.Transparent,
                ),
            )
        }
    }
}

/** Scrub detents: a tick as the thumb is picked up, one per 2 % travelled, a thud on release. */
internal class ScrubHaptics(private val haptics: HapticFeedback) {
    private var step = -1

    fun onScrub(fraction: Float) {
        val now = (fraction * 50).toInt()
        if (step == -1) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        else if (now != step) haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        step = now
    }

    fun onRelease() {
        if (step != -1) haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
        step = -1
    }
}

@Composable
internal fun rememberScrubHaptics(): ScrubHaptics {
    val haptics = LocalHapticFeedback.current
    return remember(haptics) { ScrubHaptics(haptics) }
}

/**
 * Ensures a color has sufficient perceived luminance and saturation to stand out
 * vibrantly on dark scrims and backgrounds without washing out or disappearing.
 * Uses WCAG 2.1 relative luminance and HSL space to preserve the original hue.
 */
internal fun Color.ensureLuminousForDarkBackground(minLuminance: Float = 0.38f): Color {
    val argb = this.toArgb()
    val currentLuminance = ColorUtils.calculateLuminance(argb).toFloat()
    if (currentLuminance >= minLuminance) {
        return this
    }

    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)

    // If nearly achromatic (dark gray/black cover art), bring to a clean, crisp off-white/silver
    if (hsl[1] < 0.12f) {
        hsl[2] = hsl[2].coerceAtLeast(0.82f)
        return Color(ColorUtils.HSLToColor(hsl))
    }

    // Elevate lightness iteratively until target relative luminance is met
    var step = 0
    while (step < 20) {
        hsl[2] = (hsl[2] + 0.04f).coerceAtMost(0.92f)
        // Prevent high-saturation low-luminance hues (like deep blue) from staying too dark
        if (hsl[1] > 0.85f && hsl[2] < 0.65f) {
            hsl[1] = (hsl[1] * 0.96f).coerceAtLeast(0.60f)
        }
        val candidate = ColorUtils.HSLToColor(hsl)
        if (ColorUtils.calculateLuminance(candidate).toFloat() >= minLuminance) {
            return Color(candidate)
        }
        step++
    }

    return Color(ColorUtils.HSLToColor(hsl))
}

internal fun Color.ensureVisibleOnBackdrop(
    darkBackdrop: Boolean = PlayerPalette.shade.luminance() < 0.5f,
    minLuminance: Float = 0.38f,
    maxLightLuminance: Float = 0.36f,
): Color {
    if (darkBackdrop) return ensureLuminousForDarkBackground(minLuminance)
    val argb = this.toArgb()
    if (ColorUtils.calculateLuminance(argb).toFloat() <= maxLightLuminance) return this
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(argb, hsl)
    if (hsl[1] < 0.12f) {
        hsl[2] = hsl[2].coerceAtMost(0.32f)
        return Color(ColorUtils.HSLToColor(hsl))
    }
    repeat(20) {
        hsl[2] = (hsl[2] - 0.04f).coerceAtLeast(0.16f)
        val candidate = ColorUtils.HSLToColor(hsl)
        if (ColorUtils.calculateLuminance(candidate).toFloat() <= maxLightLuminance) {
            return Color(candidate)
        }
    }
    return Color(ColorUtils.HSLToColor(hsl))
}

@Composable
internal fun SeekBar(
    progressState: StateFlow<PlaybackProgressState>,
    isPlaying: Boolean,
    trackKey: String?,
    wavyEnabled: Boolean = false,
    onSeek: (Long) -> Unit,
    isTranslucent: Boolean = false,
    fallbackDurationMs: Long = 0L,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    tertiaryColor: Color = MaterialTheme.colorScheme.tertiary,
    telemetry: com.lastwave.app.playback.automix.api.DeckBlendTelemetry = com.lastwave.app.playback.automix.api.DeckBlendTelemetry(),
    abLoopStartMs: Long? = null,
    abLoopEndMs: Long? = null,
    onCycleAbLoop: (() -> Unit)? = null,
    onClearAbLoop: (() -> Unit)? = null,
    minimalStyle: Boolean = false,
    centerLabel: (@Composable () -> Unit)? = null,
) {
    // The 60 ms ticker is read in the draw phase (bar) and at 1 s (labels +
    // invisible slider semantics). Collecting it from composition recomposed
    // this whole subtree and re-recorded the full player on every tick.
    val progressFraction = remember { mutableFloatStateOf(0f) }
    val sliderFraction = remember { mutableFloatStateOf(0f) }
    val progressDurationMs = remember { mutableLongStateOf(0L) }
    val elapsedSeconds = remember { mutableLongStateOf(0L) }
    val currentFallbackDurationMs by rememberUpdatedState(fallbackDurationMs)
    // Keep collecting while obscured (tracking must not stop), but do not publish
    // state that invalidates the full-player window behind the shade/dialog.
    val windowActive = rememberPlayerWindowActive()
    LaunchedEffect(progressState, windowActive) {
        var lastSliderSecond = -1L
        progressState.collect { s ->
            val duration = if (s.durationMs > 0L) {
                s.durationMs
            } else {
                currentFallbackDurationMs.coerceAtLeast(0L)
            }
            val fraction = if (duration > 0L) {
                (s.positionMs.toDouble() / duration.toDouble()).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }
            val second = s.positionMs / 1000L
            if (!windowActive) return@collect
            progressFraction.floatValue = fraction
            progressDurationMs.longValue = duration
            if (second != elapsedSeconds.longValue) elapsedSeconds.longValue = second
            // The Material slider is visually transparent and only feeds
            // accessibility/gesture semantics; second precision is enough and
            // avoids a composition pass on every 60 ms tick.
            if (second != lastSliderSecond) {
                lastSliderSecond = second
                sliderFraction.floatValue = fraction
            }
        }
    }
    val boundedDurationMs = (if (progressDurationMs.longValue > 0L) {
        progressDurationMs.longValue
    } else {
        fallbackDurationMs
    }).coerceAtLeast(0L)

    // Current-gesture fraction only; null = finger off, show live position.
    // Nullable (never a stale 0f) so a press without movement seeks nowhere
    // and a gesture that ends without onValueChangeFinished can't pin the bar.
    val seekInteraction = remember(trackKey) { MutableInteractionSource() }
    var isInteracting by remember(trackKey) { mutableStateOf(false) }
    LaunchedEffect(seekInteraction, trackKey) {
        var dragCount = 0
        var pressCount = 0
        seekInteraction.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> dragCount++
                is DragInteraction.Stop, is DragInteraction.Cancel -> dragCount = maxOf(0, dragCount - 1)
                is PressInteraction.Press -> pressCount++
                is PressInteraction.Release, is PressInteraction.Cancel -> pressCount = maxOf(0, pressCount - 1)
            }
            isInteracting = dragCount > 0 || pressCount > 0
        }
    }
    var dragFraction by remember(trackKey) { mutableStateOf<Float?>(null) }
    var lastSeekFraction by remember(trackKey) { mutableStateOf<Float?>(null) }
    // Heal: framework reports finger lifted or cancelled but the finished callback
    // never ran -> drop the dead value and resume live position.
    LaunchedEffect(isInteracting, trackKey) {
        if (!isInteracting) {
            delay(120L)
            dragFraction = null
            lastSeekFraction = null
        }
    }
    // Watchdog: if dragFraction was set by a tap/cancelled gesture while not interacting,
    // ensure it never permanently wedges the seekbar.
    LaunchedEffect(dragFraction, isInteracting, trackKey) {
        if (dragFraction != null && !isInteracting) {
            delay(250L)
            dragFraction = null
            lastSeekFraction = null
        }
    }

    // Second precision for the invisible slider: only changes once per second
    // (or while dragging), so SeekBar composition stays off the 60 ms ticker.
    val sliderValue = (dragFraction ?: sliderFraction.floatValue).coerceIn(0f, 1f)

    val isGlassActive = LocalLiquidGlass.current &&
        isLiquidGlassBackdropSupported() &&
        LocalLiquidGlassBackdrop.current != null
    val darkBackdrop = PlayerPalette.shade.luminance() < 0.5f
    val effectivePrimary = when {
        isGlassActive -> PlayerPalette.ink.copy(alpha = 0.95f)
        isTranslucent -> PlayerPalette.ink
        else -> primaryColor.ensureVisibleOnBackdrop(darkBackdrop, 0.38f)
    }
    val inactiveColor = when {
        isGlassActive -> PlayerPalette.ink.copy(alpha = 0.20f)
        isTranslucent -> PlayerPalette.ink.copy(alpha = 0.24f)
        else -> PlayerPalette.ink.copy(alpha = 0.24f)
    }
    val textColor = when {
        isGlassActive -> PlayerPalette.ink.copy(alpha = 0.90f)
        isTranslucent -> PlayerPalette.ink.copy(alpha = 0.85f)
        else -> PlayerPalette.ink.copy(alpha = 0.82f)
    }

    val isComic = LocalComicTheme.current
    val isSakuga = LocalSakugaTheme.current
    val sakugaFx = LocalSakugaFx.current
    val sakugaPal = LocalSakugaPalette.current
    val isGlass = LocalLiquidGlass.current
    val isDark = LocalIsDarkTheme.current
    val comicPrimary = MaterialTheme.colorScheme.primary
    val isDraggingSeek = isInteracting || dragFraction != null
    var seekBoxBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var prevSeekFrac by remember { mutableStateOf<Float?>(null) }
    var prevSeekNanos by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var seekDragVel by remember { mutableFloatStateOf(0f) }
    var lastSeekDir by remember { mutableFloatStateOf(0f) }
    val smoothSeekVel by animateFloatAsState(
        targetValue = if (isSakuga && isDraggingSeek) seekDragVel else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 700f),
        label = "sakugaSeekThumbVel",
    )
    LaunchedEffect(isDraggingSeek) {
        if (!isDraggingSeek) {
            prevSeekFrac = null
            seekDragVel = 0f
            lastSeekDir = 0f
        }
    }
    val sakugaTailPath = remember { androidx.compose.ui.graphics.Path() }
    val dropletMorph by animateFloatAsState(
        targetValue = if (isDraggingSeek) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 420f),
        label = "seekBarDroplet",
    )
    val minimalBarHeightDp by animateDpAsState(
        targetValue = if (isDraggingSeek) 7.5.dp else 4.5.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "minimalSeekBarHeight",
    )

    // Same RTL pin as above: custom Canvas draws LTR, invisible Slider must match it.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (minimalStyle) 32.dp else 44.dp)
                .let { m ->
                    if (isSakuga) {
                        m.onGloballyPositioned { if (it.isAttached) seekBoxBounds = it.boundsInRoot() }
                    } else m
                },
            contentAlignment = Alignment.Center,
        ) {
            if (isSakuga) Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (minimalStyle) 32.dp else 44.dp),
            ) {
                val fraction = (dragFraction ?: progressFraction.floatValue).coerceIn(0f, 1f)
                val width = size.width
                val centerY = size.height / 2f
                val barH = if (minimalStyle) 6.dp.toPx() else 9.dp.toPx()
                val barRadius = CornerRadius(barH / 2f, barH / 2f)
                val strokePx = 2.dp.toPx()
                val shadowOff = 1.5.dp.toPx()
                val thumbR = if (minimalStyle) {
                    if (isDraggingSeek) 9.5.dp.toPx() else 7.dp.toPx()
                } else {
                    if (isDraggingSeek) 11.5.dp.toPx() else 9.dp.toPx()
                }
                val thumbCenterX = (fraction * width).coerceIn(thumbR, (width - thumbR).coerceAtLeast(thumbR))

                drawRoundRect(
                    color = sakugaPal.shadow,
                    topLeft = Offset(shadowOff, centerY - barH / 2f + shadowOff),
                    size = Size(width, barH),
                    cornerRadius = barRadius,
                )
                drawRoundRect(
                    color = inactiveColor,
                    topLeft = Offset(0f, centerY - barH / 2f),
                    size = Size(width, barH),
                    cornerRadius = barRadius,
                )
                if (thumbCenterX > 0f) {
                    drawRoundRect(
                        color = comicPrimary,
                        topLeft = Offset(0f, centerY - barH / 2f),
                        size = Size(thumbCenterX.coerceAtMost(width), barH),
                        cornerRadius = barRadius,
                    )
                }
                drawRoundRect(
                    color = sakugaPal.ink,
                    topLeft = Offset(0f, centerY - barH / 2f),
                    size = Size(width, barH),
                    cornerRadius = barRadius,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(strokePx),
                )

                val thumbCenter = Offset(thumbCenterX, centerY)
                val tailLen = (kotlin.math.abs(smoothSeekVel) * 18.dp.toPx()).coerceAtMost(24.dp.toPx())
                if (tailLen < 1.2f) {
                    drawCircle(
                        color = sakugaPal.shadow.copy(alpha = 0.55f),
                        radius = thumbR,
                        center = thumbCenter + Offset(shadowOff, shadowOff),
                    )
                    drawCircle(color = Color.White, radius = thumbR, center = thumbCenter)
                    drawCircle(
                        color = sakugaPal.ink,
                        radius = thumbR,
                        center = thumbCenter,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(strokePx),
                    )
                } else {
                    val dir = if (smoothSeekVel >= 0f) 1f else -1f
                    val tipX = (thumbCenter.x - dir * (thumbR + tailLen)).coerceIn(0f, width)
                    sakugaTailPath.reset()
                    sakugaTailPath.moveTo(tipX, centerY)
                    sakugaTailPath.quadraticTo(thumbCenter.x - dir * thumbR * 0.55f, centerY - thumbR, thumbCenter.x, centerY - thumbR)
                    sakugaTailPath.arcTo(
                        rect = androidx.compose.ui.geometry.Rect(
                            thumbCenter.x - thumbR,
                            centerY - thumbR,
                            thumbCenter.x + thumbR,
                            centerY + thumbR,
                        ),
                        startAngleDegrees = -90f,
                        sweepAngleDegrees = dir * 180f,
                        forceMoveTo = false,
                    )
                    sakugaTailPath.quadraticTo(thumbCenter.x - dir * thumbR * 0.55f, centerY + thumbR, tipX, centerY)
                    sakugaTailPath.close()
                    drawPath(path = sakugaTailPath, color = Color.White)
                    drawPath(
                        path = sakugaTailPath,
                        color = sakugaPal.ink,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokePx,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        ),
                    )
                }
            } else if (wavyEnabled && !minimalStyle) WavyProgressTrack(
                fraction = { dragFraction ?: progressFraction.floatValue },
                isPlaying = isPlaying,
                dragging = isInteracting || dragFraction != null,
                isTranslucent = isTranslucent,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                tertiaryColor = tertiaryColor,
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) else if (minimalStyle) Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) {
                val fraction = (dragFraction ?: progressFraction.floatValue).coerceIn(0f, 1f)
                val width = size.width
                val centerY = size.height / 2f
                val barH = minimalBarHeightDp.toPx()
                val barRadius = CornerRadius(barH / 2f, barH / 2f)
                val activeColor = if (isComic) comicPrimary else effectivePrimary

                drawRoundRect(
                    color = inactiveColor,
                    topLeft = Offset(0f, centerY - barH / 2f),
                    size = Size(width, barH),
                    cornerRadius = barRadius,
                )
                val activeW = (fraction * width).coerceIn(0f, width)
                if (activeW > 0f) {
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(0f, centerY - barH / 2f),
                        size = Size(activeW, barH),
                        cornerRadius = barRadius,
                    )
                }
                val restSizePx = if (isDraggingSeek) 12.dp.toPx() else 7.6.dp.toPx()
                val dragW = 28.dp.toPx()
                val dragH = 18.dp.toPx()
                val currentW = androidx.compose.ui.util.lerp(restSizePx, dragW, dropletMorph.coerceAtLeast(0f)).coerceAtLeast(2f)
                val dotX = (fraction * width).coerceIn(currentW / 2f, (width - currentW / 2f).coerceAtLeast(currentW / 2f))
                drawLiquidGlassDropletThumb(
                    center = Offset(dotX, centerY),
                    restWidthPx = restSizePx,
                    restHeightPx = restSizePx,
                    dragWidthPx = dragW,
                    dragHeightPx = dragH,
                    dragMorph = dropletMorph,
                    thumbColor = activeColor,
                    isGlass = isGlass,
                    isComic = isComic,
                    isDark = isDark,
                )
            } else Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                // Draw-phase read: the 60 ms tick invalidates draw without a recomposition.
                val fraction = (dragFraction ?: progressFraction.floatValue).coerceIn(0f, 1f)
                val width = size.width
                val height = size.height
                val centerY = height / 2f
                val trackHeightPx = 14.dp.toPx()
                val cornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)
                val thumbWidthPx = 5.dp.toPx()
                val thumbHeightPx = 38.dp.toPx()
                val dragWidthPx = 32.dp.toPx()
                val dragHeightPx = 24.dp.toPx()
                val currentThumbW = androidx.compose.ui.util.lerp(thumbWidthPx, dragWidthPx, dropletMorph.coerceAtLeast(0f)).coerceAtLeast(2f)
                val thumbClearancePx = 9.dp.toPx()
                val rawThumbCenterX = fraction * width
                val thumbCenterX = if (width > currentThumbW) {
                    rawThumbCenterX.coerceIn(currentThumbW / 2f, width - currentThumbW / 2f)
                } else {
                    width / 2f
                }
                val activeEndX = (thumbCenterX - thumbClearancePx).coerceIn(0f, width)
                val inactiveStartX = (thumbCenterX + thumbClearancePx).coerceIn(0f, width)

                // Thick active capsule ending before the vertical thumb.
                if (activeEndX > 0f) {
                    drawRoundRect(
                        color = effectivePrimary,
                        topLeft = Offset(0f, centerY - trackHeightPx / 2f),
                        size = Size(activeEndX, trackHeightPx),
                        cornerRadius = cornerRadius,
                    )
                }

                // Thick inactive capsule starting after the vertical thumb.
                if (inactiveStartX < width) {
                    drawRoundRect(
                        color = inactiveColor,
                        topLeft = Offset(inactiveStartX, centerY - trackHeightPx / 2f),
                        size = Size(width - inactiveStartX, trackHeightPx),
                        cornerRadius = cornerRadius,
                    )
                }

                // Small endpoint marker from the reference design.
                val endpointX = width - trackHeightPx / 2f
                if (inactiveStartX < endpointX) {
                    drawCircle(
                        color = effectivePrimary.copy(alpha = 0.90f),
                        radius = 2.dp.toPx(),
                        center = Offset(endpointX, centerY),
                    )
                }

                drawLiquidGlassDropletThumb(
                    center = Offset(thumbCenterX, centerY),
                    restWidthPx = thumbWidthPx,
                    restHeightPx = thumbHeightPx,
                    dragWidthPx = dragWidthPx,
                    dragHeightPx = dragHeightPx,
                    dragMorph = dropletMorph,
                    thumbColor = effectivePrimary,
                    isGlass = isGlass,
                    isComic = isComic,
                    isDark = isDark,
                )
            }

            // A-B loop range & pin markers: overlays cleanly on both capsule and wavy tracks.
            if (abLoopStartMs != null && boundedDurationMs > 0L) {
                val loopAccent = remember(tertiaryColor, darkBackdrop) {
                    tertiaryColor.ensureVisibleOnBackdrop(darkBackdrop, 0.42f)
                }
                Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    val width = size.width
                    val centerY = size.height / 2f
                    val durF = boundedDurationMs.toFloat().coerceAtLeast(1f)
                    val aX = ((abLoopStartMs.toFloat() / durF).coerceIn(0f, 1f) * width)
                    val bX = abLoopEndMs?.let { ((it.toFloat() / durF).coerceIn(0f, 1f) * width) }
                    val liveX = ((dragFraction ?: progressFraction.floatValue).coerceIn(0f, 1f) * width)
                    val spanStart = minOf(aX, bX ?: liveX)
                    val spanEnd = maxOf(aX, bX ?: liveX)
                    val spanW = (spanEnd - spanStart).coerceAtLeast(0f)
                    val bandH = 16.dp.toPx()
                    val bandRadius = CornerRadius(bandH / 2f, bandH / 2f)
                    if (spanW > 1f) {
                        drawRoundRect(
                            color = loopAccent.copy(alpha = if (bX != null) 0.30f else 0.16f),
                            topLeft = Offset(spanStart, centerY - bandH / 2f),
                            size = Size(spanW, bandH),
                            cornerRadius = bandRadius,
                        )
                        drawRoundRect(
                            color = loopAccent.copy(alpha = if (bX != null) 0.78f else 0.45f),
                            topLeft = Offset(spanStart, centerY - bandH / 2f),
                            size = Size(spanW, bandH),
                            cornerRadius = bandRadius,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx()),
                        )
                    }
                    val pinW = 3.dp.toPx()
                    val pinH = 22.dp.toPx()
                    val pinRadius = CornerRadius(pinW / 2f, pinW / 2f)
                    fun drawPin(x: Float) {
                        val clampedX = x.coerceIn(pinW / 2f, (width - pinW / 2f).coerceAtLeast(pinW / 2f))
                        drawRoundRect(
                            color = PlayerPalette.shade.copy(alpha = 0.45f),
                            topLeft = Offset(clampedX - pinW / 2f - 1.dp.toPx(), centerY - pinH / 2f - 1.dp.toPx()),
                            size = Size(pinW + 2.dp.toPx(), pinH + 2.dp.toPx()),
                            cornerRadius = CornerRadius((pinW + 2.dp.toPx()) / 2f, (pinW + 2.dp.toPx()) / 2f),
                        )
                        drawRoundRect(
                            color = loopAccent,
                            topLeft = Offset(clampedX - pinW / 2f, centerY - pinH / 2f),
                            size = Size(pinW, pinH),
                            cornerRadius = pinRadius,
                        )
                    }
                    drawPin(aX)
                    if (bX != null) drawPin(bX)
                }
            }

            // Invisible Material interaction layer: custom visuals, reliable seeking semantics.
            val scrubHaptics = rememberScrubHaptics()
            Slider(
                value = sliderValue,
                onValueChange = {
                    scrubHaptics.onScrub(it)
                    dragFraction = it
                    lastSeekFraction = it
                    if (isSakuga) {
                        val now = SystemClock.elapsedRealtimeNanos()
                        val old = prevSeekFrac
                        if (old == null) {
                            prevSeekFrac = it
                            prevSeekNanos = now
                            seekDragVel = 0f
                            lastSeekDir = 0f
                        } else {
                            val dtSec = ((now - prevSeekNanos) / 1_000_000_000f).coerceAtLeast(0.004f)
                            val delta = it - old
                            if (kotlin.math.abs(delta) > 0.0004f) {
                                val instVel = (delta / dtSec / 1.8f).coerceIn(-1f, 1f)
                                seekDragVel = (seekDragVel * 0.45f + instVel * 0.55f).coerceIn(-1f, 1f)
                                lastSeekDir = kotlin.math.sign(seekDragVel)
                                if (kotlin.math.abs(seekDragVel) > 0.14f) {
                                    seekBoxBounds?.let { b ->
                                        val cx = b.left + (it.coerceIn(0f, 1f) * b.width)
                                        sakugaFx?.emitSliderCloud(
                                            Offset(cx, b.center.y),
                                            kotlin.math.sign(seekDragVel),
                                            kotlin.math.abs(seekDragVel),
                                        )
                                    }
                                }
                            }
                            prevSeekFrac = it
                            prevSeekNanos = now
                        }
                    }
                },
                onValueChangeFinished = {
                    scrubHaptics.onRelease()
                    val releaseFrac = lastSeekFraction ?: dragFraction ?: prevSeekFrac
                    if (isSakuga && (kotlin.math.abs(seekDragVel) > 0.12f || lastSeekDir != 0f)) {
                        seekBoxBounds?.let { b ->
                            val cx = b.left + ((releaseFrac ?: 0.5f).coerceIn(0f, 1f) * b.width)
                            sakugaFx?.emitSliderRelease(
                                Offset(cx, b.center.y),
                                if (lastSeekDir != 0f) lastSeekDir else 1f,
                            )
                        }
                    }
                    prevSeekFrac = null
                    seekDragVel = 0f
                    lastSeekDir = 0f
                    // Commit only this gesture's value; no value = no seek.
                    val target = (lastSeekFraction ?: dragFraction)?.let {
                        (it * boundedDurationMs).toLong().coerceIn(0L, boundedDurationMs)
                    }
                    dragFraction = null
                    lastSeekFraction = null
                    if (target != null && boundedDurationMs > 0L) {
                        onSeek(target)
                    }
                },
                valueRange = 0f..1f,
                enabled = boundedDurationMs > 0L,
                interactionSource = seekInteraction,
                modifier = Modifier.fillMaxSize(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent,
                    disabledThumbColor = Color.Transparent,
                    disabledActiveTrackColor = Color.Transparent,
                    disabledInactiveTrackColor = Color.Transparent,
                    disabledActiveTickColor = Color.Transparent,
                    disabledInactiveTickColor = Color.Transparent,
                ),
            )
        }

        Spacer(Modifier.height(2.dp))

        // Time labels: update once per second (or live while dragging).
        val durationSecond = boundedDurationMs / 1000L
        val currentSecond = dragFraction?.let {
            (it * durationSecond).toLong().coerceIn(0L, durationSecond)
        } ?: elapsedSeconds.longValue
        val remainingSecond = (durationSecond - currentSecond).coerceAtLeast(0L)
        val formattedElapsed = remember(currentSecond) { formatTime(currentSecond * 1000L) }
        val formattedRemaining = remember(remainingSecond, boundedDurationMs) {
            if (boundedDurationMs > 0L) "\u2212${formatTime(remainingSecond * 1000L)}" else "--:--"
        }
        val haptics = LocalHapticFeedback.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formattedElapsed,
                style = if (isTranslucent) MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium) else MaterialTheme.typography.labelMedium,
                color = textColor,
            )
            if (abLoopStartMs != null && onCycleAbLoop != null && boundedDurationMs > 0L) {
                val loopLabel = if (abLoopEndMs != null) {
                    "A–B ${formatTime(abLoopStartMs)}–${formatTime(abLoopEndMs)}"
                } else {
                    "A ${formatTime(abLoopStartMs)} · Set B"
                }
                val pillAccent = remember(tertiaryColor, darkBackdrop) {
                    tertiaryColor.ensureVisibleOnBackdrop(darkBackdrop, 0.42f)
                }
                val pillShape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                Box(
                    modifier = Modifier
                        .clip(pillShape)
                        .background(pillAccent.copy(alpha = 0.22f), pillShape)
                        .border(1.dp, pillAccent.copy(alpha = 0.65f), pillShape)
                        .combinedClickable(
                            onClick = {
                                haptics.performHapticFeedback(
                                    if (abLoopEndMs != null) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
                                )
                                onCycleAbLoop()
                            },
                            onLongClick = {
                                if (onClearAbLoop != null) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onClearAbLoop()
                                }
                            },
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = loopLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum",
                        ),
                        color = PlayerPalette.ink.copy(alpha = 0.96f),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            } else if (centerLabel != null) {
                Box(contentAlignment = Alignment.Center) {
                    centerLabel()
                }
            } else if (onCycleAbLoop != null && boundedDurationMs > 0L) {
                val loopLabel = "A–B"
                val pillShape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                Box(
                    modifier = Modifier
                        .clipOnly(pillShape)
                        .combinedClickable(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                onCycleAbLoop()
                            },
                            onLongClick = null,
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = loopLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontFeatureSettings = "tnum",
                        ),
                        color = textColor.copy(alpha = 0.56f),
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            Text(
                text = formattedRemaining,
                style = if (isTranslucent) MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium) else MaterialTheme.typography.labelMedium,
                color = textColor,
            )
        }
    }
    }
}

internal fun formatTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0) / 1000)
    return "%d:%02d".format(total / 60, total % 60)
}

/**
 * Morphs a seekbar or volume thumb into an elongated translucent Liquid Glass droplet capsule
 * while scrubbed/dragged, with crisp Ink outline in Comic Pop and clean solid pill in No-Glass.
 */
internal fun DrawScope.drawLiquidGlassDropletThumb(
    center: Offset,
    restWidthPx: Float,
    restHeightPx: Float,
    dragWidthPx: Float,
    dragHeightPx: Float,
    dragMorph: Float,
    thumbColor: Color,
    isGlass: Boolean,
    isComic: Boolean,
    isDark: Boolean,
) {
    val m = dragMorph.coerceAtLeast(0f)
    val mClamped = dragMorph.coerceIn(0f, 1f)
    val w = androidx.compose.ui.util.lerp(restWidthPx, dragWidthPx, m).coerceAtLeast(2f)
    val h = androidx.compose.ui.util.lerp(restHeightPx, dragHeightPx, m).coerceAtLeast(2f)
    val topLeft = Offset(center.x - w / 2f, center.y - h / 2f)
    val cornerR = minOf(w, h) / 2f
    val corner = CornerRadius(cornerR, cornerR)

    if (isComic) {
        val strokePad = 2.dp.toPx()
        drawRoundRect(
            color = ComicInk,
            topLeft = Offset(topLeft.x - strokePad, topLeft.y - strokePad),
            size = Size(w + strokePad * 2f, h + strokePad * 2f),
            cornerRadius = CornerRadius(cornerR + strokePad, cornerR + strokePad),
        )
        drawRoundRect(
            color = thumbColor,
            topLeft = topLeft,
            size = Size(w, h),
            cornerRadius = corner,
        )
        return
    }

    if (!isGlass) {
        val shadowPad = 1.dp.toPx()
        drawRoundRect(
            color = PlayerPalette.shade.copy(alpha = 0.35f),
            topLeft = Offset(topLeft.x - shadowPad, topLeft.y - shadowPad),
            size = Size(w + shadowPad * 2f, h + shadowPad * 2f),
            cornerRadius = CornerRadius(cornerR + shadowPad, cornerR + shadowPad),
        )
        drawRoundRect(
            color = thumbColor,
            topLeft = topLeft,
            size = Size(w, h),
            cornerRadius = corner,
        )
        if (mClamped > 0.05f) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.32f * mClamped),
                topLeft = topLeft,
                size = Size(w, h),
                cornerRadius = corner,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        return
    }

    if (mClamped <= 0.02f) {
        val shadowPad = 1.dp.toPx()
        drawRoundRect(
            color = PlayerPalette.shade.copy(alpha = 0.32f),
            topLeft = Offset(topLeft.x - shadowPad, topLeft.y - shadowPad),
            size = Size(w + shadowPad * 2f, h + shadowPad * 2f),
            cornerRadius = CornerRadius(cornerR + shadowPad, cornerR + shadowPad),
        )
        drawRoundRect(
            color = thumbColor,
            topLeft = topLeft,
            size = Size(w, h),
            cornerRadius = corner,
        )
        return
    }

    // Soft glowing halo behind the expanding glass droplet.
    val haloPad = 3.5.dp.toPx() * mClamped
    drawRoundRect(
        color = thumbColor.copy(alpha = 0.24f * mClamped),
        topLeft = Offset(topLeft.x - haloPad, topLeft.y - haloPad),
        size = Size(w + haloPad * 2f, h + haloPad * 2f),
        cornerRadius = CornerRadius(cornerR + haloPad, cornerR + haloPad),
    )

    // Translucent frosted glass core + top-sheen fill gradient.
    val coreAlpha = androidx.compose.ui.util.lerp(1f, if (isDark) 0.30f else 0.38f, mClamped)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = (if (isDark) 0.34f else 0.56f) * mClamped),
                thumbColor.copy(alpha = coreAlpha),
                PlayerPalette.shade.copy(alpha = 0.26f * mClamped),
            ),
            startY = topLeft.y,
            endY = topLeft.y + h,
        ),
        topLeft = topLeft,
        size = Size(w, h),
        cornerRadius = corner,
    )

    // Inner convex meniscus reflection along the upper curve of the droplet.
    val insetX = 3.dp.toPx() * mClamped
    val insetY = 2.dp.toPx() * mClamped
    val meniscusW = (w - insetX * 2f).coerceAtLeast(1f)
    val meniscusH = (h * 0.44f).coerceAtLeast(1f)
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = (if (isDark) 0.58f else 0.82f) * mClamped),
                Color.Transparent,
            ),
            startY = topLeft.y + insetY,
            endY = topLeft.y + insetY + meniscusH,
        ),
        topLeft = Offset(topLeft.x + insetX, topLeft.y + insetY),
        size = Size(meniscusW, meniscusH),
        cornerRadius = CornerRadius(meniscusH / 2f, meniscusH / 2f),
    )

    // Chromatic edge dispersion at higher glass clarity.
    if (LiquidGlassIntensity.value >= 0.45f) {
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF7BE8FF).copy(alpha = 0.45f * mClamped),
                    Color.Transparent,
                    Color(0xFFFFB0E8).copy(alpha = 0.42f * mClamped),
                ),
                startX = topLeft.x,
                endX = topLeft.x + w,
            ),
            topLeft = topLeft,
            size = Size(w, h),
            cornerRadius = corner,
            style = Stroke(width = 1.2.dp.toPx()),
        )
    }

    // Light-mode hairline + bright directional outer specular rim.
    if (!isDark) {
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.14f * mClamped),
            topLeft = topLeft,
            size = Size(w, h),
            cornerRadius = corner,
            style = Stroke(width = 1.8.dp.toPx()),
        )
    }
    drawRoundRect(
        brush = Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.94f * mClamped),
            0.48f to Color.White.copy(alpha = 0.22f * mClamped),
            1f to Color.White.copy(alpha = 0.56f * mClamped),
            start = topLeft,
            end = Offset(topLeft.x + w, topLeft.y + h),
        ),
        topLeft = topLeft,
        size = Size(w, h),
        cornerRadius = corner,
        style = Stroke(width = 1.35.dp.toPx()),
    )
}
