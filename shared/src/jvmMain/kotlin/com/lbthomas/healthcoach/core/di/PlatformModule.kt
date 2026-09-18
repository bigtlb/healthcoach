package com.lbthomas.healthcoach.core.di

import com.lbthomas.healthcoach.core.database.DriverFactory
import com.lbthomas.healthcoach.core.database.createDbFolder
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

actual val platformModule: Module = module {
    single { DriverFactory() }
    single(named("settingsFile")) { File(createDbFolder(), "settings.json") }
}

actual fun KoinApplication.configurePlatformContext(context: Any?) {}
