@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.lastwave.app.ui.player

import com.lastwave.app.ui.theme.PlayerPalette
import com.lastwave.app.ui.theme.comicNightWash
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
import androidx.compose.ui.layout.onSizeChanged
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
import kotlin.math.abs
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
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import com.lastwave.app.ui.comic.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
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
import com.lastwave.app.ui.theme.LocalLiquidGlass
import com.lastwave.app.ui.theme.LiquidGlassSurface
import com.lastwave.app.ui.theme.liquidGlass
import com.lastwave.app.ui.theme.liquidGlassChrome
import com.lastwave.app.ui.theme.liquidGlassContainerColor
import com.lastwave.app.ui.theme.liquidGlassSource
import com.lastwave.app.ui.theme.isLiquidGlassBackdropSupported
import com.lastwave.app.ui.theme.LocalLiquidGlassBackdrop
import com.lastwave.app.ui.theme.LocalLiquidGlassTint
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


@Composable
internal fun FullPlayer(
    state: MusicPlayerState,
    progressState: StateFlow<PlaybackProgressState>,
    player: MusicPlayer,
    lyricsState: LyricsUiState,
    wavySeekbarEnabled: Boolean = false,
    playerActionDockEnabled: Boolean = true,
    rotatingBackgroundEnabled: Boolean = true,
    lyricsRotatingBackgroundEnabled: Boolean = false,
    lyricsOffsetMs: Long = 0L,
    onSetLyricsOffsetMs: ((Long) -> Unit)? = null,
    lyricsFontScale: Float = 1.0f,
    onSetLyricsFontScale: ((Float) -> Unit)? = null,
    /** 0 = floating lyrics buttons never auto-hide; 2..10 = idle seconds. */
    lyricsButtonsAutoHideSeconds: Int = 5,
    /** Language the lyric translate button targets; blank = app language. */
    translationLanguage: String = "",
    lyricsAutoRomanize: Boolean = true,
    nowPlayingLyricsPreview: Boolean = true,
    canvas: com.lastwave.app.data.canvas.CanvasArtwork? = null,
    canvasEnabled: Boolean = true,
    canvasFullBleedEnabled: Boolean = false,
    canvasGradientBlurEnabled: Boolean = false,
    musicVideoEnabled: Boolean = true,
    showAutoEqInVolumePanel: Boolean = true,
    currentTab: FullPlayerTab,
    onTabChange: (FullPlayerTab) -> Unit,
    onRetryLyrics: () -> Unit,
    onCollapse: () -> Unit,
    onOpenArtist: (String) -> Unit = {},
    isLiked: Boolean = false,
    onToggleLiked: () -> Unit = {},
    onDoubleTapLike: () -> Unit = {},
    onToggleFullArtwork: () -> Unit = {},
    youTubeLiked: Boolean? = null,
    onToggleYouTubeLike: () -> Unit = {},
    onDragStatusChange: (Boolean) -> Unit = {},
    isOpen: Boolean = true,
    lyricsProviderStatuses: Map<com.lastwave.app.data.local.LyricsProvider, LyricsSyncType> = emptyMap(),
    onProbeLyricsProviders: (() -> Unit)? = null,
    onSelectLyricsProvider: ((com.lastwave.app.data.local.LyricsProvider) -> Unit)? = null,
    onTryAnotherLyricsSource: (() -> Unit)? = null,
    equalizerSettings: com.lastwave.app.data.local.EqualizerSettings = com.lastwave.app.data.local.EqualizerSettings(),
    onSetEqualizerEnabled: (Boolean) -> Unit = {},
    onApplyEqPreset: (String) -> Unit = {},
    onPreviewEqBandGain: (Int, Float) -> Unit = { _, _ -> },
    onSetEqBandGain: (Int, Float) -> Unit = { _, _ -> },
    downloadState: NowPlayingDownloadState = NowPlayingDownloadState(),
    onDownload: (() -> Unit)? = null,
) {
    val track = state.current ?: return
    val haptics = LocalHapticFeedback.current
    val windowActive = rememberPlayerWindowActive()
    val onDoubleTapLike = hapticClick(HapticFeedbackType.Confirm, onDoubleTapLike)
    var showLyricsOffsetDialog by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isTablet = isTabletOrWideScreen()
    val isWideOrShortLandscape = isLandscape && (isTablet || configuration.screenWidthDp >= 520 || configuration.screenHeightDp < 540)
    val isTabletLandscape = isWideOrShortLandscape || (configuration.screenWidthDp >= 540 && configuration.screenWidthDp > configuration.screenHeightDp * 1.12f)
    val isCanvasActive = canvasEnabled && canvas != null
    val syncedVideoVm: SyncedVideoViewModel = hiltViewModel()
    val showSyncedVideo by syncedVideoVm.isVideoActive.collectAsStateWithLifecycle()
    val syncedVideoState by syncedVideoVm.uiState.collectAsStateWithLifecycle()
    val canWatchVideo = !syncedVideoVm.isVideoUnavailableFor(
        title = track.title,
        artist = track.artist,
        videoId = track.videoId,
        durationMs = track.durationMs,
        state = syncedVideoState,
    )
    val onToggleSyncedVideo: (() -> Unit)? = if (canWatchVideo || showSyncedVideo) {
        { syncedVideoVm.setVideoActive(!showSyncedVideo) }
    } else null
    val isVideoActive = showSyncedVideo && musicVideoEnabled
    LaunchedEffect(track.videoId, track.title, track.durationMs, musicVideoEnabled) {
        if (!musicVideoEnabled || track.artworkUrl?.startsWith("file://") == true) return@LaunchedEffect
        // Waits out rapid skips, so only a song the user stays on spends the lookup.
        delay(VIDEO_PRELOAD_DELAY_MS)
        syncedVideoVm.preloadVideo(track.title, track.artist, track.videoId, track.durationMs)
    }
    // Portrait: edge-to-edge artwork only with the "Full Screen Artwork" setting on;
    // otherwise a square sleeve on the cover's colour blend, as in tablet landscape.
    val showFullBleed = canvasFullBleedEnabled && !isVideoActive && !isTabletLandscape
    val showSleeveCanvas = isCanvasActive && !showFullBleed && !isVideoActive
    // activeCanvas is the single gate every render branch below keys off, so it also
    // carries the Animated Album Canvas toggle. The ViewModel nulls canvasState
    // asynchronously, so without this a disabled toggle still paints one frame of
    // motion artwork over the cover.
    val activeCanvas = remember(canvas, showFullBleed, canvasEnabled) {
        if (!canvasEnabled) {
            null
        } else {
            val tall = canvas?.tallUrl
            if (showFullBleed && canvas != null && !tall.isNullOrBlank()) {
                canvas.copy(url = tall)
            } else {
                canvas
            }
        }
    }
    @Suppress("UNUSED_VARIABLE")
    var canvasAspect by remember(activeCanvas?.url) { mutableFloatStateOf(0f) }
    // How much of the hero the canvas currently covers â€” its own fade-in alpha
    // times the presentation alpha. The static hero fades out in step with it,
    // so the cover never shows through the video (its bottom fade, its 320 ms
    // reveal, the repaint window on a track change) and the two never fight for
    // the same pixels. A disposed canvas reports 0, so the hero returns the
    // moment a track without a canvas starts.
    var canvasCover by remember { mutableFloatStateOf(0f) }
    // Keep header and back button visible when opening Lyrics on car head units / landscape tablets;
    // the user can still enter distraction-free fullscreen via the Fullscreen button in lyrics controls.
    var lyricsFullscreen by remember(currentTab) {
        mutableStateOf(false)
    }
    var lyricsControlsVisible by remember(currentTab) { mutableStateOf(true) }
    val view = LocalView.current
    DisposableEffect(view, lyricsFullscreen) {
        val fullscreenActive = lyricsFullscreen
        val activity = generateSequence(view.context) { (it as? android.content.ContextWrapper)?.baseContext }
            .filterIsInstance<android.app.Activity>().firstOrNull()
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        val previousBehavior = controller?.systemBarsBehavior
        if (fullscreenActive) {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (fullscreenActive) {
                controller?.show(WindowInsetsCompat.Type.systemBars())
                previousBehavior?.let { controller?.systemBarsBehavior = it }
            }
        }
    }
    var isVideoFullscreen by remember(track.videoId, track.title) { mutableStateOf(false) }
    val videoFullscreenActive = isVideoFullscreen && isVideoActive
    DisposableEffect(view, videoFullscreenActive) {
        val activity = generateSequence(view.context) { (it as? android.content.ContextWrapper)?.baseContext }
            .filterIsInstance<android.app.Activity>().firstOrNull()
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        val previousBehavior = controller?.systemBarsBehavior
        val originalOrientation = activity?.requestedOrientation
        if (videoFullscreenActive) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (videoFullscreenActive) {
                controller?.show(WindowInsetsCompat.Type.systemBars())
                previousBehavior?.let { controller?.systemBarsBehavior = it }
                if (originalOrientation != null) {
                    activity?.requestedOrientation = originalOrientation
                }
            }
        }
    }
    var showTrackMenu by remember(track.videoId, track.title) { mutableStateOf(false) }
    var showSignalPath by remember { mutableStateOf(false) }
    var showAudioControls by remember { mutableStateOf(false) }
    var showAutoEq by remember { mutableStateOf(false) }
    var showOutputSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var isDistractionFree by remember { mutableStateOf(false) }
    LaunchedEffect(currentTab) {
        if (currentTab != FullPlayerTab.NOW_PLAYING) {
            isDistractionFree = false
        }
    }
    val signalPath by player.signalPath.collectAsStateWithLifecycle()
    val usbDac by player.usbDacState.collectAsStateWithLifecycle()
    val internalAvailability by player.internalDirectAvailability.collectAsStateWithLifecycle()
    val isCustomAudio = state.speed != 1f || state.pitch != 1f
    var dismissDragY by remember(track.videoId, track.title) { mutableFloatStateOf(0f) }
    var isDismissDragging by remember { mutableStateOf(false) }
    val shownDismissY by animateFloatAsState(
        targetValue = dismissDragY,
        animationSpec = if (isDismissDragging) snap() else ExpressiveMotion.spatialSpring(),
        label = "fullPlayerDismissY",
    )
    // The host stops drawing the shell while the player covers it. After the finger
    // lifts the sheet is still offset while it springs back, so report until it lands.
    val sheetMoving by remember { derivedStateOf { isDismissDragging || shownDismissY > 0.5f } }
    LaunchedEffect(sheetMoving) { onDragStatusChange(sheetMoving) }
    // A swipe-close keeps its offset for the close morph; reopening before that
    // morph ends kept it too and left the player parked at the bottom.
    LaunchedEffect(isOpen) { if (isOpen) dismissDragY = 0f }
    val swipeThreshold = with(LocalDensity.current) { 88.dp.toPx() }

    // Dominant cover-art color for the ambient background glow. Extracted
    // once per track through the shared Coil loader (normally a cache hit)
    // with Palette; any failure leaves the standard surface gradient.
    val context = LocalContext.current
    val artworkViewModel: ArtworkViewModel = hiltViewModel()
    val artworkKey = remember(track.title, track.artist) { ArtworkNormalizer.cacheKey(track.title, track.artist) }
    val onlineAmbientUrl by remember(artworkKey) {
        artworkViewModel.resolved.map { it[artworkKey] }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = artworkViewModel.resolved.value[artworkKey])
    // Local songs: the cover embedded in the user's file wins over online art.
    val resolvedAmbientUrl = if (track.artworkUrl?.startsWith("file://") == true) null else onlineAmbientUrl

    var ambientPrimary by remember(track.videoId, track.artworkUrl, track.title, track.artist) { mutableStateOf<Color?>(null) }
    var ambientSecondary by remember(track.videoId, track.artworkUrl, track.title, track.artist) { mutableStateOf<Color?>(null) }
    var ambientTertiary by remember(track.videoId, track.artworkUrl, track.title, track.artist) { mutableStateOf<Color?>(null) }
    LaunchedEffect(track.videoId, track.artworkUrl, track.title, track.artist, resolvedAmbientUrl) {
        val url = resolvedAmbientUrl?.takeIf { it.isNotBlank() }
            ?: ArtworkNormalizer.upscaleYoutubeArtwork(track.artworkUrl)?.takeIf { it.isNotBlank() }
            ?: return@LaunchedEffect
        val cached = ambientPaletteCache.get(url)
        if (cached != null) {
            ambientPrimary = cached.first
            ambientSecondary = cached.second
            ambientTertiary = cached.third
            return@LaunchedEffect
        }
        withContext(Dispatchers.Default) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .size(128)
                    .build()
                val bitmap = ((context.imageLoader.execute(request) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
                    ?: return@runCatching
                val palette = Palette.from(bitmap).clearFilters().generate()
                val p = palette.vibrantSwatch ?: palette.dominantSwatch ?: palette.mutedSwatch
                val s = palette.lightVibrantSwatch ?: palette.darkVibrantSwatch ?: palette.mutedSwatch ?: palette.dominantSwatch
                val t = palette.darkVibrantSwatch ?: palette.darkMutedSwatch ?: palette.dominantSwatch
                val cP = p?.let { Color(it.rgb) }
                val cS = s?.let { Color(it.rgb) }
                val cT = t?.let { Color(it.rgb) }
                if (cP != null) ambientPrimary = cP
                if (cS != null) ambientSecondary = cS
                if (cT != null) ambientTertiary = cT
                if (cP != null || cS != null || cT != null) {
                    ambientPaletteCache.put(
                        url,
                        Triple(
                            cP ?: Color.Transparent,
                            cS ?: Color.Transparent,
                            cT ?: Color.Transparent,
                        ),
                    )
                }
            }
        }
    }
    val ambientColor by animateColorAsState(
        targetValue = ambientPrimary ?: MaterialTheme.colorScheme.primary,
        animationSpec = tween(700),
        label = "playerAmbientColor",
    )
    val ambientCompanion by animateColorAsState(
        targetValue = ambientSecondary ?: androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.tertiary, ambientColor, 0.42f),
        animationSpec = tween(700),
        label = "playerAmbientCompanion",
    )
    val ambientDeep by animateColorAsState(
        targetValue = ambientTertiary ?: androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.secondary, ambientColor, 0.25f),
        animationSpec = tween(700),
        label = "playerAmbientDeep",
    )

    fun Modifier.playerVerticalSwipe(enabled: Boolean): Modifier = if (!enabled) this else pointerInput(track.videoId, track.title, currentTab) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var slopY = 0f
            var dragging = false
            while (true) {
                // Final pass: a child that scrolled with this move (the lyrics list)
                // has consumed it by now, and then the gesture stays the child's.
                val change = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val dy = change.positionChange().y
                if (!dragging) {
                    if (change.isConsumed) return@awaitEachGesture
                    slopY += dy
                    if (abs(slopY) < viewConfiguration.touchSlop) continue
                    dragging = true
                    isDismissDragging = true
                }
                val updatedDrag = if (currentTab != FullPlayerTab.NOW_PLAYING) {
                    (dismissDragY + dy).coerceAtLeast(0f)
                } else {
                    dismissDragY + dy
                }
                if (updatedDrag != dismissDragY) change.consume()
                dismissDragY = updatedDrag
            }
            if (!dragging) return@awaitEachGesture
            isDismissDragging = false
            var collapsing = false
            when {
                currentTab == FullPlayerTab.QUEUE && dismissDragY > swipeThreshold -> onTabChange(FullPlayerTab.NOW_PLAYING)
                currentTab == FullPlayerTab.NOW_PLAYING && dismissDragY < -swipeThreshold -> onTabChange(FullPlayerTab.QUEUE)
                dismissDragY > swipeThreshold -> {
                    collapsing = true
                    onCollapse()
                }
            }
            // Collapsing keeps the dragged offset: the close morph carries
            // the player away from there instead of it springing back first.
            if (!collapsing) dismissDragY = 0f
        }
    }

    // Unconditional remember keeps composition stable; usage gated by glass flag.
    // Single backdrop, sibling source below â€” never nest glass inside its own capture.
    val playerBackdrop = rememberLayerBackdrop()
    val fullGlass = LocalLiquidGlass.current && isLiquidGlassBackdropSupported()

    if (videoFullscreenActive) {
        BackHandler { isVideoFullscreen = false }
        FullscreenLandscapeVideoStage(
            track = track,
            musicPlayer = player,
            onMinimize = { isVideoFullscreen = false },
            onCloseVideo = {
                isVideoFullscreen = false
                syncedVideoVm.setVideoActive(false)
            },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }
    CompositionLocalProvider(
        LocalLiquidGlassBackdrop provides if (fullGlass) playerBackdrop else null,
        LocalLiquidGlassOverlayBackdrop provides if (fullGlass) playerBackdrop else null,
        LocalLiquidGlassTint provides if (fullGlass) ambientColor else Color.Unspecified,
    ) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = shownDismissY.coerceAtLeast(0f)
                val playerHeight = size.height.coerceAtLeast(1f)
                alpha = (1f - shownDismissY.coerceAtLeast(0f) / (playerHeight * 1.5f)).coerceIn(0.72f, 1f)
            }
            // Lyrics too: its list claims vertical drags first, so swiping down
            // anywhere else (art pane, empty state, controls) closes the player.
            .playerVerticalSwipe(enabled = currentTab != FullPlayerTab.QUEUE),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val bgWidth = constraints.maxWidth.toFloat()
            val bgHeight = constraints.maxHeight.toFloat()
            val bgMaxDimension = maxOf(bgWidth, bgHeight, 1f)

            val isLyricsTab = currentTab == FullPlayerTab.LYRICS
            val showLyricsFluid = lyricsRotatingBackgroundEnabled && rotatingBackgroundEnabled
            // Lyrics and the queue keep the cover's own colour blend behind them (as
            // Now Playing does), in every orientation, instead of the flat page / veil.
            val lyricsOnArtworkColors = isLyricsTab || currentTab == FullPlayerTab.QUEUE
            val lyricsVeil by animateFloatAsState(
                targetValue = if (isLyricsTab && !showLyricsFluid && !lyricsOnArtworkColors) 1f else 0f,
                animationSpec = tween(350),
                label = "lyricsVeil",
            )
            val comicPageOverAll by animateFloatAsState(
                targetValue = if (currentTab == FullPlayerTab.NOW_PLAYING || lyricsOnArtworkColors) 0f else 1f,
                animationSpec = tween(280),
                label = "comicPageOverAll",
            )
            val readabilityAlpha by animateFloatAsState(
                targetValue = if (isDistractionFree && currentTab == FullPlayerTab.NOW_PLAYING) 0.14f else 1f,
                animationSpec = tween(240),
                label = "readabilityAlpha",
            )
            Box(
                Modifier
                    .matchParentSize()
                    .liquidGlassSource(if (fullGlass) playerBackdrop else null),
            ) {
                FullScreenArtworkBackground(
                    track = track,
                    isPlaying = state.isPlaying,
                    currentTab = currentTab,
                    isVideoActive = isVideoActive,
                    showFullBleed = showFullBleed,
                    rotatingBackgroundEnabled = rotatingBackgroundEnabled,
                    lyricsRotatingBackgroundEnabled = lyricsRotatingBackgroundEnabled,
                    lyricsState = lyricsState,
                    progressState = progressState,
                    lyricsOffsetMs = lyricsOffsetMs,
                    resolvedAmbientUrl = resolvedAmbientUrl,
                    activeCanvas = activeCanvas,
                    canvasCover = canvasCover,
                    onCanvasCoverChanged = { canvasCover = it },
                    onCanvasAspectChanged = { canvasAspect = it },
                    pausedForTransition = shownDismissY > 1f || currentTab != FullPlayerTab.NOW_PLAYING,
                    ambientColor = ambientColor,
                    ambientCompanion = ambientCompanion,
                    ambientDeep = ambientDeep,
                    bgWidth = bgWidth,
                    bgHeight = bgHeight,
                    bgMaxDimension = bgMaxDimension,
                    lyricsVeil = lyricsVeil,
                    comicPageOverAll = comicPageOverAll,
                    readabilityAlpha = readabilityAlpha,
                    lyricsOnArtworkColors = lyricsOnArtworkColors,
                )
            }
            // Bottom inset is applied per tab below, so the queue can run under the gesture pill.
            val showError = state.error != null && !lyricsFullscreen
            val bottomInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
            val tabBottomInset = if (showError) 0.dp else bottomInset
            // Tablet landscape centres the cover sleeve on the screen, not on the
            // area under the header: lift it by half the top/bottom imbalance.
            val statusTopInset = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
            var headerHeightPx by remember { mutableIntStateOf(0) }
            val headerHeight = with(LocalDensity.current) { headerHeightPx.toDp() }
            // Keep 0.dp lift on short viewports (e.g., car head units) so the artwork sleeve never shifts up into NowPlayingHeader.
            val landscapeArtLift = if (configuration.screenHeightDp < 640) {
                0.dp
            } else {
                ((statusTopInset + headerHeight - tabBottomInset) / 2).coerceAtLeast(0.dp)
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Header
                val showHeader = !lyricsFullscreen &&
                    !(isDistractionFree && currentTab == FullPlayerTab.NOW_PLAYING && !showTrackMenu) &&
                    (currentTab != FullPlayerTab.LYRICS || lyricsControlsVisible || showLyricsOffsetDialog || showTrackMenu)
                AnimatedVisibility(
                    visible = showHeader,
                    enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                    exit = fadeOut(tween(200)) + shrinkVertically(tween(200)),
                    modifier = Modifier.onSizeChanged { headerHeightPx = it.height },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isTabletLandscape) {
                                    Modifier.widthIn(max = 1200.dp).fillMaxWidth()
                                } else {
                                    Modifier.adaptiveContentWidth(maxWidth = 640.dp)
                                }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        NowPlayingHeader(
                            currentTab = currentTab,
                            track = track,
                            sourceLabel = state.sourceLabel,
                            isPlaying = state.isPlaying,
                            lyricsOffsetMs = lyricsOffsetMs,
                            showLyricsOffsetAction = currentTab == FullPlayerTab.LYRICS && onSetLyricsOffsetMs != null,
                            horizontalPadding = if (isTabletLandscape) 32.dp else 20.dp,
                            onBackOrCollapse = {
                                if (currentTab != FullPlayerTab.NOW_PLAYING) {
                                    onTabChange(FullPlayerTab.NOW_PLAYING)
                                } else {
                                    onCollapse()
                                }
                            },
                            onOpenLyricsOffset = { showLyricsOffsetDialog = true },
                            onOpenTrackMenu = { showTrackMenu = true },
                            modifier = Modifier.playerVerticalSwipe(enabled = currentTab == FullPlayerTab.QUEUE),
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }

                AnimatedContent(
                    targetState = currentTab,
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (isTabletLandscape) {
                                Modifier.widthIn(max = 1200.dp).fillMaxWidth()
                            } else {
                                Modifier.adaptiveContentWidth(maxWidth = 680.dp)
                            }
                        ),
                    transitionSpec = {
                        // Matched fade durations keep both layers visible
                        // throughout the crossfade so the background never
                        // flashes through between cover and canvas/lyrics.
                        // Same language as the player open/close: the panel
                        // rises and grows into place like a card while the
                        // one it covers recedes, and the reverse going back.
                        val dur = 280
                        val ease = tween<Float>(dur, easing = FastOutSlowInEasing)
                        if (targetState != FullPlayerTab.NOW_PLAYING) {
                            (slideInVertically(tween(dur, easing = FastOutSlowInEasing)) { it / 8 } +
                                scaleIn(ease, initialScale = 0.92f, transformOrigin = TransformOrigin(0.5f, 1f)) +
                                fadeIn(tween(dur))) togetherWith
                                (scaleOut(ease, targetScale = 0.94f, transformOrigin = TransformOrigin(0.5f, 0f)) +
                                    fadeOut(tween(dur)))
                        } else {
                            (scaleIn(ease, initialScale = 0.94f, transformOrigin = TransformOrigin(0.5f, 0f)) +
                                fadeIn(tween(dur))) togetherWith
                                (slideOutVertically(tween(dur, easing = FastOutSlowInEasing)) { it / 8 } +
                                    scaleOut(ease, targetScale = 0.92f, transformOrigin = TransformOrigin(0.5f, 1f)) +
                                    fadeOut(tween(dur)))
                        }
                    },
                    label = "playerTabContent",
                ) { tab ->
                    Box(Modifier.padding(bottom = if (tab == FullPlayerTab.QUEUE) 0.dp else tabBottomInset)) {
                    when (tab) {
                        FullPlayerTab.LYRICS -> {
                            PredictiveBackScreen(
                                enabled = currentTab == FullPlayerTab.LYRICS,
                                backgroundColor = Color.Transparent,
                                onBack = {
                                    // Tablet landscape opens lyrics straight into fullscreen,
                                    // so back returns to Now Playing in one step there.
                                    if (lyricsFullscreen && !isTabletLandscape) {
                                        lyricsFullscreen = false
                                    } else {
                                        lyricsFullscreen = false
                                        onTabChange(FullPlayerTab.NOW_PLAYING)
                                    }
                                },
                            ) {
                                ModernLyricsPanel(
                                    state = state,
                                    player = player,
                                    lyricsState = lyricsState,
                                    lyricsProviderStatuses = lyricsProviderStatuses,
                                    onProbeLyricsProviders = onProbeLyricsProviders,
                                    progressState = progressState,
                                    wavySeekbarEnabled = wavySeekbarEnabled,
                                    lyricsOffsetMs = lyricsOffsetMs,
                                    lyricsFontScale = lyricsFontScale,
                                    onLyricsFontScaleChange = onSetLyricsFontScale ?: {},
                                    controlsAutoHideSeconds = lyricsButtonsAutoHideSeconds,
                                    controlsVisibleState = lyricsControlsVisible,
                                    onControlsVisibleChange = { lyricsControlsVisible = it },
                                    translationLanguage = translationLanguage,
                                    autoRomanize = lyricsAutoRomanize,
                                    onRetry = onRetryLyrics,
                                    onSelectLyricsProvider = onSelectLyricsProvider,
                                    onTryAnotherLyricsSource = onTryAnotherLyricsSource,
                                    onToggleFullscreen = { lyricsFullscreen = !lyricsFullscreen },
                                    isFullscreen = lyricsFullscreen,
                                    onOpenLyricsOffset = { showLyricsOffsetDialog = true },
                                    primaryColor = ambientColor,
                                    secondaryColor = ambientCompanion,
                                    tertiaryColor = ambientDeep,
                                    resolvedArtworkUrl = resolvedAmbientUrl,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .adaptiveContentWidth(maxWidth = if (isTabletLandscape) 960.dp else 720.dp),
                                )
                            }
                        }

                        FullPlayerTab.QUEUE -> {
                            PredictiveBackScreen(
                                enabled = currentTab == FullPlayerTab.QUEUE,
                                backgroundColor = Color.Transparent,
                                onBack = { onTabChange(FullPlayerTab.NOW_PLAYING) },
                            ) {
                                QueuePanel(
                                    state = state,
                                    player = player,
                                    bottomPadding = tabBottomInset,
                                    ambientColor = ambientColor,
                                    ambientCompanion = ambientCompanion,
                                    ambientDeep = ambientDeep,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .adaptiveContentWidth(maxWidth = if (isTabletLandscape) 960.dp else 720.dp)
                                        .padding(horizontal = 12.dp),
                                )
                            }
                        }

                        FullPlayerTab.NOW_PLAYING -> {
                            if (isTabletLandscape) {
                                FullPlayerLandscape(
                                    track = track,
                                    state = state,
                                    player = player,
                                    progressState = progressState,
                                    wavySeekbarEnabled = wavySeekbarEnabled,
                                    playerActionDockEnabled = playerActionDockEnabled,
                                    showSleeveCanvas = showSleeveCanvas,
                                    canvas = activeCanvas ?: canvas,
                                    ambientColor = ambientColor,
                                    ambientCompanion = ambientCompanion,
                                    ambientDeep = ambientDeep,
                                    isLiked = isLiked,
                                    onToggleLiked = onToggleLiked,
                                    onDoubleTapLike = onDoubleTapLike,
                                    youTubeLiked = youTubeLiked,
                                    onToggleYouTubeLike = onToggleYouTubeLike,
                                    onOpenArtist = onOpenArtist,
                                    onTabChange = onTabChange,
                                    shownDismissY = shownDismissY,
                                    swipeThreshold = swipeThreshold,
                                    onOpenSignalPath = { showSignalPath = true },
                                    onOpenAudioControls = { showAudioControls = true },
                                    musicVideoEnabled = musicVideoEnabled,
                                    isVideoActive = isVideoActive,
                                    onCloseVideo = { syncedVideoVm.setVideoActive(false) },
                                    onEnterFullscreen = { isVideoFullscreen = true },
                                    onToggleVideo = onToggleSyncedVideo,
                                    resolvedArtworkUrl = resolvedAmbientUrl,
                                    usbDac = usbDac,
                                    availability = internalAvailability,
                                    hasLyrics = lyricsState is LyricsUiState.Success && (lyricsState.lines.isNotEmpty() || !lyricsState.plainLyrics.isNullOrBlank()),
                                    isCustomAudio = isCustomAudio,
                                    artworkLift = landscapeArtLift,
                                    isEqEnabled = equalizerSettings.enabled,
                                    onOpenEqualizer = { showEqualizerSheet = true },
                                    onOpenOutput = { showOutputSheet = true },
                                    downloadState = downloadState,
                                    onDownload = onDownload,
                                    lyricsState = lyricsState,
                                    lyricsPreview = nowPlayingLyricsPreview,
                                )
                            } else {
                                NowPlayingScreen(
                                    track = track,
                                    state = state,
                                    progressState = progressState,
                                    player = player,
                                    lyricsState = lyricsState,
                                    signalPath = signalPath,
                                    usbDac = usbDac,
                                    availability = internalAvailability,
                                    isTablet = isTablet,
                                    isVideoActive = isVideoActive,
                                    isLiked = isLiked,
                                    isCustomAudio = isCustomAudio,
                                    isEqEnabled = equalizerSettings.enabled,
                                    wavySeekbarEnabled = wavySeekbarEnabled,
                                    playerActionDockEnabled = playerActionDockEnabled,
                                    windowActive = windowActive,
                                    isDistractionFree = isDistractionFree,
                                    primaryColor = ambientColor,
                                    secondaryColor = ambientCompanion,
                                    tertiaryColor = ambientDeep,
                                    onToggleDistractionFree = { isDistractionFree = !isDistractionFree },
                                    onToggleLiked = onToggleLiked,
                                    onDoubleTapLike = onDoubleTapLike,
                                    onToggleFullArtwork = onToggleFullArtwork,
                                    onOpenArtist = onOpenArtist,
                                    onOpenAudioControls = { showAudioControls = true },
                                    onOpenSignalPath = { showSignalPath = true },
                                    onOpenOutputPicker = { showOutputSheet = true },
                                    onOpenEqualizer = { showEqualizerSheet = true },
                                    onOpenLyrics = { onTabChange(FullPlayerTab.LYRICS) },
                                    onOpenQueue = { onTabChange(FullPlayerTab.QUEUE) },
                                    onCloseVideo = { syncedVideoVm.setVideoActive(false) },
                                    onEnterVideoFullscreen = { isVideoFullscreen = true },
                                    onToggleVideo = onToggleSyncedVideo.takeIf { musicVideoEnabled },
                                    downloadState = downloadState,
                                    onDownload = onDownload,
                                    showSleeve = !showFullBleed,
                                    sleeveCanvas = if (showSleeveCanvas) activeCanvas ?: canvas else null,
                                    pausedForTransition = shownDismissY > 1f,
                                    lyricsPreview = nowPlayingLyricsPreview,
                                )
                            }
                        }
                    }
                    }
                }
                state.error?.takeIf { showError }?.let { message ->
                    Surface(
                        onClick = hapticClick(action = player::retry),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp + bottomInset),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.ErrorOutline, null, modifier = Modifier.size(21.dp))
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(
                                    message,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "Tap to retry",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                )
                            }
                            IconButton(
                                onClick = hapticClick(action = player::clearError),
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(Icons.Filled.Close, "Dismiss error", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    if (showTrackMenu) {
        TrackContextMenuSheet(
            target = TrackMenuTarget.Track(track.title, track.artist, ""),
            capabilities = TrackMenuCapabilities(
                showCopyActions = true,
                showDeleteScrobble = false,
                showRecommendationExclusion = true,
            ),
            playableTrack = track,
            onDismiss = { showTrackMenu = false },
            onPlayInLastWave = { player.play(track, sourceLabel = state.sourceLabel) },
            canWatchVideo = canWatchVideo,
            onWatchVideo = if (canWatchVideo) ({ syncedVideoVm.setVideoActive(true) }) else null,
            onShare = { shareTrackLink(context, track) },
        )
    }

    if (showLyricsOffsetDialog && onSetLyricsOffsetMs != null) {
        LyricsOffsetDialog(
            currentMs = lyricsOffsetMs,
            onSelect = onSetLyricsOffsetMs,
            onDismiss = { showLyricsOffsetDialog = false },
            isPerTrack = true,
        )
    }
    if (showSignalPath) {
        val dac = usbDac.dac
        LaunchedEffect(Unit) { player.refreshInternalDirectAvailability() }
        SignalPathSheet(
            report = signalPath,
            needsUsbPermission = dac != null && dac.hasUsbPeripheral && !dac.usbPermissionGranted,
            onRequestUsbAccess = player::requestUsbPermission,
            onDismiss = { showSignalPath = false },
            internalAvailability = internalAvailability,
            internalEnabled = player.isInternalDirectPrefEnabled,
            internalUnderruns = if (signalPath.internalDirectActive) player.internalDirectUnderruns() else null,
            usbDiagnostic = if (dac != null) player::usbDiagnosticReport else null,
            player = player,
        )
    }
    if (showOutputSheet) {
        AudioOutputSheet(
            player = player,
            state = state,
            signalPath = signalPath,
            usbDac = usbDac,
            availability = internalAvailability,
            isTablet = isTablet,
            onDismiss = { showOutputSheet = false },
            onOpenSignalPath = { showSignalPath = true },
            onOpenAudioControls = { showAudioControls = true },
        )
    }
    if (showEqualizerSheet) {
        com.lastwave.app.ui.settings.EqualizerSheet(
            eq = equalizerSettings,
            onDismiss = { showEqualizerSheet = false },
            onSetEnabled = onSetEqualizerEnabled,
            onPickPreset = onApplyEqPreset,
            onBandPreview = onPreviewEqBandGain,
            onBandChange = onSetEqBandGain,
            onOpenAutoEq = {
                showEqualizerSheet = false
                showAutoEq = true
            },
        )
    }
    if (showAudioControls) {
        AudioControlsSheet(
            player = player,
            state = state,
            onDismiss = { showAudioControls = false },
            onOpenAutoEq = { showAutoEq = true },
            showAutoEq = showAutoEqInVolumePanel,
            onOpenEqualizer = { showEqualizerSheet = true },
            isEqEnabled = equalizerSettings.enabled,
        )
    }
    if (showAutoEq) {
        com.lastwave.app.ui.settings.autoeq.AutoEqDialog(onDismiss = { showAutoEq = false })
    }
}
}
