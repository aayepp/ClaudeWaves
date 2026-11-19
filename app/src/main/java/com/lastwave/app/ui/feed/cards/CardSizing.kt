/*
 * Responsive card sizing for native shelf cards. Widths derive from the
 * available viewport so ~2.3 cards peek on phones and scale up on tablets /
 * landscape. Pure Dp math — no recomposition cost beyond configuration.
 */
package com.lastwave.app.ui.feed.cards

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * When provided by a symmetric paged shelf container, cards match the exact
 * column width inside the 16.dp horizontal page grid so zero peeking items clip.
 */
internal val LocalSymmetricCardWidth = staticCompositionLocalOf<Dp?> { null }

/**
 * Remembers the unified [CardDimensions] for the current viewport width.
 */
@Composable
fun rememberCardDimensions(): CardDimensions {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    return remember(screenWidth) {
        calculateCardDimensions(screenWidth)
    }
}

/** Unified cover height for wide 16:9 cards; scales 16:9 in symmetric pagers. */
@Composable
fun rememberCoverHeight(): Dp =
    LocalSymmetricCardWidth.current?.let { (it.value * (9f / 16f)).dp }
        ?: rememberCardDimensions().coverHeight

/** Square card width: two full cards with a small third-cover peek on phones. */
@Composable
fun rememberSquareCardWidth(): Dp {
    LocalSymmetricCardWidth.current?.let { return it }
    val dimensions = rememberCardDimensions()
    val peekWidth = with(LocalDensity.current) { 20.toDp() }
    // Reserve the existing section margins, row start inset, and two card gaps.
    val widthForTwoCards =
        (dimensions.screenWidth - 48.dp - CardTokens.cardGap * 2 - peekWidth) / 2
    return dimensions.squareWidth.coerceAtMost(widthForTwoCards)
}

/** Wide 16:9 card width: equals coverHeight * 16/9, or full slot width in symmetric pagers. */
@Composable
fun rememberWideCardWidth(): Dp = LocalSymmetricCardWidth.current ?: rememberCardDimensions().wideWidth

/** Featured big-square card width: ~1.7 cards peek on phones, or exact slot width in symmetric pagers. */
@Composable
fun rememberFeaturedCardWidth(): Dp {
    LocalSymmetricCardWidth.current?.let { return it }
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    return when {
        screenWidth < 360.dp -> (screenWidth * 0.56f).coerceAtLeast(180.dp)
        screenWidth < 600.dp -> (screenWidth * 0.54f).coerceAtMost(260.dp)
        screenWidth < 840.dp -> (screenWidth * 0.36f).coerceAtMost(300.dp)
        else -> (screenWidth * 0.24f).coerceAtMost(320.dp)
    }
}

/** Community playlist card width: a wide glass card, ~1.3 peek on phones. */
@Composable
fun rememberCommunityCardWidth(): Dp {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    return when {
        screenWidth < 360.dp -> (screenWidth * 0.78f).coerceAtLeast(260.dp)
        screenWidth < 600.dp -> (screenWidth * 0.76f).coerceAtMost(340.dp)
        screenWidth < 840.dp -> (screenWidth * 0.5f).coerceAtMost(380.dp)
        else -> (screenWidth * 0.34f).coerceAtMost(400.dp)
    }
}

/** Podcast episode card width: large 16:9 card ~80% of screen width, clamped ~260-400dp, next card peeking. */
@Composable
fun rememberPodcastEpisodeCardWidth(): Dp {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    return (screenWidth * 0.80f).coerceIn(260.dp, 400.dp)
}

