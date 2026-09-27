package com.lbthomas.healthcoach.features.graphs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.features.bloodpressure.BloodPressureViewModel
import com.lbthomas.healthcoach.features.graphs.data.buildBpGraphEntries
import com.lbthomas.healthcoach.features.graphs.data.buildWeightGraphEntries
import com.lbthomas.healthcoach.features.graphs.ui.CompoundHealthChart
import com.lbthomas.healthcoach.features.graphs.ui.EmptyGraphState
import com.lbthomas.healthcoach.features.graphs.ui.GraphHeader
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.weight.WeightViewModel
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
fun GraphsView(
    modifier: Modifier = Modifier
) {
    val weightViewModel = koinInject<WeightViewModel>()
    val bpViewModel = koinInject<BloodPressureViewModel>()
    val settingsViewModel = koinInject<SettingsViewModel>()

    val rawWeightEntries by weightViewModel.entries.collectAsState()
    val rawBpEntries by bpViewModel.entries.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()

    val selectedTimeFrame = settings.ui.selectedGraphTimeFrame
    val showWeight = settings.weight.showInGraph
    val showBp = settings.bloodPressure.showInGraph

    val weightGraphEntries = remember(rawWeightEntries, selectedTimeFrame) {
        buildWeightGraphEntries(
            rawEntries = rawWeightEntries,
            timeFrame = selectedTimeFrame
        )
    }

    val bpGraphEntries = remember(rawBpEntries, selectedTimeFrame) {
        buildBpGraphEntries(
            rawEntries = rawBpEntries,
            timeFrame = selectedTimeFrame
        )
    }

    val hasWeightData = showWeight && weightGraphEntries.entries.isNotEmpty()
    val hasBpData = showBp && bpGraphEntries.points.isNotEmpty()
    val hasAnyData = hasWeightData || hasBpData

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            GraphHeader(
                selectedTimeFrame = selectedTimeFrame,
                settings = settings,
                settingsViewModel = settingsViewModel
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 2.dp, vertical = 4.dp)
                ) {
                    if (!hasAnyData) {
                        EmptyGraphState(
                            message = if (!showWeight && !showBp) {
                                "Enable Weight or Blood Pressure above to view graph."
                            } else {
                                "No data available for the selected series and timeframe."
                            }
                        )
                    } else {
                        CompoundHealthChart(
                            showWeight = showWeight,
                            showBp = showBp,
                            showPulse = settings.bloodPressure.showPulseInGraph,
                            weightEntries = if (showWeight) weightGraphEntries.entries else emptyList(),
                            bpPoints = if (showBp) bpGraphEntries.points else emptyList(),
                            weightUnit = settings.weight.unit,
                            weightMinEpoch = weightGraphEntries.minEpochDay,
                            weightMaxEpoch = weightGraphEntries.maxEpochDay,
                            bpMinEpoch = bpGraphEntries.minEpochDay,
                            bpMaxEpoch = bpGraphEntries.maxEpochDay,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xEADDFF,
    device = "spec:width=360dp,height=780dp,dpi=420"
)
@Composable
fun GraphsViewPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            GraphsView()
        }
    )
}
