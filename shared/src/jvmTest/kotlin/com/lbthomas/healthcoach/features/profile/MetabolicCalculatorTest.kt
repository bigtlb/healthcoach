package com.lbthomas.healthcoach.features.profile

import com.lbthomas.healthcoach.features.profile.data.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

class MetabolicCalculatorTest {

    @Test
    fun testBmrCalculationForMen() {
        // Male, 30 years old, 180 cm (1.8 m), 80 kg:
        // BMR = 10(80) + 6.25(180) - 5(30) + 5 = 800 + 1125 - 150 + 5 = 1780 kcal
        val bmr = MetabolicCalculator.calculateBmr(
            gender = Gender.MALE,
            age = 30,
            heightMeters = 1.80,
            weightKg = 80.0
        )
        assertEquals(1780.0, bmr)
    }

    @Test
    fun testBmrCalculationForWomen() {
        // Female, 25 years old, 165 cm (1.65 m), 60 kg:
        // BMR = 10(60) + 6.25(165) - 5(25) - 161 = 600 + 1031.25 - 125 - 161 = 1345.25 kcal
        val bmr = MetabolicCalculator.calculateBmr(
            gender = Gender.FEMALE,
            age = 25,
            heightMeters = 1.65,
            weightKg = 60.0
        )
        assertEquals(1345.25, bmr)
    }

    @Test
    fun testBmrCalculationForOther() {
        // Other / Midpoint, 30 years old, 180 cm (1.8 m), 80 kg:
        // Base = 10(80) + 6.25(180) - 5(30) = 800 + 1125 - 150 = 1775
        // BMR = 1775 - 78 = 1697 kcal (exactly the average of Male (1780) and Female (1614))
        val bmr = MetabolicCalculator.calculateBmr(
            gender = Gender.OTHER,
            age = 30,
            heightMeters = 1.80,
            weightKg = 80.0
        )
        assertEquals(1697.0, bmr)
    }

    @Test
    fun testMaintenanceCaloriesMultipliers() {
        val bmr = 1780.0
        assertEquals(2136.0, MetabolicCalculator.calculateMaintenanceCalories(bmr, ActivityLevel.SEDENTARY)) // 1.2
        assertEquals(2447.5, MetabolicCalculator.calculateMaintenanceCalories(bmr, ActivityLevel.LIGHT)) // 1.375
        assertEquals(2759.0, MetabolicCalculator.calculateMaintenanceCalories(bmr, ActivityLevel.MODERATE)) // 1.55
        assertEquals(3070.5, MetabolicCalculator.calculateMaintenanceCalories(bmr, ActivityLevel.VERY_ACTIVE)) // 1.725
        assertEquals(3382.0, MetabolicCalculator.calculateMaintenanceCalories(bmr, ActivityLevel.EXTRA_ACTIVE)) // 1.9
    }

    @Test
    fun testTargetCalories() {
        val maintenance = 2136.0
        assertEquals(1636.0, MetabolicCalculator.calculateTargetCalories(maintenance, -500.0))
        assertEquals(2436.0, MetabolicCalculator.calculateTargetCalories(maintenance, 300.0))
        assertNull(MetabolicCalculator.calculateTargetCalories(maintenance, null))
    }

    @Test
    fun testCalculateMetabolicProfile() {
        val profile = UserProfileData(
            name = "John",
            gender = Gender.MALE,
            age = 30,
            heightMeters = 1.80,
            profileWeightKg = 85.0,
            activityLevel = ActivityLevel.SEDENTARY,
            targetCalorieDelta = -500.0
        )

        // With effective weight 80.0 kg overriding profile weight
        val metabolic = MetabolicCalculator.calculateMetabolicProfile(profile, effectiveWeightKg = 80.0)
        assertNotNull(metabolic)
        assertEquals(1780.0, metabolic.bmr)
        assertEquals(2136.0, metabolic.maintenanceCalories)
        assertEquals(1636.0, metabolic.targetCalories)
        assertEquals(80.0, metabolic.currentEffectiveWeightKg)

        // Missing age -> null
        assertNull(MetabolicCalculator.calculateMetabolicProfile(profile.copy(age = null), 80.0))
        // Missing height -> null
        assertNull(MetabolicCalculator.calculateMetabolicProfile(profile.copy(heightMeters = null), 80.0))
        // Missing weight -> null
        assertNull(MetabolicCalculator.calculateMetabolicProfile(profile.copy(profileWeightKg = null), null))
    }

    @Test
    fun testGoalProjectionWeightLossFeasible() {
        // Current: 85 kg, Target: 75 kg (10 kg loss), Deficit: -500 kcal/day
        // Days = (10 * 7700) / 500 = 154 days
        val fromDate = LocalDate(2026, 10, 1)
        val projection = MetabolicCalculator.calculateWeightGoalProjection(
            currentWeightKg = 85.0,
            targetWeightKg = 75.0,
            targetCalorieDelta = -500.0,
            fromDate = fromDate
        )

        assertTrue(projection is WeightGoalProjection.Feasible)
        assertEquals(154, projection.totalDays)
        assertEquals(LocalDate.fromEpochDays(fromDate.toEpochDays() + 154), projection.targetDate)
        assertEquals("~5.1 months (154 days)", projection.message)
    }

    @Test
    fun testGoalProjectionWeightLossInfeasibleSurplus() {
        // Current: 85 kg, Target: 75 kg (weight loss), but Surplus: +300 kcal/day
        val projection = MetabolicCalculator.calculateWeightGoalProjection(
            currentWeightKg = 85.0,
            targetWeightKg = 75.0,
            targetCalorieDelta = 300.0
        )

        assertTrue(projection is WeightGoalProjection.Infeasible)
        assertTrue(projection.reason.contains("surplus"))
    }

    @Test
    fun testGoalProjectionWeightGainFeasible() {
        // Current: 70 kg, Target: 75 kg (5 kg gain), Surplus: +500 kcal/day
        // Days = (5 * 7700) / 500 = 77 days
        val fromDate = LocalDate(2026, 10, 1)
        val projection = MetabolicCalculator.calculateWeightGoalProjection(
            currentWeightKg = 70.0,
            targetWeightKg = 75.0,
            targetCalorieDelta = 500.0,
            fromDate = fromDate
        )

        assertTrue(projection is WeightGoalProjection.Feasible)
        assertEquals(77, projection.totalDays)
        assertEquals(LocalDate.fromEpochDays(fromDate.toEpochDays() + 77), projection.targetDate)
        assertEquals("~2.5 months (77 days)", projection.message)
    }

    @Test
    fun testGoalProjectionDurationFormatting() {
        val fromDate = LocalDate(2026, 10, 1)

        // 1 day: 0.1 kg loss @ 770 kcal/day deficit = (0.1 * 7700) / 770 = 1 day
        val proj1Day = MetabolicCalculator.calculateWeightGoalProjection(
            currentWeightKg = 80.0,
            targetWeightKg = 79.9,
            targetCalorieDelta = -770.0,
            fromDate = fromDate
        )
        assertTrue(proj1Day is WeightGoalProjection.Feasible)
        assertEquals(1, proj1Day.totalDays)
        assertEquals("1 day", proj1Day.message)

        // Days < 14: e.g. 7 days (0.5 kg loss @ 550 kcal/day deficit = 7 days)
        val proj7Days = MetabolicCalculator.calculateWeightGoalProjection(
            currentWeightKg = 80.0,
            targetWeightKg = 79.5,
            targetCalorieDelta = -550.0,
            fromDate = fromDate
        )
        assertTrue(proj7Days is WeightGoalProjection.Feasible)
        assertEquals(7, proj7Days.totalDays)
        assertEquals("7 days", proj7Days.message)

        // Days < 60: e.g. 28 days (4 weeks) (2 kg loss @ 550 kcal/day deficit = 28 days)
        val proj28Days = MetabolicCalculator.calculateWeightGoalProjection(
            currentWeightKg = 80.0,
            targetWeightKg = 78.0,
            targetCalorieDelta = -550.0,
            fromDate = fromDate
        )
        assertTrue(proj28Days is WeightGoalProjection.Feasible)
        assertEquals(28, proj28Days.totalDays)
        assertEquals("~4 weeks (28 days)", proj28Days.message)
    }

    @Test
    fun testGoalProjectionWeightGainInfeasibleDeficit() {
        // Current: 70 kg, Target: 80 kg (gain), but Deficit: -400 kcal/day
        val projection = MetabolicCalculator.calculateWeightGoalProjection(
            currentWeightKg = 70.0,
            targetWeightKg = 80.0,
            targetCalorieDelta = -400.0
        )

        assertTrue(projection is WeightGoalProjection.Infeasible)
        assertTrue(projection.reason.contains("deficit"))
    }

    @Test
    fun testGoalProjectionAchieved() {
        val projection = MetabolicCalculator.calculateWeightGoalProjection(
            currentWeightKg = 75.0,
            targetWeightKg = 75.02,
            targetCalorieDelta = -500.0
        )
        assertEquals(WeightGoalProjection.Achieved, projection)
    }

    @Test
    fun testGoalProjectionUndefined() {
        assertEquals(
            WeightGoalProjection.Undefined,
            MetabolicCalculator.calculateWeightGoalProjection(null, 75.0, -500.0)
        )
        assertEquals(
            WeightGoalProjection.Undefined,
            MetabolicCalculator.calculateWeightGoalProjection(85.0, null, -500.0)
        )
        assertEquals(
            WeightGoalProjection.Undefined,
            MetabolicCalculator.calculateWeightGoalProjection(85.0, 75.0, null)
        )
        assertEquals(
            WeightGoalProjection.Undefined,
            MetabolicCalculator.calculateWeightGoalProjection(85.0, 75.0, 0.0)
        )
    }

    @Test
    fun testWeeklyWeightChangeAndKcalDeltaConversions() {
        // US: 1 lb = 3500 kcal, so 1 lb/wk = 500 kcal/day
        assertEquals(500.0, MetabolicCalculator.weeklyWeightChangeToKcalDelta(1.0, com.lbthomas.healthcoach.core.enums.WeightUnit.US))
        assertEquals(-250.0, MetabolicCalculator.weeklyWeightChangeToKcalDelta(-0.5, com.lbthomas.healthcoach.core.enums.WeightUnit.US))
        assertEquals(-1.0, MetabolicCalculator.kcalDeltaToWeeklyWeightChange(-500.0, com.lbthomas.healthcoach.core.enums.WeightUnit.US))
        assertEquals(0.5, MetabolicCalculator.kcalDeltaToWeeklyWeightChange(250.0, com.lbthomas.healthcoach.core.enums.WeightUnit.US))

        // Metric: 1 kg = 7700 kcal, so 1 kg/wk = 1100 kcal/day
        assertEquals(1100.0, MetabolicCalculator.weeklyWeightChangeToKcalDelta(1.0, com.lbthomas.healthcoach.core.enums.WeightUnit.METRIC))
        assertEquals(-550.0, MetabolicCalculator.weeklyWeightChangeToKcalDelta(-0.5, com.lbthomas.healthcoach.core.enums.WeightUnit.METRIC))
        assertEquals(-0.5, MetabolicCalculator.kcalDeltaToWeeklyWeightChange(-550.0, com.lbthomas.healthcoach.core.enums.WeightUnit.METRIC))
    }

    @Test
    fun testCalculateDeltaFromTargetWeeks() {
        // 80 kg to 75 kg (5 kg loss) over 10 weeks -> -5 * 7700 / (10 * 7) = -38500 / 70 = -550 kcal/day
        val delta = MetabolicCalculator.calculateDeltaFromTargetWeeks(
            currentWeightKg = 80.0,
            targetWeightKg = 75.0,
            targetWeeks = 10.0
        )
        assertEquals(-550.0, delta)

        // 70 kg to 74 kg (4 kg gain) over 8 weeks -> +4 * 7700 / (8 * 7) = +30800 / 56 = +550 kcal/day
        val gainDelta = MetabolicCalculator.calculateDeltaFromTargetWeeks(
            currentWeightKg = 70.0,
            targetWeightKg = 74.0,
            targetWeeks = 8.0
        )
        assertEquals(550.0, gainDelta)

        // Invalid weeks -> null
        assertNull(MetabolicCalculator.calculateDeltaFromTargetWeeks(80.0, 75.0, 0.0))
        assertNull(MetabolicCalculator.calculateDeltaFromTargetWeeks(80.0, 75.0, -2.0))
    }

    @Test
    fun testEvaluatePaceSafety() {
        val us = com.lbthomas.healthcoach.core.enums.WeightUnit.US

        // Loss
        assertEquals(PaceSafetyLevel.MAINTAIN, MetabolicCalculator.evaluatePaceSafety(0.0, us, isLoss = true).level)
        assertEquals(PaceSafetyLevel.GENTLE, MetabolicCalculator.evaluatePaceSafety(-0.5, us, isLoss = true).level)
        assertEquals(PaceSafetyLevel.STANDARD, MetabolicCalculator.evaluatePaceSafety(-1.0, us, isLoss = true).level)
        assertEquals(PaceSafetyLevel.AGGRESSIVE, MetabolicCalculator.evaluatePaceSafety(-2.0, us, isLoss = true).level)
        assertEquals(PaceSafetyLevel.EXTREME, MetabolicCalculator.evaluatePaceSafety(-2.6, us, isLoss = true).level)

        // Gain
        assertEquals(PaceSafetyLevel.GENTLE, MetabolicCalculator.evaluatePaceSafety(0.25, us, isLoss = false).level)
        assertEquals(PaceSafetyLevel.STANDARD, MetabolicCalculator.evaluatePaceSafety(0.75, us, isLoss = false).level)
        assertEquals(PaceSafetyLevel.AGGRESSIVE, MetabolicCalculator.evaluatePaceSafety(1.5, us, isLoss = false).level)
    }
}
