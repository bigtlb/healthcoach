package com.lbthomas.healthcoach.core.enums

import kotlinx.serialization.Serializable

@Serializable
enum class ShareImageType(
    val label: String
) {
    NONE("Text Only"),
    ICON("Icon"),
    CHART("Chart")
}
