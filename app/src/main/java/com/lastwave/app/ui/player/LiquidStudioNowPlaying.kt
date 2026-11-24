@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.lastwave.app.ui.player

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.media3.common.Player
import com.lastwave.app.R
import com.lastwave.app.data.canvas.CanvasArtwork
import com.lastwave.app.playback.InternalDirectAvailability
import com.lastwave.app.playback.MusicPlayer
import com.lastwave.app.playback.MusicPlayerState
import com.lastwave.app.playback.PlayableTrack
import com.lastwave.app.playback.PlaybackProgressState
import com.lastwave.app.playback.SignalPathReport
import com.lastwave.app.playback.UsbDacMonitor
import com.lastwave.app.playback.qualityBadgeLabel
import com.lastwave.app.ui.comic.Surface
import com.lastwave.app.ui.common.DirectionalTrackContent
import com.lastwave.app.ui.common.ExpressiveInlineLoadingIndicator
import com.lastwave.app.ui.common.ExpressiveMotion
import com.lastwave.app.ui.common.playingMarquee
import com.lastwave.app.ui.common.rememberTrackStepDirection
import com.lastwave.app.ui.theme.ComicAccent
import com.lastwave.app.ui.theme.ComicCyan
import com.lastwave.app.ui.theme.ComicInk
import com.lastwave.app.ui.theme.ComicPink
import com.lastwave.app.ui.theme.LiquidGlassPreset
import com.lastwave.app.ui.theme.LiquidGlassSurface
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.LocalLiquidGlass
import com.lastwave.app.ui.theme.LocalLiquidGlassBackdrop
import com.lastwave.app.ui.theme.PlayerPalette
import com.lastwave.app.ui.theme.comicPanel
import com.lastwave.app.ui.theme.glassRim
import com.lastwave.app.ui.theme.isLiquidGlassBackdropSupported
import com.lastwave.app.ui.theme.liquidGlassChrome
import com.lastwave.app.ui.theme.liquidGlassContainerColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Shared frosted glass / comic surface for Liquid Studio controls across all 6 themes
 * (Dark Glass, Light Glass, AMOLED, non-glass `< API 31`, Comic Pop Light & Dark).
 */
@Composable
private fun LiquidStudioGlassSurface(
    shape: Shape,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    borderAlpha: Float = 0.20f,
    onClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val comic = LocalComicTheme.current
    val glassActive = LocalLiquidGlass.current && isLiquidGlassBackdropSupported() && LocalLiquidGlassBackdrop.current != null
    val darkBackdrop = PlayerPalette.shade.luminance() < 0.5f

    val bgColor = when {
        comic -> MaterialTheme.colorScheme.surface
        glassActive -> liquidGlassContainerColor(
            if (tint.isSpecified) tint.copy(alpha = 0.22f) else PlayerPalette.ink.copy(alpha = 0.14f),
        )
        darkBackdrop -> Color(0xCC121622)
        else -> PlayerPalette.shade.copy(alpha = 0.84f)
    }

    val border = when {
        comic -> null
        glassActive -> null
        else -> BorderStroke(1.dp, PlayerPalette.ink.copy(alpha = borderAlpha))
    }

    val source = interactionSource ?: remember { MutableInteractionSource() }

    Surface(
        shape = shape,
        color = bgColor,
        contentColor = PlayerPalette.ink,
        border = border,
        tonalElevation = 0.dp,
        shadowElevation = if (!comic && !glassActive) 6.dp else 0.dp,
        modifier = modifier
            .comicPanel(shape, offset = 3.dp, translucent = glassActive)
            .then(
                if (glassActive) {
                    Modifier
                        .liquidGlassChrome(
                            shape = shape,
                            enabled = true,
                            preset = LiquidGlassPreset.FloatingControls,
                            interactionSource = source,
                        )
                        .glassRim(shape)
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = source,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Box(contentAlignment = Alignment.Center, content = content)
    }
}

/**
 * Animation #1: Squircle Liquid Ripple + RGB Chromatic Lens Reveal.
 * Fires 3 staggered super-rounded squircle dispersion rings around the artwork sleeve.
 */
private fun Modifier.liquidStudioSquircleRipple(
    progress: () -> Float,
    primaryColor: Color,
    secondaryColor: Color,
    comicAccent: Color,
    isComic: Boolean,
    isDark: Boolean,
): Modifier = drawBehind {
    val rp = progress()
    if (rp <= 0.001f || rp >= 0.999f) return@drawBehind
    val delays = floatArrayOf(0f, 0.12f, 0.25f)
    val maxExpand = 52.dp.toPx()
    val baseCorner = 28.dp.toPx()
    val peakAlpha = if (isDark || isComic) 0.62f else 0.44f
    val cyan = if (isComic) ComicCyan else Color(0xFF38BDF8)
    val magenta = if (isComic) ComicPink else Color(0xFFF472B6)
    val gold = if (isComic) comicAccent else secondaryColor

    delays.forEachIndexed { idx, delayFrac ->
        val local = ((rp - delayFrac) / (1f - delayFrac)).coerceIn(0f, 1f)
        if (local > 0f && local < 1f) {
            val expand = maxExpand * local
            val decay = (1f - local) * (1f - local)
            val alpha = peakAlpha * decay * (1f - idx * 0.16f)
            val strokeW = (3.8.dp.toPx() * (1f - 0.55f * local)).coerceAtLeast(1f)
            val r = baseCorner + expand * 0.45f
            val chromaticShift = 2.4.dp.toPx() * (1f - local * 0.4f)

            if (!isComic) {
                // Chromatic RGB outer cyan fringe
                drawRoundRect(
                    color = cyan.copy(alpha = alpha * 0.55f),
                    topLeft = Offset(-(expand + chromaticShift), -(expand + chromaticShift)),
                    size = Size(size.width + (expand + chromaticShift) * 2f, size.height + (expand + chromaticShift) * 2f),
                    cornerRadius = CornerRadius(r + chromaticShift * 0.4f, r + chromaticShift * 0.4f),
                    style = Stroke(width = strokeW * 0.68f),
                )
                // Chromatic RGB inner magenta fringe
                drawRoundRect(
                    color = magenta.copy(alpha = alpha * 0.52f),
                    topLeft = Offset(-(expand - chromaticShift).coerceAtLeast(-expand), -(expand - chromaticShift).coerceAtLeast(-expand)),
                    size = Size(size.width + ((expand - chromaticShift) * 2f).coerceAtLeast(0f), size.height + ((expand - chromaticShift) * 2f).coerceAtLeast(0f)),
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(width = strokeW * 0.68f),
                )
            }
            val c1 = primaryColor.copy(alpha = alpha)
            val c2 = gold.copy(alpha = alpha)
            drawRoundRect(
                brush = Brush.linearGradient(listOf(c1, c2)),
                topLeft = Offset(-expand, -expand),
                size = Size(size.width + expand * 2f, size.height + expand * 2f),
                cornerRadius = CornerRadius(r, r),
                style = Stroke(width = strokeW),
            )
        }
    }
}

/**
 * Slid-up frosted track pill inside [NowPlayingHeader] when switching to Lyrics or Queue tabs.
 */
@Composable
internal fun LiquidStudioHeaderTrackPill(
    track: PlayableTrack,
    tabLabel: String,
    modifier: Modifier = Modifier,
) {
    LiquidStudioGlassSurface(
        shape = CircleShape,
        borderAlpha = 0.24f,
        modifier = modifier.height(38.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 6.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlayerArtwork(
                track = track,
                modifier = Modifier.size(26.dp),
                corner = 13.dp,
                isPlaying = false,
                pausedForTransition = true,
            )
            Column(modifier = Modifier.widthIn(max = 175.dp)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PlayerPalette.ink.copy(alpha = 0.96f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${track.artist} \u2022 $tabLabel",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = PlayerPalette.ink.copy(alpha = 0.68f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Animation #4: Floating Frosted Liquid Glass Title Capsule sitting beneath artwork.
 */
@Composable
private fun LiquidStudioTitlePill(
    track: PlayableTrack,
    state: MusicPlayerState,
    isPlaying: Boolean,
    windowActive: Boolean,
    isLiked: Boolean,
    isCustomAudio: Boolean,
    stepDirection: Int,
    primaryColor: Color,
    onToggleLiked: () -> Unit,
    onOpenAudioControls: () -> Unit,
    onOpenSignalPath: () -> Unit,
    onOpenArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackKey = remember(track.videoId, track.title, track.artist) {
        track.videoId?.takeIf(String::isNotBlank) ?: "${track.title}|${track.artist}"
    }
    val heartScale = remember { Animatable(1f) }
    var lastLiked by remember(trackKey) { mutableStateOf(isLiked) }
    LaunchedEffect(isLiked, trackKey) {
        if (isLiked != lastLiked) {
            lastLiked = isLiked
            heartScale.snapTo(if (isLiked) 1.32f else 0.84f)
            heartScale.animateTo(1f, spring(dampingRatio = 0.56f, stiffness = 460f))
        }
    }
    val heartTint = if (isLiked) {
        if (PlayerPalette.shade.luminance() < 0.5f) LikedHeartRedOnDark else LikedHeartRed
    } else {
        PlayerPalette.ink.copy(alpha = 0.82f)
    }
    val badge = remember(state) {
        runCatching { qualityBadgeLabel(state) }.getOrDefault("").takeIf { it.isNotBlank() && it != "AUDIO" }
    }

    LiquidStudioGlassSurface(
        shape = RoundedCornerShape(22.dp),
        borderAlpha = 0.22f,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DirectionalTrackContent(
                trackKey = trackKey,
                track = track,
                stepDirection = stepDirection,
                modifier = Modifier.weight(1f),
                label = "liquidStudioTitlePillText",
            ) { activeTrack ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = activeTrack.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PlayerPalette.ink.copy(alpha = 0.96f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.playingMarquee(isPlaying && windowActive),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = activeTrack.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = PlayerPalette.ink.copy(alpha = 0.76f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = hapticClick { onOpenArtist(activeTrack.artist) }),
                        )
                        if (badge != null) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = 0.22f))
                                    .border(1.dp, primaryColor.copy(alpha = 0.45f), CircleShape)
                                    .clickable(onClick = hapticClick(action = onOpenSignalPath))
                                    .padding(horizontal = 7.dp, vertical = 1.5.dp),
                            ) {
                                Text(
                                    text = badge,
                                    fontSize = 9.sp,
                                    lineHeight = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.4.sp,
                                    color = PlayerPalette.ink.copy(alpha = 0.94f),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCustomAudio) primaryColor.copy(alpha = 0.22f) else PlayerPalette.ink.copy(alpha = 0.10f),
                        )
                        .clickable(onClick = hapticClick(action = onOpenAudioControls)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Audio controls",
                        tint = if (isCustomAudio) primaryColor else PlayerPalette.ink.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PlayerPalette.ink.copy(alpha = 0.10f))
                        .clickable(
                            onClick = hapticClick(
                                if (isLiked) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
                                onToggleLiked,
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike" else "Like",
                        tint = heartTint,
                        modifier = Modifier
                            .size(19.dp)
                            .graphicsLayer {
                                scaleX = heartScale.value
                                scaleY = heartScale.value
                            },
                    )
                }
            }
        }
    }
}

/**
 * Animation #4: Morphing Liquid Glass Seekbar.
 * Lush 26dp translucent glass capsule on Now Playing; morphs smoothly into a slim 6dp precision
 * bar when in Click-Wheel queue mode or Lyrics view.
 */
@Composable
internal fun LiquidStudioMorphSeekBar(
    progressState: StateFlow<PlaybackProgressState>,
    isPlaying: Boolean,
    trackKey: String,
    fallbackDurationMs: Long,
    primaryColor: Color,
    secondaryColor: Color,
    onSeek: (Long) -> Unit,
    slim: Boolean,
    modifier: Modifier = Modifier,
    showTimestamps: Boolean = true,
    qualityBadge: String? = null,
    onOpenSignalPath: (() -> Unit)? = null,
    abLoopStartMs: Long? = null,
    abLoopEndMs: Long? = null,
    onCycleAbLoop: (() -> Unit)? = null,
) {
    val comic = LocalComicTheme.current
    val comicAccent = ComicAccent
    val glassActive = LocalLiquidGlass.current && isLiquidGlassBackdropSupported() && LocalLiquidGlassBackdrop.current != null
    val haptics = LocalHapticFeedback.current
    val darkBackdrop = PlayerPalette.shade.luminance() < 0.5f

    val progressFraction = remember { mutableFloatStateOf(0f) }
    val progressDurationMs = remember { mutableLongStateOf(0L) }
    val elapsedSeconds = remember { mutableLongStateOf(0L) }
    val currentFallbackDuration by rememberUpdatedState(fallbackDurationMs)

    LaunchedEffect(progressState, trackKey) {
        progressState.collect { s ->
            val duration = if (s.durationMs > 0L) s.durationMs else currentFallbackDuration.coerceAtLeast(0L)
            val fraction = if (duration > 0L) (s.positionMs.toDouble() / duration.toDouble()).toFloat().coerceIn(0f, 1f) else 0f
            progressFraction.floatValue = fraction
            progressDurationMs.longValue = duration
            val sec = s.positionMs / 1000L
            if (sec != elapsedSeconds.longValue) elapsedSeconds.longValue = sec
        }
    }

    val boundedDurationMs = (if (progressDurationMs.longValue > 0L) progressDurationMs.longValue else fallbackDurationMs).coerceAtLeast(0L)
    var dragFraction by remember(trackKey) { mutableStateOf<Float?>(null) }
    val currentDurationState by rememberUpdatedState(boundedDurationMs)
    val currentOnSeek by rememberUpdatedState(onSeek)

    val trackHeight by animateDpAsState(
        targetValue = if (slim) 6.dp else 26.dp,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 420f),
        label = "morphSeekHeight",
    )
    val morphFraction by animateFloatAsState(
        targetValue = if (slim) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.80f, stiffness = 400f),
        label = "morphSeekFrac",
    )

    val currentPosMs = dragFraction?.let { (it * boundedDurationMs).toLong() } ?: (elapsedSeconds.longValue * 1000L)
    val elapsedStr = remember(currentPosMs / 1000L, dragFraction != null) { formatTime(currentPosMs) }
    val remainingMs = (boundedDurationMs - currentPosMs).coerceAtLeast(0L)
    val remainingStr = remember(remainingMs / 1000L, boundedDurationMs, dragFraction != null) {
        if (boundedDurationMs > 0L) "\u2212${formatTime(remainingMs)}" else "--:--"
    }

    val trackSurfaceColor = when {
        comic -> MaterialTheme.colorScheme.surface
        glassActive -> PlayerPalette.ink.copy(alpha = 0.14f)
        darkBackdrop -> Color(0xAA191E2D)
        else -> PlayerPalette.shade.copy(alpha = 0.68f)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .pointerInput(trackKey, boundedDurationMs) {
                        if (currentDurationState <= 0L) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            dragFraction = (down.position.x / w).coerceIn(0f, 1f)
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    change.consume()
                                    val finalFrac = dragFraction
                                    dragFraction = null
                                    if (finalFrac != null) {
                                        currentOnSeek((finalFrac * currentDurationState).toLong())
                                    }
                                    break
                                }
                                change.consume()
                                dragFraction = (change.position.x / w).coerceIn(0f, 1f)
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                val barH = trackHeight
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barH)
                        .then(
                            if (!slim && !comic) Modifier.glassRim(CircleShape)
                            else if (comic) Modifier.border(1.5.dp, ComicInk, CircleShape)
                            else Modifier,
                        )
                        .clip(CircleShape)
                        .background(trackSurfaceColor),
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val frac = (dragFraction ?: progressFraction.floatValue).coerceIn(0f, 1f)
                        val fillW = (size.width * frac).coerceIn(0f, size.width)
                        val r = size.height / 2f

                        if (fillW > 1f) {
                            val fillBrush = if (comic) {
                                Brush.horizontalGradient(listOf(comicAccent, comicAccent))
                            } else {
                                Brush.horizontalGradient(
                                    listOf(
                                        primaryColor.copy(alpha = 0.88f),
                                        secondaryColor.copy(alpha = 0.94f),
                                    ),
                                )
                            }
                            drawRoundRect(
                                brush = fillBrush,
                                topLeft = Offset.Zero,
                                size = Size(fillW.coerceAtLeast(size.height.coerceAtMost(fillW)), size.height),
                                cornerRadius = CornerRadius(r, r),
                            )

                            // Specular glass highlight band inside thick seek capsule
                            if (morphFraction < 0.7f && !comic) {
                                drawRoundRect(
                                    brush = Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.34f * (1f - morphFraction)),
                                            Color.Transparent,
                                        ),
                                    ),
                                    topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
                                    size = Size((fillW - 4.dp.toPx()).coerceAtLeast(0f), size.height * 0.44f),
                                    cornerRadius = CornerRadius(r, r),
                                )
                            }
                        }

                        // Glowing thumb appears cleanly as bar morphs to slim mode (or while scrubbing)
                        val thumbAlpha = if (dragFraction != null) 1f else morphFraction
                        if (thumbAlpha > 0.05f) {
                            val thumbRadius = lerp(6.dp.toPx(), 7.dp.toPx(), thumbAlpha)
                            val cx = fillW.coerceIn(thumbRadius, (size.width - thumbRadius).coerceAtLeast(thumbRadius))
                            drawCircle(
                                color = if (comic) ComicInk else Color.White.copy(alpha = 0.96f * thumbAlpha),
                                radius = thumbRadius,
                                center = Offset(cx, size.height / 2f),
                            )
                            if (comic) {
                                drawCircle(
                                    color = comicAccent,
                                    radius = thumbRadius * 0.62f,
                                    center = Offset(cx, size.height / 2f),
                                )
                            }
                        }
                    }
                }
            }

            if (showTimestamps) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = elapsedStr,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = PlayerPalette.ink.copy(alpha = 0.80f),
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (abLoopStartMs != null && onCycleAbLoop != null) {
                            val loopLabel = if (abLoopEndMs != null) "A-B ON" else "LOOP A…"
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = 0.25f))
                                    .clickable(onClick = hapticClick(action = onCycleAbLoop))
                                    .padding(horizontal = 8.dp, vertical = 1.5.dp),
                            ) {
                                Text(
                                    text = loopLabel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PlayerPalette.ink,
                                )
                            }
                        } else if (!qualityBadge.isNullOrBlank() && onOpenSignalPath != null) {
                            Text(
                                text = qualityBadge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.4.sp,
                                color = PlayerPalette.ink.copy(alpha = 0.72f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = hapticClick(action = onOpenSignalPath))
                                    .padding(horizontal = 4.dp),
                            )
                        }
                    }

                    Text(
                        text = remainingStr,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = PlayerPalette.ink.copy(alpha = 0.80f),
                    )
                }
            }
        }
    }
}

/**
 * Animation #3 (Top half): 3D Cover Flow Queue synced with Click-Wheel dial.
 */
@Composable
private fun LiquidStudioCoverFlowStage(
    queue: List<PlayableTrack>,
    currentIndex: Int,
    previewIndex: Int,
    onPreviewIndexChange: (Int) -> Unit,
    onSelectQueueTrack: (Int) -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val comic = LocalComicTheme.current
    val isDark = LocalIsDarkTheme.current
    val haptics = LocalHapticFeedback.current

    if (queue.isEmpty()) return
    val safePreview = previewIndex.coerceIn(0, queue.lastIndex)
    val pagerState = rememberPagerState(
        initialPage = safePreview,
        pageCount = { queue.size },
    )

    LaunchedEffect(safePreview) {
        if (pagerState.currentPage != safePreview && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(
                safePreview,
                animationSpec = spring(dampingRatio = 0.80f, stiffness = 420f),
            )
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != previewIndex) {
            onPreviewIndexChange(pagerState.currentPage)
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val coverSide = minOf(maxWidth - 110.dp, maxHeight - 66.dp).coerceIn(136.dp, 240.dp)
        val horizPadding = ((maxWidth - coverSide) / 2f).coerceAtLeast(36.dp)
        val previewTrack = queue.getOrNull(safePreview)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = horizPadding),
                pageSpacing = (-14).dp,
                key = { idx -> "cf_${idx}_${queue[idx].videoId ?: queue[idx].title}" },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(coverSide + 24.dp),
            ) { page ->
                val item = queue[page]
                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                val absOffset = abs(pageOffset).coerceIn(0f, 1.5f)
                val closeness = (1f - absOffset).coerceIn(0f, 1f)
                val isCentered = absOffset < 0.25f

                Box(
                    modifier = Modifier
                        .size(coverSide)
                        .zIndex(10f - absOffset)
                        .graphicsLayer {
                            cameraDistance = 14f * density
                            rotationY = (-pageOffset * 38f).coerceIn(-46f, 46f)
                            val s = lerp(0.76f, 1.02f, closeness)
                            scaleX = s
                            scaleY = s
                            alpha = lerp(0.55f, 1f, closeness)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(coverSide)
                                .shadow(if (isCentered) 20.dp else 8.dp, RoundedCornerShape(22.dp))
                                .comicPanel(RoundedCornerShape(22.dp), offset = 3.dp)
                                .clickable(
                                    onClick = hapticClick {
                                        if (isCentered) {
                                            onSelectQueueTrack(page)
                                        } else {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onPreviewIndexChange(page)
                                        }
                                    },
                                ),
                        ) {
                            PlayerArtwork(
                                track = item,
                                modifier = Modifier.fillMaxSize(),
                                corner = 22.dp,
                                isPlaying = isPlaying && page == currentIndex,
                                pausedForTransition = true,
                            )
                            if (page == currentIndex) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.90f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = "PLAYING",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }

                        if (!comic && isCentered) {
                            Box(
                                modifier = Modifier
                                    .width(coverSide)
                                    .height(16.dp)
                                    .padding(top = 3.dp)
                                    .clipToBounds()
                                    .graphicsLayer {
                                        alpha = if (isDark) 0.22f else 0.14f
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    }
                                    .drawWithContent {
                                        drawContent()
                                        drawRect(
                                            brush = Brush.verticalGradient(listOf(Color.White, Color.Transparent)),
                                            blendMode = BlendMode.DstIn,
                                        )
                                    },
                            ) {
                                PlayerArtwork(
                                    track = item,
                                    modifier = Modifier
                                        .requiredSize(coverSide)
                                        .graphicsLayer { scaleY = -1f }
                                        .blur(1.5.dp),
                                    corner = 22.dp,
                                    isPlaying = false,
                                    pausedForTransition = true,
                                )
                            }
                        }
                    }
                }
            }

            if (previewTrack != null) {
                Spacer(Modifier.height(6.dp))
                LiquidStudioGlassSurface(
                    shape = CircleShape,
                    borderAlpha = 0.20f,
                    onClick = hapticClick { onSelectQueueTrack(safePreview) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "${previewTrack.title} \u2022 ${previewTrack.artist}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PlayerPalette.ink.copy(alpha = 0.95f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 220.dp),
                        )
                        Text(
                            text = "${safePreview + 1}/${queue.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animation #3 (Bottom half): Tactile Frosted iPod Click-Wheel Dial with rotary thumb scrubbing.
 */
@Composable
private fun LiquidStudioClickWheel(
    wheelSize: Dp,
    queueSize: Int,
    previewIndex: Int,
    currentIndex: Int,
    isPlaying: Boolean,
    primaryColor: Color,
    onStepPreview: (Int) -> Unit,
    onCommitCenter: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onOpenVerticalQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val comic = LocalComicTheme.current
    val haptics = LocalHapticFeedback.current
    val currentOnStep by rememberUpdatedState(onStepPreview)
    val currentQueueSize by rememberUpdatedState(queueSize)
    var rotaryAngleAccum by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier.size(wheelSize),
        contentAlignment = Alignment.Center,
    ) {
        LiquidStudioGlassSurface(
            shape = CircleShape,
            borderAlpha = 0.24f,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(queueSize) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dist = hypot(down.position.x - cx, down.position.y - cy)
                        val minDim = minOf(size.width, size.height).toFloat()
                        val innerR = minDim * 0.22f
                        val outerR = minDim * 0.50f
                        if (dist !in innerR..outerR) return@awaitEachGesture

                        var prevAngle = atan2(down.position.y - cy, down.position.x - cx)
                        rotaryAngleAccum = 0f
                        val stepAngle = 0.46f // ~26 degrees per detent tick
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val curDist = hypot(change.position.x - cx, change.position.y - cy)
                            if (curDist < innerR * 0.7f) continue

                            val curAngle = atan2(change.position.y - cy, change.position.x - cx)
                            var delta = curAngle - prevAngle
                            if (delta > PI) delta -= (2.0 * PI).toFloat()
                            if (delta < -PI) delta += (2.0 * PI).toFloat()
                            prevAngle = curAngle
                            rotaryAngleAccum += delta

                            if (abs(rotaryAngleAccum) >= stepAngle) {
                                val steps = (rotaryAngleAccum / stepAngle).toInt()
                                rotaryAngleAccum -= steps * stepAngle
                                if (currentQueueSize > 0 && steps != 0) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    currentOnStep(steps)
                                }
                                change.consume()
                            }
                        }
                    }
                },
        ) {
            // Subtle concentric groove rings inside wheel
            Canvas(modifier = Modifier.fillMaxSize()) {
                val ringAlpha = if (comic) 0.25f else 0.14f
                drawCircle(
                    color = PlayerPalette.ink.copy(alpha = ringAlpha),
                    radius = size.minDimension * 0.40f,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            // Top: switch to full vertical QueuePanel
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .clip(CircleShape)
                    .clickable(onClick = hapticClick(action = onOpenVerticalQueue))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = "Full queue",
                        tint = PlayerPalette.ink.copy(alpha = 0.88f),
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "LIST",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlayerPalette.ink.copy(alpha = 0.88f),
                    )
                }
            }

            // Left: Previous in Cover Flow
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = hapticClick { onStepPreview(-1) }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous track",
                    tint = PlayerPalette.ink.copy(alpha = 0.90f),
                    modifier = Modifier.size(22.dp),
                )
            }

            // Right: Next in Cover Flow
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = hapticClick { onStepPreview(1) }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next track",
                    tint = PlayerPalette.ink.copy(alpha = 0.90f),
                    modifier = Modifier.size(22.dp),
                )
            }

            // Bottom: Play/Pause active track
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .clip(CircleShape)
                    .clickable(
                        onClick = hapticClick(
                            if (isPlaying) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
                            onTogglePlayPause,
                        ),
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = PlayerPalette.ink.copy(alpha = 0.90f),
                    modifier = Modifier.size(18.dp),
                )
            }

            // Center elevated tactile Play/Select disc
            val willSwitch = previewIndex != currentIndex
            LiquidStudioGlassSurface(
                shape = CircleShape,
                tint = if (willSwitch) primaryColor else Color.Unspecified,
                borderAlpha = 0.30f,
                onClick = hapticClick(action = onCommitCenter),
                modifier = Modifier.size(wheelSize * 0.36f),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (willSwitch) Icons.Filled.PlayArrow else if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (willSwitch) "Play selected" else "Play or Pause",
                        tint = if (willSwitch) primaryColor else PlayerPalette.ink,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

/**
 * Full-width 5-button Liquid Studio Transport Row.
 */
@Composable
private fun LiquidStudioTransportRow(
    state: MusicPlayerState,
    player: MusicPlayer,
    compact: Boolean,
    primaryColor: Color,
    modifier: Modifier = Modifier,
) {
    val comic = LocalComicTheme.current
    val playSize = if (compact) 58.dp else 66.dp
    val skipSize = if (compact) 46.dp else 52.dp
    val sideSize = 42.dp

    val prevInteraction = remember { MutableInteractionSource() }
    val playInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val isPrevPressed by prevInteraction.collectIsPressedAsState()
    val isPlayPressed by playInteraction.collectIsPressedAsState()
    val isNextPressed by nextInteraction.collectIsPressedAsState()

    val prevScale by animateFloatAsState(if (isPrevPressed) 0.86f else 1f, ExpressiveMotion.spatialSpring(), label = "lsPrevScale")
    val playScale by animateFloatAsState(if (isPlayPressed) 0.90f else 1f, spring(dampingRatio = 0.65f, stiffness = 420f), label = "lsPlayScale")
    val nextScale by animateFloatAsState(if (isNextPressed) 0.86f else 1f, ExpressiveMotion.spatialSpring(), label = "lsNextScale")

    val shuffleTint by animateColorAsState(
        targetValue = if (state.shuffleEnabled) primaryColor else PlayerPalette.ink.copy(alpha = 0.72f),
        animationSpec = tween(180),
        label = "lsShuffleTint",
    )
    val repeatActive = state.repeatMode != Player.REPEAT_MODE_OFF
    val repeatTint by animateColorAsState(
        targetValue = if (repeatActive) primaryColor else PlayerPalette.ink.copy(alpha = 0.72f),
        animationSpec = tween(180),
        label = "lsRepeatTint",
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(sideSize)
                .clip(CircleShape)
                .clickable(
                    onClick = hapticClick(
                        if (state.shuffleEnabled) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
                        player::toggleShuffle,
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = if (state.shuffleEnabled) "Shuffle on" else "Shuffle off",
                tint = shuffleTint,
                modifier = Modifier.size(21.dp),
            )
            if (state.shuffleEnabled) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(shuffleTint),
                )
            }
        }

        LiquidStudioGlassSurface(
            shape = CircleShape,
            onClick = hapticClick(action = player::previous),
            interactionSource = prevInteraction,
            modifier = Modifier
                .size(skipSize)
                .graphicsLayer {
                    scaleX = prevScale
                    scaleY = prevScale
                },
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                tint = PlayerPalette.ink.copy(alpha = 0.96f),
                modifier = Modifier.size(28.dp),
            )
        }

        LiquidGlassSurface(
            glassModifier = Modifier.liquidGlassChrome(
                CircleShape,
                LocalLiquidGlass.current,
                LiquidGlassPreset.FloatingControls,
                interactionSource = playInteraction,
            ),
            onClick = hapticClick(
                if (state.isPlaying) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
                player::togglePlayPause,
            ),
            interactionSource = playInteraction,
            shape = CircleShape,
            color = if (comic) ComicAccent else primaryColor,
            contentColor = if (comic) ComicInk else if (primaryColor.luminance() > 0.56f) Color(0xFF10131C) else Color.White,
            modifier = Modifier
                .size(playSize)
                .graphicsLayer {
                    scaleX = playScale
                    scaleY = playScale
                },
        ) {
            if (state.isBuffering) {
                ExpressiveInlineLoadingIndicator(
                    size = 26.dp,
                    color = androidx.compose.material3.LocalContentColor.current,
                    strokeWidth = 3.dp,
                )
            } else {
                AnimatedPlayPauseIcon(
                    isPlaying = state.isPlaying,
                    modifier = Modifier.size(if (compact) 30.dp else 34.dp),
                )
            }
        }

        LiquidStudioGlassSurface(
            shape = CircleShape,
            onClick = hapticClick(action = player::next),
            interactionSource = nextInteraction,
            modifier = Modifier
                .size(skipSize)
                .graphicsLayer {
                    scaleX = nextScale
                    scaleY = nextScale
                },
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next",
                tint = PlayerPalette.ink.copy(alpha = 0.96f),
                modifier = Modifier.size(28.dp),
            )
        }

        Box(
            modifier = Modifier
                .size(sideSize)
                .clip(CircleShape)
                .clickable(
                    onClick = hapticClick(
                        if (state.repeatMode == Player.REPEAT_MODE_ONE) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
                        player::cycleRepeatMode,
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = "Repeat mode",
                tint = repeatTint,
                modifier = Modifier.size(21.dp),
            )
            if (repeatActive) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(repeatTint),
                )
            }
        }
    }
}

/**
 * Balanced 4-slot frosted action dock across bottom of Liquid Studio Now Playing.
 */
@Composable
private fun LiquidStudioBottomDock(
    hasLyrics: Boolean,
    coverFlowActive: Boolean,
    isEqEnabled: Boolean,
    isVideoActive: Boolean,
    onOpenLyrics: () -> Unit,
    onOpenOutputPicker: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onToggleCoverFlow: () -> Unit,
    onOpenVerticalQueue: () -> Unit,
    onToggleVideo: (() -> Unit)?,
    downloadState: NowPlayingDownloadState,
    onDownload: (() -> Unit)?,
    primaryColor: Color,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val isDownloaded = downloadState.status == com.lastwave.app.ui.common.TrackDownloadStatus.DOWNLOADED
    val isDownloading = downloadState.status == com.lastwave.app.ui.common.TrackDownloadStatus.DOWNLOADING

    LiquidStudioGlassSurface(
        shape = CircleShape,
        borderAlpha = 0.22f,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            LiquidStudioDockButton(
                icon = Icons.Filled.FormatQuote,
                label = stringResource(R.string.player_lyrics),
                highlighted = false,
                showDot = hasLyrics,
                highlightColor = primaryColor,
                onClick = hapticClick(action = onOpenLyrics),
            )

            LiquidStudioDockButton(
                icon = if (isEqEnabled) Icons.Filled.GraphicEq else Icons.Filled.Headphones,
                label = "Output",
                highlighted = isEqEnabled,
                showDot = isEqEnabled,
                highlightColor = primaryColor,
                onClick = hapticClick(action = onOpenOutputPicker),
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenEqualizer()
                },
            )

            LiquidStudioDockButton(
                icon = if (coverFlowActive) Icons.Filled.Album else Icons.AutoMirrored.Filled.QueueMusic,
                label = "Cover Flow",
                highlighted = coverFlowActive,
                showDot = coverFlowActive,
                highlightColor = primaryColor,
                onClick = hapticClick(action = onToggleCoverFlow),
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenVerticalQueue()
                },
            )

            when {
                onToggleVideo != null -> {
                    LiquidStudioDockButton(
                        icon = Icons.Filled.SmartDisplay,
                        label = "Video",
                        highlighted = isVideoActive,
                        showDot = isVideoActive,
                        highlightColor = primaryColor,
                        onClick = hapticClick(action = onToggleVideo),
                    )
                }
                onDownload != null && (!isDownloaded || downloadState.openInLibrary) -> {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable(onClick = hapticClick(action = onDownload)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                progress = { (downloadState.progressPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.size(20.dp),
                                color = primaryColor,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = if (isDownloaded) Icons.Filled.LibraryMusic else Icons.Filled.Download,
                                contentDescription = "Download",
                                tint = if (isDownloaded) primaryColor else PlayerPalette.ink.copy(alpha = 0.84f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                else -> {
                    LiquidStudioDockButton(
                        icon = Icons.Filled.GraphicEq,
                        label = "Equalizer",
                        highlighted = isEqEnabled,
                        showDot = isEqEnabled,
                        highlightColor = primaryColor,
                        onClick = hapticClick(action = onOpenEqualizer),
                    )
                }
            }
        }
    }
}

@Composable
private fun LiquidStudioDockButton(
    icon: ImageVector,
    label: String,
    highlighted: Boolean,
    showDot: Boolean,
    highlightColor: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.86f else 1f, ExpressiveMotion.spatialFast(), label = "lsDockScale")
    val tint = if (highlighted) highlightColor else PlayerPalette.ink.copy(alpha = 0.84f)

    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .then(
                if (highlighted) Modifier.background(highlightColor.copy(alpha = 0.18f)) else Modifier,
            )
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        if (showDot) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(highlightColor),
            )
        }
    }
}

/**
 * Full-screen tightly-proportioned portrait **Liquid Studio** Now Playing experience.
 */
@Composable
internal fun LiquidStudioNowPlayingScreen(
    track: PlayableTrack,
    state: MusicPlayerState,
    progressState: StateFlow<PlaybackProgressState>,
    player: MusicPlayer,
    lyricsState: LyricsUiState,
    signalPath: SignalPathReport,
    usbDac: UsbDacMonitor.State,
    availability: InternalDirectAvailability,
    isTablet: Boolean,
    isVideoActive: Boolean,
    isLiked: Boolean,
    isCustomAudio: Boolean,
    isEqEnabled: Boolean,
    wavySeekbarEnabled: Boolean,
    playerActionDockEnabled: Boolean = true,
    nowPlayingMinimalStyle: Boolean = false,
    windowActive: Boolean,
    isDistractionFree: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    onToggleDistractionFree: () -> Unit,
    onToggleLiked: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onToggleFullArtwork: () -> Unit = {},
    onOpenAudioControls: () -> Unit,
    onOpenSignalPath: () -> Unit,
    onOpenOutputPicker: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onCloseVideo: () -> Unit,
    onEnterVideoFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleVideo: (() -> Unit)? = null,
    downloadState: NowPlayingDownloadState = NowPlayingDownloadState(),
    onDownload: (() -> Unit)? = null,
    showSleeve: Boolean = false,
    sleeveCanvas: CanvasArtwork? = null,
    pausedForTransition: Boolean = false,
    lyricsPreview: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val isComic = LocalComicTheme.current
    val comicAccent = ComicAccent
    val isDarkTheme = LocalIsDarkTheme.current

    var coverFlowQueueMode by rememberSaveable { mutableStateOf(false) }
    var previewIndex by remember(state.currentIndex, state.queue.size) {
        mutableIntStateOf(state.currentIndex.coerceAtLeast(0))
    }

    val trackKey = remember(track.videoId, track.title, track.artist) {
        track.videoId?.takeIf(String::isNotBlank) ?: "${track.title}|${track.artist}"
    }
    val stepDirection = rememberTrackStepDirection(
        trackKey = trackKey,
        queueIndex = state.currentIndex,
        queueSize = state.queue.size,
    )
    val hasLyrics = remember(lyricsState) {
        lyricsState is LyricsUiState.Success && (lyricsState.lines.isNotEmpty() || !lyricsState.plainLyrics.isNullOrBlank())
    }
    val qualityBadge = remember(state) {
        runCatching { qualityBadgeLabel(state) }.getOrDefault("").takeIf { it.isNotBlank() && it != "AUDIO" }
    }

    // Animation #1: Squircle Liquid Ripple + brief Lens Reveal trigger on track switch & open
    val rippleProgress = remember { Animatable(0f) }
    val lensReveal = remember { Animatable(1.035f) }
    LaunchedEffect(trackKey) {
        rippleProgress.snapTo(0f)
        lensReveal.snapTo(1.035f)
        launch { rippleProgress.animateTo(1f, tween(880, easing = FastOutSlowInEasing)) }
        launch { lensReveal.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 420f)) }
    }

    // Gesture states for sleeve swipe & double-tap seek/like
    val artworkDrag = remember { Animatable(0f) }
    val swipeThreshold = remember(density) { with(density) { 68.dp.toPx() } }
    var seekOverlayDirection by remember { mutableStateOf<SeekDirection?>(null) }
    var seekOverlaySeconds by remember { mutableIntStateOf(5) }
    var seekResetJob by remember { mutableStateOf<Job?>(null) }
    var heartBurstVisible by remember { mutableStateOf(false) }
    var heartBurstJob by remember { mutableStateOf<Job?>(null) }
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }
    var lastTapRegion by remember { mutableIntStateOf(-1) }

    val prevTrack = remember(state.queue, state.currentIndex, state.repeatMode, state.shuffleEnabled) {
        if (state.shuffleEnabled) null
        else state.queue.getOrNull(state.currentIndex - 1)
            ?: if (state.repeatMode == Player.REPEAT_MODE_ALL && state.queue.size > 1) state.queue.lastOrNull() else null
    }
    val nextTrack = remember(state.queue, state.currentIndex, state.repeatMode, state.shuffleEnabled) {
        if (state.shuffleEnabled) null
        else state.queue.getOrNull(state.currentIndex + 1)
            ?: if (state.repeatMode == Player.REPEAT_MODE_ALL && state.queue.size > 1) state.queue.firstOrNull() else null
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val compactHeight = maxHeight < 700.dp
        val ultraCompactHeight = maxHeight < 550.dp
        val deckGap = if (ultraCompactHeight) 6.dp else if (compactHeight) 9.dp else 12.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .padding(horizontal = if (ultraCompactHeight) 14.dp else 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Flexible upper artwork / 3D Cover Flow stage filling leftover height cleanly
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = if (ultraCompactHeight) 2.dp else 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = coverFlowQueueMode,
                    transitionSpec = {
                        (fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                            scaleIn(tween(260, easing = FastOutSlowInEasing), initialScale = 0.94f)) togetherWith
                            (fadeOut(tween(200, easing = FastOutSlowInEasing)) +
                                scaleOut(tween(220, easing = FastOutSlowInEasing), targetScale = 0.95f))
                    },
                    label = "liquidStudioStageMode",
                ) { inCoverFlow ->
                    if (inCoverFlow) {
                        LiquidStudioCoverFlowStage(
                            queue = state.queue,
                            currentIndex = state.currentIndex,
                            previewIndex = previewIndex,
                            onPreviewIndexChange = { previewIndex = it },
                            onSelectQueueTrack = { idx ->
                                if (idx != state.currentIndex) player.seekToQueueItem(idx)
                                else player.togglePlayPause()
                            },
                            isPlaying = state.isPlaying,
                        )
                    } else {
                        val previewLines = (lyricsState as? LyricsUiState.Success)
                            ?.takeIf { lyricsPreview && it.isSynced && !it.isInstrumental && it.lines.isNotEmpty() }
                            ?.lines
                        val lyricsPresence by animateFloatAsState(
                            targetValue = if (previewLines != null && showSleeve) 1f else 0f,
                            animationSpec = ExpressiveMotion.spatialSlow(),
                            label = "lsLyricsPresence",
                        )

                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(trackKey, isDistractionFree) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        var isDrag = false
                                        var dragDx = 0f
                                        val touchSlop = viewConfiguration.touchSlop
                                        val longPressAt = SystemClock.uptimeMillis() + viewConfiguration.longPressTimeoutMillis
                                        var longPressed = false

                                        while (true) {
                                            val event = if (isDrag || longPressed) awaitPointerEvent()
                                            else withTimeoutOrNull((longPressAt - SystemClock.uptimeMillis()).coerceAtLeast(0L)) { awaitPointerEvent() }

                                            if (event == null) {
                                                longPressed = true
                                                lastTapRegion = -1
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onToggleFullArtwork()
                                                continue
                                            }
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            if (longPressed) {
                                                change.consume()
                                                if (!change.pressed) break
                                                continue
                                            }
                                            if (!change.pressed) {
                                                if (isDrag) {
                                                    val pState = player.state.value
                                                    val canNext = nextTrack != null || (pState.shuffleEnabled && pState.queue.size > 1)
                                                    val canPrev = prevTrack != null || (pState.shuffleEnabled && pState.queue.size > 1) || pState.positionMs > 5_000L
                                                    when {
                                                        dragDx < -swipeThreshold && canNext -> {
                                                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                                            player.next()
                                                        }
                                                        dragDx > swipeThreshold && canPrev -> {
                                                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                                            player.previous()
                                                        }
                                                    }
                                                    coroutineScope.launch {
                                                        artworkDrag.animateTo(0f, spring(dampingRatio = 0.80f, stiffness = 460f))
                                                    }
                                                } else if (isDistractionFree) {
                                                    onToggleDistractionFree()
                                                } else {
                                                    val now = SystemClock.elapsedRealtime()
                                                    val region = when {
                                                        down.position.x < size.width * 0.32f -> 0
                                                        down.position.x > size.width * 0.68f -> 2
                                                        else -> 1
                                                    }
                                                    if (lastTapRegion == region && now - lastTapTimestamp < 340L) {
                                                        lastTapRegion = -1
                                                        lastTapTimestamp = 0L
                                                        when (region) {
                                                            0 -> {
                                                                seekOverlaySeconds = if (seekOverlayDirection == SeekDirection.REWIND) seekOverlaySeconds + 5 else 5
                                                                seekOverlayDirection = SeekDirection.REWIND
                                                                player.seekTo((player.state.value.positionMs - 5_000L).coerceAtLeast(0L))
                                                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                                                seekResetJob?.cancel()
                                                                seekResetJob = coroutineScope.launch { delay(700L); seekOverlayDirection = null }
                                                            }
                                                            2 -> {
                                                                seekOverlaySeconds = if (seekOverlayDirection == SeekDirection.FORWARD) seekOverlaySeconds + 5 else 5
                                                                seekOverlayDirection = SeekDirection.FORWARD
                                                                val maxD = player.state.value.durationMs.coerceAtLeast(0L)
                                                                val t = player.state.value.positionMs + 5_000L
                                                                player.seekTo(if (maxD > 0L) t.coerceAtMost(maxD) else t)
                                                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                                                seekResetJob?.cancel()
                                                                seekResetJob = coroutineScope.launch { delay(700L); seekOverlayDirection = null }
                                                            }
                                                            else -> {
                                                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                                                onDoubleTapLike()
                                                                heartBurstVisible = true
                                                                heartBurstJob?.cancel()
                                                                heartBurstJob = coroutineScope.launch { delay(540L); heartBurstVisible = false }
                                                            }
                                                        }
                                                    } else {
                                                        lastTapRegion = region
                                                        lastTapTimestamp = now
                                                    }
                                                }
                                                break
                                            }
                                            if (change.isConsumed) break
                                            val dx = change.position.x - down.position.x
                                            val dy = change.position.y - down.position.y
                                            if (!isDrag) {
                                                if (abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                                                    isDrag = true
                                                    dragDx = dx
                                                    change.consume()
                                                    coroutineScope.launch { artworkDrag.snapTo(dx) }
                                                }
                                            } else {
                                                change.consume()
                                                dragDx = dx
                                                coroutineScope.launch { artworkDrag.snapTo(dx) }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (showSleeve) {
                                val lyricsBudget = if (lyricsPreview) (LYRIC_PREVIEW_HEIGHT + 14.dp) else 0.dp
                                val reflBudget = if (!isComic) 22.dp else 0.dp
                                val sleeveSide = minOf(maxWidth - 8.dp, maxHeight - lyricsBudget - reflBudget).coerceIn(168.dp, 368.dp)
                                val sleeveShape = RoundedCornerShape(26.dp)

                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(sleeveSide)
                                            .liquidStudioSquircleRipple(
                                                progress = { rippleProgress.value },
                                                primaryColor = primaryColor,
                                                secondaryColor = secondaryColor,
                                                comicAccent = comicAccent,
                                                isComic = isComic,
                                                isDark = isDarkTheme,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        BeatGlow(
                                            player = player,
                                            isPlaying = state.isPlaying,
                                            color = primaryColor,
                                            modifier = Modifier.requiredSize(sleeveSide * 1.65f),
                                        )
                                        DirectionalTrackContent(
                                            trackKey = trackKey,
                                            track = track,
                                            stepDirection = stepDirection,
                                            clip = false,
                                            label = "lsArtworkSlide",
                                            modifier = Modifier.graphicsLayer {
                                                val drag = artworkDrag.value
                                                val w = size.width.coerceAtLeast(1f)
                                                val p = (drag / w).coerceIn(-1f, 1f)
                                                cameraDistance = 16f * this.density
                                                translationX = drag * 0.90f
                                                rotationY = (-p * 24f).coerceIn(-26f, 26f)
                                                val sc = lensReveal.value * (1f - abs(p) * 0.12f)
                                                scaleX = sc
                                                scaleY = sc
                                            },
                                        ) { activeTrack ->
                                            PlayerArtwork(
                                                track = activeTrack,
                                                modifier = Modifier
                                                    .size(sleeveSide)
                                                    .artworkFlightSleeve(LocalArtworkFlight.current)
                                                    .shadow(elevation = if (state.isPlaying) 24.dp else 12.dp, shape = sleeveShape),
                                                corner = 26.dp,
                                                canvas = sleeveCanvas,
                                                isPlaying = state.isPlaying,
                                                pausedForTransition = pausedForTransition,
                                            )
                                        }
                                    }

                                    if (!isComic) {
                                        val flightFlying = LocalArtworkFlight.current?.flying == true
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .width(sleeveSide)
                                                .height(22.dp)
                                                .clipToBounds()
                                                .graphicsLayer {
                                                    alpha = if (flightFlying) 0f else if (isDarkTheme) 0.22f else 0.14f
                                                    compositingStrategy = CompositingStrategy.Offscreen
                                                }
                                                .drawWithContent {
                                                    drawContent()
                                                    drawRect(
                                                        brush = Brush.verticalGradient(listOf(Color.White, Color.Transparent)),
                                                        blendMode = BlendMode.DstIn,
                                                    )
                                                },
                                            contentAlignment = Alignment.TopCenter,
                                        ) {
                                            PlayerArtwork(
                                                track = track,
                                                corner = 26.dp,
                                                modifier = Modifier
                                                    .requiredSize(sleeveSide)
                                                    .graphicsLayer { scaleY = -1f }
                                                    .blur(1.5.dp),
                                                isPlaying = false,
                                                pausedForTransition = true,
                                            )
                                        }
                                    }

                                    if (previewLines != null && lyricsPresence > 0.01f) {
                                        Spacer(Modifier.height(6.dp))
                                        NowPlayingLyricsPreview(
                                            lines = previewLines,
                                            progressState = progressState,
                                            onOpenLyrics = onOpenLyrics,
                                            compact = compactHeight,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(LYRIC_PREVIEW_HEIGHT)
                                                .graphicsLayer { alpha = lyricsPresence },
                                        )
                                    }
                                }
                            } else {
                                // Fullmode (edge-to-edge background artwork): keep clean open viewport & edge squircle flash
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                        .liquidStudioSquircleRipple(
                                            progress = { rippleProgress.value },
                                            primaryColor = primaryColor,
                                            secondaryColor = secondaryColor,
                                            comicAccent = comicAccent,
                                            isComic = isComic,
                                            isDark = isDarkTheme,
                                        ),
                                )
                                if (!isDistractionFree && previewLines != null) {
                                    NowPlayingLyricsPreview(
                                        lines = previewLines,
                                        progressState = progressState,
                                        onOpenLyrics = onOpenLyrics,
                                        compact = compactHeight,
                                        overArtwork = true,
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                            .height(LYRIC_PREVIEW_HEIGHT + 8.dp),
                                    )
                                }
                            }

                            SeekDirectionOverlay(
                                visible = seekOverlayDirection == SeekDirection.REWIND,
                                direction = SeekDirection.REWIND,
                                seconds = seekOverlaySeconds,
                                modifier = Modifier.align(Alignment.CenterStart),
                            )
                            SeekDirectionOverlay(
                                visible = seekOverlayDirection == SeekDirection.FORWARD,
                                direction = SeekDirection.FORWARD,
                                seconds = seekOverlaySeconds,
                                modifier = Modifier.align(Alignment.CenterEnd),
                            )

                            androidx.compose.animation.AnimatedVisibility(
                                visible = heartBurstVisible,
                                enter = fadeIn(tween(120)) + scaleIn(ExpressiveMotion.spatialSpring(), initialScale = 0.6f),
                                exit = fadeOut(tween(240)) + scaleOut(tween(220), targetScale = 1.2f),
                                modifier = Modifier.align(Alignment.Center),
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = PlayerPalette.shade.copy(alpha = 0.56f),
                                    modifier = Modifier.size(72.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Favorite,
                                            contentDescription = null,
                                            tint = LikedHeartRed,
                                            modifier = Modifier.size(36.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom synchronized control deck (tightly balanced for 1-screen phone density)
            AnimatedVisibility(
                visible = !isDistractionFree,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(180)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (ultraCompactHeight) 4.dp else 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AnimatedVisibility(
                        visible = !coverFlowQueueMode,
                        enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                        exit = fadeOut(tween(160)) + shrinkVertically(tween(200)),
                    ) {
                        Column {
                            LiquidStudioTitlePill(
                                track = track,
                                state = state,
                                isPlaying = state.isPlaying,
                                windowActive = windowActive,
                                isLiked = isLiked,
                                isCustomAudio = isCustomAudio,
                                stepDirection = stepDirection,
                                primaryColor = primaryColor,
                                onToggleLiked = onToggleLiked,
                                onOpenAudioControls = onOpenAudioControls,
                                onOpenSignalPath = onOpenSignalPath,
                                onOpenArtist = onOpenArtist,
                            )
                            Spacer(Modifier.height(deckGap))
                        }
                    }

                    LiquidStudioMorphSeekBar(
                        progressState = progressState,
                        isPlaying = state.isPlaying,
                        trackKey = trackKey,
                        fallbackDurationMs = track.durationMs ?: state.durationMs,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        onSeek = player::seekTo,
                        slim = coverFlowQueueMode,
                        qualityBadge = qualityBadge,
                        onOpenSignalPath = onOpenSignalPath,
                        abLoopStartMs = state.abLoopStartMs,
                        abLoopEndMs = state.abLoopEndMs,
                        onCycleAbLoop = player::cycleAbLoop,
                    )

                    Spacer(Modifier.height(deckGap))

                    AnimatedContent(
                        targetState = coverFlowQueueMode,
                        transitionSpec = {
                            (fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                                scaleIn(tween(260, easing = FastOutSlowInEasing), initialScale = 0.90f)) togetherWith
                                (fadeOut(tween(180)) +
                                    scaleOut(tween(200), targetScale = 0.90f))
                        },
                        label = "lsControlsVsWheel",
                    ) { wheelMode ->
                        if (wheelMode) {
                            LiquidStudioClickWheel(
                                wheelSize = if (ultraCompactHeight) 128.dp else if (compactHeight) 144.dp else 158.dp,
                                queueSize = state.queue.size,
                                previewIndex = previewIndex,
                                currentIndex = state.currentIndex,
                                isPlaying = state.isPlaying,
                                primaryColor = primaryColor,
                                onStepPreview = { delta ->
                                    if (state.queue.isNotEmpty()) {
                                        previewIndex = (previewIndex + delta).coerceIn(0, state.queue.lastIndex)
                                    }
                                },
                                onCommitCenter = {
                                    if (previewIndex != state.currentIndex && previewIndex in state.queue.indices) {
                                        player.seekToQueueItem(previewIndex)
                                    } else {
                                        player.togglePlayPause()
                                    }
                                },
                                onTogglePlayPause = player::togglePlayPause,
                                onOpenVerticalQueue = {
                                    coverFlowQueueMode = false
                                    onOpenQueue()
                                },
                            )
                        } else {
                            LiquidStudioTransportRow(
                                state = state,
                                player = player,
                                compact = compactHeight,
                                primaryColor = primaryColor,
                            )
                        }
                    }

                    Spacer(Modifier.height(deckGap))

                    LiquidStudioBottomDock(
                        hasLyrics = hasLyrics,
                        coverFlowActive = coverFlowQueueMode,
                        isEqEnabled = isEqEnabled,
                        isVideoActive = isVideoActive,
                        onOpenLyrics = onOpenLyrics,
                        onOpenOutputPicker = onOpenOutputPicker,
                        onOpenEqualizer = onOpenEqualizer,
                        onToggleCoverFlow = {
                            if (state.queue.isNotEmpty()) {
                                previewIndex = state.currentIndex.coerceIn(0, state.queue.lastIndex)
                                coverFlowQueueMode = !coverFlowQueueMode
                            } else {
                                onOpenQueue()
                            }
                        },
                        onOpenVerticalQueue = {
                            coverFlowQueueMode = false
                            onOpenQueue()
                        },
                        onToggleVideo = onToggleVideo,
                        downloadState = downloadState,
                        onDownload = onDownload,
                        primaryColor = primaryColor,
                    )
                }
            }
        }
    }
}
