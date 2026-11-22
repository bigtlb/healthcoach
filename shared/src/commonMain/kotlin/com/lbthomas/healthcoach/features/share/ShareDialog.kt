package com.lbthomas.healthcoach.features.share

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.ShareImageType
import com.lbthomas.healthcoach.core.enums.WeightUnit
import com.lbthomas.healthcoach.core.theme.HealthCoachTheme
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import kotlinx.datetime.LocalDate

/**
 * Main Share Dialog.
 *
 * Assembles the following component composables (each in its own file with @Preview functions):
 * - [ShareTimeFrameSlider] — time period notch slider
 * - [ShareImageTypeSelector] — Text Only / Icon / Chart filter chips
 * - [ShowWeightValuesToggle] — privacy toggle switch
 * - [SharePreviewCard] — live text preview card
 * - [ShareWarningCard] — no-data warning
 * - [ShareActionButtons] — footer action buttons (Share, Copy, browser links, Mastodon config)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareDialog(
    viewModel: ShareDialogViewModel,
    onDismiss: () -> Unit,
    onShareFeedback: (String) -> Unit = {}
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.width(800.dp)
    ) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 400.dp, max = 700.dp)
                ) {
                    // Header with title and close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Share Weight Progress",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider()

                    // Scrollable body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        ShareTimeFrameSlider(
                            selectedTimeFrame = viewModel.selectedTimeFrame,
                            onTimeFrameChanged = viewModel::onTimeFrameChanged
                        )

                        ShareImageTypeSelector(
                            imageType = viewModel.imageType,
                            onImageTypeChanged = viewModel::onImageTypeChanged
                        )

                        ShowWeightValuesToggle(
                            showAbsoluteWeight = viewModel.showAbsoluteWeight,
                            onShowAbsoluteWeightChanged = viewModel::onShowAbsoluteWeightChanged
                        )

                        SharePreviewCard(
                            shareText = viewModel.shareText,
                            dateRange = viewModel.dateRange,
                            imageType = viewModel.imageType,
                            hasData = viewModel.hasData,
                            entries = viewModel.filteredEntries
                        )

                        if (!viewModel.hasData) {
                            ShareWarningCard("No weight data available for the selected time period.")
                        }
                    }

                    HorizontalDivider()

                    // Footer — action buttons (always visible)
                    ShareActionButtons(
                        viewModel = viewModel,
                        hasData = viewModel.hasData,
                        onShareFeedback = onShareFeedback
                    )
                }
            }
        }
    }
}

// ============================================================================
// SHARE PREVIEW MOCKUPS — ASCII ART DOCUMENTATION
// ============================================================================
//
// This section documents what each sharing option produces, so developers and
// users can see the expected output before sharing.
//
// --- Text Format Examples ---
//
// With absolute weights (showAbsoluteWeight = true):
//   "My 30-day weight progress: 185.2 → 178.6 lbs"
//
// Without absolute weights (showAbsoluteWeight = false):
//   "My 30-day weight progress: -6.6 lbs"
//
// Weight gain (positive change):
//   "My 7-day weight progress: +2.4 lbs"
//
// Kilogram units:
//   "My 30-day weight progress: -3.0 kg"
//
// --- 1. TWITTER/X SHARE (Browser) ---
// Opens: https://twitter.com/intent/tweet?text=<encoded_text>
// Browser shows the X/Twitter compose window with the text pre-filled.
// User must manually attach the chart/icon image via the [Media] button.
//
// --- 2. FACEBOOK SHARE (Browser) ---
// Opens: https://www.facebook.com/sharer/sharer.php?quote=<encoded_text>
// Facebook's sharer only supports a quote text. No image attachment via URL.
//
// --- 3. REDDIT SHARE (Browser) ---
// Opens: https://reddit.com/submit?url=&title=<encoded_text>
// Text is placed in the title field. User can switch to "Image" type.
//
// --- 4. MASTODON SHARE (Browser) ---
// Opens: https://<instance>/share?text=<encoded_text>
// Default instance: mastodon.social (configurable in dialog)
//
// --- 5. CHART IMAGE PREVIEW (Graph) ---
// When Image Type = "Chart", renders ~800×500 px weight line chart.
// Privacy mode hides Y-axis weight values. No goal line shown.
//
// --- 6. ICON IMAGE PREVIEW ---
// When Image Type = "Icon", renders ~800×500 px with centered app icon.
// Privacy mode hides the weight values, showing only the change.
//
// --- 7. ANDROID NATIVE SHARE ---
// Uses Intent.ACTION_SEND with EXTRA_TEXT + EXTRA_STREAM.
// Both text and image are shared together in one intent.
//
// --- 8. DESKTOP CLIPBOARD ---
// "Copy Text" → Plain text string ready to paste
// "Copy Image" → PNG image ready to paste (Ctrl+V)
// Works on Windows, macOS, and Linux (X11/Wayland via AWT)
//
// ============================================================================

// ============================================================================
// VIEWMODEL INTERFACE & MOCK DATA FOR PREVIEWS
// ============================================================================

/**
 * Interface that ShareDialog uses from ShareViewModel.
 * Allows previews to provide mock data without depending on Koin,
 * coroutines, or database access.
 */
interface ShareDialogViewModel {
    val selectedTimeFrame: GraphTimeFrame
    val showAbsoluteWeight: Boolean
    val imageType: ShareImageType
    val mastodonInstance: String
    val shareText: String
    val dateRange: String
    val filteredEntries: List<WeightEntryData>
    val hasData: Boolean
    fun onTimeFrameChanged(timeFrame: GraphTimeFrame)
    fun onShowAbsoluteWeightChanged(show: Boolean)
    fun onImageTypeChanged(type: ShareImageType)
    fun onMastodonInstanceChanged(instance: String)
    fun renderImage(): androidx.compose.ui.graphics.ImageBitmap?
}

private val previewWeightEntries = listOf(
    WeightEntryData("1", LocalDate(2023, 10, 15), 83.5),
    WeightEntryData("2", LocalDate(2023, 10, 22), 82.8),
    WeightEntryData("3", LocalDate(2023, 10, 29), 82.1),
    WeightEntryData("4", LocalDate(2023, 11, 5), 81.4),
    WeightEntryData("5", LocalDate(2023, 11, 12), 80.7),
    WeightEntryData("6", LocalDate(2023, 11, 19), 80.0),
    WeightEntryData("7", LocalDate(2023, 11, 21), 79.4)
)

private class PreviewShareViewModel(
    override var selectedTimeFrame: GraphTimeFrame,
    override var showAbsoluteWeight: Boolean,
    override var imageType: ShareImageType,
    override var mastodonInstance: String,
    override var shareText: String,
    override var dateRange: String,
    override var filteredEntries: List<WeightEntryData>,
    override var hasData: Boolean
) : ShareDialogViewModel {
    override fun onTimeFrameChanged(timeFrame: GraphTimeFrame) {
        selectedTimeFrame = timeFrame
    }
    override fun onShowAbsoluteWeightChanged(show: Boolean) {
        showAbsoluteWeight = show
    }
    override fun onImageTypeChanged(type: ShareImageType) {
        imageType = type
    }
    override fun onMastodonInstanceChanged(instance: String) {
        mastodonInstance = instance
    }
    override fun renderImage(): androidx.compose.ui.graphics.ImageBitmap? = null
}

private fun createPreviewViewModel(
    timeFrame: GraphTimeFrame = GraphTimeFrame.ONE_MONTH,
    imageType: ShareImageType = ShareImageType.CHART,
    showAbsoluteWeight: Boolean = true
): ShareDialogViewModel {
    val weightUnit = WeightUnit.US
    val shareText = com.lbthomas.healthcoach.core.share.ShareTextFormatter.format(
        entries = previewWeightEntries,
        allEntries = previewWeightEntries,
        timeFrame = timeFrame,
        weightUnit = weightUnit,
        showAbsoluteWeight = showAbsoluteWeight
    )
    return PreviewShareViewModel(
        selectedTimeFrame = timeFrame,
        showAbsoluteWeight = showAbsoluteWeight,
        imageType = imageType,
        mastodonInstance = "mastodon.social",
        shareText = shareText.first,
        dateRange = shareText.second,
        filteredEntries = previewWeightEntries,
        hasData = true
    )
}

/**
 * Renders the ShareDialog content directly without the dialog wrapper,
 * so it's visible in Android Studio @Preview mode.
 */
@Composable
fun ShareDialogContent(
    viewModel: ShareDialogViewModel,
    onDismiss: () -> Unit,
    onShareFeedback: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 400.dp, max = 600.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Share Weight Progress",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
            }
        }

        HorizontalDivider()

        // Scrollable body
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ShareTimeFrameSlider(
                selectedTimeFrame = viewModel.selectedTimeFrame,
                onTimeFrameChanged = viewModel::onTimeFrameChanged
            )
            ShareImageTypeSelector(
                imageType = viewModel.imageType,
                onImageTypeChanged = viewModel::onImageTypeChanged
            )
            ShowWeightValuesToggle(
                showAbsoluteWeight = viewModel.showAbsoluteWeight,
                onShowAbsoluteWeightChanged = viewModel::onShowAbsoluteWeightChanged
            )
            SharePreviewCard(
                shareText = viewModel.shareText,
                dateRange = viewModel.dateRange,
                imageType = viewModel.imageType,
                hasData = viewModel.hasData,
                entries = viewModel.filteredEntries
            )
            if (!viewModel.hasData) {
                ShareWarningCard("No weight data available for the selected time period.")
            }
        }

        HorizontalDivider()

        // Footer
        ShareActionButtons(
            viewModel = viewModel,
            hasData = viewModel.hasData,
            onShareFeedback = onShareFeedback
        )
    }
}

// ============================================================================
// FULL DIALOG PREVIEWS
// ============================================================================

@Preview(name = "Share Dialog - Default")
@Composable
fun ShareDialogPreview() {
    HealthCoachTheme {
        val viewModel = createPreviewViewModel()
        Surface {
            ShareDialogContent(viewModel = viewModel, onDismiss = {})
        }
    }
}

@Preview(name = "Share Dialog - Privacy Mode")
@Composable
fun ShareDialogPrivacyModePreview() {
    HealthCoachTheme {
        val viewModel = createPreviewViewModel(showAbsoluteWeight = false)
        Surface {
            ShareDialogContent(viewModel = viewModel, onDismiss = {})
        }
    }
}

@Preview(name = "Share Dialog - Icon Image Type")
@Composable
fun ShareDialogIconImagePreview() {
    HealthCoachTheme {
        val viewModel = createPreviewViewModel(imageType = ShareImageType.ICON)
        Surface {
            ShareDialogContent(viewModel = viewModel, onDismiss = {})
        }
    }
}

@Preview(name = "Share Dialog - Chart Image Type")
@Composable
fun ShareDialogChartImagePreview() {
    HealthCoachTheme {
        val viewModel = createPreviewViewModel(imageType = ShareImageType.CHART)
        Surface {
            ShareDialogContent(viewModel = viewModel, onDismiss = {})
        }
    }
}

@Preview(name = "Share Dialog - Text Only")
@Composable
fun ShareDialogTextOnlyPreview() {
    HealthCoachTheme {
        val viewModel = createPreviewViewModel(imageType = ShareImageType.NONE)
        Surface {
            ShareDialogContent(viewModel = viewModel, onDismiss = {})
        }
    }
}

@Preview(name = "Share Dialog - 1 Week Time Frame")
@Composable
fun ShareDialogOneWeekPreview() {
    HealthCoachTheme {
        val viewModel = createPreviewViewModel(timeFrame = GraphTimeFrame.ONE_WEEK)
        Surface {
            ShareDialogContent(viewModel = viewModel, onDismiss = {})
        }
    }
}

@Preview(name = "Share Dialog - 1 Year Time Frame")
@Composable
fun ShareDialogOneYearPreview() {
    HealthCoachTheme {
        val viewModel = createPreviewViewModel(timeFrame = GraphTimeFrame.ONE_YEAR)
        Surface {
            ShareDialogContent(viewModel = viewModel, onDismiss = {})
        }
    }
}

@Preview(name = "Share Dialog - All Time")
@Composable
fun ShareDialogAllTimePreview() {
    HealthCoachTheme {
        val viewModel = createPreviewViewModel(timeFrame = GraphTimeFrame.ALL)
        Surface {
            ShareDialogContent(viewModel = viewModel, onDismiss = {})
        }
    }
}
