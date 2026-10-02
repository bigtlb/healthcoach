package com.lbthomas.healthcoach.features.profile.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.utils.currentEpochMillis
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import com.lbthomas.healthcoach.features.weight.data.WeightRepository
import com.lbthomas.healthcoach.profile.data.ProfileSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ProfileRepository(
    private val database: Database,
    private val weightRepository: WeightRepository? = null
) {
    companion object {
        const val KEY_NAME = "name"
        const val KEY_GENDER = "gender"
        const val KEY_AGE = "age"
        const val KEY_HEIGHT_METERS = "heightMeters"
        const val KEY_PROFILE_WEIGHT_KG = "profileWeightKg"
        const val KEY_ACTIVITY_LEVEL = "activityLevel"
        const val KEY_TARGET_WEIGHT_KG = "targetWeightKg"
        const val KEY_TARGET_CALORIE_DELTA = "targetCalorieDelta"
    }

    fun observeProfile(): Flow<UserProfileData> = database
        .profileSettingQueries
        .selectAll()
        .asFlow()
        .mapToList(Dispatchers.IO)
        .map { list -> parseUserProfile(list) }

    fun getProfile(): UserProfileData {
        val list = database.profileSettingQueries.selectAll().executeAsList()
        return parseUserProfile(list)
    }

    fun getEffectiveWeight(): Double? {
        val profile = getProfile()
        if (profile.profileWeightKg != null) return profile.profileWeightKg
        val latest = database.weightEntryQueries.selectAll().executeAsList()
            .maxWithOrNull(compareBy<com.lbthomas.healthcoach.weight.data.WeightEntry> { it.date }.thenBy { it.updated_at })
        return latest?.weight
    }

    fun saveProfile(profile: UserProfileData, updatedAt: Long = currentEpochMillis()) {
        database.transaction {
            database.profileSettingQueries.insertOrUpdate(KEY_NAME, profile.name, updatedAt)
            database.profileSettingQueries.insertOrUpdate(KEY_GENDER, profile.gender.name, updatedAt)
            if (profile.age != null) {
                database.profileSettingQueries.insertOrUpdate(KEY_AGE, profile.age.toString(), updatedAt)
            } else {
                database.profileSettingQueries.delete(KEY_AGE)
            }
            if (profile.heightMeters != null) {
                database.profileSettingQueries.insertOrUpdate(KEY_HEIGHT_METERS, profile.heightMeters.toString(), updatedAt)
            } else {
                database.profileSettingQueries.delete(KEY_HEIGHT_METERS)
            }
            if (profile.profileWeightKg != null) {
                database.profileSettingQueries.insertOrUpdate(KEY_PROFILE_WEIGHT_KG, profile.profileWeightKg.toString(), updatedAt)
            } else {
                database.profileSettingQueries.delete(KEY_PROFILE_WEIGHT_KG)
            }
            database.profileSettingQueries.insertOrUpdate(KEY_ACTIVITY_LEVEL, profile.activityLevel.name, updatedAt)
            if (profile.targetWeightKg != null) {
                database.profileSettingQueries.insertOrUpdate(KEY_TARGET_WEIGHT_KG, profile.targetWeightKg.toString(), updatedAt)
            } else {
                database.profileSettingQueries.delete(KEY_TARGET_WEIGHT_KG)
            }
            if (profile.targetCalorieDelta != null) {
                database.profileSettingQueries.insertOrUpdate(KEY_TARGET_CALORIE_DELTA, profile.targetCalorieDelta.toString(), updatedAt)
            } else {
                database.profileSettingQueries.delete(KEY_TARGET_CALORIE_DELTA)
            }
        }
    }

    fun updateProfileWeight(weightKg: Double, updatedAt: Long = currentEpochMillis()) {
        database.profileSettingQueries.insertOrUpdate(KEY_PROFILE_WEIGHT_KG, weightKg.toString(), updatedAt)
    }

    fun observeEffectiveWeight(): Flow<Double?> {
        val profileFlow = observeProfile()
        val weightFlow = weightRepository?.observeAllEntries()
        return if (weightFlow != null) {
            combine(profileFlow, weightFlow) { profile, entries ->
                resolveEffectiveWeight(profile, entries)
            }
        } else {
            profileFlow.map { it.profileWeightKg }
        }
    }

    fun observeMetabolicProfile(): Flow<MetabolicProfile?> {
        return combine(observeProfile(), observeEffectiveWeight()) { profile, effectiveWeight ->
            MetabolicCalculator.calculateMetabolicProfile(profile, effectiveWeight)
        }
    }

    fun observeGoalProjection(): Flow<WeightGoalProjection> {
        return combine(observeProfile(), observeEffectiveWeight()) { profile, effectiveWeight ->
            MetabolicCalculator.calculateWeightGoalProjection(
                currentWeightKg = effectiveWeight,
                targetWeightKg = profile.targetWeightKg,
                targetCalorieDelta = profile.targetCalorieDelta
            )
        }
    }

    private fun resolveEffectiveWeight(profile: UserProfileData, entries: List<WeightEntryData>): Double? {
        val latest = entries.maxWithOrNull(compareBy<WeightEntryData> { it.date }.thenBy { it.updatedAt })
        return latest?.weight ?: profile.profileWeightKg
    }

    private fun parseUserProfile(settings: List<ProfileSetting>): UserProfileData {
        val map = settings.associateBy { it.key }
        return UserProfileData(
            name = map[KEY_NAME]?.value_ ?: "",
            gender = map[KEY_GENDER]?.value_?.let { runCatching { Gender.valueOf(it) }.getOrNull() } ?: Gender.MALE,
            age = map[KEY_AGE]?.value_?.toIntOrNull(),
            heightMeters = map[KEY_HEIGHT_METERS]?.value_?.toDoubleOrNull(),
            profileWeightKg = map[KEY_PROFILE_WEIGHT_KG]?.value_?.toDoubleOrNull(),
            activityLevel = map[KEY_ACTIVITY_LEVEL]?.value_?.let { runCatching { ActivityLevel.valueOf(it) }.getOrNull() } ?: ActivityLevel.SEDENTARY,
            targetWeightKg = map[KEY_TARGET_WEIGHT_KG]?.value_?.toDoubleOrNull(),
            targetCalorieDelta = map[KEY_TARGET_CALORIE_DELTA]?.value_?.toDoubleOrNull(),
            updatedAt = settings.maxOfOrNull { it.updated_at } ?: 0L
        )
    }
}
