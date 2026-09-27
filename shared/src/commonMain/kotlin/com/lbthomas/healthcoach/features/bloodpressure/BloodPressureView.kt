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
import com.lbthomas.healthcoach.core.utils.today
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureEntryData
import com.lbthomas.healthcoach.features.bloodpressure.ui.AddBloodPressureEntryButton
import com.lbthomas.healthcoach.features.bloodpressure.ui.AhaGuideLinkFooter
import com.lbthomas.healthcoach.features.bloodpressure.ui.BloodPressureEntryList
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
    var entryToEdit by remember { mutableStateOf<BloodPressureEntryData?>(null) }

    LaunchedEffect(showAddBloodPressureEntry) {
        if (showAddBloodPressureEntry) {
            entryToEdit = BloodPressureEntryData(
                id = "",
                dateTime = today.toString(),
                systolic = 0,
                diastolic = 0,
                pulse = null
            )
        }
    }

    entryToDelete?.let { entry ->
        DeleteBloodPressureConfirmation(
            entry = entry,
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

    entryToEdit?.let { entry ->
        BloodPressureEntryEditDialog(
            entry = entry,
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
                entryToEdit = null
                if (showAddBloodPressureEntry) onAddDismiss()
                onRequestFocus()
            },
            onDismiss = {
                entryToEdit = null
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
                        entryToEdit = BloodPressureEntryData(
                            id = "",
                            dateTime = today.toString(),
                            systolic = 0,
                            diastolic = 0,
                            pulse = null
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            BloodPressureEntryList(
                entries = entries,
                isWideLayout = isWideLayout,
                onClickEntry = { entry ->
                    entryToEdit = entry
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
