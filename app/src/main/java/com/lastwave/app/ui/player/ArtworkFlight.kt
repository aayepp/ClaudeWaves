package com.lastwave.app.ui.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * The cover's flight between the mini player's thumbnail and Now Playing's sleeve while the
 * sheet opens or closes (after Auris' PlayerDock). Both ends report themselves here; the
 * host draws the cover in between and the two ends hide while it is in the air.
 */
@Stable
internal class ArtworkFlight(val progress: () -> Float) {
    /** Thumbnail bounds in root coordinates, captured while the sheet is closed. */
    var thumb by mutableStateOf<Rect?>(null)
    /** The sleeve's coordinates; its resting place is worked out by the host. */
    var sleeve: LayoutCoordinates? = null
    /** The host the cover is drawn in; the sleeve's bounds are measured against it. */
    var host: LayoutCoordinates? = null

    // Derived (after Auris' PlayerDock.coversMiniArt) so listeners only flip twice per
    // transition (takeoff & landing) instead of invalidating the page layer on every frame.
    private val flyingState = derivedStateOf { progress().let { it > 0f && it < 1f } }
    val flying: Boolean get() = flyingState.value
}

internal val LocalArtworkFlight = staticCompositionLocalOf<ArtworkFlight?> { null }

/** Mini player thumbnail: reports where the flight starts and hides while the cover is away. */
internal fun Modifier.artworkFlightThumb(flight: ArtworkFlight?): Modifier =
    if (flight == null) this else this
        .onGloballyPositioned { if (flight.progress() == 0f) flight.thumb = it.boundsInRoot() }
        .graphicsLayer { alpha = if (flight.flying) 0f else 1f }

/** Now Playing sleeve: reports where the flight lands and hides until it does. */
internal fun Modifier.artworkFlightSleeve(flight: ArtworkFlight?): Modifier =
    if (flight == null) this else this
        .onGloballyPositioned { flight.sleeve = it }
        .graphicsLayer { alpha = if (flight.flying) 0f else 1f }
