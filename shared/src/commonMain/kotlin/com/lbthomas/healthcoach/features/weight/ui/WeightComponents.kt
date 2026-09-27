package com.lbthomas.healthcoach.features.weight.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.ui.Tooltip

internal object WeightViewDefaults {
    val MonthHeaderRowPadding = 32.dp
    val MonthSpacerPadding = 16.dp
    val MonthHeaderDividerPadding = 4.dp
    val RowDateWidth = 64.dp
    val ButtonSize = 36.dp
    val ButtonPadding = 8.dp
}

@Composable
internal fun AddWeightEntryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Tooltip("Add new weight\n(Ctrl + N or '+')", modifier = modifier) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier
                .padding(WeightViewDefaults.ButtonPadding)
                .size(WeightViewDefaults.ButtonSize),
            shape = FloatingActionButtonDefaults.smallShape,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add new weight",
            )
        }
    }
}
