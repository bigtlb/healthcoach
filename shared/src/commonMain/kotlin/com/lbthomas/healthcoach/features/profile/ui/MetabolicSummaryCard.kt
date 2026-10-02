package com.lbthomas.healthcoach.features.profile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.features.profile.data.MetabolicProfile
import kotlin.math.roundToInt

@Composable
fun MetabolicSummaryCard(
    metabolicProfile: MetabolicProfile?,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Metabolic Estimates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Reference Link to Mifflin-St Jeor (AJCN/NIH)
                Tooltip("Open official Mifflin-St Jeor study (AJCN / DOI: 10.1093/ajcn/51.2.241)") {
                    Row(
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://doi.org/10.1093/ajcn/51.2.241")
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Mifflin-St Jeor (AJCN)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Mifflin-St Jeor Reference",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (metabolicProfile != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // BMR
                    MetabolicMetricItem(
                        title = "BMR",
                        tooltipText = "Basal Metabolic Rate: calories burned at complete rest to maintain vital functions.",
                        value = "${metabolicProfile.bmr.roundToInt()}",
                        unit = "kcal/day",
                        modifier = Modifier.weight(1f)
                    )

                    // Maintenance / TDEE
                    MetabolicMetricItem(
                        title = "Maintenance (TDEE)",
                        tooltipText = "Total Daily Energy Expenditure: baseline calories needed to maintain current weight with your activity level.",
                        value = "${metabolicProfile.maintenanceCalories.roundToInt()}",
                        unit = "kcal/day",
                        modifier = Modifier.weight(1.5f)
                    )

                    // Target
                    MetabolicMetricItem(
                        title = "Daily Target",
                        tooltipText = "Target calorie budget including your configured deficit or surplus.",
                        value = metabolicProfile.targetCalories?.let { "${it.roundToInt()}" } ?: "—",
                        unit = "kcal/day",
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    text = "Enter age, height, weight, and activity level to view live BMR and maintenance calorie estimates.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetabolicMetricItem(
    title: String,
    tooltipText: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                maxLines=1,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Tooltip(tooltipText) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = tooltipText,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(name = "Metabolic Summary Card - Populated")
@Composable
fun MetabolicSummaryCardPopulatedPreview() {
    MaterialTheme {
        MetabolicSummaryCard(
            metabolicProfile = MetabolicProfile(
                bmr = 1545.0,
                maintenanceCalories = 2395.0,
                targetCalories = 1895.0,
                currentEffectiveWeightKg = 70.0
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Metabolic Summary Card - Empty")
@Composable
fun MetabolicSummaryCardEmptyPreview() {
    MaterialTheme {
        MetabolicSummaryCard(
            metabolicProfile = null,
            modifier = Modifier.padding(16.dp)
        )
    }
}
