package com.lbthomas.healthcoach.core.utils

/**
 * Converts a string to Title Case by capitalizing the first letter of each word
 * (accounting for spaces and hyphens) and lowercasing the rest.
 */
fun String.toTitleCase(): String {
    if (isBlank()) return this
    return trim().split(Regex("\\s+")).joinToString(" ") { word ->
        word.split("-").joinToString("-") { part ->
            part.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }
}
