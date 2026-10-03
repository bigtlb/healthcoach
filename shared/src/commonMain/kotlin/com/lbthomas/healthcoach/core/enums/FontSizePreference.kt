package com.lbthomas.healthcoach.core.enums

import kotlinx.serialization.Serializable

@Serializable
enum class FontSizePreference(
    val displayName: String,
    val deltaSp: Int
) {
    SMALL("Small", -2),
    MEDIUM("Medium", 0),
    LARGE("Large", 2),
    LARGEST("Largest", 4)
}
