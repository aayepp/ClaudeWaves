package com.lastwave.app.ui.player

import com.lastwave.app.ui.theme.PlayerPalette
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lastwave.app.ui.common.ExpressiveMotion
import com.lastwave.app.ui.theme.LiquidGlassPreset
import com.lastwave.app.ui.theme.LiquidGlassSurface
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalLiquidGlass
import com.lastwave.app.ui.theme.LocalLiquidGlassBackdrop
import com.lastwave.app.ui.theme.isLiquidGlassBackdropSupported
import com.lastwave.app.ui.theme.liquidGlassChrome
import com.lastwave.app.ui.theme.liquidGlassContainerColor
import com.lastwave.app.ui.theme.waterDropJiggle

private val UnlikedColor get() = PlayerPalette.ink.copy(alpha = 0.85f)
private val LikedBg get() = PlayerPalette.ink.copy(alpha = 0.92f)
private val UnlikedBg get() = PlayerPalette.ink.copy(alpha = 0.14f)
private val LikedHeartTint: Color
    get() = if (LikedBg.luminance() < 0.5f) LikedHeartRedOnDark else LikedHeartRed

@Composable
internal fun PlayerLikeButton(
    liked: Boolean,
    onClick: () -> Unit,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = ExpressiveMotion.spatialFast(),
        label = "heartPress",
    )

    val tapMotion = rememberLikedTapMotion(liked, initialScale = 0.45f)

    val fillProgress by animateFloatAsState(
        targetValue = if (liked) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "heartFill",
    )

    val isComic = LocalComicTheme.current
    val isGlassActive = LocalLiquidGlass.current && isLiquidGlassBackdropSupported() && LocalLiquidGlassBackdrop.current != null
    val background by animateColorAsState(
        targetValue = when {
            liked -> LikedBg
            isGlassActive && !isComic -> Color.Transparent
            else -> UnlikedBg
        },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "heartBackground",
    )

    val likedTint = LikedHeartTint
    val foreground by animateColorAsState(
        targetValue = if (liked) likedTint else UnlikedColor,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "heartForeground",
    )

    LiquidGlassSurface(
        glassModifier = Modifier.liquidGlassChrome(
            CircleShape,
            LocalLiquidGlass.current,
            LiquidGlassPreset.PlayerControls,
            interactionSource = interaction,
        ),
        onClick = onClick,
        interactionSource = interaction,
        shape = CircleShape,
        color = liquidGlassContainerColor(background),
        contentColor = foreground,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .likeSparkles(tapMotion, likedTint)
            .size(size)
            .waterDropJiggle(amplitude = 0.082f, tiltDegrees = 2.8f, dipDp = 3.dp)
            .semantics {
                selected = liked
                contentDescription = if (liked) "Unlike song" else "Like song"
            }
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
    ) {
        val iconSize = size * 0.52f
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (fillProgress < 1f) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = UnlikedColor,
                    modifier = Modifier
                        .size(iconSize)
                        .likeNudgeHeart(active = !liked)
                        .graphicsLayer {
                            alpha = (1f - fillProgress).coerceIn(0f, 1f)
                            val s = (1f - 0.2f * fillProgress).coerceIn(0.8f, 1f)
                            scaleX = s
                            scaleY = s
                        },
                )
            }
            if (fillProgress > 0f) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = likedTint,
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            alpha = fillProgress.coerceIn(0f, 1f)
                            val s = if (liked) tapMotion.scale else (0.8f + 0.2f * fillProgress)
                            scaleX = s
                            scaleY = s
                            rotationZ = tapMotion.rotationZ
                        },
                )
            }
        }
    }
}
