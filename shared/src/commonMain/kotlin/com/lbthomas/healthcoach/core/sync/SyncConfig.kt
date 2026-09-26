package com.lbthomas.healthcoach.core.sync

import kotlinx.serialization.Serializable

/**
 * Supported storage providers for synchronization.
 */
@Serializable
enum class SyncProviderType {
    LOCAL_FOLDER,
    GOOGLE_DRIVE,
    SMB
}

/**
 * Configuration parameters for synchronization storage providers.
 */
@Serializable
data class SyncConfig(
    val providerType: SyncProviderType = SyncProviderType.LOCAL_FOLDER,
    val remoteFileName: String = DEFAULT_REMOTE_DB_NAME,
    val localFolderPath: String = "",
    val googleAccountEmail: String = "",
    val googleAccessToken: String = "",
    val googleRefreshToken: String = ""
) {
    companion object {
        const val DEFAULT_REMOTE_DB_NAME = "healthcoach.db"
        const val LOCAL_APP_SUBFOLDER = "com.lbthomas.healthcoach"
        const val GOOGLE_APP_SUBFOLDER = "appDataFolder"
    }
}
