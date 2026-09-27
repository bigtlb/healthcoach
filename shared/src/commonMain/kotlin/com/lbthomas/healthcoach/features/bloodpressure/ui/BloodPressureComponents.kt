package com.lbthomas.healthcoach.features.bloodpressure.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.ui.Tooltip

internal object BloodPressureViewDefaults {
    val MonthHeaderRowPadding = 32.dp
    val MonthSpacerPadding = 16.dp
    val MonthHeaderDividerPadding = 4.dp
    val RowDateWidth = 68.dp
    val ButtonSize = 36.dp
    val ButtonPadding = 8.dp
    const val AHA_GUIDE_URL =
        "https://www.heart.org/en/health-topics/high-blood-pressure/understanding-blood-pressure-readings"
}

@Composable
internal fun AddBloodPressureEntryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Tooltip("Add new blood pressure\n(Ctrl + N or '+')", modifier = modifier) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .padding(BloodPressureViewDefaults.ButtonPadding)
                .size(BloodPressureViewDefaults.ButtonSize),
            shape = FloatingActionButtonDefaults.smallShape,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add new blood pressure",
            )
        }
    }
}

@Composable
internal fun AhaGuideLinkFooter(
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Tooltip(tooltip = "Open AHA Blood Pressure Categories Guide") {
        Row(
            modifier = modifier
                .clickable {
                    uriHandler.openUri(BloodPressureViewDefaults.AHA_GUIDE_URL)
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Blood Pressure Guide",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = "Open AHA Guide",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
