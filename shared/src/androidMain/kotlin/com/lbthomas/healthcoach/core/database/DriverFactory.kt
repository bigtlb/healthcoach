package com.lbthomas.healthcoach.core.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.lbthomas.healthcoach.Database

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(Database.Schema, context, "healthcoach.db")
    }
}

actual fun createDbFolder(): String = "."
