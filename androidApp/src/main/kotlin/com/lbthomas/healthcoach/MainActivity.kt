package com.lbthomas.healthcoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.lbthomas.healthcoach.core.di.appModule
import com.lbthomas.healthcoach.core.di.configurePlatformContext
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (GlobalContext.getOrNull() == null) {
            startKoin {
                configurePlatformContext(this@MainActivity)
                modules(appModule)
            }
        }

        setContent {
            App()
        }
    }

    override fun onStop() {
        super.onStop()
        val settingsViewModel = runCatching { GlobalContext.get().get<com.lbthomas.healthcoach.features.settings.SettingsViewModel>() }.getOrNull()
        val syncEngine = runCatching { GlobalContext.get().get<com.lbthomas.healthcoach.core.sync.SyncEngine>() }.getOrNull()
        val currentSettings = settingsViewModel?.settings?.value
        if (currentSettings?.syncEnabled == true && currentSettings.autoSyncOnClose && syncEngine != null) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                syncEngine.sync(currentSettings.toSyncConfig())
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}