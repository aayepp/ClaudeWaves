@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.lastwave.app.ui.player

import com.lastwave.app.ui.theme.ComicInk
import com.lastwave.app.ui.theme.comicPanel
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.NowPlayingPaletteScope
import com.lastwave.app.data.local.HomeHoldShortcut
import com.lastwave.app.ui.shell.ExpandableHomeHoldPill
import com.lastwave.app.ui.shell.LocalHomeHoldShortcut
import com.lastwave.app.ui.shell.LocalOnHomeHoldShortcut
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.lerp
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
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
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawOutline
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
import androidx.compose.ui.zIndex
import com.lastwave.app.ui.theme.glassRim
import androidx.compose.foundation.layout.requiredSize
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
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
import com.lastwave.app.ui.theme.LocalMd3NavBar
import com.lastwave.app.ui.theme.LocalIsDarkTheme
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
import com.lastwave.app.ui.theme.BackdropBoundsHolder
import com.lastwave.app.ui.theme.LocalBackdropContrastCoordinator
import com.lastwave.app.ui.theme.rememberBackdropContrastCoordinator
import com.lastwave.app.ui.theme.rememberBackdropUseLightInk
import com.lastwave.app.ui.theme.rememberLayerBackdrop
import com.lastwave.app.ui.theme.trackBackdropBounds
import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
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

enum class FullPlayerTab {
    NOW_PLAYING,
    LYRICS,
    QUEUE
}

val LocalMusicPlayer = staticCompositionLocalOf<MusicPlayer> {
    error("MusicPlayer is only available inside PlayerHost")
}

val LocalAddToPlaylist = staticCompositionLocalOf<(PlayableTrack) -> Unit> {
    error("Add-to-playlist is only available inside PlayerHost")
}

/**
 * Extra list-end clearance needed while the collapsed player overlays a
 * screen. The Material 3 bar is about 86dp tall; 88dp clears it without
 * leaving a large empty band at the end of short lists.
 */
val LocalMiniPlayerScrollClearance = staticCompositionLocalOf { 0.dp }
val LocalMiniPlayerSnackbarClearance = staticCompositionLocalOf { 0.dp }


val LocalBottomNavSlot = androidx.compose.runtime.staticCompositionLocalOf<androidx.compose.runtime.MutableState<BottomNavigationContent>> {
    error("LocalBottomNavSlot not provided")
}

/** App-wide collapsed + maximized player layered over every navigation route. */
@Composable
fun PlayerHost(
    viewModel: PlayerViewModel = hiltViewModel(),
    hasBottomNavigation: Boolean = false,
    material3NavigationEnabled: Boolean = false,
    hideMiniPlayer: Boolean = false,
    /**
     * A screen built around a WebView (Spotify / YouTube sign-in). The mini player is
     * hidden and the liquid-glass backdrop stops capturing the page: re-recording a
     * WebView into an offscreen layer every frame (the mini player animates while
     * music plays) makes the page stutter and jitter on some GPUs/ROMs.
     */
    webContentScreen: Boolean = false,
    /** Off the main tabs: a Home circle docks left of the mini player and pops back to the shell. */
    onHome: (() -> Unit)? = null,
    /** Runs the user's configured 2nd shortcut when holding & sliding from the docked Home pill. */
    onExecuteHomeHoldShortcut: (HomeHoldShortcut) -> Unit = {},
    /** Search draws its own bar where the dock sits (after Auris): the mini player rides above it. */
    searchBarBelow: Boolean = false,
    content: @Composable () -> Unit,
) {
    @Suppress("NAME_SHADOWING")
    val hideMiniPlayer = hideMiniPlayer || webContentScreen
    val state by viewModel.chromeState.collectAsStateWithLifecycle()
    val likedTrackKeys by viewModel.likedTrackKeys.collectAsStateWithLifecycle()
    val likeNudge by viewModel.likeNudge.collectAsStateWithLifecycle()
    val miniLiked = state.current?.let { viewModel.isLiked(it, likedTrackKeys) } == true
    val activeDownloads by viewModel.activeDownloads.collectAsStateWithLifecycle()
    var expanded by rememberSaveable { mutableStateOf(false) }
    var currentTab by rememberSaveable { mutableStateOf(FullPlayerTab.NOW_PLAYING) }
    // Only line-synced lyrics need the 60 ms playhead; on Now Playing each
    // tick redrew the whole full player to move the seek bar under a pixel.
    val lyricsOpen = expanded && currentTab == FullPlayerTab.LYRICS
    DisposableEffect(lyricsOpen) {
        com.lastwave.app.playback.PlaybackUiDemand.needsHighFrequency = lyricsOpen
        onDispose { com.lastwave.app.playback.PlaybackUiDemand.needsHighFrequency = false }
    }
    var playlistTrack by remember { mutableStateOf<PlayableTrack?>(null) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val keepLyricsAwake = expanded && currentTab == FullPlayerTab.LYRICS && settings.keepLyricsScreenAwake
    val currentView = androidx.compose.ui.platform.LocalView.current
    DisposableEffect(keepLyricsAwake) {
        if (keepLyricsAwake) {
            currentView.keepScreenOn = true
        }
        onDispose {
            if (keepLyricsAwake) {
                currentView.keepScreenOn = false
            }
        }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val requestAddToPlaylist = remember(viewModel) {
        { track: PlayableTrack ->
            viewModel.prepareCustomPlaylists()
            playlistTrack = track
        }
    }
    val trackKey = state.current?.let { it.videoId ?: "${it.artist}|${it.title}" }
    LaunchedEffect(trackKey) {
        if (trackKey == null) {
            expanded = false
            currentTab = FullPlayerTab.NOW_PLAYING
        }
    }
    LaunchedEffect(expanded) {
        if (!expanded) {
            currentTab = FullPlayerTab.NOW_PLAYING
        }
    }
    LaunchedEffect(Unit) {
        viewModel.navEvents.collect {
            expanded = false
            currentTab = FullPlayerTab.NOW_PLAYING
        }
    }
    LaunchedEffect(Unit) {
        viewModel.mixLauncher.requests.collect {
            expanded = false
            currentTab = FullPlayerTab.NOW_PLAYING
        }
    }
    LaunchedEffect(Unit) {
        viewModel.genreExplorer.pendingGenre.collect { genre ->
            if (genre != null) {
                expanded = false
                currentTab = FullPlayerTab.NOW_PLAYING
            }
        }
    }
    LaunchedEffect(Unit) {
        com.lastwave.app.widget.wave.WaveWidgetNavigation.requests.collect { requestId ->
            if (requestId <= 0L) return@collect
            val hasTrack = viewModel.player.state.value.current != null ||
                withTimeoutOrNull(2_000L) {
                    viewModel.player.state.first { it.current != null }
                    true
                } == true
            if (hasTrack && com.lastwave.app.widget.wave.WaveWidgetNavigation.consume(requestId)) {
                currentTab = FullPlayerTab.NOW_PLAYING
                expanded = true
            }
        }
    }
    val miniPlayerVisible = state.current != null && !expanded && !settings.buttonNowPlayingEnabled && !hideMiniPlayer

    val miniGlass = LocalLiquidGlass.current && isLiquidGlassBackdropSupported() && !webContentScreen
    // This layer wraps every screen and is drawn on screen too: a flat fill here hid the
    // page wash and the user's background image behind the whole app.
    val pageWash = com.lastwave.app.ui.theme.rememberPageWash()
    val miniBackdrop = if (miniGlass) {
        rememberLayerBackdrop {
            pageWash()
            drawContent()
        }
    } else null

    // Player presentation (open / close): driven by sheet expansion progress (0..1).
    // The player slides smoothly from the bottom over the content like a native
    // sheet with spring physics (matching PixelMusic), while the page behind
    // receives a clean dimming scrim without blur or zoom distortions.
    val playerExpansion = remember { Animatable(if (expanded && state.current != null) 1f else 0f) }
    val expansionTarget = if (expanded && state.current != null) 1f else 0f
    // The full player is heavy to compose. Mount it (invisible, expansion still 0) and let it
    // finish its first composition/measure before the spring starts, so that cost lands before
    // the animation instead of inside its first frames (the open stutter/freeze).
    var playerMounted by remember { mutableStateOf(expanded && state.current != null) }
    LaunchedEffect(expansionTarget) {
        if (expansionTarget == 1f) {
            if (!playerMounted) {
                playerMounted = true
                withFrameNanos { }
                withFrameNanos { }
            }
        }
        com.lastwave.app.ui.theme.GlassMotion.stampSheet()
        try {
            playerExpansion.animateTo(
                targetValue = expansionTarget,
                animationSpec = PlayerSheetSpec,
            ) {
                com.lastwave.app.ui.theme.GlassMotion.stampSheet()
            }
        } finally {
            com.lastwave.app.ui.theme.GlassMotion.clearSheet()
        }
        if (expansionTarget == 0f) playerMounted = false
    }

    var isPlayerDragging by remember { mutableStateOf(false) }
    val playerLayerShown by remember { derivedStateOf { playerExpansion.value > 0f } }
    val sheetMoving by remember { derivedStateOf { playerExpansion.value < 1f } }
    // Full-bleed covers have no sleeve to land on; the card itself still uses the
    // exact same linear macOS-Dock container morph without flying a second thumbnail.
    val artworkFlight = if (settings.canvasFullBleed) null else remember { ArtworkFlight { playerExpansion.value } }
    val contentCovered by remember { derivedStateOf { playerExpansion.value >= 1f && !isPlayerDragging } }
    val showMiniPlayer by remember { derivedStateOf { playerExpansion.value < 0.45f } }
    val showBottomNav by remember { derivedStateOf { state.current == null || playerExpansion.value < 0.625f } }
    // Mini player rides above the growing card for the first half of the morph.
    val miniOnTop by remember { derivedStateOf { playerExpansion.value > 0f && playerExpansion.value < 0.5f } }

    // Container-transform morph: the full player grows out of the mini player card
    // (PixelMusic-style). Bounds are captured only while the player is at rest so
    // the mini player's own follow-the-card translation can't feed back into them.
    var hostOrigin by remember { mutableStateOf(Offset.Zero) }
    var miniCardBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val morphCardColor = MaterialTheme.colorScheme.surfaceContainerHigh

    // Pulling the mini player up moves the sheet under the finger; letting go throws it
    // open or back with the finger's speed, on the same spring a tap uses.
    val sheetScope = rememberCoroutineScope()
    val miniPull = remember {
        object : MiniPlayerPull {
            override fun drag(deltaY: Float) {
                val travel = miniCardBounds?.top?.takeIf { it > 0f } ?: return
                playerMounted = true
                com.lastwave.app.ui.theme.GlassMotion.stampSheet()
                sheetScope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                    playerExpansion.snapTo((playerExpansion.value - deltaY / travel).coerceIn(0f, 1f))
                }
            }

            override fun release(velocityY: Float) {
                val travel = miniCardBounds?.top?.takeIf { it > 0f } ?: return
                val velocity = -velocityY / travel
                val open = if (kotlin.math.abs(velocity) > 1.2f) velocity > 0f else playerExpansion.value > 0.35f
                sheetScope.launch {
                    com.lastwave.app.ui.theme.GlassMotion.stampSheet()
                    try {
                        playerExpansion.animateTo(if (open) 1f else 0f, PlayerSheetSpec, initialVelocity = velocity) {
                            com.lastwave.app.ui.theme.GlassMotion.stampSheet()
                        }
                    } finally {
                        com.lastwave.app.ui.theme.GlassMotion.clearSheet()
                    }
                    if (!open) playerMounted = false
                }
                expanded = open
            }
        }
    }

    val bottomNavSlot = remember { mutableStateOf<BottomNavigationContent>({ _ -> }) }
    val isShortCarViewport = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 560
    // Off: progress stays at 0, which is the classic full bar + full mini player layout (auto-enabled on short car head unit viewports).
    val chromeCollapse = rememberChromeCollapseState(enabled = hasBottomNavigation && (settings.collapsingNavBar || isShortCarViewport))
    var homePillExtraWidth by remember { mutableStateOf(0.dp) }
    // While docking, the card slides down over the fading bar, not under it.
    val chromeDocking by remember { derivedStateOf { chromeCollapse.progress() > 0f } }
    val dropController = remember { MiniPlayerDropController() }
    val lifecycleOwner = LocalLifecycleOwner.current
    var activeToastPill by remember { mutableStateOf<PlayerToastEvent?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.toastEvents.collect { event ->
            activeToastPill = event
        }
    }
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.downloadCompletedEvents.collect { event ->
                val savedLabel = context.getString(com.lastwave.app.R.string.toast_saved_to_downloads)
                val subtitle = if (event.formatBadge.isNotBlank()) "${event.formatBadge} · $savedLabel" else savedLabel
                activeToastPill = PlayerToastEvent(
                    kind = PlayerToastKind.DOWNLOADED,
                    title = context.getString(com.lastwave.app.R.string.toast_downloaded, event.title),
                    subtitle = subtitle,
                )
            }
        }
    }
    val backdropContrastCoordinator = rememberBackdropContrastCoordinator(
        backdrop = if (miniGlass) miniBackdrop else null,
        paused = { contentCovered },
    )
    CompositionLocalProvider(
        LocalMusicPlayer provides viewModel.player,
        LocalAddToPlaylist provides requestAddToPlaylist,
        LocalMiniPlayerScrollClearance provides if (state.current != null && !settings.buttonNowPlayingEnabled && !hideMiniPlayer) 98.dp else 0.dp,
        LocalMiniPlayerSnackbarClearance provides if (miniPlayerVisible) {
            // Keep snackbars clear of the tray: its bottom lift plus its ~80dp card.
            (if (hasBottomNavigation) {
                if (material3NavigationEnabled) 84.dp else 80.dp
            } else {
                14.dp
            }) + 80.dp
        } else {
            0.dp
        },
        LocalBottomNavSlot provides bottomNavSlot,
        LocalChromeCollapse provides chromeCollapse,
        LocalLiquidGlassBackdrop provides (if (miniGlass) miniBackdrop else null),
        LocalBackdropContrastCoordinator provides backdropContrastCoordinator,
        LocalLikeNudge provides likeNudge,
        LocalArtworkFlight provides artworkFlight,
        LocalMiniPlayerDropController provides dropController,
        LocalHomeOnRight provides settings.homeButtonRight,
        LocalSetHomePillExtraWidth provides { homePillExtraWidth = it },
        LocalHomeHoldShortcut provides HomeHoldShortcut.fromId(settings.homeHoldShortcut),
        LocalOnHomeHoldShortcut provides onExecuteHomeHoldShortcut,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    hostOrigin = it.boundsInRoot().topLeft
                    artworkFlight?.host = it
                },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        // Under the opaque full player the shell is invisible, but drawing it anyway
                        // re-rendered the whole feed (and its path clip, masked on the CPU) every frame.
                        if (contentCovered) return@drawWithContent
                        drawContent()
                        val p = playerExpansion.value.coerceIn(0f, 1f)
                        if (p > 0f) {
                            drawRect(Color.Black.copy(alpha = 0.45f * p))
                        }
                    },
            ) {
                // Own layer: the scrim above redraws every frame of the sheet's motion, and
                // without it the page (and the glass backdrop recorded from it) was re-drawn
                // with it. Isolated, the glass keeps its last capture while the sheet moves.
                Box(Modifier.fillMaxSize().graphicsLayer().liquidGlassSource(if (miniGlass) miniBackdrop else null)) {
                    com.lastwave.app.ui.common.AmbientAnimationsGate(visible = { !contentCovered }) {
                        content()
                    }
                }
            }
            // Content scrolling under the status bar and the bottom chrome melts away instead
            // of meeting a hard edge, and rows no longer poke out between the mini player and dock.
            if (!webContentScreen) {
                val miniShown = state.current != null && !settings.buttonNowPlayingEnabled && !hideMiniPlayer && showMiniPlayer
                val expandedScrimHeight = when {
                    hasBottomNavigation && miniShown -> if (isShortCarViewport) 156.dp else 210.dp
                    hasBottomNavigation -> if (isShortCarViewport) 104.dp else 140.dp
                    miniShown -> if (isShortCarViewport) 96.dp else 130.dp
                    else -> WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                }
                val collapsedScrimHeight = if (isShortCarViewport) 88.dp else 108.dp
                val bottomHeight = if (hasBottomNavigation || miniShown) {
                    androidx.compose.ui.unit.lerp(expandedScrimHeight, collapsedScrimHeight, chromeCollapse.progress().coerceIn(0f, 1f))
                } else {
                    expandedScrimHeight
                }
                val edgeAlpha = { (1f - playerExpansion.value * 2.2f).coerceIn(0f, 1f) }
                val backdrop = if (miniGlass) miniBackdrop else null
                // The band draws over content, so it only feathers 16dp past the status bar:
                // taller would blur each screen's own header buttons sitting below it.
                EdgeFade(backdrop, atTop = true, WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp, edgeAlpha, Modifier.align(Alignment.TopCenter))
                // Search draws its own bar at the bottom of the page; the band drawn over the
                // page would blur that bar away.
                if (!searchBarBelow) {
                    EdgeFade(backdrop, atTop = false, bottomHeight, edgeAlpha, Modifier.align(Alignment.BottomCenter))
                }
            }
            if (state.current != null && !settings.buttonNowPlayingEnabled && !hideMiniPlayer && showMiniPlayer) {
                val miniBottomPadding = if (hasBottomNavigation || searchBarBelow) {
                    if (material3NavigationEnabled) 80.dp else 76.dp
                } else {
                    14.dp
                }
                // Off Home the card sits in the same row Home's collapsed bar uses, beside the Home circle.
                val dockProgress = if (onHome != null) Docked else chromeCollapse.progress
                val miniAlpha: GraphicsLayerScope.() -> Unit = {
                    alpha = (1f - playerExpansion.value.coerceIn(0f, 1f) * 2.2f).coerceIn(0f, 1f)
                }
                if (onHome != null) {
                    HomeDockButton(
                        backdrop = if (miniGlass) miniBackdrop else null,
                        onClick = onHome,
                        modifier = Modifier
                            .align(if (settings.homeButtonRight) Alignment.BottomEnd else Alignment.BottomStart)
                            .graphicsLayer(miniAlpha)
                            .collapsedNavPill(Docked),
                    )
                }
                MiniPlayer(
                    state = state,
                    progressState = viewModel.miniProgressState,
                    isLiked = miniLiked,
                    onToggleLiked = { state.current?.let(viewModel::toggleLiked) },
                    onExpand = { expanded = true },
                    onToggle = viewModel.player::togglePlayPause,
                    onPrevious = viewModel.player::previous,
                    onNext = viewModel.player::next,
                    onClose = viewModel.player::stopAndClear,
                    bottomPadding = miniBottomPadding,
                    backdrop = if (miniGlass) miniBackdrop else null,
                    pull = miniPull,
                    onCardBounds = { rootBounds ->
                        if (playerExpansion.value == 0f) {
                            miniCardBounds = rootBounds.translate(-hostOrigin)
                        }
                    },
                    compactProgress = dockProgress,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(if (miniOnTop || chromeDocking) 1f else 0f)
                        .graphicsLayer {
                            miniAlpha()
                            // Stay flush with the morphing card's top edge as it rises and closes.
                            translationY = -(miniCardBounds?.top ?: 0f) * playerExpansion.value.coerceIn(0f, 1f)
                        }
                        .dockBesidePill(
                            miniBottomPadding,
                            dockProgress,
                            pillOnRight = settings.homeButtonRight,
                            extraPillWidth = { homePillExtraWidth },
                        ),
                )
            }

            // Sleep Timer Overlay Button (isolated in its own leaf collector so timer ticks never recompose PlayerHost)
            if (!expanded) {
                SleepTimerOverlayHost(player = viewModel.player)
            }
            val currentTrack = state.current
            if (currentTrack != null && !expanded && settings.buttonNowPlayingEnabled) {
                DraggableNowPlayingButton(
                    track = currentTrack,
                    progressState = viewModel.miniProgressState,
                    isPlaying = state.isPlaying,
                    onExpand = { expanded = true },
                    onClose = viewModel.player::stopAndClear,
                )
            }
            if (activeDownloads.isNotEmpty() && !expanded) {
                val latestProgress = activeDownloads.firstOrNull()?.progressPercent ?: 0
                com.lastwave.app.ui.download.DraggableDownloadOverlay(
                    activeDownloadsCount = activeDownloads.size,
                    latestProgressPercent = latestProgress,
                    onOpenDownloads = { viewModel.navigateToDownloads() },
                )
            }
            if ((playerLayerShown || playerMounted) && state.current != null) {
                val sheetDensity = LocalDensity.current
                val morphGlass = LocalLiquidGlass.current
                val morphComic = LocalComicTheme.current
                val morphDark = LocalIsDarkTheme.current
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = playerExpansion.value.coerceIn(0f, 1f)
                            // Start rect = mini player card; fallback (button mode / no mini)
                            // = zero-height strip at the bottom, i.e. a reveal from below.
                            val start = miniCardBounds
                                ?: androidx.compose.ui.geometry.Rect(0f, size.height, size.width, size.height)
                            val left = androidx.compose.ui.util.lerp(start.left, 0f, p)
                            val right = androidx.compose.ui.util.lerp(start.right, size.width, p)
                            val top = androidx.compose.ui.util.lerp(start.top, 0f, p)
                            val bottom = androidx.compose.ui.util.lerp(start.bottom, size.height, p)
                            val startRadius = minOf(start.height / 2f, PLAYER_SHEET_CORNER_RADIUS.toPx() + 8.dp.toPx())
                            val radius = androidx.compose.ui.util.lerp(startRadius, 0f, p)
                            // Content is anchored to the card's top edge and clipped to the card.
                            translationY = top
                            // Pre-mount frames (p == 0): composed and measured, but not drawn over the mini player.
                            alpha = if (p == 0f) 0f else 1f
                            shadowElevation = if (p > 0f && p < 1f) {
                                sin(p * kotlin.math.PI.toFloat()) * 14.dp.toPx()
                            } else {
                                0f
                            }
                            shape = PlayerMorphShape(left, right, bottom - top, radius)
                            clip = p < 1f
                        }
                        .drawBehind {
                            // Solid card surface visible until the full player fades in.
                            if (playerExpansion.value < 1f) drawRect(morphCardColor)
                        }
                        .drawWithContent {
                            drawContent()
                            val p = playerExpansion.value.coerceIn(0f, 1f)
                            if (p <= 0.01f || p >= 0.99f) return@drawWithContent
                            val sweep = sin(p * kotlin.math.PI.toFloat())
                            val start = miniCardBounds
                                ?: androidx.compose.ui.geometry.Rect(0f, size.height, size.width, size.height)
                            val left = androidx.compose.ui.util.lerp(start.left, 0f, p)
                            val right = androidx.compose.ui.util.lerp(start.right, size.width, p)
                            val top = androidx.compose.ui.util.lerp(start.top, 0f, p)
                            val bottom = androidx.compose.ui.util.lerp(start.bottom, size.height, p)
                            val startRadius = minOf(start.height / 2f, PLAYER_SHEET_CORNER_RADIUS.toPx() + 8.dp.toPx())
                            val radius = androidx.compose.ui.util.lerp(startRadius, 0f, p)
                            val morphOutline = PlayerMorphShape(
                                left = left,
                                right = right,
                                height = bottom - top,
                                radius = radius,
                            ).createOutline(size, layoutDirection, this)
                            if (morphComic) {
                                drawOutline(
                                    outline = morphOutline,
                                    color = ComicInk.copy(alpha = 0.85f * sweep),
                                    style = Stroke(width = 2.5.dp.toPx()),
                                )
                            } else if (morphGlass) {
                                val sweepStartX = androidx.compose.ui.util.lerp(-size.width * 0.25f, size.width * 0.65f, p)
                                val sweepEndX = sweepStartX + size.width * 0.65f
                                drawRect(
                                    brush = Brush.linearGradient(
                                        0f to Color.Transparent,
                                        0.45f to Color.White.copy(alpha = (if (morphDark) 0.14f else 0.22f) * sweep),
                                        0.55f to Color(0xFF7BE8FF).copy(alpha = (if (morphDark) 0.08f else 0.12f) * sweep),
                                        1f to Color.Transparent,
                                        start = Offset(sweepStartX, 0f),
                                        end = Offset(sweepEndX, (bottom - top).coerceAtLeast(1f)),
                                    ),
                                    topLeft = Offset(left, 0f),
                                    size = Size((right - left).coerceAtLeast(0f), (bottom - top).coerceAtLeast(0f)),
                                    blendMode = BlendMode.Plus,
                                )
                                if (!morphDark) {
                                    drawOutline(
                                        outline = morphOutline,
                                        color = Color.Black.copy(alpha = 0.14f * sweep),
                                        style = Stroke(width = 2.dp.toPx()),
                                    )
                                }
                                drawOutline(
                                    outline = morphOutline,
                                    brush = Brush.linearGradient(
                                        0f to Color.White.copy(alpha = 0.85f * sweep),
                                        0.5f to Color(0xFF7BE8FF).copy(alpha = 0.42f * sweep),
                                        1f to Color.White.copy(alpha = 0.50f * sweep),
                                        start = Offset(sweepStartX, 0f),
                                        end = Offset(sweepEndX, (bottom - top).coerceAtLeast(1f)),
                                    ),
                                    style = Stroke(width = 1.6.dp.toPx()),
                                )
                            }
                        },
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val p = playerExpansion.value.coerceIn(0f, 1f)
                                // Fullmode has no flying thumbnail, so crossfade smoothly from p = 0.14
                                // as the MiniPlayer dissolves; Normal waits until p = 0.25 for sleeve takeoff.
                                val contentAlpha = if (artworkFlight == null) {
                                    val raw = ((p - 0.14f) / 0.86f).coerceIn(0f, 1f)
                                    raw * raw * (3f - 2f * raw)
                                } else {
                                    ((p - 0.25f) / 0.75f).coerceIn(0f, 1f)
                                }
                                alpha = contentAlpha
                                val settlePx = with(sheetDensity) { (if (artworkFlight == null) 14.dp else 24.dp).toPx() }
                                translationY = androidx.compose.ui.util.lerp(settlePx, 0f, contentAlpha)
                            },
                    ) {
                        PredictiveBackScreen(
                            enabled = expanded && state.current != null && currentTab == FullPlayerTab.NOW_PLAYING,
                            onBack = { expanded = false },
                        ) {
                            androidx.compose.runtime.CompositionLocalProvider(
                                LocalFullPlayerOpen provides (expanded || playerLayerShown),
                            ) {
                                ExpandedPlayer(
                                    viewModel = viewModel,
                                    currentTab = currentTab,
                                    onTabChange = { currentTab = it },
                                    onRetryLyrics = viewModel::retryLyrics,
                                    onCollapse = { expanded = false },
                                    onOpenArtist = { artist ->
                                        expanded = false
                                        viewModel.openArtist(artist)
                                    },
                                    onDragStatusChange = { isPlayerDragging = it },
                                    isOpen = expanded,
                                )
                            }
                        }
                    }
                }
            }

            // The cover flies from the mini player's thumbnail to the sleeve (and back),
            // drawn above both while the sheet moves; each end hides meanwhile.
            val flight = artworkFlight
            val flyingTrack = state.current
            if (flight != null && flyingTrack != null && sheetMoving && playerLayerShown) {
                Box(Modifier.fillMaxSize().zIndex(2f)) {
                    ArtworkImage(
                        name = flyingTrack.title,
                        artist = flyingTrack.artist,
                        embeddedUrl = flyingTrack.artworkUrl,
                        fallbackIcon = Icons.Filled.MusicNote,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .requiredSize(FlightBaseSize)
                            .graphicsLayer {
                                val host = flight.host?.takeIf { it.isAttached }
                                val sleeve = flight.sleeve?.takeIf { it.isAttached }
                                val thumb = flight.thumb?.translate(-hostOrigin)
                                val card = miniCardBounds
                                if (host == null || sleeve == null || thumb == null || card == null) {
                                    alpha = 0f
                                    return@graphicsLayer
                                }
                                val p = playerExpansion.value.coerceIn(0f, 1f)
                                val arc = sin(p * kotlin.math.PI.toFloat())
                                // Undo the sheet's own lift and settle to find where the sleeve rests.
                                val contentAlpha = ((p - 0.25f) / 0.75f).coerceIn(0f, 1f)
                                val lift = androidx.compose.ui.util.lerp(card.top, 0f, p) +
                                    androidx.compose.ui.util.lerp(24.dp.toPx(), 0f, contentAlpha)
                                val rest = host.localBoundingBoxOf(sleeve, clipBounds = false).translate(0f, -lift)
                                val w = androidx.compose.ui.util.lerp(thumb.width, rest.width, p)
                                val h = androidx.compose.ui.util.lerp(thumb.height, rest.height, p)
                                val sideCurve = arc * (-10.dp.toPx() * (if (settings.homeButtonRight) -1f else 1f))
                                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                                translationX = androidx.compose.ui.util.lerp(thumb.left, rest.left, p) + sideCurve
                                translationY = androidx.compose.ui.util.lerp(thumb.top, rest.top, p)
                                scaleX = w / size.width
                                scaleY = h / size.height
                                val radius = androidx.compose.ui.util.lerp(10.dp.toPx(), 24.dp.toPx(), p)
                                shadowElevation = arc * 18.dp.toPx()
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(radius / scaleX.coerceAtLeast(0.01f))
                                clip = true
                                alpha = 1f
                            },
                    )
                }
            }

            if (hasBottomNavigation && showBottomNav) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val p = if (state.current == null) 1f else (1f - playerExpansion.value * 1.6f).coerceIn(0f, 1f)
                            val e = if (state.current == null) 0f else playerExpansion.value.coerceIn(0f, 1f)
                            alpha = p
                            // Nav bar sinks off the bottom as the player card grows.
                            translationY = e * 96.dp.toPx()
                        },
                ) {
                    CompositionLocalProvider(LocalMd3NavBar provides material3NavigationEnabled) {
                        bottomNavSlot.value(material3NavigationEnabled)
                    }
                }
            }
            
            playlistTrack?.let { track ->
                AddToPlaylistDialogHost(
                    viewModel = viewModel,
                    track = track,
                    onDismiss = { playlistTrack = null },
                    onAdd = { playlistIds, duplicatePlaylistIds ->
                        viewModel.addToPlaylists(playlistIds, duplicatePlaylistIds, track)
                        playlistTrack = null
                    },
                    onFindDuplicates = { playlistIds ->
                        viewModel.findDuplicatePlaylistIds(playlistIds, track)
                    },
                    onFindCachedDuplicates = { playlistIds ->
                        viewModel.findCachedDuplicatePlaylistIds(playlistIds, track)
                    },
                    onCreate = { title ->
                        viewModel.createPlaylistAndAdd(title, track)
                        playlistTrack = null
                    },
                )
            }

            val toastBottomPadding = when {
                expanded || playerExpansion.value > 0.5f -> 96.dp
                miniPlayerVisible -> {
                    val miniBase = when {
                        onHome != null -> 14.dp
                        hasBottomNavigation || searchBarBelow -> if (material3NavigationEnabled) 80.dp else 76.dp
                        else -> 14.dp
                    }
                    miniBase + 78.dp
                }
                hasBottomNavigation || searchBarBelow -> (if (material3NavigationEnabled) 80.dp else 76.dp) + 14.dp
                else -> 20.dp
            }
            PlayerToastPillHost(
                toast = activeToastPill,
                onDismiss = { activeToastPill = null },
                onTap = { event ->
                    activeToastPill = null
                    if (event.kind == PlayerToastKind.DOWNLOADED) {
                        expanded = false
                        viewModel.navigateToDownloads()
                    }
                },
                backdrop = if (miniGlass && !playerLayerShown) miniBackdrop else null,
                forceDarkSurface = expanded || playerExpansion.value > 0.5f,
                bottomPadding = toastBottomPadding,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(10f),
            )
            MiniPlayerLiquidDropOverlay(
                controller = dropController,
                miniCardBounds = { miniCardBounds },
                hostOrigin = { hostOrigin },
                expanded = expanded,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(9f),
            )
        }
    }
}

@Composable
private fun SleepTimerOverlayHost(player: MusicPlayer) {
    val sleepTimer by remember(player) {
        player.stateWithoutProgress
            .map { it.sleepTimerRemainingMs to it.sleepTimerEndOfTrack }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null to false)
    val (remainingMs, endOfTrack) = sleepTimer
    if (remainingMs != null || endOfTrack) {
        DraggableSleepTimerOverlay(
            sleepTimerRemainingMs = remainingMs,
            sleepTimerEndOfTrack = endOfTrack,
            onCycleTimer = { player.cycleSleepTimer() },
            onCloseTimer = { player.setSleepTimerMinutes(0) },
        )
    }
}

/**
 * Screen-edge band that softens content scrolling under the status bar ([atTop]) or
 * the bottom chrome. With glass it blurs the captured page; without, a direct gradient
 * fades cleanly into the page colour with zero offscreen FBO passes.
 */
@Composable
private fun EdgeFade(
    backdrop: Backdrop?,
    atTop: Boolean,
    height: androidx.compose.ui.unit.Dp,
    alpha: () -> Float,
    modifier: Modifier,
) {
    val page = MaterialTheme.colorScheme.background
    if (backdrop == null) {
        val directBrush = remember(page, atTop) {
            val ramp = arrayOf(
                0f to Color.Transparent,
                0.5f to page.copy(alpha = 0.9f),
                1f to page,
            )
            Brush.verticalGradient(
                *if (atTop) ramp.map { (1f - it.first) to it.second }.reversed().toTypedArray() else ramp,
            )
        }
        Box(
            modifier
                .fillMaxWidth()
                .height(height)
                .graphicsLayer { this.alpha = alpha() }
                .drawBehind { drawRect(directBrush) },
        )
        return
    }
    val mask = remember(atTop) {
        val ramp = arrayOf(0f to Color.Transparent, 0.5f to Color.Black.copy(alpha = 0.9f), 1f to Color.Black)
        Brush.verticalGradient(*if (atTop) ramp.map { (1f - it.first) to it.second }.reversed().toTypedArray() else ramp)
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                this.alpha = alpha()
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                drawRect(mask, blendMode = BlendMode.DstIn)
            }
            .drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(0.dp) },
                effects = { blur(24.dp.toPx()) },
                highlight = null,
                shadow = null,
                onDrawSurface = { drawRect(page.copy(alpha = 0.3f)) },
            ),
    )
}

@Composable
internal fun ExpandedPlayer(
    viewModel: PlayerViewModel,
    currentTab: FullPlayerTab,
    onTabChange: (FullPlayerTab) -> Unit,
    onRetryLyrics: () -> Unit,
    onCollapse: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onDragStatusChange: (Boolean) -> Unit = {},
    isOpen: Boolean = true,
) {
    val state by viewModel.fullPlayerState.collectAsStateWithLifecycle()
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
    val lyricsProviderStatuses by viewModel.lyricsProviderStatuses.collectAsStateWithLifecycle()
    val canvas by viewModel.canvasState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val equalizer by viewModel.equalizer.collectAsStateWithLifecycle()
    val likedTrackKeys by viewModel.likedTrackKeys.collectAsStateWithLifecycle()
    val currentTrack = state.current
    // Single source of truth shared with toggleLiked() so heart state never disagrees.
    val isLiked = currentTrack != null && viewModel.isLiked(currentTrack, likedTrackKeys)
    val youTubeLiked by viewModel.youTubeLiked.collectAsStateWithLifecycle()
    val lyricsOffsetMs by viewModel.liveLyricsOffsetMs.collectAsStateWithLifecycle()
    val currentTrackDownloadState by viewModel.currentTrackDownloadState.collectAsStateWithLifecycle()
    val canShowNowPlayingDownload = settings.nowPlayingDownloadButtonEnabled &&
        currentTrack != null &&
        !currentTrack.isImportedLocalFile()
    NowPlayingPaletteScope {
    FullPlayer(
        state = state,
        progressState = viewModel.progressState,
        player = viewModel.player,
        lyricsState = lyricsState,
        lyricsProviderStatuses = lyricsProviderStatuses,
        onProbeLyricsProviders = viewModel::probeLyricsProviders,
        wavySeekbarEnabled = settings.wavySeekbarEnabled,
        playerActionDockEnabled = settings.playerActionDockEnabled,
        nowPlayingMinimalStyle = settings.nowPlayingMinimalStyle,
        rotatingBackgroundEnabled = settings.rotatingBackgroundEnabled,
        lyricsRotatingBackgroundEnabled = settings.lyricsRotatingBackgroundEnabled,
        lyricsOffsetMs = lyricsOffsetMs,
        onSetLyricsOffsetMs = viewModel::setLyricsOffsetMs,
        lyricsFontScale = settings.lyricsFontScale,
        onSetLyricsFontScale = viewModel::setLyricsFontScale,
        lyricsButtonsAutoHideSeconds = settings.lyricsButtonsAutoHideSeconds,
        translationLanguage = settings.translationLanguage,
        lyricsAutoRomanize = settings.lyricsAutoRomanize,
        nowPlayingLyricsPreview = settings.nowPlayingLyricsPreview,
        canvas = canvas,
        canvasEnabled = settings.canvasEnabled,
        canvasFullBleedEnabled = settings.canvasFullBleed,
        canvasGradientBlurEnabled = settings.canvasGradientBlurEnabled,
        musicVideoEnabled = settings.musicVideoEnabled,
        showAutoEqInVolumePanel = settings.showAutoEqInVolumePanel,
        currentTab = currentTab,
        onTabChange = onTabChange,
        onRetryLyrics = onRetryLyrics,
        onCollapse = onCollapse,
        onOpenArtist = onOpenArtist,
        isLiked = isLiked,
        // One saved state drives the heart and YouTube sync.
        onToggleLiked = { currentTrack?.let(viewModel::toggleLiked) },
        onDoubleTapLike = { currentTrack?.let(viewModel::like) },
        onToggleFullArtwork = viewModel::toggleFullArtwork,
        youTubeLiked = youTubeLiked,
        onToggleYouTubeLike = { currentTrack?.let(viewModel::toggleYouTubeLike) },
        onDragStatusChange = onDragStatusChange,
        isOpen = isOpen,
        onSelectLyricsProvider = viewModel::selectLyricsProvider,
        onTryAnotherLyricsSource = viewModel::tryAnotherLyricsSource,
        equalizerSettings = equalizer,
        onSetEqualizerEnabled = viewModel::setEqualizerEnabled,
        onApplyEqPreset = viewModel::applyEqPreset,
        onPreviewEqBandGain = viewModel::previewEqBandGain,
        onSetEqBandGain = viewModel::setEqBandGain,
        downloadState = currentTrackDownloadState,
        onDownload = when {
            !canShowNowPlayingDownload -> null
            currentTrackDownloadState.openInLibrary -> {
                {
                    onCollapse()
                    viewModel.openDownloadedTrack()
                }
            }
            else -> viewModel::downloadCurrentTrack
        },
    )
    }
}

@Composable
internal fun PlayerArtwork(
    track: PlayableTrack,
    modifier: Modifier,
    corner: androidx.compose.ui.unit.Dp,
    decodeSizePx: Int? = null,
    canvas: com.lastwave.app.data.canvas.CanvasArtwork? = null,
    isPlaying: Boolean = false,
    pausedForTransition: Boolean = false,
    transformations: List<coil.transform.Transformation> = emptyList(),
    onAspectRatioChanged: (Float) -> Unit = {},
) {
    // The sleeve cover and its canvas are stacked in the same bounds: the
    // cover is the placeholder while the video loads, then hands the pixels
    // over as the canvas's own alpha climbs. Without this the static art
    // stays at full opacity underneath the video and shows through wherever
    // the canvas is transparent.
    var sleeveCover by remember(canvas) { mutableFloatStateOf(0f) }
    Box(modifier.comicPanel(RoundedCornerShape(corner), offset = if (corner >= 16.dp) 5.dp else 0.dp).clip(RoundedCornerShape(corner)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
        ArtworkImage(
            name = track.title,
            artist = track.artist,
            embeddedUrl = track.artworkUrl,
            fallbackIcon = Icons.Filled.MusicNote,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha =
                        if (sleeveCover >= 0.95f) 0f else (1f - sleeveCover).coerceIn(0f, 1f)
                },
            decodeSizePx = decodeSizePx,
            transformations = transformations,
        )
        if (canvas != null) {
            CanvasArtworkPlayer(
                canvas = canvas,
                isPlaying = isPlaying,
                pausedForTransition = pausedForTransition,
                onAspectRatioChanged = onAspectRatioChanged,
                onCoverChanged = { sleeveCover = it },
                contentMode = CanvasContentMode.CROP,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** True for files copied by the Local songs importer (app-private storage). */
internal fun PlayableTrack.isImportedLocalFile(): Boolean =
    com.lastwave.app.data.generate.LocalAudioFormats.importedPathOrNull(playbackUrl) != null

// -- Player open/close presentation (PixelMusic-style bottom sheet transition) ---

/**
 * Critically damped, ~0.45s (Auris' PlayerSheetMotion, after Apple Music): leaves briskly and
 * settles into place without the bounce the old under-damped open/close springs had.
 */
internal val PlayerSheetSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 195f,
    visibilityThreshold = 0.001f,
)

/** Top corner radius of the sliding sheet in collapsed/transit state. */
internal val PLAYER_SHEET_CORNER_RADIUS = 28.dp

/**
 * Outline for the morphing player card: a rounded rect spanning [left]..[right]
 * horizontally and 0..[height] vertically (layer is already translated to the card top).
 */
internal data class PlayerMorphShape(
    val left: Float,
    val right: Float,
    val height: Float,
    val radius: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): Outline {
        val rect = androidx.compose.ui.geometry.Rect(left, 0f, right, height.coerceAtLeast(0f))
        val r = radius.coerceIn(0f, rect.minDimension / 2f)
        return if (r <= 0.5f) {
            Outline.Rectangle(rect)
        } else {
            Outline.Rounded(androidx.compose.ui.geometry.RoundRect(rect, CornerRadius(r, r)))
        }
    }
}

private val Docked: () -> Float = { 1f }

/** Same glass circle as Home's collapsed nav pill, so the docked row reads identically on every screen. */
@Composable
private fun HomeDockButton(
    backdrop: com.lastwave.app.ui.theme.LayerBackdrop?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val liquidGlass = LocalLiquidGlass.current
    val glassActive = liquidGlass && isLiquidGlassBackdropSupported() && backdrop != null
    val buttonBounds = remember { BackdropBoundsHolder() }
    val useLightInk = rememberBackdropUseLightInk(
        backdrop = backdrop,
        bounds = buttonBounds,
        enabled = glassActive,
        scrimBaseAlpha = 0.28f,
        cropStartX = 0.20f,
        cropEndX = 0.80f,
        cropStartY = 0.20f,
        cropEndY = 0.80f,
    )
    val contentColor by animateColorAsState(
        targetValue = if (glassActive) {
            if (useLightInk) Color.White else Color(0xFF111319)
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = androidx.compose.animation.core.tween(com.lastwave.app.ui.theme.ADAPTIVE_INK_FADE_MS),
        label = "homeDockButtonInk",
    )
    val comic = LocalComicTheme.current
    val containerColor = liquidGlassContainerColor(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        enabled = liquidGlass,
        backdrop = backdrop,
    )
    Box(modifier) {
        ExpandableHomeHoldPill(
            primaryIcon = Icons.Filled.Home,
            primaryLabel = androidx.compose.ui.res.stringResource(com.lastwave.app.R.string.feed_home),
            contentColor = contentColor,
            onTap = onClick,
            surfaceModifier = Modifier
                .trackBackdropBounds(buttonBounds)
                .shadow(if (liquidGlass || comic) 0.dp else 12.dp, CircleShape)
                .background(containerColor, CircleShape)
                .comicPanel(CircleShape, translucent = liquidGlass)
                .liquidGlassChrome(CircleShape, liquidGlass, LiquidGlassPreset.BottomNavigation, backdrop)
                .then(if (liquidGlass) Modifier.glassRim() else Modifier),
        )
    }
}

/** Size the flying cover is laid out at; the flight scales it to each end. */
private val FlightBaseSize = 320.dp
