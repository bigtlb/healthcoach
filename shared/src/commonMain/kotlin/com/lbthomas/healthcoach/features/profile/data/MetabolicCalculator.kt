package com.lbthomas.healthcoach.features.profile.data

import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.utils.displayDate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Clock

enum class PaceSafetyLevel {
    MAINTAIN,
    GENTLE,
    STANDARD,
    AGGRESSIVE,
    EXTREME,
    INCOMPATIBLE
}

data class PaceSafetyInfo(
    val level: PaceSafetyLevel,
    val label: String,
    val description: String
)

object MetabolicCalculator {
    const val KCAL_PER_KG_FAT = 7700.0
    const val KCAL_PER_LB_FAT = 3500.0

    /**
     * Converts a daily calorie deficit/surplus to expected weekly weight change.
     * In US (lbs): 3500 kcal / lb -> (delta * 7) / 3500 = delta / 500.
     * In Metric (kg): 7700 kcal / kg -> (delta * 7) / 7700 = delta / 1100.
     */
    fun kcalDeltaToWeeklyWeightChange(deltaKcalPerDay: Double, unit: WeightUnit): Double {
        return if (unit == WeightUnit.US) {
            deltaKcalPerDay / 500.0
        } else {
            deltaKcalPerDay / 1100.0
        }
    }

    /**
     * Converts a desired weekly weight change to daily calorie delta.
     * In US (lbs/week): weeklyChange * 500 kcal/day.
     * In Metric (kg/week): weeklyChange * 1100 kcal/day.
     */
    fun weeklyWeightChangeToKcalDelta(weeklyChange: Double, unit: WeightUnit): Double {
        return if (unit == WeightUnit.US) {
            weeklyChange * 500.0
        } else {
            weeklyChange * 1100.0
        }
    }

    /**
     * Calculates daily calorie delta required to bridge weight difference over a target duration in weeks.
     */
    fun calculateDeltaFromTargetWeeks(
        currentWeightKg: Double,
        targetWeightKg: Double,
        targetWeeks: Double
    ): Double? {
        if (targetWeeks <= 0.0) return null
        val weightDiffKg = targetWeightKg - currentWeightKg
        val totalCalories = weightDiffKg * KCAL_PER_KG_FAT
        val totalDays = targetWeeks * 7.0
        return totalCalories / totalDays
    }

    /**
     * Evaluates pace safety and sustainability based on weekly rate, direction, and weight unit.
     */
    fun evaluatePaceSafety(
        weeklyRate: Double,
        unit: WeightUnit,
        isLoss: Boolean
    ): PaceSafetyInfo {
        val absRate = abs(weeklyRate)
        if (absRate < 0.05) {
            return PaceSafetyInfo(
                level = PaceSafetyLevel.MAINTAIN,
                label = "Maintenance",
                description = "Neutral energy balance to maintain current body weight."
            )
        }

        val rateInLbs = if (unit == WeightUnit.US) absRate else absRate * 2.20462

        return if (isLoss) {
            when {
                rateInLbs <= 0.75 -> PaceSafetyInfo(
                    level = PaceSafetyLevel.GENTLE,
                    label = "Gentle Pace",
                    description = "Highly sustainable with minimal metabolic fatigue and maximum muscle retention."
                )
                rateInLbs <= 1.5 -> PaceSafetyInfo(
                    level = PaceSafetyLevel.STANDARD,
                    label = "Recommended Pace",
                    description = "Standard, effective rate for steady fat loss and consistent habit formation."
                )
                rateInLbs <= 2.2 -> PaceSafetyInfo(
                    level = PaceSafetyLevel.AGGRESSIVE,
                    label = "Aggressive Pace",
                    description = "Faster progress requiring strict adherence; ensure adequate protein intake."
                )
                else -> PaceSafetyInfo(
                    level = PaceSafetyLevel.EXTREME,
                    label = "Very Aggressive",
                    description = "High deficit may cause lethargy and lean tissue loss. Consult a healthcare professional."
                )
            }
        } else {
            when {
                rateInLbs <= 0.5 -> PaceSafetyInfo(
                    level = PaceSafetyLevel.GENTLE,
                    label = "Lean Gain Pace",
                    description = "Optimal lean muscle gain while minimizing unwanted adipose accumulation."
                )
                rateInLbs <= 1.0 -> PaceSafetyInfo(
                    level = PaceSafetyLevel.STANDARD,
                    label = "Standard Gain",
                    description = "Steady surplus supporting progressive strength and athletic training."
                )
                else -> PaceSafetyInfo(
                    level = PaceSafetyLevel.AGGRESSIVE,
                    label = "High Surplus",
                    description = "Higher surplus increases rate of fat gain alongside muscle growth."
                )
            }
        }
    }

    /**
     * Calculates Basal Metabolic Rate (BMR) using the Mifflin-St Jeor formula.
     * Men: BMR = 10 * weight(kg) + 6.25 * height(cm) - 5 * age(years) + 5
     * Women: BMR = 10 * weight(kg) + 6.25 * height(cm) - 5 * age(years) - 161
     * Other: BMR = 10 * weight(kg) + 6.25 * height(cm) - 5 * age(years) - 78 (midpoint between Male and Female)
     */
    fun calculateBmr(
        gender: Gender,
        age: Int,
        heightMeters: Double,
        weightKg: Double
    ): Double {
        val heightCm = heightMeters * 100.0
        val base = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age)
        return when (gender) {
            Gender.MALE -> base + 5.0
            Gender.FEMALE -> base - 161.0
            Gender.OTHER -> base - 78.0
        }
    }

    /**
     * Calculates Total Daily Energy Expenditure (TDEE / maintenance calories)
     * by applying the Physical Activity Level multiplier to BMR.
     */
    fun calculateMaintenanceCalories(
        bmr: Double,
        activityLevel: ActivityLevel
    ): Double {
        return bmr * activityLevel.multiplier
    }

    /**
     * Calculates target daily calories based on maintenance calories and surplus/deficit delta.
     */
    fun calculateTargetCalories(
        maintenanceCalories: Double,
        targetCalorieDelta: Double?
    ): Double? {
        return targetCalorieDelta?.let { maintenanceCalories + it }
    }

    /**
     * Computes full metabolic profile given a UserProfileData and effective current weight.
     */
    fun calculateMetabolicProfile(
        profile: UserProfileData,
        effectiveWeightKg: Double?
    ): MetabolicProfile? {
        val weight = effectiveWeightKg ?: profile.profileWeightKg ?: return null
        val age = profile.age ?: return null
        val height = profile.heightMeters ?: return null
        if (weight <= 0.0 || age <= 0 || height <= 0.0) return null

        val bmr = calculateBmr(
            gender = profile.gender,
            age = age,
            heightMeters = height,
            weightKg = weight
        )
        val maintenance = calculateMaintenanceCalories(bmr, profile.activityLevel)
        val target = calculateTargetCalories(maintenance, profile.targetCalorieDelta)

        return MetabolicProfile(
            bmr = bmr,
            maintenanceCalories = maintenance,
            targetCalories = target,
            currentEffectiveWeightKg = weight
        )
    }

    /**
     * Computes timeline projection to reach target weight based on daily calorie deficit/surplus.
     * Energy equivalence: ~7,700 kcal per 1 kg of body fat.
     */
    fun calculateWeightGoalProjection(
        currentWeightKg: Double?,
        targetWeightKg: Double?,
        targetCalorieDelta: Double?,
        fromDate: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    ): WeightGoalProjection {
        if (currentWeightKg == null || targetWeightKg == null || targetWeightKg <= 0.0 || currentWeightKg <= 0.0) {
            return WeightGoalProjection.Undefined
        }

        val weightDiff = targetWeightKg - currentWeightKg
        if (abs(weightDiff) < 0.05) {
            return WeightGoalProjection.Achieved
        }

        if (targetCalorieDelta == null || targetCalorieDelta == 0.0) {
            return WeightGoalProjection.Undefined
        }

        // Weight loss goal: target < current
        if (weightDiff < 0) {
            if (targetCalorieDelta > 0) {
                return WeightGoalProjection.Infeasible("Target weight cannot be reached with a caloric surplus.")
            }
            val dailyDeficit = abs(targetCalorieDelta)
            val totalCaloriesNeeded = abs(weightDiff) * KCAL_PER_KG_FAT
            val totalDays = ceil(totalCaloriesNeeded / dailyDeficit).toInt()
            val targetDate = LocalDate.fromEpochDays(fromDate.toEpochDays() + totalDays)
            val message = formatProjectionMessage(totalDays, targetDate)
            return WeightGoalProjection.Feasible(totalDays, targetDate, message)
        }

        // Weight gain goal: target > current
        if (weightDiff > 0) {
            if (targetCalorieDelta < 0) {
                return WeightGoalProjection.Infeasible("Target weight cannot be reached with a caloric deficit.")
            }
            val dailySurplus = targetCalorieDelta
            val totalCaloriesNeeded = weightDiff * KCAL_PER_KG_FAT
            val totalDays = ceil(totalCaloriesNeeded / dailySurplus).toInt()
            val targetDate = LocalDate.fromEpochDays(fromDate.toEpochDays() + totalDays)
            val message = formatProjectionMessage(totalDays, targetDate)
            return WeightGoalProjection.Feasible(totalDays, targetDate, message)
        }

        return WeightGoalProjection.Undefined
    }

    private fun formatProjectionMessage(totalDays: Int, targetDate: LocalDate): String {
        val durationText = when {
            totalDays == 1 -> "1 day"
            totalDays < 14 -> "$totalDays days"
            totalDays < 60 -> {
                val weeks = (totalDays / 7.0 * 10).roundToInt() / 10.0
                "~$weeks weeks ($totalDays days)"
            }
            else -> {
                val months = (totalDays / 30.4375 * 10).roundToInt() / 10.0
                "~$months months ($totalDays days)"
            }
        }
        return "$durationText (estimated target: ${targetDate.displayDate()})"
    }
}
