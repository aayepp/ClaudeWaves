package com.lastwave.app.ui.shell

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lastwave.app.data.local.HomeHoldShortcut
import com.lastwave.app.ui.player.CompactPillSize
import com.lastwave.app.ui.player.LocalHomeOnRight
import com.lastwave.app.ui.player.LocalSetHomePillExtraWidth
import com.lastwave.app.ui.theme.ComicAccent
import com.lastwave.app.ui.theme.ComicInk
import com.lastwave.app.ui.theme.LocalComicTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private const val HomeHoldActivationMs = 150L
internal val ExpandedHomePillWidth = 140.dp
private val HomePillExtraMax = ExpandedHomePillWidth - CompactPillSize // 84.dp

/** Active customizable 2nd shortcut revealed on long-press of the Home / docked pill. */
internal val LocalHomeHoldShortcut = staticCompositionLocalOf { HomeHoldShortcut.LIBRARY }

/** Callback executing the chosen 2nd shortcut (switches MainShell tab or navigates cleanly). */
internal val LocalOnHomeHoldShortcut = staticCompositionLocalOf<(HomeHoldShortcut) -> Unit> { {} }

/** Shared request to select a MainShell tab after popping/jumping to the main tabs. */
internal val LocalRequestedMainTab = staticCompositionLocalOf<MutableState<String?>?> { null }

internal fun HomeHoldShortcut.icon(): ImageVector = when (this) {
    HomeHoldShortcut.LIBRARY -> Icons.AutoMirrored.Filled.QueueMusic
    HomeHoldShortcut.STATS -> Icons.Filled.Leaderboard
    HomeHoldShortcut.DOWNLOADS -> Icons.Filled.Download
    HomeHoldShortcut.SEARCH -> Icons.Filled.Search
    HomeHoldShortcut.DISCOVER -> Icons.Filled.Explore
    HomeHoldShortcut.JAM -> Icons.Filled.People
    HomeHoldShortcut.GENERATOR -> Icons.Filled.AutoAwesome
    HomeHoldShortcut.SETTINGS -> Icons.Filled.Settings
}

/**
 * Wraps the compact 56dp Home / docked navigation pill so:
 *  - short tap runs [onTap] as before;
 *  - press-and-hold expands smoothly sideways (`56dp -> 140dp`), shifting the MiniPlayer via
 *    [LocalSetHomePillExtraWidth], revealing `[Home | <2nd Shortcut>]` (or reversed on right edge)
 *    and letting the user slide directly onto the 2nd shortcut before releasing.
 */
@Composable
internal fun ExpandableHomeHoldPill(
    primaryIcon: ImageVector,
    primaryLabel: String,
    contentColor: Color,
    onTap: () -> Unit,
    onHoldPrimary: () -> Unit = onTap,
    surfaceModifier: Modifier = Modifier,
    modifier: Modifier = Modifier,
) {
    val shortcut = LocalHomeHoldShortcut.current
    val onShortcut = LocalOnHomeHoldShortcut.current
    val homeOnRight = LocalHomeOnRight.current
    val setExtraWidth = LocalSetHomePillExtraWidth.current
    val comic = LocalComicTheme.current
    val liquidStudio = com.lastwave.app.ui.theme.LocalLiquidStudioStyle.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val latestTap by rememberUpdatedState(onTap)
    val latestHoldPrimary by rememberUpdatedState(onHoldPrimary)
    val latestShortcut by rememberUpdatedState(shortcut)
    val latestOnShortcut by rememberUpdatedState(onShortcut)
    val latestHomeOnRight by rememberUpdatedState(homeOnRight)
    val latestSetExtraWidth by rememberUpdatedState(setExtraWidth)

    val expandAnim = remember { Animatable(0f) }
    var isHolding by remember { mutableStateOf(false) }
    var selectedSlot by remember { mutableIntStateOf(0) } // 0 = Home / primary, 1 = 2nd shortcut

    DisposableEffect(setExtraWidth) {
        onDispose { setExtraWidth(0.dp) }
    }

    val currentWidth: Dp = CompactPillSize + HomePillExtraMax * expandAnim.value.coerceIn(0f, 1.05f)
    val selectionAnim by animateFloatAsState(
        targetValue = selectedSlot.toFloat(),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 1200f),
        label = "homeHoldSlotSlide",
    )

    val slot0Tint by animateColorAsState(
        targetValue = if (selectedSlot == 0 && expandAnim.value > 0.2f) {
            if (comic) ComicInk else MaterialTheme.colorScheme.primary
        } else {
            contentColor.copy(alpha = if (expandAnim.value > 0.2f) 0.72f else 1f)
        },
        animationSpec = tween(70),
        label = "homeHoldSlot0Tint",
    )
    val slot1Tint by animateColorAsState(
        targetValue = if (selectedSlot == 1 && expandAnim.value > 0.2f) {
            if (comic) ComicInk else MaterialTheme.colorScheme.primary
        } else {
            contentColor.copy(alpha = 0.72f)
        },
        animationSpec = tween(70),
        label = "homeHoldSlot1Tint",
    )

    Box(
        modifier = modifier
            .height(CompactPillSize)
            .width(currentWidth)
            .graphicsLayer {
                if (liquidStudio) {
                    val bounce = kotlin.math.sin(expandAnim.value.coerceIn(0f, 1f) * kotlin.math.PI.toFloat())
                    scaleY = 1f + 0.045f * bounce
                }
            }
            .then(surfaceModifier)
            .clip(CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = primaryLabel
                onClick {
                    latestTap()
                    true
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val initialX = down.position.x
                    val initialY = down.position.y
                    val touchSlop = viewConfiguration.touchSlop
                    val slideThresholdPx = 12.dp.toPx()
                    val deadline = SystemClock.uptimeMillis() + HomeHoldActivationMs
                    var holdActivated = false
                    var lastDx = 0f
                    selectedSlot = 0

                    try {
                        while (true) {
                            val remaining = if (holdActivated) Long.MAX_VALUE
                            else (deadline - SystemClock.uptimeMillis()).coerceAtLeast(0L)
                            val event = if (holdActivated) {
                                awaitPointerEvent()
                            } else {
                                withTimeoutOrNull(remaining) { awaitPointerEvent() }
                            }
                            if (event == null) {
                                holdActivated = true
                                isHolding = true
                                down.consume()
                                val inward = if (latestHomeOnRight) lastDx < -slideThresholdPx else lastDx > slideThresholdPx
                                selectedSlot = if (inward) 1 else 0
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    expandAnim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = if (liquidStudio) {
                                            spring(dampingRatio = 0.68f, stiffness = 480f)
                                        } else {
                                            spring(dampingRatio = 0.78f, stiffness = 920f)
                                        },
                                    ) {
                                        latestSetExtraWidth(HomePillExtraMax * value.coerceIn(0f, 1.05f))
                                    }
                                }
                                continue
                            }
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                if (!holdActivated) {
                                    latestTap()
                                } else {
                                    val chosen = selectedSlot
                                    if (chosen == 1) {
                                        latestOnShortcut(latestShortcut)
                                    } else {
                                        latestHoldPrimary()
                                    }
                                }
                                break
                            }
                            val dx = change.position.x - initialX
                            val dy = change.position.y - initialY
                            if (!holdActivated) {
                                lastDx = dx
                                if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                                    break
                                }
                            } else {
                                change.consume()
                                val inward = if (latestHomeOnRight) dx < -slideThresholdPx else dx > slideThresholdPx
                                val nextSlot = if (inward) 1 else 0
                                if (nextSlot != selectedSlot) {
                                    selectedSlot = nextSlot
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                }
                            }
                        }
                    } finally {
                        if (isHolding || expandAnim.value > 0f) {
                            isHolding = false
                            scope.launch {
                                expandAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = if (liquidStudio) {
                                        spring(dampingRatio = 0.72f, stiffness = 520f)
                                    } else {
                                        spring(dampingRatio = 0.85f, stiffness = 1000f)
                                    },
                                ) {
                                    latestSetExtraWidth(HomePillExtraMax * value.coerceIn(0f, 1.05f))
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val exp = expandAnim.value.coerceIn(0f, 1f)
        if (exp < 0.20f) {
            Icon(
                imageVector = primaryIcon,
                contentDescription = primaryLabel,
                tint = contentColor,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { alpha = (1f - exp / 0.20f).coerceIn(0f, 1f) },
            )
        }
        if (exp > 0.02f) {
            val contentAlpha = ((exp - 0.02f) / 0.30f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
                    .graphicsLayer { alpha = contentAlpha },
            ) {
                // Active highlight capsule: slides from outer edge (Home) inward to 2nd Shortcut.
                val startOffsetPx = with(density) {
                    val stepPx = 66.dp.toPx()
                    if (homeOnRight) (1f - selectionAnim) * stepPx else selectionAnim * stepPx
                }
                Box(
                    modifier = Modifier
                        .offset { IntOffset(startOffsetPx.roundToInt(), 0) }
                        .width(64.dp)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            if (comic) ComicAccent.copy(alpha = 0.85f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        )
                        .then(
                            if (comic) Modifier.border(1.5.dp, ComicInk, CircleShape) else Modifier,
                        ),
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (homeOnRight) {
                        HoldSlotLabel(
                            icon = shortcut.icon(),
                            label = shortcut.shortLabel,
                            tint = slot1Tint,
                            modifier = Modifier.width(64.dp),
                        )
                        HoldSlotLabel(
                            icon = Icons.Filled.Home,
                            label = "Home",
                            tint = slot0Tint,
                            modifier = Modifier.width(64.dp),
                        )
                    } else {
                        HoldSlotLabel(
                            icon = Icons.Filled.Home,
                            label = "Home",
                            tint = slot0Tint,
                            modifier = Modifier.width(64.dp),
                        )
                        HoldSlotLabel(
                            icon = shortcut.icon(),
                            label = shortcut.shortLabel,
                            tint = slot1Tint,
                            modifier = Modifier.width(64.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldSlotLabel(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
