package com.lbthomas.healthcoach.core.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

val nowLocal: LocalDateTime
    get() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

val today: LocalDate
    get() = nowLocal.date

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

fun parseRfc3339Instant(dateTimeStr: String): Instant {
    val trimmed = dateTimeStr.trim()
    val normalized = if (trimmed.contains(" ") && !trimmed.contains("T")) {
        trimmed.replace(" ", "T")
    } else {
        trimmed
    }
    return Instant.parse(normalized)
}

fun parseToLocalDateTime(dateTimeStr: String, timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime {
    val instant = parseRfc3339Instant(dateTimeStr)
    return instant.toLocalDateTime(timeZone)
}

fun toRfc3339Zulu(
    date: LocalDate,
    hour: Int,
    minute: Int,
    second: Int = 0,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String {
    val localDateTime = LocalDateTime(date.year, date.month, date.day, hour, minute, second)
    val instant = localDateTime.toInstant(timeZone)
    return instant.toString()
}

fun parseBpDateTime(dateTimeStr: String, timeZone: TimeZone = TimeZone.currentSystemDefault()): Pair<LocalDate, LocalTime?> {
    val trimmed = dateTimeStr.trim()
    if (trimmed.length == 10 && !trimmed.contains("T") && !trimmed.contains(" ") && !trimmed.contains("Z")) {
        return Pair(LocalDate.parse(trimmed), null)
    }
    return try {
        val instant = parseRfc3339Instant(trimmed)
        val ldt = instant.toLocalDateTime(timeZone)
        Pair(ldt.date, ldt.time)
    } catch (_: Exception) {
        val datePart = trimmed.take(10)
        Pair(LocalDate.parse(datePart), null)
    }
}

fun formatBpStorageString(
    date: LocalDate,
    time: LocalTime?,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String {
    return if (time == null) {
        date.toString()
    } else {
        toRfc3339Zulu(date, time.hour, time.minute, time.second, timeZone)
    }
}

fun LocalTime.formatTime(): String {
    val h = hour.toString().padStart(2, '0')
    val m = minute.toString().padStart(2, '0')
    return "$h:$m"
}

fun LocalTime.displayTime(): String {
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val amPm = if (hour >= 12) "PM" else "AM"
    val m = minute.toString().padStart(2, '0')
    return "$h:$m $amPm"
}

fun LocalDateTime.formatDateTime(): String {
    return "${date.displayDate()} ${time.formatTime()}"
}

fun formatEpochMillis(epochMillis: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return ldt.formatDateTime()
}