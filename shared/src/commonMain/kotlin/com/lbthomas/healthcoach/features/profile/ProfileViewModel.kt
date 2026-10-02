package com.lbthomas.healthcoach.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.utils.toTitleCase
import com.lbthomas.healthcoach.features.profile.data.*
import com.lbthomas.healthcoach.features.settings.data.SettingsStore
import kotlinx.coroutines.flow.*
import kotlin.math.abs
import kotlin.math.roundToInt

enum class GoalInputMode(val displayName: String) {
    RATE("Pace"),
    TIMELINE("Target Date"),
    MANUAL("Calories")
}

data class ProfileFormState(
    val name: String = "",
    val gender: Gender = Gender.MALE,
    val ageText: String = "",
    val heightFeetText: String = "",
    val heightInchesText: String = "",
    val heightCmText: String = "",
    val weightText: String = "",
    val activityLevel: ActivityLevel = ActivityLevel.SEDENTARY,
    val targetWeightText: String = "",
    val targetCalorieDeltaText: String = "",
    val goalInputMode: GoalInputMode = GoalInputMode.RATE,
    val targetWeeksText: String = "",
    val weeklyRate: Double? = null,
    val paceSafety: PaceSafetyInfo? = null,
    val validationError: String? = null,
    val computedMetabolic: MetabolicProfile? = null,
    val computedProjection: WeightGoalProjection = WeightGoalProjection.Undefined
)

open class ProfileViewModel : ViewModel {
    val repository: ProfileRepository?
    val settingsStore: SettingsStore?

    val profile: StateFlow<UserProfileData>
    val effectiveWeightKg: StateFlow<Double?>
    val metabolicProfile: StateFlow<MetabolicProfile?>
    val goalProjection: StateFlow<WeightGoalProjection>

    private val _formState = MutableStateFlow(ProfileFormState())
    val formState: StateFlow<ProfileFormState> = _formState.asStateFlow()

    // Live preview values computed from formState
    val formBmr: StateFlow<Double?>
    val formMaintenanceCalories: StateFlow<Double?>
    val formTargetCalories: StateFlow<Double?>
    val formGoalProjection: StateFlow<WeightGoalProjection>

    constructor(
        repository: ProfileRepository,
        settingsStore: SettingsStore? = null
    ) : super() {
        this.repository = repository
        this.settingsStore = settingsStore

        profile = repository
            .observeProfile()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfileData())

        effectiveWeightKg = repository
            .observeEffectiveWeight()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        metabolicProfile = repository
            .observeMetabolicProfile()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        goalProjection = repository
            .observeGoalProjection()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightGoalProjection.Undefined)

        formBmr = _formState
            .map { it.computedMetabolic?.bmr }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        formMaintenanceCalories = _formState
            .map { it.computedMetabolic?.maintenanceCalories }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        formTargetCalories = _formState
            .map { it.computedMetabolic?.targetCalories }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        formGoalProjection = _formState
            .map { it.computedProjection }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightGoalProjection.Undefined)
    }

    // Preview constructor
    constructor(
        previewProfile: UserProfileData = UserProfileData(),
        previewEffectiveWeightKg: Double? = null,
        previewMetabolicProfile: MetabolicProfile? = null,
        previewGoalProjection: WeightGoalProjection = WeightGoalProjection.Undefined,
        previewFormState: ProfileFormState = ProfileFormState()
    ) : super() {
        this.repository = null
        this.settingsStore = null
        profile = MutableStateFlow(previewProfile).asStateFlow()
        effectiveWeightKg = MutableStateFlow(previewEffectiveWeightKg).asStateFlow()
        metabolicProfile = MutableStateFlow(previewMetabolicProfile).asStateFlow()
        goalProjection = MutableStateFlow(previewGoalProjection).asStateFlow()
        _formState.value = previewFormState

        formBmr = MutableStateFlow(previewMetabolicProfile?.bmr).asStateFlow()
        formMaintenanceCalories = MutableStateFlow(previewMetabolicProfile?.maintenanceCalories).asStateFlow()
        formTargetCalories = MutableStateFlow(previewMetabolicProfile?.targetCalories).asStateFlow()
        formGoalProjection = MutableStateFlow(previewGoalProjection).asStateFlow()
    }

    private fun updateForm(transform: (ProfileFormState) -> ProfileFormState) {
        _formState.update { current ->
            val modified = transform(current)
            recomputeFormState(modified, currentWeightUnit())
        }
    }

    fun onNameChange(value: String) {
        updateForm { it.copy(name = value, validationError = null) }
    }

    fun onGenderChange(value: Gender) {
        updateForm { it.copy(gender = value) }
    }

    fun onAgeChange(value: String) {
        updateForm { it.copy(ageText = value, validationError = null) }
    }

    fun onHeightFeetChange(value: String) {
        updateForm { it.copy(heightFeetText = value, validationError = null) }
    }

    fun onHeightInchesChange(value: String) {
        updateForm { it.copy(heightInchesText = value, validationError = null) }
    }

    fun onHeightCmChange(value: String) {
        updateForm { it.copy(heightCmText = value, validationError = null) }
    }

    fun onWeightChange(value: String) {
        updateForm { it.copy(weightText = value, validationError = null) }
    }

    fun onActivityLevelChange(value: ActivityLevel) {
        updateForm { it.copy(activityLevel = value) }
    }

    fun onTargetWeightChange(value: String) {
        updateForm { it.copy(targetWeightText = value, validationError = null) }
    }

    fun onGoalInputModeChange(mode: GoalInputMode) {
        updateForm { it.copy(goalInputMode = mode, validationError = null) }
    }

    fun onWeeklyRateChange(rate: Double, unit: WeightUnit = currentWeightUnit()) {
        val roundedRate = (rate * 100.0).roundToInt() / 100.0
        val deltaKcal = MetabolicCalculator.weeklyWeightChangeToKcalDelta(roundedRate, unit)
        val deltaStr = deltaKcal.roundToInt().toString()
        updateForm {
            it.copy(
                weeklyRate = roundedRate,
                targetCalorieDeltaText = deltaStr,
                validationError = null
            )
        }
    }

    fun onPresetRateSelected(rate: Double, unit: WeightUnit = currentWeightUnit()) {
        onWeeklyRateChange(rate, unit)
    }

    fun onTargetWeeksChange(weeksText: String, unit: WeightUnit = currentWeightUnit()) {
        val weeks = weeksText.trim().toDoubleOrNull()
        if (weeks != null && weeks > 0) {
            val currentKg = parseWeightKg(_formState.value.weightText, unit)
            val targetKg = parseWeightKg(_formState.value.targetWeightText, unit)
            if (currentKg != null && targetKg != null) {
                val delta = MetabolicCalculator.calculateDeltaFromTargetWeeks(currentKg, targetKg, weeks)
                if (delta != null) {
                    val deltaStr = delta.roundToInt().toString()
                    updateForm {
                        it.copy(
                            targetWeeksText = weeksText,
                            targetCalorieDeltaText = deltaStr,
                            validationError = null
                        )
                    }
                    return
                }
            }
        }
        updateForm {
            it.copy(
                targetWeeksText = weeksText,
                validationError = null
            )
        }
    }

    fun onTargetCalorieDeltaChange(value: String) {
        updateForm { it.copy(targetCalorieDeltaText = value, validationError = null) }
    }

    private fun parseWeightKg(text: String, unit: WeightUnit): Double? {
        val entered = text.trim().toDoubleOrNull() ?: return null
        if (entered <= 0 || entered > 1000) return null
        return if (unit == WeightUnit.METRIC) entered else entered / 2.20462
    }

    fun loadFromProfile(unit: WeightUnit = currentWeightUnit()) {
        val currentProfile = repository?.getProfile() ?: profile.value
        val effWeight = currentProfile.profileWeightKg ?: repository?.getEffectiveWeight() ?: effectiveWeightKg.value

        val heightFeet: String
        val heightInches: String
        val heightCm: String
        if (currentProfile.heightMeters != null) {
            val totalInches = (currentProfile.heightMeters / 0.0254 * 10).roundToInt() / 10.0
            val feet = (totalInches / 12).toInt()
            val inches = ((totalInches - (feet * 12)) * 10).roundToInt() / 10.0
            heightFeet = feet.toString()
            heightInches = if (inches % 1.0 == 0.0) inches.toInt().toString() else inches.toString()

            val cm = (currentProfile.heightMeters * 100.0 * 10).roundToInt() / 10.0
            heightCm = if (cm % 1.0 == 0.0) cm.toInt().toString() else cm.toString()
        } else {
            heightFeet = ""
            heightInches = ""
            heightCm = ""
        }

        val weightStr = if (effWeight != null) {
            if (unit == WeightUnit.US) {
                val lbs = (effWeight * 2.20462 * 10).roundToInt() / 10.0
                if (lbs % 1.0 == 0.0) lbs.toInt().toString() else lbs.toString()
            } else {
                val kg = (effWeight * 10).roundToInt() / 10.0
                if (kg % 1.0 == 0.0) kg.toInt().toString() else kg.toString()
            }
        } else ""

        val targetWeightStr = if (currentProfile.targetWeightKg != null) {
            if (unit == WeightUnit.US) {
                val lbs = (currentProfile.targetWeightKg * 2.20462 * 10).roundToInt() / 10.0
                if (lbs % 1.0 == 0.0) lbs.toInt().toString() else lbs.toString()
            } else {
                val kg = (currentProfile.targetWeightKg * 10).roundToInt() / 10.0
                if (kg % 1.0 == 0.0) kg.toInt().toString() else kg.toString()
            }
        } else ""

        val deltaStr = currentProfile.targetCalorieDelta?.let {
            if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
        } ?: ""

        val initialRaw = ProfileFormState(
            name = currentProfile.name,
            gender = currentProfile.gender,
            ageText = currentProfile.age?.toString() ?: "",
            heightFeetText = heightFeet,
            heightInchesText = heightInches,
            heightCmText = heightCm,
            weightText = weightStr,
            activityLevel = currentProfile.activityLevel,
            targetWeightText = targetWeightStr,
            targetCalorieDeltaText = deltaStr,
            validationError = null
        )

        _formState.value = recomputeFormState(initialRaw, unit)
    }

    fun saveProfile(unit: WeightUnit = currentWeightUnit()): Boolean {
        val form = _formState.value

        // Parse and validate age
        val age = if (form.ageText.isNotBlank()) {
            val parsed = form.ageText.trim().toIntOrNull()
            if (parsed == null || parsed <= 0 || parsed > 130) {
                _formState.update { it.copy(validationError = "Please enter a valid age between 1 and 130.") }
                return false
            }
            parsed
        } else null

        // Parse and validate height
        val heightMeters = if (unit == WeightUnit.US) {
            if (form.heightFeetText.isNotBlank() || form.heightInchesText.isNotBlank()) {
                val feet = form.heightFeetText.trim().toIntOrNull() ?: 0
                val inches = form.heightInchesText.trim().toDoubleOrNull() ?: 0.0
                val totalInches = (feet * 12.0) + inches
                if (totalInches <= 0 || totalInches > 120) {
                    _formState.update { it.copy(validationError = "Please enter a valid height.") }
                    return false
                }
                totalInches * 0.0254
            } else null
        } else {
            if (form.heightCmText.isNotBlank()) {
                val cm = form.heightCmText.trim().toDoubleOrNull()
                if (cm == null || cm <= 0 || cm > 300) {
                    _formState.update { it.copy(validationError = "Please enter a valid height in cm.") }
                    return false
                }
                cm / 100.0
            } else null
        }

        // Parse weight
        val weightKg = if (form.weightText.isNotBlank()) {
            val entered = form.weightText.trim().toDoubleOrNull()
            if (entered == null || entered <= 0 || entered > 1000) {
                _formState.update { it.copy(validationError = "Please enter a valid weight.") }
                return false
            }
            if (unit == WeightUnit.METRIC) entered else entered / 2.20462
        } else null

        // Parse target weight
        val targetWeightKg = if (form.targetWeightText.isNotBlank()) {
            val entered = form.targetWeightText.trim().toDoubleOrNull()
            if (entered == null || entered <= 0 || entered > 1000) {
                _formState.update { it.copy(validationError = "Please enter a valid target weight.") }
                return false
            }
            if (unit == WeightUnit.METRIC) entered else entered / 2.20462
        } else null

        // Parse target calorie delta
        val targetCalorieDelta = if (form.targetCalorieDeltaText.isNotBlank()) {
            val entered = form.targetCalorieDeltaText.trim().toDoubleOrNull()
            if (entered == null || entered < -5000 || entered > 5000) {
                _formState.update { it.copy(validationError = "Please enter a valid calorie delta (e.g. -500 or +300).") }
                return false
            }
            entered
        } else null

        val updatedProfile = UserProfileData(
            name = form.name.toTitleCase(),
            gender = form.gender,
            age = age,
            heightMeters = heightMeters,
            profileWeightKg = weightKg,
            activityLevel = form.activityLevel,
            targetWeightKg = targetWeightKg,
            targetCalorieDelta = targetCalorieDelta
        )

        repository?.saveProfile(updatedProfile)
        return true
    }

    private fun currentWeightUnit(): WeightUnit {
        return settingsStore?.settings?.value?.weight?.unit ?: WeightUnit.US
    }

    private fun recomputeFormState(
        form: ProfileFormState,
        unit: WeightUnit
    ): ProfileFormState {
        val age = form.ageText.trim().toIntOrNull()
        val heightMeters = if (unit == WeightUnit.US) {
            val feet = form.heightFeetText.trim().toIntOrNull() ?: 0
            val inches = form.heightInchesText.trim().toDoubleOrNull() ?: 0.0
            val totalInches = (feet * 12.0) + inches
            if (totalInches > 0) totalInches * 0.0254 else null
        } else {
            val cm = form.heightCmText.trim().toDoubleOrNull()
            if (cm != null && cm > 0) cm / 100.0 else null
        }

        val weightKg = if (form.weightText.isNotBlank()) {
            val entered = form.weightText.trim().toDoubleOrNull()
            if (entered != null && entered > 0) {
                if (unit == WeightUnit.METRIC) entered else entered / 2.20462
            } else null
        } else null

        val targetWeightKg = if (form.targetWeightText.isNotBlank()) {
            val entered = form.targetWeightText.trim().toDoubleOrNull()
            if (entered != null && entered > 0) {
                if (unit == WeightUnit.METRIC) entered else entered / 2.20462
            } else null
        } else null

        val targetCalorieDelta = form.targetCalorieDeltaText.trim().toDoubleOrNull()

        val tempProfile = UserProfileData(
            name = form.name,
            gender = form.gender,
            age = age,
            heightMeters = heightMeters,
            profileWeightKg = weightKg,
            activityLevel = form.activityLevel,
            targetWeightKg = targetWeightKg,
            targetCalorieDelta = targetCalorieDelta
        )

        val metabolic = MetabolicCalculator.calculateMetabolicProfile(tempProfile, weightKg)
        val projection = MetabolicCalculator.calculateWeightGoalProjection(
            currentWeightKg = weightKg,
            targetWeightKg = targetWeightKg,
            targetCalorieDelta = targetCalorieDelta
        )

        // Derive weekly rate and pace safety
        val calculatedWeeklyRate = targetCalorieDelta?.let {
            val raw = MetabolicCalculator.kcalDeltaToWeeklyWeightChange(it, unit)
            (raw * 100.0).roundToInt() / 100.0
        }

        val isLoss = if (weightKg != null && targetWeightKg != null) {
            targetWeightKg < weightKg
        } else {
            (targetCalorieDelta ?: 0.0) < 0
        }

        val paceSafety = calculatedWeeklyRate?.let {
            MetabolicCalculator.evaluatePaceSafety(it, unit, isLoss)
        }

        // Derive target weeks text if feasible projection
        val derivedWeeksText = if (projection is WeightGoalProjection.Feasible) {
            val weeks = (projection.totalDays / 7.0 * 10).roundToInt() / 10.0
            if (weeks % 1.0 == 0.0) weeks.toInt().toString() else weeks.toString()
        } else form.targetWeeksText

        return form.copy(
            computedMetabolic = metabolic,
            computedProjection = projection,
            weeklyRate = calculatedWeeklyRate ?: form.weeklyRate,
            paceSafety = paceSafety,
            targetWeeksText = if (form.goalInputMode != GoalInputMode.TIMELINE) derivedWeeksText else form.targetWeeksText
        )
    }
}
