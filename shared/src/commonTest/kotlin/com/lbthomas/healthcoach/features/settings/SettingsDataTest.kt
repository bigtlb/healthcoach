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
            selectedGraphTimeFrame = GraphTimeFrame.ONE_MONTH,
            themeMode = ThemeMode.DARK
        )

        val serialized = json.encodeToString(SettingsData.serializer(), original)
        val deserialized = json.decodeFromString(SettingsData.serializer(), serialized)

        assertEquals(SelectedPage.GraphsView, deserialized.selectedPage)
        assertEquals(GraphTimeFrame.ONE_MONTH, deserialized.selectedGraphTimeFrame)
        assertEquals(ThemeMode.DARK, deserialized.themeMode)
    }

    @Test
    fun testSettingsViewModelUpdates() {
        val stateFlow = MutableStateFlow(SettingsData())
        val viewModel = SettingsViewModel(settings = stateFlow)

        viewModel.setSelectedPage(SelectedPage.BloodPressureView)
        assertEquals(SelectedPage.BloodPressureView, viewModel.settings.value.selectedPage)

        viewModel.setSelectedGraphTimeFrame(GraphTimeFrame.THREE_MONTHS)
        assertEquals(GraphTimeFrame.THREE_MONTHS, viewModel.settings.value.selectedGraphTimeFrame)

        viewModel.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, viewModel.settings.value.themeMode)
    }
}
