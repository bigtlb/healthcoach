package com.lbthomas.healthcoach.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.SelectedPage
import com.lbthomas.healthcoach.core.ui.Tooltip
import com.lbthomas.healthcoach.features.sync.SyncActionButton
import com.lbthomas.healthcoach.features.sync.SyncViewModel
import healthcoach.shared.generated.resources.Res
import healthcoach.shared.generated.resources.scales
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    selectedPage: SelectedPage,
    isWideLayout: Boolean = false,
    syncViewModel: SyncViewModel? = null,
    onSelection: (SelectedPage) -> Unit,
    onShowSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            if (isWideLayout) {
                Icon(
                    painter = painterResource(Res.drawable.scales),
                    contentDescription = "HealthCoach",
                    modifier = Modifier.size(AppNavigationDefaults.TitleIconSize),
                    tint = Color.Unspecified
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(Res.drawable.scales),
                        contentDescription = "HealthCoach",
                        modifier = Modifier.size(24.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = AppNavigationDefaults.Tabs.firstOrNull { it.page == selectedPage }?.title ?: "Health Coach",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        actions = {
            if (isWideLayout) {
                AppActionButtons(
                    onSelection = onSelection,
                    selectedPage = selectedPage,
                    isWideLayout = true,
                    syncViewModel = syncViewModel,
                    onShowSettings = onShowSettings
                )
            } else {
                if (syncViewModel != null) {
                    SyncActionButton(syncViewModel = syncViewModel)
                }
                SettingsButton(onShowSettings)
            }
        }
    )
}

@Composable
internal fun AppActionButtons(
    onSelection: (SelectedPage) -> Unit,
    selectedPage: SelectedPage,
    isWideLayout: Boolean = false,
    syncViewModel: SyncViewModel? = null,
    onShowSettings: () -> Unit
) {
    val tabs = if (isWideLayout) {
        AppNavigationDefaults.Tabs.filter { it.page != SelectedPage.GraphsView }
    } else {
        AppNavigationDefaults.Tabs
    }

    tabs.forEach { tab ->
        AppFeatureButton(
            tabTitle = tab.title,
            onSelection = { onSelection(tab.page) },
            isSelected = selectedPage == tab.page,
            tabIcon = tab.icon
        )
    }

    if (syncViewModel != null) {
        SyncActionButton(syncViewModel = syncViewModel)
    }
    SettingsButton(onShowSettings)
}

@Composable
internal fun SettingsButton(onShowSettings: () -> Unit) {
    Tooltip("Settings (Ctrl + S or ,)") {
        IconButton(
            onClick = { onShowSettings() },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings"
            )
        }
    }
}

@Composable
internal fun AppFeatureButton(
    tabTitle: String,
    onSelection: () -> Unit,
    isSelected: Boolean,
    tabIcon: ImageVector
) {
    Tooltip("$tabTitle (Ctrl + ${tabTitle.first()})") {
        IconToggleButton(
            onCheckedChange = { checked -> if (checked) onSelection() },
            checked = isSelected,
            colors = IconButtonDefaults.iconToggleButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                checkedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                checkedContentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(
                imageVector = tabIcon,
                contentDescription = tabTitle
            )
        }
    }
}

@Preview
@Composable
private fun AppTopBarPreview() {
    Surface {
        AppTopBar(
            selectedPage = SelectedPage.WeightView,
            isWideLayout = false,
            syncViewModel = null,
            onSelection = {},
            onShowSettings = {}
        )
    }
}
