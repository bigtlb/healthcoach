package com.lbthomas.healthcoach.features.profile

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.profile.data.*
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.settings.data.SettingsStore
import com.lbthomas.healthcoach.features.settings.data.WeightSettings
import com.lbthomas.healthcoach.features.weight.data.WeightRepository
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.*

class ProfileViewModelTest {

    private lateinit var database: Database
    private lateinit var weightRepository: WeightRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var settingsStore: SettingsStore
    private lateinit var tempSettingsFile: File
    private lateinit var viewModel: ProfileViewModel

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        database = Database(driver)
        weightRepository = WeightRepository(database)
        profileRepository = ProfileRepository(database, weightRepository)

        tempSettingsFile = File.createTempFile("test_settings", ".json")
        tempSettingsFile.deleteOnExit()
        settingsStore = SettingsStore(tempSettingsFile)

        viewModel = ProfileViewModel(
            repository = profileRepository,
            settingsStore = settingsStore
        )
    }

    @AfterTest
    fun tearDown() {
        tempSettingsFile.delete()
    }

    @Test
    fun testSaveAndLoadProfileUSUnits() = runBlocking {
        settingsStore.updateSettings { it.copy(weight = WeightSettings(unit = WeightUnit.US)) }

        viewModel.onNameChange("John Doe")
        viewModel.onGenderChange(Gender.MALE)
        viewModel.onAgeChange("30")
        viewModel.onHeightFeetChange("5")
        viewModel.onHeightInchesChange("11") // 71 inches = 1.8034 m
        viewModel.onWeightChange("176.4") // ~80.01 kg
        viewModel.onActivityLevelChange(ActivityLevel.SEDENTARY)
        viewModel.onTargetWeightChange("165.3") // ~74.98 kg
        viewModel.onTargetCalorieDeltaChange("-500")

        val saved = viewModel.saveProfile(WeightUnit.US)
        assertTrue(saved)
        assertNull(viewModel.formState.value.validationError)

        val profileInRepo = profileRepository.getProfile()
        assertEquals("John Doe", profileInRepo.name)
        assertEquals(Gender.MALE, profileInRepo.gender)
        assertEquals(30, profileInRepo.age)
        assertEquals(1.8034, profileInRepo.heightMeters!!, 0.001)
        assertEquals(80.01, profileInRepo.profileWeightKg!!, 0.05)
        assertEquals(ActivityLevel.SEDENTARY, profileInRepo.activityLevel)
        assertEquals(74.98, profileInRepo.targetWeightKg!!, 0.05)
        assertEquals(-500.0, profileInRepo.targetCalorieDelta)

        // Load into form again
        viewModel.loadFromProfile(WeightUnit.US)
        val form = viewModel.formState.value
        assertEquals("John Doe", form.name)
        assertEquals("30", form.ageText)
        assertEquals("5", form.heightFeetText)
        assertEquals("11", form.heightInchesText)
        assertEquals("176.4", form.weightText)
        assertEquals("-500", form.targetCalorieDeltaText)
    }

    @Test
    fun testSaveAndLoadProfileMetricUnits() = runBlocking {
        settingsStore.updateSettings { it.copy(weight = WeightSettings(unit = WeightUnit.METRIC)) }

        viewModel.onNameChange("jane smith")
        viewModel.onGenderChange(Gender.FEMALE)
        viewModel.onAgeChange("25")
        viewModel.onHeightCmChange("165") // 1.65 m
        viewModel.onWeightChange("60") // 60 kg
        viewModel.onActivityLevelChange(ActivityLevel.MODERATE)
        viewModel.onTargetWeightChange("55") // 55 kg
        viewModel.onTargetCalorieDeltaChange("-300")

        val saved = viewModel.saveProfile(WeightUnit.METRIC)
        assertTrue(saved)

        val profileInRepo = profileRepository.getProfile()
        assertEquals("Jane Smith", profileInRepo.name)
        assertEquals(Gender.FEMALE, profileInRepo.gender)
        assertEquals(25, profileInRepo.age)
        assertEquals(1.65, profileInRepo.heightMeters)
        assertEquals(60.0, profileInRepo.profileWeightKg)
        assertEquals(ActivityLevel.MODERATE, profileInRepo.activityLevel)
        assertEquals(55.0, profileInRepo.targetWeightKg)
        assertEquals(-300.0, profileInRepo.targetCalorieDelta)
    }

    @Test
    fun testNameTitleCasedOnSave() {
        viewModel.onNameChange("  alice   in   wonderland  ")
        val saved = viewModel.saveProfile(WeightUnit.US)
        assertTrue(saved)
        val profileInRepo = profileRepository.getProfile()
        assertEquals("Alice In Wonderland", profileInRepo.name)
    }

    @Test
    fun testValidationErrors() {
        // Invalid age
        viewModel.onAgeChange("invalid")
        val savedInvalidAge = viewModel.saveProfile(WeightUnit.US)
        assertFalse(savedInvalidAge)
        assertNotNull(viewModel.formState.value.validationError)

        // Reset and test invalid calorie delta
        viewModel.onAgeChange("30")
        viewModel.onTargetCalorieDeltaChange("not_a_number")
        val savedInvalidDelta = viewModel.saveProfile(WeightUnit.US)
        assertFalse(savedInvalidDelta)
        assertNotNull(viewModel.formState.value.validationError)
    }

    @Test
    fun testLiveFormCalculation() {
        viewModel.onGenderChange(Gender.MALE)
        viewModel.onAgeChange("30")
        viewModel.onHeightFeetChange("5")
        viewModel.onHeightInchesChange("10.866") // ~1.80 m
        viewModel.onWeightChange("176.37") // ~80 kg
        viewModel.onActivityLevelChange(ActivityLevel.SEDENTARY)
        viewModel.onTargetWeightChange("165.35") // ~75 kg
        viewModel.onTargetCalorieDeltaChange("-500")

        // Form BMR, Maintenance, Target calories should be computed synchronously
        val metabolic = viewModel.formState.value.computedMetabolic
        assertNotNull(metabolic)
        assertTrue(metabolic.bmr in 1775.0..1785.0)
        assertTrue(metabolic.maintenanceCalories in 2130.0..2145.0)
        assertTrue(metabolic.targetCalories!! in 1630.0..1645.0)

        val projection = viewModel.formState.value.computedProjection
        assertTrue(projection is WeightGoalProjection.Feasible)
    }

    @Test
    fun testWeeklyRateChangeAndPresets() {
        viewModel.onGenderChange(Gender.MALE)
        viewModel.onAgeChange("30")
        viewModel.onHeightFeetChange("5")
        viewModel.onHeightInchesChange("11")
        viewModel.onWeightChange("180")
        viewModel.onTargetWeightChange("170")

        // Select preset -1.0 lb/week in US units
        viewModel.onPresetRateSelected(-1.0, WeightUnit.US)
        assertEquals("-500", viewModel.formState.value.targetCalorieDeltaText)
        assertEquals(-1.0, viewModel.formState.value.weeklyRate)
        assertEquals(PaceSafetyLevel.STANDARD, viewModel.formState.value.paceSafety?.level)

        // Select preset -0.5 lb/week in US units
        viewModel.onPresetRateSelected(-0.5, WeightUnit.US)
        assertEquals("-250", viewModel.formState.value.targetCalorieDeltaText)
        assertEquals(-0.5, viewModel.formState.value.weeklyRate)
        assertEquals(PaceSafetyLevel.GENTLE, viewModel.formState.value.paceSafety?.level)

        // Direct weekly rate change
        viewModel.onWeeklyRateChange(-1.5, WeightUnit.US)
        assertEquals("-750", viewModel.formState.value.targetCalorieDeltaText)
        assertEquals(-1.5, viewModel.formState.value.weeklyRate)
        assertEquals(PaceSafetyLevel.STANDARD, viewModel.formState.value.paceSafety?.level)
    }

    @Test
    fun testTargetWeeksTimelineCalculation() = runBlocking {
        settingsStore.updateSettings { it.copy(weight = WeightSettings(unit = WeightUnit.METRIC)) }

        viewModel.onGenderChange(Gender.MALE)
        viewModel.onAgeChange("30")
        viewModel.onHeightCmChange("180")
        viewModel.onWeightChange("80") // 80 kg
        viewModel.onTargetWeightChange("75") // 75 kg (5 kg loss)
        viewModel.onGoalInputModeChange(GoalInputMode.TIMELINE)

        // Set 10 weeks -> -5 * 7700 / (10 * 7) = -550 kcal/day
        viewModel.onTargetWeeksChange("10", WeightUnit.METRIC)
        assertEquals("-550", viewModel.formState.value.targetCalorieDeltaText)
        assertEquals(-0.5, viewModel.formState.value.weeklyRate)
        assertTrue(viewModel.formState.value.computedProjection is WeightGoalProjection.Feasible)
    }

    @Test
    fun testDefaultToLatestWeightEntryWhenProfileWeightEmpty() {
        // Add weight entry to weightRepository
        weightRepository.addEntry(
            date = kotlinx.datetime.LocalDate(2026, 9, 25),
            weight = 78.5
        )

        // Ensure profile table has no explicit weight setting
        profileRepository.saveProfile(
            UserProfileData(
                name = "Test User",
                gender = Gender.FEMALE,
                age = 28,
                profileWeightKg = null
            )
        )

        // When loadFromProfile is called in Metric units, it should default to 78.5 kg
        viewModel.loadFromProfile(WeightUnit.METRIC)
        val form = viewModel.formState.value
        assertEquals("78.5", form.weightText)

        // When loadFromProfile is called in US units, it should default to ~173.1 lbs
        viewModel.loadFromProfile(WeightUnit.US)
        val formUs = viewModel.formState.value
        assertEquals("173.1", formUs.weightText)
    }
}
