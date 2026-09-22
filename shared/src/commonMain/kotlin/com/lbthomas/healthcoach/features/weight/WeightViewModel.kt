package com.lbthomas.healthcoach.features.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lbthomas.healthcoach.features.weight.data.WeightEntryData
import com.lbthomas.healthcoach.features.weight.data.WeightRepository
import com.lbthomas.healthcoach.weight.data.WeightEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

open class WeightViewModel : ViewModel {
    val entries: StateFlow<List<WeightEntryData>>
    val repository: WeightRepository?

    constructor(repository: WeightRepository) : super() {
        entries = repository
            .observeAllEntries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        this.repository = repository
    }

    constructor(
        previewEntries: StateFlow<List<WeightEntryData>>
    ) : super() {
        entries = previewEntries
        this.repository = null
    }

    fun addEntry(date: LocalDate, weight: Double) {
        repository?.addEntry(date, weight)
    }

    fun updateEntry(entry: WeightEntryData) {
        repository?.updateEntry(entry)
    }

    fun deleteEntry(id: Long) {
        repository?.deleteEntry(id)
    }
}