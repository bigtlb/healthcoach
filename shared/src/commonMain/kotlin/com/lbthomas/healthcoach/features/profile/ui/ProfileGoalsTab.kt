package com.lbthomas.healthcoach.features.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.profile.GoalInputMode
import com.lbthomas.healthcoach.features.profile.ProfileFormState
import com.lbthomas.healthcoach.features.profile.ProfileViewModel

sealed interface ProfileGoalsEvent {
    data class TargetWeightChanged(val targetWeight: String) : ProfileGoalsEvent
    data class GoalInputModeChanged(val mode: GoalInputMode) : ProfileGoalsEvent
    data class WeeklyRateChanged(val rate: Double, val unit: WeightUnit) : ProfileGoalsEvent
    data class PresetRateSelected(val rate: Double, val unit: WeightUnit) : ProfileGoalsEvent
    data class TargetWeeksChanged(val weeks: String, val unit: WeightUnit) : ProfileGoalsEvent
    data class TargetCalorieDeltaChanged(val delta: String) : ProfileGoalsEvent
}

@Composable
fun ProfileGoalsTab(
    profileViewModel: ProfileViewModel,
    weightUnit: WeightUnit,
    modifier: Modifier = Modifier
) {
    val formState by profileViewModel.formState.collectAsState()
    ProfileGoalsTab(
        formState = formState,
        weightUnit = weightUnit,
        onEvent = { event ->
            when (event) {
                is ProfileGoalsEvent.TargetWeightChanged -> profileViewModel.onTargetWeightChange(event.targetWeight)
                is ProfileGoalsEvent.GoalInputModeChanged -> profileViewModel.onGoalInputModeChange(event.mode)
                is ProfileGoalsEvent.WeeklyRateChanged -> profileViewModel.onWeeklyRateChange(event.rate, event.unit)
                is ProfileGoalsEvent.PresetRateSelected -> profileViewModel.onPresetRateSelected(event.rate, event.unit)
                is ProfileGoalsEvent.TargetWeeksChanged -> profileViewModel.onTargetWeeksChange(event.weeks, event.unit)
                is ProfileGoalsEvent.TargetCalorieDeltaChanged -> profileViewModel.onTargetCalorieDeltaChange(event.delta)
            }
        },
        modifier = modifier
    )
}

@Composable
fun ProfileGoalsTab(
    formState: ProfileFormState,
    weightUnit: WeightUnit,
    onEvent: (ProfileGoalsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Target Weight Field
        OutlinedTextField(
            value = formState.targetWeightText,
            onValueChange = { onEvent(ProfileGoalsEvent.TargetWeightChanged(it)) },
            label = { Text("Target Weight (${if (weightUnit == WeightUnit.US) "lbs" else "kg"}) (Optional)") },
            placeholder = { Text("e.g. ${if (weightUnit == WeightUnit.US) "165" else "75"}") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Strategy Mode Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Goal Configuration Strategy",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                GoalInputMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = formState.goalInputMode == mode,
                        onClick = { onEvent(ProfileGoalsEvent.GoalInputModeChanged(mode)) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = GoalInputMode.entries.size
                        )
                    ) {
                        Text(mode.displayName)
                    }
                }
            }
        }

        // Strategy Specific Controls
        when (formState.goalInputMode) {
            GoalInputMode.RATE -> {
                RateGoalControls(
                    formState = formState,
                    weightUnit = weightUnit,
                    onWeeklyRateChange = { rate, unit -> onEvent(ProfileGoalsEvent.WeeklyRateChanged(rate, unit)) },
                    onPresetRateSelected = { rate, unit -> onEvent(ProfileGoalsEvent.PresetRateSelected(rate, unit)) }
                )
            }
            GoalInputMode.TIMELINE -> {
                TimelineGoalControls(
                    formState = formState,
                    weightUnit = weightUnit,
                    onTargetWeeksChange = { weeks, unit -> onEvent(ProfileGoalsEvent.TargetWeeksChanged(weeks, unit)) }
                )
            }
            GoalInputMode.MANUAL -> {
                ManualDeltaGoalControls(
                    formState = formState,
                    weightUnit = weightUnit,
                    onTargetCalorieDeltaChange = { onEvent(ProfileGoalsEvent.TargetCalorieDeltaChanged(it)) }
                )
            }
        }

        // Validation Error Display
        if (formState.validationError != null) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = formState.validationError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Live Preview Cards
        GoalProjectionCard(projection = formState.computedProjection)
        MetabolicSummaryCard(metabolicProfile = formState.computedMetabolic)
    }
}

@Preview
@Composable
private fun ProfileGoalsTabPreview() {
    com.lbthomas.healthcoach.core.di.previewAppModuleWith().let { module ->
        org.koin.compose.KoinApplication(
            configuration = org.koin.dsl.koinConfiguration(declaration = { modules(module) }),
            content = {
                val profileViewModel: ProfileViewModel = org.koin.compose.koinInject()
                MaterialTheme {
                    Surface(modifier = Modifier.padding(16.dp)) {
                        ProfileGoalsTab(
                            profileViewModel = profileViewModel,
                            weightUnit = WeightUnit.US
                        )
                    }
                }
            }
        )
    }
}
