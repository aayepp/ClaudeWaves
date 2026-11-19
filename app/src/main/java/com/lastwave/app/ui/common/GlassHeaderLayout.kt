package com.lastwave.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import com.lastwave.app.ui.theme.headerLiquidGlass
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lastwave.app.ui.theme.LayerBackdrop
import com.lastwave.app.ui.theme.LocalLiquidGlass
import com.lastwave.app.ui.theme.isLiquidGlassBackdropSupported
import com.lastwave.app.ui.theme.liquidGlassSource
import com.lastwave.app.ui.theme.rememberLayerBackdrop
import com.lastwave.app.ui.theme.rememberPageWash

/**
 * Puts [header] (an [ExpressiveHeader]) over [content] so content scrolls
 * under the glass. Stacked in a Column, the header only refracted its own
 * flat background and read as a solid bar.
 *
 * [header] gets the modifier and backdrop to pass to ExpressiveHeader.
 * [content] gets the header height to add to its scrollable's top content
 * padding. Without glass (Comic Pop, glass off) the header stays solid and
 * content still scrolls under it, meeting a soft fade instead of a hard cut.
 * Content is the backdrop source and the header a sibling, never inside it.
 */
@Composable
fun GlassHeaderLayout(
    modifier: Modifier = Modifier,
    header: @Composable (modifier: Modifier, backdrop: LayerBackdrop?) -> Unit,
    content: @Composable (topInset: Dp) -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    val pageWash = rememberPageWash()
    var headerHeightPx by remember { mutableIntStateOf(0) }
    // Glass off, Comic Pop or Android < 12: no backdrop, so the header stays solid.
    // The page wash gives the glass colour to blur. An extra ink band used to, but this
    // layer is drawn on screen too, and its gradient kinked at the header's edge: a line.
    val backdrop = if (LocalLiquidGlass.current && isLiquidGlassBackdropSupported()) {
        rememberLayerBackdrop {
            pageWash()
            drawContent()
        }
    } else null
    val glass = backdrop != null
    val headerHeight = with(LocalDensity.current) { headerHeightPx.toDp() }
    Box(modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .liquidGlassSource(if (glass) backdrop else null),
        ) {
            content(headerHeight)
        }
        header(
            Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { if (it.height != headerHeightPx) headerHeightPx = it.height }
                .then(if (glass) Modifier else Modifier.fadeBelow(background)),
            if (glass) backdrop else null,
        )
    }
}

/**
 * A short fade from [color] to transparent just below a solid bar, drawn outside its
 * bounds, so content scrolling under the bar softens instead of being cut on a line.
 */
internal fun Modifier.fadeBelow(color: Color, height: Dp = 16.dp): Modifier = drawBehind {
    val h = height.toPx()
    drawRect(
        Brush.verticalGradient(listOf(color, Color.Transparent), startY = size.height, endY = size.height + h),
        topLeft = Offset(0f, size.height),
        size = Size(size.width, h),
    )
}

/** A capture of a screen's own scrolling content for a [ScrolledTopBar], or null without glass. */
@Composable
fun rememberScreenBackdrop(): LayerBackdrop? {
    val pageWash = rememberPageWash()
    return if (LocalLiquidGlass.current && isLiquidGlassBackdropSupported()) {
        rememberLayerBackdrop {
            pageWash()
            drawContent()
        }
    } else null
}

/**
 * Floating top bar over a screen's own scrolling content (playlist, album, artist
 * detail). Its background fades in once content scrolls under it: liquid glass
 * blurring [backdrop] (put [liquidGlassSource] on the list, a sibling of this bar),
 * else the page colour with a soft fade below it. No shadow or hard edge: both
 * drew a line under the bar.
 */
@Composable
fun ScrolledTopBar(
    backdrop: LayerBackdrop?,
    shown: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(260), label = "scrolledTopBar")
    val page = MaterialTheme.colorScheme.background
    Box(modifier) {
        Box(
            Modifier
                .matchParentSize()
                .then(
                    if (backdrop != null) {
                        Modifier.headerLiquidGlass(backdrop, RectangleShape, alpha = { alpha })
                    } else {
                        Modifier.graphicsLayer { this.alpha = alpha }.background(page).fadeBelow(page)
                    },
                ),
        )
        content()
    }
}
