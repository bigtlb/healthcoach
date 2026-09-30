package com.lbthomas.healthcoach.core.enums

import kotlinx.serialization.Serializable

@Serializable
enum class SelectedPage {
    WeightView,
    JournalView,
    BloodPressureView,
    GraphsView
}

typealias AppPage = SelectedPage
