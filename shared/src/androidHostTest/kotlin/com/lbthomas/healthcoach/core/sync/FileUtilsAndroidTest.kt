package com.lbthomas.healthcoach.core.sync

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileUtilsAndroidTest {

    @Test
    fun testJoinPathStandardFiles() {
        val path = FileUtils.joinPath("/storage/emulated/0/Download", "com.lbthomas.healthcoach", "test.db")
        assertEquals("/storage/emulated/0/Download/com.lbthomas.healthcoach/test.db", path)
    }

    @Test
    fun testJoinPathContentUri() {
        val baseUri = "content://com.android.externalstorage.documents/tree/primary%3ADocuments"
        val subDir = "com.lbthomas.healthcoach"
        val fileName = "healthcoach.db"

        val joinedDir = FileUtils.joinPath(baseUri, subDir)
        assertEquals("$baseUri/$subDir", joinedDir)

        val joinedFile = FileUtils.joinPath(joinedDir, fileName)
        assertEquals("$baseUri/$subDir/$fileName", joinedFile)

        val directJoined = FileUtils.joinPath(baseUri, subDir, fileName)
        assertEquals("$baseUri/$subDir/$fileName", directJoined)
    }

    @Test
    fun testFileOperationsOnStandardPath() {
        val tempDir = File.createTempFile("hc_android_test", "").apply {
            delete()
            mkdirs()
        }
        try {
            val targetDir = FileUtils.joinPath(tempDir.absolutePath, "com.lbthomas.healthcoach")
            assertTrue(FileUtils.ensureDirectoryExists(targetDir))
            assertTrue(FileUtils.isDirectoryWritable(targetDir))

            val testFile = FileUtils.joinPath(targetDir, "test.txt")
            assertFalse(FileUtils.fileExists(testFile))

            val content = "HealthCoach Android Sync Test"
            assertTrue(FileUtils.writeUtf8String(testFile, content))
            assertTrue(FileUtils.fileExists(testFile))
            assertEquals(content, FileUtils.readUtf8String(testFile))
            assertEquals(content.length.toLong(), FileUtils.getFileSize(testFile))

            val hash = FileUtils.calculateFileSha256(testFile)
            val expectedHash = FileUtils.calculateSha256(content.encodeToByteArray())
            assertEquals(expectedHash, hash)

            val copyFile = FileUtils.joinPath(targetDir, "test_copy.txt")
            assertTrue(FileUtils.copyFile(testFile, copyFile))
            assertTrue(FileUtils.fileExists(copyFile))
            assertEquals(content, FileUtils.readUtf8String(copyFile))

            val gzFile = FileUtils.joinPath(targetDir, "test.txt.gz")
            assertTrue(FileUtils.compressGzip(testFile, gzFile))
            assertTrue(FileUtils.fileExists(gzFile))
            assertTrue(FileUtils.isGzipFile(gzFile))

            val decompressedFile = FileUtils.joinPath(targetDir, "test_decompressed.txt")
            assertTrue(FileUtils.decompressGzip(gzFile, decompressedFile))
            assertEquals(content, FileUtils.readUtf8String(decompressedFile))

            assertTrue(FileUtils.deleteFile(testFile))
            assertFalse(FileUtils.fileExists(testFile))
            assertTrue(FileUtils.deleteFile(copyFile))
            assertTrue(FileUtils.deleteFile(gzFile))
            assertTrue(FileUtils.deleteFile(decompressedFile))
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
