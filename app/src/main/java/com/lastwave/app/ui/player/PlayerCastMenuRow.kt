package com.lastwave.app.ui.player

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lastwave.app.ui.comic.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lastwave.app.R
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.lastwave.app.playback.MusicPlayer

@Composable
internal fun PlayerCastMenuRow(
    player: MusicPlayer,
    compact: Boolean = false,
) {
    val context = LocalContext.current
    val available = remember(player) { player.initializeCast() }
    var routeButton by remember { mutableStateOf<MediaRouteButton?>(null) }
    val badgeSize = if (compact) 32.dp else 40.dp
    val badgeCorner = if (compact) 10.dp else 13.dp
    Row(
        Modifier.fillMaxWidth().clickable(
            onClick = hapticClick {
                if (available) routeButton?.performClick()
                else Toast.makeText(context, context.getString(R.string.cast_unavailable), Toast.LENGTH_SHORT).show()
            },
        ).padding(horizontal = 12.dp, vertical = if (compact) 9.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val badge = Modifier.size(badgeSize).clip(RoundedCornerShape(badgeCorner))
            .background(MaterialTheme.colorScheme.primaryContainer)
        if (available) {
            AndroidView(
                modifier = badge,
                factory = { viewContext ->
                    MediaRouteButton(viewContext).apply {
                        background = null
                        contentDescription = "Chromecast"
                        CastButtonFactory.setUpMediaRouteButton(viewContext, this)
                        routeButton = this
                    }
                },
            )
        } else {
            Icon(Icons.Filled.Cast, null, badge.padding(if (compact) 7.dp else 10.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(if (compact) 12.dp else 16.dp))
        Text(
            stringResource(R.string.cast_title),
            style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
        )
    }
}
