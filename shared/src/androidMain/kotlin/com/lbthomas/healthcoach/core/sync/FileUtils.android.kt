package com.lbthomas.healthcoach.core.sync

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

actual object FileUtils {
    actual fun calculateFileSha256(filePath: String): String? {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) return null
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { input ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            null
        }
    }

    actual fun calculateSha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }

    actual fun copyFile(sourcePath: String, destinationPath: String): Boolean {
        val src = File(sourcePath)
        val dst = File(destinationPath)
        if (!src.exists()) return false
        return try {
            dst.parentFile?.mkdirs()
            FileInputStream(src).use { input ->
                FileOutputStream(dst).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    actual fun moveFile(sourcePath: String, destinationPath: String): Boolean {
        val src = File(sourcePath)
        val dst = File(destinationPath)
        if (!src.exists()) return false
        dst.parentFile?.mkdirs()
        if (src.renameTo(dst)) return true
        return if (copyFile(sourcePath, destinationPath)) {
            src.delete()
            true
        } else {
            false
        }
    }

    actual fun deleteFile(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.exists()) file.delete() else true
    }

    actual fun fileExists(filePath: String): Boolean {
        return File(filePath).exists()
    }

    actual fun ensureDirectoryExists(directoryPath: String): Boolean {
        if (directoryPath.isBlank()) return false
        val dir = File(directoryPath)
        return dir.exists() && dir.isDirectory || dir.mkdirs()
    }

    actual fun isDirectoryWritable(directoryPath: String): Boolean {
        if (directoryPath.isBlank()) return false
        val dir = File(directoryPath)
        return dir.exists() && dir.isDirectory && dir.canWrite()
    }

    actual fun getFileSize(filePath: String): Long {
        val file = File(filePath)
        return if (file.exists()) file.length() else 0L
    }

    actual fun getFileLastModified(filePath: String): Long {
        val file = File(filePath)
        return if (file.exists()) file.lastModified() else 0L
    }

    actual fun joinPath(base: String, vararg parts: String): String {
        var result = File(base)
        for (part in parts) {
            if (part.isNotBlank()) {
                result = File(result, part)
            }
        }
        return result.path
    }
}
