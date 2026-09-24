package com.lbthomas.healthcoach.core.enums

import kotlinx.serialization.Serializable

@Serializable
enum class GraphTimeFrame(
    val label: String,
    val shortLabel: String,
    val days: Int?
) {
    ALL("All", "All", null),
    TWO_YEAR("2 year", "2 Y", 730),
    ONE_YEAR("1 year", "1 Y", 365),
    YEAR_TO_DATE("Year to date", "YTD", null),
    SIX_MONTHS("6 months", "6 M", 31 * 6),
    THREE_MONTHS("3 months", "3 M", 31 * 3),
    TWO_MONTHS("2 months", "2 M", 31 * 2),
    ONE_MONTH("1 month", "1 M", 31),
    TWO_WEEKS("2 weeks", "2 W", 14),
    ONE_WEEK("1 week", "1 W", 7)
}
