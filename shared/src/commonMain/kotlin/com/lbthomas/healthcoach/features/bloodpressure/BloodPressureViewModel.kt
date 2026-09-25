package com.lbthomas.healthcoach.features.bloodpressure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureEntryData
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

open class BloodPressureViewModel : ViewModel {
    val entries: StateFlow<List<BloodPressureEntryData>>
    val repository: BloodPressureRepository?

    constructor(repository: BloodPressureRepository) : super() {
        entries = repository
            .observeAllEntries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        this.repository = repository
    }

    constructor(
        previewEntries: StateFlow<List<BloodPressureEntryData>>
    ) : super() {
        entries = previewEntries
        this.repository = null
    }

    fun addEntry(dateTime: String, systolic: Int, diastolic: Int, pulse: Int?): String? {
        return repository?.addEntry(dateTime, systolic, diastolic, pulse)
    }

    fun updateEntry(entry: BloodPressureEntryData) {
        repository?.updateEntry(entry)
    }

    fun deleteEntry(id: String) {
        repository?.deleteEntry(id)
    }
}
