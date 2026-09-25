package com.lbthomas.healthcoach.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.Logger
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.Database.Companion.Schema

fun createDatabase(driverFactory: DriverFactory): Database {
    val driver = driverFactory.createDriver()
    val database = Database(driver)

    database.transaction {
        val dbVersion = getDbVersion(driver)
        val schemaVersion = Schema.version
        if (dbVersion == 0L) {
            Schema.create(driver)
            setDbVersion(driver, schemaVersion)
            Logger.i("dbinit: created tables, setVersion to $schemaVersion")
        } else {
            Logger.i("dbinit: existing tables, version $dbVersion")
            if (schemaVersion > dbVersion) {
                Logger.i("dbinit: upgrading tables from $dbVersion to $schemaVersion")
                Schema.migrate(driver, dbVersion, schemaVersion)
                setDbVersion(driver, schemaVersion)
                Logger.i("dbinit: upgraded tables to version $schemaVersion")
            }
        }
    }
    return database
}

fun getDbVersion(driver: SqlDriver): Long {
    val mapper = { cursor: SqlCursor ->
        QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null)
    }
    return driver.executeQuery(null, "PRAGMA user_version", mapper, 0, null).value ?: 0L
}

fun setDbVersion(driver: SqlDriver, version: Long) {
    driver.execute(null, "PRAGMA user_version = $version", 0, null).value
}
