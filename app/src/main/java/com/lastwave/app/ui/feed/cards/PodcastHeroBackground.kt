package com.lastwave.app.ui.feed.cards

import com.lastwave.app.ui.common.heroVeil
import com.lastwave.app.ui.common.heroFadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lastwave.app.R

/**
 * Top hero photo for the Podcasts chip feed.
 *
 * Extends full-width behind the status bar, header and chips row, fading into
 * the dark app background via a vertical gradient. Supports live background URLs
 * with a high-fidelity local fallback drawable, crossfades smoothly, and scrolls
 * with the list.
 */
@Composable
fun PodcastHeroBackground(
    backgroundUrl: String?,
    scrollOffsetY: () -> Float,
    modifier: Modifier = Modifier,
    heroHeight: Dp = 320.dp,
) {
    val context = LocalContext.current
    val imageModel = backgroundUrl?.takeIf { it.isNotBlank() } ?: R.drawable.podcast_hero_fallback

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight)
            .graphicsLayer {
                translationY = -scrollOffsetY().coerceAtLeast(0f)
            }
            .heroFadeOut(),
    ) {
        // Hero Image with crossfade and local fallback
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageModel)
                .crossfade(true)
                .error(R.drawable.podcast_hero_fallback)
                .fallback(R.drawable.podcast_hero_fallback)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        Box(Modifier.fillMaxSize().background(heroVeil()))
    }
}
