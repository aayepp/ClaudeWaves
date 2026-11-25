package com.lastwave.app.ui.shell

import com.lastwave.app.ui.comic.clipOnly
import com.lastwave.app.ui.theme.ComicInk
import com.lastwave.app.ui.theme.ComicAccent
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.comicPanel
import com.lastwave.app.ui.theme.rememberPageWash
import androidx.compose.ui.draw.drawBehind
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Search
import com.lastwave.app.ui.comic.AlertDialog
import com.lastwave.app.ui.comic.Button
import androidx.compose.material3.ButtonDefaults
import com.lastwave.app.ui.comic.Checkbox
import androidx.compose.material3.Icon
import com.lastwave.app.ui.comic.IconButton
import androidx.compose.material3.MaterialTheme
import com.lastwave.app.ui.comic.Surface
import androidx.compose.material3.Text
import com.lastwave.app.ui.comic.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.lastwave.app.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lastwave.app.ui.comic.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import android.view.HapticFeedbackConstants
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.platform.LocalView
import android.graphics.Bitmap
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.core.graphics.scale
import com.lastwave.app.ui.theme.drawInteractiveGlass
import com.lastwave.app.ui.theme.liquidGlass
import com.lastwave.app.ui.theme.rememberGlassInteraction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.IntBuffer
import kotlin.time.Duration.Companion.seconds
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.ui.common.AmbientAnimationsGate
import com.lastwave.app.ui.common.ExpressiveMotion
import com.lastwave.app.ui.common.PredictiveBackScreen
import com.lastwave.app.ui.common.adaptiveContentWidth
import com.lastwave.app.ui.feed.FeedScreen
import com.lastwave.app.ui.home.HomeScreen
import com.lastwave.app.ui.player.LocalChromeCollapse
import com.lastwave.app.ui.player.collapsedNavPill
import com.lastwave.app.ui.player.collapsingNavBar
import com.lastwave.app.ui.player.LocalMiniPlayerScrollClearance
import com.lastwave.app.ui.playlist.PlaylistScreen
import androidx.compose.foundation.shape.CornerBasedShape
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import androidx.compose.ui.draw.blur as drawBlur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.effects.lens
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.border
import kotlin.math.roundToInt
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.sign
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.LocalLiquidGlass
import com.lastwave.app.ui.theme.LiquidGlassIntensity
import com.lastwave.app.ui.theme.LocalMd3NavBar
import com.lastwave.app.ui.theme.LiquidGlassPreset
import com.lastwave.app.ui.theme.liquidGlassChrome
import com.lastwave.app.ui.theme.liquidGlassContainerColor
import com.lastwave.app.ui.theme.waterDropJiggle
import com.lastwave.app.ui.theme.BackdropBoundsHolder
import com.lastwave.app.ui.theme.LayerBackdrop
import com.lastwave.app.ui.theme.glassRim
import com.lastwave.app.ui.theme.SquircleShape
import com.lastwave.app.ui.theme.rememberBackdropUseLightInk
import com.lastwave.app.ui.theme.rememberLayerBackdrop
import com.lastwave.app.ui.theme.isLiquidGlassBackdropSupported
import com.lastwave.app.ui.theme.liquidGlassSource
import com.lastwave.app.ui.theme.trackBackdropBounds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState
import javax.inject.Inject

/** Thin bridge exposing AppUpdateManager's live update state to MainShell */
@HiltViewModel
class MainShellViewModel @Inject constructor(
    val appUpdateManager: com.lastwave.app.data.update.AppUpdateManager,
    val ytAuthManager: com.lastwave.app.data.ytmusic.YtMusicAuthManager,
    val settingsPreferences: com.lastwave.app.data.local.SettingsPreferences,
    addonHealthMonitor: com.lastwave.app.data.addon.AddonHealthMonitor,
) : ViewModel() {
    val updateInfo = appUpdateManager.updateInfo
    val ytConnection = ytAuthManager.connection
    val settings = settingsPreferences.settings

    /** Names of enabled addons whose launch health check found the link expired. */
    val expiredAddonNames = kotlinx.coroutines.flow.combine(
        addonHealthMonitor.healthMap,
        settingsPreferences.httpAddons,
    ) { health, addons ->
        addons.filter { it.enabled && health[it.id] == com.lastwave.app.data.addon.AddonHealth.Expired }
            .map { it.name.ifBlank { it.url } }
    }

    fun dismissUpdate(version: String) {
        appUpdateManager.dismissUpdate(version)
    }

    fun openUpdate(context: android.content.Context) {
        appUpdateManager.openUpdate(context)
    }

    fun setSuppressYouTubeLoginPrompt(suppress: Boolean) {
        viewModelScope.launch {
            settingsPreferences.setSuppressYouTubeLoginPrompt(suppress)
        }
    }
}

private enum class MainTab(val labelRes: Int) {
    FEED(com.lastwave.app.R.string.nav_feed),
    STATS(com.lastwave.app.R.string.nav_stats),
    PLAYLISTS(com.lastwave.app.R.string.nav_playlists),
}

/** Shared with any screen hosted inside [MainShell] so their scrolling
 *  lists know how much bottom content padding to reserve — the nav
 *  overlays content (it's not a Scaffold bottomBar reserving space), so
 *  each screen leaves this much room for its last item to clear it. */
object FloatingNavDefaults {
    val ContentBottomPadding = 112.dp

    /**
     * Full bottom clearance for edge-to-edge scrolling content: the floating
     * dock's visual height + margins ([ContentBottomPadding]) PLUS the live
     * navigation-bar (gesture area) inset. Screens that let their list draw
     * beneath the transparent gesture area must use this instead of the raw
     * constant, otherwise the last row hides behind the dock/gesture bar.
     */
    @Composable
    fun contentBottomPadding(): Dp {
        val hideNav = com.lastwave.app.ui.theme.LocalHideNavTabs.current
        val basePadding = when {
            hideNav -> 16.dp
            LocalMd3NavBar.current -> ContentBottomPadding
            else -> 80.dp
        }
        return basePadding +
            LocalMiniPlayerScrollClearance.current +
            WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
    }
}

private val DockShape: CornerBasedShape = RoundedCornerShape(32.dp)
private val PillShape: Shape = CircleShape


@Composable
fun MainShell(
    onOpenSettings: () -> Unit,
    onOpenOnboarding: () -> Unit = {},
    onOpenSearch: () -> Unit,
    onOpenDiscover: () -> Unit,
    onOpenGenres: () -> Unit,
    onOpenFriends: () -> Unit,
    onOpenFriendProfile: (username: String, displayName: String?, avatarUrl: String?) -> Unit = { _, _, _ -> },
    onOpenFeedPlaylist: (String) -> Unit,
    onOpenFeedSection: (String) -> Unit = {},
    onOpenPlaylist: (Long) -> Unit = {},
    onOpenGenerator: () -> Unit = {},
    onOpenNewReleases: () -> Unit = {},
    onOpenDownloads: () -> Unit = {},
    onOpenPodcasts: () -> Unit = {},
    onOpenPodcastShow: (String) -> Unit = {},
    onOpenYouTubeLogin: () -> Unit = {},
    onOpenModules: () -> Unit = {},
    onReturnToHome: () -> Boolean = { false },
    mainShellViewModel: MainShellViewModel = hiltViewModel(),
) {
    val tabs = MainTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val feedTapEvents = remember { kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Drive the dock selection from an explicit tab index instead of
    // pagerState.currentPage. currentPage flips mid-scroll (halfway through
    // animateScrollToPage), which re-triggers the pill width animation + the
    // generator FAB enter/exit while the pager is still moving — the two
    // competing size animations clip the dock (rectangular, limited) and let
    // the FAB draw over the pill. Updating immediately on tap and syncing from
    // settledPage on swipe keeps one clean transition.
    var selectedTabIndex by remember { mutableIntStateOf(pagerState.currentPage) }
    var liquidNavigationJob by remember { mutableStateOf<Job?>(null) }
    var liquidNavigationRequest by remember { mutableIntStateOf(0) }
    LaunchedEffect(pagerState.settledPage, pagerState.isScrollInProgress, liquidNavigationJob) {
        if (!pagerState.isScrollInProgress && liquidNavigationJob?.isActive != true &&
            selectedTabIndex != pagerState.settledPage
        ) {
            selectedTabIndex = pagerState.settledPage
        }
    }
    val requestedMainTabState = LocalRequestedMainTab.current
    val requestedTabName = requestedMainTabState?.value
    LaunchedEffect(requestedTabName) {
        val targetTab = when (requestedTabName) {
            "PLAYLISTS" -> MainTab.PLAYLISTS
            "STATS" -> MainTab.STATS
            "FEED" -> MainTab.FEED
            else -> null
        }
        if (targetTab != null) {
            val idx = tabs.indexOf(targetTab)
            if (idx >= 0) {
                selectedTabIndex = idx
                pagerState.scrollToPage(idx)
            }
            requestedMainTabState?.value = null
        }
    }
    val ytConnection by mainShellViewModel.ytConnection.collectAsStateWithLifecycle()
    val settings by mainShellViewModel.settings.collectAsStateWithLifecycle(initialValue = null)
    var promptDismissedThisSession by rememberSaveable { mutableStateOf(false) }

    val currentSettings = settings
    val shouldShowLoginPrompt = currentSettings != null &&
        !ytConnection.isConnected &&
        !currentSettings.suppressYouTubeLoginPrompt &&
        !promptDismissedThisSession

    if (shouldShowLoginPrompt) {
        var dontShowAgain by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = {
                if (dontShowAgain) mainShellViewModel.setSuppressYouTubeLoginPrompt(true)
                promptDismissedThisSession = true
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = if (LocalLiquidGlass.current) {
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            title = {
                Text(
                    text = "Get better recommendations",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Log in to YouTube Music, or create a free account, to get personalized songs and queues.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clipOnly(RoundedCornerShape(8.dp))
                            .clickable { dontShowAgain = !dontShowAgain }
                            .padding(vertical = 4.dp),
                    ) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Don't show again",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dontShowAgain) mainShellViewModel.setSuppressYouTubeLoginPrompt(true)
                        promptDismissedThisSession = true
                        onOpenYouTubeLogin()
                    },
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text("Take me to YT Music login")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (dontShowAgain) mainShellViewModel.setSuppressYouTubeLoginPrompt(true)
                        promptDismissedThisSession = true
                    },
                ) {
                    Text("OK, thanks")
                }
            },
        )
    }

    val expiredAddonNames by mainShellViewModel.expiredAddonNames.collectAsStateWithLifecycle(initialValue = emptyList())
    var expiredPromptDismissed by rememberSaveable { mutableStateOf(false) }
    // Waits for the login prompt so two dialogs never stack on launch.
    if (expiredAddonNames.isNotEmpty() && !expiredPromptDismissed && !shouldShowLoginPrompt) {
        AlertDialog(
            onDismissRequest = { expiredPromptDismissed = true },
            shape = RoundedCornerShape(28.dp),
            containerColor = if (LocalLiquidGlass.current) {
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            title = {
                Text(
                    text = pluralStringResource(R.plurals.addon_expired_title, expiredAddonNames.size),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.addon_expired_body, expiredAddonNames.joinToString(", ")),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        expiredPromptDismissed = true
                        onOpenModules()
                    },
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(stringResource(R.string.addon_expired_renew))
                }
            },
            dismissButton = {
                TextButton(onClick = { expiredPromptDismissed = true }) {
                    Text(stringResource(R.string.addon_expired_later))
                }
            },
        )
    }

    val pageWash = rememberPageWash()
    val hideNavTabs = com.lastwave.app.ui.theme.LocalHideNavTabs.current
    val useLiquidNavigation = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        LocalLiquidGlass.current && !hideNavTabs
    val navigationLiquidState = if (useLiquidNavigation) rememberLiquidState() else null
    val navGlass = isLiquidGlassBackdropSupported()
    val sharedBackdrop = com.lastwave.app.ui.theme.LocalLiquidGlassBackdrop.current as? com.lastwave.app.ui.theme.LayerBackdrop
    val navigationBackdrop = if (!useLiquidNavigation && navGlass) {
        sharedBackdrop ?: rememberLayerBackdrop {
            pageWash()
            drawContent()
        }
    } else null
    val ownsNavigationBackdrop = navigationBackdrop != null && navigationBackdrop !== sharedBackdrop
    val chromeCollapse = LocalChromeCollapse.current
    // Each tab keeps its own scroll offset: a compact bar carried onto a tab
    // resting at its top would have nothing to scroll back up to expand it.
    LaunchedEffect(chromeCollapse, pagerState.settledPage) { chromeCollapse?.expand() }

    Box(Modifier.fillMaxSize()) {
        val feedIndex = tabs.indexOf(MainTab.FEED)
        HorizontalPager(
            state = pagerState,
            // Keep every tab composed. With 1, jumping Playlists -> Home
            // (dock tap = instant scrollToPage) disposed and rebuilt the whole
            // Feed (sections, shelves, covers) on that frame, and Statistics
            // was rebuilt the same way from the far tab. Only 3 tabs, and
            // off-screen pages are not drawn, so this is cheap.
            beyondViewportPageCount = tabs.size - 1,
            modifier = Modifier.fillMaxSize()
                .then(if (chromeCollapse != null) Modifier.nestedScroll(chromeCollapse.connection) else Modifier)
                .then(
                if (navigationLiquidState != null) {
                    Modifier.liquefiable(navigationLiquidState).drawBehind(pageWash)
                } else {
                    Modifier.liquidGlassSource(if (ownsNavigationBackdrop) navigationBackdrop else null)
                },
            ),
        ) { page ->
            // derivedStateOf: reading currentPage directly recomposed every
            // page's whole tab each time it flipped halfway through a swipe.
            val isCurrent by remember(page) { derivedStateOf { page == pagerState.currentPage } }
            val pageShown by remember(page) {
                derivedStateOf { page == pagerState.currentPage || pagerState.isScrollInProgress }
            }
            AmbientAnimationsGate(visible = { pageShown }) {
                PredictiveBackScreen(
                    enabled = isCurrent && tabs[page] != MainTab.FEED,
                    onBack = {
                        scope.launch {
                            if (kotlin.math.abs(pagerState.currentPage - feedIndex) > 1) {
                                pagerState.scrollToPage(feedIndex)
                            } else {
                                pagerState.animateScrollToPage(
                                    feedIndex,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = 650f,
                                    ),
                                )
                            }
                        }
                    },
                ) {
                    when (tabs[page]) {
                        MainTab.FEED -> FeedScreen(
                            onOpenSettings = onOpenSettings,
                            onOpenOnboarding = onOpenOnboarding,
                            onOpenSearch = onOpenSearch,
                            onOpenDiscover = onOpenDiscover,
                            onOpenGenres = onOpenGenres,
                            onOpenPlaylist = onOpenPlaylist,
                            onOpenFeedPlaylist = onOpenFeedPlaylist,
                            onOpenFeedSection = onOpenFeedSection,
                            onOpenGenerator = onOpenGenerator,
                            onOpenFriends = onOpenFriends,
                            onOpenFriendProfile = onOpenFriendProfile,
                            onOpenNewReleases = onOpenNewReleases,
                            onOpenDownloads = onOpenDownloads,
                            onOpenPodcasts = onOpenPodcasts,
                            onOpenPodcastShow = onOpenPodcastShow,
                            onOpenYouTubeLogin = onOpenYouTubeLogin,
                            feedTapEvents = feedTapEvents,
                        )
                        MainTab.STATS -> HomeScreen(
                            onOpenSettings = onOpenSettings,
                            onOpenSearch = onOpenSearch,
                            onOpenDiscover = onOpenDiscover,
                            onOpenGenres = onOpenGenres,
                            onOpenFriends = onOpenFriends,
                            onOpenDownloads = onOpenDownloads,
                        )
                        MainTab.PLAYLISTS -> PlaylistScreen(
                            onOpenPlaylist = onOpenPlaylist,
                            onOpenFeedPlaylist = onOpenFeedPlaylist,
                            onOpenGenerator = onOpenGenerator,
                        )
                    }
                }
            }
        }

        // The update prompt is the app-wide dialog (ui/update/AppUpdateDialog), opened on launch.

        val bottomNavSlot = com.lastwave.app.ui.player.LocalBottomNavSlot.current
        val clearance = com.lastwave.app.ui.player.LocalMiniPlayerScrollClearance.current
        val isMiniPlayerActive = clearance > 0.dp
        val systemBottomInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()

        val currentSelectedIndex by androidx.compose.runtime.rememberUpdatedState(selectedTabIndex)
        val navHaptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        val currentOnSelect by androidx.compose.runtime.rememberUpdatedState { index: Int ->
            navHaptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.SegmentTick)
            if (tabs[index] == MainTab.FEED) {
                val wasOnDetail = onReturnToHome()
                if (wasOnDetail) {
                    if (selectedTabIndex != feedIndex) selectedTabIndex = feedIndex
                    scope.launch { pagerState.scrollToPage(feedIndex) }
                    return@rememberUpdatedState
                }
                if (pagerState.currentPage != feedIndex) {
                    if (selectedTabIndex != feedIndex) selectedTabIndex = feedIndex
                    scope.launch { pagerState.scrollToPage(feedIndex) }
                    return@rememberUpdatedState
                }
                feedTapEvents.tryEmit(Unit)
            } else {
                if (index != selectedTabIndex) selectedTabIndex = index
                scope.launch {
                    if (kotlin.math.abs(index - pagerState.currentPage) > 1) {
                        pagerState.scrollToPage(index)
                    } else {
                        pagerState.animateScrollToPage(
                            index,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = 650f,
                            ),
                        )
                    }
                }
            }
            Unit
        }
        val currentOnOpenSearch by androidx.compose.runtime.rememberUpdatedState(onOpenSearch)

        // A scrub commits once on release. It must never use Feed's re-tap
        // refresh action, or enqueue a page animation at every crossed tab.
        fun selectLiquidTab(index: Int, refreshIfSelected: Boolean) {
            navHaptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.SegmentTick)
            val isFeed = tabs[index] == MainTab.FEED
            val returnedFromDetail = isFeed && onReturnToHome()
            val alreadySettled = selectedTabIndex == index &&
                pagerState.settledPage == index && !pagerState.isScrollInProgress
            if (refreshIfSelected && isFeed && alreadySettled && !returnedFromDetail) {
                feedTapEvents.tryEmit(Unit)
                return
            }
            val request = ++liquidNavigationRequest
            liquidNavigationJob?.cancel()
            selectedTabIndex = index
            liquidNavigationJob = scope.launch {
                try {
                    if (returnedFromDetail || kotlin.math.abs(index - pagerState.currentPage) > 1) {
                        pagerState.scrollToPage(index)
                    } else {
                        pagerState.animateScrollToPage(
                            index,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = 650f,
                            ),
                        )
                    }
                } finally {
                    // A previous canceled request must not reset a newer target.
                    // Also reconcile when a content swipe interrupts this request.
                    if (request == liquidNavigationRequest) {
                        selectedTabIndex = pagerState.settledPage
                    }
                }
            }
        }
        val currentOnLiquidSelect by androidx.compose.runtime.rememberUpdatedState { index: Int ->
            selectLiquidTab(index, refreshIfSelected = true)
        }
        val currentOnScrubSelect by androidx.compose.runtime.rememberUpdatedState { index: Int ->
            selectLiquidTab(index, refreshIfSelected = false)
        }

        androidx.compose.runtime.DisposableEffect(navigationBackdrop, navigationLiquidState, tabs, hideNavTabs) {
            if (!hideNavTabs) {
                bottomNavSlot.value = { material3NavigationEnabled ->
                    Box(Modifier.fillMaxSize()) {
                        val collapse = LocalChromeCollapse.current
                        val collapseProgress = collapse?.progress ?: { 0f }
                        val barModifier = Modifier.align(Alignment.BottomCenter).collapsingNavBar(collapseProgress)
                        val homeOnRight = com.lastwave.app.ui.player.LocalHomeOnRight.current
                        val pillModifier = Modifier
                            .align(if (homeOnRight) Alignment.BottomEnd else Alignment.BottomStart)
                            .collapsedNavPill(collapseProgress)
                        val expand = { collapse?.expand(); Unit }
                        val barVisible = { collapseProgress() < 0.95f }
                        val pillVisible = { collapseProgress() > 0.05f }
                        if (navigationLiquidState != null) {
                            val items = tabs.map { tab ->
                                LiquidNavigationItem(
                                    label = androidx.compose.ui.res.stringResource(tab.labelRes),
                                    icon = tab.icon(),
                                    glyph = tab.glyph(),
                                )
                            }
                            LiquidNavigationBar(
                                liquidState = navigationLiquidState,
                                items = items,
                                selectedIndex = currentSelectedIndex,
                                onSelect = currentOnLiquidSelect,
                                onScrubSelect = currentOnScrubSelect,
                                modifier = barModifier,
                                onSearch = currentOnOpenSearch,
                                style = if (material3NavigationEnabled) {
                                    LiquidNavigationStyle.Material3
                                } else {
                                    LiquidNavigationStyle.Tabs
                                },
                                isVisible = barVisible,
                            )
                            LiquidNavigationPill(navigationLiquidState, items[currentSelectedIndex], expand, pillModifier, isVisible = pillVisible)
                        } else {
                            FloatingNavBar(
                                backdrop = navigationBackdrop,
                                tabs = tabs,
                                selectedIndex = currentSelectedIndex,
                                onSelect = currentOnSelect,
                                modifier = barModifier,
                                onOpenSearch = currentOnOpenSearch,
                                isVisible = barVisible,
                            )
                            FloatingNavPill(navigationBackdrop, tabs[currentSelectedIndex], expand, pillModifier, isVisible = pillVisible)
                        }
                    }
                }
            } else {
                bottomNavSlot.value = { _ -> }
            }
            onDispose { bottomNavSlot.value = { _ -> } }
        }
    }
}


@Composable
private fun FloatingNavBar(
    backdrop: LayerBackdrop?,
    tabs: List<MainTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
    isVisible: () -> Boolean = { true },
) {
    val liquidGlass = LocalLiquidGlass.current
    val glassNavBounds = remember { mutableStateMapOf<Int, Rect>() }
    val dockInteraction = remember { MutableInteractionSource() }
    val searchInteraction = remember { MutableInteractionSource() }
    val comic = LocalComicTheme.current
    val glassActive = liquidGlass && isLiquidGlassBackdropSupported() && backdrop != null
    val sharedBackdrop = com.lastwave.app.ui.theme.LocalLiquidGlassBackdrop.current as? com.lastwave.app.ui.theme.LayerBackdrop ?: backdrop
    val dockBackdropBounds = remember { BackdropBoundsHolder() }
    val searchBackdropBounds = remember { BackdropBoundsHolder() }
    val dockUseLightInk = com.lastwave.app.ui.theme.rememberBackdropUseLightInkPerSlot(
        backdrop = sharedBackdrop,
        bounds = dockBackdropBounds,
        enabled = glassActive,
        slots = tabs.size,
        scrimBaseAlpha = 0.28f,
        isVisible = isVisible,
    )
    val dockRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    val searchUseLightInk = rememberBackdropUseLightInk(
        backdrop = sharedBackdrop,
        bounds = searchBackdropBounds,
        enabled = glassActive,
        scrimBaseAlpha = 0.28f,
        cropStartX = 0.20f,
        cropEndX = 0.80f,
        cropStartY = 0.20f,
        cropEndY = 0.80f,
        isVisible = isVisible,
    )
    val searchContentColor by animateColorAsState(
        targetValue = if (glassActive) {
            if (searchUseLightInk) Color.White else Color(0xFF111319)
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(com.lastwave.app.ui.theme.ADAPTIVE_INK_FADE_MS),
        label = "floatingSearchInk",
    )


    Box(
        modifier = modifier
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        // No animateContentSize here: the dock pills already animate their own
        // width and the FAB animates its enter/exit size. Animating this outer
        // wrapper at the same time squeezes the Row mid-transition, clipping
        // the dock to a narrow rectangle and pushing the FAB over the pill.
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.adaptiveContentWidth(maxWidth = 500.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            Surface(
                shape = DockShape,
                color = liquidGlassContainerColor(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    enabled = liquidGlass,
                    backdrop = backdrop,
                ),
                tonalElevation = if (liquidGlass) 0.dp else 6.dp,
                shadowElevation = if (liquidGlass || comic) 0.dp else 12.dp,
                modifier = Modifier.weight(1f).widthIn(max = 420.dp).trackBackdropBounds(dockBackdropBounds).waterDropJiggle(amplitude = 0.045f, tiltDegrees = 2.1f, dipDp = 3.5.dp).comicPanel(DockShape, translucent = liquidGlass).liquidGlassChrome(DockShape, liquidGlass, LiquidGlassPreset.BottomNavigation, backdrop, interactionSource = dockInteraction)
                    .then(if (liquidGlass) Modifier.glassRim(DockShape) else Modifier),
            ) {
                val latestSelectedIndex by androidx.compose.runtime.rememberUpdatedState(selectedIndex)
                val latestOnSelect by androidx.compose.runtime.rememberUpdatedState(onSelect)
                val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
                Box(Modifier.padding(4.dp)) {
                NavSelectionPill(target = glassNavBounds[selectedIndex], backdrop = if (liquidGlass) backdrop else null)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(tabs) {
                            val bridge = 8.dp.toPx()
                            val hitIndex: (Offset) -> Int? = { pos ->
                                glassNavBounds.entries
                                    .firstOrNull { it.value.inflate(bridge).contains(pos) }
                                    ?.key
                            }
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val initialHit = hitIndex(down.position)
                                if (initialHit != null && initialHit != latestSelectedIndex) {
                                    latestOnSelect(initialHit)
                                }
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                    if (change == null || !change.pressed) break
                                    val currentHit = hitIndex(change.position)
                                    if (currentHit != null && currentHit != latestSelectedIndex) {
                                        // A tick per tab crossed while the pill is carried across.
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        latestOnSelect(currentHit)
                                    }
                                }
                            }
                        },
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val onClick = remember(index) { { onSelect(index) } }
                        FloatingNavItem(
                            label = androidx.compose.ui.res.stringResource(tab.labelRes),
                            icon = tab.icon(),
                            selected = selectedIndex == index,
                            onClick = onClick,
                            adaptiveUseLightInk = if (glassActive) dockUseLightInk[if (dockRtl) tabs.size - 1 - index else index] else null,
                            modifier = Modifier.weight(1f).onGloballyPositioned { coords ->
                                glassNavBounds[index] = coords.boundsInParent()
                            },
                        )
                    }
                }
                }
            }
            // Standalone search circle beside the dock, same surface.
            Surface(
                onClick = onOpenSearch,
                shape = CircleShape,
                color = liquidGlassContainerColor(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    enabled = liquidGlass,
                    backdrop = backdrop,
                ),
                contentColor = searchContentColor,
                tonalElevation = if (liquidGlass) 0.dp else 6.dp,
                shadowElevation = if (liquidGlass || comic) 0.dp else 12.dp,
                interactionSource = searchInteraction,
                modifier = Modifier.size(60.dp)
                    .trackBackdropBounds(searchBackdropBounds)
                    .waterDropJiggle(amplitude = 0.072f, tiltDegrees = 2.2f, dipDp = 3.dp)
                    .comicPanel(CircleShape, translucent = liquidGlass)
                    .liquidGlassChrome(CircleShape, liquidGlass, LiquidGlassPreset.BottomNavigation, backdrop, interactionSource = searchInteraction)
                    .then(if (liquidGlass) Modifier.glassRim() else Modifier),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = androidx.compose.ui.res.stringResource(com.lastwave.app.R.string.search_cd),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

/** The collapsed dock: one glass circle holding only the active tab's icon. */
@Composable
private fun FloatingNavPill(
    backdrop: LayerBackdrop?,
    tab: MainTab,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isVisible: () -> Boolean = { true },
) {
    val liquidGlass = LocalLiquidGlass.current
    val glassActive = liquidGlass && isLiquidGlassBackdropSupported() && backdrop != null
    val pillBounds = remember { BackdropBoundsHolder() }
    val useLightInk = rememberBackdropUseLightInk(
        backdrop = com.lastwave.app.ui.theme.LocalLiquidGlassBackdrop.current ?: backdrop,
        bounds = pillBounds,
        enabled = glassActive,
        scrimBaseAlpha = 0.28f,
        cropStartX = 0.20f,
        cropEndX = 0.80f,
        cropStartY = 0.20f,
        cropEndY = 0.80f,
        isVisible = isVisible,
    )
    val contentColor by animateColorAsState(
        targetValue = if (glassActive) {
            if (useLightInk) Color.White else Color(0xFF111319)
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(com.lastwave.app.ui.theme.ADAPTIVE_INK_FADE_MS),
        label = "floatingNavPillInk",
    )
    val comic = LocalComicTheme.current
    val containerColor = liquidGlassContainerColor(
        MaterialTheme.colorScheme.surfaceContainerHigh,
        enabled = liquidGlass,
        backdrop = backdrop,
    )
    Box(modifier) {
        ExpandableHomeHoldPill(
            primaryIcon = tab.icon(),
            primaryLabel = androidx.compose.ui.res.stringResource(tab.labelRes),
            contentColor = contentColor,
            onTap = onClick,
            surfaceModifier = Modifier
                .trackBackdropBounds(pillBounds)
                .shadow(if (liquidGlass || comic) 0.dp else 12.dp, CircleShape)
                .background(containerColor, CircleShape)
                .comicPanel(CircleShape, translucent = liquidGlass)
                .liquidGlassChrome(CircleShape, liquidGlass, LiquidGlassPreset.BottomNavigation, backdrop)
                .then(if (liquidGlass) Modifier.glassRim() else Modifier),
        )
    }
}

@Composable
private fun FloatingNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    adaptiveUseLightInk: Boolean? = null,
) {
    val comic = LocalComicTheme.current
    val contentColor by animateColorAsState(
        targetValue = when {
            adaptiveUseLightInk != null -> {
                val baseInk = if (adaptiveUseLightInk) Color.White else Color(0xFF111319)
                if (selected) baseInk else baseInk.copy(alpha = 0.72f)
            }
            !selected -> MaterialTheme.colorScheme.onSurfaceVariant
            comic -> ComicInk
            else -> MaterialTheme.colorScheme.onPrimaryContainer
        },
        animationSpec = ExpressiveMotion.effectsDefault(),
        label = "navItemContent",
    )

    // Every tab keeps its label: a dock of bare icons made the unselected tabs a guess.
    Surface(
        onClick = onClick,
        shape = PillShape,
        // The selection is [NavSelectionPill], drawn once behind the row.
        color = Color.Transparent,
        modifier = modifier.height(52.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            // Halo only over adaptive glass, where the backdrop can be any tone.
            val halo = if (adaptiveUseLightInk != null) com.lastwave.app.ui.theme.inkHalo(contentColor) else Color.Transparent
            Box(Modifier.size(22.dp)) {
                if (adaptiveUseLightInk != null) {
                    Icon(icon, null, tint = halo, modifier = Modifier.matchParentSize().drawBlur(3.dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded))
                }
                Icon(icon, null, tint = contentColor, modifier = Modifier.matchParentSize())
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(shadow = androidx.compose.ui.graphics.Shadow(halo, blurRadius = 8f)),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The dock's selection: one pill that travels between tabs with Auris' 3-spring physics
 * (position, lift swell with subtle landing dip, fast glass presence) and second-derivative
 * acceleration squash & stretch (stretches wide on launch, squashes tall while braking).
 */
@Composable
private fun NavSelectionPill(target: Rect?, backdrop: com.kyant.backdrop.Backdrop?) {
    val comic = LocalComicTheme.current
    val x = remember { Animatable(0f) }
    val width = remember { Animatable(0f) }
    val lift = remember { Animatable(0f) }
    val glassPresence = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    var lastSpeed by remember { mutableFloatStateOf(0f) }
    var accelDeform by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(target) {
        val to = target ?: return@LaunchedEffect
        if (!placed) {
            x.snapTo(to.left)
            width.snapTo(to.width)
            placed = true
            return@LaunchedEffect
        }
        lastSpeed = 0f
        accelDeform = 0f
        // Fast glass spring turns the pill into lens glass immediately on launch.
        launch { glassPresence.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 920f)) }
        launch { lift.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = 620f)) }
        launch { width.animateTo(to.width, spring(dampingRatio = 0.8f, stiffness = 500f)) }
        var shedStarted = false
        val totalDistance = kotlin.math.abs(to.left - x.value).coerceAtLeast(1f)
        x.animateTo(to.left, spring(dampingRatio = 0.78f, stiffness = 360f)) {
            val speed = kotlin.math.abs(velocity)
            val accel = speed - lastSpeed
            lastSpeed = speed
            // Positive accel (launching) stretches wide/flat; negative accel (braking) squashes tall/narrow.
            val targetDeform = (accel / 1400f).coerceIn(-0.14f, 0.18f)
            accelDeform += (targetDeform - accelDeform) * 0.45f
            val remaining = kotlin.math.abs(to.left - value) / totalDistance
            if (!shedStarted && remaining < 0.28f) {
                shedStarted = true
                // Shed glass as it lands while the scale spring finishes its subtle dip-and-settle.
                launch { glassPresence.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 680f)) }
                launch { lift.animateTo(0f, spring(dampingRatio = 0.68f, stiffness = 420f)) }
            }
        }
        accelDeform = 0f
        if (!shedStarted) {
            launch { glassPresence.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 680f)) }
            lift.animateTo(0f, spring(dampingRatio = 0.68f, stiffness = 420f))
        }
    }
    if (!placed || target == null) return

    val fill = if (comic) ComicAccent else MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (backdrop != null) 0.28f else 1f)
    Box(
        Modifier
            .layout { measurable, _ ->
                val w = width.value.roundToInt().coerceAtLeast(0)
                val h = target.height.roundToInt()
                val placeable = measurable.measure(androidx.compose.ui.unit.Constraints.fixed(w, h))
                layout(w, h) { placeable.place(x.value.roundToInt(), target.top.roundToInt()) }
            }
            .graphicsLayer {
                val speedStretch = (kotlin.math.abs(x.velocity) / 4800f).coerceAtMost(0.10f)
                val deform = (accelDeform + speedStretch).coerceIn(-0.12f, 0.20f)
                val swell = 1f + 0.11f * lift.value
                scaleX = swell + deform
                scaleY = swell - deform * 0.65f
            }
            .then(if (comic) Modifier.border(2.dp, ComicInk, PillShape) else Modifier)
            .drawBehind {
                val shed = if (backdrop != null) glassPresence.value else 0f
                drawOutline(PillShape.createOutline(size, layoutDirection, this), fill.copy(alpha = fill.alpha * (1f - shed)))
            },
    ) {
        if (backdrop != null) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = glassPresence.value }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { RoundedCornerShape(percent = 50) },
                        effects = {
                            vibrancy()
                            blur(4.dp.toPx())
                            lens(12.dp.toPx(), 18.dp.toPx(), LiquidGlassIntensity.value >= 0.45f)
                        },
                        highlight = { Highlight.Default },
                    ),
            )
        }
    }
}

private fun MainTab.glyph(): TabGlyph = when (this) {
    MainTab.FEED -> TabGlyph.HOME
    MainTab.STATS -> TabGlyph.STATS
    MainTab.PLAYLISTS -> TabGlyph.PLAYLISTS
}

private fun MainTab.icon(): ImageVector = when (this) {
    MainTab.FEED -> Icons.Filled.Home
    MainTab.STATS -> Icons.Filled.Leaderboard
    MainTab.PLAYLISTS -> Icons.AutoMirrored.Filled.QueueMusic
}
