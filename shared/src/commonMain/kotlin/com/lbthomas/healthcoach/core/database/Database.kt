package com.lbthomas.healthcoach.core.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.Logger
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.Database.Companion.Schema
import com.lbthomas.healthcoach.features.foodjournal.data.DefaultFoodData

fun createDatabase(driverFactory: DriverFactory): Database {
    val driver = driverFactory.createDriver()
    return createDatabaseForDriver(driver)
}

fun createDatabaseForPath(driverFactory: DriverFactory, dbFilePath: String): Database {
    val driver = driverFactory.createDriverForPath(dbFilePath)
    return createDatabaseForDriver(driver)
}

fun createDatabaseForDriver(driver: SqlDriver): Database {
    val database = Database(driver)

    configureAutoVacuum(driver)

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

    DefaultFoodData.ensureDefaultFoodData(database)

    return database
}

fun configureAutoVacuum(driver: SqlDriver) {
    try {
        val currentAutoVacuum = getAutoVacuum(driver)
        if (currentAutoVacuum != 2L) { // 2 = INCREMENTAL
            driver.execute(null, "PRAGMA auto_vacuum = INCREMENTAL", 0, null)
            val dbVersion = getDbVersion(driver)
            if (dbVersion > 0L) {
                // If tables already exist, run VACUUM to migrate database to incremental auto_vacuum mode
                driver.execute(null, "VACUUM", 0, null)
            }
            Logger.i("dbinit: auto_vacuum configured to INCREMENTAL")
        }
    } catch (e: Exception) {
        Logger.w("Failed to configure auto_vacuum: ${e.message}")
    }
}

fun getAutoVacuum(driver: SqlDriver): Long {
    val mapper = { cursor: SqlCursor ->
        QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null)
    }
    return try {
        driver.executeQuery(null, "PRAGMA auto_vacuum", mapper, 0, null).value ?: 0L
    } catch (_: Exception) {
        0L
    }
}

fun incrementalVacuum(driver: SqlDriver, pages: Int = 0) {
    try {
        if (pages > 0) {
            driver.execute(null, "PRAGMA incremental_vacuum($pages)", 0, null)
        } else {
            driver.execute(null, "PRAGMA incremental_vacuum", 0, null)
        }
    } catch (e: Exception) {
        Logger.w("Failed to execute incremental_vacuum: ${e.message}")
    }
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
