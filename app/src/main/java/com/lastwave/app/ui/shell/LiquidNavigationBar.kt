package com.lastwave.app.ui.shell

import com.lastwave.app.ui.comic.clipOnly
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.rememberCoroutineScope
import com.lastwave.app.ui.player.kick
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import com.lastwave.app.ui.theme.glassRim
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lastwave.app.ui.comic.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.lastwave.app.R
import com.lastwave.app.ui.theme.BackdropBoundsHolder
import com.lastwave.app.ui.theme.GlassRecipe
import com.lastwave.app.ui.theme.LiquidGlassIntensity
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.LocalLiquidGlassBackdrop
import com.lastwave.app.ui.theme.rememberBackdropUseLightInk
import com.lastwave.app.ui.theme.rememberBackdropUseLightInkPerSlot
import com.lastwave.app.ui.theme.trackBackdropBounds
import com.lastwave.app.ui.theme.waterDropJiggle
import io.github.fletchmckee.liquid.LiquidState
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs
import kotlin.math.roundToInt

/** [glyph], when set, replaces [icon] with an icon that animates from the inside. */
internal data class LiquidNavigationItem(val label: String, val icon: ImageVector, val glyph: TabGlyph? = null)
internal enum class LiquidNavigationStyle { Tabs, Material3 }
private val LensShape = CircleShape

@Composable
internal fun LiquidNavigationBar(
    liquidState: LiquidState,
    items: List<LiquidNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onScrubSelect: (Int) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    style: LiquidNavigationStyle = LiquidNavigationStyle.Tabs,
    isVisible: () -> Boolean = { true },
) {
    key(style) {
        LiquidNavigationBarContent(liquidState, items, selectedIndex, onSelect, onScrubSelect, onSearch, modifier, style, isVisible)
    }
}

@Composable
private fun LiquidNavigationBarContent(
    pageState: LiquidState,
    items: List<LiquidNavigationItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onScrubSelect: (Int) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier,
    style: LiquidNavigationStyle,
    isVisible: () -> Boolean,
) {
    if (items.isEmpty()) return
    val compact = style == LiquidNavigationStyle.Material3
    val isDark = LocalIsDarkTheme.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    val count = items.size
    val selected by rememberUpdatedState(selectedIndex)
    val select by rememberUpdatedState(onSelect)
    val scrub by rememberUpdatedState(onScrubSelect)
    val start = liquidNavigationVisualIndex(selectedIndex, count, isRtl)
    val settledPosition = remember(count, isRtl) { Animatable(start) }
    val trailingPosition = remember(count, isRtl) { Animatable(start) }
    var pressed by remember { mutableStateOf(false) }
    var touchPosition by remember { mutableFloatStateOf(Float.NaN) }
    var touchY by remember { mutableFloatStateOf(0f) }
    var captionAtPress by remember { mutableIntStateOf(selectedIndex) }
    var releaseGeneration by remember { mutableIntStateOf(0) }
    // The drop stays lifted for the whole flight to a tapped tab, not only under the finger,
    // and sets down as the accent pill when it lands (after Auris' selection pill).
    val lift by animateFloatAsState(
        if (pressed || settledPosition.isRunning) 1f else 0f,
        spring(dampingRatio = 0.90f, stiffness = 520f),
        label = "liquidDropLift",
    )
    val currentPosition = {
        if (touchPosition.isFinite()) touchPosition else settledPosition.value
    }
    LaunchedEffect(selectedIndex, pressed, releaseGeneration) {
        if (!pressed) {
            if (touchPosition.isFinite()) {
                settledPosition.snapTo(touchPosition)
                touchPosition = Float.NaN
            }
            settledPosition.animateTo(
                liquidNavigationVisualIndex(selectedIndex, count, isRtl),
                spring(dampingRatio = 0.78f, stiffness = 300f),
            )
        }
    }
    // Only the shape trails the finger. Position has no tab springs during touch.
    LaunchedEffect(count, isRtl) {
        snapshotFlow { currentPosition() }.collectLatest {
            trailingPosition.animateTo(it, spring(dampingRatio = 0.92f, stiffness = 540f))
        }
    }

    val dockBounds = remember { BackdropBoundsHolder() }
    val useLightInk = rememberBackdropUseLightInkPerSlot(
        backdrop = LocalLiquidGlassBackdrop.current,
        bounds = dockBounds,
        enabled = true,
        slots = count,
        scrimBaseAlpha = if (isDark) 0.32f else 0.30f,
        isVisible = isVisible,
    )
    val darkInk = if (isDark) Color(0xFF111319) else MaterialTheme.colorScheme.onSurface
    val inks = useLightInk.mapIndexed { i, light ->
        key(i) {
            animateColorAsState(
                targetValue = if (light) Color.White else darkInk,
                animationSpec = tween(com.lastwave.app.ui.theme.ADAPTIVE_INK_FADE_MS),
                label = "liquidNavInk",
            ).value
        }
    }
    // Slots are visual positions; in RTL the first item sits in the last slot.
    val inkAt = { index: Int -> inks[if (isRtl) count - 1 - index else index] }
    val accent = MaterialTheme.colorScheme.primary
    val pageColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    val railTint = railTint(isDark)
    val railState = rememberLiquidState()
    val railHeight = if (compact) 60.dp else 64.dp
    val pressedScale = railHeight.value / (railHeight.value - 8f)
    val captionIndex = if (pressed) captionAtPress else selectedIndex
    val railMaxWidth = if (compact) {
        val caption = rememberTextMeasurer().measure(
            text = items[captionIndex.coerceIn(items.indices)].label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            softWrap = false,
            maxLines = 1,
        )
        val captionSlotWidth = with(density) {
            // Match glyph padding and reserve a pixel for equal-slot rounding.
            (caption.size.width + 24.dp.roundToPx() + 6.dp.roundToPx() + 2 * 4.dp.roundToPx() + 1).toDp()
        }
        maxOf(280.dp, captionSlotWidth * count + 8.dp)
    } else 360.dp

    Box(
        modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(horizontal = 16.dp, vertical = if (compact) 12.dp else 8.dp),
        contentAlignment = Alignment.Center,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BoxWithConstraints(
            Modifier.weight(1f, fill = false).widthIn(max = railMaxWidth)
                .fillMaxWidth().height(railHeight)
                .trackBackdropBounds(dockBounds)
                .testTag("liquid_navigation_bar")
                .waterDropJiggle(amplitude = 0.022f)
                .railShadow(),
        ) {
            val tabWidth = (maxWidth - 8.dp) / count
            val lensWidth = tabWidth
            val tabPx = with(density) { tabWidth.toPx() }
            val lensPx = with(density) { lensWidth.toPx() }

            // A -> B -> C is acyclic: page A, clear navigation plane B,
            // sibling lens C. The visible opaque rail covers B's original draw.
            // B has heavier frost than the outer rail, so the selected lens C reads
            // as frosted glass; B's glyphs sit above that blur and stay sharp.
            Box(
                Modifier.matchParentSize().clearAndSetSemantics { }
                    .clip(CircleShape)
                    .captureHeadroom(20.dp)
                    .liquefiable(railState)
                    .background(pageColor)
                    .liquid(pageState) {
                        shape = RoundedCornerShape(0.dp)
                        frost = 0.dp
                        refraction = 0f
                        curve = 0f
                        edge = 0f
                    }
                    .padding(20.dp),
            ) {
                Box(Modifier.matchParentSize().background(surfaceColor, CircleShape)
                    .navigationRail(pageState, railTint, blurRadius = 24.dp))
                NavigationGlyphs(
                    items, selectedIndex, captionIndex, compact, inkAt,
                    captured = true, onSelect = {}, modifier = Modifier.fillMaxSize().padding(4.dp),
                )
            }
            // A has an opaque theme background. This covers the clearer source copy
            // completely, including the antialiased edges of text and icons.
            Box(Modifier.matchParentSize().background(surfaceColor, CircleShape).navigationRail(pageState, railTint).glassRim())
            Box(
                Modifier.fillMaxSize().padding(4.dp).selectableGroup()
                    .pointerInput(count, isRtl, lensPx, tabPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            val width = size.width
                            val height = size.height
                            val fingerStart = liquidNavigationRawPosition(down.position.x, width.toFloat(), count)
                            val visualStart = currentPosition()
                            val onExistingLens = abs(fingerStart - visualStart) <= lensPx / (2f * tabPx)
                            val grabOffset = if (onExistingLens) visualStart - fingerStart else 0f
                            captionAtPress = selected
                            touchPosition = liquidNavigationRubberBand(fingerStart + grabOffset, count)
                            touchY = 0f
                            pressed = true
                            var dragged = false
                            try {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    val dx = change.position.x - down.position.x
                                    val dy = change.position.y - down.position.y
                                    if (event.changes.count { it.pressed } > 1 ||
                                        size.width != width || size.height != height || change.isConsumed ||
                                        change.position.y < -24.dp.toPx() || change.position.y > height + 24.dp.toPx()
                                    ) {
                                        event.changes.forEach { it.consume() }
                                        break
                                    }
                                    if (!dragged && abs(dy) > viewConfiguration.touchSlop && abs(dy) >= abs(dx)) {
                                        change.consume()
                                        break
                                    }
                                    touchPosition = liquidNavigationRubberBand(
                                        liquidNavigationRawPosition(change.position.x, width.toFloat(), count) + grabOffset,
                                        count,
                                    )
                                    touchY = (dy / height.coerceAtLeast(1)).coerceIn(-0.2f, 0.2f)
                                    if (!change.pressed) {
                                        if (dragged) {
                                            change.consume()
                                            val destination = liquidNavigationIndex(touchPosition, count, isRtl)
                                            if (destination != selected) scrub(destination)
                                        }
                                        break
                                    }
                                    if (abs(dx) > viewConfiguration.touchSlop && abs(dx) > abs(dy)) dragged = true
                                    // Slop governs event ownership, never visual movement.
                                    if (dragged) change.consume()
                                }
                            } finally {
                                pressed = false
                                touchY = 0f
                                releaseGeneration++
                            }
                        }
                    },
            ) {
                NavigationGlyphs(
                    items, selectedIndex, captionIndex, compact,
                    { i -> inkAt(i).let { if (i == selectedIndex && !pressed) it else it.copy(alpha = 0.92f) } },
                    captured = false, onSelect = { select(it) }, modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier.align(AbsoluteAlignment.TopLeft).absoluteOffset {
                        IntOffset((currentPosition() * tabPx + (tabPx - lensPx) / 2f).roundToInt(), 0)
                    }.width(lensWidth).fillMaxHeight().graphicsLayer {
                        val amount = lift.coerceIn(0f, 1f)
                        val flow = ((currentPosition() - trailingPosition.value) * 2f).coerceIn(-1f, 1f)
                        // Keep the frosted selection visible at rest; never
                        // blend doubled glyphs.
                        // Uniform scaling preserves semicircular ends. At rest
                        // the lens fits inside the rail; on press their radii match.
                        scaleX = 1f + amount * (pressedScale - 1f + 0.012f * abs(flow))
                        scaleY = scaleX
                        translationY = (-1.dp.toPx() + touchY * 4.dp.toPx()) * amount
                        shape = LensShape
                        clip = false
                        shadowElevation = (1.5.dp.toPx() + 3.5.dp.toPx() * amount)
                        ambientShadowColor = Color.Black.copy(alpha = 0.08f)
                        spotShadowColor = Color.Black.copy(alpha = 0.16f)
                    }.testTag("liquid_navigation_lens")
                        .liquidBubbleHighlight(isDark) { 0.25f + 0.75f * lift.coerceIn(0f, 1f) }
                        .liquid(railState) {
                            val amount = lift.coerceIn(0f, 1f)
                            shape = LensShape
                            frost = 0.dp
                            refraction = 0.08f + 0.37f * amount
                            curve = 0.04f + 0.16f * amount
                            edge = 0.012f + 0.023f * amount
                            // Chromatic separation belongs to manipulation, not rest.
                            dispersion = 0.14f * amount
                            saturation = LiquidGlassIntensity.saturation(
                                GlassRecipe.SATURATION_FROSTED,
                                GlassRecipe.SATURATION_CLEAR,
                            )
                            // Landed: an accent pill. Lifted: clear glass.
                            tint = androidx.compose.ui.graphics.lerp(
                                accent.copy(alpha = if (isDark) 0.34f else 0.26f),
                                Color.White.copy(alpha = if (isDark) 0.012f else 0.018f),
                                amount,
                            )
                        },
                )
            }
        }
        // Standalone search circle beside the tab rail, cut from the same glass.
        // The lens tilts as if looking, then springs back.
        val searchBounds = remember { BackdropBoundsHolder() }
        val searchUseLightInk = rememberBackdropUseLightInk(
            backdrop = LocalLiquidGlassBackdrop.current,
            bounds = searchBounds,
            enabled = true,
            scrimBaseAlpha = if (isDark) 0.32f else 0.30f,
            cropStartX = 0.20f,
            cropEndX = 0.80f,
            cropStartY = 0.20f,
            cropEndY = 0.80f,
            isVisible = isVisible,
        )
        val searchInk by animateColorAsState(
            targetValue = when {
                searchUseLightInk -> Color.White
                isDark -> Color(0xFF111319)
                else -> MaterialTheme.colorScheme.onSurface
            },
            animationSpec = tween(com.lastwave.app.ui.theme.ADAPTIVE_INK_FADE_MS),
            label = "liquidSearchInk",
        )
        val searchTilt = remember { Animatable(0f) }
        val searchScope = rememberCoroutineScope()
        Box(
            Modifier.size(railHeight)
                .trackBackdropBounds(searchBounds)
                .testTag("liquid_navigation_search")
                .waterDropJiggle(amplitude = 0.038f)
                .shadow(8.dp, CircleShape, clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.10f),
                    spotColor = Color.Black.copy(alpha = 0.14f))
                .background(surfaceColor, CircleShape)
                .navigationRail(pageState, railTint)
                .glassRim()
                .clipOnly(CircleShape)
                .clickable(role = Role.Button, onClick = {
                    searchScope.launch { searchTilt.kick() }
                    onSearch()
                }),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = stringResource(R.string.search_cd),
                tint = searchInk,
                modifier = Modifier.size(24.dp).graphicsLayer {
                    rotationZ = -22f * searchTilt.value
                    val s = 1f + 0.12f * searchTilt.value
                    scaleX = s
                    scaleY = s
                },
            )
        }
      }
    }
}

/** The collapsed bar: the same glass rail as a circle holding only the active tab's icon. */
@Composable
internal fun LiquidNavigationPill(
    liquidState: LiquidState,
    item: LiquidNavigationItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isVisible: () -> Boolean = { true },
) {
    val isDark = LocalIsDarkTheme.current
    val pillBounds = remember { BackdropBoundsHolder() }
    val useLightInk = rememberBackdropUseLightInk(
        backdrop = LocalLiquidGlassBackdrop.current,
        bounds = pillBounds,
        enabled = true,
        scrimBaseAlpha = if (isDark) 0.32f else 0.30f,
        cropStartX = 0.20f,
        cropEndX = 0.80f,
        cropStartY = 0.20f,
        cropEndY = 0.80f,
        isVisible = isVisible,
    )
    val pillInk by animateColorAsState(
        targetValue = when {
            useLightInk -> Color.White
            isDark -> Color(0xFF111319)
            else -> MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(com.lastwave.app.ui.theme.ADAPTIVE_INK_FADE_MS),
        label = "liquidNavPillInk",
    )
    Box(
        modifier.trackBackdropBounds(pillBounds)
            .railShadow()
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .navigationRail(liquidState, railTint(isDark))
            .glassRim()
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            item.icon,
            contentDescription = item.label,
            tint = pillInk,
            modifier = Modifier.size(24.dp),
        )
    }
}

private fun railTint(isDark: Boolean): Color =
    if (isDark) Color(0xFF111319).copy(alpha = 0.32f) else Color.White.copy(alpha = 0.30f)

private fun Modifier.railShadow(): Modifier = shadow(
    8.dp, CircleShape, clip = false,
    ambientColor = Color.Black.copy(alpha = 0.10f),
    spotColor = Color.Black.copy(alpha = 0.14f),
)

/** Sample headroom without changing hit areas or painting over adjacent overlays. */
private fun Modifier.captureHeadroom(padding: Dp): Modifier = layout { measurable, constraints ->
    val inset = padding.roundToPx()
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    val placeable = measurable.measure(Constraints.fixed(width + inset * 2, height + inset * 2))
    layout(width, height) { placeable.place(-inset, -inset) }
}

// Same base blur and saturation as the mini player, both following the intensity
// slider, so the docked row reads as one frost. A fixed [blurRadius] opts out.
private fun Modifier.navigationRail(state: LiquidState, color: Color, blurRadius: Dp? = null): Modifier = liquid(state) {
    shape = CircleShape
    frost = blurRadius ?: (GlassRecipe.BLUR_BASE_DP.dp * LiquidGlassIntensity.blur())
    refraction = 0.04f + 0.08f * LiquidGlassIntensity.value
    curve = 0.12f
    edge = 0.012f
    saturation = LiquidGlassIntensity.saturation(
        GlassRecipe.SATURATION_FROSTED,
        GlassRecipe.SATURATION_CLEAR,
    )
    dispersion = 0.01f + 0.03f * LiquidGlassIntensity.value
    tint = color.copy(alpha = LiquidGlassIntensity.tint(color.alpha))
}
