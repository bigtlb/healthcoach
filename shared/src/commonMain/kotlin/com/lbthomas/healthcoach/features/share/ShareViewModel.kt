package com.lbthomas.healthcoach.features.share

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lbthomas.healthcoach.core.enums.GraphTimeFrame
import com.lbthomas.healthcoach.core.enums.ShareImageType
import com.lbthomas.healthcoach.core.share.ChartImageRenderer
import com.lbthomas.healthcoach.core.share.ShareTextFormatter
import com.lbthomas.healthcoach.features.graphs.data.buildWeightGraphEntries
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.weight.WeightViewModel
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for the share dialog.
 */
class ShareViewModel(
    private val weightViewModel: WeightViewModel,
    private val settingsViewModel: SettingsViewModel
) : ViewModel(), com.lbthomas.healthcoach.features.share.ShareDialogViewModel {
    override var selectedTimeFrame: GraphTimeFrame by mutableStateOf(settingsViewModel.settings.value.sharing.selectedTimeFrame)
        private set

    override var showAbsoluteWeight: Boolean by mutableStateOf(settingsViewModel.settings.value.sharing.showAbsoluteWeight)
        private set

    override var imageType: ShareImageType by mutableStateOf(settingsViewModel.settings.value.sharing.imageType)
        private set

    override var mastodonInstance: String by mutableStateOf(settingsViewModel.settings.value.sharing.mastodonInstance)
        private set

    override var shareText by mutableStateOf("")
        private set

    override var dateRange by mutableStateOf("")
        private set

    override var filteredEntries by mutableStateOf<List<WeightEntryData>>(emptyList())
        private set

    override var hasData by mutableStateOf(false)
        private set

    init {
        // Load data immediately from current state
        updateData()
        // Also observe for changes (e.g., if data loads asynchronously)
        viewModelScope.launch {
            weightViewModel.entries.collectLatest { entries ->
                updateData()
            }
        }
    }

    /**
     * Refresh data manually — called when the dialog opens to ensure we have the latest data.
     */
    fun refreshData() {
        updateData()
    }

    override fun onTimeFrameChanged(timeFrame: GraphTimeFrame) {
        selectedTimeFrame = timeFrame
        settingsViewModel.setShareTimeFrame(timeFrame)
        updateData()
    }

    override fun onShowAbsoluteWeightChanged(show: Boolean) {
        showAbsoluteWeight = show
        settingsViewModel.setShowAbsoluteWeight(show)
        updateData()
    }

    override fun onImageTypeChanged(type: ShareImageType) {
        imageType = type
        settingsViewModel.setShareImageType(type)
        updateData()
    }

    override fun onMastodonInstanceChanged(instance: String) {
        mastodonInstance = instance
        settingsViewModel.setMastodonInstance(instance)
        updateData()
    }

    override fun renderImage(): androidx.compose.ui.graphics.ImageBitmap? {
        if (filteredEntries.isEmpty() || imageType == ShareImageType.NONE) return null
        val weightUnit = settingsViewModel.settings.value.weight.unit
        val allEntries = weightViewModel.entries.value
        return ChartImageRenderer.render(
            entries = filteredEntries,
            allEntries = allEntries,
            timeFrame = selectedTimeFrame,
            weightUnit = weightUnit,
            imageType = imageType,
            showAbsoluteWeight = showAbsoluteWeight
        )
    }

    private fun updateData() {
        val allEntries = weightViewModel.entries.value
        val weightUnit = settingsViewModel.settings.value.weight.unit

        val graphEntries = buildWeightGraphEntries(
            rawEntries = allEntries,
            timeFrame = selectedTimeFrame
        )
        filteredEntries = graphEntries.entries
        hasData = filteredEntries.isNotEmpty()

        val formatted = ShareTextFormatter.format(
            entries = filteredEntries,
            allEntries = allEntries,
            timeFrame = selectedTimeFrame,
            weightUnit = weightUnit,
            showAbsoluteWeight = showAbsoluteWeight
        )
        shareText = formatted.first
        dateRange = formatted.second
    }
}
