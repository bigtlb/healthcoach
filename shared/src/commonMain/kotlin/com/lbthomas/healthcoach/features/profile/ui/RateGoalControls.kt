package com.lbthomas.healthcoach.features.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.features.profile.ProfileFormState
import com.lbthomas.healthcoach.features.profile.data.PaceSafetyInfo
import com.lbthomas.healthcoach.features.profile.data.PaceSafetyLevel
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun RateGoalControls(
    formState: ProfileFormState,
    weightUnit: WeightUnit,
    onWeeklyRateChange: (Double, WeightUnit) -> Unit,
    onPresetRateSelected: (Double, WeightUnit) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentWeight = formState.weightText.toDoubleOrNull()
    val targetWeight = formState.targetWeightText.toDoubleOrNull()
    val isLoss = if (currentWeight != null && targetWeight != null) {
        targetWeight < currentWeight
    } else {
        (formState.weeklyRate ?: 0.0) <= 0.0
    }
    val isGain = if (currentWeight != null && targetWeight != null) {
        targetWeight > currentWeight
    } else {
        (formState.weeklyRate ?: 0.0) > 0.0
    }

    val presets = if (weightUnit == WeightUnit.US) {
        if (isGain) {
            listOf(0.0 to "Maintain\n(0)", 0.25 to "Lean\n(+0.25)", 0.5 to "Standard\n(+0.5)", 1.0 to "Aggressive\n(+1.0)")
        } else {
            listOf(0.0 to "Maintain\n(0)", -0.5 to "Gentle\n(-0.5)", -1.0 to "Standard\n(-1.0)", -1.5 to "Aggressive\n(-1.5)")
        }
    } else {
        if (isGain) {
            listOf(0.0 to "Maintain\n(0)", 0.1 to "Lean\n(+0.1)", 0.25 to "Standard\n(+0.25)", 0.5 to "Aggressive\n(+0.5)")
        } else {
            listOf(0.0 to "Maintain\n(0)", -0.25 to "Gentle\n(-0.25)", -0.5 to "Standard\n(-0.5)", -0.75 to "Aggressive\n(-0.75)")
        }
    }

    val currentRate = formState.weeklyRate ?: 0.0
    val deltaInt = formState.targetCalorieDeltaText.toDoubleOrNull()?.roundToInt() ?: 0
    val rateUnitStr = if (weightUnit == WeightUnit.US) "lbs/week" else "kg/week"
    val rateDescription = when {
        abs(currentRate) < 0.01 -> "Maintain current weight (0 $rateUnitStr, 0 kcal/day)"
        currentRate < 0 -> "Pace: ${abs(currentRate)} $rateUnitStr loss (${deltaInt} kcal/day)"
        else -> "Pace: +$currentRate $rateUnitStr gain (+${deltaInt} kcal/day)"
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rateDescription,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Quick Presets
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            presets.forEachIndexed { index, (presetRate, label) ->
                SegmentedButton(
                    selected = abs(currentRate - presetRate) < 0.05,
                    onClick = { onPresetRateSelected(presetRate, weightUnit) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = presets.size
                    )
                ) {
                    Text(
                        text = label,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        // Slider
        val minRate = if (weightUnit == WeightUnit.US) -2.5f else -1.25f
        val maxRate = if (weightUnit == WeightUnit.US) 1.5f else 0.75f
        val sliderValue = currentRate.toFloat().coerceIn(minRate, maxRate)

        Slider(
            value = sliderValue,
            onValueChange = {
                val stepRounded = ((it * 20.0).roundToInt() / 20.0)
                onWeeklyRateChange(stepRounded, weightUnit)
            },
            valueRange = minRate..maxRate,
            modifier = Modifier.fillMaxWidth()
        )

        // Pace Safety Indicator
        formState.paceSafety?.let { safety ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = when (safety.level) {
                    PaceSafetyLevel.MAINTAIN, PaceSafetyLevel.GENTLE, PaceSafetyLevel.STANDARD ->
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    PaceSafetyLevel.AGGRESSIVE ->
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    PaceSafetyLevel.EXTREME ->
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (safety.level) {
                            PaceSafetyLevel.EXTREME -> Icons.Default.Warning
                            PaceSafetyLevel.AGGRESSIVE -> Icons.Default.Info
                            else -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = when (safety.level) {
                            PaceSafetyLevel.EXTREME -> MaterialTheme.colorScheme.error
                            PaceSafetyLevel.AGGRESSIVE -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = safety.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = safety.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xffffff)
@Composable
private fun RateGoalControlsPreview() {
    MaterialTheme {
        RateGoalControls(
            formState = ProfileFormState(
                weightText = "180.0",
                targetWeightText = "165.0",
                weeklyRate = -1.0,
                targetCalorieDeltaText = "-500",
                paceSafety = PaceSafetyInfo(
                    level = PaceSafetyLevel.STANDARD,
                    label = "Standard Pace (Recommended)",
                    description = "A sustainable deficit rate optimizing fat loss and body composition."
                )
            ),
            weightUnit = WeightUnit.US,
            onWeeklyRateChange = { _, _ -> },
            onPresetRateSelected = { _, _ -> }
        )
    }
}
