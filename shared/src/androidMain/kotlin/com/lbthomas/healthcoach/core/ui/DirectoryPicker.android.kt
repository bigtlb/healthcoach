package com.lbthomas.healthcoach.core.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.lbthomas.healthcoach.core.sync.FileUtils

@Composable
actual fun rememberDirectoryPicker(onDirectorySelected: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, flags)
            } catch (_: Exception) {
            }
            FileUtils.setContext(context.applicationContext)
            onDirectorySelected(it.toString())
        }
    }

    return {
        launcher.launch(null)
    }
}
