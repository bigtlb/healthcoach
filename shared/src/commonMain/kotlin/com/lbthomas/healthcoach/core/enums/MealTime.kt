package com.lbthomas.healthcoach.core.enums

enum class MealTime(val displayName: String, val order: Int) {
    BREAKFAST("Breakfast", 0),
    MORNING_SNACK("Morning Snack", 1),
    LUNCH("Lunch", 2),
    MIDDAY_SNACK("Mid-day Snack", 3),
    DINNER("Dinner", 4),
    EVENING_SNACK("Evening Snack", 5);

    companion object {
        fun fromName(name: String): MealTime {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: BREAKFAST
        }
    }
}
