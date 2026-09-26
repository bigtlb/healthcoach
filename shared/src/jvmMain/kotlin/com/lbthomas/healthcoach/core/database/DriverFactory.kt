package com.lbthomas.healthcoach.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.lbthomas.healthcoach.core.database.createDbFolder
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

actual open class DriverFactory(private val customDbFolder: String? = null) {
    actual open fun createDriver(): SqlDriver {
        val dbFolder = customDbFolder ?: createDbFolder()
        return JdbcSqliteDriver("jdbc:sqlite:${dbFolder}/healthcoach.db")
    }

    actual open fun createDriverForPath(dbFilePath: String): SqlDriver {
        return JdbcSqliteDriver("jdbc:sqlite:${dbFilePath}")
    }

    actual open fun getDatabaseDirectory(): String {
        return customDbFolder ?: createDbFolder()
    }

    actual open fun getDatabaseFilePath(): String {
        return "${getDatabaseDirectory()}/healthcoach.db"
    }
}

actual fun createDbFolder(): String {
    val osName = System.getProperty("os.name", "").lowercase()
    val userHome = System.getProperty("user.home", "")
    val appName = "healthcoach"

    val baseDir = when {
        osName.contains("win") -> System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() } ?: "$userHome/AppData/Local"
        osName.contains("mac") -> "$userHome/Library/Application Support"
        else -> System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() } ?: "$userHome/.local/share"
    }

    return Path(baseDir).resolve(appName).createDirectories().toString()
}
