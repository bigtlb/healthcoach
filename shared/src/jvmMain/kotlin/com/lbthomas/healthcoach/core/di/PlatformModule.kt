package com.lbthomas.healthcoach.core.di

import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.lbthomas.healthcoach.core.database.DriverFactory
import com.lbthomas.healthcoach.core.database.createDbFolder
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private class FileLogWriter(private val logFile: File) : LogWriter() {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    override fun log(severity: Severity, message: String, tag: String, throwable: Throwable?) {
        try {
            val timestamp = dateFormat.format(Date())
            val tagStr = if (tag.isNotEmpty()) "[$tag] " else ""
            val logLine = "$timestamp [${severity.name.uppercase()}] $tagStr$message\n"
            val errorLine = throwable?.let {
                val sw = StringWriter()
                it.printStackTrace(PrintWriter(sw))
                sw.toString()
            } ?: ""
            synchronized(this) {
                val parent = logFile.parentFile
                if (parent != null && !parent.exists()) {
                    parent.mkdirs()
                }
                logFile.appendText(logLine + errorLine)
            }
        } catch (_: Throwable) {}
    }
}

actual val platformModule: Module = module {
    single { DriverFactory() }
    single(named("settingsFile")) { File(createDbFolder(), "settings.json") }
}

actual fun KoinApplication.configurePlatformContext(context: Any?) {
    val logFile = File(createDbFolder(), "healthcoach.log")
    Logger.setLogWriters(CommonWriter(), FileLogWriter(logFile))
}
