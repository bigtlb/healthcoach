package com.lbthomas.healthcoach.core.di


import app.cash.sqldelight.db.SqlDriver
import com.lbthomas.healthcoach.Database
import com.lbthomas.healthcoach.core.database.DriverFactory
import com.lbthomas.healthcoach.core.database.createDatabaseForDriver
import com.lbthomas.healthcoach.core.sync.SyncEngine
import com.lbthomas.healthcoach.core.sync.p2p.PeerServerManager
import com.lbthomas.healthcoach.features.bloodpressure.BloodPressureViewModel
import com.lbthomas.healthcoach.features.bloodpressure.data.BloodPressureRepository
import com.lbthomas.healthcoach.features.settings.SettingsViewModel
import com.lbthomas.healthcoach.features.settings.data.SettingsStore
import com.lbthomas.healthcoach.features.sync.SyncViewModel
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
    single<SqlDriver> { get<DriverFactory>().createDriver() }
    single<Database> { createDatabaseForDriver(get()) }

    single { SettingsStore(get(named("settingsFile"))) }
    factory {
        SettingsViewModel(
            persistence = get(),
            peerServerManager = getOrNull(),
            discoveryAdvertiser = getOrNull(),
            discoveryBrowser = getOrNull()
        )
    }

    single { WeightRepository(get()) }
    factory { WeightViewModel(repository = get()) }

    single { BloodPressureRepository(get()) }
    factory { BloodPressureViewModel(repository = get()) }

    single { SyncEngine(localDatabase = get(), driverFactory = get(), settingsStore = get()) }
    single { SyncViewModel(syncEngine = get(), settingsStore = get()) }
    single {
        val driver: SqlDriver = get()
        PeerServerManager(driverFactory = get(), settingsStore = get()).apply {
            onDatabaseReset = {
                driver.notifyListeners("weightEntry", "bloodPressureEntry")
            }
        }
    }
}
