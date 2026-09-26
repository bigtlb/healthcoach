package com.lbthomas.healthcoach.features.settings

import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.SelectedPage
import com.lbthomas.healthcoach.core.enums.ThemeMode
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.theme.AppTheme
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
        assertEquals(AppTheme.DEFAULT, settings.appTheme)
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
            appTheme = AppTheme.TEAL,
            adaptiveDisplay = false,
            splitterPosition = 0.42f,
            showPulseInGraph = true
        )

        val serialized = json.encodeToString(SettingsData.serializer(), original)
        val deserialized = json.decodeFromString(SettingsData.serializer(), serialized)

        assertEquals(SelectedPage.GraphsView, deserialized.selectedPage)
        assertEquals(GraphTimeFrame.YEAR_TO_DATE, deserialized.selectedGraphTimeFrame)
        assertEquals(ThemeMode.DARK, deserialized.themeMode)
        assertEquals(AppTheme.TEAL, deserialized.appTheme)
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

        viewModel.setAppTheme(AppTheme.BLUE)
        assertEquals(AppTheme.BLUE, viewModel.settings.value.appTheme)

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

        viewModel.setLocalSyncPath("/path/to/sync")
        assertEquals("/path/to/sync", viewModel.settings.value.localSyncPath)

        viewModel.setGoogleSession(
            email = "user@gmail.com",
            accessToken = "access123",
            refreshToken = "refresh456",
            tokenStatus = "Active (expires in 60m)",
            expiresAt = 1700000000000L,
            refreshTokenExpiresAt = 1700604800000L
        )
        assertEquals("user@gmail.com", viewModel.settings.value.googleAccountEmail)
        assertEquals("access123", viewModel.settings.value.googleAccessToken)
        assertEquals("refresh456", viewModel.settings.value.googleRefreshToken)
        assertEquals("Active (expires in 60m)", viewModel.settings.value.googleTokenStatus)
        assertEquals(1700000000000L, viewModel.settings.value.googleTokenExpiresAt)
        assertEquals(1700604800000L, viewModel.settings.value.googleRefreshTokenExpiresAt)

        viewModel.clearGoogleSession()
        assertEquals("", viewModel.settings.value.googleAccessToken)
        assertEquals("", viewModel.settings.value.googleRefreshToken)
        assertEquals("", viewModel.settings.value.googleAccountEmail)
        assertEquals("Revoked", viewModel.settings.value.googleTokenStatus)
        assertEquals(null, viewModel.settings.value.googleTokenExpiresAt)
        assertEquals(null, viewModel.settings.value.googleRefreshTokenExpiresAt)
    }
}
