package com.lbthomas.healthcoach.core.enums

import kotlinx.serialization.Serializable

@Serializable
enum class GraphTimeFrame(
    val label: String,
    val days: Int?
) {
    ALL("All", null),
    TWO_YEAR("2 year", 730),
    ONE_YEAR("1 year", 365),
    YEAR_TO_DATE("Year to date", null),
    SIX_MONTHS("6 months", 31 * 6),
    THREE_MONTHS("3 months", 31 * 3),
    TWO_MONTHS("2 months", 31 * 2),
    ONE_MONTH("1 month", 31),
    TWO_WEEKS("2 weeks", 14),
    ONE_WEEK("1 week", 7)
}
