package com.lbthomas.healthcoach.core.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock


val today: LocalDate
    get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

fun YearMonth.displayName(): String {
    val monthName = month.name.lowercase().replaceFirstChar { it.titlecase() }
    return "$monthName $year"
}

fun LocalDate.displayDate(): String {
    val monthName = month.name.lowercase().replaceFirstChar { it.titlecase() }
    return "$monthName ${this.day}, ${this.year}"
}


fun LocalDate.DOW(): String {
    return this.dayOfWeek.name.lowercase().replaceFirstChar { it.titlecase() }.take(3)
}