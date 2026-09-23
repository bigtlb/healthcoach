package com.lbthomas.healthcoach.features.settings

import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.SelectedPage
import com.lbthomas.healthcoach.core.enums.ThemeMode
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsDataTest {

    @Test
    fun testDefaultSettingsValues() {
        val settings = SettingsData()

        assertEquals(SelectedPage.WeightView, settings.selectedPage)
        assertEquals(GraphTimeFrame.ALL, settings.selectedGraphTimeFrame)
        assertEquals(GraphTimeFrame.ALL, settings.graphTimeFrame)
        assertEquals(WeightUnit.US, settings.weightUnit)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(true, settings.adaptiveDisplay)
        assertEquals(0.5f, settings.splitterPosition)
        assertEquals(true, settings.showWeightInGraph)
        assertEquals(true, settings.showBloodPressureInGraph)
        assertEquals(false, settings.showPulseInGraph)
    }

    @Test
    fun testSettingsSerialization() {
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        val original = SettingsData(
            selectedPage = SelectedPage.GraphsView,
            selectedGraphTimeFrame = GraphTimeFrame.YEAR_TO_DATE,
            themeMode = ThemeMode.DARK,
            adaptiveDisplay = false,
            splitterPosition = 0.42f,
            showPulseInGraph = true
        )

        val serialized = json.encodeToString(SettingsData.serializer(), original)
        val deserialized = json.decodeFromString(SettingsData.serializer(), serialized)

        assertEquals(SelectedPage.GraphsView, deserialized.selectedPage)
        assertEquals(GraphTimeFrame.YEAR_TO_DATE, deserialized.selectedGraphTimeFrame)
        assertEquals(ThemeMode.DARK, deserialized.themeMode)
        assertEquals(false, deserialized.adaptiveDisplay)
        assertEquals(0.42f, deserialized.splitterPosition)
        assertEquals(true, deserialized.showPulseInGraph)
    }

    @Test
    fun testSettingsViewModelUpdates() {
        val stateFlow = MutableStateFlow(SettingsData())
        val viewModel = SettingsViewModel(settings = stateFlow)

        viewModel.setSelectedPage(SelectedPage.BloodPressureView)
        assertEquals(SelectedPage.BloodPressureView, viewModel.settings.value.selectedPage)

        viewModel.setSelectedGraphTimeFrame(GraphTimeFrame.YEAR_TO_DATE)
        assertEquals(GraphTimeFrame.YEAR_TO_DATE, viewModel.settings.value.selectedGraphTimeFrame)

        viewModel.setSelectedGraphTimeFrame(GraphTimeFrame.THREE_MONTHS)
        assertEquals(GraphTimeFrame.THREE_MONTHS, viewModel.settings.value.selectedGraphTimeFrame)

        viewModel.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, viewModel.settings.value.themeMode)

        viewModel.setAdaptiveDisplay(false)
        assertEquals(false, viewModel.settings.value.adaptiveDisplay)

        viewModel.setSplitterPosition(0.65f)
        assertEquals(0.65f, viewModel.settings.value.splitterPosition)

        viewModel.setShowWeightInGraph(false)
        assertEquals(false, viewModel.settings.value.showWeightInGraph)

        viewModel.setShowBloodPressureInGraph(false)
        assertEquals(false, viewModel.settings.value.showBloodPressureInGraph)

        viewModel.setShowPulseInGraph(true)
        assertEquals(true, viewModel.settings.value.showPulseInGraph)
    }
}
