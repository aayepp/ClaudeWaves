package com.lastwave.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lastwave.app.R
import com.lastwave.app.data.local.DEFAULT_HOME_CHART_COUNTRIES
import com.lastwave.app.data.local.SettingsPreferences
import com.lastwave.app.data.music.CHART_COUNTRY_CHOICES
import com.lastwave.app.data.music.ChartTimeWindow
import com.lastwave.app.data.music.chartRegionName
import com.lastwave.app.ui.comic.Card
import com.lastwave.app.ui.comic.TextButton
import com.lastwave.app.ui.common.ExpressiveHeader
import com.lastwave.app.ui.common.GlassHeaderLayout
import com.lastwave.app.ui.common.adaptiveContentWidth
import com.lastwave.app.ui.common.safeDrawingBottomPadding
import com.lastwave.app.ui.common.safeHorizontalContentPadding
import com.lastwave.app.ui.feed.isHomeBlockHidden
import com.lastwave.app.ui.feed.HomeBlock
import com.lastwave.app.ui.feed.homeLayoutRows
import com.lastwave.app.ui.feed.rememberHomeLayoutDragState
import com.lastwave.app.ui.feed.MoodChip
import com.lastwave.app.ui.feed.resolveHomeBlocks
import com.lastwave.app.ui.player.LocalMiniPlayerScrollClearance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The same Home layout that edit mode on Home changes, plus the chart countries. */
@HiltViewModel
class HomeSectionsViewModel @Inject constructor(
    private val settingsPreferences: SettingsPreferences,
) : ViewModel() {
    internal val blocks: StateFlow<List<HomeBlock>> = settingsPreferences.settings
        .map { resolveHomeBlocks(it.homeSectionsOrder) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, HomeBlock.entries)

    val hidden: StateFlow<Set<String>> = settingsPreferences.settings
        .map { it.hiddenHomeSections }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val chartCountries: StateFlow<List<String>> = settingsPreferences.settings
        .map { it.homeChartCountries }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DEFAULT_HOME_CHART_COUNTRIES)

    val chartTimeWindow: StateFlow<ChartTimeWindow> = settingsPreferences.settings
        .map { it.chartTimeWindow }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ChartTimeWindow.VIRAL)

    internal fun reorder(order: List<HomeBlock>) {
        viewModelScope.launch { settingsPreferences.setHomeSectionsOrder(order.map { it.id }) }
    }

    internal fun toggle(block: HomeBlock) {
        viewModelScope.launch { settingsPreferences.toggleHomeSectionHidden(block.id) }
    }

    fun reset() {
        viewModelScope.launch { settingsPreferences.resetUniversalHomeSections() }
    }

    /** Picks or drops [country]; at most three, kept in the order they were picked. */
    fun toggleChartCountry(country: String) {
        val current = chartCountries.value
        val next = when {
            country in current -> current - country
            current.size < MAX_HOME_CHARTS -> current + country
            else -> return
        }
        viewModelScope.launch { settingsPreferences.setHomeChartCountries(next) }
    }

    fun selectChartTimeWindow(window: ChartTimeWindow) {
        viewModelScope.launch { settingsPreferences.setChartTimeWindow(window) }
    }
}

private const val MAX_HOME_CHARTS = 3

@Composable
fun HomeSectionsScreen(
    onBack: () -> Unit,
    viewModel: HomeSectionsViewModel = hiltViewModel(),
) {
    val blocks by viewModel.blocks.collectAsStateWithLifecycle()
    val hidden by viewModel.hidden.collectAsStateWithLifecycle()
    val picked by viewModel.chartCountries.collectAsStateWithLifecycle()
    val chartTimeWindow by viewModel.chartTimeWindow.collectAsStateWithLifecycle()
    val visible = blocks.count { !isHomeBlockHidden(it, hidden) }
    val listState = rememberLazyListState()
    val drag = rememberHomeLayoutDragState(blocks, listState, viewModel::reorder)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        GlassHeaderLayout(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentWidth(maxWidth = 860.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            header = { headerModifier, headerBackdrop ->
                ExpressiveHeader(
                    modifier = headerModifier,
                    backdrop = headerBackdrop,
                    title = stringResource(R.string.settings_home_sections),
                    subtitle = stringResource(R.string.home_sections_visible, visible, blocks.size),
                    onBack = onBack,
                    actions = { TextButton(onClick = viewModel::reset) { Text("Reset") } },
                )
            },
        ) { headerInset ->
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = headerInset + 4.dp,
                    bottom = 32.dp + LocalMiniPlayerScrollClearance.current + safeDrawingBottomPadding(),
                ),
                modifier = Modifier.fillMaxSize().safeHorizontalContentPadding(),
            ) {
                item(key = "hint") {
                    Text(
                        "Drag ≡ to reorder Home, tap the eye to hide a section. Greeting and search stay on top. " +
                            "You can also hold any section on Home to move or hide it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                homeLayoutRows(drag, hidden, onToggle = viewModel::toggle)
                item(key = "home_charts") {
                    Spacer(Modifier.height(16.dp))
                    HomeChartsCard(
                        picked = picked,
                        selectedWindow = chartTimeWindow,
                        onToggle = viewModel::toggleChartCountry,
                        onSelectWindow = viewModel::selectChartTimeWindow,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeChartsCard(
    picked: List<String>,
    selectedWindow: ChartTimeWindow,
    onToggle: (String) -> Unit,
    onSelectWindow: (ChartTimeWindow) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text("Home charts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Pick up to $MAX_HOME_CHARTS · slides follow the order you pick. None keeps the featured playlists.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("Chart timeframe", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChartTimeWindow.entries.forEach { window ->
                    MoodChip(
                        label = window.label,
                        selected = window == selectedWindow,
                        onClick = { onSelectWindow(window) },
                    )
                }
            }
            Text("Regions", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CHART_COUNTRY_CHOICES.forEach { country ->
                    val slot = picked.indexOf(country)
                    MoodChip(
                        label = if (slot >= 0) "${slot + 1} · ${chartRegionName(country)}" else chartRegionName(country),
                        selected = slot >= 0,
                        onClick = { onToggle(country) },
                    )
                }
            }
        }
    }
}
