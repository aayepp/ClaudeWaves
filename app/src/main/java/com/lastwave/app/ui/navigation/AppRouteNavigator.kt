package com.lastwave.app.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton navigation coordinator allowing external components (notifications,
 * system intents, shortcuts) to trigger navigation to app destinations.
 */
@Singleton
class AppRouteNavigator @Inject constructor() {
    private val _pendingRoute = MutableStateFlow<String?>(null)
    val pendingRoute: StateFlow<String?> = _pendingRoute.asStateFlow()

    fun navigateTo(route: String) {
        if (route.isNotBlank()) {
            _pendingRoute.value = route
        }
    }

    @Volatile private var returnToPlayer = false

    /** Opens [route] over a collapsing Now Playing; that screen's back reopens the player. */
    fun navigateFromPlayer(route: String) {
        returnToPlayer = true
        navigateTo(route)
    }

    /** True once for the screen [navigateFromPlayer] opened. */
    fun consumeReturnToPlayer(): Boolean = returnToPlayer.also { returnToPlayer = false }

    fun consumeRoute(): String? {
        val route = _pendingRoute.value
        _pendingRoute.value = null
        return route
    }
}
