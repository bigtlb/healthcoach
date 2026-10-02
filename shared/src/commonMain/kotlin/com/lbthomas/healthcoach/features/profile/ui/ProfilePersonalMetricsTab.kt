package com.lbthomas.healthcoach.features.profile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.profile.ProfileFormState
import com.lbthomas.healthcoach.features.profile.ProfileViewModel
import com.lbthomas.healthcoach.features.profile.data.ActivityLevel
import com.lbthomas.healthcoach.features.profile.data.Gender

sealed interface ProfilePersonalMetricsEvent {
    data class NameChanged(val name: String) : ProfilePersonalMetricsEvent
    data class GenderChanged(val gender: Gender) : ProfilePersonalMetricsEvent
    data object ShowGenderGuide : ProfilePersonalMetricsEvent
    data class AgeChanged(val age: String) : ProfilePersonalMetricsEvent
    data class HeightFeetChanged(val feet: String) : ProfilePersonalMetricsEvent
    data class HeightInchesChanged(val inches: String) : ProfilePersonalMetricsEvent
    data class HeightCmChanged(val cm: String) : ProfilePersonalMetricsEvent
    data class WeightChanged(val weight: String) : ProfilePersonalMetricsEvent
    data class ActivityLevelChanged(val level: ActivityLevel) : ProfilePersonalMetricsEvent
}

@Composable
fun ProfilePersonalMetricsTab(
    profileViewModel: ProfileViewModel,
    weightUnit: WeightUnit,
    onShowGenderGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formState by profileViewModel.formState.collectAsState()
    ProfilePersonalMetricsTab(
        formState = formState,
        weightUnit = weightUnit,
        onEvent = { event ->
            when (event) {
                is ProfilePersonalMetricsEvent.NameChanged -> profileViewModel.onNameChange(event.name)
                is ProfilePersonalMetricsEvent.GenderChanged -> profileViewModel.onGenderChange(event.gender)
                is ProfilePersonalMetricsEvent.ShowGenderGuide -> onShowGenderGuide()
                is ProfilePersonalMetricsEvent.AgeChanged -> profileViewModel.onAgeChange(event.age)
                is ProfilePersonalMetricsEvent.HeightFeetChanged -> profileViewModel.onHeightFeetChange(event.feet)
                is ProfilePersonalMetricsEvent.HeightInchesChanged -> profileViewModel.onHeightInchesChange(event.inches)
                is ProfilePersonalMetricsEvent.HeightCmChanged -> profileViewModel.onHeightCmChange(event.cm)
                is ProfilePersonalMetricsEvent.WeightChanged -> profileViewModel.onWeightChange(event.weight)
                is ProfilePersonalMetricsEvent.ActivityLevelChanged -> profileViewModel.onActivityLevelChange(event.level)
            }
        },
        modifier = modifier
    )
}

@Composable
fun ProfilePersonalMetricsTab(
    formState: ProfileFormState,
    weightUnit: WeightUnit,
    onEvent: (ProfilePersonalMetricsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var activityDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Name
        OutlinedTextField(
            value = formState.name,
            onValueChange = { onEvent(ProfilePersonalMetricsEvent.NameChanged(it)) },
            label = { Text("Name / Nickname (Optional)") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Gender Selection
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Gender / Metabolic Model",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Gender.entries.forEachIndexed { index, gender ->
                    SegmentedButton(
                        selected = formState.gender == gender,
                        onClick = { onEvent(ProfilePersonalMetricsEvent.GenderChanged(gender)) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = Gender.entries.size
                        )
                    ) {
                        Text(gender.displayName)
                    }
                }
            }

            // Gender & Metabolic Calculations Guide Link
            Row(
                modifier = Modifier
                    .clickable { onEvent(ProfilePersonalMetricsEvent.ShowGenderGuide) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Gender & Metabolic Calculations Guide (TL;DR, clinical GAHT guidelines, & citations)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Age and Height Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Age
            OutlinedTextField(
                value = formState.ageText,
                onValueChange = { onEvent(ProfilePersonalMetricsEvent.AgeChanged(it)) },
                label = { Text("Age (years)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            // Height
            if (weightUnit == WeightUnit.US) {
                OutlinedTextField(
                    value = formState.heightFeetText,
                    onValueChange = { onEvent(ProfilePersonalMetricsEvent.HeightFeetChanged(it)) },
                    label = { Text("Height (ft)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = formState.heightInchesText,
                    onValueChange = { onEvent(ProfilePersonalMetricsEvent.HeightInchesChanged(it)) },
                    label = { Text("Height (in)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            } else {
                OutlinedTextField(
                    value = formState.heightCmText,
                    onValueChange = { onEvent(ProfilePersonalMetricsEvent.HeightCmChanged(it)) },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(2f),
                    singleLine = true
                )
            }
        }

        // Current Weight and Activity Level Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Weight
            OutlinedTextField(
                value = formState.weightText,
                onValueChange = { onEvent(ProfilePersonalMetricsEvent.WeightChanged(it)) },
                label = { Text("Weight (${if (weightUnit == WeightUnit.US) "lbs" else "kg"})", maxLines=1) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            // Activity Level Dropdown
            Box(modifier = Modifier.weight(1.5f)) {
                OutlinedTextField(
                    value = formState.activityLevel.shortName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Physical Activity") },
                    trailingIcon = {
                        IconButton(onClick = { activityDropdownExpanded = !activityDropdownExpanded }) {
                            Icon(
                                imageVector = if (activityDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Select Activity Level"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activityDropdownExpanded = true }
                )

                DropdownMenu(
                    expanded = activityDropdownExpanded,
                    onDismissRequest = { activityDropdownExpanded = false }
                ) {
                    ActivityLevel.entries.forEach { level ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = level.displayName,
                                        fontWeight = if (formState.activityLevel == level) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = "Multiplier: ${level.multiplier}x BMR",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onEvent(ProfilePersonalMetricsEvent.ActivityLevelChanged(level))
                                activityDropdownExpanded = false
                            }
                        )
                    }
                }
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

        // Live Metabolic Baseline Preview Card
        MetabolicSummaryCard(metabolicProfile = formState.computedMetabolic)
    }
}

@Preview
@Composable
private fun ProfilePersonalMetricsTabPreview() {
    com.lbthomas.healthcoach.core.di.previewAppModuleWith().let { module ->
        org.koin.compose.KoinApplication(
            configuration = org.koin.dsl.koinConfiguration(declaration = { modules(module) }),
            content = {
                val profileViewModel: ProfileViewModel = org.koin.compose.koinInject()
                MaterialTheme {
                    Surface(modifier = Modifier.padding(16.dp)) {
                        ProfilePersonalMetricsTab(
                            profileViewModel = profileViewModel,
                            weightUnit = WeightUnit.US,
                            onShowGenderGuide = {}
                        )
                    }
                }
            }
        )
    }
}
