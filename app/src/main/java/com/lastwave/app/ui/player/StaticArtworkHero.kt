package com.lastwave.app.ui.player

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lastwave.app.data.artwork.ArtworkNormalizer

/**
 * Renders static album cover art in a full-bleed hero mode:
 * - Standard mode (`mirrorExtend = false`): top-anchored crop with smooth vertical feathering.
 * - Mirror-extended mode (`mirrorExtend = true`): displays the uncropped `1:1` cover cleanly
 *   and seamlessly extends it top & bottom using vertically-mirrored blurred strips so bright,
 *   pastel, dark, and typographic covers fill the viewport without hard seams or crushed black bars.
 *   YouTube 4:3 video thumbnails (`ytimg.com`) auto-crop horizontal black bars prior to mirroring.
 */
@Composable
fun StaticArtworkHero(
    name: String,
    artist: String,
    embeddedUrl: String?,
    resolvedUrl: String?,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    topFade: Float = 0f,
    bottomFade: Float = 0.42f,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.TopCenter,
    mirrorExtend: Boolean = false,
) {
    val context = LocalContext.current

    val highResUrl = resolvedUrl?.takeIf { it.isNotBlank() }
    val displayUrl = if (highResUrl != null) {
        highResUrl
    } else if (ArtworkNormalizer.isRealImage(embeddedUrl)) {
        ArtworkNormalizer.upscaleYoutubeArtwork(embeddedUrl)
    } else {
        null
    }

    if (mirrorExtend) {
        if (alpha <= 0.001f) return
        Box(modifier = modifier.graphicsLayer { this.alpha = alpha }) {
            AnimatedContent(
                targetState = displayUrl,
                transitionSpec = { fadeIn(tween(420)) togetherWith fadeOut(tween(420)) },
                label = "mirrorFullmodeArtwork",
            ) { targetUrl ->
                if (targetUrl.isNullOrBlank()) {
                    Box(Modifier.fillMaxSize())
                    return@AnimatedContent
                }
                val isVideoThumb = ArtworkNormalizer.isYoutubeVideoThumbnail(targetUrl)
                val cropZoom = if (isVideoThumb) 1.35f else 1f
                val sharpModel = remember(targetUrl, context) {
                    ImageRequest.Builder(context)
                        .data(targetUrl)
                        .allowHardware(true)
                        .build()
                }
                val blurredModel = remember(targetUrl, context) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        sharpModel
                    } else {
                        ImageRequest.Builder(context)
                            .data(targetUrl)
                            .allowHardware(false)
                            .transformations(BlurTransformation(radius = 24, maxDimension = 140))
                            .build()
                    }
                }
                val hwBlurTop = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(28.dp) else Modifier
                val hwBlurBot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(32.dp) else Modifier
                val hwBlurBase = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(38.dp) else Modifier
                val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

                BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
                    val coverSide = minOf(maxWidth, maxHeight * 0.64f)
                    val topOffset = (statusTop + 26.dp).coerceAtMost(((maxHeight - coverSide) * 0.34f).coerceAtLeast(0.dp))
                    val horizOffset = (maxWidth - coverSide) / 2f

                    // Ambient blurred base layer for wide / foldable screens
                    AsyncImage(
                        model = blurredModel,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(hwBlurBase)
                            .graphicsLayer {
                                scaleX = cropZoom * 1.08f
                                scaleY = cropZoom * 1.08f
                            },
                    )

                    // Top mirrored extension above focal cover
                    Box(
                        modifier = Modifier
                            .offset(y = topOffset - coverSide + 18.dp)
                            .size(maxWidth, coverSide)
                            .then(hwBlurTop)
                            .graphicsLayer { scaleY = -1f }
                            .clipToBounds(),
                    ) {
                        AsyncImage(
                            model = blurredModel,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = cropZoom
                                    scaleY = cropZoom
                                },
                        )
                    }

                    // Bottom mirrored extension behind playback controls
                    Box(
                        modifier = Modifier
                            .offset(y = topOffset + coverSide - 22.dp)
                            .size(maxWidth, coverSide)
                            .then(hwBlurBot)
                            .graphicsLayer { scaleY = -1f }
                            .clipToBounds(),
                    ) {
                        AsyncImage(
                            model = blurredModel,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = cropZoom
                                    scaleY = cropZoom
                                },
                        )
                    }

                    // Crisp uncropped 1:1 cover with soft top & bottom edge feathering
                    Box(
                        modifier = Modifier
                            .offset(x = horizOffset, y = topOffset)
                            .size(coverSide)
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.00f to Color.Transparent,
                                        0.07f to Color.Black,
                                        0.87f to Color.Black,
                                        1.00f to Color.Transparent,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                            }
                            .clipToBounds(),
                    ) {
                        AsyncImage(
                            model = sharpModel,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = cropZoom
                                    scaleY = cropZoom
                                },
                        )
                    }
                }
            }
        }
        return
    }

    if (displayUrl.isNullOrBlank()) return

    val model = remember(displayUrl, context) {
        ImageRequest.Builder(context)
            .data(displayUrl)
            .crossfade(350)
            .allowHardware(true)
            .build()
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                if (alpha <= 0.001f) return@drawWithContent
                drawContent()
                if (topFade > 0.001f || bottomFade > 0.001f) {
                    val topFrac = topFade.coerceIn(0f, 0.45f)
                    val bottomFrac = bottomFade.coerceIn(0f, 0.50f)
                    val stops = buildList {
                        if (topFrac > 0.001f) {
                            add(0f to Color.Transparent)
                            add(topFrac to Color.Black)
                        } else {
                            add(0f to Color.Black)
                        }
                        if (bottomFrac > 0.001f) {
                            add((1f - bottomFrac).coerceAtLeast(topFrac) to Color.Black)
                            add(1f to Color.Transparent)
                        } else {
                            add(1f to Color.Black)
                        }
                    }
                    drawRect(
                        brush = Brush.verticalGradient(
                            *stops.toTypedArray(),
                            startY = 0f,
                            endY = size.height,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
            },
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = contentScale,
            alignment = alignment,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
