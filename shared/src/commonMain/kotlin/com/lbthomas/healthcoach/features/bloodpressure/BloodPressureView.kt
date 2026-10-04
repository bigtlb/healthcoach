package com.lbthomas.healthcoach.features.bloodpressure

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
import com.lbthomas.healthcoach.core.utils.formatTime
import com.lbthomas.healthcoach.core.utils.today
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureEntryData
import com.lbthomas.healthcoach.features.bloodpressure.ui.AddBloodPressureEntryButton
import com.lbthomas.healthcoach.features.bloodpressure.ui.AhaGuideLinkFooter
import com.lbthomas.healthcoach.features.bloodpressure.ui.BloodPressureEntryEditDialog
import com.lbthomas.healthcoach.features.bloodpressure.ui.BloodPressureEntryList
import com.lbthomas.healthcoach.features.bloodpressure.ui.BloodPressureViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.dsl.koinConfiguration

@Composable
fun BloodPressureView(
    showAddBloodPressureEntry: Boolean,
    modifier: Modifier = Modifier,
    isWideLayout: Boolean = false,
    onAddDismiss: () -> Unit = {},
    onRequestFocus: () -> Unit = {}
) {
    val viewModel = koinInject<BloodPressureViewModel>()
    val entries by viewModel.entries.collectAsState()

    var entryToDelete by remember { mutableStateOf<BloodPressureEntryData?>(null) }
    var targetToEdit by remember { mutableStateOf<Pair<LocalDate, LocalTime?>?>(null) }

    LaunchedEffect(showAddBloodPressureEntry) {
        if (showAddBloodPressureEntry) {
            targetToEdit = Pair(today, null)
        }
    }

    entryToDelete?.let { entry ->
        val timeSuffix = if (entry.hasTime) " at ${entry.time?.formatTime()}" else ""
        val message = "Are you sure you want to delete the blood pressure entry for ${entry.date}$timeSuffix (${entry.systolic}/${entry.diastolic} mmHg)?"
        DeleteConfirmationDialog(
            title = "Delete Entry",
            message = message,
            testTag = "delete_blood_pressure_confirmation_dialog",
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

    targetToEdit?.let { (date, time) ->
        BloodPressureEntryEditDialog(
            initialDate = date,
            initialTime = time,
            getEntriesForDateTime = { targetDate, targetTime ->
                val current = if (targetTime != null) {
                    entries.find { it.date == targetDate && it.hasTime && it.time?.hour == targetTime.hour && it.time?.minute == targetTime.minute }
                } else {
                    entries.find { it.date == targetDate && !it.hasTime }
                }

                val prior = if (targetTime != null) {
                    val targetTimeStr = targetTime.toString()
                    entries
                        .filter { it.date < targetDate || (it.date == targetDate && it.hasTime && (it.time?.toString() ?: "") < targetTimeStr) }
                        .maxWithOrNull(
                            compareBy<BloodPressureEntryData> { it.date }
                                .thenBy { it.time?.toString() ?: "" }
                        )
                } else {
                    entries
                        .filter { it.date < targetDate }
                        .maxWithOrNull(
                            compareBy<BloodPressureEntryData> { it.date }
                                .thenBy { it.time?.toString() ?: "" }
                        )
                }
                Pair(current, prior)
            },
            onConfirm = { updatedEntry ->
                if (updatedEntry.id.isEmpty() || updatedEntry.id == "0") {
                    viewModel.addEntry(
                        dateTime = updatedEntry.dateTime,
                        systolic = updatedEntry.systolic,
                        diastolic = updatedEntry.diastolic,
                        pulse = updatedEntry.pulse
                    )
                } else {
                    viewModel.updateEntry(updatedEntry)
                }
                targetToEdit = null
                if (showAddBloodPressureEntry) onAddDismiss()
                onRequestFocus()
            },
            onDismiss = {
                targetToEdit = null
                if (showAddBloodPressureEntry) onAddDismiss()
                onRequestFocus()
            }
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
                    text = "Blood Pressure",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AddBloodPressureEntryButton(
                    onClick = {
                        targetToEdit = Pair(today, null)
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            BloodPressureEntryList(
                entries = entries,
                isWideLayout = isWideLayout,
                onClickEntry = { entry ->
                    targetToEdit = Pair(entry.date, entry.time)
                },
                onDeleteEntry = { entry ->
                    entryToDelete = entry
                }
            )
        }

        AhaGuideLinkFooter(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp)
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xEADDFF,
    device = "spec:width=360dp,height=780dp,dpi=420"
)
@Composable
fun BloodPressureViewPreview() {
    KoinApplication(
        configuration = koinConfiguration(declaration = { modules(previewAppModule) }),
        content = {
            BloodPressureView(showAddBloodPressureEntry = false)
        }
    )
}
