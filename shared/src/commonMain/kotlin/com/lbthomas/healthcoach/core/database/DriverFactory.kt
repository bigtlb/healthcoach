package com.lbthomas.healthcoach.core.database

import app.cash.sqldelight.db.SqlDriver

expect open class DriverFactory {
    open fun createDriver(): SqlDriver
    open fun createDriverForPath(dbFilePath: String): SqlDriver
    open fun getDatabaseDirectory(): String
    open fun getDatabaseFilePath(): String
}

expect fun createDbFolder(): String
