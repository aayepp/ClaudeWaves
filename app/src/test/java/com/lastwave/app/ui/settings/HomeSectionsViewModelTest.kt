package com.lastwave.app.ui.settings

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import com.lastwave.app.data.local.MiscSettings
import com.lastwave.app.data.local.SettingsPreferences
import com.lastwave.app.data.music.ChartTimeWindow
import com.lastwave.app.ui.feed.HomeBlock
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE, application = Application::class)
class HomeSectionsViewModelTest {
    private val settings = MutableStateFlow(MiscSettings())
    private val preferences = mockk<SettingsPreferences>(relaxed = true)
    private lateinit var viewModel: HomeSectionsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { preferences.settings } returns settings
        viewModel = HomeSectionsViewModel(preferences)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun savedOrderLeadsAndUnknownIdsFromOlderLayoutsAreDropped() {
        // "spotlight_hero" is an id from the old 45-section list.
        settings.value = MiscSettings(homeSectionsOrder = listOf("recommended", "spotlight_hero", "top_charts"))

        assertThat(viewModel.blocks.value.take(2))
            .containsExactly(HomeBlock.RECOMMENDED, HomeBlock.TOP_CHARTS).inOrder()
        assertThat(viewModel.blocks.value).containsExactlyElementsIn(HomeBlock.entries)
    }

    @Test
    fun reorderAndHideAreSavedByBlockId() {
        val order = listOf(HomeBlock.MADE_FOR_YOU) + (HomeBlock.entries - HomeBlock.MADE_FOR_YOU)
        viewModel.reorder(order)
        viewModel.toggle(HomeBlock.MOOD_CHIPS)

        coVerify { preferences.setHomeSectionsOrder(order.map { it.id }) }
        coVerify { preferences.toggleHomeSectionHidden("mood_chips") }
    }

    @Test
    fun chartCountriesStopAtThreeAndKeepPickOrder() {
        settings.value = MiscSettings(homeChartCountries = listOf("MY", "ID", "ZZ"))

        viewModel.toggleChartCountry("SG")
        coVerify(exactly = 0) { preferences.setHomeChartCountries(any()) }

        viewModel.toggleChartCountry("ID")
        coVerify { preferences.setHomeChartCountries(listOf("MY", "ZZ")) }
    }

    @Test
    fun chartTimeWindowReflectsSettingsAndPersistsSelections() {
        settings.value = MiscSettings(chartTimeWindow = ChartTimeWindow.WEEKLY)
        assertThat(viewModel.chartTimeWindow.value).isEqualTo(ChartTimeWindow.WEEKLY)

        viewModel.selectChartTimeWindow(ChartTimeWindow.MONTHLY)
        coVerify { preferences.setChartTimeWindow(ChartTimeWindow.MONTHLY) }
    }
}
