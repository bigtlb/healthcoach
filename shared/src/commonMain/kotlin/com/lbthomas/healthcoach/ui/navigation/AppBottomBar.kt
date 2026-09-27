package com.lbthomas.healthcoach.ui.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.lbthomas.healthcoach.core.enums.SelectedPage
import com.lbthomas.healthcoach.core.ui.Tooltip

@Composable
fun AppBottomBar(
    selectedPage: SelectedPage,
    onSelection: (SelectedPage) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        AppNavigationDefaults.Tabs.forEach { tab ->
            NavigationBarItem(
                selected = selectedPage == tab.page,
                onClick = { onSelection(tab.page) },
                icon = {
                    Tooltip("${tab.title} (Ctrl + ${tab.title.first()})") {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title
                        )
                    }
                },
                label = {
                    Text(
                        text = tab.title,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            )
        }
    }
}

@Preview
@Composable
private fun AppBottomBarPreview() {
    Surface {
        AppBottomBar(
            selectedPage = SelectedPage.WeightView,
            onSelection = {}
        )
    }
}
