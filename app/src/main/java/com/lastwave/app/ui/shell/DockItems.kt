package com.lastwave.app.ui.shell

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lastwave.app.ui.comic.Button
import com.lastwave.app.ui.comic.Checkbox
import com.lastwave.app.ui.common.ModalBottomSheet
import kotlin.math.roundToInt

/**
 * One home-dock button: a pager tab when [tab] is set, otherwise a shortcut that opens a
 * screen or acts. Ids are persisted, so never rename one.
 */
internal enum class DockItem(val id: String, val label: String, val icon: ImageVector, val tab: MainTab? = null) {
    FEED("feed", "Feed", Icons.Filled.Home, MainTab.FEED),
    STATS("stats", "Stats", Icons.Filled.Leaderboard, MainTab.STATS),
    PLAYLISTS("playlists", "Library", Icons.AutoMirrored.Filled.QueueMusic, MainTab.PLAYLISTS),
    LIKED("liked", "Liked", Icons.Filled.Favorite),
    SHUFFLE_LIKED("shuffle_liked", "Shuffle", Icons.Filled.Shuffle),
    DOWNLOADS("downloads", "Downloads", Icons.Filled.Download),
    DISCOVER("discover", "Discover", Icons.Filled.Explore),
    PODCASTS("podcasts", "Podcasts", Icons.Filled.Podcasts),
    NEW_RELEASES("new_releases", "New", Icons.Filled.NewReleases),
    GENRES("genres", "Genres", Icons.Filled.Category),
    JAM("jam", "Jam", Icons.Filled.Groups),
    SETTINGS("settings", "Settings", Icons.Filled.Settings),
    ;

    val isTab: Boolean get() = tab != null

    companion object {
        const val MAX = 4
        val DEFAULT = listOf(FEED, STATS, PLAYLISTS)

        /** Unknown ids dropped, at most [MAX], and never without a tab so Home has a page. */
        fun parse(raw: String?): List<DockItem> {
            val items = raw.orEmpty().split(',')
                .mapNotNull { id -> entries.firstOrNull { it.id == id.trim() } }
                .distinct()
                .take(MAX)
            return if (items.any { it.isTab }) items else DEFAULT
        }

        fun serialize(items: List<DockItem>): String = items.joinToString(",") { it.id }
    }
}

/** Long-press editor for the dock: pick up to [DockItem.MAX], drag the handle to reorder. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DockEditorSheet(
    current: List<DockItem>,
    onDismiss: () -> Unit,
    onSave: (List<DockItem>) -> Unit,
) {
    val selected = remember { mutableStateListOf<DockItem>().apply { addAll(current) } }
    val haptics = LocalHapticFeedback.current
    val rowHeight = 52.dp
    val rowPx = with(LocalDensity.current) { rowHeight.toPx() }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Susun dock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Pilih hingga ${DockItem.MAX}. Seret ≡ untuk tukar susunan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "${selected.size} / ${DockItem.MAX}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(10.dp))

            selected.toList().forEachIndexed { index, item ->
              // Keyed by item so a row keeps its drag gesture while it moves to a new position.
              key(item) {
                val isDragging = draggingIndex == index
                DockEditorRow(
                    item = item,
                    checked = true,
                    // The last tab stays: without one, Home would have no page to show.
                    enabled = !(item.isTab && selected.count { it.isTab } == 1),
                    onToggle = { selected.remove(item) },
                    handle = Modifier.pointerInput(item) {
                        detectDragGestures(
                            onDragStart = {
                                draggingIndex = selected.indexOf(item)
                                dragOffset = 0f
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragEnd = { draggingIndex = -1; dragOffset = 0f },
                            onDragCancel = { draggingIndex = -1; dragOffset = 0f },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount.y
                                val from = draggingIndex
                                val steps = (dragOffset / rowPx).roundToInt()
                                val to = (from + steps).coerceIn(0, selected.lastIndex)
                                if (from >= 0 && to != from) {
                                    selected.add(to, selected.removeAt(from))
                                    dragOffset -= (to - from) * rowPx
                                    draggingIndex = to
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                }
                            },
                        )
                    },
                    modifier = Modifier
                        .height(rowHeight)
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffset else 0f
                            scaleX = if (isDragging) 1.02f else 1f
                            scaleY = scaleX
                        },
                )
              }
            }
            DockItem.entries.filter { it !in selected }.forEach { item ->
                DockEditorRow(
                    item = item,
                    checked = false,
                    enabled = selected.size < DockItem.MAX,
                    onToggle = { selected.add(item) },
                    handle = null,
                    modifier = Modifier.height(rowHeight),
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onSave(selected.toList()) },
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text("Siap", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DockEditorRow(
    item: DockItem,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    handle: Modifier?,
    modifier: Modifier,
) {
    val faded = !checked && !enabled
    Row(
        modifier = modifier.fillMaxWidth().graphicsLayer { alpha = if (faded) 0.45f else 1f },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (handle != null) {
            Icon(
                Icons.Filled.DragHandle,
                contentDescription = "Seret untuk susun ${item.label}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = handle.size(24.dp),
            )
        } else {
            Spacer(Modifier.width(24.dp))
        }
        Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        Text(item.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (item.isTab) {
            Text(
                "Tab",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() }, enabled = enabled)
    }
}
