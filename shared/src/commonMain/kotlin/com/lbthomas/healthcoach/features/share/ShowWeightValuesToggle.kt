package com.lbthomas.healthcoach.features.share

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.theme.HealthCoachTheme

@Composable
fun ShowWeightValuesToggle(
    showAbsoluteWeight: Boolean,
    onShowAbsoluteWeightChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Show weight values",
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = showAbsoluteWeight,
            onCheckedChange = onShowAbsoluteWeightChanged
        )
    }
}

// ============================================================================
// PREVIEWS
// ============================================================================

@Preview(name = "Weight Values Toggle - On")
@Composable
fun ShowWeightValuesTogglePreviewOn() {
    HealthCoachTheme {
        Surface {
            ShowWeightValuesToggle(
                showAbsoluteWeight = true,
                onShowAbsoluteWeightChanged = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Weight Values Toggle - Off")
@Composable
fun ShowWeightValuesTogglePreviewOff() {
    HealthCoachTheme {
        Surface {
            ShowWeightValuesToggle(
                showAbsoluteWeight = false,
                onShowAbsoluteWeightChanged = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
