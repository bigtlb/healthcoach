package com.lbthomas.healthcoach.features.foodjournal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.enums.MealTime
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.features.foodjournal.data.MealEntryData
import com.lbthomas.healthcoach.features.foodjournal.ui.DailyCalorieSummaryCard
import com.lbthomas.healthcoach.features.foodjournal.ui.DeleteConfirmationDialog
import com.lbthomas.healthcoach.features.foodjournal.ui.FoodSearchAndLogDialog
import com.lbthomas.healthcoach.features.foodjournal.ui.MealTimeCardList
import com.lbthomas.healthcoach.features.foodjournal.ui.PortionEntryDialog
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
fun JournalView(
    showLogFood: Boolean = false,
    modifier: Modifier = Modifier,
    isWideLayout: Boolean = false,
    onLogFoodDismiss: () -> Unit = {},
    onRequestFocus: () -> Unit = {}
) {
    val viewModel = koinInject<FoodJournalViewModel>()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dailySummary by viewModel.dailyMealSummary.collectAsState()

    var showSearchAndLogDialog by remember { mutableStateOf(false) }
    var targetMealTime by remember { mutableStateOf(MealTime.BREAKFAST) }
    var entryToEdit by remember { mutableStateOf<MealEntryData?>(null) }
    var entryToDelete by remember { mutableStateOf<MealEntryData?>(null) }

    LaunchedEffect(showLogFood) {
        if (showLogFood) {
            targetMealTime = MealTime.BREAKFAST
            showSearchAndLogDialog = true
        }
    }

    if (showSearchAndLogDialog) {
        FoodSearchAndLogDialog(
            viewModel = viewModel,
            initialMealTime = targetMealTime,
            initialDate = selectedDate,
            onDismiss = {
                showSearchAndLogDialog = false
                if (showLogFood) onLogFoodDismiss()
                onRequestFocus()
            }
        )
    }

    entryToEdit?.let { entry ->
        PortionEntryDialog(
            initialDate = entry.date,
            initialMealTime = entry.mealTime,
            existingMealEntry = entry,
            onConfirm = { updatedEntry ->
                viewModel.updateMealEntry(updatedEntry)
                entryToEdit = null
                onRequestFocus()
            },
            onDismiss = {
                entryToEdit = null
                onRequestFocus()
            }
        )
    }

    entryToDelete?.let { entry ->
        DeleteConfirmationDialog(
            title = "Delete Meal Entry",
            message = "Are you sure you want to delete \"${entry.foodName}\" from ${entry.mealTime.displayName}?",
            note = null,
            confirmButtonText = "Delete",
            onConfirm = {
                viewModel.deleteMealEntry(entry.id)
                entryToDelete = null
                onRequestFocus()
            },
            onDismiss = {
                entryToDelete = null
                onRequestFocus()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isWideLayout) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Food Journal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Tooltip("Log food entry") {
                    FloatingActionButton(
                        onClick = {
                            targetMealTime = MealTime.BREAKFAST
                            showSearchAndLogDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Log food",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Daily Calorie Summary and Date Navigation Card
        DailyCalorieSummaryCard(
            selectedDate = selectedDate,
            totalCalories = dailySummary.totalCalories,
            onPreviousDay = { viewModel.selectPreviousDay() },
            onNextDay = { viewModel.selectNextDay() },
            onToday = { viewModel.selectToday() },
            onDateSelected = { date -> viewModel.setSelectedDate(date) }
        )

        // 6 Meal Time Slots Card List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            MealTimeCardList(
                summary = dailySummary,
                onAddFoodClick = { mealTime ->
                    targetMealTime = mealTime
                    showSearchAndLogDialog = true
                },
                onEditEntryClick = { entry ->
                    entryToEdit = entry
                },
                onDeleteEntryClick = { entry ->
                    entryToDelete = entry
                }
            )
        }
    }
}

@Preview(
    name = "Journal View - Compact",
    showBackground = true,
    backgroundColor = 0xEADDFF,
    device = "spec:width=360dp,height=780dp,dpi=420"
)
@Composable
fun JournalViewCompactPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            JournalView(showLogFood = false, isWideLayout = false)
        }
    )
}

@Preview(
    name = "Journal View - Wide Split Layout",
    showBackground = true,
    widthDp = 600,
    heightDp = 800
)
@Composable
fun JournalViewWidePreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            JournalView(showLogFood = false, isWideLayout = true)
        }
    )
}
