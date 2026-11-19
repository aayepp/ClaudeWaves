package com.lastwave.app.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.Indication
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.comicPanel
import kotlinx.coroutines.launch

/**
 * Snapshot of one long-pressed card or row: window bounds and its rendered bitmap.
 * Captured on demand one frame after the hold so menus can lift the exact item
 * the user pressed rather than a generic placeholder.
 */
@Stable
class HeldItem internal constructor(
    val bounds: Rect,
) {
    var image: ImageBitmap? by mutableStateOf(null)
        internal set
}

/**
 * Single-slot bridge from a long-pressed item to the menu it opens.
 * Tapping a ⋮ button never records here, so it keeps opening the standard sheet.
 */
object LongPressOrigin {
    private var held: HeldItem? = null
    private var recordedAt = 0L

    internal fun record(item: HeldItem) {
        held = item
        recordedAt = System.nanoTime()
    }

    /** Returns the freshly held item (within 500 ms), or null for normal button taps. */
    fun consume(): HeldItem? {
        val item = held?.takeIf { System.nanoTime() - recordedAt < FRESH_NANOS }
        held = null
        return item
    }

    private const val FRESH_NANOS = 500_000_000L
}

/**
 * [combinedClickable] that records the pressed item's bounds and bitmap into [LongPressOrigin]
 * only when a long-press actually lands (zero overhead during normal scrolling/taps).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.longPressMenuClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    role: Role? = null,
): Modifier {
    val capture = remember { HoldCapture() }
    val graphicsContext = LocalGraphicsContext.current
    val scope = rememberCoroutineScope()
    val longClick = onLongClick?.let { held ->
        {
            capture.coordinates?.takeIf { it.isAttached }?.let { coordinates ->
                val item = HeldItem(coordinates.boundsInWindow())
                LongPressOrigin.record(item)
                capture.pending = item
                capture.requests++
            }
            held()
        }
    }
    return this
        .onPlaced { capture.coordinates = it }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = indication,
            enabled = enabled,
            role = role,
            onClick = onClick,
            onLongClick = longClick,
        )
        .drawWithContent {
            drawContent()
            val request = capture.requests
            if (request == capture.handled) return@drawWithContent
            capture.handled = request
            val item = capture.pending ?: return@drawWithContent
            capture.pending = null
            val layer = graphicsContext.createGraphicsLayer()
            layer.record { this@drawWithContent.drawContent() }
            scope.launch {
                try {
                    item.image = layer.toImageBitmap()
                } finally {
                    graphicsContext.releaseGraphicsLayer(layer)
                }
            }
        }
}

private class HoldCapture {
    var coordinates: LayoutCoordinates? = null
    var pending: HeldItem? = null
    var requests by mutableIntStateOf(0)
    var handled = 0
}

/**
 * Renders the lifted snapshot of a long-pressed item above a context menu with a
 * spring scale-in and theme-aware rim (specular glass rim or Comic Pop ink border).
 */
@Composable
fun LiftedHeldItemPreview(
    held: HeldItem,
    modifier: Modifier = Modifier,
) {
    val bitmap = held.image ?: return
    val isDark = LocalIsDarkTheme.current
    val isComic = LocalComicTheme.current
    val density = LocalDensity.current
    val lift = remember(held) { Animatable(0f) }
    LaunchedEffect(held) {
        lift.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
    }
    val shape = RoundedCornerShape(18.dp)
    val maxWidthDp = with(density) { held.bounds.width.toDp().coerceIn(140.dp, 360.dp) }
    val maxHeightDp = with(density) { held.bounds.height.toDp().coerceIn(64.dp, 220.dp) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .widthIn(max = maxWidthDp)
                .heightIn(max = maxHeightDp)
                .graphicsLayer {
                    val p = lift.value
                    val s = 0.92f + 0.10f * p
                    scaleX = s
                    scaleY = s
                    translationY = (1f - p) * 18f
                    alpha = p.coerceIn(0f, 1f)
                }
                .then(
                    if (isComic) {
                        Modifier.comicPanel(shape, offset = 4.dp)
                    } else {
                        Modifier
                            .shadow(16.dp, shape, ambientColor = Color.Black.copy(alpha = 0.28f), spotColor = Color.Black.copy(alpha = 0.36f))
                            .clip(shape)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    0f to Color.White.copy(alpha = if (isDark) 0.45f else 0.85f),
                                    0.5f to Color.White.copy(alpha = 0.08f),
                                    1f to Color.White.copy(alpha = if (isDark) 0.18f else 0.38f),
                                    start = Offset.Zero,
                                    end = Offset.Infinite,
                                ),
                                shape = shape,
                            )
                    },
                ),
        )
    }
}
