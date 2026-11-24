package com.lastwave.app.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.lastwave.app.ui.theme.ADAPTIVE_INK_FADE_MS
import com.lastwave.app.ui.theme.BackdropBoundsHolder
import com.lastwave.app.ui.theme.ComicAccent
import com.lastwave.app.ui.theme.LiquidGlassPreset
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.LocalLiquidGlass
import com.lastwave.app.ui.theme.comicPanel
import com.lastwave.app.ui.theme.glassRim
import com.lastwave.app.ui.theme.isLiquidGlassBackdropSupported
import com.lastwave.app.ui.theme.liquidGlassChrome
import com.lastwave.app.ui.theme.liquidGlassContainerColor
import com.lastwave.app.ui.theme.rememberBackdropUseLightInk
import com.lastwave.app.ui.theme.trackBackdropBounds
import kotlinx.coroutines.delay

enum class PlayerToastKind {
    DOWNLOADED,
    LIKED_ADDED,
    LIKED_REMOVED,
    PLAYLIST_ADDED,
    ERROR,
    INFO,
}

data class PlayerToastEvent(
    val kind: PlayerToastKind,
    val title: String,
    val subtitle: String? = null,
    val id: Long = System.nanoTime(),
)

/**
 * Floating Option-2 Liquid Glass pill notification anchored cleanly above the MiniPlayer /
 * bottom controls, replacing plain system Android toasts across Light, Dark, AMOLED,
 * non-glass, and Comic Pop themes.
 */
@Composable
internal fun PlayerToastPillHost(
    toast: PlayerToastEvent?,
    onDismiss: () -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    onTap: ((PlayerToastEvent) -> Unit)? = null,
    backdrop: Backdrop? = null,
    forceDarkSurface: Boolean = false,
) {
    var renderedToast by remember { mutableStateOf(toast) }
    if (toast != null) renderedToast = toast

    LaunchedEffect(toast?.id) {
        val current = toast ?: return@LaunchedEffect
        delay(if (current.kind == PlayerToastKind.DOWNLOADED) 3_200L else 2_400L)
        onDismiss()
    }

    val animatedBottom by animateDpAsState(
        targetValue = bottomPadding,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 360f),
        label = "toastPillBottom",
    )

    Box(
        modifier = modifier
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
            )
            .padding(horizontal = 16.dp)
            .padding(bottom = animatedBottom),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = toast != null,
            enter = fadeIn(tween(180)) +
                slideInVertically(spring(dampingRatio = 0.82f, stiffness = 420f)) { it / 2 } +
                scaleIn(initialScale = 0.92f, animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)),
            exit = fadeOut(tween(160)) +
                slideOutVertically(tween(180)) { it / 2 } +
                scaleOut(targetScale = 0.94f, animationSpec = tween(160)),
            label = "toastPillVisibility",
        ) {
            val current = renderedToast ?: return@AnimatedVisibility
            AnimatedContent(
                targetState = current,
                contentKey = { it.id },
                transitionSpec = {
                    (fadeIn(tween(150)) + scaleIn(initialScale = 0.96f, animationSpec = tween(150))) togetherWith
                        fadeOut(tween(110))
                },
                label = "toastPillSwitch",
            ) { event ->
                var dragOffsetY by remember(event.id) { mutableFloatStateOf(0f) }
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = dragOffsetY
                            alpha = (1f - dragOffsetY / 160f).coerceIn(0.45f, 1f)
                        }
                        .pointerInput(event.id) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (dragOffsetY > 24.dp.toPx()) onDismiss() else dragOffsetY = 0f
                                },
                                onDragCancel = { dragOffsetY = 0f },
                            ) { _, dy ->
                                dragOffsetY = (dragOffsetY + dy).coerceAtLeast(0f)
                            }
                        },
                ) {
                    PlayerToastPillCard(
                        toast = event,
                        backdrop = backdrop,
                        forceDarkSurface = forceDarkSurface,
                        onClick = { onTap?.invoke(event) ?: onDismiss() },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerToastPillCard(
    toast: PlayerToastEvent,
    backdrop: Backdrop?,
    forceDarkSurface: Boolean,
    onClick: () -> Unit,
) {
    val liquidGlass = LocalLiquidGlass.current
    val comic = LocalComicTheme.current
    val isDark = LocalIsDarkTheme.current
    val scheme = MaterialTheme.colorScheme
    val glassBackdrop = if (liquidGlass && !comic && isLiquidGlassBackdropSupported() && !forceDarkSurface) {
        backdrop
    } else {
        null
    }

    val pillBounds = remember { BackdropBoundsHolder() }
    val useLightInk = rememberBackdropUseLightInk(
        backdrop = glassBackdrop,
        bounds = pillBounds,
        enabled = glassBackdrop != null,
        scrimBaseAlpha = 0.28f,
        defaultUseLightInk = forceDarkSurface || isDark,
    )
    val onDarkBg = when {
        glassBackdrop != null -> useLightInk
        comic -> isDark
        liquidGlass -> forceDarkSurface || isDark
        else -> isDark
    }
    val contentColor by animateColorAsState(
        targetValue = when {
            glassBackdrop != null -> if (useLightInk) Color.White else Color(0xFF111319)
            comic -> scheme.onSurface
            liquidGlass -> if (onDarkBg) Color.White else Color(0xFF111319)
            else -> scheme.onSurface
        },
        animationSpec = tween(ADAPTIVE_INK_FADE_MS),
        label = "toastPillInk",
    )

    val pillShape = CircleShape
    val surfaceModifier = when {
        comic -> Modifier
            .comicPanel(pillShape, offset = 3.dp, stroke = 2.dp, translucent = false)
            .background(scheme.surfaceContainerHigh, pillShape)
        glassBackdrop != null -> Modifier
            .trackBackdropBounds(pillBounds)
            .background(
                liquidGlassContainerColor(
                    color = scheme.surfaceContainerHigh,
                    enabled = true,
                    backdrop = glassBackdrop,
                ),
                pillShape,
            )
            .liquidGlassChrome(
                shape = pillShape,
                enabled = true,
                preset = LiquidGlassPreset.FloatingControls,
                backdrop = glassBackdrop,
            )
            .glassRim(pillShape)
        liquidGlass -> {
            val fillBrush = Brush.linearGradient(
                if (onDarkBg) {
                    listOf(Color(0xFF242B3E).copy(alpha = 0.90f), Color(0xFF121622).copy(alpha = 0.94f))
                } else {
                    listOf(Color.White.copy(alpha = 0.92f), Color(0xFFF0F4FA).copy(alpha = 0.95f))
                },
            )
            val rimBrush = Brush.verticalGradient(
                if (onDarkBg) {
                    listOf(Color.White.copy(alpha = 0.44f), Color.White.copy(alpha = 0.14f))
                } else {
                    listOf(Color.White.copy(alpha = 0.95f), Color.Black.copy(alpha = 0.10f))
                },
            )
            Modifier
                .shadow(14.dp, pillShape)
                .background(fillBrush, pillShape)
                .border(1.dp, rimBrush, pillShape)
        }
        else -> Modifier
            .shadow(10.dp, pillShape)
            .background(scheme.surfaceContainerHigh, pillShape)
            .border(1.dp, scheme.outlineVariant.copy(alpha = if (isDark) 0.45f else 0.30f), pillShape)
    }

    val (badgeIcon, iconTint) = when (toast.kind) {
        PlayerToastKind.DOWNLOADED ->
            Icons.Filled.Check to if (onDarkBg) Color(0xFF34D399) else Color(0xFF0F9D6E)
        PlayerToastKind.LIKED_ADDED ->
            Icons.Filled.Favorite to if (onDarkBg) LikedHeartRedOnDark else LikedHeartRed
        PlayerToastKind.LIKED_REMOVED ->
            Icons.Outlined.FavoriteBorder to contentColor.copy(alpha = 0.80f)
        PlayerToastKind.PLAYLIST_ADDED ->
            Icons.Filled.PlaylistAdd to if (comic) ComicAccent else if (onDarkBg) Color(0xFF60A5FA) else scheme.primary
        PlayerToastKind.ERROR ->
            Icons.Filled.ErrorOutline to if (onDarkBg) Color(0xFFFF6B6B) else scheme.error
        PlayerToastKind.INFO ->
            Icons.Filled.Info to if (comic) ComicAccent else scheme.primary
    }

    Box(modifier = surfaceModifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .widthIn(max = 328.dp)
                .clip(pillShape)
                .clickable(onClick = onClick)
                .padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(iconTint.copy(alpha = if (comic) 0.24f else 0.20f), CircleShape)
                    .border(1.dp, iconTint.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = toast.title,
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = toast.subtitle
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = contentColor.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
