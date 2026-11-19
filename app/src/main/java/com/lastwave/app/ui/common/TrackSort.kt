package com.lastwave.app.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lastwave.app.R
import com.lastwave.app.ui.comic.DropdownMenuItem
import com.lastwave.app.ui.comic.Surface
import com.lastwave.app.ui.playlist.PlaylistTrackSort
import java.text.Collator

/** Sorts that make sense for a list the user doesn't own: it has no "date added". */
internal val ReadOnlyTrackSorts = listOf(
    PlaylistTrackSort.CUSTOM,
    PlaylistTrackSort.NAME,
    PlaylistTrackSort.ARTIST,
    PlaylistTrackSort.PLAY_TIME,
)

/**
 * The list in display order. CUSTOM and DATE_ADDED keep the source order
 * (reversed when descending); LATEST_LIKED sorts newest [addedAtMillis] first
 * (descending) while falling back to DATE_ADDED order for untimestamped entries;
 * songs with no known length always sort last.
 */
internal fun <T> List<T>.sortedForDisplay(
    sort: PlaylistTrackSort,
    ascending: Boolean,
    title: (T) -> String,
    artist: (T) -> String,
    durationMs: (T) -> Long?,
    newestFirst: Boolean = false,
    addedAtMillis: (T) -> Long? = { null },
): List<T> {
    // Locale collation so "Émile" sorts with "E", like YouTube Music.
    val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
    fun by(text: (T) -> String): Comparator<T> = Comparator { a, b -> collator.compare(text(a), text(b)) }
    fun Comparator<T>.directed() = if (ascending) this else reversed()
    return when (sort) {
        PlaylistTrackSort.CUSTOM -> if (ascending) this else reversed()
        PlaylistTrackSort.LATEST_LIKED -> {
            val dateAscending = if (newestFirst) !ascending else ascending
            val hasAnyTimestamp = any { addedAtMillis(it) != null }
            if (!hasAnyTimestamp) {
                return if (dateAscending) this else reversed()
            }
            val legacy = withIndex()
                .filter { addedAtMillis(it.value) == null }
                .let { if (dateAscending) it else it.reversed() }
                .map { it.value }
            val timestamped = withIndex()
                .filter { addedAtMillis(it.value) != null }
                .sortedWith(
                    if (ascending) {
                        compareBy<IndexedValue<T>> { addedAtMillis(it.value)!! }.thenBy { it.index }
                    } else {
                        compareByDescending<IndexedValue<T>> { addedAtMillis(it.value)!! }.thenBy { it.index }
                    },
                )
                .map { it.value }
            if (ascending) legacy + timestamped else timestamped + legacy
        }
        PlaylistTrackSort.DATE_ADDED -> {
            // DATE_ADDED ascending means oldest-first; invert when the source arrives newest-first.
            val effectiveAscending = if (newestFirst) !ascending else ascending
            if (effectiveAscending) this else reversed()
        }
        PlaylistTrackSort.NAME -> sortedWith(by(title).directed())
        PlaylistTrackSort.ARTIST -> sortedWith(by(artist).directed())
        PlaylistTrackSort.PLAY_TIME -> sortedWith(
            compareBy<T> { durationMs(it) == null }.then(compareBy<T> { durationMs(it) }.directed()),
        )
    }
}

/**
 * The sort pill and its menu. Picking the current sort again flips its
 * direction; a new sort starts ascending, except date added and latest liked (newest first).
 */
@Composable
internal fun TrackSortMenu(
    sort: PlaylistTrackSort,
    ascending: Boolean,
    onChange: (PlaylistTrackSort, Boolean) -> Unit,
    options: List<PlaylistTrackSort> = ReadOnlyTrackSorts,
    @StringRes customLabel: Int = R.string.sort_original,
) {
    val haptic = LocalHapticFeedback.current
    var open by remember { mutableStateOf(false) }
    fun label(option: PlaylistTrackSort) = if (option == PlaylistTrackSort.CUSTOM) customLabel else option.labelRes
    Box {
        Surface(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                open = true
            },
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            tonalElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(label(sort)),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                AnimatedToggleIcon(
                    on = ascending, onIcon = Icons.Filled.ArrowUpward, offIcon = Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            modifier = Modifier.widthIn(min = 210.dp),
        ) {
            options.forEach { option ->
                val isSelected = sort == option
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(label(option)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.width(16.dp))
                            Icon(
                                imageVector = when {
                                    isSelected && !ascending -> Icons.Filled.ArrowDownward
                                    isSelected -> Icons.Filled.ArrowUpward
                                    else -> Icons.Filled.SwapVert
                                },
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        val defaultsDescending =
                            option == PlaylistTrackSort.DATE_ADDED || option == PlaylistTrackSort.LATEST_LIKED
                        onChange(option, if (isSelected) !ascending else !defaultsDescending)
                        open = false
                    },
                )
            }
        }
    }
}
