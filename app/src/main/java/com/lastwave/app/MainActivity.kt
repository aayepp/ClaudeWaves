package com.lastwave.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.ui.common.ArtworkViewModel
import com.lastwave.app.ui.common.LocalArtworkViewModel
import com.lastwave.app.ui.common.LocalArtworkCellular
import com.lastwave.app.ui.common.LocalLosslessBadgeViewModel
import com.lastwave.app.ui.common.LosslessBadgeViewModel
import com.lastwave.app.util.AppLocaleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.lastwave.app.data.repository.LastFmAuthCallbackCoordinator
import com.lastwave.app.ui.navigation.LastWaveNavHost
import com.lastwave.app.ui.navigation.Screen
import com.lastwave.app.ui.player.PlayerHost
import com.lastwave.app.ui.theme.LastWaveTheme
import com.lastwave.app.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Last.fm redirects its Custom Tab to this Activity after approval.
@AndroidEntryPoint
open class MainShellActivity : androidx.fragment.app.FragmentActivity() {

    @Inject
    lateinit var lastFmAuthCallback: LastFmAuthCallbackCoordinator

    @Inject
    lateinit var linkPlaybackResolver: dagger.Lazy<com.lastwave.app.playback.LinkPlaybackResolver>

    @Inject
    lateinit var appRouteNavigator: dagger.Lazy<com.lastwave.app.ui.navigation.AppRouteNavigator>

    @Inject
    lateinit var settingsPreferences: com.lastwave.app.data.local.SettingsPreferences

    @Inject
    lateinit var appLocaleManager: AppLocaleManager

    @Inject
    lateinit var trackDownloadManager: dagger.Lazy<com.lastwave.app.data.download.TrackDownloadManager>

    @Inject
    lateinit var musicPlayer: dagger.Lazy<com.lastwave.app.playback.MusicPlayer>

    @Inject
    lateinit var idleColdStartCoordinator: dagger.Lazy<IdleColdStartCoordinator>

    private var freshlyCreated = false
    private var ignoreRestoreInstanceState = false

    private val audioPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching { trackDownloadManager.get().restoreDownloadsAfterReinstall() }
            }
        }
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        checkAndRequestAudioPermission()
    }

    private val systemSplashExited = androidx.compose.runtime.mutableStateOf(false)

    override fun attachBaseContext(newBase: Context) {
        // FragmentActivity gets no AppCompat locale backport, so pin the
        // selected locale here on every creation (all API levels).
        super.attachBaseContext(AppLocaleManager.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        StartupTrail.mark("activity.onCreate.start")
        // Must be called before super.onCreate() and before setContent().
        val splashScreen = runCatching { installSplashScreen() }
            .onFailure { android.util.Log.e(STARTUP_TAG, "Splash compatibility layer unavailable", it) }
            .getOrNull()
        val idleColdReset = IdleColdStartCoordinator.consumeColdRestartPending(this)
        ignoreRestoreInstanceState = idleColdReset
        val effectiveSavedState = if (idleColdReset) null else savedInstanceState
        if (idleColdReset && IdleColdStartPolicy.isRecentsHistoryLaunch(intent)) {
            setIntent(IdleColdStartPolicy.resolveCleanLauncherIntent(this))
        }
        super.onCreate(effectiveSavedState)
        if (effectiveSavedState == null) {
            freshlyCreated = true
            runCatching { idleColdStartCoordinator.get().noteUserActivity() }
        }
        // Only a launcher open gets the splash: a notification, widget or shared link
        // opens the app to show something, and the splash would cover it.
        val coldStart = effectiveSavedState == null &&
            intent?.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_LAUNCHER)
        systemSplashExited.value = !coldStart || splashScreen == null
        volumeControlStream = android.media.AudioManager.STREAM_MUSIC
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Adopt choices made via system Settings -> App languages so the
            // in-app picker shows the truth. The framework recreates us itself.
            lifecycleScope.launch {
                runCatching { appLocaleManager.syncFromSystemIfNeeded() }
            }
        } else {
            // No framework per-app locales here: recreate once per language
            // change so attachBaseContext re-wraps with the new locale.
            // drop(1) skips the initial emission — recreation never loops.
            lifecycleScope.launch {
                runCatching {
                    settingsPreferences.settings
                        .map { it.appLanguageTag }
                        .distinctUntilChanged()
                        .drop(1)
                        .collect { recreate() }
                }
            }
        }
        // Launch intents (play-from-search, shared/opened links, deep nav) must
        // only be consumed on the first creation. On rotation or config-change
        // recreation (effectiveSavedState != null) re-running them would re-fire
        // playback or re-navigate; subsequent intents arrive via onNewIntent.
        if (effectiveSavedState == null) {
            runCatching { lastFmAuthCallback.capture(intent) }
                .onFailure { android.util.Log.e(STARTUP_TAG, "Auth callback ignored during startup", it) }
            handlePlaybackIntent(intent)
            handleNavigationIntent(intent)
        }
        runCatching {
            splashScreen?.setOnExitAnimationListener { provider ->
                StartupTrail.mark("splash.exit.callback")
                // Hold ComicSplash at frame 0 until the system overlay hands off cleanly,
                // then immediately start the medallion bloom without a jump or pop.
                runCatching { provider.remove() }
                    .onFailure { android.util.Log.w(STARTUP_TAG, "Splash exit animation skipped", it) }
                systemSplashExited.value = true
            }
        }.onFailure {
            android.util.Log.w(STARTUP_TAG, "Splash exit listener unavailable", it)
            systemSplashExited.value = true
        }
        runCatching {
            enableEdgeToEdge(
                statusBarStyle = androidx.activity.SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
                navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ),
            )
        }
        applyRefreshRate()
        if (effectiveSavedState == null) {
            val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

            if (needsNotificationPermission) {
                runCatching { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
                    .onFailure {
                        android.util.Log.w(STARTUP_TAG, "Notification permission request skipped", it)
                        checkAndRequestAudioPermission()
                    }
            } else {
                checkAndRequestAudioPermission()
            }
        }

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeState by themeViewModel.uiState.collectAsStateWithLifecycle()

            androidx.compose.runtime.LaunchedEffect(themeState.refreshRate) {
                preferredRefreshRate = themeState.refreshRate
                applyRefreshRate()
            }

            LastWaveTheme(themeState = themeState) {
                val currentConfig = androidx.compose.ui.platform.LocalViewConfiguration.current
                val customViewConfig = androidx.compose.runtime.remember(currentConfig) {
                    object : androidx.compose.ui.platform.ViewConfiguration by currentConfig {
                        override val touchSlop: Float
                            get() = currentConfig.touchSlop * 1.5f
                        override val minimumTouchTargetSize: androidx.compose.ui.unit.DpSize
                            get() = androidx.compose.ui.unit.DpSize(48.dp, 48.dp)
                    }
                }
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalViewConfiguration provides customViewConfig
                ) {
                    val navController = rememberNavController()
                    val chromeFlags by androidx.compose.runtime.remember(navController, themeState.hideNavTabs) {
                        navController.currentBackStackEntryFlow
                            .map { entry ->
                                val route = entry.destination.route
                                ChromeFlags(
                                    hasBottomNavigation = route == Screen.MainShell.route && !themeState.hideNavTabs,
                                    isJamScreen = route == Screen.Jam.route ||
                                        route?.startsWith("jam") == true ||
                                        route?.startsWith("onboarding") == true,
                                    isWebLoginScreen = route == Screen.SpotifyLogin.route ||
                                        route == Screen.SpotifyTokenImport.route ||
                                        route == Screen.YouTubeLogin.route,
                                    offHome = route != Screen.MainShell.route &&
                                        route != Screen.Splash.route &&
                                        route != Screen.Login.route &&
                                        route?.startsWith("onboarding") != true,
                                    isSearchScreen = route == Screen.Search.route,
                                )
                            }
                            .distinctUntilChanged()
                    }.collectAsStateWithLifecycle(initialValue = ChromeFlags())
                    val (hasBottomNavigation, isJamScreen, isWebLoginScreen, offHome, isSearchScreen) = chromeFlags
                    // Popping keeps the shell's entry, so its pager returns on the tab last opened.
                    val goHome: () -> Unit = androidx.compose.runtime.remember(navController) {
                        {
                            if (!navController.popBackStack(Screen.MainShell.route, inclusive = false)) {
                                navController.navigate(Screen.MainShell.route) {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                            }
                        }
                    }

                    val artworkVm: ArtworkViewModel = hiltViewModel()
                    val losslessBadgeVm: LosslessBadgeViewModel = hiltViewModel()
                    val artworkCellular by artworkVm.isCellular.collectAsStateWithLifecycle()
                    val navContent = androidx.compose.runtime.remember(navController) {
                        @androidx.compose.runtime.Composable { LastWaveNavHost(navController) }
                    }

                    val userProfile by androidx.compose.runtime.remember {
                        settingsPreferences.settings
                            .map {
                                com.lastwave.app.ui.theme.UserProfile(
                                    name = it.customProfileName,
                                    avatarUri = it.customProfileAvatarUri,
                                    onboardingCompleted = it.onboardingCompleted,
                                )
                            }
                            .distinctUntilChanged()
                    }.collectAsStateWithLifecycle(initialValue = com.lastwave.app.ui.theme.UserProfile())

                    CompositionLocalProvider(
                        LocalArtworkViewModel provides artworkVm,
                        LocalArtworkCellular provides artworkCellular,
                        LocalLosslessBadgeViewModel provides losslessBadgeVm,
                        com.lastwave.app.ui.theme.LocalUserProfile provides userProfile,
                    ) {
                        PlayerHost(
                            hasBottomNavigation = hasBottomNavigation,
                            material3NavigationEnabled = themeState.md3NavBar,
                            hideMiniPlayer = isJamScreen,
                            webContentScreen = isWebLoginScreen,
                            // Search's own bar takes the dock's place, so no Home circle there.
                            onHome = goHome.takeIf { offHome && !isSearchScreen },
                            searchBarBelow = isSearchScreen,
                            content = navContent,
                        )
                        // Desktop-style in-app update dialog, shared by every screen.
                        com.lastwave.app.ui.update.AppUpdateDialogHost()
                        // Once per launch, over Home while it loads; not on rotation or recreate.
                        var showSplash by androidx.compose.runtime.saveable.rememberSaveable {
                            androidx.compose.runtime.mutableStateOf(coldStart)
                        }
                        if (showSplash) {
                            com.lastwave.app.ui.common.ComicSplash(
                                readyToAnimate = systemSplashExited.value,
                                onDone = { showSplash = false },
                            )
                        }
                        com.lastwave.app.ui.update.ChangelogHost(splashDismissed = !showSplash)
                    }
                }
            }
        }
        StartupTrail.mark("activity.content.set")
        // First drawn frame: if the process dies between content.set and
        // here, the killer lives in first composition (theme/backdrop/mini
        // player init). Past this point the UI is alive.
        runCatching {
            window.decorView.viewTreeObserver.addOnPreDrawListener(
                object : android.view.ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        runCatching {
                            window.decorView.viewTreeObserver.removeOnPreDrawListener(this)
                        }
                        StartupTrail.mark("activity.firstFrame")
                        return true
                    }
                },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        val coordinator = if (::idleColdStartCoordinator.isInitialized) idleColdStartCoordinator.get() else null
        if (freshlyCreated) {
            freshlyCreated = false
            coordinator?.noteForegroundVisible()
        } else if (coordinator != null && coordinator.checkAndTriggerColdRestartOnReturn(this, intent)) {
            return
        } else {
            coordinator?.noteForegroundVisible()
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        if (ignoreRestoreInstanceState) {
            ignoreRestoreInstanceState = false
            return
        }
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (::idleColdStartCoordinator.isInitialized) {
            runCatching { idleColdStartCoordinator.get().noteUserActivity() }
        }
    }

    override fun onResume() {
        super.onResume()
        applyRefreshRate()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyRefreshRate()
    }

    /**
     * Buffered log records are written on a 200ms cadence, so anything logged
     * just before the app leaves the foreground would otherwise sit unwritten
     * until the next launch.
     */
    override fun onStop() {
        super.onStop()
        com.lastwave.app.diagnostics.AppLog.flush()
    }

    /**
     * Marks the session as ended on purpose, so the next launch does not report
     * a phantom "ended without a clean-shutdown marker".
     *
     * `isFinishing` excludes configuration changes, which destroy and recreate
     * this activity while the session is plainly still alive.
     */
    override fun onDestroy() {
        if (isFinishing) {
            com.lastwave.app.diagnostics.Diagnostics.markCleanShutdown(this)
        }
        super.onDestroy()
    }

    /** Settings > Appearance > Refresh rate, in Hz; 0 asks for the panel's fastest. */
    private var preferredRefreshRate = com.lastwave.app.ui.common.REFRESH_RATE_AUTO

    /**
     * Ask the window scheduler for the chosen refresh rate, or the panel's
     * fastest on Auto. This is a preference, not a forced mode: Android can
     * still lower it for battery, thermals or a user's display setting, and
     * 60 Hz panels remain at 60 Hz.
     */
    @Suppress("DEPRECATION")
    private fun applyRefreshRate() {
        runCatching {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display ?: windowManager.defaultDisplay
            } else {
                windowManager.defaultDisplay
            }
            val currentMode = display.mode
            val matchingModes = display.supportedModes.filter {
                it.physicalWidth == currentMode.physicalWidth &&
                    it.physicalHeight == currentMode.physicalHeight
            }
            val wanted = preferredRefreshRate
            val targetMode = if (wanted == com.lastwave.app.ui.common.REFRESH_RATE_AUTO) {
                matchingModes.maxByOrNull { it.refreshRate }
            } else {
                matchingModes.minByOrNull { kotlin.math.abs(it.refreshRate - wanted) }
            } ?: currentMode
            val attributes = window.attributes
            var changed = false
            if (attributes.preferredDisplayModeId != targetMode.modeId) {
                attributes.preferredDisplayModeId = targetMode.modeId
                changed = true
            }
            if (attributes.preferredRefreshRate != targetMode.refreshRate) {
                attributes.preferredRefreshRate = targetMode.refreshRate
                changed = true
            }
            if (changed) {
                window.attributes = attributes
            }
            // Android 15+ votes on frame rate per view; requesting HIGH on Auto lets
            // 90/120/144/165 Hz panels run at peak smoothness during scroll and navigation
            // instead of capping idle/transition frames at 60 Hz.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                window.decorView.requestedFrameRate =
                    if (wanted == com.lastwave.app.ui.common.REFRESH_RATE_AUTO) {
                        android.view.View.REQUESTED_FRAME_RATE_CATEGORY_HIGH
                    } else {
                        targetMode.refreshRate
                    }
            }
        }
    }

    private fun handlePlaybackIntent(intent: Intent?) {
        if (intent?.action == android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
            val playbackIntent = Intent(this, com.lastwave.app.playback.MusicPlaybackService::class.java)
                .setAction(intent.action)
            intent.extras?.let { playbackIntent.putExtras(it) }
            androidx.core.content.ContextCompat.startForegroundService(this, playbackIntent)
            return
        }
        val uri = intent?.data
        val scheme = uri?.scheme.orEmpty().lowercase()
        val host = uri?.host.orEmpty().lowercase()
        val isSupportedMusicLink = intent?.action == Intent.ACTION_VIEW &&
            (scheme == "http" || scheme == "https") &&
            (host == "youtube.com" || host == "www.youtube.com" ||
                host == "m.youtube.com" || host == "music.youtube.com" || host == "youtu.be" ||
                host == "open.spotify.com" || host == "spotify.link")
        val isLocalAudioIntent = intent?.action == Intent.ACTION_VIEW &&
            (scheme == "content" || scheme == "file") &&
            uri != null
        val hasPlaybackTarget = isSupportedMusicLink || isLocalAudioIntent || intent?.action == Intent.ACTION_SEND
        if (hasPlaybackTarget) {
            runCatching { linkPlaybackResolver.get().handleIntent(intent) }
        }
    }

    private fun handleNavigationIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getStringExtra("widget_navigation") == "search") {
            runCatching { appRouteNavigator.get().navigateTo(Screen.Search.route) }
            return
        }
        if (intent.action == com.lastwave.app.widget.wave.WaveWidgetNavigation.ACTION_OPEN_NOW_PLAYING) {
            com.lastwave.app.widget.wave.WaveWidgetNavigation.requestOpen()
            return
        }
        val uri = intent.data
        if (uri != null &&
            ((uri.scheme == "lastwave" && (uri.host == "jam" || uri.host == "sync")) ||
                ((uri.scheme == "https" || uri.scheme == "http") &&
                    (uri.host == "jam.clashgram.org" || uri.host == "jam.clashprojects.qd.je")))
        ) {
            val code = uri.getQueryParameter("code")
            runCatching { appRouteNavigator.get().navigateTo(Screen.Jam.createRoute(code)) }
            return
        }
        val isDownloadsTarget = intent.action == com.lastwave.app.data.download.TrackDownloadManager.ACTION_VIEW_DOWNLOADS ||
            intent.getStringExtra(com.lastwave.app.data.download.TrackDownloadManager.EXTRA_NAVIGATE_TO) == Screen.Downloads.route
        if (isDownloadsTarget) {
            runCatching { appRouteNavigator.get().navigateTo(Screen.Downloads.route) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        freshlyCreated = false
        if (::idleColdStartCoordinator.isInitialized &&
            idleColdStartCoordinator.get().checkAndTriggerColdRestartOnReturn(this, intent)
        ) {
            return
        }
        if (::idleColdStartCoordinator.isInitialized) {
            runCatching { idleColdStartCoordinator.get().noteUserActivity() }
        }
        runCatching { lastFmAuthCallback.capture(intent) }
        handlePlaybackIntent(intent)
        handleNavigationIntent(intent)
    }

    private fun checkAndRequestAudioPermission() {
        if (hasPromptedAudioThisSession) return
        hasPromptedAudioThisSession = true

        if (com.lastwave.app.data.download.TrackDownloadManager.isDownloadsScanned(this)) return

        val requiredPermission = com.lastwave.app.data.download.TrackDownloadManager.getRequiredAudioPermission()
        if (checkSelfPermission(requiredPermission) == PackageManager.PERMISSION_GRANTED) {
            lifecycleScope.launch(Dispatchers.IO) {
                runCatching { trackDownloadManager.get().restoreDownloadsAfterReinstall() }
            }
            return
        }

        runCatching {
            audioPermissionLauncher.launch(requiredPermission)
        }.onFailure {
            android.util.Log.w(STARTUP_TAG, "Audio permission request skipped", it)
        }
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
            val player = runCatching { musicPlayer.get() }.getOrNull()
            if (player != null && player.state.value.isCasting) {
                when (event.keyCode) {
                    android.view.KeyEvent.KEYCODE_VOLUME_UP -> {
                        val current = player.state.value.appVolume
                        player.setAppVolume((current + 0.05f).coerceIn(0f, 1f))
                        return true
                    }
                    android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        val current = player.state.value.appVolume
                        player.setAppVolume((current - 0.05f).coerceIn(0f, 1f))
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private companion object {
        const val STARTUP_TAG = "LastWaveStartup"
        @Volatile
        var hasPromptedAudioThisSession = false
    }
}

/** Legacy alias class retained for backwards compatibility with any explicit intents. */
@AndroidEntryPoint
class MainActivity : MainShellActivity()

private data class ChromeFlags(
    val hasBottomNavigation: Boolean = false,
    val isJamScreen: Boolean = false,
    val isWebLoginScreen: Boolean = false,
    val offHome: Boolean = false,
    val isSearchScreen: Boolean = false,
)
