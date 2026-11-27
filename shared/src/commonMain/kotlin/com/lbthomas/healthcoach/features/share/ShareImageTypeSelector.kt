package com.lbthomas.healthcoach.features.share

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.ShareImageType
import com.lbthomas.healthcoach.core.theme.HealthCoachTheme

@Composable
fun ShareImageTypeSelector(
    imageType: ShareImageType,
    onImageTypeChanged: (ShareImageType) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        val types = listOf(
            ShareImageType.NONE to "Text",
            ShareImageType.ICON to "Icon",
            ShareImageType.CHART to "Chart"
        )
        types.forEachIndexed { index, (type, label) ->
            SegmentedButton(
                selected = imageType == type,
                onClick = { onImageTypeChanged(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// ============================================================================
// PREVIEWS
// ============================================================================

@Preview(name = "Image Type - Text Only Selected")
@Composable
fun ShareImageTypeSelectorPreviewTextOnly() {
    HealthCoachTheme {
        Surface {
            ShareImageTypeSelector(
                imageType = ShareImageType.NONE,
                onImageTypeChanged = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Image Type - Icon Selected")
@Composable
fun ShareImageTypeSelectorPreviewIcon() {
    HealthCoachTheme {
        Surface {
            ShareImageTypeSelector(
                imageType = ShareImageType.ICON,
                onImageTypeChanged = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Image Type - Chart Selected")
@Composable
fun ShareImageTypeSelectorPreviewChart() {
    HealthCoachTheme {
        Surface {
            ShareImageTypeSelector(
                imageType = ShareImageType.CHART,
                onImageTypeChanged = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
