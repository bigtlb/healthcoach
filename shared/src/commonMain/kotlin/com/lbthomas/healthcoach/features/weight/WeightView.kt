package com.lbthomas.healthcoach.features.weight

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.di.previewAppModule
import com.lbthomas.healthcoach.core.ui.DeleteConfirmationDialog
import com.lbthomas.healthcoach.core.utils.today
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import com.lbthomas.healthcoach.features.weight.ui.AddWeightEntryButton
import com.lbthomas.healthcoach.features.weight.ui.WeightEntryEditDialog
import com.lbthomas.healthcoach.features.weight.ui.WeightEntryList
import kotlinx.datetime.LocalDate
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
fun WeightView(
    showAddWeightEntry: Boolean,
    modifier: Modifier = Modifier,
    isWideLayout: Boolean = false,
    onAddDismiss: () -> Unit = {},
    onRequestFocus: () -> Unit = {}
) {
    val viewModel = koinInject<WeightViewModel>()
    val settings by koinInject<SettingsViewModel>().settings.collectAsState()
    val entries by viewModel.entries.collectAsState()

    var entryToDelete by remember { mutableStateOf<WeightEntryData?>(null) }
    var dateToEdit by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(showAddWeightEntry) {
        if (showAddWeightEntry) {
            dateToEdit = today
        }
    }

    entryToDelete?.let { entry ->
        DeleteConfirmationDialog(
            title = "Delete Entry",
            message = "Are you sure you want to delete the weight entry for ${entry.date}?",
            testTag = "delete_confirmation_dialog",
            onConfirm = {
                viewModel.deleteEntry(entry.id)
                entryToDelete = null
                onRequestFocus()
            },
            onDismiss = {
                entryToDelete = null
                onRequestFocus()
            }
        )
    }

    dateToEdit?.let { date ->
        WeightEntryEditDialog(
            initialDate = date,
            getEntriesForDate = { targetDate ->
                val current = entries.find { it.date == targetDate }
                val prior = entries.filter { it.date < targetDate }.maxByOrNull { it.date }
                Pair(current, prior)
            },
            onConfirm = { updatedEntry ->
                if (updatedEntry.id.isEmpty() || updatedEntry.id == "0")
                    viewModel.addEntry(updatedEntry.date, updatedEntry.weight)
                else
                    viewModel.updateEntry(updatedEntry)
                dateToEdit = null
                if (showAddWeightEntry) onAddDismiss()
                onRequestFocus()
            },
            onDismiss = {
                dateToEdit = null
                if (showAddWeightEntry) onAddDismiss()
                onRequestFocus()
            },
            settings = settings
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        if (isWideLayout) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weight",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AddWeightEntryButton(
                    onClick = {
                        dateToEdit = today
                    }
                )
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            WeightEntryList(
                entries = entries,
                settings = settings,
                isWideLayout = isWideLayout,
                onClickEntry = { entry ->
                    dateToEdit = entry.date
                },
                onDeleteEntry = { entry ->
                    entryToDelete = entry
                }
            )
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xEADDFF,
    device = "spec:width=360dp,height=780dp,dpi=420"
)
@Composable
fun WeightViewPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            WeightView(false)
        }
    )
}
