package com.lastwave.app.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.LocalLiquidGlass
import com.lastwave.app.ui.theme.comicPanel
import com.lastwave.app.ui.theme.glassCardBorder
import com.lastwave.app.ui.theme.glassCardColor

/**
 * Keeps screen snackbars above the floating mini player, styled as a translucent glass pill
 * when Liquid Glass is active. Uses tinted glass surface + specular rim rather than sampling
 * [LocalLiquidGlassBackdrop] because Scaffold snackbar hosts sit INSIDE PlayerHost's
 * liquidGlassSource (sampling it from inside causes a backdrop self-capture crash).
 */
@Composable
fun MiniPlayerAwareSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val useGlass = LocalLiquidGlass.current && !LocalComicTheme.current
    val isComic = LocalComicTheme.current
    val isDark = LocalIsDarkTheme.current

    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(bottom = LocalMiniPlayerSnackbarClearance.current),
    ) { data ->
        val actionLabel = data.visuals.actionLabel
        val shape = CircleShape
        val containerColor = when {
            useGlass -> glassCardColor(
                if (isDark) MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            )
            isComic -> MaterialTheme.colorScheme.surfaceContainerHigh
            else -> MaterialTheme.colorScheme.inverseSurface
        }
        val contentColor = when {
            useGlass || isComic -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.inverseOnSurface
        }
        val actionColor = when {
            useGlass || isComic -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.inversePrimary
        }

        Surface(
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            border = if (useGlass) glassCardBorder() else null,
            shadowElevation = if (isComic) 0.dp else 8.dp,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .widthIn(max = 520.dp)
                .comicPanel(shape, offset = 3.dp, translucent = useGlass),
        ) {
            Row(
                modifier = Modifier
                    .defaultMinSize(minHeight = 46.dp)
                    .padding(
                        start = 18.dp,
                        end = if (actionLabel != null) 8.dp else 18.dp,
                        top = 6.dp,
                        bottom = 6.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (actionLabel != null) {
                    TextButton(onClick = { data.performAction() }) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = actionColor,
                        )
                    }
                }
            }
        }
    }
}
