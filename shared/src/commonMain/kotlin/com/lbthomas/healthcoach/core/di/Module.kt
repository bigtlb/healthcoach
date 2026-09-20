package com.lbthomas.healthcoach.core.di


import com.lbthomas.healthcoach.core.database.createDatabase
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsStore
import com.lbthomas.healthcoach.features.weight.WeightViewModel
import com.lbthomas.healthcoach.features.weight.data.WeightRepository
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

expect val platformModule: Module
expect fun KoinApplication.configurePlatformContext(context: Any?)

val appModule = module {
    includes(platformModule)
    single { createDatabase(get()) }

    single { SettingsStore(get(named("settingsFile"))) }
    factory { SettingsViewModel(persistence = get()) }

    single { WeightRepository(get()) }
    factory { WeightViewModel(repository = get()) }
}
