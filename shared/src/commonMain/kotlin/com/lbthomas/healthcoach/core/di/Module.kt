package com.lbthomas.healthcoach.core.di


import com.lbthomas.healthcoach.core.database.createDatabase
import com.lbthomas.healthcoach.features.settings.SettingsStore
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.core.KoinApplication

expect val platformModule: Module
expect fun KoinApplication.configurePlatformContext(context: Any?)

val appModule = module {
    includes(platformModule)
    single { createDatabase(get()) }
    single { SettingsStore(get(named("settingsFile"))) }

    factory { SettingsViewModel(get()) }
}
