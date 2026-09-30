package com.lbthomas.healthcoach.features.graphs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.previewAppModuleWith
import com.lbthomas.healthcoach.core.theme.HealthCoachTheme
import com.lbthomas.healthcoach.features.bloodpressure.BloodPressureViewModel
import com.lbthomas.healthcoach.features.foodjournal.FoodJournalViewModel
import com.lbthomas.healthcoach.features.graphs.data.buildBpGraphEntries
import com.lbthomas.healthcoach.features.graphs.data.buildCalorieGraphEntries
import com.lbthomas.healthcoach.features.graphs.data.buildWeightGraphEntries
import com.lbthomas.healthcoach.features.graphs.ui.CompoundHealthChart
import com.lbthomas.healthcoach.features.graphs.ui.EmptyGraphState
import com.lbthomas.healthcoach.features.graphs.ui.GraphHeader
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.BloodPressureSettings
import com.lbthomas.healthcoach.features.settings.data.FoodJournalSettings
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import com.lbthomas.healthcoach.features.settings.data.SettingsStore
import com.lbthomas.healthcoach.features.settings.data.WeightSettings
import com.lbthomas.healthcoach.features.weight.WeightViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration
import org.koin.dsl.module

@Composable
fun GraphsView(
    modifier: Modifier = Modifier
) {
    val weightViewModel = koinInject<WeightViewModel>()
    val bpViewModel = koinInject<BloodPressureViewModel>()
    val foodJournalViewModel = koinInject<FoodJournalViewModel>()
    val settingsViewModel = koinInject<SettingsViewModel>()

    val rawWeightEntries by weightViewModel.entries.collectAsState()
    val rawBpEntries by bpViewModel.entries.collectAsState()
    val rawCalorieTotals by foodJournalViewModel.dailyCalorieTotals.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()

    val selectedTimeFrame = settings.ui.selectedGraphTimeFrame
    val showWeight = settings.weight.showInGraph
    val showBp = settings.bloodPressure.showInGraph
    val showCalories = settings.foodJournal.showInGraph

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

    val calorieGraphEntries = remember(rawCalorieTotals, selectedTimeFrame) {
        buildCalorieGraphEntries(
            rawTotals = rawCalorieTotals,
            timeFrame = selectedTimeFrame
        )
    }

    val hasWeightData = showWeight && weightGraphEntries.entries.isNotEmpty()
    val hasBpData = showBp && bpGraphEntries.points.isNotEmpty()
    val hasCalorieData = showCalories && calorieGraphEntries.points.isNotEmpty()
    val hasAnyData = hasWeightData || hasBpData || hasCalorieData

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
                            message = if (!showWeight && !showBp && !showCalories) {
                                "Enable Weight, Blood Pressure, or Calories above to view graph."
                            } else {
                                "No data available for the selected series and timeframe."
                            }
                        )
                    } else {
                        CompoundHealthChart(
                            showWeight = showWeight,
                            showBp = showBp,
                            showPulse = settings.bloodPressure.showPulseInGraph,
                            showCalories = showCalories,
                            weightEntries = if (showWeight) weightGraphEntries.entries else emptyList(),
                            bpPoints = if (showBp) bpGraphEntries.points else emptyList(),
                            caloriePoints = if (showCalories) calorieGraphEntries.points else emptyList(),
                            weightUnit = settings.weight.unit,
                            weightMinEpoch = weightGraphEntries.minEpochDay,
                            weightMaxEpoch = weightGraphEntries.maxEpochDay,
                            bpMinEpoch = bpGraphEntries.minEpochDay,
                            bpMaxEpoch = bpGraphEntries.maxEpochDay,
                            calorieMinEpoch = calorieGraphEntries.minEpochDay,
                            calorieMaxEpoch = calorieGraphEntries.maxEpochDay,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Preview(
    name = "Graphs View - Composite (All Series)",
    showBackground = true,
    backgroundColor = 0xEADDFF,
    device = "spec:width=360dp,height=780dp,dpi=420"
)
@Composable
fun GraphsViewCompositePreview() {
    val compositeModule = remember {
        previewAppModuleWith(
            SettingsData(
                weight = WeightSettings(showInGraph = true),
                bloodPressure = BloodPressureSettings(showInGraph = true, showPulseInGraph = true),
                foodJournal = FoodJournalSettings(showInGraph = true)
            )
        )
    }
    KoinApplication(
        configuration = koinConfiguration(declaration = {
            modules(compositeModule)
        }),
        content = {
            HealthCoachTheme {
                GraphsView()
            }
        }
    )
}

@Preview(
    name = "Graphs View - Weight Only",
    showBackground = true,
    backgroundColor = 0xEADDFF,
    device = "spec:width=360dp,height=780dp,dpi=420"
)
@Composable
fun GraphsViewWeightOnlyPreview() {
    val weightOnlyModule = remember {
        previewAppModuleWith(
            SettingsData(
                weight = WeightSettings(showInGraph = true),
                bloodPressure = BloodPressureSettings(showInGraph = false),
                foodJournal = FoodJournalSettings(showInGraph = false)
            )
        )
    }
    KoinApplication(
        configuration = koinConfiguration(declaration = {
            modules(weightOnlyModule)
        }),
        content = {
            HealthCoachTheme {
                GraphsView()
            }
        }
    )
}

@Preview(
    name = "Graphs View - Blood Pressure Only",
    showBackground = true,
    backgroundColor = 0xEADDFF,
    device = "spec:width=360dp,height=780dp,dpi=420"
)
@Composable
fun GraphsViewBloodPressureOnlyPreview() {
    val bpOnlyModule = remember {
        previewAppModuleWith(
            SettingsData(
                weight = WeightSettings(showInGraph = false),
                bloodPressure = BloodPressureSettings(showInGraph = true, showPulseInGraph = true),
                foodJournal = FoodJournalSettings(showInGraph = false)
            )
        )
    }
    KoinApplication(
        configuration = koinConfiguration(declaration = {
            modules(bpOnlyModule)
        }),
        content = {
            HealthCoachTheme {
                GraphsView()
            }
        }
    )
}

@Preview(
    name = "Graphs View - Journal / Calories Only",
    showBackground = true,
    backgroundColor = 0xEADDFF,
    device = "spec:width=360dp,height=780dp,dpi=420"
)
@Composable
fun GraphsViewJournalOnlyPreview() {
    val journalOnlyModule = remember {
        previewAppModuleWith(
            SettingsData(
                weight = WeightSettings(showInGraph = false),
                bloodPressure = BloodPressureSettings(showInGraph = false),
                foodJournal = FoodJournalSettings(showInGraph = true)
            )
        )
    }
    KoinApplication(
        configuration = koinConfiguration(declaration = {
            modules(journalOnlyModule)
        }),
        content = {
            HealthCoachTheme {
                GraphsView()
            }
        }
    )
}

@Preview(
    name = "Graphs View - BP and Calories",
    showBackground = true,
    backgroundColor = 0xEADDFF,
    device = "spec:width=360dp,height=780dp,dpi=420"
)
@Composable
fun GraphsViewBpAndCaloriesPreview() {
    val bpAndCaloriesModule = remember {
        previewAppModuleWith(
            SettingsData(
                weight = WeightSettings(showInGraph = false),
                bloodPressure = BloodPressureSettings(showInGraph = true, showPulseInGraph = true),
                foodJournal = FoodJournalSettings(showInGraph = true)
            )
        )
    }
    KoinApplication(
        configuration = koinConfiguration(declaration = {
            modules(bpAndCaloriesModule)
        }),
        content = {
            HealthCoachTheme {
                GraphsView()
            }
        }
    )
}
