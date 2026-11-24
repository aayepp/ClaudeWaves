@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.lastwave.app.ui.player

import com.lastwave.app.ui.common.AnimatedToggleIcon

import androidx.compose.foundation.layout.offset
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.material3.minimumInteractiveComponentSize
import com.lastwave.app.data.lossless.LosslessListTier
import com.lastwave.app.ui.common.LocalBadgeOnDarkBackdrop
import com.lastwave.app.ui.common.QualityBadge
import com.lastwave.app.ui.common.QualityTierBadge
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.produceState
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import com.lastwave.app.playback.mediaIdKey
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import com.lastwave.app.ui.common.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.lastwave.app.R
import com.lastwave.app.data.canvas.CanvasArtwork
import com.lastwave.app.playback.AudioQualityTier
import com.lastwave.app.playback.InternalDirectAvailability
import com.lastwave.app.playback.InternalRoute
import com.lastwave.app.playback.MusicPlayer
import com.lastwave.app.playback.MusicPlayerState
import com.lastwave.app.playback.PlayableTrack
import com.lastwave.app.playback.PlaybackProgressState
import com.lastwave.app.playback.SignalPathReport
import com.lastwave.app.playback.UsbDacMonitor
import com.lastwave.app.playback.automix.api.BlendPhase
import com.lastwave.app.playback.automix.api.DeckBlendTelemetry
import com.lastwave.app.playback.formatSampleRateKHz
import com.lastwave.app.playback.isSpatialAudioCodec
import com.lastwave.app.playback.resolveAudioQualityTier
import com.lastwave.app.ui.comic.Button
import com.lastwave.app.ui.comic.OutlinedButton
import com.lastwave.app.ui.comic.Surface
import com.lastwave.app.ui.comic.clip
import com.lastwave.app.ui.common.DirectionalTrackContent
import com.lastwave.app.ui.common.ExpressiveInlineLoadingIndicator
import com.lastwave.app.ui.common.ExpressiveMotion
import com.lastwave.app.ui.common.playingMarquee
import com.lastwave.app.ui.common.rememberTrackStepDirection
import com.lastwave.app.ui.theme.ComicCyan
import com.lastwave.app.ui.theme.ComicInk
import com.lastwave.app.ui.theme.ComicMint
import com.lastwave.app.ui.theme.ComicPink
import com.lastwave.app.ui.theme.ComicAccent
import com.lastwave.app.ui.theme.ComicGold
import com.lastwave.app.ui.theme.LiquidGlassPreset
import com.lastwave.app.ui.theme.LiquidGlassSurface
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.LocalLiquidGlass
import com.lastwave.app.ui.theme.LocalLiquidGlassBackdrop
import com.lastwave.app.ui.theme.PlayerPalette
import com.lastwave.app.ui.theme.comicNightWash
import com.lastwave.app.ui.theme.isLiquidGlassBackdropSupported
import com.lastwave.app.ui.theme.liquidGlassChrome
import com.lastwave.app.ui.theme.liquidGlassContainerColor
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import com.lastwave.app.ui.comic.clipOnly
import com.lastwave.app.ui.shell.liquidBubbleHighlight
import com.lastwave.app.ui.shell.liquidNavigationIndex
import com.lastwave.app.ui.shell.liquidNavigationRawPosition
import com.lastwave.app.ui.shell.liquidNavigationRubberBand
import com.lastwave.app.ui.shell.liquidNavigationVisualIndex
import com.lastwave.app.ui.theme.GlassRecipe
import com.lastwave.app.ui.theme.LiquidGlassIntensity
import com.lastwave.app.ui.theme.glassRim
import com.lastwave.app.ui.theme.waterDropJiggle
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlin.math.abs
import kotlin.math.roundToInt
import com.lastwave.app.util.ArtistHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Full-bleed edge-to-edge background for Now Playing, Lyrics, and Queue tabs.
 * On Now Playing, renders the real track artwork (and animated Canvas when active)
 * scaled edge-to-edge with [ContentScale.Crop], plus an adaptive theme readability scrim.
 */
@Composable
internal fun BoxScope.FullScreenArtworkBackground(
    track: PlayableTrack,
    isPlaying: Boolean,
    currentTab: FullPlayerTab,
    isVideoActive: Boolean,
    showFullBleed: Boolean,
    rotatingBackgroundEnabled: Boolean,
    lyricsRotatingBackgroundEnabled: Boolean,
    lyricsState: LyricsUiState,
    progressState: StateFlow<PlaybackProgressState>,
    lyricsOffsetMs: Long,
    resolvedAmbientUrl: String?,
    activeCanvas: CanvasArtwork?,
    canvasCover: Float,
    onCanvasCoverChanged: (Float) -> Unit,
    onCanvasAspectChanged: (Float) -> Unit,
    pausedForTransition: Boolean,
    ambientColor: Color,
    ambientCompanion: Color,
    ambientDeep: Color,
    bgWidth: Float,
    bgHeight: Float,
    bgMaxDimension: Float,
    lyricsVeil: Float,
    comicPageOverAll: Float,
    readabilityAlpha: Float,
    /** Lyrics keep the cover colour blend (light halftone only), like Now Playing. */
    lyricsOnArtworkColors: Boolean = false,
) {
    val isLyricsTab = currentTab == FullPlayerTab.LYRICS
    val showLyricsFluid = lyricsRotatingBackgroundEnabled && rotatingBackgroundEnabled
    val isNowPlaying = currentTab == FullPlayerTab.NOW_PLAYING
    val heroAlpha by animateFloatAsState(
        targetValue = if (showFullBleed && isNowPlaying) 1f else 0f,
        animationSpec = tween(240, easing = FastOutSlowInEasing),
        label = "fullBleedArtworkAlpha",
    )
    val heroCover = if (canvasCover >= 0.95f) 0f else (1f - canvasCover).coerceIn(0f, 1f)
    // Skip the expensive blurred backdrop only while a full-screen Canvas video covers it;
    // the square cover alone fills just the top of the screen.
    val hiddenBehindFullBleed = showFullBleed && isNowPlaying && heroAlpha >= 0.99f && canvasCover >= 0.99f

    Box(
        Modifier
            .matchParentSize()
            .comicNightWash { if (hiddenBehindFullBleed) 0f else comicPageOverAll },
    ) {
        if (!hiddenBehindFullBleed) {
            FluidArtworkBackground(
                track = track,
                isPlaying = isPlaying,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = (1f - lyricsVeil).coerceIn(0f, 1f) },
                extraBlur = isLyricsTab,
                rotatingBackgroundEnabled = if (isLyricsTab) showLyricsFluid else rotatingBackgroundEnabled,
                lyricsRotatingBackgroundEnabled = lyricsRotatingBackgroundEnabled,
                fallback = {
                    val staticBlurTransform = remember(currentTab) {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                            listOf(
                                BlurTransformation(
                                    radius = if (currentTab == FullPlayerTab.LYRICS) 25 else 18,
                                    maxDimension = 100,
                                )
                            )
                        } else {
                            emptyList()
                        }
                    }
                    PlayerArtwork(
                        track = track,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 1.35f
                                scaleY = 1.35f
                                alpha = 0.9f
                            }
                            .then(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Modifier.blur(if (currentTab == FullPlayerTab.LYRICS) 36.dp else 20.dp)
                                } else {
                                    Modifier
                                }
                            ),
                        corner = 0.dp,
                        decodeSizePx = 200,
                        transformations = staticBlurTransform,
                    )
                    Box(Modifier.fillMaxSize().background(PlayerPalette.shade.copy(alpha = if (isLyricsTab) 0.40f else 0.52f)))
                    AmbientGlowLayers(ambientColor, ambientCompanion, ambientDeep, bgWidth, bgHeight, bgMaxDimension)
                },
            )
        }
        if (showLyricsFluid && !hiddenBehindFullBleed) {
            LyricsDynamicAmbientBackdrop(
                isLyricsTab = isLyricsTab,
                lyricsState = lyricsState,
                progressState = progressState,
                lyricsOffsetMs = lyricsOffsetMs,
                ambientColor = ambientColor,
                ambientCompanion = ambientCompanion,
                ambientDeep = ambientDeep,
                bgWidth = bgWidth,
                bgHeight = bgHeight,
                bgMaxDimension = bgMaxDimension,
            )
        }

        if (!hiddenBehindFullBleed) {
            // Comic halftone wash subtly sits beneath the full-bleed cover on Now Playing and full strength off-tab.
            Box(Modifier.fillMaxSize().comicNightWash { if (isNowPlaying || lyricsOnArtworkColors) 0.14f else 1f })
        }

        if (showFullBleed) {
            StaticArtworkHero(
                name = track.title,
                artist = track.artist,
                embeddedUrl = track.artworkUrl,
                resolvedUrl = resolvedAmbientUrl,
                alpha = heroAlpha * heroCover,
                mirrorExtend = true,
                modifier = Modifier.fillMaxSize(),
            )
            if (activeCanvas != null) {
                CanvasArtworkPlayer(
                    canvas = activeCanvas,
                    isPlaying = isPlaying,
                    contentMode = CanvasContentMode.CROP,
                    alignPortraitTop = true,
                    bottomFade = 0f,
                    presentationAlpha = { heroAlpha },
                    onCoverChanged = onCanvasCoverChanged,
                    onAspectRatioChanged = onCanvasAspectChanged,
                    pausedForTransition = pausedForTransition,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Adaptive readability scrim using existing theme shade token (black in dark/glass, paper in Comic Light).
        if (!isLyricsTab) {
            val queueWash by animateFloatAsState(
                targetValue = if (currentTab == FullPlayerTab.QUEUE) 1f else 0f,
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                label = "queueWash",
            )
            val scrimStrength = readabilityAlpha.coerceIn(0f, 1f)
            if (scrimStrength > 0.01f && queueWash < 0.99f) {
                val scrimBrush = if (showFullBleed && isNowPlaying) {
                    Brush.verticalGradient(
                        0.00f to PlayerPalette.shade.copy(alpha = 0.34f),
                        0.12f to PlayerPalette.shade.copy(alpha = 0.08f),
                        0.24f to Color.Transparent,
                        0.50f to Color.Transparent,
                        0.61f to PlayerPalette.shade.copy(alpha = 0.22f),
                        0.76f to PlayerPalette.shade.copy(alpha = 0.44f),
                        0.91f to PlayerPalette.shade.copy(alpha = 0.56f),
                        1.00f to PlayerPalette.shade.copy(alpha = 0.64f),
                    )
                } else {
                    Brush.verticalGradient(
                        0.00f to PlayerPalette.shade.copy(alpha = 0.62f),
                        0.16f to PlayerPalette.shade.copy(alpha = 0.24f),
                        0.32f to Color.Transparent,
                        0.46f to PlayerPalette.shade.copy(alpha = 0.22f),
                        0.60f to PlayerPalette.shade.copy(alpha = 0.68f),
                        0.78f to PlayerPalette.shade.copy(alpha = 0.90f),
                        1.00f to PlayerPalette.shade.copy(alpha = 0.96f),
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = scrimStrength * (1f - queueWash) }
                        .background(scrimBrush),
                )
            }
            if (queueWash > 0.01f) {
                // Uniform frosted wash over fluid artwork on Queue so light themes stay
                // consistently bright top-to-bottom instead of leaving dark cover holes.
                val darkBackdrop = PlayerPalette.shade.luminance() < 0.5f
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = queueWash }
                        .background(
                            if (darkBackdrop) {
                                Brush.verticalGradient(
                                    0.00f to PlayerPalette.shade.copy(alpha = 0.58f),
                                    0.50f to PlayerPalette.shade.copy(alpha = 0.64f),
                                    1.00f to PlayerPalette.shade.copy(alpha = 0.74f),
                                )
                            } else {
                                Brush.verticalGradient(
                                    0.00f to PlayerPalette.shade.copy(alpha = 0.84f),
                                    1.00f to PlayerPalette.shade.copy(alpha = 0.86f),
                                )
                            },
                        ),
                )
            }
        } else if (!isVideoActive) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.00f to PlayerPalette.shade.copy(alpha = 0.35f),
                            0.50f to PlayerPalette.shade.copy(alpha = 0.25f),
                            1.00f to PlayerPalette.shade.copy(alpha = 0.65f),
                        ),
                    ),
            )
        }

        if (lyricsVeil > 0.01f) {
            LyricsVeilBackdrop(
                track = track,
                ambientColor = ambientColor,
                ambientCompanion = ambientCompanion,
                ambientDeep = ambientDeep,
                lyricsVeil = lyricsVeil,
            )
        }
    }
}

@Composable
private fun AmbientGlowLayers(
    ambientColor: Color,
    ambientCompanion: Color,
    ambientDeep: Color,
    bgWidth: Float,
    bgHeight: Float,
    bgMaxDimension: Float,
) {
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                0f to ambientColor.copy(alpha = 0.58f),
                0.45f to ambientColor.copy(alpha = 0.22f),
                1f to Color.Transparent,
                center = Offset(bgWidth * 0.25f, bgHeight * 0.20f),
                radius = bgMaxDimension * 0.85f,
            ),
        ),
    )
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                0f to ambientCompanion.copy(alpha = 0.52f),
                0.50f to ambientCompanion.copy(alpha = 0.20f),
                1f to Color.Transparent,
                center = Offset(bgWidth * 0.88f, bgHeight * 0.65f),
                radius = bgMaxDimension * 0.78f,
            ),
        ),
    )
    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(
                0f to ambientDeep.copy(alpha = 0.42f),
                0.55f to ambientDeep.copy(alpha = 0.14f),
                1f to Color.Transparent,
                center = Offset(bgWidth * 0.15f, bgHeight * 0.82f),
                radius = bgMaxDimension * 0.70f,
            ),
        ),
    )
}

@Composable
private fun LyricsVeilBackdrop(
    track: PlayableTrack,
    ambientColor: Color,
    ambientCompanion: Color,
    ambientDeep: Color,
    lyricsVeil: Float,
) {
    val darkBackdrop = PlayerPalette.shade.luminance() < 0.5f
    val colors = remember(ambientColor, ambientCompanion, ambientDeep) {
        listOf(ambientColor, ambientCompanion, ambientDeep)
    }
    val appleMusicBackgroundBrush = remember(colors, darkBackdrop) {
        val stops = if (darkBackdrop) {
            listOf(
                colors.getOrElse(0) { AppleMusicFallbackGradient[0] }.copy(alpha = 0.82f),
                colors.getOrElse(1) { AppleMusicFallbackGradient[1] }.copy(alpha = 0.74f),
                colors.getOrElse(2) { AppleMusicFallbackGradient[2] }.copy(alpha = 0.92f),
            )
        } else {
            // Comic Light sets PlayerPalette.ink to dark ComicInk; keep the paper bright so text stays legible.
            listOf(
                colors.getOrElse(0) { PlayerPalette.shade }.copy(alpha = 0.24f),
                colors.getOrElse(1) { PlayerPalette.shade }.copy(alpha = 0.18f),
                colors.getOrElse(2) { PlayerPalette.shade }.copy(alpha = 0.28f),
            )
        }
        Brush.verticalGradient(stops)
    }
    val appleMusicBottomScrim = remember(darkBackdrop) {
        Brush.verticalGradient(
            listOf(Color.Transparent, PlayerPalette.shade.copy(alpha = if (darkBackdrop) 0.42f else 0.36f)),
        )
    }
    Box(Modifier.fillMaxSize().graphicsLayer { alpha = lyricsVeil }) {
        Box(Modifier.fillMaxSize().background(if (darkBackdrop) AppleMusicFallbackGradient.last() else PlayerPalette.shade))
        PlayerArtwork(
            track = track,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.35f
                    scaleY = 1.35f
                    alpha = if (darkBackdrop) 0.50f else 0.22f
                }
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(46.dp) else Modifier,
                ),
            corner = 0.dp,
            decodeSizePx = 200,
            transformations = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                listOf(BlurTransformation(radius = 25, maxDimension = 100))
            } else {
                emptyList()
            },
        )
        Box(Modifier.fillMaxSize().background(appleMusicBackgroundBrush))
        Box(Modifier.fillMaxSize().background(PlayerPalette.shade.copy(alpha = if (darkBackdrop) 0.44f else 0.72f)))
        Box(Modifier.fillMaxSize().background(appleMusicBottomScrim))
    }
}

@Composable
internal fun NowPlayingHeader(
    currentTab: FullPlayerTab,
    track: PlayableTrack,
    sourceLabel: String,
    isPlaying: Boolean,
    lyricsOffsetMs: Long,
    showLyricsOffsetAction: Boolean,
    horizontalPadding: Dp,
    onBackOrCollapse: () -> Unit,
    onOpenLyricsOffset: () -> Unit,
    onOpenTrackMenu: () -> Unit,
    modifier: Modifier = Modifier,
    minimalStyle: Boolean = false,
) {
    if (minimalStyle && currentTab == FullPlayerTab.NOW_PLAYING) {
        val origin = sourceLabel.takeIf { it.isNotBlank() } ?: "ClaudeWaves"
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = horizontalPadding),
        ) {
            IconButton(
                onClick = hapticClick(action = onBackOrCollapse),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = "Minimize player",
                    tint = PlayerPalette.ink.copy(alpha = 0.90f),
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = "Playing from $origin",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = PlayerPalette.ink.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 46.dp),
            )
            IconButton(
                onClick = hapticClick(action = onOpenTrackMenu),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Song options",
                    tint = PlayerPalette.ink.copy(alpha = 0.90f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        return
    }
    val isComic = LocalComicTheme.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = horizontalPadding),
    ) {
        LiquidGlassSurface(
            onClick = hapticClick(action = onBackOrCollapse),
            shape = CircleShape,
            color = liquidGlassContainerColor(PlayerPalette.ink.copy(alpha = 0.14f)),
            contentColor = PlayerPalette.ink.copy(alpha = 0.94f),
            glassModifier = Modifier.liquidGlassChrome(
                CircleShape,
                LocalLiquidGlass.current,
                LiquidGlassPreset.FloatingControls,
            ),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AnimatedToggleIcon(
                    on = currentTab != FullPlayerTab.NOW_PLAYING, onIcon = Icons.AutoMirrored.Filled.ArrowBack, offIcon = Icons.Filled.ExpandMore,
                    contentDescription = if (currentTab != FullPlayerTab.NOW_PLAYING) "Back to player" else "Minimize player",
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = if (showLyricsOffsetAction) 96.dp else 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = when (currentTab) {
                    FullPlayerTab.NOW_PLAYING -> "NOW PLAYING"
                    FullPlayerTab.LYRICS -> "LYRICS"
                    FullPlayerTab.QUEUE -> "PLAYING QUEUE"
                },
                style = MaterialTheme.typography.labelMedium,
                color = PlayerPalette.ink.copy(alpha = 0.95f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.9.sp,
                maxLines = 1,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (currentTab == FullPlayerTab.NOW_PLAYING) {
                    val dotColor = MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) dotColor else PlayerPalette.ink.copy(alpha = 0.55f)),
                    )
                }
                Text(
                    text = if (currentTab == FullPlayerTab.LYRICS) {
                        "${track.title} \u2022 ${track.artist}"
                    } else {
                        sourceLabel.takeIf { it.isNotBlank() } ?: "ClaudeWaves"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = PlayerPalette.ink.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showLyricsOffsetAction) {
                LiquidGlassSurface(
                    onClick = hapticClick(action = onOpenLyricsOffset),
                    shape = CircleShape,
                    color = if (lyricsOffsetMs != 0L) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        liquidGlassContainerColor(PlayerPalette.ink.copy(alpha = 0.14f))
                    },
                    contentColor = if (lyricsOffsetMs != 0L) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        PlayerPalette.ink.copy(alpha = 0.94f)
                    },
                    glassModifier = Modifier.liquidGlassChrome(
                        CircleShape,
                        LocalLiquidGlass.current,
                        LiquidGlassPreset.FloatingControls,
                    ),
                    modifier = Modifier
                        .size(40.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Timer,
                            contentDescription = stringResource(R.string.player_lyrics_offset_cd),
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
            LiquidGlassSurface(
                onClick = hapticClick(action = onOpenTrackMenu),
                shape = CircleShape,
                color = liquidGlassContainerColor(PlayerPalette.ink.copy(alpha = 0.14f)),
                contentColor = PlayerPalette.ink.copy(alpha = 0.94f),
                glassModifier = Modifier.liquidGlassChrome(
                    CircleShape,
                    LocalLiquidGlass.current,
                    LiquidGlassPreset.FloatingControls,
                ),
                modifier = Modifier
                    .size(40.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Song options",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun NowPlayingSongInfo(
    track: PlayableTrack,
    isPlaying: Boolean,
    windowActive: Boolean,
    isLiked: Boolean,
    isCustomAudio: Boolean,
    onToggleLiked: () -> Unit,
    onOpenAudioControls: () -> Unit,
    onOpenArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
    stepDirection: Int = 0,
) {
    val isComic = LocalComicTheme.current
    val infoTrackKey = remember(track.videoId, track.title, track.artist) {
        track.videoId?.takeIf(String::isNotBlank) ?: "${track.title}|${track.artist}"
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DirectionalTrackContent(
            trackKey = infoTrackKey,
            track = track,
            stepDirection = stepDirection,
            // Far enough to read as a slide with the artwork; at 0.16 it looked like a cut.
            slideFraction = 0.3f,
            modifier = Modifier.weight(1f),
            label = "nowPlayingInfoSlide",
        ) { activeTrack ->
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = activeTrack.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        letterSpacing = (-0.35).sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = PlayerPalette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .playingMarquee(isPlaying && windowActive),
                )
                Spacer(Modifier.height(3.dp))
                val splitArtists = remember(activeTrack.artist) { ArtistHelper.splitArtists(activeTrack.artist) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .playingMarquee(isPlaying && windowActive),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    splitArtists.forEachIndexed { index, artName ->
                        Text(
                            text = artName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = PlayerPalette.ink.copy(alpha = 0.78f),
                            maxLines = 1,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = hapticClick { onOpenArtist(artName) },
                            ),
                        )
                        if (index < splitArtists.lastIndex) {
                            Text(
                                text = ", ",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal,
                                ),
                                color = PlayerPalette.ink.copy(alpha = 0.52f),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerLikeButton(
                liked = isLiked,
                onClick = hapticClick(if (isLiked) HapticFeedbackType.ToggleOff else HapticFeedbackType.Confirm, onToggleLiked),
                size = 48.dp,
            )

            val audioInteraction = remember { MutableInteractionSource() }
            val isPressed by audioInteraction.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.88f else 1f,
                animationSpec = ExpressiveMotion.spatialFast(),
                label = "songInfoAudioScale",
            )
            LiquidGlassSurface(
                glassModifier = Modifier.liquidGlassChrome(
                    CircleShape,
                    LocalLiquidGlass.current,
                    LiquidGlassPreset.PlayerControls,
                    interactionSource = audioInteraction,
                ),
                onClick = hapticClick(HapticFeedbackType.ContextClick, onOpenAudioControls),
                interactionSource = audioInteraction,
                shape = CircleShape,
                color = when {
                    isCustomAudio && isComic -> ComicAccent
                    isCustomAudio -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f)
                    else -> liquidGlassContainerColor(PlayerPalette.ink.copy(alpha = 0.14f))
                },
                contentColor = when {
                    isCustomAudio && isComic -> ComicInk
                    isCustomAudio -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> PlayerPalette.ink.copy(alpha = 0.92f)
                },
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "Audio and speed controls",
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
        }
    }
}

private data class OutputRouteDisplay(
    val icon: ImageVector,
    val label: String,
    /** Comic Pop paints each kind of output in its own colour. */
    val comicColor: Color,
    val isHardwareVerified: Boolean = false,
)

private fun resolveAudioOutputDisplay(
    isCasting: Boolean,
    signalPath: SignalPathReport,
    usbDac: UsbDacMonitor.State,
    availability: InternalDirectAvailability,
    isTablet: Boolean,
): OutputRouteDisplay {
    val activeDac = usbDac.dac
    val speaker = if (isTablet) "Tablet Speaker" else "Phone Speaker"
    return when {
        isCasting -> OutputRouteDisplay(Icons.Filled.Cast, "Google Cast", ComicGold)
        signalPath.usbExclusiveActive || (activeDac != null && activeDac.hasUsbPeripheral && availability.route == InternalRoute.USB) -> {
            val base = signalPath.dacName?.takeIf { it.isNotBlank() }
                ?: availability.routeName.takeIf { it.isNotBlank() }
                ?: "USB DAC"
            val spec = if (signalPath.usbExclusiveActive && signalPath.outputRateHz > 0) {
                " (${signalPath.outputBits}-bit / ${formatSampleRateKHz(signalPath.outputRateHz / 1000.0)} kHz)"
            } else {
                ""
            }
            OutputRouteDisplay(OutputGlyphs.UsbDac, "$base$spec", ComicGold, isHardwareVerified = signalPath.bitPerfect)
        }
        availability.route == InternalRoute.BLUETOOTH -> OutputRouteDisplay(
            OutputGlyphs.Earbuds,
            availability.routeName.ifBlank { "Bluetooth" },
            ComicPink,
            isHardwareVerified = signalPath.directToHal,
        )
        availability.route == InternalRoute.WIRED -> OutputRouteDisplay(
            OutputGlyphs.Iem,
            availability.routeName.ifBlank { "Wired Headphones" },
            ComicMint,
            isHardwareVerified = signalPath.directToHal,
        )
        availability.route == InternalRoute.USB -> OutputRouteDisplay(
            OutputGlyphs.UsbDac,
            availability.routeName.ifBlank { "USB Audio" },
            ComicGold,
            isHardwareVerified = signalPath.directToHal,
        )
        availability.route == InternalRoute.SPEAKER -> OutputRouteDisplay(
            OutputGlyphs.PhoneSpeaker,
            speaker,
            ComicCyan,
            isHardwareVerified = signalPath.directToHal,
        )
        else -> OutputRouteDisplay(
            OutputGlyphs.PhoneSpeaker,
            availability.routeName
                .takeUnless { it.equals("remote-submix", ignoreCase = true) }
                ?.takeIf { it.isNotBlank() }
                ?: speaker,
            ComicCyan,
        )
    }
}

/** Lossy streams get a plain codec badge ("AAC", "OPUS"); null when nothing is known yet. */
internal fun lossyCodecLabel(state: MusicPlayerState): String? {
    val codec = state.audioCodec?.uppercase()?.replace(Regex("""\s*\d+\s*K(?:BPS)?"""), "")?.trim()
    return codec?.takeIf { it.isNotBlank() && it != "AUDIO" }
        ?: state.bitrateKbps?.let { "$it KBPS" }
}

@Composable
internal fun AudioInformationRow(
    state: MusicPlayerState,
    signalPath: SignalPathReport,
    usbDac: UsbDacMonitor.State,
    availability: InternalDirectAvailability,
    isTablet: Boolean,
    onOpenSignalPath: () -> Unit,
    onOpenOutputPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val qualityTier = remember(state.audioCodec, state.isLossless, state.samplingRateKHz, state.bitDepth) {
        resolveAudioQualityTier(state.audioCodec, state.isLossless, state.samplingRateKHz, state.bitDepth)
    }
    val outputDisplay = remember(state.isCasting, signalPath, usbDac.dac, availability, isTablet) {
        resolveAudioOutputDisplay(state.isCasting, signalPath, usbDac, availability, isTablet)
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        OutputRouteButton(outputDisplay, onOpenOutputPicker)
        Box(contentAlignment = Alignment.Center) {
            NowPlayingQualityBadge(state, qualityTier, onOpenSignalPath)
            TierUpgradeBurst(
                rememberTierUpgradeBurst(state.current?.let { it.videoId ?: "${it.title}|${it.artist}" }, qualityTier),
            )
        }
    }
}

/** Icon-only output button; the device name stays in the sheet and for TalkBack. */
@Composable
private fun OutputRouteButton(display: OutputRouteDisplay, onClick: () -> Unit) {
    val isComic = LocalComicTheme.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = ExpressiveMotion.spatialFast(),
        label = "outputRouteScale",
    )
    LiquidGlassSurface(
        glassModifier = Modifier.liquidGlassChrome(
            CircleShape,
            LocalLiquidGlass.current,
            LiquidGlassPreset.PlayerControls,
            interactionSource = interaction,
        ),
        onClick = hapticClick(HapticFeedbackType.ContextClick, onClick),
        interactionSource = interaction,
        shape = CircleShape,
        color = when {
            isComic -> display.comicColor
            isDark -> liquidGlassContainerColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            else -> MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = when {
            isComic -> ComicInk
            isDark -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSecondaryContainer
        },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = display.icon,
                contentDescription = "Audio output: ${display.label}",
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

/**
 * The song-list quality badge at Now Playing size. A mix in progress shows its
 * own neutral badge; a quality switch keeps the current badge and pulses it.
 */
@Composable
private fun NowPlayingQualityBadge(state: MusicPlayerState, tier: AudioQualityTier, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val neutralContent = PlayerPalette.ink.copy(alpha = 0.82f)
    val neutralContainer = PlayerPalette.ink.copy(alpha = 0.10f)
    val mixing = state.deckBlendTelemetry.isBlendingActive || state.deckBlendTelemetry.phase == BlendPhase.MIX_COMPLETED
    val badge: @Composable (Modifier) -> Unit = when {
        mixing -> { m -> QualityBadge("AUTOMIX", QualityGlyphs.AutoMix, neutralContent, neutralContainer, m, large = true) }
        state.isCrossfading -> { m -> QualityBadge("CROSSFADE", QualityGlyphs.Crossfade, neutralContent, neutralContainer, m, large = true) }
        isSpatialAudioCodec(state.audioCodec) -> { m -> QualityTierBadge(LosslessListTier.ATMOS, m, large = true) }
        tier == AudioQualityTier.HI_RES -> { m -> QualityTierBadge(LosslessListTier.HI_RES, m, large = true) }
        tier == AudioQualityTier.LOSSLESS -> { m -> QualityTierBadge(LosslessListTier.LOSSLESS, m, large = true) }
        else -> {
            val label = lossyCodecLabel(state) ?: return
            { m -> QualityBadge(label, QualityGlyphs.Standard, neutralContent, neutralContainer, m, large = true) }
        }
    }
    val switching = state.qualitySwitch != null
    val pulse = if (switching) {
        rememberInfiniteTransition(label = "qualitySwitchPulse").animateFloat(
            initialValue = 1f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
            label = "qualitySwitchAlpha",
        ).value
    } else {
        1f
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClickLabel = "Signal path",
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
            ),
    ) {
        badge(Modifier.graphicsLayer { alpha = pulse })
    }
}

@Composable
internal fun PlaybackSeekBar(
    progressState: StateFlow<PlaybackProgressState>,
    isPlaying: Boolean,
    trackKey: String?,
    wavyEnabled: Boolean,
    onSeek: (Long) -> Unit,
    isTranslucent: Boolean,
    fallbackDurationMs: Long,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    telemetry: DeckBlendTelemetry,
    modifier: Modifier = Modifier,
    /** Where the song's best part starts; drawn as a tappable dot on the track. */
    bestPartMs: Long? = null,
    abLoopStartMs: Long? = null,
    abLoopEndMs: Long? = null,
    onCycleAbLoop: (() -> Unit)? = null,
    onClearAbLoop: (() -> Unit)? = null,
    minimalStyle: Boolean = false,
    centerLabel: (@Composable () -> Unit)? = null,
) {
    // Duration only: collecting progress itself would recompose on every 60 ms tick.
    val durationMs by remember(progressState) {
        progressState.map { it.durationMs }.distinctUntilChanged()
    }.collectAsState(initial = 0L)
    val totalMs = durationMs.takeIf { it > 0L } ?: fallbackDurationMs
    val showBestPart = bestPartMs != null && totalMs > 0L && bestPartMs < totalMs
    // One pulse when playback runs into the best part. Collected, not read in
    // composition, so the 60 ms ticker never recomposes the bar; a seek far past it
    // (the jump of more than a few seconds) is not "arriving" and stays quiet.
    val pulse = remember(trackKey) { Animatable(0f) }
    LaunchedEffect(trackKey, bestPartMs) {
        val target = bestPartMs ?: return@LaunchedEffect
        var last = -1L
        progressState.collect { s ->
            val arrived = last in 0 until target && s.positionMs >= target && s.positionMs - last < 3_000L
            last = s.positionMs
            if (arrived) {
                pulse.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
                pulse.animateTo(0f, tween(900, easing = FastOutSlowInEasing))
            }
        }
    }
    if (minimalStyle) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showBestPart) {
                MinimalBestPartWhisper(
                    onClick = { onSeek(bestPartMs!!) },
                    pulse = { pulse.value },
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }
            SeekBar(
                progressState = progressState,
                isPlaying = isPlaying,
                trackKey = trackKey,
                wavyEnabled = wavyEnabled,
                onSeek = onSeek,
                isTranslucent = isTranslucent,
                fallbackDurationMs = fallbackDurationMs,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                tertiaryColor = tertiaryColor,
                telemetry = telemetry,
                abLoopStartMs = abLoopStartMs,
                abLoopEndMs = abLoopEndMs,
                onCycleAbLoop = onCycleAbLoop,
                onClearAbLoop = onClearAbLoop,
                minimalStyle = true,
                centerLabel = centerLabel,
            )
        }
    } else {
        Box(modifier = modifier.fillMaxWidth()) {
            SeekBar(
                progressState = progressState,
                isPlaying = isPlaying,
                trackKey = trackKey,
                wavyEnabled = wavyEnabled,
                onSeek = onSeek,
                isTranslucent = isTranslucent,
                fallbackDurationMs = fallbackDurationMs,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                tertiaryColor = tertiaryColor,
                telemetry = telemetry,
                abLoopStartMs = abLoopStartMs,
                abLoopEndMs = abLoopEndMs,
                onCycleAbLoop = onCycleAbLoop,
                onClearAbLoop = onClearAbLoop,
                minimalStyle = false,
                centerLabel = centerLabel,
            )
            if (showBestPart) {
                BestPartPill(
                    border = Brush.sweepGradient(listOf(primaryColor, secondaryColor, tertiaryColor, primaryColor)),
                    onClick = { onSeek(bestPartMs!!) },
                    pulse = { pulse.value },
                    // Above the track in the bar's empty headroom: under it, between the time
                    // labels, it crowded the controls.
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = (-12).dp).zIndex(1f),
                )
            }
        }
    }
}

/**
 * Quiet editorial "Best part" action chip for Minimal Design: frosted hairline pill
 * with press scale + ripple, flame icon, label (no timestamp numbers), and chevron
 * so users recognize it immediately as a clickable control.
 */
@Composable
private fun MinimalBestPartWhisper(
    onClick: () -> Unit,
    pulse: () -> Float,
    modifier: Modifier = Modifier,
) {
    val isComic = LocalComicTheme.current
    val isDark = PlayerPalette.shade.luminance() < 0.5f
    val accentColor = when {
        isComic -> MaterialTheme.colorScheme.onSecondaryContainer
        isDark -> Color(0xFFFFD60A).copy(alpha = 0.92f)
        else -> Color(0xFF8A6000)
    }
    val pillBackground = when {
        isComic -> MaterialTheme.colorScheme.secondaryContainer
        isDark -> PlayerPalette.ink.copy(alpha = 0.09f)
        else -> PlayerPalette.ink.copy(alpha = 0.07f)
    }
    val pillBorder = when {
        isComic -> ComicInk
        isDark -> PlayerPalette.ink.copy(alpha = 0.20f)
        else -> PlayerPalette.ink.copy(alpha = 0.18f)
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = ExpressiveMotion.spatialFast(),
        label = "minBestPartScale",
    )
    Row(
        modifier = modifier
            .graphicsLayer {
                val s = (1f + 0.08f * pulse()) * pressScale
                scaleX = s
                scaleY = s
            }
            .clip(CircleShape)
            .background(pillBackground)
            .border(if (isComic) 1.5.dp else 0.75.dp, pillBorder, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(),
                role = androidx.compose.ui.semantics.Role.Button,
                onClick = hapticClick(HapticFeedbackType.ContextClick, onClick),
            )
            .padding(horizontal = 9.dp, vertical = 3.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Outlined.LocalFireDepartment,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = stringResource(R.string.player_best_part),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
            ),
            color = accentColor,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = accentColor.copy(alpha = 0.78f),
            modifier = Modifier.size(13.dp),
        )
    }
}

/** Best-part start of [track], looked up once per song (chorus, else replay peak). */
@Composable
internal fun rememberBestPartMs(player: com.lastwave.app.playback.MusicPlayer, track: com.lastwave.app.playback.PlayableTrack): Long? =
    produceState<Long?>(null, track.mediaIdKey()) { value = player.findBestPartMs(track) }.value

/**
 * "Best part" pill above the seek bar: jumps to the chorus listeners replay most.
 * [pulse] (0..1, read at draw time) swells it and lights its rim as the part arrives.
 */
@Composable
private fun BestPartPill(border: Brush, onClick: () -> Unit, pulse: () -> Float, modifier: Modifier = Modifier) {
    val ink = PlayerPalette.ink
    Row(
        modifier = modifier
            .graphicsLayer {
                val s = 1f + 0.1f * pulse()
                scaleX = s
                scaleY = s
            }
            .drawBehind {
                val p = pulse()
                if (p > 0f) {
                    // Soft halo in the rim's colours, spreading past the pill as it glows.
                    val grow = 6.dp.toPx() * p
                    drawRoundRect(
                        brush = border,
                        topLeft = Offset(-grow, -grow),
                        size = Size(size.width + grow * 2, size.height + grow * 2),
                        cornerRadius = CornerRadius(size.height / 2f + grow),
                        alpha = 0.45f * p,
                    )
                }
            }
            .clip(CircleShape)
            .border(1.dp, border, CircleShape)
            .background(PlayerPalette.shade.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            androidx.compose.material.icons.Icons.Outlined.LocalFireDepartment,
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(14.dp),
        )
        Text(stringResource(R.string.player_best_part), style = MaterialTheme.typography.labelMedium, color = ink)
    }
}

/**
 * Compact audio visualizer between seek bar and main controls.
 * Strictly renders only when real [levels] data is available; omitted with 0dp height otherwise.
 */
@Composable
internal fun AudioVisualizer(
    levels: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = PlayerPalette.ink.copy(alpha = 0.85f),
) {
    if (levels.isEmpty()) return
    Canvas(
        modifier = modifier
            .fillMaxWidth(0.55f)
            .height(22.dp),
    ) {
        val count = levels.size.coerceAtLeast(1)
        val gap = 3.dp.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2.dp.toPx())
        val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
        levels.forEachIndexed { index, raw ->
            val level = raw.coerceIn(0.08f, 1f)
            val barHeight = (size.height * level).coerceAtLeast(3.dp.toPx())
            val x = index * (barWidth + gap)
            val y = (size.height - barHeight) / 2f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = radius,
            )
        }
    }
}

@Composable
internal fun MainPlaybackControls(
    state: MusicPlayerState,
    player: MusicPlayer,
    isTranslucent: Boolean = true,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isComic = LocalComicTheme.current
    val prevInteraction = remember { MutableInteractionSource() }
    val playInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val isPrevPressed by prevInteraction.collectIsPressedAsState()
    val isPlayPressed by playInteraction.collectIsPressedAsState()
    val isNextPressed by nextInteraction.collectIsPressedAsState()
    val prevScale by animateFloatAsState(if (isPrevPressed) 0.86f else 1f, ExpressiveMotion.spatialSpring(), label = "npPrevScale")
    val playScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "npPlayScale",
    )
    val nextScale by animateFloatAsState(if (isNextPressed) 0.86f else 1f, ExpressiveMotion.spatialSpring(), label = "npNextScale")

    // Seven controls in one row (shuffle, prev, −15, play, +15, next, repeat):
    // sized so the minimum still fits a 320dp-wide phone column.
    val playSize = if (compact) 58.dp else 66.dp
    val skipSize = 48.dp
    val sideSize = 40.dp
    val isGlassActive = LocalLiquidGlass.current && isLiquidGlassBackdropSupported() && LocalLiquidGlassBackdrop.current != null

    val shuffleTint by animateColorAsState(
        targetValue = when {
            state.shuffleEnabled -> MaterialTheme.colorScheme.primary
            else -> PlayerPalette.ink.copy(alpha = 0.78f)
        },
        animationSpec = tween(180),
        label = "npShuffleTint",
    )
    // Press motion that says what the button did. Shuffle and repeat react to the state,
    // not the tap, so a change from the notification or Android Auto animates too.
    val motionScope = rememberCoroutineScope()
    val prevNudge = remember { Animatable(0f) }
    val nextNudge = remember { Animatable(0f) }
    val shuffleFlip = remember { Animatable(1f) }
    var lastShuffle by remember { mutableStateOf(state.shuffleEnabled) }
    LaunchedEffect(state.shuffleEnabled) {
        if (state.shuffleEnabled == lastShuffle) return@LaunchedEffect
        lastShuffle = state.shuffleEnabled
        shuffleFlip.snapTo(0f)
        shuffleFlip.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    var repeatSpins by remember { mutableIntStateOf(0) }
    var lastRepeat by remember { mutableIntStateOf(state.repeatMode) }
    val repeatPop = remember { Animatable(0f) }
    LaunchedEffect(state.repeatMode) {
        if (state.repeatMode == lastRepeat) return@LaunchedEffect
        lastRepeat = state.repeatMode
        repeatSpins++
        if (state.repeatMode == Player.REPEAT_MODE_ONE) repeatPop.kick()
    }
    val repeatRotation by animateFloatAsState(
        repeatSpins * 360f,
        spring(dampingRatio = 0.6f, stiffness = 180f),
        label = "npRepeatSpin",
    )

    val repeatActive = state.repeatMode != Player.REPEAT_MODE_OFF
    val repeatTint by animateColorAsState(
        targetValue = when {
            repeatActive -> MaterialTheme.colorScheme.primary
            else -> PlayerPalette.ink.copy(alpha = 0.78f)
        },
        animationSpec = tween(180),
        label = "npRepeatTint",
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = hapticClick(if (state.shuffleEnabled) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn, player::toggleShuffle),
            modifier = Modifier
                .size(sideSize)
                .semantics {
                    selected = state.shuffleEnabled
                    stateDescription = if (state.shuffleEnabled) "Shuffle on" else "Shuffle off"
                },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = if (state.shuffleEnabled) "Shuffle on" else "Shuffle off",
                    tint = shuffleTint,
                    modifier = Modifier.size(22.dp).graphicsLayer {
                        // The arrows cross over once: mirrored halfway, then back.
                        val v = shuffleFlip.value
                        scaleX = kotlin.math.cos(2.0 * Math.PI * v).toFloat()
                        translationY = -3.dp.toPx() * kotlin.math.sin(Math.PI * v).toFloat()
                    },
                )
                if (state.shuffleEnabled) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 5.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(shuffleTint),
                    )
                }
            }
        }

        LiquidGlassSurface(
            glassModifier = Modifier.liquidGlassChrome(CircleShape, LocalLiquidGlass.current, LiquidGlassPreset.PlayerControls, interactionSource = prevInteraction),
            onClick = hapticClick(action = { player.previous(); motionScope.launch { prevNudge.kick() } }),
            interactionSource = prevInteraction,
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = PlayerPalette.ink.copy(alpha = 0.96f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .size(skipSize)
                .graphicsLayer {
                    scaleX = prevScale
                    scaleY = prevScale
                },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    modifier = Modifier.size(if (compact) 30.dp else 34.dp).graphicsLayer {
                        translationX = -7.dp.toPx() * prevNudge.value
                    },
                )
            }
        }

        SeekStepButton(player = player, forward = false, size = sideSize)

        LiquidGlassSurface(
            glassModifier = Modifier.liquidGlassChrome(CircleShape, LocalLiquidGlass.current, LiquidGlassPreset.FloatingControls, interactionSource = playInteraction),
            onClick = hapticClick(if (state.isPlaying) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn, player::togglePlayPause),
            interactionSource = playInteraction,
            shape = CircleShape,
            color = when {
                isComic -> ComicAccent
                isGlassActive -> liquidGlassContainerColor(PlayerPalette.ink.copy(alpha = 0.20f))
                isTranslucent -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.primary
            },
            contentColor = when {
                isComic -> ComicInk
                isGlassActive -> PlayerPalette.ink
                else -> MaterialTheme.colorScheme.onPrimary
            },
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            // A wide pill rather than a circle: the main action reads first and is
            // easier to hit. Shrinks back towards a circle on narrow phones.
            modifier = Modifier
                .weight(1f, fill = false)
                .widthIn(min = playSize, max = playSize * 1.6f)
                .fillMaxWidth()
                .height(playSize)
                .graphicsLayer {
                    scaleX = playScale
                    scaleY = playScale
                },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.isBuffering) {
                    ExpressiveInlineLoadingIndicator(
                        size = if (compact) 26.dp else 30.dp,
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
        }

        SeekStepButton(player = player, forward = true, size = sideSize)

        LiquidGlassSurface(
            glassModifier = Modifier.liquidGlassChrome(CircleShape, LocalLiquidGlass.current, LiquidGlassPreset.PlayerControls, interactionSource = nextInteraction),
            onClick = hapticClick(action = { player.next(); motionScope.launch { nextNudge.kick() } }),
            interactionSource = nextInteraction,
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = PlayerPalette.ink.copy(alpha = 0.96f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .size(skipSize)
                .graphicsLayer {
                    scaleX = nextScale
                    scaleY = nextScale
                },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    modifier = Modifier.size(if (compact) 30.dp else 34.dp).graphicsLayer {
                        translationX = 7.dp.toPx() * nextNudge.value
                    },
                )
            }
        }

        IconButton(
            onClick = hapticClick(
                if (state.repeatMode == Player.REPEAT_MODE_ONE) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
                player::cycleRepeatMode,
            ),
            modifier = Modifier
                .size(sideSize)
                .semantics {
                    selected = repeatActive
                    stateDescription = when (state.repeatMode) {
                        Player.REPEAT_MODE_ONE -> "Repeat one"
                        Player.REPEAT_MODE_ALL -> "Repeat all"
                        else -> "Repeat off"
                    }
                },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = when (state.repeatMode) {
                        Player.REPEAT_MODE_ONE -> "Repeat one"
                        Player.REPEAT_MODE_ALL -> "Repeat all"
                        else -> "Repeat off"
                    },
                    tint = repeatTint,
                    modifier = Modifier.size(22.dp).graphicsLayer {
                        rotationZ = repeatRotation
                        val s = 1f + 0.25f * repeatPop.value
                        scaleX = s
                        scaleY = s
                    },
                )
                if (repeatActive) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 5.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(repeatTint),
                    )
                }
            }
        }
    }
}

private data class DockActionItem(
    val key: String,
    val icon: ImageVector,
    val label: String,
    val showBadgeDot: Boolean,
    val comicAccent: Color,
    val isHighlighted: Boolean = false,
    val isLoading: Boolean = false,
    val progressPercent: Int = 0,
    val succeeded: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
internal fun SecondaryActionRow(
    hasLyrics: Boolean,
    compact: Boolean = false,
    useDraggableDock: Boolean = true,
    onShowLyrics: () -> Unit,
    onShowQueue: () -> Unit,
    modifier: Modifier = Modifier,
    /** Swaps the cover for the synced music video; null hides the slot (no video). */
    onToggleVideo: (() -> Unit)? = null,
    isVideoActive: Boolean = false,
    downloadState: NowPlayingDownloadState = NowPlayingDownloadState(),
    /**
     * One-tap track download while streaming; null hides the slot (disabled in Settings or local tracks).
     * Once saved it becomes a one-time Library shortcut, then hides and the remaining slots center cleanly.
     */
    onDownload: (() -> Unit)? = null,
) {
    val isDownloaded = downloadState.status == com.lastwave.app.ui.common.TrackDownloadStatus.DOWNLOADED
    val isDownloading = downloadState.status == com.lastwave.app.ui.common.TrackDownloadStatus.DOWNLOADING
    val queueLabel = stringResource(R.string.player_queue)
    val lyricsLabel = stringResource(R.string.player_lyrics)
    val downloadLabel = when {
        isDownloaded -> stringResource(R.string.player_download_library)
        isDownloading && downloadState.progressPercent > 0 -> "${downloadState.progressPercent}%"
        else -> stringResource(R.string.player_download)
    }

    val actions = buildList {
        if (onToggleVideo != null) {
            add(
                DockActionItem(
                    key = "video",
                    icon = Icons.Filled.SmartDisplay,
                    label = if (isVideoActive) "Cover" else "Video",
                    showBadgeDot = isVideoActive,
                    comicAccent = ComicPink,
                    isHighlighted = isVideoActive,
                    onClick = onToggleVideo,
                ),
            )
        }
        add(
            DockActionItem(
                key = "queue",
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                label = queueLabel,
                showBadgeDot = false,
                comicAccent = ComicCyan,
                isHighlighted = true,
                onClick = onShowQueue,
            ),
        )
        add(
            DockActionItem(
                key = "lyrics",
                icon = Icons.Filled.FormatQuote,
                label = lyricsLabel,
                showBadgeDot = hasLyrics,
                comicAccent = ComicMint,
                isHighlighted = hasLyrics,
                onClick = onShowLyrics,
            ),
        )
        if (onDownload != null && (!isDownloaded || downloadState.openInLibrary)) {
            add(
                DockActionItem(
                    key = "download",
                    icon = if (isDownloaded) Icons.Filled.LibraryMusic else Icons.Filled.Download,
                    label = downloadLabel,
                    showBadgeDot = isDownloaded,
                    comicAccent = ComicAccent,
                    isHighlighted = isDownloading || isDownloaded,
                    isLoading = isDownloading,
                    progressPercent = downloadState.progressPercent,
                    succeeded = isDownloaded,
                    onClick = onDownload,
                ),
            )
        }
    }
    if (actions.isEmpty()) return

    if (!useDraggableDock) {
        val itemHeight = if (compact) 48.dp else 54.dp
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.forEach { item ->
                ClassicSecondaryActionTile(
                    item = item,
                    height = itemHeight,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        return
    }

    val count = actions.size
    val isComic = LocalComicTheme.current
    val isDark = LocalIsDarkTheme.current
    val darkBackdrop = PlayerPalette.shade.luminance() < 0.5f
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val useRefractionLens = !isComic && LocalLiquidGlass.current && isLiquidGlassBackdropSupported()
    val dockHeight = if (compact) 50.dp else 56.dp

    val latestActions by rememberUpdatedState(actions)
    var pressed by remember { mutableStateOf(false) }
    var cancelled by remember { mutableStateOf(false) }
    var touchPosition by remember(count) { mutableFloatStateOf(0f) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var innerWidthPx by remember { mutableIntStateOf(0) }
    val trailingPosition = remember(count, isRtl) { Animatable(0f) }

    // No resting glass pill: the convex lens appears only while pressing or dragging.
    val lift by animateFloatAsState(
        targetValue = if (pressed && !cancelled) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.86f, stiffness = 560f),
        label = "playerDockLift",
    )
    val currentPosition = { touchPosition }

    LaunchedEffect(count, isRtl) {
        snapshotFlow { currentPosition() }.collectLatest {
            trailingPosition.animateTo(it, spring(dampingRatio = 0.92f, stiffness = 540f))
        }
    }

    val activeSlotIndex = liquidNavigationIndex(currentPosition(), count, isRtl).coerceIn(actions.indices)
    val comicLensColor by animateColorAsState(
        targetValue = actions[activeSlotIndex].comicAccent.copy(alpha = if (isDark) 0.44f else 1f),
        animationSpec = tween(160),
        label = "playerDockComicLens",
    )

    // Center neatly when fewer than 4 slots (e.g. Download disabled or already saved).
    val dockWidthModifier = when (count) {
        1 -> Modifier.widthIn(min = 124.dp, max = 160.dp)
        2 -> Modifier.fillMaxWidth(0.64f).widthIn(min = 196.dp, max = 256.dp)
        3 -> Modifier.fillMaxWidth(0.86f).widthIn(min = 256.dp, max = 328.dp)
        else -> Modifier.fillMaxWidth().widthIn(max = 420.dp)
    }

    val railState = rememberLiquidState()
    val pressedScale = dockHeight.value / (dockHeight.value - 8f).coerceAtLeast(1f)
    val slotWidthPx = if (innerWidthPx > 0) innerWidthPx.toFloat() / count else 0f
    val slotWidthDp = with(density) { slotWidthPx.toDp() }
    val outerBg = when {
        isComic -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> liquidGlassContainerColor(PlayerPalette.ink.copy(alpha = 0.14f))
    }

    val gestureModifier = Modifier.pointerInput(count, isRtl) {
        val cancelMarginPx = 36.dp.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val width = size.width
            val height = size.height
            if (width <= 0) return@awaitEachGesture
            val fingerStart = liquidNavigationRawPosition(down.position.x, width.toFloat(), count)
            touchPosition = liquidNavigationRubberBand(fingerStart, count)
            touchY = 0f
            cancelled = false
            pressed = true
            var dragged = false
            var lastHoveredIndex = liquidNavigationIndex(touchPosition, count, isRtl)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val dx = change.position.x - down.position.x
                    val dy = change.position.y - down.position.y
                    if (event.changes.count { it.pressed } > 1 || change.isConsumed) {
                        cancelled = true
                        break
                    }
                    if (!dragged && abs(dy) > viewConfiguration.touchSlop && abs(dy) >= abs(dx)) {
                        cancelled = true
                        break
                    }
                    val outOfVerticalBounds =
                        change.position.y < -cancelMarginPx || change.position.y > height + cancelMarginPx
                    cancelled = outOfVerticalBounds
                    touchPosition = liquidNavigationRubberBand(
                        liquidNavigationRawPosition(change.position.x, width.toFloat(), count),
                        count,
                    )
                    touchY = (dy / height.coerceAtLeast(1)).coerceIn(-0.2f, 0.2f)
                    val hovered = liquidNavigationIndex(touchPosition, count, isRtl)
                    if (!outOfVerticalBounds && hovered != lastHoveredIndex) {
                        lastHoveredIndex = hovered
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                    }
                    if (!change.pressed) {
                        if (!cancelled) {
                            change.consume()
                            val currentList = latestActions
                            val destination = liquidNavigationIndex(touchPosition, currentList.size, isRtl)
                                .coerceIn(currentList.indices)
                            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                            currentList[destination].onClick()
                        }
                        break
                    }
                    if (abs(dx) > viewConfiguration.touchSlop && abs(dx) > abs(dy)) dragged = true
                    if (dragged) change.consume()
                }
            } finally {
                pressed = false
                cancelled = false
                touchY = 0f
            }
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val dockModifier = dockWidthModifier
            .height(dockHeight)
            .let { if (!isComic) it.waterDropJiggle(amplitude = 0.018f) else it }

        // Keep outer container unclipped so the lifted lens can swell past the rail like Home UI.
        Box(modifier = dockModifier) {
            // Rail + slots captured together when real refraction is active; sliding lens stays an acyclic sibling.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .let { if (useRefractionLens) it.liquefiable(railState) else it },
            ) {
                if (isComic) {
                    Surface(
                        shape = CircleShape,
                        color = outerBg,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier.matchParentSize(),
                    ) {}
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(outerBg, CircleShape)
                            .liquidGlassChrome(CircleShape, LocalLiquidGlass.current, LiquidGlassPreset.PlayerControls)
                            .glassRim(),
                    )
                }
                if (useRefractionLens) {
                    DockSlotsRow(
                        actions = actions,
                        isComic = isComic,
                        isDark = isDark,
                        isRtl = isRtl,
                        lifted = pressed && !cancelled,
                        currentPosition = currentPosition,
                        onActivate = { item -> item.onClick() },
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .onSizeChanged { innerWidthPx = it.width }
                    .then(gestureModifier),
            ) {
                if (slotWidthPx > 0f && lift > 0.01f) {
                    val lensBaseModifier = Modifier
                        .align(AbsoluteAlignment.TopLeft)
                        .absoluteOffset {
                            IntOffset((currentPosition() * slotWidthPx).roundToInt(), 0)
                        }
                        .width(slotWidthDp)
                        .fillMaxHeight()
                        .graphicsLayer {
                            val amount = lift.coerceIn(0f, 1f)
                            val flow = ((currentPosition() - trailingPosition.value) * 2f).coerceIn(-1f, 1f)
                            val baseScale = 0.88f + amount * (pressedScale - 0.88f)
                            scaleX = baseScale + amount * 0.03f * abs(flow)
                            scaleY = baseScale - amount * 0.015f * abs(flow)
                            translationY = (-1.dp.toPx() + touchY * 3.dp.toPx()) * amount
                            alpha = amount
                            shape = CircleShape
                            clip = false
                            if (!isComic) {
                                shadowElevation = 4.5.dp.toPx() * amount
                                ambientShadowColor = Color.Black.copy(alpha = 0.08f)
                                spotShadowColor = Color.Black.copy(alpha = 0.16f)
                            }
                        }

                    when {
                        useRefractionLens -> {
                            Box(
                                lensBaseModifier
                                    .liquidBubbleHighlight(darkBackdrop) { lift.coerceIn(0f, 1f) }
                                    .liquid(railState) {
                                        val amount = lift.coerceIn(0f, 1f)
                                        shape = CircleShape
                                        frost = 0.dp
                                        refraction = 0.42f * amount
                                        curve = 0.20f * amount
                                        edge = 0.035f * amount
                                        dispersion = 0.12f * amount
                                        saturation = LiquidGlassIntensity.saturation(
                                            GlassRecipe.SATURATION_FROSTED,
                                            GlassRecipe.SATURATION_CLEAR,
                                        )
                                        tint = Color.White.copy(alpha = (if (darkBackdrop) 0.02f else 0.03f) * amount)
                                    },
                            )
                        }
                        isComic -> {
                            Box(
                                lensBaseModifier
                                    .clipOnly(CircleShape)
                                    .background(comicLensColor, CircleShape)
                                    .border(1.8.dp, ComicInk, CircleShape),
                            )
                        }
                        else -> {
                            Box(
                                lensBaseModifier
                                    .clipOnly(CircleShape)
                                    .background(PlayerPalette.ink.copy(alpha = if (darkBackdrop) 0.24f else 0.18f), CircleShape)
                                    .liquidBubbleHighlight(darkBackdrop) { lift.coerceIn(0f, 1f) },
                            )
                        }
                    }
                }
                if (!useRefractionLens) {
                    DockSlotsRow(
                        actions = actions,
                        isComic = isComic,
                        isDark = isDark,
                        isRtl = isRtl,
                        lifted = pressed && !cancelled,
                        currentPosition = currentPosition,
                        onActivate = { item -> item.onClick() },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DockSlotsRow(
    actions: List<DockActionItem>,
    isComic: Boolean,
    isDark: Boolean,
    isRtl: Boolean,
    lifted: Boolean,
    currentPosition: () -> Float,
    onActivate: (DockActionItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = actions.size
    val slotContentColor = if (isComic && !isDark) ComicInk else PlayerPalette.ink.copy(alpha = 0.94f)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEachIndexed { index, item ->
            val visualSlot = liquidNavigationVisualIndex(index, count, isRtl)
            DockActionSlot(
                item = item,
                contentColor = slotContentColor,
                isComic = isComic,
                isDark = isDark,
                scaleProvider = {
                    val dist = abs(currentPosition() - visualSlot)
                    if (lifted) 1f + (1f - dist).coerceAtLeast(0f) * 0.12f else 1f
                },
                alphaProvider = {
                    val dist = abs(currentPosition() - visualSlot)
                    if (lifted) (if (dist < 0.65f) 1f else 0.66f) else 0.94f
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                        contentDescription = item.label
                        onClick(label = item.label) {
                            onActivate(item)
                            true
                        }
                    },
            )
        }
    }
}

@Composable
private fun ClassicSecondaryActionTile(
    item: DockActionItem,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val isComic = LocalComicTheme.current
    val isDark = LocalIsDarkTheme.current
    val shownProgress by animateFloatAsState(item.progressPercent / 100f, tween(300), label = "classicSecProgress")
    val tick = remember { Animatable(0f) }
    var showTick by remember { mutableStateOf(false) }
    var wasLoading by remember { mutableStateOf(item.isLoading) }
    LaunchedEffect(item.isLoading) {
        val finished = wasLoading && !item.isLoading && item.succeeded
        wasLoading = item.isLoading
        if (finished) {
            showTick = true
            tick.snapTo(0f)
            tick.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
            delay(700L)
            showTick = false
        }
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, ExpressiveMotion.spatialSpring(), label = "classicSecScale")
    val shape = RoundedCornerShape(14.dp)
    val tileColor by animateColorAsState(
        targetValue = when {
            isComic && !isDark ->
                if (item.isHighlighted) item.comicAccent else item.comicAccent.copy(alpha = 0.44f)
            isComic ->
                item.comicAccent.copy(alpha = if (item.isHighlighted) 0.36f else 0.22f)
            else -> liquidGlassContainerColor(PlayerPalette.ink.copy(alpha = 0.14f))
        },
        animationSpec = tween(180),
        label = "classicSecColor",
    )
    val tileContentColor = if (isComic && !isDark) ComicInk else PlayerPalette.ink.copy(alpha = 0.94f)

    LiquidGlassSurface(
        glassModifier = Modifier.liquidGlassChrome(shape, LocalLiquidGlass.current, LiquidGlassPreset.PlayerControls, interactionSource = interaction),
        onClick = hapticClick(action = item.onClick),
        interactionSource = interaction,
        shape = shape,
        color = tileColor,
        contentColor = tileContentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Box(Modifier.fillMaxSize()) {
            if (item.showBadgeDot) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isComic) {
                                if (isDark) ComicAccent else ComicInk
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        ),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (showTick) {
                    DrawInTick(progress = { tick.value }, color = tileContentColor, modifier = Modifier.size(19.dp))
                } else if (item.isLoading) {
                    if (item.progressPercent in 1..99) {
                        CircularProgressIndicator(
                            progress = { shownProgress },
                            modifier = Modifier.size(18.dp),
                            color = tileContentColor,
                            trackColor = tileContentColor.copy(alpha = 0.22f),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = tileContentColor,
                            strokeWidth = 2.dp,
                        )
                    }
                } else {
                    Icon(imageVector = item.icon, contentDescription = item.label, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Out and back: 0 to 1 quickly, then a springy settle to 0. Drives tap nudges and pops. */
internal suspend fun Animatable<Float, *>.kick() {
    snapTo(0f)
    animateTo(1f, tween(90, easing = FastOutSlowInEasing))
    animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 520f))
}

/** A check mark that draws itself in as [progress] runs 0..1, with a small pop at the end. */
@Composable
internal fun DrawInTick(progress: () -> Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val p = progress()
        val pop = 1f + 0.18f * kotlin.math.sin(p * Math.PI).toFloat()
        val full = Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.52f)
            lineTo(size.width * 0.42f, size.height * 0.74f)
            lineTo(size.width * 0.84f, size.height * 0.28f)
        }
        val measure = androidx.compose.ui.graphics.PathMeasure().apply { setPath(full, false) }
        val drawn = Path()
        measure.getSegment(0f, measure.length * p, drawn, true)
        scale(pop) {
            drawPath(drawn, color, style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
private fun DockActionSlot(
    item: DockActionItem,
    contentColor: Color,
    isComic: Boolean,
    isDark: Boolean,
    scaleProvider: () -> Float,
    alphaProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    val shownProgress by animateFloatAsState(item.progressPercent / 100f, tween(300), label = "dockDlProgress")
    val tick = remember { Animatable(0f) }
    var showTick by remember { mutableStateOf(false) }
    var wasLoading by remember { mutableStateOf(item.isLoading) }
    LaunchedEffect(item.isLoading) {
        val finished = wasLoading && !item.isLoading && item.succeeded
        wasLoading = item.isLoading
        if (finished) {
            showTick = true
            tick.snapTo(0f)
            tick.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
            delay(700L)
            showTick = false
        }
    }

    Column(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .graphicsLayer {
                val s = scaleProvider()
                scaleX = s
                scaleY = s
                alpha = alphaProvider()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (showTick) {
                DrawInTick(progress = { tick.value }, color = contentColor, modifier = Modifier.size(18.dp))
            } else if (item.isLoading) {
                if (item.progressPercent in 1..99) {
                    CircularProgressIndicator(
                        progress = { shownProgress },
                        modifier = Modifier.size(18.dp),
                        color = contentColor,
                        trackColor = contentColor.copy(alpha = 0.22f),
                        strokeWidth = 2.dp,
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = contentColor,
                        strokeWidth = 2.dp,
                    )
                }
            } else {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (item.showBadgeDot) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 5.dp, y = (-2).dp)
                        .size(5.dp)
                        .clipOnly(CircleShape)
                        .background(
                            if (isComic) {
                                if (isDark) ComicAccent else ComicInk
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        ),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun shareTrackLink(context: Context, track: PlayableTrack) {
    val ytUrl = track.videoId?.takeIf { it.isNotBlank() }?.let { "https://music.youtube.com/watch?v=$it" }
    val shareText = if (ytUrl != null) "${track.title} — ${track.artist}\n$ytUrl" else "${track.title} — ${track.artist}"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "${track.title} — ${track.artist}")
        putExtra(Intent.EXTRA_TEXT, shareText)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AudioOutputSheet(
    player: MusicPlayer,
    state: MusicPlayerState,
    signalPath: SignalPathReport,
    usbDac: UsbDacMonitor.State,
    availability: InternalDirectAvailability,
    isTablet: Boolean,
    onDismiss: () -> Unit,
    onOpenSignalPath: () -> Unit,
    onOpenAudioControls: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val outputDisplay = remember(state.isCasting, signalPath, usbDac.dac, availability, isTablet) {
        resolveAudioOutputDisplay(state.isCasting, signalPath, usbDac, availability, isTablet)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp),
            ) {}
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = outputDisplay.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Audio Output",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = outputDisplay.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "Active Signal Engine",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = buildString {
                            append(signalPath.outputEngineName)
                            if (signalPath.outputRateHz > 0) {
                                append(" \u2022 ${signalPath.outputBits}-bit / ${formatSampleRateKHz(signalPath.outputRateHz / 1000.0)} kHz")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenSignalPath()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text("Signal Path", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onOpenAudioControls()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text("Volume & Speed", maxLines = 1)
                        }
                    }
                }
            }

            val dac = usbDac.dac
            if (dac != null && dac.hasUsbPeripheral && !dac.usbPermissionGranted) {
                Button(
                    onClick = {
                        player.requestUsbPermission()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Filled.Usb, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enable Direct USB DAC Access (${dac.name})")
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                OutlinedButton(
                    onClick = {
                        val launchedMediaOutput = runCatching {
                            context.startActivity(
                                Intent("com.android.settings.panel.action.MEDIA_OUTPUT")
                                    .putExtra("com.android.settings.panel.extra.PACKAGE_NAME", context.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }.isSuccess
                        if (!launchedMediaOutput) {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Headphones, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Switch System Media Output")
                }
            }

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PlayerCastMenuRow(player)
            }
        }
    }
}

/**
 * Edge-to-edge portrait Now Playing layout:
 * - Top reserved header clearance over full-bleed background
 * - Flexible middle artwork display area supporting swipe prev/next, double-tap seek/like, single-tap distraction-free toggle, and inline cover video
 * - Bottom stack: SongInformation -> AudioInformationRow -> PlaybackSeekBar -> Optional AudioVisualizer -> MainPlaybackControls -> SecondaryActions
 */
@Composable
internal fun NowPlayingScreen(
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
    visualizerLevels: List<Float> = emptyList(),
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
    /** Square cover sleeve in the art area (full-screen artwork off). */
    showSleeve: Boolean = false,
    sleeveCanvas: com.lastwave.app.data.canvas.CanvasArtwork? = null,
    pausedForTransition: Boolean = false,
    /** Four synced lyric lines under the sleeve (Settings › Lyrics under the cover). */
    lyricsPreview: Boolean = true,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val swipeThreshold = remember(density) { with(density) { 72.dp.toPx() } }
    // The sleeve follows the finger during a skip swipe; it only jumped once the
    // threshold was passed, so the gesture gave no feedback until it was over.
    val artworkDrag = remember { Animatable(0f) }
    val rippleProgress = remember { Animatable(1f) }
    val coverPopScale = remember { Animatable(1f) }
    val reflectionReveal = remember { Animatable(1f) }
    var sleeveWidthPx by remember { mutableFloatStateOf(0f) }
    var committingSwipeDirection by remember { mutableIntStateOf(0) }
    var suppressNextDirectionalSlide by remember { mutableStateOf(false) }
    var swipeCommitJob by remember { mutableStateOf<Job?>(null) }
    var dragAnimJob by remember { mutableStateOf<Job?>(null) }

    var seekOverlayDirection by remember { mutableStateOf<SeekDirection?>(null) }
    var seekOverlaySeconds by remember { mutableIntStateOf(5) }
    var seekResetJob by remember { mutableStateOf<Job?>(null) }
    var heartBurstVisible by remember { mutableStateOf(false) }
    var heartBurstJob by remember { mutableStateOf<Job?>(null) }
    var pendingSingleTapJob by remember { mutableStateOf<Job?>(null) }
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }
    var lastTapRegion by remember { mutableIntStateOf(-1) } // 0 = left, 1 = center, 2 = right

    val hasLyrics = remember(lyricsState) {
        lyricsState is LyricsUiState.Success && (lyricsState.lines.isNotEmpty() || !lyricsState.plainLyrics.isNullOrBlank())
    }
    val trackKey = remember(track.videoId, track.title, track.artist) {
        track.videoId?.takeIf(String::isNotBlank) ?: "${track.title}|${track.artist}"
    }
    var settledDragTrackKey by remember { mutableStateOf(trackKey) }
    var lastRippleTrackKey by remember { mutableStateOf(trackKey) }
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
    val currentPrevTrack by rememberUpdatedState(prevTrack)
    val currentNextTrack by rememberUpdatedState(nextTrack)
    val currentShowSleeve by rememberUpdatedState(showSleeve)
    val currentTrackKey by rememberUpdatedState(trackKey)
    val currentOnToggleDistractionFree by rememberUpdatedState(onToggleDistractionFree)
    val currentOnDoubleTapLike by rememberUpdatedState(onDoubleTapLike)
    val currentOnToggleFullArtwork by rememberUpdatedState(onToggleFullArtwork)
    val stepDirection = rememberTrackStepDirection(
        trackKey = trackKey,
        queueIndex = state.currentIndex,
        queueSize = state.queue.size,
    )

    // Kept at NowPlayingScreen level (not inside `if (showSleeve)`) so toggling
    // Full Artwork / Video or skipping tracks never leaves settledDragTrackKey
    // or committingSwipeDirection stale.
    LaunchedEffect(trackKey) {
        if (trackKey != lastRippleTrackKey) {
            lastRippleTrackKey = trackKey
            val landedFromSwipe = committingSwipeDirection != 0 || suppressNextDirectionalSlide
            swipeCommitJob?.cancel()
            swipeCommitJob = null
            dragAnimJob?.cancel()
            dragAnimJob = null
            artworkDrag.snapTo(0f)
            committingSwipeDirection = 0
            suppressNextDirectionalSlide = false
            settledDragTrackKey = trackKey
            coverPopScale.snapTo(1f)
            if (landedFromSwipe) {
                reflectionReveal.snapTo(0f)
                launch {
                    reflectionReveal.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
                }
            } else {
                reflectionReveal.snapTo(1f)
            }
            launch {
                rippleProgress.snapTo(0f)
                rippleProgress.animateTo(1f, tween(860, easing = FastOutSlowInEasing))
            }
        }
    }
    LaunchedEffect(showSleeve, isVideoActive) {
        swipeCommitJob?.cancel()
        swipeCommitJob = null
        dragAnimJob?.cancel()
        dragAnimJob = null
        committingSwipeDirection = 0
        suppressNextDirectionalSlide = false
        settledDragTrackKey = currentTrackKey
        if (artworkDrag.value != 0f) {
            artworkDrag.snapTo(0f)
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val compactHeight = maxHeight < 700.dp
        val ultraCompactHeight = maxHeight < 550.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .padding(horizontal = if (ultraCompactHeight) 16.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Flexible middle artwork display area (edge-to-edge artwork remains uninterrupted behind).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(
                        vertical = if (nowPlayingMinimalStyle) {
                            if (ultraCompactHeight) 2.dp else 4.dp
                        } else {
                            if (ultraCompactHeight) 4.dp else 8.dp
                        },
                    )
                    .pointerInput(isVideoActive, isDistractionFree) {
                        if (isVideoActive) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val dragBlocked = committingSwipeDirection != 0
                            var isDrag = false
                            var releaseHandled = false
                            var dragDeltaX = 0f
                            val touchSlop = viewConfiguration.touchSlop
                            val initialX = down.position.x
                            val initialY = down.position.y
                            val longPressAt = SystemClock.uptimeMillis() + viewConfiguration.longPressTimeoutMillis
                            var longPressed = false

                            while (true) {
                                // Held still past the long-press timeout: toggle Full Artwork
                                // here instead of making users hunt for it in Settings.
                                val event = if (isDrag || longPressed) awaitPointerEvent() else
                                    withTimeoutOrNull((longPressAt - SystemClock.uptimeMillis()).coerceAtLeast(0L)) { awaitPointerEvent() }
                                if (event == null) {
                                    longPressed = true
                                    pendingSingleTapJob?.cancel()
                                    lastTapRegion = -1
                                    lastTapTimestamp = 0L
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    currentOnToggleFullArtwork()
                                    continue
                                }
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (longPressed) {
                                    change.consume()
                                    if (!change.pressed) break
                                    continue
                                }
                                if (!change.pressed) {
                                    releaseHandled = true
                                    if (isDrag) {
                                        val wPx = (if (sleeveWidthPx > 1f) sleeveWidthPx else with(density) { 280.dp.toPx() }).coerceAtLeast(swipeThreshold * 2.6f)
                                        val pState = player.state.value
                                        val hasShuffledMulti = pState.shuffleEnabled && pState.queue.size > 1
                                        val canGoNext = currentNextTrack != null || hasShuffledMulti
                                        val willRestartCurrent = pState.positionMs > 5_000L
                                        val canGoPrev = currentPrevTrack != null || hasShuffledMulti || willRestartCurrent
                                        dragAnimJob?.cancel()
                                        dragAnimJob = null
                                        when {
                                            dragDeltaX < -swipeThreshold && canGoNext -> {
                                                if (currentShowSleeve && currentNextTrack != null) {
                                                    val commitFromKey = currentTrackKey
                                                    // Set synchronously BEFORE launching so the post-loop cancel
                                                    // guard never competes with or cancels the commit animation.
                                                    committingSwipeDirection = 1
                                                    swipeCommitJob?.cancel()
                                                    var jobRef: Job? = null
                                                    jobRef = coroutineScope.launch {
                                                        try {
                                                            artworkDrag.animateTo(-wPx, tween(durationMillis = 190, easing = FastOutSlowInEasing))
                                                            suppressNextDirectionalSlide = true
                                                            player.next()
                                                            delay(420L)
                                                            if (committingSwipeDirection != 0 && currentTrackKey == commitFromKey) {
                                                                committingSwipeDirection = 0
                                                                suppressNextDirectionalSlide = false
                                                                settledDragTrackKey = currentTrackKey
                                                                artworkDrag.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 460f))
                                                            }
                                                        } finally {
                                                            if (swipeCommitJob === jobRef) {
                                                                swipeCommitJob = null
                                                                if (committingSwipeDirection != 0) {
                                                                    committingSwipeDirection = 0
                                                                    suppressNextDirectionalSlide = false
                                                                    settledDragTrackKey = currentTrackKey
                                                                }
                                                            }
                                                        }
                                                    }
                                                    swipeCommitJob = jobRef
                                                } else {
                                                    player.next()
                                                    dragAnimJob = coroutineScope.launch {
                                                        artworkDrag.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 460f))
                                                    }
                                                }
                                            }
                                            dragDeltaX > swipeThreshold && canGoPrev -> {
                                                if (currentShowSleeve && !willRestartCurrent && currentPrevTrack != null) {
                                                    val commitFromKey = currentTrackKey
                                                    committingSwipeDirection = -1
                                                    swipeCommitJob?.cancel()
                                                    var jobRef: Job? = null
                                                    jobRef = coroutineScope.launch {
                                                        try {
                                                            artworkDrag.animateTo(wPx, tween(durationMillis = 190, easing = FastOutSlowInEasing))
                                                            suppressNextDirectionalSlide = true
                                                            player.previous()
                                                            delay(420L)
                                                            if (committingSwipeDirection != 0 && currentTrackKey == commitFromKey) {
                                                                committingSwipeDirection = 0
                                                                suppressNextDirectionalSlide = false
                                                                settledDragTrackKey = currentTrackKey
                                                                artworkDrag.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 460f))
                                                            }
                                                        } finally {
                                                            if (swipeCommitJob === jobRef) {
                                                                swipeCommitJob = null
                                                                if (committingSwipeDirection != 0) {
                                                                    committingSwipeDirection = 0
                                                                    suppressNextDirectionalSlide = false
                                                                    settledDragTrackKey = currentTrackKey
                                                                }
                                                            }
                                                        }
                                                    }
                                                    swipeCommitJob = jobRef
                                                } else {
                                                    player.previous()
                                                    dragAnimJob = coroutineScope.launch {
                                                        artworkDrag.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 460f))
                                                    }
                                                }
                                            }
                                            else -> {
                                                dragAnimJob = coroutineScope.launch {
                                                    artworkDrag.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 460f))
                                                }
                                            }
                                        }
                                    } else if (isDistractionFree) {
                                        currentOnToggleDistractionFree()
                                    } else {
                                        val now = SystemClock.elapsedRealtime()
                                        val region = when {
                                            initialX < size.width * 0.32f -> 0
                                            initialX > size.width * 0.68f -> 2
                                            else -> 1
                                        }
                                        val isDoubleTap = lastTapRegion == region && lastTapTimestamp != 0L && now - lastTapTimestamp < 360L
                                        if (isDoubleTap) {
                                            pendingSingleTapJob?.cancel()
                                            pendingSingleTapJob = null
                                            lastTapTimestamp = now
                                            when (region) {
                                                0 -> {
                                                    val nextSeconds = if (seekOverlayDirection == SeekDirection.REWIND) seekOverlaySeconds + 5 else 5
                                                    seekOverlaySeconds = nextSeconds
                                                    seekOverlayDirection = SeekDirection.REWIND
                                                    val pos = (player.state.value.positionMs - 5_000L).coerceAtLeast(0L)
                                                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                                    player.seekTo(pos)
                                                    seekResetJob?.cancel()
                                                    seekResetJob = coroutineScope.launch {
                                                        delay(700L)
                                                        seekOverlayDirection = null
                                                        lastTapRegion = -1
                                                    }
                                                }
                                                2 -> {
                                                    val nextSeconds = if (seekOverlayDirection == SeekDirection.FORWARD) seekOverlaySeconds + 5 else 5
                                                    seekOverlaySeconds = nextSeconds
                                                    seekOverlayDirection = SeekDirection.FORWARD
                                                    val maxDur = player.state.value.durationMs.coerceAtLeast(0L)
                                                    val pos = (player.state.value.positionMs + 5_000L).let { target ->
                                                        if (maxDur > 0L) target.coerceAtMost(maxDur) else target
                                                    }
                                                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                                    player.seekTo(pos)
                                                    seekResetJob?.cancel()
                                                    seekResetJob = coroutineScope.launch {
                                                        delay(700L)
                                                        seekOverlayDirection = null
                                                        lastTapRegion = -1
                                                    }
                                                }
                                                else -> {
                                                    lastTapRegion = -1
                                                    lastTapTimestamp = 0L
                                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                                    currentOnDoubleTapLike()
                                                    heartBurstVisible = true
                                                    heartBurstJob?.cancel()
                                                    heartBurstJob = coroutineScope.launch {
                                                        delay(560L)
                                                        heartBurstVisible = false
                                                    }
                                                }
                                            }
                                        } else {
                                            lastTapRegion = region
                                            lastTapTimestamp = now
                                            seekOverlayDirection = null
                                            pendingSingleTapJob?.cancel()
                                            // A single tap only arms the double-tap window: it no
                                            // longer hides the controls (users found that annoying).
                                            pendingSingleTapJob = coroutineScope.launch {
                                                delay(280L)
                                                lastTapRegion = -1
                                            }
                                        }
                                    }
                                    break
                                }

                                if (change.isConsumed) break
                                val dx = change.position.x - initialX
                                val dy = change.position.y - initialY
                                if (!isDrag) {
                                    if (!dragBlocked && kotlin.math.abs(dx) > touchSlop && kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                                        isDrag = true
                                        pendingSingleTapJob?.cancel()
                                        pendingSingleTapJob = null
                                        val pState = player.state.value
                                        val hasShuffledMulti = pState.shuffleEnabled && pState.queue.size > 1
                                        val canGoNext = currentNextTrack != null || hasShuffledMulti
                                        val canGoPrev = currentPrevTrack != null || hasShuffledMulti || pState.positionMs > 5_000L
                                        val blockedEdge = (dx > 0f && !canGoPrev) || (dx < 0f && !canGoNext)
                                        val effectiveDx = if (blockedEdge) dx * 0.30f else dx
                                        dragDeltaX = effectiveDx
                                        change.consume()
                                        dragAnimJob?.cancel()
                                        dragAnimJob = coroutineScope.launch { artworkDrag.snapTo(effectiveDx) }
                                    }
                                } else {
                                    change.consume()
                                    val pState = player.state.value
                                    val hasShuffledMulti = pState.shuffleEnabled && pState.queue.size > 1
                                    val canGoNext = currentNextTrack != null || hasShuffledMulti
                                    val canGoPrev = currentPrevTrack != null || hasShuffledMulti || pState.positionMs > 5_000L
                                    val blockedEdge = (dx > 0f && !canGoPrev) || (dx < 0f && !canGoNext)
                                    val effectiveDx = if (blockedEdge) dx * 0.30f else dx
                                    // One tick as the swipe becomes a skip, either way.
                                    if (!blockedEdge && (kotlin.math.abs(effectiveDx) > swipeThreshold) != (kotlin.math.abs(dragDeltaX) > swipeThreshold)) {
                                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    }
                                    dragDeltaX = effectiveDx
                                    dragAnimJob?.cancel()
                                    dragAnimJob = coroutineScope.launch { artworkDrag.snapTo(effectiveDx) }
                                }
                            }
                            // Only on a cancelled gesture (not normal finger-up release), so
                            // this never competes with a newly launched swipeCommitJob.
                            if (!releaseHandled && committingSwipeDirection == 0 && artworkDrag.value != 0f) {
                                dragAnimJob?.cancel()
                                dragAnimJob = coroutineScope.launch {
                                    artworkDrag.animateTo(0f, spring(dampingRatio = 0.82f, stiffness = 460f))
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                val previewLines = (lyricsState as? LyricsUiState.Success)
                    ?.takeIf { lyricsPreview && it.isSynced && !it.isInstrumental && it.lines.isNotEmpty() }
                    ?.lines
                val hasLyricsPreview = previewLines != null
                val lyricsTextAlpha by animateFloatAsState(
                    targetValue = if (hasLyricsPreview) 1f else 0f,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "nowPlayingLyricsTextAlpha",
                )
                var retainedLines by remember { mutableStateOf(previewLines) }
                LaunchedEffect(trackKey, previewLines, lyricsTextAlpha <= 0.01f) {
                    if (previewLines != null) {
                        retainedLines = previewLines
                    } else if (lyricsTextAlpha <= 0.01f) {
                        retainedLines = null
                    }
                }
                // Clear stale lyrics text immediately on track change so old lines never jump to index 0 mid-3D swipe.
                LaunchedEffect(trackKey) {
                    if (previewLines == null) retainedLines = null
                }
                val activePreviewLines = previewLines ?: retainedLines

                if (isVideoActive) {
                    SyncedCoverVideoPlayer(
                        track = track,
                        musicPlayer = player,
                        onCloseVideo = onCloseVideo,
                        onEnterFullscreen = onEnterVideoFullscreen,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                } else if (showSleeve) {
                    // Lock sleeve size and vertical position whenever Lyrics Preview is enabled so
                    // delayed lyrics fetches never push artwork down and back up (0% stutter).
                    val lyricsPresence by animateFloatAsState(
                        targetValue = if (lyricsPreview) 1f else 0f,
                        animationSpec = ExpressiveMotion.spatialSlow(),
                        label = "nowPlayingLyricsPresence",
                    )
                    val sleeveInset = if (nowPlayingMinimalStyle) 6.dp else 16.dp
                    val isComic = LocalComicTheme.current
                    val isDarkTheme = LocalIsDarkTheme.current
                    val comicAccent = ComicAccent
                    val ringBaseColors = remember(primaryColor, secondaryColor, tertiaryColor, isComic, comicAccent) {
                        if (isComic) listOf(ComicCyan, comicAccent, ComicPink)
                        else listOf(primaryColor, secondaryColor, tertiaryColor)
                    }

                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        // One animated progress drives top spacer, sleeve shrink, reflection collapse,
                        // and centered 3-line lyrics gap so nothing jumps or glides unevenly.
                        val lyricsBlockBudget = (LYRIC_PREVIEW_HEIGHT + 22.dp) * lyricsPresence
                        val reflectionAllowance = if (!isComic) 28.dp * (1f - lyricsPresence) else 0.dp
                        val sleeveSide = (minOf(maxWidth, maxHeight - lyricsBlockBudget - reflectionAllowance) - sleeveInset).coerceIn(0.dp, 520.dp)
                        val sleeveShape = RoundedCornerShape(24.dp)
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Spacer(Modifier.weight((1f - lyricsPresence).coerceAtLeast(0.001f)))
                            Spacer(Modifier.height((if (ultraCompactHeight) 2.dp else 6.dp) * lyricsPresence))
                            Box(
                                modifier = Modifier
                                    .size(sleeveSide)
                                    .zIndex(1f)
                                    .onSizeChanged { sleeveWidthPx = it.width.toFloat() }
                                    .drawBehind {
                                        val rp = rippleProgress.value
                                        if (rp < 0.999f) {
                                            val delays = floatArrayOf(0f, 0.14f, 0.28f)
                                            val maxExpand = 44.dp.toPx()
                                            val baseCorner = 24.dp.toPx()
                                            val peakAlpha = if (isDarkTheme || isComic) 0.56f else 0.40f
                                            delays.forEachIndexed { idx, delayFrac ->
                                                val local = ((rp - delayFrac) / (1f - delayFrac)).coerceIn(0f, 1f)
                                                if (local > 0f && local < 1f) {
                                                    val expand = maxExpand * local
                                                    val decay = (1f - local) * (1f - local)
                                                    val alpha = peakAlpha * decay * (1f - idx * 0.18f)
                                                    val c1 = ringBaseColors[idx % ringBaseColors.size].copy(alpha = alpha)
                                                    val c2 = ringBaseColors[(idx + 1) % ringBaseColors.size].copy(alpha = alpha)
                                                    val r = baseCorner + expand * 0.4f
                                                    drawRoundRect(
                                                        brush = Brush.linearGradient(listOf(c1, c2)),
                                                        topLeft = Offset(-expand, -expand),
                                                        size = Size(size.width + expand * 2f, size.height + expand * 2f),
                                                        cornerRadius = CornerRadius(r, r),
                                                        style = Stroke(width = (3.5.dp.toPx() * (1f - 0.6f * local))),
                                                    )
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                BeatGlow(
                                    player = player,
                                    isPlaying = state.isPlaying,
                                    color = primaryColor,
                                    modifier = Modifier.requiredSize(sleeveSide * 1.7f),
                                )
                                listOfNotNull(
                                    prevTrack?.let { -1f to it },
                                    nextTrack?.let { 1f to it },
                                ).forEach { (sideSign, neighbor) ->
                                    androidx.compose.runtime.key(sideSign) {
                                        PlayerArtwork(
                                            track = neighbor,
                                            modifier = Modifier
                                                .size(sleeveSide)
                                                .graphicsLayer {
                                                    if (trackKey != settledDragTrackKey) {
                                                        alpha = 0f
                                                        return@graphicsLayer
                                                    }
                                                    val drag = artworkDrag.value
                                                    val wPx = size.width.coerceAtLeast(1f)
                                                    val towardThis = if (sideSign > 0f) (-drag / wPx).coerceIn(0f, 1f) else (drag / wPx).coerceIn(0f, 1f)
                                                    if (towardThis <= 0.002f) {
                                                        alpha = 0f
                                                        return@graphicsLayer
                                                    }
                                                    val ease = towardThis * (2f - towardThis)
                                                    val sc = androidx.compose.ui.util.lerp(0.82f, 1f, ease)
                                                    scaleX = sc
                                                    scaleY = sc
                                                    cameraDistance = 16f * this.density
                                                    rotationY = -sideSign * androidx.compose.ui.util.lerp(24f, 0f, ease)
                                                    translationX = sideSign * wPx * 0.56f * (1f - ease)
                                                    alpha = (towardThis * 2.2f).coerceIn(0f, 1f)
                                                }
                                                .shadow(elevation = if (state.isPlaying) 28.dp else 14.dp, shape = sleeveShape),
                                            corner = 24.dp,
                                            isPlaying = false,
                                            pausedForTransition = true,
                                        )
                                    }
                                }
                                DirectionalTrackContent(
                                    trackKey = trackKey,
                                    track = track,
                                    stepDirection = stepDirection,
                                    clip = false,
                                    slideFraction = 0.26f,
                                    animate = !suppressNextDirectionalSlide && committingSwipeDirection == 0,
                                    label = "nowPlayingArtworkSlide",
                                    // On the content, not the gesture box: moving that would
                                    // move the touch coordinates the drag is measured in.
                                    modifier = Modifier.graphicsLayer {
                                        if (trackKey != settledDragTrackKey) {
                                            translationX = 0f
                                            rotationY = 0f
                                            rotationZ = 0f
                                            scaleX = 1f
                                            scaleY = 1f
                                            alpha = 1f
                                            return@graphicsLayer
                                        }
                                        val drag = artworkDrag.value
                                        val width = size.width.coerceAtLeast(1f)
                                        val progress = (drag / width).coerceIn(-1f, 1f)
                                        val absP = abs(progress)
                                        cameraDistance = 16f * this.density
                                        translationX = drag * 0.96f
                                        rotationY = (-progress * 26f).coerceIn(-28f, 28f)
                                        rotationZ = progress * 3.5f
                                        val sc = coverPopScale.value * (1f - absP * 0.14f)
                                        scaleX = sc
                                        scaleY = sc
                                        alpha = 1f - ((absP - 0.55f) / 0.45f).coerceIn(0f, 1f)
                                    },
                                ) { activeTrack ->
                                    PlayerArtwork(
                                        track = activeTrack,
                                        modifier = Modifier
                                            .size(sleeveSide)
                                            .artworkFlightSleeve(LocalArtworkFlight.current)
                                            .shadow(elevation = if (state.isPlaying) 28.dp else 14.dp, shape = sleeveShape),
                                        corner = 24.dp,
                                        canvas = sleeveCanvas,
                                        isPlaying = state.isPlaying,
                                        pausedForTransition = pausedForTransition,
                                    )
                                }
                            }
                            val reflHeight = 34.dp * (1f - lyricsPresence)
                            val reflTopGap = 6.dp * (1f - lyricsPresence)
                            if (!isComic && reflHeight > 0.5.dp) {
                                val flightFlying = LocalArtworkFlight.current?.flying == true
                                val reflBaseAlpha = (if (flightFlying) 0f else if (isDarkTheme) 0.26f else 0.16f) * (1f - lyricsPresence)
                                Box(
                                    modifier = Modifier
                                        .padding(top = reflTopGap)
                                        .width(sleeveSide)
                                        .height(reflHeight)
                                        .clipToBounds()
                                        .graphicsLayer {
                                            val effectiveRefl = reflBaseAlpha * reflectionReveal.value
                                            if (trackKey != settledDragTrackKey) {
                                                alpha = effectiveRefl
                                                translationX = 0f
                                                rotationY = 0f
                                                compositingStrategy = CompositingStrategy.Offscreen
                                                return@graphicsLayer
                                            }
                                            val drag = artworkDrag.value
                                            val wPx = size.width.coerceAtLeast(1f)
                                            val progress = (drag / wPx).coerceIn(-1f, 1f)
                                            val absP = abs(progress)
                                            alpha = effectiveRefl * (1f - (absP * 1.4f).coerceIn(0f, 1f))
                                            translationX = drag * 0.96f
                                            cameraDistance = 16f * this.density
                                            rotationY = (-progress * 26f).coerceIn(-28f, 28f)
                                            compositingStrategy = CompositingStrategy.Offscreen
                                        }
                                        .drawWithContent {
                                            drawContent()
                                            drawRect(
                                                brush = Brush.verticalGradient(
                                                    0.0f to Color.White,
                                                    0.72f to Color.White.copy(alpha = 0.20f),
                                                    1.0f to Color.Transparent,
                                                ),
                                                blendMode = BlendMode.DstIn,
                                            )
                                        },
                                    contentAlignment = Alignment.TopCenter,
                                ) {
                                    PlayerArtwork(
                                        track = track,
                                        corner = 24.dp,
                                        modifier = Modifier
                                            .requiredSize(sleeveSide)
                                            .graphicsLayer { scaleY = -1f }
                                            .blur(1.5.dp),
                                        isPlaying = false,
                                        pausedForTransition = true,
                                    )
                                }
                            }
                            // Centered 3-line lyrics gap container: mathematically centers
                            // the preview between artwork bottom edge and song info / progress.
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .clipToBounds(),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (lyricsPresence > 0.01f && activePreviewLines != null && lyricsTextAlpha > 0.01f) {
                                    NowPlayingLyricsPreview(
                                        lines = activePreviewLines,
                                        progressState = progressState,
                                        onOpenLyrics = onOpenLyrics,
                                        compact = compactHeight,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(LYRIC_PREVIEW_HEIGHT)
                                            .graphicsLayer {
                                                alpha = (((lyricsPresence - 0.15f) / 0.85f).coerceIn(0f, 1f)) * lyricsTextAlpha
                                                translationY = (1f - lyricsPresence) * 6.dp.toPx()
                                            },
                                    )
                                }
                            }
                        }
                    }
                } else if (!isDistractionFree && lyricsPreview && activePreviewLines != null && lyricsTextAlpha > 0.01f) {
                    NowPlayingLyricsPreview(
                        lines = activePreviewLines,
                        progressState = progressState,
                        onOpenLyrics = onOpenLyrics,
                        compact = compactHeight,
                        overArtwork = true,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = if (compactHeight) 8.dp else 12.dp)
                            .height(LYRIC_PREVIEW_HEIGHT + 12.dp)
                            .graphicsLayer { alpha = lyricsTextAlpha },
                    )
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
                        color = PlayerPalette.shade.copy(alpha = 0.52f),
                        modifier = Modifier.size(76.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = LikedHeartRed,
                                modifier = Modifier.size(38.dp),
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = !isDistractionFree,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(180)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = if (nowPlayingMinimalStyle) {
                                if (ultraCompactHeight) 6.dp else if (compactHeight) 10.dp else 14.dp
                            } else {
                                if (ultraCompactHeight) 6.dp else if (compactHeight) 10.dp else 14.dp
                            },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (nowPlayingMinimalStyle) {
                        MinimalSongInfoRow(
                            track = track,
                            isPlaying = state.isPlaying,
                            windowActive = windowActive,
                            isLiked = isLiked,
                            isCustomAudio = isCustomAudio,
                            onToggleLiked = onToggleLiked,
                            onOpenAudioControls = onOpenAudioControls,
                            onOpenArtist = onOpenArtist,
                            stepDirection = stepDirection,
                        )

                        Spacer(Modifier.height(if (ultraCompactHeight) 8.dp else if (compactHeight) 12.dp else 20.dp))

                        PlaybackSeekBar(
                            progressState = progressState,
                            isPlaying = state.isPlaying,
                            trackKey = track.videoId ?: "${track.artist}|${track.title}",
                            wavyEnabled = wavySeekbarEnabled,
                            onSeek = player::seekTo,
                            isTranslucent = true,
                            fallbackDurationMs = track.durationMs ?: state.durationMs,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            tertiaryColor = tertiaryColor,
                            telemetry = state.deckBlendTelemetry,
                            bestPartMs = rememberBestPartMs(player, track),
                            abLoopStartMs = state.abLoopStartMs,
                            abLoopEndMs = state.abLoopEndMs,
                            onCycleAbLoop = player::cycleAbLoop,
                            onClearAbLoop = player::clearAbLoop,
                            minimalStyle = true,
                            centerLabel = {
                                MinimalInlineQualityLabel(
                                    state = state,
                                    onOpenSignalPath = onOpenSignalPath,
                                    onCycleAbLoop = player::cycleAbLoop,
                                )
                            },
                        )

                        Spacer(Modifier.height(if (ultraCompactHeight) 8.dp else if (compactHeight) 14.dp else 24.dp))

                        MinimalPlaybackControls(
                            state = state,
                            player = player,
                            compact = compactHeight,
                        )

                        Spacer(Modifier.height(if (ultraCompactHeight) 8.dp else if (compactHeight) 14.dp else 22.dp))

                        MinimalBottomActionRow(
                            hasLyrics = hasLyrics,
                            onShowLyrics = onOpenLyrics,
                            onShowQueue = onOpenQueue,
                            onToggleVideo = onToggleVideo,
                            isVideoActive = isVideoActive,
                            downloadState = downloadState,
                            onDownload = onDownload,
                        )
                    } else {
                        NowPlayingSongInfo(
                            track = track,
                            isPlaying = state.isPlaying,
                            windowActive = windowActive,
                            isLiked = isLiked,
                            isCustomAudio = isCustomAudio,
                            onToggleLiked = onToggleLiked,
                            onOpenAudioControls = onOpenAudioControls,
                            onOpenArtist = onOpenArtist,
                            stepDirection = stepDirection,
                        )

                        Spacer(Modifier.height(if (ultraCompactHeight) 4.dp else if (compactHeight) 8.dp else 14.dp))

                        AudioInformationRow(
                            state = state,
                            signalPath = signalPath,
                            usbDac = usbDac,
                            availability = availability,
                            isTablet = isTablet,
                            onOpenSignalPath = onOpenSignalPath,
                            onOpenOutputPicker = onOpenOutputPicker,
                        )

                        Spacer(Modifier.height(if (ultraCompactHeight) 6.dp else if (compactHeight) 10.dp else 16.dp))

                        PlaybackSeekBar(
                            progressState = progressState,
                            isPlaying = state.isPlaying,
                            trackKey = track.videoId ?: "${track.artist}|${track.title}",
                            wavyEnabled = wavySeekbarEnabled,
                            onSeek = player::seekTo,
                            isTranslucent = true,
                            fallbackDurationMs = track.durationMs ?: state.durationMs,
                            primaryColor = primaryColor,
                            secondaryColor = secondaryColor,
                            tertiaryColor = tertiaryColor,
                            telemetry = state.deckBlendTelemetry,
                            bestPartMs = rememberBestPartMs(player, track),
                            abLoopStartMs = state.abLoopStartMs,
                            abLoopEndMs = state.abLoopEndMs,
                            onCycleAbLoop = player::cycleAbLoop,
                            onClearAbLoop = player::clearAbLoop,
                        )

                        if (!ultraCompactHeight) {
                            AudioVisualizer(
                                levels = visualizerLevels,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }

                        Spacer(Modifier.height(if (ultraCompactHeight) 4.dp else if (compactHeight) 8.dp else 16.dp))

                        MainPlaybackControls(
                            state = state,
                            player = player,
                            isTranslucent = true,
                            compact = compactHeight,
                        )

                        Spacer(Modifier.height(if (ultraCompactHeight) 8.dp else if (compactHeight) 12.dp else 22.dp))

                        SecondaryActionRow(
                            hasLyrics = hasLyrics,
                            compact = compactHeight,
                            useDraggableDock = playerActionDockEnabled,
                            onShowLyrics = onOpenLyrics,
                            onShowQueue = onOpenQueue,
                            onToggleVideo = onToggleVideo,
                            isVideoActive = isVideoActive,
                            downloadState = downloadState,
                            onDownload = onDownload,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SeekDirectionOverlay(
    visible: Boolean,
    direction: SeekDirection,
    seconds: Int,
    modifier: Modifier = Modifier,
) {
    val isForward = direction == SeekDirection.FORWARD
    val shape = if (isForward) {
        RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp, topStart = 72.dp, bottomStart = 72.dp)
    } else {
        RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 72.dp, bottomEnd = 72.dp)
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(100)) + scaleIn(ExpressiveMotion.spatialSpring(), initialScale = 0.88f),
        exit = fadeOut(tween(200)),
        modifier = modifier
            .fillMaxWidth(0.42f)
            .height(130.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(PlayerPalette.shade.copy(alpha = 0.58f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = PlayerPalette.ink.copy(alpha = 0.22f),
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isForward) Icons.Filled.FastForward else Icons.Filled.FastRewind,
                            contentDescription = stringResource(
                                if (isForward) R.string.player_seek_fwd_cd else R.string.player_seek_back_cd,
                            ),
                            tint = PlayerPalette.ink,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isForward) "+${seconds}s" else "-${seconds}s",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = PlayerPalette.ink,
                )
            }
        }
    }
}

/**
 * Soft cover-coloured glow behind the square sleeve that swells on each kick and
 * settles back, using the same beat detection as Music Haptics (the motor stays
 * off unless that setting is on). Without beats (paused, USB bit-perfect, Cast)
 * it rests at a faint, still glow. Animation is read in the draw phase only.
 */
@Composable
internal fun BeatGlow(
    player: MusicPlayer,
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val controller = player.hapticsController
    DisposableEffect(controller) {
        controller.setVisualBeatWanted(true)
        onDispose { controller.setVisualBeatWanted(false) }
    }
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(controller) {
        controller.beatPulses.collectLatest { strength ->
            // Rise fast, then a long ease back: reads as the cover breathing with the bass.
            pulse.animateTo((0.45f + 0.55f * strength).coerceAtMost(1f), tween(70, easing = LinearEasing))
            pulse.animateTo(0f, tween(620, easing = FastOutSlowInEasing))
        }
    }
    val level by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.4f,
        animationSpec = tween(500),
        label = "beatGlowLevel",
    )
    // The cover's hue at full brightness: ambient colours are often near-black,
    // which would vanish on the dark player background.
    val glow = remember(color) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[1] = hsv[1].coerceIn(0.45f, 0.85f)
        hsv[2] = 1f
        Color(android.graphics.Color.HSVToColor(hsv))
    }
    Box(
        modifier = modifier.drawBehind {
            val p = pulse.value
            val radius = size.minDimension / 2f * (0.90f + 0.10f * p)
            // The sleeve covers the inner ~60%; the brightest band sits right at its
            // edge so the light spills out around the cover, not hidden behind it.
            drawCircle(
                brush = Brush.radialGradient(
                    0f to glow.copy(alpha = ((0.50f + 0.30f * p) * level).coerceIn(0f, 0.9f)),
                    0.60f to glow.copy(alpha = ((0.34f + 0.36f * p) * level).coerceIn(0f, 0.8f)),
                    0.80f to glow.copy(alpha = ((0.12f + 0.22f * p) * level).coerceIn(0f, 0.5f)),
                    1f to Color.Transparent,
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        },
    )
}

/**
 * Jump 15 s back or forward. Drawn like shuffle / repeat (a bare icon, no ring)
 * so the play pill and the prev / next rings stay the strongest marks in the row.
 */
@Composable
private fun SeekStepButton(
    player: MusicPlayer,
    forward: Boolean,
    size: Dp,
) {
    val label = if (forward) "Forward 15 seconds" else "Back 15 seconds"
    // The ring turns the way it jumps (clockwise forward), then springs back.
    val spin = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    IconButton(
        onClick = hapticClick(HapticFeedbackType.ContextClick) {
            val now = player.state.value
            val duration = now.durationMs.coerceAtLeast(0L)
            val target = now.positionMs + if (forward) 15_000L else -15_000L
            player.seekTo(if (duration > 0L) target.coerceIn(0L, duration) else target.coerceAtLeast(0L))
            scope.launch { spin.kick() }
        },
        modifier = Modifier.size(size),
    ) {
        // The digits are sized in dp so system font scale can't push them out of
        // the ring, and trimmed of font padding so they centre on it exactly.
        val digits = with(LocalDensity.current) { 9.dp.toSp() }
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Replay,
                contentDescription = label,
                tint = PlayerPalette.ink.copy(alpha = 0.78f),
                // Mirrored for forward: the same arrow, turning the other way.
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer {
                        scaleX = if (forward) -1f else 1f
                        rotationZ = (if (forward) 40f else -40f) * spin.value
                    },
            )
            Text(
                text = "15",
                style = TextStyle(
                    fontSize = digits,
                    lineHeight = digits,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
                ),
                color = PlayerPalette.ink.copy(alpha = 0.85f),
                // Replay's ring sits 1/24 below the icon's centre.
                modifier = Modifier.offset(y = 1.2.dp),
            )
        }
    }
}

@Composable
internal fun MinimalSongInfoRow(
    track: PlayableTrack,
    isPlaying: Boolean,
    windowActive: Boolean,
    isLiked: Boolean,
    isCustomAudio: Boolean,
    onToggleLiked: () -> Unit,
    onOpenAudioControls: () -> Unit,
    onOpenArtist: (String) -> Unit,
    modifier: Modifier = Modifier,
    stepDirection: Int = 0,
) {
    val infoTrackKey = remember(track.videoId, track.title, track.artist) {
        track.videoId?.takeIf(String::isNotBlank) ?: "${track.title}|${track.artist}"
    }
    val heartTint = if (isLiked) {
        if (PlayerPalette.shade.luminance() < 0.5f) LikedHeartRedOnDark else LikedHeartRed
    } else {
        PlayerPalette.ink.copy(alpha = 0.80f)
    }
    val volumeInteraction = remember { MutableInteractionSource() }
    val volumePressed by volumeInteraction.collectIsPressedAsState()
    val volumeScale by animateFloatAsState(
        targetValue = if (volumePressed) 0.88f else 1f,
        animationSpec = ExpressiveMotion.spatialFast(),
        label = "minimalVolScale",
    )
    val likeInteraction = remember { MutableInteractionSource() }
    val likePressed by likeInteraction.collectIsPressedAsState()
    val likeScale by animateFloatAsState(
        targetValue = if (likePressed) 0.88f else 1f,
        animationSpec = ExpressiveMotion.spatialFast(),
        label = "minimalLikeScale",
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DirectionalTrackContent(
            trackKey = infoTrackKey,
            track = track,
            stepDirection = stepDirection,
            slideFraction = 0.3f,
            modifier = Modifier.weight(1f),
            label = "minimalSongInfoSlide",
        ) { activeTrack ->
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = activeTrack.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        letterSpacing = (-0.35).sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = PlayerPalette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .playingMarquee(isPlaying && windowActive),
                )
                Spacer(Modifier.height(3.dp))
                val splitArtists = remember(activeTrack.artist) { ArtistHelper.splitArtists(activeTrack.artist) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .playingMarquee(isPlaying && windowActive),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    splitArtists.forEachIndexed { index, artName ->
                        Text(
                            text = artName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = PlayerPalette.ink.copy(alpha = 0.78f),
                            maxLines = 1,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = hapticClick { onOpenArtist(artName) },
                            ),
                        )
                        if (index < splitArtists.lastIndex) {
                            Text(
                                text = ", ",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal,
                                ),
                                color = PlayerPalette.ink.copy(alpha = 0.52f),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = likeScale
                        scaleY = likeScale
                    }
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = likeInteraction,
                        indication = null,
                        onClick = hapticClick(if (isLiked) HapticFeedbackType.ToggleOff else HapticFeedbackType.Confirm, onToggleLiked),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isLiked) "Remove from liked songs" else "Like song",
                    tint = heartTint,
                    modifier = Modifier.size(22.dp),
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = volumeScale
                        scaleY = volumeScale
                    }
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = volumeInteraction,
                        indication = null,
                        onClick = hapticClick(HapticFeedbackType.ContextClick, onOpenAudioControls),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val volTint = if (isCustomAudio) MaterialTheme.colorScheme.primary else PlayerPalette.ink.copy(alpha = 0.82f)
                Icon(
                    imageVector = if (isCustomAudio) Icons.Filled.Tune else Icons.Filled.VolumeUp,
                    contentDescription = "Volume and audio controls",
                    tint = volTint,
                    modifier = Modifier.size(22.dp),
                )
                if (isCustomAudio) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 5.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(volTint),
                    )
                }
            }
        }
    }
}

/** Shared compact/medium quality badge for a live [MusicPlayerState]. */
@Composable
internal fun CompactStreamQualityBadge(
    state: MusicPlayerState,
    modifier: Modifier = Modifier,
    neutralContent: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    neutralContainer: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    medium: Boolean = false,
) {
    val qualityTier = remember(state.audioCodec, state.isLossless, state.samplingRateKHz, state.bitDepth) {
        resolveAudioQualityTier(state.audioCodec, state.isLossless, state.samplingRateKHz, state.bitDepth)
    }
    val mixing = state.deckBlendTelemetry.isBlendingActive || state.deckBlendTelemetry.phase == BlendPhase.MIX_COMPLETED
    when {
        mixing -> QualityBadge("AUTOMIX", QualityGlyphs.AutoMix, neutralContent, neutralContainer, modifier, compact = !medium, medium = medium)
        state.isCrossfading -> QualityBadge("CROSSFADE", QualityGlyphs.Crossfade, neutralContent, neutralContainer, modifier, compact = !medium, medium = medium)
        isSpatialAudioCodec(state.audioCodec) -> QualityTierBadge(LosslessListTier.ATMOS, modifier, compact = !medium, medium = medium)
        qualityTier == AudioQualityTier.HI_RES -> QualityTierBadge(LosslessListTier.HI_RES, modifier, compact = !medium, medium = medium)
        qualityTier == AudioQualityTier.LOSSLESS -> QualityTierBadge(LosslessListTier.LOSSLESS, modifier, compact = !medium, medium = medium)
        else -> {
            val label = lossyCodecLabel(state) ?: return
            QualityBadge(label, QualityGlyphs.Standard, neutralContent, neutralContainer, modifier, compact = !medium, medium = medium)
        }
    }
}

@Composable
internal fun MinimalInlineQualityLabel(
    state: MusicPlayerState,
    onOpenSignalPath: () -> Unit,
    onCycleAbLoop: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val neutralContent = PlayerPalette.ink.copy(alpha = 0.86f)
    val neutralContainer = PlayerPalette.ink.copy(alpha = 0.11f)
    val switching = state.qualitySwitch != null
    val pulse = if (switching) {
        rememberInfiniteTransition(label = "minQualityPulse").animateFloat(
            initialValue = 1f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
            label = "minQualityAlpha",
        ).value
    } else {
        1f
    }
    val onDarkBackdrop = PlayerPalette.shade.luminance() < 0.5f

    CompositionLocalProvider(LocalBadgeOnDarkBackdrop provides onDarkBackdrop) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .clip(RoundedCornerShape(6.dp))
                .combinedClickable(
                    onClickLabel = "Signal path",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenSignalPath()
                    },
                    onLongClick = onCycleAbLoop?.let { cycle ->
                        {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            cycle()
                        }
                    },
                )
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            CompactStreamQualityBadge(
                state = state,
                neutralContent = neutralContent,
                neutralContainer = neutralContainer,
                medium = true,
                modifier = Modifier.graphicsLayer { alpha = pulse },
            )
        }
    }
}

@Composable
internal fun MinimalPlaybackControls(
    state: MusicPlayerState,
    player: MusicPlayer,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val prevInteraction = remember { MutableInteractionSource() }
    val playInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }
    val isPrevPressed by prevInteraction.collectIsPressedAsState()
    val isPlayPressed by playInteraction.collectIsPressedAsState()
    val isNextPressed by nextInteraction.collectIsPressedAsState()
    val prevScale by animateFloatAsState(if (isPrevPressed) 0.86f else 1f, ExpressiveMotion.spatialSpring(), label = "minPrevScale")
    val playScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "minPlayScale",
    )
    val nextScale by animateFloatAsState(if (isNextPressed) 0.86f else 1f, ExpressiveMotion.spatialSpring(), label = "minNextScale")

    val motionScope = rememberCoroutineScope()
    val prevNudge = remember { Animatable(0f) }
    val nextNudge = remember { Animatable(0f) }
    val shuffleFlip = remember { Animatable(1f) }
    var lastShuffle by remember { mutableStateOf(state.shuffleEnabled) }
    LaunchedEffect(state.shuffleEnabled) {
        if (state.shuffleEnabled == lastShuffle) return@LaunchedEffect
        lastShuffle = state.shuffleEnabled
        shuffleFlip.snapTo(0f)
        shuffleFlip.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
    }
    var repeatSpins by remember { mutableIntStateOf(0) }
    var lastRepeat by remember { mutableIntStateOf(state.repeatMode) }
    val repeatPop = remember { Animatable(0f) }
    LaunchedEffect(state.repeatMode) {
        if (state.repeatMode == lastRepeat) return@LaunchedEffect
        lastRepeat = state.repeatMode
        repeatSpins++
        if (state.repeatMode == Player.REPEAT_MODE_ONE) repeatPop.kick()
    }
    val repeatRotation by animateFloatAsState(
        repeatSpins * 360f,
        spring(dampingRatio = 0.6f, stiffness = 180f),
        label = "minRepeatSpin",
    )

    val shuffleTint by animateColorAsState(
        targetValue = if (state.shuffleEnabled) MaterialTheme.colorScheme.primary else PlayerPalette.ink.copy(alpha = 0.66f),
        animationSpec = tween(180),
        label = "minShuffleTint",
    )
    val repeatActive = state.repeatMode != Player.REPEAT_MODE_OFF
    val repeatTint by animateColorAsState(
        targetValue = if (repeatActive) MaterialTheme.colorScheme.primary else PlayerPalette.ink.copy(alpha = 0.66f),
        animationSpec = tween(180),
        label = "minRepeatTint",
    )
    val bareInk = PlayerPalette.ink.copy(alpha = 0.96f)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = hapticClick(if (state.shuffleEnabled) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn, player::toggleShuffle),
            modifier = Modifier
                .size(44.dp)
                .semantics {
                    selected = state.shuffleEnabled
                    stateDescription = if (state.shuffleEnabled) "Shuffle on" else "Shuffle off"
                },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = if (state.shuffleEnabled) "Shuffle on" else "Shuffle off",
                    tint = shuffleTint,
                    modifier = Modifier.size(21.dp).graphicsLayer {
                        val v = shuffleFlip.value
                        scaleX = kotlin.math.cos(2.0 * Math.PI * v).toFloat()
                        translationY = -3.dp.toPx() * kotlin.math.sin(Math.PI * v).toFloat()
                    },
                )
                if (state.shuffleEnabled) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 5.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(shuffleTint),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(50.dp)
                .graphicsLayer {
                    scaleX = prevScale
                    scaleY = prevScale
                }
                .clip(CircleShape)
                .clickable(
                    interactionSource = prevInteraction,
                    indication = null,
                    onClick = hapticClick(action = { player.previous(); motionScope.launch { prevNudge.kick() } }),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                tint = bareInk,
                modifier = Modifier.size(if (compact) 30.dp else 34.dp).graphicsLayer {
                    translationX = -7.dp.toPx() * prevNudge.value
                },
            )
        }

        Box(
            modifier = Modifier
                .size(if (compact) 54.dp else 62.dp)
                .graphicsLayer {
                    scaleX = playScale
                    scaleY = playScale
                }
                .clip(CircleShape)
                .clickable(
                    interactionSource = playInteraction,
                    indication = null,
                    onClick = hapticClick(if (state.isPlaying) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn, player::togglePlayPause),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (state.isBuffering) {
                ExpressiveInlineLoadingIndicator(
                    size = if (compact) 30.dp else 34.dp,
                    color = bareInk,
                    strokeWidth = 3.dp,
                )
            } else {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalContentColor provides bareInk,
                ) {
                    AnimatedPlayPauseIcon(
                        isPlaying = state.isPlaying,
                        modifier = Modifier.size(if (compact) 36.dp else 44.dp),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(50.dp)
                .graphicsLayer {
                    scaleX = nextScale
                    scaleY = nextScale
                }
                .clip(CircleShape)
                .clickable(
                    interactionSource = nextInteraction,
                    indication = null,
                    onClick = hapticClick(action = { player.next(); motionScope.launch { nextNudge.kick() } }),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next",
                tint = bareInk,
                modifier = Modifier.size(if (compact) 30.dp else 34.dp).graphicsLayer {
                    translationX = 7.dp.toPx() * nextNudge.value
                },
            )
        }

        IconButton(
            onClick = hapticClick(
                if (state.repeatMode == Player.REPEAT_MODE_ONE) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
                player::cycleRepeatMode,
            ),
            modifier = Modifier
                .size(44.dp)
                .semantics {
                    selected = repeatActive
                    stateDescription = when (state.repeatMode) {
                        Player.REPEAT_MODE_ONE -> "Repeat one"
                        Player.REPEAT_MODE_ALL -> "Repeat all"
                        else -> "Repeat off"
                    }
                },
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = when (state.repeatMode) {
                        Player.REPEAT_MODE_ONE -> "Repeat one"
                        Player.REPEAT_MODE_ALL -> "Repeat all"
                        else -> "Repeat off"
                    },
                    tint = repeatTint,
                    modifier = Modifier.size(21.dp).graphicsLayer {
                        rotationZ = repeatRotation
                        val s = 1f + 0.25f * repeatPop.value
                        scaleX = s
                        scaleY = s
                    },
                )
                if (repeatActive) {
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 5.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(repeatTint),
                    )
                }
            }
        }
    }
}

@Composable
internal fun MinimalBottomActionRow(
    hasLyrics: Boolean,
    onShowLyrics: () -> Unit,
    onShowQueue: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleVideo: (() -> Unit)? = null,
    isVideoActive: Boolean = false,
    downloadState: NowPlayingDownloadState = NowPlayingDownloadState(),
    onDownload: (() -> Unit)? = null,
) {
    val isDownloaded = downloadState.status == com.lastwave.app.ui.common.TrackDownloadStatus.DOWNLOADED
    val isDownloading = downloadState.status == com.lastwave.app.ui.common.TrackDownloadStatus.DOWNLOADING
    val items = buildList {
        if (onToggleVideo != null) {
            add(
                DockActionItem(
                    key = "video",
                    icon = Icons.Filled.SmartDisplay,
                    label = if (isVideoActive) "Switch to album art" else "Watch music video",
                    showBadgeDot = isVideoActive,
                    comicAccent = ComicPink,
                    isHighlighted = isVideoActive,
                    onClick = onToggleVideo,
                ),
            )
        }
        add(
            DockActionItem(
                key = "queue",
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                label = stringResource(R.string.player_queue),
                showBadgeDot = false,
                comicAccent = ComicCyan,
                isHighlighted = false,
                onClick = onShowQueue,
            ),
        )
        add(
            DockActionItem(
                key = "lyrics",
                icon = Icons.Filled.FormatQuote,
                label = stringResource(R.string.player_lyrics),
                showBadgeDot = hasLyrics,
                comicAccent = ComicMint,
                isHighlighted = false,
                onClick = onShowLyrics,
            ),
        )
        if (onDownload != null && (!isDownloaded || downloadState.openInLibrary)) {
            add(
                DockActionItem(
                    key = "download",
                    icon = if (isDownloaded) Icons.Filled.LibraryMusic else Icons.Filled.Download,
                    label = if (isDownloaded) stringResource(R.string.player_download_library) else stringResource(R.string.player_download),
                    showBadgeDot = isDownloaded,
                    comicAccent = ComicAccent,
                    isHighlighted = isDownloading || isDownloaded,
                    isLoading = isDownloading,
                    progressPercent = downloadState.progressPercent,
                    succeeded = isDownloaded,
                    onClick = onDownload,
                ),
            )
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            MinimalBottomGlyph(item = item)
        }
    }
}

@Composable
private fun MinimalBottomGlyph(item: DockActionItem) {
    val shownProgress by animateFloatAsState(item.progressPercent / 100f, tween(300), label = "minDlProgress")
    val tick = remember { Animatable(0f) }
    var showTick by remember { mutableStateOf(false) }
    var wasLoading by remember { mutableStateOf(item.isLoading) }
    LaunchedEffect(item.isLoading) {
        val finished = wasLoading && !item.isLoading && item.succeeded
        wasLoading = item.isLoading
        if (finished) {
            showTick = true
            tick.snapTo(0f)
            tick.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
            delay(700L)
            showTick = false
        }
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.86f else 1f, ExpressiveMotion.spatialFast(), label = "minBottomScale")
    val tint = if (item.isHighlighted) MaterialTheme.colorScheme.primary else PlayerPalette.ink.copy(alpha = 0.80f)

    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = hapticClick(action = item.onClick),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (showTick) {
            DrawInTick(progress = { tick.value }, color = tint, modifier = Modifier.size(20.dp))
        } else if (item.isLoading) {
            if (item.progressPercent in 1..99) {
                CircularProgressIndicator(
                    progress = { shownProgress },
                    modifier = Modifier.size(19.dp),
                    color = tint,
                    trackColor = tint.copy(alpha = 0.22f),
                    strokeWidth = 2.dp,
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(19.dp),
                    color = tint,
                    strokeWidth = 2.dp,
                )
            }
        } else {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = tint,
                modifier = Modifier.size(21.dp),
            )
        }
        if (item.showBadgeDot) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (item.isHighlighted) tint else MaterialTheme.colorScheme.primary),
            )
        }
    }
}
