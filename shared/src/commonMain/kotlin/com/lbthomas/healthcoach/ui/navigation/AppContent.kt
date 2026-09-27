package com.lbthomas.healthcoach.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.SelectedPage
import com.lbthomas.healthcoach.core.ui.HorizontalSplitPane
import com.lbthomas.healthcoach.features.bloodpressure.BloodPressureView
import com.lbthomas.healthcoach.features.graphs.GraphsView
import com.lbthomas.healthcoach.features.weight.WeightView

@Composable
fun AppContent(
    selectedPage: SelectedPage,
    isWideLayout: Boolean,
    splitterPosition: Float = 0.5f,
    onSplitterPositionChange: (Float) -> Unit = {},
    showAddWeightEntry: Boolean,
    onAddWeightDismiss: () -> Unit = {},
    showAddBloodPressureEntry: Boolean,
    onAddBloodPressureDismiss: () -> Unit = {},
    onRequestFocus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(if (isWideLayout) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background)
            .fillMaxSize()
            .then(if (isWideLayout) Modifier.padding(top = 5.dp) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isWideLayout && (selectedPage == SelectedPage.WeightView || selectedPage == SelectedPage.BloodPressureView)) {
            HorizontalSplitPane(
                modifier = Modifier.fillMaxSize(),
                initialFraction = splitterPosition,
                onFractionChange = onSplitterPositionChange,
                first = {
                    if (selectedPage == SelectedPage.WeightView) {
                        WeightView(
                            showAddWeightEntry = showAddWeightEntry,
                            isWideLayout = true,
                            onAddDismiss = onAddWeightDismiss,
                            onRequestFocus = onRequestFocus
                        )
                    } else {
                        BloodPressureView(
                            showAddBloodPressureEntry = showAddBloodPressureEntry,
                            isWideLayout = true,
                            onAddDismiss = onAddBloodPressureDismiss,
                            onRequestFocus = onRequestFocus
                        )
                    }
                },
                second = {
                    GraphsView()
                }
            )
        } else {
            when (selectedPage) {
                SelectedPage.WeightView -> WeightView(
                    showAddWeightEntry = showAddWeightEntry,
                    isWideLayout = false,
                    onAddDismiss = onAddWeightDismiss,
                    onRequestFocus = onRequestFocus
                )

                SelectedPage.BloodPressureView -> BloodPressureView(
                    showAddBloodPressureEntry = showAddBloodPressureEntry,
                    isWideLayout = false,
                    onAddDismiss = onAddBloodPressureDismiss,
                    onRequestFocus = onRequestFocus
                )

                SelectedPage.GraphsView -> GraphsView()
            }
        }
    }
}
