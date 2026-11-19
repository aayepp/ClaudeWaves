package com.lastwave.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * Page fill with glass on: the theme background plus soft blobs of the accent colours.
 * Glass cards are translucent, so the blobs glow through them the way a blurred
 * wallpaper glows through vivo's control-centre cards; over a flat page a glass card
 * is only a tinted box. The blobs are already soft, so cards need no blur pass of
 * their own. Static: no animation.
 */
fun DrawScope.drawPageWash(background: Color, a: Color, b: Color, glass: Boolean, dark: Boolean) {
    drawRect(background)
    if (!glass) return
    val intensity = LiquidGlassIntensity.value
    val alpha = if (dark) lerp(0.16f, 0.30f, intensity) else lerp(0.20f, 0.34f, intensity)
    fun blob(color: Color, x: Float, y: Float, r: Float, weight: Float = 1f) = drawRect(
        Brush.radialGradient(
            listOf(color.copy(alpha = alpha * weight), color.copy(alpha = 0f)),
            center = Offset(size.width * x, size.height * y),
            radius = size.width * r,
        ),
    )
    blob(a, 0.1f, 0.06f, 0.85f)
    blob(b, 1f, 0.42f, 0.8f)
    blob(a, 0.05f, 0.92f, 0.75f)
    blob(b, 0.92f, 0.86f, 0.65f, weight = 0.7f)
}

/** [drawPageWash] with this screen's theme, for backdrop capture lambdas. */
@Composable
fun rememberPageWash(): DrawScope.() -> Unit {
    val scheme = MaterialTheme.colorScheme
    val glass = LocalLiquidGlass.current
    val dark = LocalIsDarkTheme.current
    return remember(scheme.background, scheme.primary, scheme.tertiary, glass, dark) {
        { drawPageWash(scheme.background, scheme.primary, scheme.tertiary, glass, dark) }
    }
}

/** Opaque page background a screen paints over the app root: carries the wash. */
@Composable
fun Modifier.pageBackground(): Modifier = drawBehind(rememberPageWash())

/** Card fill: see-through with glass on, following the intensity slider (0 solid, 1 clearest). */
@Composable
fun glassCardColor(base: Color = MaterialTheme.colorScheme.surfaceContainerHigh): Color =
    if (LocalLiquidGlass.current) base.copy(alpha = base.alpha * lerp(0.90f, 0.44f, LiquidGlassIntensity.value)) else base

/** Diagonal specular rim catching the top-left edge of a glass card, bouncing faintly at bottom-right. */
@Composable
fun glassCardBorder(): BorderStroke? {
    if (!LocalLiquidGlass.current) return null
    val dark = LocalIsDarkTheme.current
    val boost = lerp(0.85f, 1.15f, LiquidGlassIntensity.value)
    return BorderStroke(
        1.dp,
        Brush.linearGradient(
            0f to Color.White.copy(alpha = ((if (dark) 0.38f else 0.92f) * boost).coerceAtMost(1f)),
            0.48f to if (dark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.08f),
            1f to if (dark) Color.White.copy(alpha = (0.20f * boost).coerceAtMost(1f)) else Color.Black.copy(alpha = 0.12f),
        ),
    )
}
