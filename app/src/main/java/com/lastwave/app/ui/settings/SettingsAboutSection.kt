package com.lastwave.app.ui.settings

import com.lastwave.app.ui.comic.clipOnly
import com.lastwave.app.ui.common.selectableRefreshRates
import com.lastwave.app.ui.common.resolveRefreshRate
import com.lastwave.app.ui.common.REFRESH_RATE_AUTO
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.HorizontalDivider

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Vibration
import com.lastwave.app.data.local.musicHapticsConfig
import com.lastwave.app.playback.haptics.MusicHapticsConfig
import com.lastwave.app.playback.haptics.MusicHapticsEngineMode
import com.lastwave.app.playback.haptics.MusicHapticsIntensity
import com.lastwave.app.playback.haptics.MusicHapticsMode
import com.lastwave.app.ui.player.QualityGlyphs
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.FolderOpen
import com.lastwave.app.ui.jam.LastWaveJamSheet
import com.lastwave.app.ui.jam.LastWaveJamViewModel
import com.lastwave.app.ui.comic.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import com.lastwave.app.ui.common.HeaderActionIcon
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import com.lastwave.app.util.BatteryOptimizationHelper
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterTiltShift
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Waves
import com.lastwave.app.data.local.LyricsProvider
import com.lastwave.app.data.local.RecommendationSource
import com.lastwave.app.ui.player.LyricsOffsetDialog
import com.lastwave.app.ui.player.LyricsSizeDialog
import androidx.compose.material.icons.filled.FormatSize
import kotlin.math.roundToInt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.SpeakerPhone
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Extension
import com.lastwave.app.ui.comic.AlertDialog
import com.lastwave.app.ui.comic.Button
import androidx.compose.material3.ButtonDefaults
import com.lastwave.app.ui.comic.Card
import androidx.compose.material3.CardDefaults
import com.lastwave.app.ui.comic.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import com.lastwave.app.ui.comic.FilterChip
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.blur
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lastwave.app.ui.comic.FilledTonalButton
import com.lastwave.app.ui.comic.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import com.lastwave.app.ui.comic.OutlinedButton
import androidx.compose.material3.Slider
import com.lastwave.app.ui.comic.Surface
import com.lastwave.app.ui.comic.RadioButton
import com.lastwave.app.ui.comic.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.lastwave.app.ui.comic.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.lastwave.app.ui.comic.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.lastwave.app.data.local.AppLanguage
import com.lastwave.app.data.local.nativeDisplayName
import com.lastwave.app.data.lyrics.TRANSLATION_LANGUAGES
import com.lastwave.app.data.lyrics.translationLanguageName
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import java.util.Locale

import com.lastwave.app.ui.player.LocalMiniPlayerScrollClearance
import com.lastwave.app.R
import com.lastwave.app.data.local.AccentMode
import com.lastwave.app.data.local.ThemeMode
import com.lastwave.app.data.local.AppIcon
import com.lastwave.app.data.local.EQ_BAND_FREQS_HZ
import com.lastwave.app.data.local.EQ_MAX_GAIN_DB
import com.lastwave.app.data.local.EqualizerPresets
import com.lastwave.app.data.local.EqualizerSettings
import com.lastwave.app.playback.ClarityPresets
import com.lastwave.app.playback.LoudnessMode
import com.lastwave.app.data.local.eqBandLabel
import com.lastwave.app.ui.common.ConnectedButtonGroup
import com.lastwave.app.ui.common.ConnectedButtonItem
import com.lastwave.app.ui.common.ExpressiveHeader
import com.lastwave.app.ui.common.safeDrawingBottomPadding
import com.lastwave.app.ui.common.safeHorizontalContentPadding
import com.lastwave.app.ui.common.adaptiveContentWidth
import com.lastwave.app.ui.theme.ExpressivePillShape
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import kotlin.math.roundToInt
import kotlin.math.roundToLong


internal fun appVersionName(context: android.content.Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "5.0.0"
} catch (error: Exception) {
    "5.0.0"
} catch (error: LinkageError) {
    "5.0.0"
}

@Composable
internal fun ThemeModeSelectorCard(
    currentThemeMode: ThemeMode,
    onSelectThemeMode: (ThemeMode) -> Unit,
    position: GroupPosition = GroupPosition.SINGLE,
    isHighlighted: Boolean = false,
) {
    val shape = groupShape(position)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .settingHighlightGlow(isHighlighted = isHighlighted, shape = shape),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    Icons.Filled.BrightnessAuto,
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_theme_mode),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        stringResource(R.string.settings_theme_mode_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            val items = listOf(
                ConnectedButtonItem(stringResource(R.string.theme_mode_system), Icons.Filled.BrightnessAuto),
                ConnectedButtonItem(stringResource(R.string.theme_mode_light), Icons.Filled.LightMode),
                ConnectedButtonItem(stringResource(R.string.theme_mode_dark), Icons.Filled.DarkMode),
            )
            val selectedIdx = when (currentThemeMode) {
                ThemeMode.SYSTEM -> 0
                ThemeMode.LIGHT -> 1
                ThemeMode.DARK -> 2
            }
            ConnectedButtonGroup(
                items = items,
                selectedIndex = selectedIdx,
                onSelect = { idx ->
                    val mode = when (idx) {
                        0 -> ThemeMode.SYSTEM
                        1 -> ThemeMode.LIGHT
                        else -> ThemeMode.DARK
                    }
                    onSelectThemeMode(mode)
                },
            )
        }
    }
}

@Composable
internal fun RefreshRateSelectorCard(
    currentRate: Int,
    onSelectRate: (Int) -> Unit,
    position: GroupPosition = GroupPosition.SINGLE,
    isHighlighted: Boolean = false,
) {
    val shape = groupShape(position)
    val display = androidx.compose.ui.platform.LocalView.current.display
    val rates = remember(display) { display.selectableRefreshRates() }
    // A rate saved on another phone resolves to the nearest one this panel has.
    val selectedRate = if (currentRate == REFRESH_RATE_AUTO) REFRESH_RATE_AUTO else display.resolveRefreshRate(currentRate)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .settingHighlightGlow(isHighlighted = isHighlighted, shape = shape),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    Icons.Filled.Speed,
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.settings_refresh_rate),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        stringResource(R.string.settings_refresh_rate_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            val options = listOf(REFRESH_RATE_AUTO) + rates
            ConnectedButtonGroup(
                items = options.map { rate ->
                    ConnectedButtonItem(
                        if (rate == REFRESH_RATE_AUTO) stringResource(R.string.refresh_rate_auto)
                        else stringResource(R.string.refresh_rate_hz, rate),
                    )
                },
                selectedIndex = options.indexOf(selectedRate).coerceAtLeast(0),
                onSelect = { idx -> onSelectRate(options[idx]) },
            )
        }
    }
}

@Composable
internal fun AboutCard(
    versionName: String,
    appIcon: AppIcon = AppIcon.DEFAULT,
    onOpenChangelog: () -> Unit = {},
    onSecretAdminTrigger: () -> Unit = {},
) {
    val context = LocalContext.current
    var logoTapCount by remember { mutableIntStateOf(0) }
    var lastLogoTapMs by remember { mutableLongStateOf(0L) }
    Card(
        shape = CardOuterShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(colorResource(appIcon.backgroundRes))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        val now = android.os.SystemClock.elapsedRealtime()
                        logoTapCount = if (now - lastLogoTapMs <= 3_500L) logoTapCount + 1 else 1
                        lastLogoTapMs = now
                        if (logoTapCount >= 7) {
                            logoTapCount = 0
                            onSecretAdminTrigger()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(appIcon.logoRes),
                    contentDescription = stringResource(R.string.app_name),
                    tint = Color.Unspecified,
                    modifier = Modifier.size(72.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            val appName = stringResource(R.string.app_name)
            val wavesSplit = appName.lastIndexOf("waves", ignoreCase = true).takeIf { it > 0 } ?: appName.length
            val aboutWavesBrush = remember {
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0ECAE8),
                        Color(0xFF267CFF),
                        Color(0xFFB05CFF),
                        Color(0xFFFA4C6E),
                    ),
                )
            }
            Text(
                text = buildAnnotatedString {
                    append(appName.take(wavesSplit))
                    withStyle(SpanStyle(brush = aboutWavesBrush)) { append(appName.drop(wavesSplit)) }
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = ExpressivePillShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                onClick = onOpenChangelog,
            ) {
                Text(
                    "Version $versionName",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                // GPL v3 §5(d): the app must still say it is licensed under the GPL.
                text = "Maintained by aayepp • Licensed under GPL v3",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** OEM Settings/browser components are optional and occasionally broken on
 * custom ROMs. Never let an external activity failure escape a click event. */
internal fun startActivitySafely(context: android.content.Context, intent: Intent): Boolean {
    val safeIntent = Intent(intent).apply {
        if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        context.startActivity(safeIntent)
        true
    } catch (_: Exception) {
        false
    } catch (_: LinkageError) {
        false
    }
}

internal const val CLAUDEWAVES_REPO_URL = "https://github.com/aayepp/ClaudeWaves"
internal const val CLAUDEWAVES_RELEASES_URL = "https://github.com/aayepp/ClaudeWaves/releases/latest"

internal fun buildDeviceDebugSummary(
    context: android.content.Context,
    versionName: String,
    liquidGlassEnabled: Boolean,
): String {
    val versionCode = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(info)
    }.getOrDefault(0L)
    val abi = android.os.Build.SUPPORTED_ABIS.firstOrNull().orEmpty().ifBlank { "unknown" }
    val glassSupported = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val glassStatus = when {
        !glassSupported -> "Unsupported (API ${android.os.Build.VERSION.SDK_INT})"
        liquidGlassEnabled -> "Active"
        else -> "Off"
    }
    val refreshHz = runCatching {
        val dm = context.getSystemService(android.content.Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
        dm?.getDisplay(android.view.Display.DEFAULT_DISPLAY)?.refreshRate?.toInt()
    }.getOrNull()
    val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim()
    return buildString {
        appendLine("App: ClaudeWaves v$versionName ($versionCode)")
        appendLine("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        appendLine("Device: $device ($abi)")
        if (refreshHz != null && refreshHz > 0) appendLine("Display: ${refreshHz} Hz")
        appendLine("Liquid Glass: $glassStatus")
        append("Audio Engine: C++ / Oboe (16 KB aligned)")
    }
}

@Composable
internal fun DeviceDiagnosticsCard(
    versionName: String,
    liquidGlassEnabled: Boolean,
) {
    val context = LocalContext.current
    val versionCode = remember(context) {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(info)
        }.getOrDefault(0L)
    }
    val abi = remember { android.os.Build.SUPPORTED_ABIS.firstOrNull().orEmpty().ifBlank { "unknown" } }
    val refreshHz = remember(context) {
        runCatching {
            val dm = context.getSystemService(android.content.Context.DISPLAY_SERVICE) as? android.hardware.display.DisplayManager
            dm?.getDisplay(android.view.Display.DEFAULT_DISPLAY)?.refreshRate?.toInt()
        }.getOrNull()
    }
    val glassSupported = com.lastwave.app.ui.theme.isLiquidGlassBackdropSupported()
    val glassStatus = remember(liquidGlassEnabled, glassSupported) {
        when {
            !glassSupported -> "Fallback (Android < 12)"
            liquidGlassEnabled -> "Active"
            else -> "Off"
        }
    }
    val rows = remember(versionName, versionCode, abi, refreshHz, glassStatus) {
        listOf(
            "App Version" to "v$versionName ($versionCode)",
            "Android" to "${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})",
            "Device" to "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}".trim(),
            "Architecture" to abi,
            "Display" to (refreshHz?.takeIf { it > 0 }?.let { "${it} Hz" } ?: "Default"),
            "Liquid Glass" to glassStatus,
            "Audio Engine" to "C++ / Oboe • USB DAC ready",
        )
    }

    Card(
        shape = CardOuterShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Device & Audio Engine",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                com.lastwave.app.ui.comic.FilledTonalButton(
                    onClick = {
                        val summary = buildDeviceDebugSummary(context, versionName, liquidGlassEnabled)
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("ClaudeWaves Debug Info", summary))
                        android.widget.Toast.makeText(context, "Debug info copied", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("Copy Info", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GesturesTipsSheet(onDismiss: () -> Unit) {
    val tips = remember {
        listOf(
            "Scrub Bottom Bar" to "Drag your finger across the bottom navigation bar to glide smoothly between Feed, Stats, and Library.",
            "Pull Up Mini Player" to "Swipe up on the mini player to expand Now Playing with a flying artwork transition.",
            "Best Part Pill" to "Tap the glowing Best Part pill above the seek bar to jump straight to the chorus, viral bridge, or drop.",
            "Swipe Songs to Queue" to "Swipe track rows to quickly add songs to Up Next without opening menus.",
            "Long-Press Cards" to "Hold any song or shelf card to lift it into a quick floating preview & action tray.",
            "Reorder Home Shelves" to "Long-press any section header on Home to move, hide, or rearrange your Feed layout.",
            "Double-Tap Feed Tab" to "Tap the Feed tab again to jump back to top and refresh your recommendations.",
        )
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Gestures & Shortcuts",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            tips.forEach { (title, desc) ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
