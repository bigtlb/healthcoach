package com.lbthomas.healthcoach.features.profile.data

import kotlinx.datetime.LocalDate

enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other")
}

enum class ActivityLevel(val displayName: String, val shortName: String, val multiplier: Double) {
    SEDENTARY("Sedentary (desk job, little exercise)", "Sedentary", 1.2),
    LIGHT("Lightly Active (light exercise 1-3 days/wk)", "Light", 1.375),
    MODERATE("Moderately Active (moderate exercise 3-5 days/wk)", "Moderate", 1.55),
    VERY_ACTIVE("Very Active (hard exercise 6-7 days/wk)", "Very", 1.725),
    EXTRA_ACTIVE("Extra Active (very intense daily exercise/physical job)","Extra", 1.9)
}

data class UserProfileData(
    val name: String = "",
    val gender: Gender = Gender.MALE,
    val age: Int? = null,
    val heightMeters: Double? = null,
    val profileWeightKg: Double? = null,
    val activityLevel: ActivityLevel = ActivityLevel.SEDENTARY,
    val targetWeightKg: Double? = null,
    val targetCalorieDelta: Double? = null, // e.g. -500 for deficit, +300 for surplus
    val updatedAt: Long = 0L
)

data class MetabolicProfile(
    val bmr: Double,
    val maintenanceCalories: Double,
    val targetCalories: Double?,
    val currentEffectiveWeightKg: Double
)

sealed interface WeightGoalProjection {
    data class Feasible(val totalDays: Int, val targetDate: LocalDate, val message: String) : WeightGoalProjection
    data class Infeasible(val reason: String) : WeightGoalProjection
    data object Achieved : WeightGoalProjection
    data object Undefined : WeightGoalProjection
}
