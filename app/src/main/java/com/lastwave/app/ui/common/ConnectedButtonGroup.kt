package com.lastwave.app.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lastwave.app.ui.comic.Surface
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.M3ExpressiveShape
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

data class ConnectedButtonItem(
    val label: String,
    val icon: ImageVector? = null,
)

/**
 * Connected segmented selector.
 * Comic Pop keeps its bold flat ink-bordered button group; normal Light / Dark / AMOLED themes
 * render an iOS 26-inspired Liquid Glass Segmented Control with a draggable, inflating glass lens.
 */
@Composable
fun ConnectedButtonGroup(
    items: List<ConnectedButtonItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    if (LocalComicTheme.current) {
        ComicConnectedButtonGroup(items, selectedIndex, onSelect, modifier)
    } else {
        LiquidGlassSegmentedControl(items, selectedIndex, onSelect, modifier)
    }
}

@Composable
private fun LiquidGlassSegmentedControl(
    items: List<ConnectedButtonItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = LocalIsDarkTheme.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val safeSelected = selectedIndex.coerceIn(0, items.lastIndex)
    val latestOnSelect by rememberUpdatedState(onSelect)
    val latestCount by rememberUpdatedState(items.size)

    var containerWidthPx by remember { mutableIntStateOf(1) }
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(safeSelected.toFloat()) }
    var rubberOvershoot by remember { mutableFloatStateOf(0f) }

    val padDp = 3.dp
    val padPx = with(density) { padDp.toPx() }
    val innerWidthPx = (containerWidthPx - padPx * 2f).coerceAtLeast(1f)
    val segWidthPx = innerWidthPx / items.size.coerceAtLeast(1)
    val segWidthDp = with(density) { segWidthPx.toDp() }

    val targetPos = if (isScrubbing) scrubPosition else safeSelected.toFloat()
    val animatedPos by animateFloatAsState(
        targetValue = targetPos,
        animationSpec = if (isScrubbing) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 2200f)
        } else {
            spring(dampingRatio = 0.72f, stiffness = 520f)
        },
        label = "liquidSegPos",
    )
    val liftProgress by animateFloatAsState(
        targetValue = if (isScrubbing) 1f else 0f,
        animationSpec = if (isScrubbing) {
            spring(dampingRatio = 0.74f, stiffness = 760f)
        } else {
            spring(dampingRatio = 0.58f, stiffness = 520f)
        },
        label = "liquidSegLift",
    )
    val rubberAnimated by animateFloatAsState(
        targetValue = if (isScrubbing) rubberOvershoot else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 520f),
        label = "liquidSegRubber",
    )

    val troughColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.09f else 0.07f)
    val troughRim = Color.White.copy(alpha = if (isDark) 0.16f else 0.62f)
    val lensBaseColor = if (isDark) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f - liftProgress.coerceIn(0f, 1f) * 0.36f)
    } else {
        Color.White.copy(alpha = 0.92f - liftProgress.coerceIn(0f, 1f) * 0.42f)
    }
    val activeIdxVisual = animatedPos.roundToInt().coerceIn(0, items.lastIndex)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .onSizeChanged { if (it.width > 0) containerWidthPx = it.width }
            .pointerInput(items.size, innerWidthPx, padPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val count = latestCount.coerceAtLeast(1)
                    val segW = innerWidthPx / count
                    fun rawIndex(x: Float): Float = ((x - padPx) / segW) - 0.5f

                    var lastIdx = rawIndex(down.position.x).roundToInt().coerceIn(0, count - 1)
                    isScrubbing = true
                    scrubPosition = lastIdx.toFloat()
                    rubberOvershoot = 0f
                    var moved = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (abs(change.positionChange().x) > 0.5f) moved = true
                        if (moved) {
                            change.consume()
                            val raw = rawIndex(change.position.x)
                            scrubPosition = raw.coerceIn(0f, (count - 1).toFloat())
                            val over = when {
                                raw < 0f -> raw
                                raw > count - 1 -> raw - (count - 1)
                                else -> 0f
                            }
                            rubberOvershoot = sign(over) * (abs(over) / (1f + abs(over) * 2.4f)) * 0.18f
                            val idx = scrubPosition.roundToInt().coerceIn(0, count - 1)
                            if (idx != lastIdx) {
                                lastIdx = idx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                latestOnSelect(idx)
                            }
                        }
                    }

                    val finalIdx = scrubPosition.roundToInt().coerceIn(0, count - 1)
                    isScrubbing = false
                    rubberOvershoot = 0f
                    if (!moved) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    latestOnSelect(finalIdx)
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        // Glass pill trough
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(troughColor)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(troughRim, Color.Black.copy(alpha = if (isDark) 0.20f else 0.06f)),
                    ),
                    shape = CircleShape,
                ),
        )

        // Floating, draggable, inflating Liquid Glass selection lens
        val safeLift = liftProgress.coerceAtLeast(-0.12f)
        val lensOffsetPx = padPx + animatedPos * segWidthPx + rubberAnimated * 26f
        Box(
            modifier = Modifier
                .offset { IntOffset(lensOffsetPx.roundToInt(), 0) }
                .width(segWidthDp)
                .height(40.dp)
                .graphicsLayer {
                    scaleX = 1f + safeLift * 0.05f + abs(rubberAnimated) * 0.16f
                    scaleY = 1f + safeLift * 0.11f - abs(rubberAnimated) * 0.06f
                }
                .shadow(
                    elevation = (3f + safeLift.coerceAtLeast(0f) * 5f).dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = if (isDark) 0.34f else 0.14f),
                    spotColor = Color.Black.copy(alpha = if (isDark) 0.36f else 0.16f),
                )
                .clip(CircleShape)
                .background(lensBaseColor)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = if (isDark) 0.24f else 0.55f),
                        0.55f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = if (isDark) 0.12f else 0.04f),
                    ),
                )
                .border(
                    width = (1f + safeLift.coerceAtLeast(0f) * 0.4f).dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = if (isDark) 0.42f + safeLift.coerceAtLeast(0f) * 0.35f else 0.92f),
                            Color.White.copy(alpha = if (isDark) 0.10f else 0.30f),
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        // Segment labels & icons
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = padDp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == activeIdxVisual
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = ExpressiveMotion.effectsFast(),
                    label = "liquidSegFg_$index",
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (item.icon != null) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                            tint = contentColor,
                        )
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ComicConnectedButtonGroup(
    items: List<ConnectedButtonItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy((-1).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            val shape = when {
                items.size == 1 -> M3ExpressiveShape.GroupSingle
                index == 0 -> M3ExpressiveShape.GroupLeft
                index == items.lastIndex -> M3ExpressiveShape.GroupRight
                else -> M3ExpressiveShape.GroupMiddle
            }

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.96f else 1f,
                animationSpec = ExpressiveMotion.spatialFast(),
                label = "connectedBtnScale_$index",
            )

            val containerColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                },
                animationSpec = ExpressiveMotion.effectsDefault(),
                label = "connectedBtnBg_$index",
            )

            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                animationSpec = ExpressiveMotion.effectsDefault(),
                label = "connectedBtnFg_$index",
            )

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .zIndex(if (isSelected) 1f else 0f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(index)
                        },
                    ),
                shape = shape,
                color = containerColor,
                contentColor = contentColor,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                ),
                tonalElevation = if (isSelected) 2.dp else 0.dp,
            ) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 44.dp)
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        if (item.icon != null) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = contentColor,
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = contentColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
