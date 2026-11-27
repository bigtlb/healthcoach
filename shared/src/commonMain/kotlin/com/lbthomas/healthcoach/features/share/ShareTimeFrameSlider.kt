package com.lbthomas.healthcoach.features.share

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.theme.HealthCoachTheme

/**
 * Ordered list of GraphTimeFrame entries for the slider (shortest to longest).
 */
private val sliderTimeFrames = listOf(
    GraphTimeFrame.ONE_DAY,
    GraphTimeFrame.THREE_DAYS,
    GraphTimeFrame.ONE_WEEK,
    GraphTimeFrame.TWO_WEEKS,
    GraphTimeFrame.ONE_MONTH,
    GraphTimeFrame.TWO_MONTHS,
    GraphTimeFrame.THREE_MONTHS,
    GraphTimeFrame.SIX_MONTHS,
    GraphTimeFrame.YEAR_TO_DATE,
    GraphTimeFrame.ONE_YEAR,
    GraphTimeFrame.TWO_YEAR,
    GraphTimeFrame.ALL
)

@Composable
fun ShareTimeFrameSlider(
    selectedTimeFrame: GraphTimeFrame,
    onTimeFrameChanged: (GraphTimeFrame) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val selectedIndex = sliderTimeFrames.indexOf(selectedTimeFrame)
        var sliderPosition by remember { mutableFloatStateOf(selectedIndex.toFloat()) }

        LaunchedEffect(selectedIndex) {
            sliderPosition = selectedIndex.toFloat()
        }

        // Compute the current label from the live slider position, not the parent's state,
        // so the label updates in real-time while dragging.
        val currentIndex = sliderPosition.toInt().coerceIn(0, sliderTimeFrames.size - 1)
        val currentLabel = sliderTimeFrames[currentIndex].label

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Time Frame",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = currentLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = sliderPosition,
            onValueChange = { newValue ->
                sliderPosition = newValue
            },
            onValueChangeFinished = {
                val index = sliderPosition.toInt().coerceIn(0, sliderTimeFrames.size - 1)
                onTimeFrameChanged(sliderTimeFrames[index])
            },
            valueRange = 0f..(sliderTimeFrames.size - 1).toFloat(),
            steps = sliderTimeFrames.size - 2
        )
    }
}

// ============================================================================
// PREVIEWS
// ============================================================================

@Preview(name = "Time Frame - 1 Day")
@Composable
fun ShareTimeFrameSliderPreview1Day() {
    HealthCoachTheme {
        Surface {
            ShareTimeFrameSlider(
                selectedTimeFrame = GraphTimeFrame.ONE_DAY,
                onTimeFrameChanged = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Time Frame - 3 Days")
@Composable
fun ShareTimeFrameSliderPreview3Days() {
    HealthCoachTheme {
        Surface {
            ShareTimeFrameSlider(
                selectedTimeFrame = GraphTimeFrame.THREE_DAYS,
                onTimeFrameChanged = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Time Frame - 1 Week")
@Composable
fun ShareTimeFrameSliderPreview1Week() {
    HealthCoachTheme {
        Surface {
            ShareTimeFrameSlider(
                selectedTimeFrame = GraphTimeFrame.ONE_WEEK,
                onTimeFrameChanged = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Time Frame - 1 Month")
@Composable
fun ShareTimeFrameSliderPreview1Month() {
    HealthCoachTheme {
        Surface {
            ShareTimeFrameSlider(
                selectedTimeFrame = GraphTimeFrame.ONE_MONTH,
                onTimeFrameChanged = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Time Frame - 3 Months")
@Composable
fun ShareTimeFrameSliderPreview3Months() {
    HealthCoachTheme {
        Surface {
            ShareTimeFrameSlider(
                selectedTimeFrame = GraphTimeFrame.THREE_MONTHS,
                onTimeFrameChanged = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Time Frame - 1 Year")
@Composable
fun ShareTimeFrameSliderPreview1Year() {
    HealthCoachTheme {
        Surface {
            ShareTimeFrameSlider(
                selectedTimeFrame = GraphTimeFrame.ONE_YEAR,
                onTimeFrameChanged = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Time Frame - YTD")
@Composable
fun ShareTimeFrameSliderPreviewYTD() {
    HealthCoachTheme {
        Surface {
            ShareTimeFrameSlider(
                selectedTimeFrame = GraphTimeFrame.YEAR_TO_DATE,
                onTimeFrameChanged = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "Time Frame - All Time")
@Composable
fun ShareTimeFrameSliderPreviewAll() {
    HealthCoachTheme {
        Surface {
            ShareTimeFrameSlider(
                selectedTimeFrame = GraphTimeFrame.ALL,
                onTimeFrameChanged = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
