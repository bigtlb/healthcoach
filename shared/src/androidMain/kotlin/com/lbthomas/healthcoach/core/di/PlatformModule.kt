package com.lbthomas.healthcoach.core.di

import android.content.Context
import co.touchlab.kermit.LogcatWriter
import co.touchlab.kermit.Logger
import com.lbthomas.healthcoach.core.database.DriverFactory
import com.lbthomas.healthcoach.core.logging.LoggingConfig
import com.lbthomas.healthcoach.core.sync.p2p.PeerDiscoveryAdvertiser
import com.lbthomas.healthcoach.core.sync.p2p.PeerDiscoveryBrowser
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

actual val platformModule: Module = module {
    single { DriverFactory(get()) }
    single(named("settingsFile")) { File(get<Context>().filesDir, "settings.json") }
    single { PeerDiscoveryAdvertiser(get()) }
    single { PeerDiscoveryBrowser(get()) }
}

actual fun KoinApplication.configurePlatformContext(context: Any?) {
    if (context is Context) {
        androidContext(context)
    }
    Logger.setLogWriters(LogcatWriter())
    Logger.setMinSeverity(LoggingConfig.minSeverity)
}
