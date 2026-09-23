package com.lbthomas.healthcoach.core.enums

import kotlinx.serialization.Serializable

@Serializable
enum class SelectedPage {
    WeightView,
    GraphsView,
    BloodPressureView
}

typealias AppPage = SelectedPage
