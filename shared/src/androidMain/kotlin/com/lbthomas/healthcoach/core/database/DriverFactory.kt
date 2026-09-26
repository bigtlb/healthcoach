package com.lbthomas.healthcoach.core.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.lbthomas.healthcoach.Database

actual open class DriverFactory(private val context: Context) {
    actual open fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(Database.Schema, context, "healthcoach.db")
    }

    actual open fun createDriverForPath(dbFilePath: String): SqlDriver {
        return AndroidSqliteDriver(Database.Schema, context, dbFilePath)
    }

    actual open fun getDatabaseDirectory(): String {
        return context.getDatabasePath("healthcoach.db").parentFile?.absolutePath ?: context.filesDir.absolutePath
    }

    actual open fun getDatabaseFilePath(): String {
        return context.getDatabasePath("healthcoach.db").absolutePath
    }
}

actual fun createDbFolder(): String = "."
