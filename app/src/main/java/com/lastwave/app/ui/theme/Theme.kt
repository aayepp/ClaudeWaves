package com.lastwave.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import android.app.Activity
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.lastwave.app.data.repository.ThemeUiState

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.staticCompositionLocalOf
import com.lastwave.app.data.local.ThemeMode

/** Shared composition local indicating whether the active theme is dark mode. */
val LocalIsDarkTheme = staticCompositionLocalOf { true }
/** Shared composition local indicating whether bottom navigation tabs are hidden. */
val LocalHideNavTabs = staticCompositionLocalOf { false }
/** Settings "Use Bold Font": every text, lyrics included, drawn heavier. */
val LocalUseBoldFont = staticCompositionLocalOf { false }
/** True on constrained RAM / entry-level SoC devices (e.g. 2–4 GB phones). */
val LocalLowTierDevice = staticCompositionLocalOf { false }

/** Custom default profile chosen during startup tutorial (shown across Home UI & Settings). */
data class UserProfile(
    val name: String = "",
    val avatarUri: String? = null,
    val onboardingCompleted: Boolean = true,
)
val LocalUserProfile = staticCompositionLocalOf { UserProfile() }

/** Swallows every haptic request; provided app-wide when Settings > Haptic Feedback is off. */
private object SilentHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) = Unit
}

private const val THEME_TRANSITION_MS = 320

/** App-wide UI scale. Below 1 fits more on screen; 1f is the system's own size. */
private const val UI_SCALE = 0.95f

private fun lerpColorScheme(a: ColorScheme, b: ColorScheme, t: Float): ColorScheme = a.copy(
    primary = lerp(a.primary, b.primary, t),
    onPrimary = lerp(a.onPrimary, b.onPrimary, t),
    primaryContainer = lerp(a.primaryContainer, b.primaryContainer, t),
    onPrimaryContainer = lerp(a.onPrimaryContainer, b.onPrimaryContainer, t),
    inversePrimary = lerp(a.inversePrimary, b.inversePrimary, t),
    secondary = lerp(a.secondary, b.secondary, t),
    onSecondary = lerp(a.onSecondary, b.onSecondary, t),
    secondaryContainer = lerp(a.secondaryContainer, b.secondaryContainer, t),
    onSecondaryContainer = lerp(a.onSecondaryContainer, b.onSecondaryContainer, t),
    tertiary = lerp(a.tertiary, b.tertiary, t),
    onTertiary = lerp(a.onTertiary, b.onTertiary, t),
    tertiaryContainer = lerp(a.tertiaryContainer, b.tertiaryContainer, t),
    onTertiaryContainer = lerp(a.onTertiaryContainer, b.onTertiaryContainer, t),
    background = lerp(a.background, b.background, t),
    onBackground = lerp(a.onBackground, b.onBackground, t),
    surface = lerp(a.surface, b.surface, t),
    onSurface = lerp(a.onSurface, b.onSurface, t),
    surfaceVariant = lerp(a.surfaceVariant, b.surfaceVariant, t),
    onSurfaceVariant = lerp(a.onSurfaceVariant, b.onSurfaceVariant, t),
    surfaceTint = lerp(a.surfaceTint, b.surfaceTint, t),
    surfaceContainerLowest = lerp(a.surfaceContainerLowest, b.surfaceContainerLowest, t),
    surfaceContainerLow = lerp(a.surfaceContainerLow, b.surfaceContainerLow, t),
    surfaceContainer = lerp(a.surfaceContainer, b.surfaceContainer, t),
    surfaceContainerHigh = lerp(a.surfaceContainerHigh, b.surfaceContainerHigh, t),
    surfaceContainerHighest = lerp(a.surfaceContainerHighest, b.surfaceContainerHighest, t),
    outline = lerp(a.outline, b.outline, t),
    outlineVariant = lerp(a.outlineVariant, b.outlineVariant, t),
)

@Composable
private fun animateColorScheme(target: ColorScheme, animate: Boolean): ColorScheme {
    if (!animate) return target
    var fromScheme by remember { mutableStateOf(target) }
    var toScheme by remember { mutableStateOf(target) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(target) {
        if (target != toScheme) {
            fromScheme = lerpColorScheme(fromScheme, toScheme, progress.value)
            toScheme = target
            progress.snapTo(0f)
            progress.animateTo(1f, tween(THEME_TRANSITION_MS, easing = FastOutSlowInEasing))
        }
    }
    val t = progress.value
    return if (t >= 1f) toScheme else lerpColorScheme(fromScheme, toScheme, t)
}

/**
 * Wraps the whole app. Supports System Default, Light, and Dark modes.
 * Kyant0 Backdrop captures underlying content into a hardware-accelerated layer
 * shared app-wide; screens add their own sibling sources for local content
 * (nav bar, player, headers).
 */
@Composable
fun LastWaveTheme(
    themeState: ThemeUiState,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeState.themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val view = LocalView.current
    val context = LocalContext.current
    val isLowTier = remember(context) {
        if (view.isInEditMode) false else com.lastwave.app.util.DevicePerformanceTier.isLowTier(context)
    }
    // ThemeRepository resolves the scheme (now-playing artwork, else wallpaper
    // Monet on S+), so the UI, widgets and notification agree. Single progress animatable
    // glides smoothly on capable devices without 28 parallel coroutines.
    val activeColorScheme = animateColorScheme(
        target = if (isDark) themeState.darkColorScheme else themeState.lightColorScheme,
        animate = !isLowTier,
    )
    if (!view.isInEditMode) {
        SideEffect { applySystemBarIcons(view, isDark) }
        // The first application can be lost: on a cold start the window's bar
        // appearance was observed back at its default after composition, leaving
        // white status icons on a light page. Apply again once the window has
        // settled, and whenever the app returns to the foreground.
        LaunchedEffect(isDark) {
            delay(350)
            applySystemBarIcons(view, isDark)
            delay(1200)
            applySystemBarIcons(view, isDark)
        }
        LifecycleResumeEffect(isDark) {
            applySystemBarIcons(view, isDark)
            onPauseOrDispose { }
        }
    }

    val comic = themeState.isComic
    val sakuga = themeState.isSakuga
    val flatCel = comic || sakuga
    val typography = remember(themeState.useCustomFont, themeState.useBoldFont, comic) {
        when {
            !themeState.useCustomFont -> if (themeState.useBoldFont) SystemBoldTypography else SystemTypography
            themeState.useBoldFont -> LastWaveBoldTypography.let { if (comic) it.withComicHeadings() else it }
            else -> LastWaveTypography.let { if (comic) it.withComicHeadings() else it }
        }
    }

    MaterialTheme(
        colorScheme = activeColorScheme,
        typography = typography,
        shapes = LastWaveShapes,
    ) {
        val scheme = MaterialTheme.colorScheme
        val glass = themeState.liquidGlass && !flatCel
        val sakugaPalette = remember(scheme, isDark, themeState.mode) {
            deriveSakugaPalette(
                scheme = scheme,
                isDark = isDark,
                useOwnPalette = themeState.mode == com.lastwave.app.data.local.AccentMode.SAKUGA,
            )
        }
        val sakugaFx = remember { SakugaFxController() }
        SideEffect {
            sakugaFx.enabled = sakuga
            sakugaFx.mangaSfxEnabled = themeState.sakugaMangaSfx
            sakugaFx.palette = sakugaPalette
        }
        Surface(
            modifier = Modifier.fillMaxSize().drawBehind {
                drawPageWash(scheme.background, scheme.primary, scheme.tertiary, glass, isDark)
            },
            color = Color.Transparent,
            contentColor = scheme.onBackground,
        ) {
            // Settings > Haptic Feedback gates every LocalHapticFeedback call in
            // the app from this single point (the system "touch feedback"
            // setting still applies on top when it is on).
            val systemHaptics = LocalHapticFeedback.current
            val appHaptics = remember(systemHaptics, themeState.uiHaptics) {
                if (themeState.uiHaptics) systemHaptics else SilentHapticFeedback
            }
            // The system font has real weights, so Android's own "Bold text"
            // adjustment thickens it everywhere, even text with an explicit
            // fontWeight. Not used for the app font: its stroke is set by the
            // wght axis above, and adjusting the requested weight would only
            // fake-bold it on top.
            val defaultFontResolver = LocalFontFamilyResolver.current
            val fontResolver = remember(context, themeState.useBoldFont, themeState.useCustomFont, defaultFontResolver) {
                if (themeState.useBoldFont && !themeState.useCustomFont &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ) {
                    val config = Configuration(context.resources.configuration).apply {
                        fontWeightAdjustment = BOLD_FONT_BOOST.toInt()
                    }
                    createFontFamilyResolver(context.createConfigurationContext(config))
                } else {
                    defaultFontResolver
                }
            }
            // One knob for density: every dp and sp in the app shrinks together.
            val baseDensity = LocalDensity.current
            val compactDensity = remember(baseDensity) {
                Density(baseDensity.density * UI_SCALE, baseDensity.fontScale)
            }
            SideEffect { LiquidGlassIntensity.value = themeState.liquidGlassIntensity }
            GlassTiltEffect(enabled = glass && isDeviceGlassCapable() && !isLowTier)
            CompositionLocalProvider(
                LocalDensity provides compactDensity,
                LocalFontFamilyResolver provides fontResolver,
                LocalHapticFeedback provides appHaptics,
                LocalLowTierDevice provides isLowTier,
                // Comic/Sakuga panels are flat ink and paper; glass would blur the line work.
                LocalLiquidGlass provides (themeState.liquidGlass && !flatCel),
                LocalComicTheme provides flatCel,
                LocalSakugaTheme provides sakuga,
                LocalSakugaMangaSfx provides themeState.sakugaMangaSfx,
                LocalSakugaPalette provides sakugaPalette,
                LocalSakugaFx provides if (sakuga) sakugaFx else null,
                // Sakuga uses its anime accent; Comic night uses yellow or the picked accent's light tone.
                LocalComicAccent provides when {
                    sakuga -> sakugaPalette.accent
                    comic -> themeState.darkColorScheme.primary
                    else -> ComicGold
                },
                // Material pads small controls out to a 48dp box and applies the caller's
                // modifier to that box, so a comic/sakuga outline drawn there sat outside the
                // control: a second ring around every chip and icon button. Touch reach is
                // unaffected: hit testing still extends small targets (ViewConfiguration).
                LocalMinimumInteractiveComponentSize provides
                    if (flatCel) Dp.Unspecified else LocalMinimumInteractiveComponentSize.current,
                LocalComicLine provides activeColorScheme.outline,
                LocalIndication provides when {
                    sakuga -> SakugaIndication
                    comic -> ComicIndication
                    else -> LocalIndication.current
                },
                LocalMd3NavBar provides themeState.md3NavBar,
                LocalHideNavTabs provides themeState.hideNavTabs,
                LocalUseBoldFont provides themeState.useBoldFont,
                LocalIsDarkTheme provides isDark,
            ) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                    content()
                    if (sakuga) {
                        SakugaFxOverlay(
                            controller = sakugaFx,
                            modifier = Modifier.fillMaxSize().zIndex(1000f),
                        )
                    }
                }
            }
        }
    }
}

/** Dark status and navigation icons on a light page, light icons on a dark one; bars stay transparent. */
private fun applySystemBarIcons(view: android.view.View, isDark: Boolean) {
    var ctx: android.content.Context? = view.context
    while (ctx is ContextWrapper && ctx !is Activity) {
        ctx = ctx.baseContext
    }
    val window = (ctx as? Activity)?.window ?: return
    runCatching {
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !isDark
        controller.isAppearanceLightNavigationBars = !isDark
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }
}
