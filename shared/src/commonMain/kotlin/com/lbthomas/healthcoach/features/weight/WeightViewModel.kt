package com.lbthomas.healthcoach.features.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import com.lbthomas.healthcoach.features.weight.data.WeightRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

open class WeightViewModel : ViewModel {
    val entries: StateFlow<List<WeightEntryData>>

    constructor(repository: WeightRepository) : super() {
        entries = repository
            .observeAllEntries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    constructor(
        previewEntries: StateFlow<List<WeightEntryData>>
    ) : super() {
        entries = previewEntries
    }
}