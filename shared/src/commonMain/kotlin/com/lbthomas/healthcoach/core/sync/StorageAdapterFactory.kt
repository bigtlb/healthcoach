package com.lbthomas.healthcoach.core.sync

import com.lbthomas.healthcoach.core.sync.adapters.GoogleDriveStorageAdapter
import com.lbthomas.healthcoach.core.sync.adapters.LocalFolderAdapter
import com.lbthomas.healthcoach.core.sync.auth.GoogleOAuthManager

/**
 * Factory for creating configured [RemoteStorageAdapter] instances.
 */
object StorageAdapterFactory {
    fun createAdapter(config: SyncConfig): RemoteStorageAdapter {
        return when (config.providerType) {
            SyncProviderType.LOCAL_FOLDER -> {
                LocalFolderAdapter(
                    basePath = config.localFolderPath,
                    subFolder = SyncConfig.LOCAL_APP_SUBFOLDER
                )
            }
            SyncProviderType.GOOGLE_DRIVE -> {
                GoogleDriveStorageAdapter(
                    appSubFolder = SyncConfig.GOOGLE_APP_SUBFOLDER,
                    clientId = GoogleOAuthManager.getResolvedClientId(),
                    accountEmail = config.googleAccountEmail,
                    accessToken = config.googleAccessToken,
                    refreshToken = config.googleRefreshToken
                )
            }
            SyncProviderType.SMB -> {
                throw UnsupportedOperationException("SMB storage adapter is deferred to FR-11")
            }
        }
    }
}
