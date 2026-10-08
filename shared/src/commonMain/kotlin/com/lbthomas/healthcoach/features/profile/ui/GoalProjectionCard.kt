package com.lbthomas.healthcoach.features.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.utils.displayDate
import com.lbthomas.healthcoach.features.profile.data.WeightGoalProjection
import kotlinx.datetime.LocalDate

@Composable
fun GoalProjectionCard(
    projection: WeightGoalProjection,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = when (projection) {
            is WeightGoalProjection.Feasible -> CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            )
            is WeightGoalProjection.Infeasible -> CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            )
            is WeightGoalProjection.Achieved -> CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            )
            is WeightGoalProjection.Undefined -> CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val icon = when (projection) {
                    is WeightGoalProjection.Feasible -> Icons.Default.Event
                    is WeightGoalProjection.Infeasible -> Icons.Default.Warning
                    is WeightGoalProjection.Achieved -> Icons.Default.CheckCircle
                    is WeightGoalProjection.Undefined -> Icons.Default.Info
                }
                val iconTint = when (projection) {
                    is WeightGoalProjection.Feasible -> MaterialTheme.colorScheme.onSecondaryContainer
                    is WeightGoalProjection.Infeasible -> MaterialTheme.colorScheme.onErrorContainer
                    is WeightGoalProjection.Achieved -> MaterialTheme.colorScheme.onPrimaryContainer
                    is WeightGoalProjection.Undefined -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = "Weight Goal Timeline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconTint
                )
            }

            when (projection) {
                is WeightGoalProjection.Feasible -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estimated Duration:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = projection.message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estimated Target Date:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = projection.targetDate.displayDate(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                is WeightGoalProjection.Infeasible -> {
                    Text(
                        text = projection.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }

                is WeightGoalProjection.Achieved -> {
                    Text(
                        text = "Congratulations! You are at your target weight.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                is WeightGoalProjection.Undefined -> {
                    Text(
                        text = "Enter current weight, target weight, and calorie delta to see your projected timeline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(name = "Goal Projection - Feasible")
@Composable
fun GoalProjectionCardFeasiblePreview() {
    MaterialTheme {
        GoalProjectionCard(
            projection = WeightGoalProjection.Feasible(
                totalDays = 74,
                targetDate = LocalDate(2026, 12, 15),
                message = "~10.5 weeks (74 days)"
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Goal Projection - Infeasible")
@Composable
fun GoalProjectionCardInfeasiblePreview() {
    MaterialTheme {
        GoalProjectionCard(
            projection = WeightGoalProjection.Infeasible(
                reason = "Weight loss goal requires a calorie deficit, but current configuration is a surplus (+300 kcal/day)."
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Goal Projection - Achieved")
@Composable
fun GoalProjectionCardAchievedPreview() {
    MaterialTheme {
        GoalProjectionCard(
            projection = WeightGoalProjection.Achieved,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Goal Projection - Undefined")
@Composable
fun GoalProjectionCardUndefinedPreview() {
    MaterialTheme {
        GoalProjectionCard(
            projection = WeightGoalProjection.Undefined,
            modifier = Modifier.padding(16.dp)
        )
    }
}
