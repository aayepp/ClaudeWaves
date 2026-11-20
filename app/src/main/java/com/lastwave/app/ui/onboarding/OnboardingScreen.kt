package com.lastwave.app.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import com.lastwave.app.ui.comic.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lastwave.app.data.local.HttpAddonConfig
import com.lastwave.app.data.local.MiscSettings
import com.lastwave.app.data.local.ThemeMode
import com.lastwave.app.data.repository.ThemeUiState
import com.lastwave.app.ui.theme.comicPanel
import com.lastwave.app.ui.theme.glassCardBorder
import com.lastwave.app.ui.theme.glassCardColor
import com.lastwave.app.ui.theme.pageBackground
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val ONBOARDING_PAGE_COUNT = 8

private val ACCENT_SWATCHES = listOf(
    Color(0xFF7C4DFF),
    Color(0xFFE91E63),
    Color(0xFF00BFA5),
    Color(0xFFFF6D00),
    Color(0xFF2979FF),
    Color(0xFFFFC400),
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onOpenYouTubeLogin: () -> Unit,
    onOpenSpotifyLogin: () -> Unit,
    isRepeatMode: Boolean = false,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val theme by viewModel.themeState.collectAsStateWithLifecycle()
    val ytConnected by viewModel.ytConnected.collectAsStateWithLifecycle()
    val spotifyConnected by viewModel.spotifyConnected.collectAsStateWithLifecycle()
    val httpAddons by viewModel.httpAddons.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == ONBOARDING_PAGE_COUNT - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pageBackground()
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
        ) {
            // Top bar: step indicator + Skip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(ONBOARDING_PAGE_COUNT) { idx ->
                        val active = idx == pagerState.currentPage
                        val width by animateDpAsState(
                            targetValue = if (active) 22.dp else 7.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "stepDot",
                        )
                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                                ),
                        )
                    }
                }
                TextButton(
                    onClick = { viewModel.completeOnboarding(onFinished) },
                ) {
                    Text(if (isRepeatMode) "Close" else "Skip")
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalOnboardingPageActive provides (pagerState.currentPage == page),
                ) {
                when (page) {
                    0 -> WowFeaturesSpotlightPage()
                    1 -> ThemeStylePage(
                        theme = theme,
                        onThemeMode = viewModel::setThemeMode,
                        onAmoled = viewModel::setAmoled,
                        onComic = viewModel::setComicTheme,
                        onLiquidGlass = viewModel::setLiquidGlass,
                        onAccent = viewModel::setManualAccent,
                    )
                    2 -> ProfileSetupPage(
                        settings = settings,
                        onSaveName = viewModel::saveProfileName,
                        onPickAvatar = viewModel::saveProfileAvatar,
                        onClearAvatar = viewModel::clearProfileAvatar,
                    )
                    3 -> AccountsSetupPage(
                        ytConnected = ytConnected,
                        spotifyConnected = spotifyConnected,
                        onConnectYouTube = onOpenYouTubeLogin,
                        onConnectSpotify = onOpenSpotifyLogin,
                    )
                    4 -> AddonsSetupPage(
                        httpAddons = httpAddons,
                        onImportCustom = viewModel::importCustomAddon,
                        onRemoveAddon = viewModel::removeAddon,
                    )
                    5 -> AudioQualityPage(
                        settings = settings,
                        onAudioPreset = viewModel::setAudioPreset,
                        onAutomix = viewModel::setAutomixEnabled,
                        onSmartCache = viewModel::setSmartOfflineCacheEnabled,
                    )
                    6 -> PermissionsLocalMusicPage()
                    7 -> InteractiveQuickTourPage()
                }
                }
            }

            // Bottom action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / $ONBOARDING_PAGE_COUNT",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        if (isLastPage) {
                            viewModel.completeOnboarding(onFinished)
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = if (isLastPage) "Get Started" else "Next",
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isLastPage) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageScaffold(
    icon: ImageVector,
    title: String,
    subtitle: String,
    /** A short looping demo of what this page sets up, shown under its title. */
    demo: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(56.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        demo()
        Spacer(Modifier.height(2.dp))
        content()
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun OnboardingCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val border = if (selected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        glassCardBorder()
    }
    Surface(
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
        } else {
            glassCardColor()
        },
        border = border,
        modifier = modifier
            .fillMaxWidth()
            .comicPanel(shape, offset = 3.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Box(Modifier.padding(16.dp)) { content() }
    }
}

// ── Page 1: Profile Setup ─────────────────────────────────────────────────────

@Composable
private fun ProfileSetupPage(
    settings: MiscSettings,
    onSaveName: (String) -> Unit,
    onPickAvatar: (android.net.Uri) -> Unit,
    onClearAvatar: () -> Unit,
) {
    var nameInput by rememberSaveable(settings.customProfileName) { mutableStateOf(settings.customProfileName) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onPickAvatar(uri)
    }

    OnboardingPageScaffold(
        icon = Icons.Filled.Person,
        title = "Set Up Your Profile",
        subtitle = "Set your username and profile picture. They appear across Home and Settings.",
        demo = { ProfileDemo() },
    ) {
        OnboardingCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { photoPicker.launch("image/*") },
                    contentAlignment = Alignment.Center,
                ) {
                    if (!settings.customProfileAvatarUri.isNullOrBlank()) {
                        AsyncImage(
                            model = settings.customProfileAvatarUri,
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Upload photo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { photoPicker.launch("image/*") }) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (settings.customProfileAvatarUri == null) "Upload Photo" else "Change Photo")
                    }
                    if (!settings.customProfileAvatarUri.isNullOrBlank()) {
                        OutlinedButton(onClick = onClearAvatar) {
                            Text("Remove")
                        }
                    }
                }
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        onSaveName(it)
                    },
                    label = { Text("Username / Display name") },
                    placeholder = { Text("e.g. Ayep") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ── Page 2: Accounts (YouTube Music & Spotify) ────────────────────────────────

@Composable
private fun AccountsSetupPage(
    ytConnected: Boolean,
    spotifyConnected: Boolean,
    onConnectYouTube: () -> Unit,
    onConnectSpotify: () -> Unit,
) {
    OnboardingPageScaffold(
        icon = Icons.Filled.LibraryMusic,
        title = "Link Your Music Accounts",
        subtitle = "Sign in to sync libraries, personalised feeds, and import your playlists.",
        demo = { AccountsDemo() },
    ) {
        AccountConnectCard(
            title = "YouTube Music",
            subtitle = "Sync liked songs, personalised Home feed, and playlists",
            connected = ytConnected,
            icon = Icons.Filled.PlayCircle,
            onClick = onConnectYouTube,
        )
        AccountConnectCard(
            title = "Spotify",
            subtitle = "Link your account to import playlists and animated Canvas",
            connected = spotifyConnected,
            icon = Icons.AutoMirrored.Filled.QueueMusic,
            onClick = onConnectSpotify,
        )
        Text(
            text = "Both are optional — you can connect or disconnect anytime in Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AccountConnectCard(
    title: String,
    subtitle: String,
    connected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    OnboardingCard(selected = connected, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (connected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Connected", tint = MaterialTheme.colorScheme.primary)
            } else {
                FilledTonalButton(onClick = onClick) { Text("Sign In") }
            }
        }
    }
}

// ── Page 3: Theme Style ───────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeStylePage(
    theme: ThemeUiState,
    onThemeMode: (ThemeMode) -> Unit,
    onAmoled: (Boolean) -> Unit,
    onComic: (Boolean) -> Unit,
    onLiquidGlass: (Boolean) -> Unit,
    onAccent: (Color) -> Unit,
) {
    OnboardingPageScaffold(
        icon = Icons.Filled.Palette,
        title = "Pick Your Style",
        subtitle = "Preview changes live. Switch anytime in Settings › Appearance.",
        demo = { ThemeDemo() },
    ) {
        OnboardingCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Theme Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "System" to ThemeMode.SYSTEM,
                        "Light" to ThemeMode.LIGHT,
                        "Dark" to ThemeMode.DARK,
                    ).forEach { (label, mode) ->
                        val selected = theme.themeMode == mode
                        if (selected) {
                            Button(onClick = { onThemeMode(mode) }, shape = CircleShape) { Text(label) }
                        } else {
                            OutlinedButton(onClick = { onThemeMode(mode) }, shape = CircleShape) { Text(label) }
                        }
                    }
                }
            }
        }
        OnboardingCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ToggleRow(
                    title = "Liquid Glass",
                    subtitle = "Translucent blurred glass bars, pills & specular rims",
                    checked = theme.liquidGlass && !theme.isComic,
                    onCheckedChange = { enabled ->
                        if (enabled) onComic(false)
                        onLiquidGlass(enabled)
                    },
                )
                ToggleRow(
                    title = "Comic Pop",
                    subtitle = "Bold ink outlines, paper feel & halftone accents",
                    checked = theme.isComic,
                    onCheckedChange = onComic,
                )
                ToggleRow(
                    title = "AMOLED Pure Black",
                    subtitle = "True #000000 background in Dark mode",
                    checked = theme.amoled,
                    onCheckedChange = onAmoled,
                )
            }
        }
        OnboardingCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Accent Color", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ACCENT_SWATCHES.forEach { swatch ->
                        val hex = String.format("#%06X", swatch.toArgb() and 0xFFFFFF)
                        val isPicked = theme.accentColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    width = if (isPicked) 3.dp else 1.dp,
                                    color = if (isPicked) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.35f),
                                    shape = CircleShape,
                                )
                                .clickable { onAccent(swatch) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isPicked) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Page 4: Website Addons ────────────────────────────────────────────────────

@Composable
private fun AddonsSetupPage(
    httpAddons: List<HttpAddonConfig>,
    onImportCustom: (name: String, url: String) -> Boolean,
    onRemoveAddon: (String) -> Unit,
) {
    var customUrl by rememberSaveable { mutableStateOf("") }
    var customName by rememberSaveable { mutableStateOf("") }
    var importFeedback by remember { mutableStateOf<String?>(null) }

    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    OnboardingPageScaffold(
        icon = Icons.Filled.Extension,
        title = "Get Your Hi-Res Addon",
        subtitle = "These websites make an addon link for you. Open one, generate your addon link there, copy it, then paste it below.",
        demo = { AddonDemo() },
    ) {
        ONBOARDING_ADDON_GENERATORS.forEach { site ->
            OnboardingCard(onClick = { uriHandler.openUri(site.url) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(site.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            site.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open ${site.name} website",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Text(
            "1. Tap a website above.\n2. Generate your addon link there and copy it.\n3. Come back and paste it in Addon URL below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OnboardingCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Paste Your Addon Link", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = customUrl,
                    onValueChange = {
                        customUrl = it
                        importFeedback = null
                    },
                    label = { Text("Addon URL") },
                    placeholder = { Text("Paste the link you generated") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Name (optional)") },
                    placeholder = { Text("My Addon") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        if (onImportCustom(customName, customUrl)) {
                            importFeedback = "Addon imported!"
                            customUrl = ""
                            customName = ""
                        } else {
                            importFeedback = "Enter a valid addon URL"
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import Addon")
                }
                importFeedback?.let { msg ->
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (httpAddons.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        httpAddons.forEach { addon ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(addon.name, style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        addon.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                IconButton(onClick = { onRemoveAddon(addon.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove addon")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Page 5: Audio Quality & Playback ──────────────────────────────────────────

@Composable
private fun AudioQualityPage(
    settings: MiscSettings,
    onAudioPreset: (preferLossless: Boolean, quality: Int) -> Unit,
    onAutomix: (Boolean) -> Unit,
    onSmartCache: (Boolean) -> Unit,
) {
    OnboardingPageScaffold(
        icon = Icons.Filled.HighQuality,
        title = "Audio Quality & Playback",
        subtitle = "Tune how LastWave streams, blends tracks, and caches songs for offline listening.",
        demo = { AudioDemo() },
    ) {
        val isHiRes = settings.preferLosslessStreaming
        val isHigh = !isHiRes && settings.wifiStreamingQuality >= 256
        val isAuto = !isHiRes && !isHigh

        OnboardingCard(selected = isHiRes, onClick = { onAudioPreset(true, 320) }) {
            QualityPresetRow(
                title = "Hi-Res Lossless (Recommended)",
                subtitle = "Prioritise FLAC / lossless addon streams when available",
                selected = isHiRes,
            )
        }
        OnboardingCard(selected = isHigh, onClick = { onAudioPreset(false, 256) }) {
            QualityPresetRow(
                title = "High Quality",
                subtitle = "256 kbps Opus/AAC — crisp sound with balanced data usage",
                selected = isHigh,
            )
        }
        OnboardingCard(selected = isAuto, onClick = { onAudioPreset(false, 128) }) {
            QualityPresetRow(
                title = "Data Saver / Auto",
                subtitle = "Lighter bandwidth for slower mobile connections",
                selected = isAuto,
            )
        }
        OnboardingCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ToggleRow(
                    title = "Automix Crossfade",
                    subtitle = "Smooth DJ-style transitions between songs",
                    checked = settings.crossfadeEnabled,
                    onCheckedChange = onAutomix,
                )
                ToggleRow(
                    title = "Smart Offline Cache",
                    subtitle = "Automatically cache top played & liked tracks on Wi-Fi",
                    checked = settings.smartOfflineCacheEnabled,
                    onCheckedChange = onSmartCache,
                )
            }
        }
    }
}

@Composable
private fun QualityPresetRow(title: String, subtitle: String, selected: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// ── Page 6: Permissions & Local Music ─────────────────────────────────────────

@Composable
private fun PermissionsLocalMusicPage() {
    val context = LocalContext.current
    val needsNotifPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val audioPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var notifGranted by remember {
        mutableStateOf(
            !needsNotifPerm || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var audioGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPerm) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifGranted = granted
    }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        audioGranted = granted
    }

    OnboardingPageScaffold(
        icon = Icons.Filled.FolderOpen,
        title = "Permissions & Local Music",
        subtitle = "Grant once now so playback controls and on-device songs work smoothly.",
        demo = { PermissionsDemo() },
    ) {
        OnboardingCard(selected = notifGranted) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("Playback Notifications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Show media controls on lock screen and download progress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (notifGranted) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Granted", tint = MaterialTheme.colorScheme.primary)
                } else if (needsNotifPerm) {
                    FilledTonalButton(onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text("Allow")
                    }
                }
            }
        }
        OnboardingCard(selected = audioGranted) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text("Local Device Audio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Scan and play FLAC, MP3, and WAV files stored on your phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (audioGranted) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Granted", tint = MaterialTheme.colorScheme.primary)
                } else {
                    FilledTonalButton(onClick = { audioLauncher.launch(audioPerm) }) {
                        Text("Allow")
                    }
                }
            }
        }
        Text(
            text = "Skip if you only stream online — you can enable local music later anytime.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Page 7: What Makes LastWave Different (WOW Spotlight) ─────────────────────

@Composable
private fun WowFeaturesSpotlightPage() {
    OnboardingPageScaffold(
        icon = Icons.Filled.AutoAwesome,
        title = "Welcome to LastWave",
        subtitle = "Built for listeners who want audiophile sound, living visuals, and effortless offline playback.",
        demo = { WelcomeDemo() },
    ) {
        WowSpotlightCard(
            icon = Icons.Filled.Headphones,
            badge = "AUDIOPHILE ENGINE",
            title = "Bit-Perfect USB DAC, AutoEQ & Parametric EQ",
            description = "Native C++/Oboe audio pipeline bypasses Android resampling on USB DACs, paired with thousands of headphone AutoEQ profiles and a 10-band parametric EQ.",
        )
        WowSpotlightCard(
            icon = Icons.Filled.GraphicEq,
            badge = "LIVING VISUALS",
            title = "Animated Covers & Synced Music Video",
            description = "Motion album covers play even offline once viewed, and you can flip into synced Music Video mode right inside Now Playing.",
        )
        WowSpotlightCard(
            icon = Icons.Filled.CloudDone,
            badge = "OFFLINE & HI-RES",
            title = "Smart Offline Cache & Hi-Res Addons",
            description = "Songs you finish playing are cached automatically for offline playback, while addon servers unlock Hi-Res FLAC streaming.",
        )
    }
}

@Composable
private fun WowSpotlightCard(
    icon: ImageVector,
    badge: String,
    title: String,
    description: String,
) {
    OnboardingCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Page 8: Interactive Quick Tour & Gestures ─────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InteractiveQuickTourPage() {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var swipedDone by remember { mutableStateOf(false) }
    var longPressDone by remember { mutableStateOf(false) }
    val swipeOffset = remember { Animatable(0f) }
    val liftScale by animateFloatAsState(
        targetValue = if (longPressDone) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "demoLift",
    )

    OnboardingPageScaffold(
        icon = Icons.Filled.TouchApp,
        title = "Quick Interactive Tour",
        subtitle = "Try the gestures below right now, plus key shortcuts across the app.",
        demo = { QuickTourDemo() },
    ) {
        // Try-It Sandbox #1: Swipe-to-Queue
        OnboardingCard(selected = swipedDone) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (swipedDone) Icons.Filled.CheckCircle else Icons.Filled.SwipeRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = if (swipedDone) "Queued! Nice swipe." else "Try: Swipe this song right to queue",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(swipeOffset.value.roundToInt(), 0) }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        if (swipeOffset.value > 72f) {
                                            swipedDone = true
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        swipeOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                    }
                                },
                                onDragCancel = {
                                    scope.launch { swipeOffset.animateTo(0f) }
                                },
                            ) { _, dragAmount ->
                                scope.launch {
                                    swipeOffset.snapTo((swipeOffset.value + dragAmount).coerceIn(0f, 160f))
                                }
                            }
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("Midnight Waves", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Drag right → Play Next", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Try-It Sandbox #2: Long-Press Lift Card
        OnboardingCard(selected = longPressDone) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (longPressDone) Icons.Filled.CheckCircle else Icons.Filled.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = if (longPressDone) "Lifted! Long-press opens quick menu." else "Try: Press & hold this card",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = liftScale
                            scaleY = liftScale
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .combinedClickable(
                            onClick = {},
                            onLongClick = {
                                longPressDone = true
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                        ),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("Hold any song or album card", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Lifts the card for instant actions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Cheat-sheet: Dock, Mini Player, Now Playing & Power Features
        OnboardingCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("More Shortcuts to Know", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                GestureTipRow(
                    icon = Icons.Filled.Tune,
                    title = "Dock & Mini Player",
                    detail = "Slide along the bottom tab pill to scrub tabs. Swipe the mini player left/right to skip, up to expand, or down to close.",
                )
                GestureTipRow(
                    icon = Icons.Filled.Lyrics,
                    title = "Now Playing & Lyrics",
                    detail = "Tap the Best part pill to jump straight to the chorus, swipe up for Queue, and toggle lyrics translation or romanization.",
                )
                GestureTipRow(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = "Power Features",
                    detail = "Automix DJ crossfade, Listen Together Jam rooms, Music Haptics, and offline Hi-Res playback are all ready inside Settings.",
                )
            }
        }
    }
}

@Composable
private fun GestureTipRow(icon: ImageVector, title: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
