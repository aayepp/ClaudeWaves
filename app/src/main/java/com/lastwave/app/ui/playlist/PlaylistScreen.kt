package com.lastwave.app.ui.playlist

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lastwave.app.R
import com.lastwave.app.data.generate.GeneratedTrack
import com.lastwave.app.data.playlist.LIKED_SONGS_MODE
import com.lastwave.app.data.playlist.LOCAL_SONGS_MODE
import com.lastwave.app.data.playlist.SavedPlaylist
import com.lastwave.app.data.playlist.SpotifyUserPlaylist
import com.lastwave.app.playback.toPlayableTrack
import com.lastwave.app.ui.comic.AlertDialog
import com.lastwave.app.ui.comic.DropdownMenuItem
import com.lastwave.app.ui.comic.IconButton
import com.lastwave.app.ui.comic.Surface
import com.lastwave.app.ui.comic.TextButton
import com.lastwave.app.ui.comic.clipOnly
import com.lastwave.app.ui.theme.ComicInk
import com.lastwave.app.ui.theme.comicPanel
import com.lastwave.app.ui.common.ArtworkImage
import com.lastwave.app.ui.common.ConnectedButtonGroup
import com.lastwave.app.ui.common.ConnectedButtonItem
import com.lastwave.app.ui.common.ExpressiveHeader
import com.lastwave.app.ui.common.GenerationProgressCard
import com.lastwave.app.ui.common.GlassHeaderLayout
import com.lastwave.app.ui.common.PlaylistCover
import com.lastwave.app.ui.common.TrackContextMenuSheet
import com.lastwave.app.ui.common.TrackMenuCapabilities
import com.lastwave.app.ui.common.TrackMenuTarget
import com.lastwave.app.ui.common.adaptiveContentWidth
import com.lastwave.app.ui.settings.toPlayableTrack as downloadedToPlayableTrack
import com.lastwave.app.ui.shell.FloatingNavDefaults
import com.lastwave.app.ui.theme.ExpressivePillShape
import com.lastwave.app.ui.theme.LocalComicTheme
import com.lastwave.app.ui.theme.LocalIsDarkTheme
import com.lastwave.app.ui.theme.LocalLiquidStudioStyle
import com.lastwave.app.ui.theme.glassCardBorder
import com.lastwave.app.ui.theme.glassCardColor

private enum class LibrarySegment(val label: String) {
    ALL("All"),
    PLAYLISTS("Playlists"),
    OFFLINE("Offline"),
    LOCAL("Local"),
}

private sealed interface CoverGridEntry {
    val stableKey: String

    data class Spotify(val playlist: SpotifyUserPlaylist) : CoverGridEntry {
        override val stableKey: String = "sp_${playlist.id}"
    }

    data class Room(val playlist: SavedPlaylist) : CoverGridEntry {
        override val stableKey: String = "pl_${playlist.id}_${playlist.remotePlaylistId ?: "local"}"
    }
}

private val GeneratorPillShape = RoundedCornerShape(50)

/**
 * Segmented Category Hub Library screen (Library 3):
 *  1. Compact header (`Library` + Search toggle + `Generator` pill + `+` create)
 *  2. 4-way segmented bar (`All | Playlists | Offline | Local`)
 *  3. `All`: 2×2 Quick-Vault grid (`Liked Songs`, `Downloads`, `Local Songs`, `Smart Mix`)
 *     + `Playlists` header with sort + frameless 3-col (4-col wide) cover wall
 *  4. `Offline` & `Local`: inline playable track lists with links to full management screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    onOpenPlaylist: (Long) -> Unit = {},
    onOpenFeedPlaylist: (String) -> Unit = {},
    onOpenGenerator: () -> Unit = {},
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadedTracks by viewModel.downloadedTracks.collectAsStateWithLifecycle()
    val downloadedCount by viewModel.downloadedCount.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val musicPlayer = com.lastwave.app.ui.player.LocalMusicPlayer.current
    val playbackState by musicPlayer.listPlaybackState.collectAsStateWithLifecycle()

    var selectedSegment by rememberSaveable { mutableStateOf(LibrarySegment.ALL) }
    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var coverEditorPlaylistId by remember { mutableStateOf<Long?>(null) }
    var coverPickerPlaylistId by remember { mutableStateOf<Long?>(null) }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val playlistId = coverPickerPlaylistId
        if (uri != null && playlistId != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            viewModel.setCustomCover(playlistId, uri.toString())
        }
        coverPickerPlaylistId = null
    }

    // Re-reads from Room whenever this tab regains visibility — this is how
    // a playlist just saved by Generate shows up here without polling.
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose { }
    }

    var menuTarget by remember { mutableStateOf<Pair<Long, GeneratedTrack>?>(null) }
    var quickPeekPlaylist by remember { mutableStateOf<SavedPlaylist?>(null) }
    var selectedPlaylists by remember { mutableStateOf(setOf<Long>()) }
    var showMultipleDeleteConfirm by remember { mutableStateOf(false) }

    val localSongsPlaylist = remember(state.playlists) {
        state.playlists.firstOrNull { it.mode == LOCAL_SONGS_MODE }
    }
    val likedRoomPlaylist = remember(state.playlists) {
        state.playlists.firstOrNull { it.mode == LIKED_SONGS_MODE }
    }
    val spotifyLiked = remember(state.spotifyPlaylists, state.isSpotifySyncEnabled) {
        if (state.isSpotifySyncEnabled) state.spotifyPlaylists.firstOrNull { it.id == "liked" } else null
    }
    val likedCount = maxOf(
        likedRoomPlaylist?.let { it.remoteTrackCount ?: it.tracks.size } ?: 0,
        spotifyLiked?.trackCount ?: 0,
    )

    val filteredPlaylists = remember(state.playlists, searchQuery) {
        state.playlists.filter {
            it.mode != LOCAL_SONGS_MODE && it.matchesPlaylistSearch(searchQuery)
        }
    }
    val effectiveSpotify = remember(state.spotifyPlaylists, state.isSpotifySyncEnabled, searchQuery) {
        if (!state.isSpotifySyncEnabled) emptyList()
        else if (searchQuery.isBlank()) state.spotifyPlaylists
        else state.spotifyPlaylists.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val gridEntries = remember(effectiveSpotify, filteredPlaylists) {
        effectiveSpotify.map(CoverGridEntry::Spotify) + filteredPlaylists.map(CoverGridEntry::Room)
    }

    val filteredDownloads = remember(downloadedTracks, searchQuery) {
        if (searchQuery.isBlank()) downloadedTracks
        else downloadedTracks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredLocalTracks = remember(localSongsPlaylist, searchQuery) {
        val raw = localSongsPlaylist?.tracks.orEmpty()
        if (searchQuery.isBlank()) raw
        else raw.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GlassHeaderLayout(
            Modifier
                .fillMaxSize()
                .adaptiveContentWidth(maxWidth = 860.dp)
                .align(Alignment.TopCenter),
            header = { headerModifier, headerBackdrop ->
                val isDark = LocalIsDarkTheme.current
                val isComic = LocalComicTheme.current
                val pillSurface = when {
                    isComic -> MaterialTheme.colorScheme.surface
                    isDark -> glassCardColor(MaterialTheme.colorScheme.surfaceContainerHigh)
                    else -> glassCardColor(MaterialTheme.colorScheme.surface)
                }
                val pillBorder = if (isComic) {
                    null
                } else {
                    glassCardBorder() ?: BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.42f else 0.55f),
                    )
                }

                ExpressiveHeader(
                    modifier = headerModifier,
                    backdrop = headerBackdrop,
                    title = if (selectedPlaylists.isNotEmpty()) {
                        stringResource(R.string.pl_selected_count, selectedPlaylists.size)
                    } else {
                        stringResource(R.string.pl_title)
                    },
                    subtitle = if (selectedPlaylists.isNotEmpty()) stringResource(R.string.pl_select_more_hint) else null,
                    actions = {
                        if (selectedPlaylists.isNotEmpty()) {
                            IconButton(
                                onClick = { showMultipleDeleteConfirm = true },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.pl_delete_selected_cd), tint = MaterialTheme.colorScheme.error)
                            }
                            IconButton(
                                onClick = { selectedPlaylists = setOf() },
                                modifier = Modifier.size(40.dp),
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.pl_clear_selection_cd))
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    if (searchVisible || searchQuery.isNotBlank()) {
                                        searchQuery = ""
                                        searchVisible = false
                                    } else {
                                        searchVisible = true
                                    }
                                },
                                modifier = Modifier.size(38.dp),
                            ) {
                                Icon(
                                    imageVector = if (searchVisible || searchQuery.isNotBlank()) Icons.Filled.Close else Icons.Filled.Search,
                                    contentDescription = "Search library",
                                )
                            }

                            Surface(
                                onClick = onOpenGenerator,
                                shape = GeneratorPillShape,
                                color = pillSurface,
                                border = pillBorder,
                                modifier = Modifier
                                    .heightIn(min = 36.dp)
                                    .comicPanel(GeneratorPillShape, offset = 2.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "Generator",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }

                            IconButton(
                                onClick = viewModel::openCreateDialog,
                                modifier = Modifier.size(38.dp),
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.pl_create_cd))
                            }
                        }
                    },
                    bottomContent = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AnimatedVisibility(
                                visible = searchVisible || searchQuery.isNotBlank(),
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut(),
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = {
                                        Text(
                                            "Search library...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Filled.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = if (isComic) MaterialTheme.colorScheme.outline else Color.Transparent,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp),
                                )
                            }

                            ConnectedButtonGroup(
                                items = LibrarySegment.entries.map { ConnectedButtonItem(label = it.label) },
                                selectedIndex = selectedSegment.ordinal,
                                onSelect = { index ->
                                    selectedSegment = LibrarySegment.entries.getOrElse(index) { LibrarySegment.ALL }
                                },
                                modifier = Modifier.padding(top = 2.dp, bottom = 6.dp),
                            )
                        }
                    },
                )
            },
        ) { headerInset ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            ) {
                val columns = if (maxWidth >= 600.dp) 4 else 3
                val gridRows = remember(gridEntries, columns) { gridEntries.chunked(columns) }
                val liquidStudio = LocalLiquidStudioStyle.current

                when {
                    state.isLoading && state.playlists.isEmpty() && !state.isGenerating -> {
                        Box(Modifier.padding(top = headerInset)) { LoadingState() }
                    }
                    selectedSegment == LibrarySegment.PLAYLISTS && gridEntries.isEmpty() && !state.isGenerating -> {
                        Box(Modifier.padding(top = headerInset)) { EmptyState() }
                    }
                    else -> LazyColumn(
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = (if (liquidStudio) 8.dp else 14.dp) + headerInset,
                            bottom = FloatingNavDefaults.contentBottomPadding(),
                        ),
                        verticalArrangement = Arrangement.spacedBy(if (liquidStudio) 10.dp else 14.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        when (selectedSegment) {
                            LibrarySegment.ALL, LibrarySegment.PLAYLISTS -> {
                                if (selectedSegment == LibrarySegment.ALL) {
                                    if (liquidStudio && (filteredPlaylists.isNotEmpty() || effectiveSpotify.isNotEmpty())) {
                                        item(key = "liquid_studio_cover_flow", contentType = "liquid_studio_cover_flow") {
                                            LiquidStudioLibraryCoverFlow(
                                                roomPlaylists = filteredPlaylists,
                                                spotifyPlaylists = effectiveSpotify,
                                                onOpenRoom = onOpenPlaylist,
                                                onPlayRoomTrack = { pl, idx ->
                                                    if (pl.tracks.isNotEmpty()) {
                                                        musicPlayer.playQueue(
                                                            pl.tracks.map(GeneratedTrack::toPlayableTrack),
                                                            startIndex = idx.coerceIn(0, pl.tracks.lastIndex),
                                                            sourceLabel = pl.title,
                                                        )
                                                    } else {
                                                        onOpenPlaylist(pl.id)
                                                    }
                                                },
                                                onOpenSpotify = onOpenFeedPlaylist,
                                            )
                                        }
                                    }
                                    item(key = "quick_vault_grid", contentType = "quick_vault_grid") {
                                        LibraryQuickVaultGrid(
                                            likedCount = likedCount,
                                            downloadedCount = downloadedCount,
                                            localCount = localSongsPlaylist?.tracks?.size ?: 0,
                                            onOpenLiked = {
                                                when {
                                                    likedRoomPlaylist != null && (likedRoomPlaylist.tracks.isNotEmpty() || spotifyLiked == null) ->
                                                        onOpenPlaylist(likedRoomPlaylist.id)
                                                    spotifyLiked != null -> onOpenFeedPlaylist("sp_liked")
                                                    else -> selectedSegment = LibrarySegment.PLAYLISTS
                                                }
                                            },
                                            onOpenDownloads = viewModel::openDownloads,
                                            onOpenLocal = {
                                                val localId = localSongsPlaylist?.id
                                                if (localId != null) onOpenPlaylist(localId)
                                                else selectedSegment = LibrarySegment.LOCAL
                                            },
                                            onOpenGenerator = onOpenGenerator,
                                        )
                                    }
                                }

                                if (state.isGenerating) {
                                    item(key = "generationProgress", contentType = "generationProgress") {
                                        GenerationProgressCard(message = state.generatingMessage)
                                    }
                                }

                                if (state.justSavedBannerVisible) {
                                    item(key = "banner") {
                                        LaunchedEffect(state.justSavedBannerVisible) {
                                            kotlinx.coroutines.delay(3000)
                                            viewModel.dismissJustSavedBanner()
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                "Playlist saved!",
                                                modifier = Modifier.padding(14.dp),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                    }
                                }

                                item(key = "playlists_header", contentType = "playlists_header") {
                                    PlaylistsSectionHeader(
                                        sortMode = state.sortMode,
                                        onSortModeChange = viewModel::setSortMode,
                                    )
                                }

                                if (gridRows.isEmpty()) {
                                    item(key = "playlists_empty", contentType = "playlists_empty") {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = stringResource(R.string.pl_empty),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                } else {
                                    itemsIndexed(
                                        items = gridRows,
                                        key = { _, row -> row.joinToString("|") { it.stableKey } },
                                        contentType = { _, _ -> "cover_grid_row" },
                                    ) { _, rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            rowItems.forEach { entry ->
                                                Box(Modifier.weight(1f)) {
                                                    when (entry) {
                                                        is CoverGridEntry.Spotify -> SpotifyCoverGridTile(
                                                            playlist = entry.playlist,
                                                            onClick = {
                                                                val routeId = if (entry.playlist.id == "liked") "sp_liked" else "sp_${entry.playlist.id}"
                                                                onOpenFeedPlaylist(routeId)
                                                            },
                                                        )
                                                        is CoverGridEntry.Room -> {
                                                            val pl = entry.playlist
                                                            PlaylistCoverGridTile(
                                                                playlist = pl,
                                                                isRegenerating = state.regeneratingId == pl.id,
                                                                isPlaying = playbackState.isPlaying,
                                                                playbackSource = playbackState.sourceLabel,
                                                                isSelected = selectedPlaylists.contains(pl.id),
                                                                onClick = {
                                                                    if (selectedPlaylists.isNotEmpty()) {
                                                                        selectedPlaylists = if (selectedPlaylists.contains(pl.id)) {
                                                                            selectedPlaylists - pl.id
                                                                        } else {
                                                                            selectedPlaylists + pl.id
                                                                        }
                                                                    } else {
                                                                        onOpenPlaylist(pl.id)
                                                                    }
                                                                },
                                                                onLongClick = {
                                                                    if (selectedPlaylists.isNotEmpty()) {
                                                                        selectedPlaylists = if (selectedPlaylists.contains(pl.id)) {
                                                                            selectedPlaylists - pl.id
                                                                        } else {
                                                                            selectedPlaylists + pl.id
                                                                        }
                                                                    } else {
                                                                        quickPeekPlaylist = pl
                                                                    }
                                                                },
                                                                onPlay = {
                                                                    musicPlayer.playQueue(
                                                                        pl.tracks.map(GeneratedTrack::toPlayableTrack),
                                                                        startIndex = 0,
                                                                        sourceLabel = pl.title,
                                                                    )
                                                                },
                                                                onTogglePin = { viewModel.togglePinned(pl.id) },
                                                                onDownload = { viewModel.downloadPlaylist(pl.id) },
                                                                onMakeLocal = { viewModel.makeLocal(pl.id) },
                                                                onRegenerate = {
                                                                    viewModel.regenerate(pl.id) { newId ->
                                                                        onOpenPlaylist(newId)
                                                                    }
                                                                },
                                                                onEditCover = { coverEditorPlaylistId = pl.id },
                                                                onRename = { viewModel.requestRename(pl.id) },
                                                                onExport = { viewModel.openExportSheet(pl.id) },
                                                                onDelete = { viewModel.requestDelete(pl.id) },
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            repeat((columns - rowItems.size).coerceAtLeast(0)) {
                                                Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }

                            LibrarySegment.OFFLINE -> {
                                item(key = "offline_header", contentType = "segment_header") {
                                    SegmentActionHeader(
                                        title = "Downloads · ${filteredDownloads.size}",
                                        actionLabel = "Manage",
                                        hasTracks = filteredDownloads.isNotEmpty(),
                                        onPlayAll = {
                                            musicPlayer.playQueue(
                                                tracks = filteredDownloads.map { it.downloadedToPlayableTrack() },
                                                startIndex = 0,
                                                sourceLabel = "Downloads",
                                            )
                                        },
                                        onAction = viewModel::openDownloads,
                                    )
                                }
                                if (filteredDownloads.isEmpty()) {
                                    item(key = "offline_empty", contentType = "segment_empty") {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Text(
                                                text = "No offline downloads yet",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            TextButton(onClick = viewModel::openDownloads) {
                                                Text("Open Downloads")
                                            }
                                        }
                                    }
                                } else {
                                    itemsIndexed(
                                        items = filteredDownloads,
                                        key = { _, item -> "dl_${item.id}" },
                                        contentType = { _, _ -> "inline_track" },
                                    ) { index, track ->
                                        LibraryInlineTrackRow(
                                            title = track.title,
                                            artist = track.artist,
                                            artworkUrl = track.artworkUrl,
                                            trailingBadge = track.formatBadge.takeIf { it.isNotBlank() && it != "AUDIO" },
                                            showDivider = index < filteredDownloads.lastIndex,
                                            onClick = {
                                                musicPlayer.playQueue(
                                                    tracks = filteredDownloads.map { it.downloadedToPlayableTrack() },
                                                    startIndex = index,
                                                    sourceLabel = "Downloads",
                                                )
                                            },
                                        )
                                    }
                                }
                            }

                            LibrarySegment.LOCAL -> {
                                item(key = "local_header", contentType = "segment_header") {
                                    SegmentActionHeader(
                                        title = "Local Songs · ${filteredLocalTracks.size}",
                                        actionLabel = "Browse",
                                        hasTracks = filteredLocalTracks.isNotEmpty(),
                                        onPlayAll = {
                                            musicPlayer.playQueue(
                                                tracks = filteredLocalTracks.map(GeneratedTrack::toPlayableTrack),
                                                startIndex = 0,
                                                sourceLabel = localSongsPlaylist?.title ?: "Local Songs",
                                                enableRadioContinuation = false,
                                            )
                                        },
                                        onAction = {
                                            localSongsPlaylist?.let { onOpenPlaylist(it.id) }
                                        },
                                    )
                                }
                                if (filteredLocalTracks.isEmpty()) {
                                    item(key = "local_empty", contentType = "segment_empty") {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Text(
                                                text = "No local songs found",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            localSongsPlaylist?.let { localPl ->
                                                TextButton(onClick = { onOpenPlaylist(localPl.id) }) {
                                                    Text(stringResource(R.string.local_choose_folder))
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    itemsIndexed(
                                        items = filteredLocalTracks,
                                        key = { idx, item -> "loc_${item.url.ifBlank { "${item.name}_${item.artist}" }}_$idx" },
                                        contentType = { _, _ -> "inline_track" },
                                    ) { index, track ->
                                        LibraryInlineTrackRow(
                                            title = track.name,
                                            artist = track.artist,
                                            artworkUrl = track.artworkUrl,
                                            showDivider = index < filteredLocalTracks.lastIndex,
                                            onClick = {
                                                musicPlayer.playQueue(
                                                    tracks = filteredLocalTracks.map(GeneratedTrack::toPlayableTrack),
                                                    startIndex = index,
                                                    sourceLabel = localSongsPlaylist?.title ?: "Local Songs",
                                                    enableRadioContinuation = false,
                                                )
                                            },
                                            onMenuClick = localSongsPlaylist?.let { localPl ->
                                                { menuTarget = localPl.id to track }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showMultipleDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showMultipleDeleteConfirm = false },
                title = { Text(stringResource(R.string.pl_delete_multi_title)) },
                text = { Text(stringResource(R.string.pl_delete_multi_text, selectedPlaylists.size)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteMultiple(selectedPlaylists)
                            selectedPlaylists = setOf()
                            showMultipleDeleteConfirm = false
                        },
                    ) {
                        Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMultipleDeleteConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
                },
            )
        }

        state.toastMessage?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(3000)
                viewModel.dismissToast()
            }
            Surface(
                shape = ExpressivePillShape,
                color = MaterialTheme.colorScheme.inverseSurface,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = FloatingNavDefaults.contentBottomPadding() + 12.dp),
            ) {
                Text(msg, color = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
            }
        }
    }

    // ── Export bottom sheet (§4.6) ──
    state.exportSheetForPlaylistId?.let { id ->
        ExportBottomSheet(
            onDismiss = viewModel::dismissExportSheet,
            onSaveCsv = { viewModel.exportSave(id, ExportFormat.CSV) },
            onSaveM3u = { viewModel.exportSave(id, ExportFormat.M3U) },
            onShareCsv = { viewModel.exportShare(id, ExportFormat.CSV) },
            onShareM3u = { viewModel.exportShare(id, ExportFormat.M3U) },
        )
    }

    // ── Delete confirm dialog ──
    if (state.deleteConfirmForPlaylistId != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteConfirm,
            title = { Text(stringResource(R.string.pl_delete_single_title)) },
            text = { Text(stringResource(R.string.pl_delete_undone)) },
            confirmButton = { TextButton(onClick = viewModel::confirmDelete) { Text(stringResource(R.string.common_delete)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissDeleteConfirm) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    if (state.createDialogVisible) {
        var title by remember(state.createDialogVisible) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = viewModel::dismissCreateDialog,
            title = { Text(stringResource(R.string.pl_create_title)) },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.pl_name_label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.createCustomPlaylist(title) }, enabled = title.isNotBlank()) { Text(stringResource(R.string.common_create)) }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissCreateDialog) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    state.renamePlaylistId?.let { playlistId ->
        val playlist = state.playlists.firstOrNull { it.id == playlistId }
        var title by remember(playlistId) { mutableStateOf(playlist?.title.orEmpty()) }
        AlertDialog(
            onDismissRequest = viewModel::dismissRename,
            title = { Text(stringResource(R.string.pl_rename_title)) },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.pl_name_label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.renamePlaylist(title) }, enabled = title.isNotBlank()) { Text(stringResource(R.string.common_rename)) }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissRename) { Text(stringResource(R.string.common_cancel)) } },
        )
    }

    coverEditorPlaylistId?.let { playlistId ->
        state.playlists.firstOrNull { it.id == playlistId }?.let { playlist ->
            AlertDialog(
                onDismissRequest = { coverEditorPlaylistId = null },
                title = { Text(stringResource(R.string.pl_cover_title)) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PlaylistCover(playlist = playlist, modifier = Modifier.size(132.dp), cornerRadius = 26.dp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (playlist.customCoverUri.isNullOrBlank()) {
                                stringResource(R.string.pl_cover_auto_desc)
                            } else {
                                stringResource(R.string.pl_cover_custom_desc)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coverPickerPlaylistId = playlist.id
                            coverEditorPlaylistId = null
                            coverPicker.launch(arrayOf("image/*"))
                        },
                    ) {
                        Text(if (playlist.customCoverUri.isNullOrBlank()) stringResource(R.string.pl_choose_image) else stringResource(R.string.pl_change_image))
                    }
                },
                dismissButton = {
                    Row {
                        if (!playlist.customCoverUri.isNullOrBlank()) {
                            TextButton(
                                onClick = {
                                    viewModel.setCustomCover(playlist.id, null)
                                    coverEditorPlaylistId = null
                                },
                            ) { Text(stringResource(R.string.pl_use_automatic)) }
                        }
                        TextButton(onClick = { coverEditorPlaylistId = null }) { Text(stringResource(R.string.common_done)) }
                    }
                },
            )
        }
    }

    if (state.deleteScrobbleAuthRequired) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteScrobbleAuthRequired,
            title = { Text(stringResource(R.string.pl_scrobble_auth_title)) },
            text = { Text(stringResource(R.string.pl_scrobble_auth_text)) },
            confirmButton = { TextButton(onClick = viewModel::dismissDeleteScrobbleAuthRequired) { Text(stringResource(R.string.pl_scrobble_auth_ok)) } },
        )
    }

    // ── Playlist Quick-Peek translucent card ──
    quickPeekPlaylist?.let { peekPl ->
        PlaylistQuickPeekDialog(
            playlist = peekPl,
            onDismiss = { quickPeekPlaylist = null },
            onOpen = {
                quickPeekPlaylist = null
                onOpenPlaylist(peekPl.id)
            },
            onPlayTrack = { trackIdx ->
                quickPeekPlaylist = null
                if (peekPl.tracks.isNotEmpty()) {
                    musicPlayer.playQueue(
                        peekPl.tracks.map(GeneratedTrack::toPlayableTrack),
                        startIndex = trackIdx.coerceIn(0, peekPl.tracks.lastIndex),
                        sourceLabel = peekPl.title,
                    )
                }
            },
            onShuffle = {
                quickPeekPlaylist = null
                if (peekPl.tracks.isNotEmpty()) {
                    musicPlayer.playQueue(
                        peekPl.tracks.map(GeneratedTrack::toPlayableTrack),
                        startIndex = 0,
                        sourceLabel = peekPl.title,
                        startShuffled = true,
                    )
                }
            },
            onSelect = {
                quickPeekPlaylist = null
                selectedPlaylists = selectedPlaylists + peekPl.id
            },
        )
    }

    // ── Shared track context menu (§1.7) ──
    menuTarget?.let { (playlistId, track) ->
        TrackContextMenuSheet(
            target = TrackMenuTarget.Track(track.name, track.artist, track.url),
            capabilities = TrackMenuCapabilities(showCopyActions = true, showDeleteScrobble = true),
            playbackSourceLabel = state.playlists.firstOrNull { it.id == playlistId }?.title ?: "Playlists",
            onDismiss = { menuTarget = null },
            onDeleteScrobble = { name, artist -> viewModel.deleteScrobble(name, artist) },
            onRefreshArtwork = { viewModel.refreshArtwork(track.name, track.artist) },
        )
    }
}

@Composable
private fun PlaylistQuickPeekDialog(
    playlist: SavedPlaylist,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onPlayTrack: (Int) -> Unit,
    onShuffle: () -> Unit,
    onSelect: () -> Unit,
) {
    val comic = LocalComicTheme.current
    val isDark = LocalIsDarkTheme.current
    val scale = remember { Animatable(0.86f) }
    LaunchedEffect(playlist.id) {
        scale.snapTo(0.86f)
        scale.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 440f))
    }
    val previewTracks = remember(playlist.id, playlist.tracks) { playlist.tracks.take(5) }
    val trackCount = playlist.remoteTrackCount ?: playlist.tracks.size
    val cardShape = RoundedCornerShape(26.dp)
    val cardColor = when {
        comic -> MaterialTheme.colorScheme.surface
        isDark -> Color(0xEB151824)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)
    }
    val cardBorder = if (comic) {
        BorderStroke(2.dp, ComicInk)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.30f else 0.48f))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.56f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = cardShape,
                color = cardColor,
                border = cardBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 440.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        alpha = ((scale.value - 0.80f) / 0.20f).coerceIn(0f, 1f)
                    }
                    .comicPanel(cardShape, offset = 4.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        PlaylistCover(
                            playlist = playlist,
                            modifier = Modifier.size(68.dp),
                            cornerRadius = 16.dp,
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            ) {
                                Text(
                                    text = "QUICK PEEK",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                            Text(
                                text = playlist.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "$trackCount tracks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onSelect) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Select playlist",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f))

                    if (previewTracks.isEmpty()) {
                        Text(
                            text = "Open playlist to load its tracks.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            previewTracks.forEachIndexed { index, track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onPlayTrack(index) }
                                        .padding(horizontal = 6.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    ArtworkImage(
                                        name = track.name,
                                        artist = track.artist,
                                        embeddedUrl = track.artworkUrl,
                                        fallbackIcon = Icons.Filled.MusicNote,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = track.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    track.durationMs?.takeIf { it > 0L }?.let { ms ->
                                        val totalSec = ms / 1000L
                                        val mins = totalSec / 60L
                                        val secs = totalSec % 60L
                                        Text(
                                            text = "%d:%02d".format(mins, secs),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = { onPlayTrack(0) },
                            enabled = previewTracks.isNotEmpty(),
                            shape = CircleShape,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Play All", maxLines = 1)
                        }
                        FilledTonalButton(
                            onClick = onShuffle,
                            enabled = previewTracks.isNotEmpty(),
                            shape = CircleShape,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Shuffle", maxLines = 1)
                        }
                        TextButton(onClick = onOpen) {
                            Text("Open")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistsSectionHeader(
    sortMode: PlaylistSortMode,
    onSortModeChange: (PlaylistSortMode) -> Unit,
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val sortLabel = when (sortMode) {
        PlaylistSortMode.DATE_DESC -> "Recent"
        PlaylistSortMode.DATE_ASC -> "Oldest"
        PlaylistSortMode.NAME -> "Name"
        PlaylistSortMode.TRACK_COUNT -> "Tracks"
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.pl_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = 36.dp)
                    .clipOnly(RoundedCornerShape(10.dp))
                    .clickable { sortMenuExpanded = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = sortLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = stringResource(R.string.pl_sort_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            DropdownMenu(
                expanded = sortMenuExpanded,
                onDismissRequest = { sortMenuExpanded = false },
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                SortMenuItem(stringResource(R.string.pl_sort_newest), sortMode == PlaylistSortMode.DATE_DESC) {
                    onSortModeChange(PlaylistSortMode.DATE_DESC); sortMenuExpanded = false
                }
                SortMenuItem(stringResource(R.string.pl_sort_oldest), sortMode == PlaylistSortMode.DATE_ASC) {
                    onSortModeChange(PlaylistSortMode.DATE_ASC); sortMenuExpanded = false
                }
                SortMenuItem(stringResource(R.string.pl_sort_name), sortMode == PlaylistSortMode.NAME) {
                    onSortModeChange(PlaylistSortMode.NAME); sortMenuExpanded = false
                }
                SortMenuItem(stringResource(R.string.pl_sort_track_count), sortMode == PlaylistSortMode.TRACK_COUNT) {
                    onSortModeChange(PlaylistSortMode.TRACK_COUNT); sortMenuExpanded = false
                }
            }
        }
    }
}

@Composable
private fun SortMenuItem(label: String, active: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                label,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            )
        },
        trailingIcon = {
            if (active) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        },
        onClick = onClick,
    )
}

@Composable
private fun SegmentActionHeader(
    title: String,
    actionLabel: String,
    hasTracks: Boolean,
    onPlayAll: () -> Unit,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (hasTracks) {
                Surface(
                    onClick = onPlayAll,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.heightIn(min = 34.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play all",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Play",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = 34.dp)
                    .clipOnly(RoundedCornerShape(10.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private fun SavedPlaylist.matchesPlaylistSearch(query: String): Boolean =
    query.isBlank() ||
        title.contains(query, ignoreCase = true) ||
        tracks.any { t ->
            t.name.contains(query, ignoreCase = true) ||
                t.artist.contains(query, ignoreCase = true)
        }
