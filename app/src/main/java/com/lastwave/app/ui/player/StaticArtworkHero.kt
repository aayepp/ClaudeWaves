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
import androidx.compose.ui.draw.BlurredEdgeTreatment
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
import androidx.compose.ui.unit.Dp
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
                val lightBlurModel = remember(targetUrl, context) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        sharpModel
                    } else {
                        ImageRequest.Builder(context)
                            .data(targetUrl)
                            .allowHardware(false)
                            .transformations(BlurTransformation(radius = 10, maxDimension = 180))
                            .build()
                    }
                }
                val deepBlurModel = remember(targetUrl, context) {
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
                val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

                BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
                    val coverSide = minOf(maxWidth, maxHeight * 0.62f)
                    val topOffset = (statusTop + 28.dp).coerceAtMost(((maxHeight - coverSide) * 0.28f).coerceAtLeast(0.dp))
                    val horizOffset = (maxWidth - coverSide) / 2f
                    val totalH = maxHeight.value.coerceAtLeast(1f)
                    val coverTopF = (topOffset.value / totalH).coerceIn(0.02f, 0.35f)
                    val coverBotF = ((topOffset.value + coverSide.value) / totalH).coerceIn(0.35f, 0.85f)
                    val sideF = (coverSide.value / totalH).coerceIn(0.25f, 0.70f)

                    // Wide / foldable ambient fill behind centered mirrored stack
                    if (coverSide < maxWidth) {
                        AsyncImage(
                            model = deepBlurModel,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        Modifier.blur(38.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                                    } else {
                                        Modifier
                                    },
                                )
                                .graphicsLayer {
                                    scaleX = cropZoom * 1.08f
                                    scaleY = cropZoom * 1.08f
                                },
                        )
                    }

                    // Layer 0: Crisp pixel-aligned 1:1 cover + seamless top/bottom vertical mirrors (0.dp gap)
                    MirroredArtworkTiles(
                        model = sharpModel,
                        coverSide = coverSide,
                        topOffset = topOffset,
                        horizOffset = horizOffset,
                        maxHeight = maxHeight,
                        cropZoom = cropZoom,
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ProgressiveBlurOverlay(
                            model = sharpModel,
                            coverSide = coverSide,
                            topOffset = topOffset,
                            horizOffset = horizOffset,
                            maxHeight = maxHeight,
                            cropZoom = cropZoom,
                            blurRadius = 10.dp,
                            stops = remember(coverTopF, coverBotF, sideF) {
                                smoothVerticalStops(
                                    topFull = (coverTopF * 0.68f).coerceAtLeast(0.01f),
                                    topClear = (coverTopF + sideF * 0.055f).coerceAtMost(coverBotF - 0.12f),
                                    botStart = (coverBotF - sideF * 0.08f).coerceAtLeast(coverTopF + 0.12f),
                                    botFull = (coverBotF + sideF * 0.12f).coerceAtMost(0.96f),
                                )
                            },
                        )
                        ProgressiveBlurOverlay(
                            model = sharpModel,
                            coverSide = coverSide,
                            topOffset = topOffset,
                            horizOffset = horizOffset,
                            maxHeight = maxHeight,
                            cropZoom = cropZoom,
                            blurRadius = 22.dp,
                            stops = remember(coverTopF, coverBotF, sideF) {
                                smoothVerticalStops(
                                    topFull = (coverTopF * 0.62f).coerceAtLeast(0.01f),
                                    topClear = (coverTopF + sideF * 0.02f).coerceAtMost(coverBotF - 0.14f),
                                    botStart = (coverBotF - sideF * 0.02f).coerceAtLeast(coverTopF + 0.14f),
                                    botFull = (coverBotF + sideF * 0.30f).coerceAtMost(0.98f),
                                )
                            },
                        )
                        ProgressiveBlurOverlay(
                            model = sharpModel,
                            coverSide = coverSide,
                            topOffset = topOffset,
                            horizOffset = horizOffset,
                            maxHeight = maxHeight,
                            cropZoom = cropZoom,
                            blurRadius = 38.dp,
                            stops = remember(coverTopF, coverBotF, sideF) {
                                smoothVerticalStops(
                                    topFull = (coverTopF * 0.52f).coerceAtLeast(0.005f),
                                    topClear = (coverTopF * 0.90f).coerceAtLeast(0.02f),
                                    botStart = (coverBotF + sideF * 0.10f).coerceAtMost(0.92f),
                                    botFull = (coverBotF + sideF * 0.50f).coerceAtMost(0.99f),
                                )
                            },
                        )
                    } else {
                        ProgressiveBlurOverlay(
                            model = lightBlurModel,
                            coverSide = coverSide,
                            topOffset = topOffset,
                            horizOffset = horizOffset,
                            maxHeight = maxHeight,
                            cropZoom = cropZoom,
                            blurRadius = 0.dp,
                            stops = remember(coverTopF, coverBotF, sideF) {
                                smoothVerticalStops(
                                    topFull = (coverTopF * 0.68f).coerceAtLeast(0.01f),
                                    topClear = (coverTopF + sideF * 0.055f).coerceAtMost(coverBotF - 0.12f),
                                    botStart = (coverBotF - sideF * 0.08f).coerceAtLeast(coverTopF + 0.12f),
                                    botFull = (coverBotF + sideF * 0.14f).coerceAtMost(0.96f),
                                )
                            },
                        )
                        ProgressiveBlurOverlay(
                            model = deepBlurModel,
                            coverSide = coverSide,
                            topOffset = topOffset,
                            horizOffset = horizOffset,
                            maxHeight = maxHeight,
                            cropZoom = cropZoom,
                            blurRadius = 0.dp,
                            stops = remember(coverTopF, coverBotF, sideF) {
                                smoothVerticalStops(
                                    topFull = (coverTopF * 0.55f).coerceAtLeast(0.005f),
                                    topClear = (coverTopF + sideF * 0.01f).coerceAtMost(coverBotF - 0.14f),
                                    botStart = coverBotF.coerceAtLeast(coverTopF + 0.14f),
                                    botFull = (coverBotF + sideF * 0.38f).coerceAtMost(0.985f),
                                )
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

@Composable
private fun MirroredArtworkTiles(
    model: ImageRequest,
    coverSide: Dp,
    topOffset: Dp,
    horizOffset: Dp,
    maxHeight: Dp,
    cropZoom: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        MirroredTile(
            model = model,
            coverSide = coverSide,
            offsetX = horizOffset,
            offsetY = topOffset - coverSide,
            flipY = true,
            cropZoom = cropZoom,
        )
        MirroredTile(
            model = model,
            coverSide = coverSide,
            offsetX = horizOffset,
            offsetY = topOffset,
            flipY = false,
            cropZoom = cropZoom,
        )
        MirroredTile(
            model = model,
            coverSide = coverSide,
            offsetX = horizOffset,
            offsetY = topOffset + coverSide,
            flipY = true,
            cropZoom = cropZoom,
        )
        if (topOffset + coverSide * 2 < maxHeight) {
            MirroredTile(
                model = model,
                coverSide = coverSide,
                offsetX = horizOffset,
                offsetY = topOffset + coverSide * 2,
                flipY = false,
                cropZoom = cropZoom,
            )
        }
    }
}

@Composable
private fun MirroredTile(
    model: ImageRequest,
    coverSide: Dp,
    offsetX: Dp,
    offsetY: Dp,
    flipY: Boolean,
    cropZoom: Float,
) {
    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(coverSide)
            .graphicsLayer { if (flipY) scaleY = -1f }
            .clipToBounds(),
    ) {
        AsyncImage(
            model = model,
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

@Composable
private fun ProgressiveBlurOverlay(
    model: ImageRequest,
    coverSide: Dp,
    topOffset: Dp,
    horizOffset: Dp,
    maxHeight: Dp,
    cropZoom: Float,
    blurRadius: Dp,
    stops: Array<Pair<Float, Color>>,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(*stops),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        MirroredArtworkTiles(
            model = model,
            coverSide = coverSide,
            topOffset = topOffset,
            horizOffset = horizOffset,
            maxHeight = maxHeight,
            cropZoom = cropZoom,
            modifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0.dp) {
                Modifier.blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            } else {
                Modifier
            },
        )
    }
}

private fun smoothVerticalStops(
    topFull: Float,
    topClear: Float,
    botStart: Float,
    botFull: Float,
): Array<Pair<Float, Color>> {
    val tf = topFull.coerceIn(0f, 0.45f)
    val tc = topClear.coerceIn(tf + 0.005f, 0.48f)
    val bs = botStart.coerceIn(tc + 0.005f, 0.96f)
    val bf = botFull.coerceIn(bs + 0.005f, 0.995f)
    return buildList {
        if (tf > 0f) add(0f to Color.Black)
        add(tf to Color.Black)
        add((tf + (tc - tf) * 0.25f) to Color.Black.copy(alpha = 0.84f))
        add((tf + (tc - tf) * 0.50f) to Color.Black.copy(alpha = 0.50f))
        add((tf + (tc - tf) * 0.75f) to Color.Black.copy(alpha = 0.16f))
        add(tc to Color.Transparent)
        add(bs to Color.Transparent)
        add((bs + (bf - bs) * 0.25f) to Color.Black.copy(alpha = 0.16f))
        add((bs + (bf - bs) * 0.50f) to Color.Black.copy(alpha = 0.50f))
        add((bs + (bf - bs) * 0.75f) to Color.Black.copy(alpha = 0.84f))
        add(bf to Color.Black)
        add(1f to Color.Black)
    }.toTypedArray()
}
