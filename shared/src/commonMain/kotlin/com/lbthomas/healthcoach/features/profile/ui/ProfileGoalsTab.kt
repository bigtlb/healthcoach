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
        onTargetWeightChange = { profileViewModel.onTargetWeightChange(it) },
        onGoalInputModeChange = { profileViewModel.onGoalInputModeChange(it) },
        onWeeklyRateChange = { rate, unit -> profileViewModel.onWeeklyRateChange(rate, unit) },
        onPresetRateSelected = { rate, unit -> profileViewModel.onPresetRateSelected(rate, unit) },
        onTargetWeeksChange = { weeks, unit -> profileViewModel.onTargetWeeksChange(weeks, unit) },
        onTargetCalorieDeltaChange = { profileViewModel.onTargetCalorieDeltaChange(it) },
        modifier = modifier
    )
}

@Composable
fun ProfileGoalsTab(
    formState: ProfileFormState,
    weightUnit: WeightUnit,
    onTargetWeightChange: (String) -> Unit,
    onGoalInputModeChange: (GoalInputMode) -> Unit,
    onWeeklyRateChange: (Double, WeightUnit) -> Unit,
    onPresetRateSelected: (Double, WeightUnit) -> Unit,
    onTargetWeeksChange: (String, WeightUnit) -> Unit,
    onTargetCalorieDeltaChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Target Weight Field
        OutlinedTextField(
            value = formState.targetWeightText,
            onValueChange = onTargetWeightChange,
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
                        onClick = { onGoalInputModeChange(mode) },
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
                    onWeeklyRateChange = onWeeklyRateChange,
                    onPresetRateSelected = onPresetRateSelected
                )
            }
            GoalInputMode.TIMELINE -> {
                TimelineGoalControls(
                    formState = formState,
                    weightUnit = weightUnit,
                    onTargetWeeksChange = onTargetWeeksChange
                )
            }
            GoalInputMode.MANUAL -> {
                ManualDeltaGoalControls(
                    formState = formState,
                    weightUnit = weightUnit,
                    onTargetCalorieDeltaChange = onTargetCalorieDeltaChange
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
