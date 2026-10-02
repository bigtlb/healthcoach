package com.lbthomas.healthcoach.features.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.profile.ProfileFormState
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TimelineGoalControls(
    formState: ProfileFormState,
    weightUnit: WeightUnit,
    onTargetWeeksChange: (String, WeightUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentWeight = formState.weightText.toDoubleOrNull()
    val targetWeight = formState.targetWeightText.toDoubleOrNull()

    if (currentWeight == null || targetWeight == null || currentWeight <= 0 || targetWeight <= 0) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Please enter both Current Weight in Personal Metrics and Target Weight above to calculate target timeline duration.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    } else {
        val timelinePresets = listOf("4", "8", "12", "16", "24", "52")

        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quick Timeline Presets
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                timelinePresets.forEachIndexed { index, preset ->
                    SegmentedButton(
                        selected = formState.targetWeeksText == preset,
                        onClick = { onTargetWeeksChange(preset, weightUnit) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = timelinePresets.size
                        )
                    ) {
                        Text(
                            text = "$preset wks",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = formState.targetWeeksText,
                    onValueChange = { onTargetWeeksChange(it, weightUnit) },
                    label = { Text("Target Duration (weeks)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                val deltaInt = formState.targetCalorieDeltaText.toDoubleOrNull()?.roundToInt() ?: 0
                val rateStr = formState.weeklyRate?.let { abs(it).toString() } ?: "—"
                val unitStr = if (weightUnit == WeightUnit.US) "lbs/wk" else "kg/wk"

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Required Delta",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$deltaInt kcal/day (~$rateStr $unitStr)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xffffff)
@Composable
private fun TimelineGoalControlsPreview() {
    MaterialTheme {
        TimelineGoalControls(
            formState = ProfileFormState(
                weightText = "180.0",
                targetWeightText = "165.0",
                targetWeeksText = "15",
                weeklyRate = -1.0,
                targetCalorieDeltaText = "-500"
            ),
            weightUnit = WeightUnit.US,
            onTargetWeeksChange = { _, _ -> }
        )
    }
}
