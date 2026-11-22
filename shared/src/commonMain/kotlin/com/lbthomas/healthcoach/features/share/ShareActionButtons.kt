package com.lbthomas.healthcoach.features.share

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lbthomas.healthcoach.core.enums.ShareImageType
import com.lbthomas.healthcoach.core.share.ShareApi
import com.lbthomas.healthcoach.core.share.buildXUrl
import com.lbthomas.healthcoach.core.share.buildFacebookUrl
import com.lbthomas.healthcoach.core.share.buildRedditUrl
import com.lbthomas.healthcoach.core.share.buildMastodonUrl
import com.lbthomas.healthcoach.core.theme.HealthCoachTheme

/**
 * Data class for a share platform option.
 */
data class SharePlatform(
    val id: String,
    val label: String,
    val buildUrl: (String, String) -> String
)

/**
 * Combine progress text with date range on a second line.
 */
private fun buildFullShareText(shareText: String, dateRange: String): String {
    return if (dateRange.isNotEmpty()) {
        "$shareText\n$dateRange"
    } else {
        shareText
    }
}

/**
 * Available browser share platforms. Add new platforms here to extend the list.
 */
private val sharePlatforms = listOf(
    SharePlatform("x", "X") { text, _ -> buildXUrl(text) },
    SharePlatform("facebook", "Facebook") { text, _ -> buildFacebookUrl(text) },
    SharePlatform("reddit", "Reddit") { text, _ -> buildRedditUrl(text) },
    SharePlatform("mastodon", "Mastodon") { text, instance -> buildMastodonUrl(text, instance) }
)

@Composable
fun ShareActionButtons(
    viewModel: ShareDialogViewModel,
    hasData: Boolean,
    onShareFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPlatform by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Primary action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Share button (primary action) — opens native share sheet on Android,
            // copies to clipboard on desktop
            Button(
                onClick = {
                    val image = viewModel.renderImage()
                    val fullText = buildFullShareText(viewModel.shareText, viewModel.dateRange)
                    val message = if (image != null) {
                        ShareApi.shareTextAndImage(fullText, image)
                    } else {
                        ShareApi.shareText(fullText)
                    }
                    message?.let(onShareFeedback)
                },
                enabled = hasData,
                modifier = Modifier.weight(1f)
            ) {
                Text("Share")
            }

            // Copy Text button
            OutlinedButton(
                onClick = {
                    val fullText = buildFullShareText(viewModel.shareText, viewModel.dateRange)
                    ShareApi.copyTextToClipboard(fullText)?.let(onShareFeedback)
                },
                enabled = hasData,
                modifier = Modifier.weight(1f)
            ) {
                Text("Copy Text")
            }

            // Copy Image button (only if not Text Only)
            if (viewModel.imageType != ShareImageType.NONE) {
                OutlinedButton(
                    onClick = {
                        val image = viewModel.renderImage()
                        if (image != null) {
                            ShareApi.copyImageToClipboard(image)?.let(onShareFeedback)
                        }
                    },
                    enabled = hasData,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Copy Image")
                }
            }
        }

        // Browser share section — horizontally scrollable chips
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Share via browser",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowForward,
                    contentDescription = "Scroll for more options",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(sharePlatforms) { platform ->
                    FilterChip(
                        onClick = {
                            if (platform.id == "mastodon") {
                                // Toggle Mastodon config panel
                                selectedPlatform = if (selectedPlatform == "mastodon") null else "mastodon"
                            } else {
                                val fullText = buildFullShareText(viewModel.shareText, viewModel.dateRange)
                                val url = platform.buildUrl(fullText, viewModel.mastodonInstance)
                                ShareApi.openShareUrl(url)?.let(onShareFeedback)
                            }
                        },
                        label = { Text(platform.label) },
                        selected = selectedPlatform == platform.id,
                        enabled = hasData
                    )
                }
            }

            // Mastodon instance config — shown only when Mastodon chip is selected
            if (selectedPlatform == "mastodon") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Mastodon Instance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = viewModel.mastodonInstance,
                            onValueChange = viewModel::onMastodonInstanceChanged,
                            placeholder = { Text("mastodon.social") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalIconButton(
                            onClick = {
                                val fullText = buildFullShareText(viewModel.shareText, viewModel.dateRange)
                                val url = buildMastodonUrl(fullText, viewModel.mastodonInstance)
                                ShareApi.openShareUrl(url)?.let(onShareFeedback)
                                selectedPlatform = null
                            },
                            enabled = hasData && viewModel.mastodonInstance.isNotBlank()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Share to Mastodon")
                        }
                    }
                    Text(
                        text = "Opens a new post on your Mastodon server",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ============================================================================
// PREVIEWS
// ============================================================================

private object PreviewShareActionViewModel : ShareDialogViewModel {
    override var selectedTimeFrame: com.lbthomas.healthcoach.core.enums.GraphTimeFrame =
        com.lbthomas.healthcoach.core.enums.GraphTimeFrame.ONE_MONTH
    override var showAbsoluteWeight: Boolean = true
    override var imageType: ShareImageType = ShareImageType.CHART
    override var mastodonInstance: String = "mastodon.social"
    override var shareText: String = "My 30-day weight progress: 185.2 → 178.6 lbs"
    override var dateRange: String = "Oct 22, 2023 - Nov 21, 2023"
    override var filteredEntries: List<com.lbthomas.healthcoach.features.weight.data.WeightEntryData> = emptyList()
    override var hasData: Boolean = true
    override fun onTimeFrameChanged(timeFrame: com.lbthomas.healthcoach.core.enums.GraphTimeFrame) {}
    override fun onShowAbsoluteWeightChanged(show: Boolean) {}
    override fun onImageTypeChanged(type: ShareImageType) {}
    override fun onMastodonInstanceChanged(instance: String) {}
    override fun renderImage(): androidx.compose.ui.graphics.ImageBitmap? = null
}

private object PreviewShareActionViewModelNoImage : ShareDialogViewModel {
    override var selectedTimeFrame: com.lbthomas.healthcoach.core.enums.GraphTimeFrame =
        com.lbthomas.healthcoach.core.enums.GraphTimeFrame.ONE_MONTH
    override var showAbsoluteWeight: Boolean = true
    override var imageType: ShareImageType = ShareImageType.NONE
    override var mastodonInstance: String = "mastodon.social"
    override var shareText: String = "My 30-day weight progress: 185.2 → 178.6 lbs"
    override var dateRange: String = "Oct 22, 2023 - Nov 21, 2023"
    override var filteredEntries: List<com.lbthomas.healthcoach.features.weight.data.WeightEntryData> = emptyList()
    override var hasData: Boolean = true
    override fun onTimeFrameChanged(timeFrame: com.lbthomas.healthcoach.core.enums.GraphTimeFrame) {}
    override fun onShowAbsoluteWeightChanged(show: Boolean) {}
    override fun onImageTypeChanged(type: ShareImageType) {}
    override fun onMastodonInstanceChanged(instance: String) {}
    override fun renderImage(): androidx.compose.ui.graphics.ImageBitmap? = null
}

private object PreviewShareActionViewModelDisabled : ShareDialogViewModel {
    override var selectedTimeFrame: com.lbthomas.healthcoach.core.enums.GraphTimeFrame =
        com.lbthomas.healthcoach.core.enums.GraphTimeFrame.ONE_MONTH
    override var showAbsoluteWeight: Boolean = true
    override var imageType: ShareImageType = ShareImageType.CHART
    override var mastodonInstance: String = "mastodon.social"
    override var shareText: String = ""
    override var dateRange: String = ""
    override var filteredEntries: List<com.lbthomas.healthcoach.features.weight.data.WeightEntryData> = emptyList()
    override var hasData: Boolean = false
    override fun onTimeFrameChanged(timeFrame: com.lbthomas.healthcoach.core.enums.GraphTimeFrame) {}
    override fun onShowAbsoluteWeightChanged(show: Boolean) {}
    override fun onImageTypeChanged(type: ShareImageType) {}
    override fun onMastodonInstanceChanged(instance: String) {}
    override fun renderImage(): androidx.compose.ui.graphics.ImageBitmap? = null
}

@Preview(name = "Action Buttons - With Image (Chart)")
@Composable
fun ShareActionButtonsPreviewWithImage() {
    HealthCoachTheme {
        Surface {
            ShareActionButtons(
                viewModel = PreviewShareActionViewModel,
                hasData = true,
                onShareFeedback = {}
            )
        }
    }
}

@Preview(name = "Action Buttons - Text Only (no Copy Image)")
@Composable
fun ShareActionButtonsPreviewTextOnly() {
    HealthCoachTheme {
        Surface {
            ShareActionButtons(
                viewModel = PreviewShareActionViewModelNoImage,
                hasData = true,
                onShareFeedback = {}
            )
        }
    }
}

@Preview(name = "Action Buttons - Disabled (no data)")
@Composable
fun ShareActionButtonsPreviewDisabled() {
    HealthCoachTheme {
        Surface {
            ShareActionButtons(
                viewModel = PreviewShareActionViewModelDisabled,
                hasData = false,
                onShareFeedback = {}
            )
        }
    }
}
