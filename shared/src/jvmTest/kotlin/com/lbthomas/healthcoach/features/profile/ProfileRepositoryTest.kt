package com.lbthomas.healthcoach.features.profile

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.features.profile.data.*
import com.lbthomas.healthcoach.features.weight.data.WeightRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.*

class ProfileRepositoryTest {

    private lateinit var database: Database
    private lateinit var weightRepository: WeightRepository
    private lateinit var profileRepository: ProfileRepository

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        database = Database(driver)
        weightRepository = WeightRepository(database)
        profileRepository = ProfileRepository(database, weightRepository)
    }

    @Test
    fun testSaveAndGetProfile() {
        val initialProfile = profileRepository.getProfile()
        assertEquals("", initialProfile.name)
        assertEquals(Gender.MALE, initialProfile.gender)
        assertNull(initialProfile.age)
        assertNull(initialProfile.heightMeters)
        assertNull(initialProfile.profileWeightKg)

        val profileToSave = UserProfileData(
            name = "Alice",
            gender = Gender.FEMALE,
            age = 32,
            heightMeters = 1.68,
            profileWeightKg = 64.5,
            activityLevel = ActivityLevel.MODERATE,
            targetWeightKg = 58.0,
            targetCalorieDelta = -350.0
        )

        profileRepository.saveProfile(profileToSave)

        val loaded = profileRepository.getProfile()
        assertEquals("Alice", loaded.name)
        assertEquals(Gender.FEMALE, loaded.gender)
        assertEquals(32, loaded.age)
        assertEquals(1.68, loaded.heightMeters)
        assertEquals(64.5, loaded.profileWeightKg)
        assertEquals(ActivityLevel.MODERATE, loaded.activityLevel)
        assertEquals(58.0, loaded.targetWeightKg)
        assertEquals(-350.0, loaded.targetCalorieDelta)
    }

    @Test
    fun testObserveProfileFlow() = runBlocking {
        val profileToSave = UserProfileData(
            name = "Bob",
            gender = Gender.MALE,
            age = 45,
            heightMeters = 1.82,
            profileWeightKg = 88.0,
            activityLevel = ActivityLevel.LIGHT,
            targetWeightKg = 80.0,
            targetCalorieDelta = -500.0
        )

        profileRepository.saveProfile(profileToSave)

        val observed = profileRepository.observeProfile().first()
        assertEquals("Bob", observed.name)
        assertEquals(Gender.MALE, observed.gender)
        assertEquals(45, observed.age)
        assertEquals(1.82, observed.heightMeters)
    }

    @Test
    fun testEffectiveWeightResolutionAndAutomaticSync() = runBlocking {
        // Initial state: profile weight is 85 kg, no entries in WeightRepository
        profileRepository.saveProfile(
            UserProfileData(
                name = "Charlie",
                gender = Gender.MALE,
                age = 30,
                heightMeters = 1.80,
                profileWeightKg = 85.0,
                activityLevel = ActivityLevel.SEDENTARY,
                targetWeightKg = 75.0,
                targetCalorieDelta = -500.0
            )
        )

        val initialEffective = profileRepository.observeEffectiveWeight().first()
        assertEquals(85.0, initialEffective)

        val initialMetabolic = profileRepository.observeMetabolicProfile().first()
        assertNotNull(initialMetabolic)
        assertEquals(85.0, initialMetabolic.currentEffectiveWeightKg)

        // Log new weight in Weight tracking (83.0 kg)
        weightRepository.addEntry(
            date = LocalDate(2026, 10, 1),
            weight = 83.0
        )

        // Effective weight should immediately resolve to 83.0 kg
        val updatedEffective = profileRepository.observeEffectiveWeight().first()
        assertEquals(83.0, updatedEffective)

        // Stored profileWeightKg in database should be updated automatically
        val updatedProfile = profileRepository.getProfile()
        assertEquals(83.0, updatedProfile.profileWeightKg)

        // Metabolic profile should recompute with 83.0 kg
        val updatedMetabolic = profileRepository.observeMetabolicProfile().first()
        assertNotNull(updatedMetabolic)
        assertEquals(83.0, updatedMetabolic.currentEffectiveWeightKg)
        // BMR for 83kg, 180cm, 30yo: 10(83) + 6.25(180) - 5(30) + 5 = 830 + 1125 - 150 + 5 = 1810 kcal
        assertEquals(1810.0, updatedMetabolic.bmr)
    }

    @Test
    fun testGoalProjectionFlow() = runBlocking {
        profileRepository.saveProfile(
            UserProfileData(
                name = "Diana",
                gender = Gender.FEMALE,
                age = 28,
                heightMeters = 1.65,
                profileWeightKg = 70.0,
                activityLevel = ActivityLevel.LIGHT,
                targetWeightKg = 60.0,
                targetCalorieDelta = -500.0
            )
        )

        val projection = profileRepository.observeGoalProjection().first()
        assertTrue(projection is WeightGoalProjection.Feasible)
        // 10 kg loss * 7700 / 500 = 154 days
        assertEquals(154, projection.totalDays)
    }
}
