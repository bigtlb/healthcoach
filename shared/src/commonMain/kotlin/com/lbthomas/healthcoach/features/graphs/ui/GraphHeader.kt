package com.lbthomas.healthcoach.features.graphs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.theme.extendedColors
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
internal fun GraphHeader(
    selectedTimeFrame: GraphTimeFrame,
    settings: SettingsData,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val extColors = MaterialTheme.extendedColors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Series Selection FilterChips
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = settings.weight.showInGraph,
                onClick = { settingsViewModel.setShowWeightInGraph(!settings.weight.showInGraph) },
                label = { Text("Weight", style = MaterialTheme.typography.labelMedium) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(extColors.graphWeight.color, CircleShape)
                    )
                }
            )

            FilterChip(
                selected = settings.bloodPressure.showInGraph,
                onClick = { settingsViewModel.setShowBloodPressureInGraph(!settings.bloodPressure.showInGraph) },
                label = { Text("Blood Pressure", style = MaterialTheme.typography.labelMedium) },
                leadingIcon = {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(extColors.graphSystolic.color, CircleShape)
                    )
                }
            )
        }

        // Time Frame Dropdown
        GraphTimeFrameDropdown(
            selectedTimeFrame = selectedTimeFrame,
            onTimeFrameSelected = { settingsViewModel.setSelectedGraphTimeFrame(it) }
        )
    }
}

@Composable
internal fun GraphTimeFrameDropdown(
    selectedTimeFrame: GraphTimeFrame,
    onTimeFrameSelected: (GraphTimeFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = selectedTimeFrame.label,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Time Frame",
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            GraphTimeFrame.entries.forEach { timeFrame ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = timeFrame.label
                        )
                    },
                    onClick = {
                        onTimeFrameSelected(timeFrame)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
internal fun EmptyGraphState(
    message: String = "Log weight or blood pressure entries to view your progress graph.",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ShowChart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Graph Data Available",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
private fun GraphHeaderPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            val settingsViewModel: SettingsViewModel = koinInject()
            Surface(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                GraphHeader(
                    selectedTimeFrame = GraphTimeFrame.ONE_MONTH,
                    settings = SettingsData(),
                    settingsViewModel = settingsViewModel
                )
            }
        }
    )
}
