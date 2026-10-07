package com.lbthomas.healthcoach.features.settings

import com.lbthomas.healthcoach.core.sync.SyncConfig
import com.lbthomas.healthcoach.core.sync.SyncProviderType
import com.lbthomas.healthcoach.core.sync.adapters.LocalFolderAdapter
import com.lbthomas.healthcoach.core.sync.adapters.PeerToPeerStorageAdapter
import com.lbthomas.healthcoach.features.settings.data.SettingsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsResetTest {

    @Test
    fun testLocalFolderAdapterDeleteFile() = runBlocking {
        val tempDir = File.createTempFile("hc_test_local_sync", "").apply {
            delete()
            mkdirs()
        }
        try {
            val adapter = LocalFolderAdapter(basePath = tempDir.absolutePath, subFolder = "")
            val testFile = File(tempDir, SyncConfig.DEFAULT_REMOTE_DB_NAME)
            val testGzFile = File(tempDir, "${SyncConfig.DEFAULT_REMOTE_DB_NAME}.gz")

            testFile.writeText("test sqlite content")
            testGzFile.writeText("test gz content")
            assertTrue(testFile.exists())
            assertTrue(testGzFile.exists())

            val deleteResult = adapter.deleteFile(SyncConfig.DEFAULT_REMOTE_DB_NAME)
            assertTrue(deleteResult)
            assertFalse(testFile.exists())
            assertFalse(testGzFile.exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testPeerToPeerStorageAdapterDeleteFileReturnsFalse() = runBlocking {
        val p2pAdapter = PeerToPeerStorageAdapter(
            serverHost = "127.0.0.1",
            serverPort = 8765,
            serverToken = "test_token",
            serverName = "Test Server",
            clientInstanceId = "client-1",
            clientDeviceName = "Client Device"
        )
        // Peer instances manage their own local databases independently; deleteFile returns false
        val result = p2pAdapter.deleteFile("healthcoach.db")
        assertFalse(result)
    }

    @Test
    fun testSettingsViewModelResetRemoteDestinationForPeerFails() = runBlocking {
        val settingsFlow = MutableStateFlow(
            SettingsData().copy(
                sync = SettingsData().sync.copy(
                    syncProvider = SyncProviderType.PEER_TO_PEER
                )
            )
        )
        val viewModel = SettingsViewModel(settings = settingsFlow)
        val result = viewModel.resetRemoteDestination(SyncProviderType.PEER_TO_PEER)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun testSettingsViewModelResetRemoteDestinationForLocalFolder() = runBlocking {
        val tempDir = File.createTempFile("hc_test_reset_dest", "").apply {
            delete()
            mkdirs()
        }
        try {
            val subFolder = File(tempDir, SyncConfig.LOCAL_APP_SUBFOLDER).apply { mkdirs() }
            val syncFile = File(subFolder, SyncConfig.DEFAULT_REMOTE_DB_NAME).apply {
                writeText("test content")
            }
            assertTrue(syncFile.exists())

            val settingsFlow = MutableStateFlow(
                SettingsData().copy(
                    sync = SettingsData().sync.copy(
                        syncProvider = SyncProviderType.LOCAL_FOLDER,
                        localSyncPath = tempDir.absolutePath
                    )
                )
            )
            val viewModel = SettingsViewModel(settings = settingsFlow)
            val result = viewModel.resetRemoteDestination(SyncProviderType.LOCAL_FOLDER)
            assertTrue(result.isSuccess)
            assertFalse(syncFile.exists())
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
