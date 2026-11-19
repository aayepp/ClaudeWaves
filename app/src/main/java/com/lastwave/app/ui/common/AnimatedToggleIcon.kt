package com.lastwave.app.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import com.lastwave.app.ui.player.sparkleBurst
import kotlinx.coroutines.launch

/**
 * An icon that switches between [onIcon] and [offIcon] with a pop: the new one springs
 * in from 60% size. With [sparkle], turning on also bursts sparkles (a save, a finished
 * download). Keyed on [on] itself, so a change made elsewhere animates too; the first
 * composition stays still.
 */
@Composable
fun AnimatedToggleIcon(
    on: Boolean,
    onIcon: ImageVector,
    offIcon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    sparkle: Boolean = false,
) {
    val pop = remember { Animatable(1f) }
    val burst = remember { Animatable(1f) }
    var last by remember { mutableStateOf(on) }
    LaunchedEffect(on) {
        if (on == last) return@LaunchedEffect
        last = on
        launch {
            pop.snapTo(0.6f)
            pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f))
        }
        if (on && sparkle) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        }
    }
    Icon(
        imageVector = if (on) onIcon else offIcon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
            .then(if (sparkle) Modifier.sparkleBurst({ burst.value }, tint) else Modifier)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            },
    )
}
