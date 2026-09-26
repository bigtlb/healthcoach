package com.lbthomas.healthcoach.core.sync

import com.lbthomas.healthcoach.core.sync.adapters.GoogleDriveStorageAdapter
import com.lbthomas.healthcoach.core.sync.adapters.LocalFolderAdapter
import com.lbthomas.healthcoach.core.sync.auth.AuthState
import com.lbthomas.healthcoach.core.sync.auth.ProviderCredentials
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RemoteStorageAdapterTest {

    private lateinit var testBaseDir: File

    @Before
    fun setUp() {
        testBaseDir = File(System.getProperty("java.io.tmpdir"), "healthcoach_test_${System.currentTimeMillis()}")
        testBaseDir.mkdirs()
    }

    @After
    fun tearDown() {
        testBaseDir.deleteRecursively()
    }

    @Test
    fun testLocalFolderAdapterAppendsAppSubfolderUnderTheCovers() = runBlocking {
        val adapter = LocalFolderAdapter(basePath = testBaseDir.absolutePath)
        val expectedTargetDir = File(testBaseDir, "com.lbthomas.healthcoach").absolutePath

        assertEquals(expectedTargetDir, adapter.targetDirectory)

        val connectionResult = adapter.testConnection()
        assertTrue(connectionResult.isSuccess)
        assertTrue(File(expectedTargetDir).exists())
    }

    @Test
    fun testLocalFolderAdapterUploadDownloadAndMetadata() = runBlocking {
        val adapter = LocalFolderAdapter(basePath = testBaseDir.absolutePath)

        // Create a dummy local source file
        val sourceFile = File(testBaseDir, "source.db")
        sourceFile.writeText("SQLite format 3 test payload data 12345")

        val customDbName = "custom_healthcoach.db"

        // Upload to adapter
        val uploadResult = adapter.uploadFile(
            sourcePath = sourceFile.absolutePath,
            fileName = customDbName
        )
        assertTrue(uploadResult)

        // Verify file created in subfolder
        val expectedRemoteFile = File(adapter.targetDirectory, customDbName)
        assertTrue(expectedRemoteFile.exists())

        // Verify metadata
        val metadata = adapter.getFileMetadata(customDbName)
        assertNotNull(metadata)
        assertEquals(customDbName, metadata.name)
        assertTrue(metadata.size > 0)
        assertTrue(metadata.sha256Hash.isNotEmpty())

        // Verify download
        val destinationFile = File(testBaseDir, "downloaded.db")
        val downloadResult = adapter.downloadFile(
            fileName = customDbName,
            destinationPath = destinationFile.absolutePath
        )
        assertTrue(downloadResult)
        assertTrue(destinationFile.exists())
        assertEquals(sourceFile.readText(), destinationFile.readText())
    }

    @Test
    fun testLocalFolderAdapterOptimisticConcurrencyControl() = runBlocking {
        val adapter = LocalFolderAdapter(basePath = testBaseDir.absolutePath)

        val sourceFile1 = File(testBaseDir, "source1.db")
        sourceFile1.writeText("Version 1 content")

        val fileName = "sync_concurrency.db"

        // Initial upload
        assertTrue(adapter.uploadFile(sourceFile1.absolutePath, fileName))
        val initialMetadata = adapter.getFileMetadata(fileName)
        assertNotNull(initialMetadata)

        // Simulate concurrent modification on remote
        val remoteFile = File(adapter.targetDirectory, fileName)
        remoteFile.writeText("Concurrent modification content")

        val sourceFile2 = File(testBaseDir, "source2.db")
        sourceFile2.writeText("Version 2 content")

        // Upload with old expected hash should fail
        val uploadWithStaleHash = adapter.uploadFile(
            sourcePath = sourceFile2.absolutePath,
            fileName = fileName,
            expectedHash = initialMetadata.sha256Hash
        )
        assertFalse(uploadWithStaleHash)

        // Upload with latest expected hash should succeed
        val updatedMetadata = adapter.getFileMetadata(fileName)
        assertNotNull(updatedMetadata)
        val uploadWithFreshHash = adapter.uploadFile(
            sourcePath = sourceFile2.absolutePath,
            fileName = fileName,
            expectedHash = updatedMetadata.sha256Hash
        )
        assertTrue(uploadWithFreshHash)
    }

    @Test
    fun testGoogleDriveStorageAdapterAuthenticationAndSecurityNotice() = runBlocking {
        val adapter = GoogleDriveStorageAdapter(
            customBasePath = testBaseDir.absolutePath,
            accessToken = "gdt_mock_access_token_12345",
            accountEmail = "user@gmail.com"
        )

        assertEquals(SyncProviderType.GOOGLE_DRIVE, adapter.providerType)
        assertNotNull(adapter.securityNotice)
        assertTrue(adapter.securityNotice.contains("appDataFolder"))
        assertTrue(adapter.authState.value is AuthState.Authenticated)

        // Disconnect
        val disconnectRes = adapter.disconnect()
        assertTrue(disconnectRes.isSuccess)
        assertEquals(AuthState.Unauthenticated, adapter.authState.value)

        // Re-authenticate
        val authRes = adapter.authenticate(
            ProviderCredentials(
                token = "gdt_mock_new_token_67890",
                username = "user@gmail.com"
            )
        )
        assertTrue(authRes.isSuccess)
        val token = authRes.getOrNull()
        assertNotNull(token)
        assertTrue(token.isNotBlank())
        assertTrue(adapter.authState.value is AuthState.Authenticated)
    }

    @Test
    fun testGoogleDriveStorageAdapterUploadDownloadAndMetadata() = runBlocking {
        val adapter = GoogleDriveStorageAdapter(
            customBasePath = testBaseDir.absolutePath,
            accessToken = "gdt_mock_token_123"
        )

        // Verify targetDirectory uses /appDataFolder
        val expectedTargetDir = File(testBaseDir, "appDataFolder").absolutePath
        assertEquals(expectedTargetDir, adapter.targetDirectory)

        val testFile = File(testBaseDir, "source_gdrive.db")
        testFile.writeText("Google Drive encrypted appData db content")

        val fileName = "healthcoach.db"

        // Upload
        val uploadRes = adapter.uploadFile(testFile.absolutePath, fileName)
        assertTrue(uploadRes)

        // Metadata
        val metadata = adapter.getFileMetadata(fileName)
        assertNotNull(metadata)
        assertEquals(fileName, metadata.name)
        assertTrue(metadata.size > 0)
        assertTrue(metadata.sha256Hash.isNotEmpty())

        // Download
        val destFile = File(testBaseDir, "downloaded_gdrive.db")
        val downloadRes = adapter.downloadFile(fileName, destFile.absolutePath)
        assertTrue(downloadRes)
        assertEquals(testFile.readText(), destFile.readText())
    }

    @Test
    fun testGoogleDriveStorageAdapterConstants() {
        assertEquals("https://www.googleapis.com/auth/drive.appdata", GoogleDriveStorageAdapter.SCOPE_DRIVE_APPDATA)
        assertEquals("https://www.googleapis.com/drive/v3/files", GoogleDriveStorageAdapter.API_BASE_URL)
        assertEquals("https://www.googleapis.com/upload/drive/v3/files", GoogleDriveStorageAdapter.UPLOAD_BASE_URL)
    }

    @Test
    fun testStorageAdapterFactory() {
        val localConfig = SyncConfig(
            providerType = SyncProviderType.LOCAL_FOLDER,
            localFolderPath = testBaseDir.absolutePath,
            remoteFileName = "my_custom.db"
        )
        val localAdapter = StorageAdapterFactory.createAdapter(localConfig)
        assertEquals(SyncProviderType.LOCAL_FOLDER, localAdapter.providerType)
        assertTrue(localAdapter is LocalFolderAdapter)
        assertEquals(File(testBaseDir, "com.lbthomas.healthcoach").absolutePath, localAdapter.targetDirectory)

        val googleConfig = SyncConfig(
            providerType = SyncProviderType.GOOGLE_DRIVE,
            googleAccountEmail = "user@gmail.com",
            googleAccessToken = "mock_token"
        )
        val googleAdapter = StorageAdapterFactory.createAdapter(googleConfig)
        assertEquals(SyncProviderType.GOOGLE_DRIVE, googleAdapter.providerType)
        assertTrue(googleAdapter is GoogleDriveStorageAdapter)
        assertTrue(googleAdapter.authState.value is AuthState.Authenticated)
    }
}
