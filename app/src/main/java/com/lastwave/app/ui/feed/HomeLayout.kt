package com.lastwave.app.ui.feed

import com.lastwave.app.ui.common.AnimatedToggleIcon

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.lastwave.app.ui.common.LazyListReorder
import com.lastwave.app.ui.common.rememberLazyListReorder
import com.lastwave.app.ui.common.reorderHandle
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.lastwave.app.ui.common.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lastwave.app.ui.comic.Button
import com.lastwave.app.ui.comic.DropdownMenuItem
import com.lastwave.app.ui.comic.IconButton
import com.lastwave.app.ui.comic.Surface
import com.lastwave.app.ui.theme.sectionTitleStyle

/**
 * The Home blocks a user can reorder or hide. Greeting and search stay on top
 * and are not listed. Ids are what Settings stores in homeSectionsOrder and
 * hiddenHomeSections; ids written by older layouts simply match nothing.
 */
internal enum class HomeBlock(
    val id: String,
    val title: String,
    val icon: ImageVector,
    /** Optional extras start hidden so the default Home stays as it was. */
    val hiddenByDefault: Boolean = false,
) {
    QUICK_ACCESS("quick_access", "Quick access", Icons.Filled.Apps),
    MOOD_CHIPS("mood_chips", "Mood chips", Icons.Filled.Mood),
    TOP_CHARTS("top_charts", "Top charts", Icons.Filled.Leaderboard),
    CONTINUE_LISTENING("continue_listening", "Continue listening", Icons.Filled.History),
    MADE_FOR_YOU("made_for_you", "Made for you", Icons.AutoMirrored.Filled.QueueMusic),
    RECOMMENDED("recommended", "Recommended for you", Icons.Filled.LibraryMusic),
    BECAUSE_YOU_LISTEN("because_you_listen", "Because you listen to", Icons.Filled.AutoAwesome),
    HI_RES_PICKS("hi_res_picks", "Hi-Res picks", Icons.Filled.HighQuality),
    PODCASTS("podcasts", "Podcasts", Icons.Filled.Podcasts),
    NEW_RELEASES("new_releases", "New releases", Icons.Filled.NewReleases, hiddenByDefault = true),
    YOUR_MIXES("your_mixes", "Your mixes", Icons.Filled.Shuffle, hiddenByDefault = true),
    QUICK_PICKS("quick_picks", "Quick picks", Icons.Filled.Bolt, hiddenByDefault = true),
    SUGGESTED_PLAYLISTS("suggested_playlists", "Suggested playlists", Icons.AutoMirrored.Filled.PlaylistPlay, hiddenByDefault = true),
    JUMP_BACK_IN("jump_back_in", "Jump back in", Icons.Filled.Album, hiddenByDefault = true),
    COUNTRY_CHARTS("country_charts", "Country charts", Icons.Filled.Public, hiddenByDefault = true),
    DOWNLOADS("downloads", "Downloads", Icons.Filled.DownloadDone, hiddenByDefault = true),
    LOCAL_MUSIC("local_music", "Local music", Icons.Filled.PhoneAndroid, hiddenByDefault = true),
    ON_REPEAT("on_repeat", "Your stats · On repeat", Icons.Filled.Repeat, hiddenByDefault = true),
    FAVOURITE_ARTISTS("favourite_artists", "Favourite artists", Icons.Filled.Person, hiddenByDefault = true),
    REDISCOVER("rediscover", "Rediscover", Icons.Filled.Restore, hiddenByDefault = true),
}

/**
 * The stored set lists what the user toggled away from each block's default:
 * for a normal block that means hidden, for a [HomeBlock.hiddenByDefault] block
 * it means shown. One toggle call flips either way.
 */
internal fun isHomeBlockHidden(block: HomeBlock, toggled: Set<String>): Boolean =
    (block.id in toggled) != block.hiddenByDefault

/** Saved order first, then any block it doesn't mention, in default order. */
internal fun resolveHomeBlocks(order: List<String>): List<HomeBlock> =
    (order.mapNotNull { id -> HomeBlock.entries.firstOrNull { it.id == id } } + HomeBlock.entries).distinct()

/**
 * Swaps [block] with the nearest visible block above or below it, so one tap
 * always moves it past something the user can see. Hidden blocks keep their slots.
 */
internal fun moveHomeBlock(order: List<HomeBlock>, toggled: Set<String>, block: HomeBlock, up: Boolean): List<HomeBlock> {
    val from = order.indexOf(block)
    if (from < 0) return order
    val candidates = if (up) (from - 1 downTo 0) else (from + 1..order.lastIndex)
    val to = candidates.firstOrNull { !isHomeBlockHidden(order[it], toggled) } ?: return order
    return order.toMutableList().apply {
        this[from] = order[to]
        this[to] = block
    }
}

private val RowHeight = 56.dp
private val RowGap = 8.dp

private fun rowKey(block: HomeBlock) = "home_block_${block.id}"

/** Rows the drag reorders live; saved once, when the finger lifts. */
@Stable
internal class HomeLayoutDragState(val rows: SnapshotStateList<HomeBlock>, val reorder: LazyListReorder)

@Composable
internal fun rememberHomeLayoutDragState(
    order: List<HomeBlock>,
    listState: LazyListState,
    onReorder: (List<HomeBlock>) -> Unit,
): HomeLayoutDragState {
    val rows = remember(order) { order.toMutableStateList() }
    val reorder = rememberLazyListReorder(
        listState = listState,
        canDropOn = { key -> rows.any { rowKey(it) == key } },
        onMove = { from, to ->
            val i = rows.indexOfFirst { rowKey(it) == from }
            val j = rows.indexOfFirst { rowKey(it) == to }
            if (i >= 0 && j >= 0) rows.add(j, rows.removeAt(i))
        },
        onEnd = { onReorder(rows.toList()) },
    )
    return remember(rows, reorder) { HomeLayoutDragState(rows, reorder) }
}

/**
 * Drag ≡ to move a block, tap the eye to hide it. Emitted as lazy items so the
 * other rows slide out of the way and the host list scrolls at its edges.
 */
internal fun LazyListScope.homeLayoutRows(
    state: HomeLayoutDragState,
    hidden: Set<String>,
    onToggle: (HomeBlock) -> Unit,
) {
    items(state.rows, key = ::rowKey) { block ->
        HomeLayoutRow(block, state.reorder, isHomeBlockHidden(block, hidden), onToggle)
    }
}

@Composable
private fun LazyItemScope.HomeLayoutRow(
    block: HomeBlock,
    reorder: LazyListReorder,
    isHidden: Boolean,
    onToggle: (HomeBlock) -> Unit,
) {
    val isDragging = reorder.draggingKey == rowKey(block)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            // The dragged row follows the finger itself; animating its slot too
            // would move it twice and make it jitter.
            .animateItem(
                placementSpec = if (isDragging) null else spring(
                    stiffness = Spring.StiffnessMediumLow,
                    visibilityThreshold = IntOffset.VisibilityThreshold,
                ),
            )
            .padding(bottom = RowGap)
            .fillMaxWidth()
            .height(RowHeight)
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = if (isDragging) reorder.offset else 0f
                val lift = if (isDragging) 1.02f else 1f
                scaleX = lift
                scaleY = lift
            },
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(block.icon, contentDescription = null, modifier = Modifier.size(20.dp).alpha(if (isHidden) 0.45f else 1f))
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (isHidden) "${block.title} · hidden" else block.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isDragging) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.weight(1f).alpha(if (isHidden) 0.55f else 1f),
            )
            IconButton(onClick = { onToggle(block) }) {
                AnimatedToggleIcon(
                    on = isHidden, onIcon = Icons.Filled.VisibilityOff, offIcon = Icons.Filled.Visibility,
                    contentDescription = if (isHidden) "Show ${block.title}" else "Hide ${block.title}",
                )
            }
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = "Drag ${block.title}",
                modifier = Modifier
                    .size(48.dp)
                    .reorderHandle(reorder, rowKey(block))
                    .padding(12.dp),
            )
        }
    }
}

/** The full rearrange list, opened from a section's hold menu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeLayoutSheet(
    order: List<HomeBlock>,
    hidden: Set<String>,
    onReorder: (List<HomeBlock>) -> Unit,
    onToggle: (HomeBlock) -> Unit,
    onDismiss: () -> Unit,
) {
    val listState = rememberLazyListState()
    val drag = rememberHomeLayoutDragState(order, listState, onReorder)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // Row drags must never pull the sheet down with them.
        sheetGesturesEnabled = false,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        LazyColumn(state = listState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            item(key = "home_layout_header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Edit home", style = sectionTitleStyle(), fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) { Text("Done") }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Greeting and search stay on top",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            homeLayoutRows(drag, hidden, onToggle)
        }
    }
}

/**
 * Hold anywhere on a Home section to move it, hide it, or open the full
 * rearrange sheet. Cards and songs inside keep their own hold menus: their
 * clickables take the press first, so this only fires on the section itself.
 */
@Composable
internal fun HomeBlockHold(
    block: HomeBlock,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (up: Boolean) -> Unit,
    onHide: () -> Unit,
    onRearrange: () -> Unit,
    content: @Composable () -> Unit,
) {
    var pressAt by remember { mutableStateOf<Offset?>(null) }
    var height by remember { mutableIntStateOf(0) }
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    Box(
        Modifier
            .onSizeChanged { height = it.height }
            .pointerInput(block) {
                detectTapGestures(
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        pressAt = it
                    },
                )
            },
    ) {
        // A section emits several siblings (spacer, header, row). A lazy item stacks
        // them vertically, but a bare Box overlays them, which ate every section gap.
        Column(Modifier.fillMaxWidth()) { content() }
        val at = pressAt ?: Offset.Zero
        DropdownMenu(
            expanded = pressAt != null,
            onDismissRequest = { pressAt = null },
            // The menu anchors below the section; lift it back up to the finger.
            offset = with(density) { DpOffset(at.x.toDp(), (at.y - height).toDp()) },
        ) {
            Text(
                block.title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            fun choose(action: () -> Unit) {
                pressAt = null
                action()
            }
            if (canMoveUp) {
                DropdownMenuItem(
                    text = { Text("Move up") },
                    leadingIcon = { Icon(Icons.Filled.ArrowUpward, contentDescription = null) },
                    onClick = { choose { onMove(true) } },
                )
            }
            if (canMoveDown) {
                DropdownMenuItem(
                    text = { Text("Move down") },
                    leadingIcon = { Icon(Icons.Filled.ArrowDownward, contentDescription = null) },
                    onClick = { choose { onMove(false) } },
                )
            }
            DropdownMenuItem(
                text = { Text("Hide") },
                leadingIcon = { Icon(Icons.Filled.VisibilityOff, contentDescription = null) },
                onClick = { choose(onHide) },
            )
            DropdownMenuItem(
                text = { Text("Rearrange home") },
                leadingIcon = { Icon(Icons.Filled.DragHandle, contentDescription = null) },
                onClick = { choose(onRearrange) },
            )
        }
    }
}
