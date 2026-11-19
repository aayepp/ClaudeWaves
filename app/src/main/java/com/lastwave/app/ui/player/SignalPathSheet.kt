package com.lastwave.app.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import com.lastwave.app.ui.common.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lastwave.app.R
import com.lastwave.app.data.local.CELLULAR_QUALITY_AUTO
import com.lastwave.app.data.lossless.LosslessListTier
import com.lastwave.app.playback.AudioQualityTier
import com.lastwave.app.playback.InternalBackend
import com.lastwave.app.playback.InternalDirectAvailability
import com.lastwave.app.playback.InternalDirectStream
import com.lastwave.app.playback.InternalRoute
import com.lastwave.app.playback.MusicPlayer
import com.lastwave.app.playback.PathCheck
import com.lastwave.app.playback.SignalPathReport
import com.lastwave.app.playback.internalFailureDetail
import com.lastwave.app.playback.resolveAudioQualityTier
import com.lastwave.app.playback.MusicPlayerState
import com.lastwave.app.playback.qualityBadgeLabel
import com.lastwave.app.playback.isSpatialAudioCodec
import com.lastwave.app.ui.comic.Button
import com.lastwave.app.ui.comic.Surface
import com.lastwave.app.ui.comic.TextButton
import com.lastwave.app.ui.comic.clip
import com.lastwave.app.ui.common.QualityTierBadge
import java.util.Locale

/**
 * Signal path sheet opened from the quality badge (FLAC 24/96…). Shows only
 * measured values: the verdict, source → output, every [PathCheck], and live
 * stream health. BIT-PERFECT appears only when every check passes; drift
 * refreshes while playing (1 Hz report ticks).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalPathSheet(
    report: SignalPathReport,
    needsUsbPermission: Boolean,
    onRequestUsbAccess: () -> Unit,
    onDismiss: () -> Unit,
    internalAvailability: InternalDirectAvailability? = null,
    internalEnabled: Boolean = false,
    internalUnderruns: Long? = null,
    usbDiagnostic: (() -> String)? = null,
    player: MusicPlayer? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        // Measured values must stay readable whatever the glass intensity is.
        glass = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.signal_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    VerdictPill(report)
                }
                val verdict = verdictText(report)
                if (verdict.isNotEmpty()) {
                    Text(
                        verdict,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            report.bitPerfect -> MaterialTheme.colorScheme.primary
                            report.directToHal -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            SourceToOutput(report)

            if (player != null) NowPlayingQualitySelectorCard(player = player)

            SignalSection(stringResource(R.string.sig_checks)) {
                report.checks.forEach { CheckRow(it) }
            }

            SignalSection(stringResource(R.string.signal_health)) {
                HealthRow(
                    stringResource(R.string.signal_drift),
                    report.driftPpm?.let { String.format(Locale.ROOT, "%+.1f PPM", it) }
                        ?: if (report.isPlaying) stringResource(R.string.signal_measuring) else "—",
                )
                // Exclusive usbdevfs never uses AudioTrack. Gold is a stricter
                // verdict (clock + unity/FU + DSP off); do not call exclusive
                // output "Shared AudioTrack" just because a metadata check failed.
                val routeTrack = stringResource(
                    when {
                        report.internalDirectActive -> R.string.signal_internal_track
                        report.usbExclusiveActive || report.bitPerfect -> R.string.signal_direct_track
                        else -> R.string.signal_shared_track
                    },
                )
                val streamState = stringResource(if (report.isPlaying) R.string.signal_playing else R.string.signal_idle)
                HealthRow(
                    stringResource(R.string.signal_stream),
                    buildString {
                        append(streamState)
                        if (report.appRateHz > 0) append(" • ${report.appRateHz} Hz")
                        append(" • $routeTrack")
                    },
                )
                HealthRow(stringResource(R.string.signal_glitches), report.glitchCount.toString())
            }

            if (report.usbExclusiveActive) {
                SignalSection(stringResource(R.string.signal_usb_hardware_section)) {
                    HealthRow(stringResource(R.string.signal_usb_interface), stringResource(R.string.signal_usb_interface_value))
                    HealthRow(stringResource(R.string.signal_android_mixer), stringResource(R.string.signal_android_mixer_bypassed))
                    val epRate = if (report.outputRateHz > 0) report.outputRateHz else report.appRateHz
                    HealthRow(
                        stringResource(R.string.signal_internal_endpoint),
                        if (epRate > 0) {
                            stringResource(R.string.signal_internal_endpoint_value, epRate, "${report.outputBits}-bit PCM")
                        } else {
                            "—"
                        },
                    )
                }
            } else {
                InternalOutputSection(report, internalAvailability, internalEnabled, internalUnderruns)
            }

            if (usbDiagnostic != null) {
                val clipboard = LocalClipboardManager.current
                var copied by remember { mutableStateOf(false) }
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(usbDiagnostic()))
                        copied = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(if (copied) R.string.signal_usb_diag_copied else R.string.signal_usb_diag_copy))
                }
            }

            if (needsUsbPermission) {
                Button(onClick = onRequestUsbAccess, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.signal_grant_usb))
                }
            }
        }
    }
}

/** The one-line answer: what the source is and what actually reaches the output. */
@Composable
private fun SourceToOutput(report: SignalPathReport) {
    val tier = resolveAudioQualityTier(
        codec = report.sourceLabel,
        isLossless = report.isSourceLossless,
        samplingRateKHz = report.sourceRateHz?.let { it / 1000.0 },
        bitDepth = report.sourceBitDepth,
    )
    // Bit-perfect is the one result worth celebrating: the whole card lights up
    // and the arrow becomes a verified seal.
    val colors = MaterialTheme.colorScheme
    val (container, content) = if (report.bitPerfect) {
        colors.primaryContainer to colors.onPrimaryContainer
    } else {
        colors.surfaceContainerHigh to colors.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SignalCardShape)
            .background(container)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) {
            // The source label already reads "24/96kHz"; no second PCM line under it.
            PathEndpoint(
                label = stringResource(R.string.sig_node_source),
                title = report.sourceLabel,
                detail = null,
                badge = when (tier) {
                    AudioQualityTier.HI_RES -> LosslessListTier.HI_RES
                    AudioQualityTier.LOSSLESS -> LosslessListTier.LOSSLESS
                    AudioQualityTier.STANDARD -> null
                },
                modifier = Modifier.weight(1f),
            )
            if (report.bitPerfect) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Icon(
                        Icons.Filled.Verified,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        stringResource(R.string.signal_bit_perfect),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.primary,
                    )
                }
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colors.outline,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(20.dp),
                )
            }
            PathEndpoint(
            label = stringResource(R.string.sig_node_output),
            title = report.dacName?.takeIf { it.isNotBlank() } ?: report.outputEngineName,
            detail = pcmLabel(report.outputBits, report.outputRateHz.takeIf { it > 0 }),
            badge = null,
            modifier = Modifier.weight(1f),
            alignEnd = true,
        )
        }
    }
}

@Composable
private fun PathEndpoint(
    label: String,
    title: String,
    detail: String?,
    badge: LosslessListTier?,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    val align = if (alignEnd) TextAlign.End else TextAlign.Start
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        // Secondary lines dim the card's own content colour so they stay readable
        // on the bit-perfect highlight too.
        val secondary = LocalContentColor.current.copy(alpha = 0.72f)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = secondary,
        )
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = align,
        )
        if (detail != null) {
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = secondary,
                textAlign = align,
            )
        }
        if (badge != null) QualityTierBadge(badge, Modifier.padding(top = 4.dp))
    }
}

/** Unknown parts are left out rather than guessed (no assumed 16-bit / 44.1 kHz). */
private fun pcmLabel(bits: Int?, rateHz: Int?): String {
    val khz = rateHz?.let { hz ->
        val k = hz / 1000.0
        if (k % 1.0 == 0.0) "${k.toInt()} kHz" else "$k kHz"
    }
    return listOfNotNull(bits?.takeIf { it > 0 }?.let { "$it-bit" }, khz).joinToString(" / ").ifEmpty { "—" }
}

@Composable
private fun SignalSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = SignalCardShape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            content()
        }
    }
}

/** Every Signal Path section uses this card shape, padding 16, surfaceContainerHigh. */
private val SignalCardShape = RoundedCornerShape(16.dp)

@Composable
private fun CheckRow(check: PathCheck) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            if (check.passed) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = stringResource(if (check.passed) R.string.sig_check_passed else R.string.sig_check_altered),
            tint = if (check.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(check.labelRes),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                stringResource(check.detailRes, *check.detailArgs.toTypedArray()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InternalOutputSection(
    report: SignalPathReport,
    internalAvailability: InternalDirectAvailability?,
    internalEnabled: Boolean,
    internalUnderruns: Long?,
) {
    SignalSection(stringResource(R.string.signal_internal_section)) {
        val session = report.internalSession
        val route = session?.route ?: internalAvailability?.route ?: InternalRoute.UNKNOWN
        val routeName = session?.routeName ?: internalAvailability?.routeName.orEmpty()
        HealthRow(
            stringResource(R.string.signal_internal_route),
            routeLabel(route) + if (routeName.isNotBlank()) " • $routeName" else "",
        )
        HealthRow(
            stringResource(R.string.signal_internal_status),
            when {
                report.internalDirectActive -> stringResource(R.string.internal_reason_active)
                !internalEnabled -> stringResource(R.string.signal_internal_off)
                internalAvailability != null -> stringResource(internalFailureDetail(internalAvailability.reason))
                else -> "—"
            },
        )
        val probe = internalAvailability?.probe
        HealthRow(
            stringResource(R.string.signal_internal_endpoint),
            when {
                session != null -> stringResource(
                    R.string.signal_internal_endpoint_value,
                    session.deviceRateHz,
                    InternalDirectStream.labelOf(session.deviceEncoding),
                ) + when {
                    session.backend == InternalBackend.DIRECT_TRACK ->
                        " • " + stringResource(R.string.signal_internal_backend_direct)
                    session.mmap -> " • MMAP"
                    else -> ""
                }
                probe != null && probe.usable -> stringResource(
                    R.string.signal_internal_endpoint_value,
                    probe.sampleRateHz,
                    InternalDirectStream.labelOf(probe.encoding),
                ) + if (probe.mmap) " • MMAP" else ""
                probe != null && internalAvailability != null ->
                    stringResource(internalFailureDetail(internalAvailability.reason))
                else -> stringResource(R.string.signal_internal_not_checked)
            },
        )
        if (internalUnderruns != null) {
            HealthRow(stringResource(R.string.signal_internal_underruns), internalUnderruns.toString())
        }
    }
}

/** Verdict line: confirmation when perfect, otherwise the first failing check. */
@Composable
private fun verdictText(report: SignalPathReport): String {
    if (report.bitPerfect) return stringResource(R.string.signal_verdict_ok)
    if (report.directToHal && report.parametricEqActive) return stringResource(R.string.signal_verdict_direct_eq)
    if (report.directToHal) return stringResource(R.string.signal_verdict_direct_hal)
    val failing = report.checks.firstOrNull { !it.passed } ?: return ""
    return stringResource(failing.labelRes) +
        ": " +
        stringResource(failing.detailRes, *failing.detailArgs.toTypedArray())
}

@Composable
private fun VerdictPill(report: SignalPathReport) {
    val colors = MaterialTheme.colorScheme
    val (bg, fg) = when {
        report.bitPerfect -> colors.primary to colors.onPrimary
        report.directToHal -> colors.tertiary to colors.onTertiary
        report.clockFallbackResampled -> colors.errorContainer to colors.onErrorContainer
        else -> colors.surfaceContainerHighest to colors.onSurfaceVariant
    }
    val text = when {
        report.bitPerfect -> stringResource(R.string.signal_bit_perfect)
        report.directToHal && report.parametricEqActive -> stringResource(R.string.signal_direct_eq)
        report.directToHal -> stringResource(R.string.signal_direct_hal)
        report.clockFallbackResampled && !report.internalDirectActive -> stringResource(R.string.signal_resampled_dac)
        else -> stringResource(R.string.signal_check_path)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = fg)
    }
}

@Composable
private fun HealthRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun routeLabel(route: InternalRoute): String = stringResource(
    when (route) {
        InternalRoute.SPEAKER -> R.string.route_speaker
        InternalRoute.WIRED -> R.string.route_wired
        InternalRoute.BLUETOOTH -> R.string.route_bluetooth
        InternalRoute.USB -> R.string.route_usb
        InternalRoute.OTHER -> R.string.route_other
        InternalRoute.UNKNOWN -> R.string.route_unknown
    },
)

private data class NowPlayingQualityOption(
    val code: Int,
    val title: String,
    val subtitle: String,
    val badge: String,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NowPlayingQualitySelectorCard(
    player: MusicPlayer,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val isCellular = player.isCellularRouteActive
    var selectedQuality by remember(isCellular) { mutableIntStateOf(player.activeStreamingQualityPref) }
    val switchingTo by player.qualityChangeInFlight.collectAsState()
    val playerState by player.state.collectAsState()
    // The cells pick a preference; the stream can still be lower when a song has no copy at
    // that quality (a Hi-Res pick playing 320 kbps). Mark what is actually playing so the
    // highlighted cell never reads as the playing quality.
    val playingCode = playerState.current?.let { playingQualityCode(playerState) }
    val options = remember(isCellular) {
        buildList {
            if (isCellular) {
                add(NowPlayingQualityOption(CELLULAR_QUALITY_AUTO, "Auto", "Adaptive for mobile data", "AUTO"))
            }
            add(NowPlayingQualityOption(28, "Dolby Atmos", "Spatial Immersive Audio", "ATMOS"))
            add(NowPlayingQualityOption(27, "Hi-Res Lossless", "Up to 24-bit / 192 kHz FLAC", "HI-RES"))
            add(NowPlayingQualityOption(6, "CD Lossless", "16-bit / 44.1 kHz FLAC", "CD"))
            add(NowPlayingQualityOption(5, "Standard 320k", "Up to 320 kbps AAC / MP3", "320K"))
            add(NowPlayingQualityOption(-1, "YouTube Music", "128–256 kbps Original", "YT"))
            add(NowPlayingQualityOption(4, "Data Saver", "Low-bitrate stream", "SAVER"))
        }
    }

    Surface(
        shape = SignalCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Streaming Quality",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (isCellular) "Active route: Mobile Data • Live hot-swap" else "Active route: Wi-Fi • Live hot-swap",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val picked = options.firstOrNull { it.code == selectedQuality }
                    if (playingCode != null && picked != null && playingCode != selectedQuality && switchingTo == null) {
                        Text(
                            text = "Playing ${qualityBadgeLabel(playerState)} — ${picked.title} isn't available for this song",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 2,
            ) {
                options.forEach { opt ->
                    val isSelected = selectedQuality == opt.code
                    val isPlaying = playingCode == opt.code && !isSelected
                    // primary, not primaryContainer: in monochrome and some light schemes the
                    // container sat at the same grey as the unselected cells.
                    val bgColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
                    }
                    val contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bgColor,
                        border = if (isPlaying) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                                selectedQuality = opt.code
                                player.setActiveStreamingQuality(opt.code)
                            },
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = opt.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isPlaying) "PLAYING" else opt.badge,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = opt.subtitle,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                                color = contentColor.copy(alpha = 0.78f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (isSelected && switchingTo == opt.code) {
                                // matchParentSize: the indicator's 240dp default width must not
                                // count, or FlowRow pushed this cell onto a full row of its own.
                                Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(3.dp)) {
                                    LinearProgressIndicator(
                                        color = contentColor,
                                        trackColor = contentColor.copy(alpha = 0.25f),
                                        modifier = Modifier.matchParentSize().clip(CircleShape),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The quality cell matching the stream that is actually playing (see [NowPlayingQualitySelectorCard]). */
private fun playingQualityCode(state: MusicPlayerState): Int = when {
    isSpatialAudioCodec(state.audioCodec) -> 28
    else -> when (resolveAudioQualityTier(state.audioCodec, state.isLossless, state.samplingRateKHz, state.bitDepth)) {
        AudioQualityTier.HI_RES -> 27
        AudioQualityTier.LOSSLESS -> 6
        AudioQualityTier.STANDARD ->
            if (state.audioCodec?.contains("320") == true || (state.bitrateKbps ?: 0) >= 300) 5 else -1
    }
}
