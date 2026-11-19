package com.lastwave.app.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Fades a full-bleed hero (art and veil) out at its bottom. Ending on solid page
 * colour drew a hard edge where the hero box stops and the page wash or the user's
 * background image carries on below.
 */
fun Modifier.heroFadeOut(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(Brush.verticalGradient(0.7f to Color.Black, 1f to Color.Transparent), blendMode = BlendMode.DstIn)
    }

/**
 * Veil over full-bleed artwork behind album, artist and podcast headers.
 * It is the page colour, not black: the titles on it use onBackground, so a
 * black veil left dark ink on darkened art in every light theme.
 */
@Composable
fun heroVeil(): Brush {
    val page = MaterialTheme.colorScheme.background
    return Brush.verticalGradient(
        0.0f to page.copy(alpha = 0.55f),
        0.30f to page.copy(alpha = 0.20f),
        0.60f to page.copy(alpha = 0.55f),
        0.85f to page.copy(alpha = 0.90f),
        1.0f to page,
    )
}
