package com.lbthomas.healthcoach.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.SelectedPage

data class AppTab(
    val page: SelectedPage,
    val title: String,
    val icon: ImageVector
)

object AppNavigationDefaults {
    val Tabs = listOf(
        AppTab(SelectedPage.WeightView, "Weight", Icons.Default.Scale),
        AppTab(SelectedPage.JournalView, "Journal", Icons.Default.Restaurant),
        AppTab(SelectedPage.BloodPressureView, "Blood Pressure", Icons.Default.Favorite),
        AppTab(SelectedPage.GraphsView, "Graphs", Icons.AutoMirrored.Filled.ShowChart)
    )
    val TitleIconSize = 32.dp
}
