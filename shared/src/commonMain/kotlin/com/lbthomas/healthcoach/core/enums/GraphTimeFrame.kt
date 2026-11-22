package com.lbthomas.healthcoach.core.enums

import kotlinx.serialization.Serializable

@Serializable
enum class GraphTimeFrame(
    val label: String,
    val shortLabel: String,
    val days: Int?
) {
    ONE_DAY("1 day", "1D", 1),
    THREE_DAYS("3 days", "3D", 3),
    ONE_WEEK("1 week", "1W", 7),
    TWO_WEEKS("2 weeks", "2W", 14),
    ONE_MONTH("1 month", "1M", 31),
    TWO_MONTHS("2 months", "2M", 31 * 2),
    THREE_MONTHS("3 months", "3M", 31 * 3),
    SIX_MONTHS("6 months", "6M", 31 * 6),
    YEAR_TO_DATE("Year to date", "YTD", null),
    ONE_YEAR("1 year", "1Y", 365),
    TWO_YEAR("2 year", "2Y", 730),
    ALL("All", "All", null)
}
