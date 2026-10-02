package com.lbthomas.healthcoach.features.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.profile.ProfileFormState
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ManualDeltaGoalControls(
    formState: ProfileFormState,
    weightUnit: WeightUnit,
    onTargetCalorieDeltaChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedTextField(
            value = formState.targetCalorieDeltaText,
            onValueChange = { onTargetCalorieDeltaChange(it) },
            label = { Text("Calorie Delta (kcal/day)") },
            placeholder = { Text("e.g. -500 or +300") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        val deltaInt = formState.targetCalorieDeltaText.toDoubleOrNull()?.roundToInt()
        if (deltaInt != null) {
            val rateStr = formState.weeklyRate?.let { abs(it).toString() } ?: "—"
            val unitStr = if (weightUnit == WeightUnit.US) "lbs/week" else "kg/week"
            Text(
                text = if (deltaInt < 0) {
                    "Deficit of ${abs(deltaInt)} kcal/day corresponds to ~$rateStr $unitStr weight loss."
                } else if (deltaInt > 0) {
                    "Surplus of $deltaInt kcal/day corresponds to ~$rateStr $unitStr weight gain."
                } else {
                    "Maintenance: 0 kcal/day delta."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
private fun ManualDeltaGoalControlsPreview() {
    MaterialTheme {
        ManualDeltaGoalControls(
            formState = ProfileFormState(
                targetCalorieDeltaText = "-500",
                weeklyRate = -1.0
            ),
            weightUnit = WeightUnit.US,
            onTargetCalorieDeltaChange = {}
        )
    }
}
