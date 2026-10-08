package com.lbthomas.healthcoach.core.sync

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

actual object FileUtils {
    @Volatile
    private var appContext: Context? = null

    fun setContext(context: Context) {
        appContext = context.applicationContext
    }

    private data class SafPath(val treeUri: Uri, val relativeSegments: List<String>)

    private fun parseSafPath(path: String): SafPath? {
        if (!path.startsWith("content://")) return null
        return try {
            val uri = Uri.parse(path)
            val authority = uri.authority ?: return null
            val pathSegments = uri.pathSegments ?: return null

            val treeIndex = pathSegments.indexOf("tree")
            if (treeIndex == -1 || treeIndex + 1 >= pathSegments.size) {
                SafPath(uri, emptyList())
            } else {
                val treeDocId = pathSegments[treeIndex + 1]
                val treeUri = DocumentsContract.buildTreeDocumentUri(authority, treeDocId)
                val relativeSegments = pathSegments.drop(treeIndex + 2)
                SafPath(treeUri, relativeSegments)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getDocumentFile(safPath: SafPath, createDirectories: Boolean = false): DocumentFile? {
        val ctx = appContext ?: return null
        var current = DocumentFile.fromTreeUri(ctx, safPath.treeUri) ?: return null
        if (!current.exists()) return null

        for (segment in safPath.relativeSegments) {
            val next = current.findFile(segment)
            if (next != null) {
                current = next
            } else if (createDirectories) {
                val created = current.createDirectory(segment) ?: return null
                current = created
            } else {
                return null
            }
        }
        return current
    }

    private fun openInputStream(path: String): InputStream? {
        if (path.startsWith("content://")) {
            val safPath = parseSafPath(path) ?: return null
            val doc = getDocumentFile(safPath, createDirectories = false) ?: return null
            if (!doc.exists() || !doc.isFile) return null
            return appContext?.contentResolver?.openInputStream(doc.uri)
        }
        val file = File(path)
        if (!file.exists() || !file.isFile) return null
        return try {
            FileInputStream(file)
        } catch (_: Exception) {
            null
        }
    }

    private fun openOutputStream(path: String, mimeType: String = "application/octet-stream"): OutputStream? {
        if (path.startsWith("content://")) {
            val safPath = parseSafPath(path) ?: return null
            if (safPath.relativeSegments.isEmpty()) return null
            val parentSafPath = safPath.copy(relativeSegments = safPath.relativeSegments.dropLast(1))
            val fileName = safPath.relativeSegments.last()
            val parentDoc = getDocumentFile(parentSafPath, createDirectories = true) ?: return null
            var fileDoc = parentDoc.findFile(fileName)
            if (fileDoc == null) {
                fileDoc = parentDoc.createFile(mimeType, fileName) ?: return null
            }
            return try {
                appContext?.contentResolver?.openOutputStream(fileDoc.uri, "wt")
            } catch (_: Exception) {
                null
            }
        }
        val file = File(path)
        return try {
            file.parentFile?.mkdirs()
            FileOutputStream(file)
        } catch (_: Exception) {
            null
        }
    }

    actual fun calculateFileSha256(filePath: String): String? {
        val input = openInputStream(filePath) ?: return null
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            input.use { inStream ->
                var bytesRead: Int
                while (inStream.read(buffer).also { bytesRead = it } != -1) {
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

    actual fun calculateUncompressedSha256(filePath: String): String? {
        val isGz = isGzipFile(filePath)
        val rawInput = openInputStream(filePath) ?: return null
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            val stream = if (isGz) GZIPInputStream(rawInput) else rawInput
            stream.use { input ->
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

    actual fun copyFile(sourcePath: String, destinationPath: String): Boolean {
        if (!fileExists(sourcePath)) return false
        val input = openInputStream(sourcePath) ?: return false
        val output = openOutputStream(destinationPath) ?: run {
            input.close()
            return false
        }
        return try {
            input.use { inStream ->
                output.use { outStream ->
                    inStream.copyTo(outStream)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    actual fun moveFile(sourcePath: String, destinationPath: String): Boolean {
        if (!fileExists(sourcePath)) return false
        if (sourcePath.startsWith("content://") && destinationPath.startsWith("content://")) {
            val srcSaf = parseSafPath(sourcePath) ?: return false
            val dstSaf = parseSafPath(destinationPath) ?: return false
            val srcDoc = getDocumentFile(srcSaf, createDirectories = false) ?: return false
            if (!srcDoc.exists()) return false

            val sameParent = srcSaf.treeUri == dstSaf.treeUri &&
                srcSaf.relativeSegments.dropLast(1) == dstSaf.relativeSegments.dropLast(1)
            if (sameParent && dstSaf.relativeSegments.isNotEmpty()) {
                val targetName = dstSaf.relativeSegments.last()
                val existingDst = getDocumentFile(dstSaf, createDirectories = false)
                if (existingDst != null && existingDst.exists()) {
                    existingDst.delete()
                }
                if (srcDoc.renameTo(targetName)) return true
            }
            return if (copyFile(sourcePath, destinationPath)) {
                srcDoc.delete()
                true
            } else {
                false
            }
        } else if (!sourcePath.startsWith("content://") && !destinationPath.startsWith("content://")) {
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
        } else {
            return if (copyFile(sourcePath, destinationPath)) {
                deleteFile(sourcePath)
                true
            } else {
                false
            }
        }
    }

    actual fun deleteFile(filePath: String): Boolean {
        if (filePath.startsWith("content://")) {
            val safPath = parseSafPath(filePath) ?: return true
            val doc = getDocumentFile(safPath, createDirectories = false) ?: return true
            return if (doc.exists()) doc.delete() else true
        }
        val file = File(filePath)
        return if (file.exists()) file.delete() else true
    }

    actual fun fileExists(filePath: String): Boolean {
        if (filePath.startsWith("content://")) {
            val safPath = parseSafPath(filePath) ?: return false
            val doc = getDocumentFile(safPath, createDirectories = false) ?: return false
            return doc.exists()
        }
        return File(filePath).exists()
    }

    actual fun ensureDirectoryExists(directoryPath: String): Boolean {
        if (directoryPath.isBlank()) return false
        if (directoryPath.startsWith("content://")) {
            val safPath = parseSafPath(directoryPath) ?: return false
            val doc = getDocumentFile(safPath, createDirectories = true)
            return doc != null && doc.isDirectory && doc.exists()
        }
        val dir = File(directoryPath)
        return dir.exists() && dir.isDirectory || dir.mkdirs()
    }

    actual fun isDirectoryWritable(directoryPath: String): Boolean {
        if (directoryPath.isBlank()) return false
        if (directoryPath.startsWith("content://")) {
            val safPath = parseSafPath(directoryPath) ?: return false
            val doc = getDocumentFile(safPath, createDirectories = false) ?: return false
            return doc.exists() && doc.isDirectory && doc.canWrite()
        }
        val dir = File(directoryPath)
        return dir.exists() && dir.isDirectory && dir.canWrite()
    }

    actual fun getFileSize(filePath: String): Long {
        if (filePath.startsWith("content://")) {
            val safPath = parseSafPath(filePath) ?: return 0L
            val doc = getDocumentFile(safPath, createDirectories = false) ?: return 0L
            return if (doc.exists()) doc.length() else 0L
        }
        val file = File(filePath)
        return if (file.exists()) file.length() else 0L
    }

    actual fun getFileLastModified(filePath: String): Long {
        if (filePath.startsWith("content://")) {
            val safPath = parseSafPath(filePath) ?: return 0L
            val doc = getDocumentFile(safPath, createDirectories = false) ?: return 0L
            return if (doc.exists()) doc.lastModified() else 0L
        }
        val file = File(filePath)
        return if (file.exists()) file.lastModified() else 0L
    }

    actual fun joinPath(base: String, vararg parts: String): String {
        if (base.startsWith("content://")) {
            val cleanBase = base.trimEnd('/')
            val joinedParts = parts.filter { it.isNotBlank() }.map { it.trim('/') }
            return if (joinedParts.isEmpty()) cleanBase else (listOf(cleanBase) + joinedParts).joinToString("/")
        }
        var result = File(base)
        for (part in parts) {
            if (part.isNotBlank()) {
                result = File(result, part)
            }
        }
        return result.path
    }

    actual fun isGzipFile(filePath: String): Boolean {
        if (filePath.startsWith("content://")) {
            val safPath = parseSafPath(filePath) ?: return false
            val doc = getDocumentFile(safPath, createDirectories = false) ?: return false
            if (!doc.exists() || !doc.isFile || doc.length() < 2) return false
            val ctx = appContext ?: return false
            return try {
                ctx.contentResolver.openInputStream(doc.uri)?.use { input ->
                    val b1 = input.read()
                    val b2 = input.read()
                    b1 == 0x1F && b2 == 0x8B
                } ?: false
            } catch (_: Exception) {
                false
            }
        }
        val file = File(filePath)
        if (!file.exists() || !file.isFile || file.length() < 2) return false
        return try {
            FileInputStream(file).use { input ->
                val b1 = input.read()
                val b2 = input.read()
                b1 == 0x1F && b2 == 0x8B
            }
        } catch (_: Exception) {
            false
        }
    }

    actual fun compressGzip(sourcePath: String, destinationPath: String): Boolean {
        if (!fileExists(sourcePath)) return false
        val input = openInputStream(sourcePath) ?: return false
        val output = openOutputStream(destinationPath) ?: run {
            input.close()
            return false
        }
        return try {
            input.use { inStream ->
                GZIPOutputStream(output).use { gzipOut ->
                    inStream.copyTo(gzipOut)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    actual fun decompressGzip(sourcePath: String, destinationPath: String): Boolean {
        if (!fileExists(sourcePath)) return false
        val input = openInputStream(sourcePath) ?: return false
        val output = openOutputStream(destinationPath) ?: run {
            input.close()
            return false
        }
        return try {
            GZIPInputStream(input).use { gzipIn ->
                output.use { outStream ->
                    gzipIn.copyTo(outStream)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    actual fun decompressGzipIfNeeded(sourcePath: String, destinationPath: String): Boolean {
        return if (isGzipFile(sourcePath)) {
            decompressGzip(sourcePath, destinationPath)
        } else {
            copyFile(sourcePath, destinationPath)
        }
    }

    actual fun readUtf8String(filePath: String): String? {
        val input = openInputStream(filePath) ?: return null
        return try {
            input.use { it.bufferedReader(Charsets.UTF_8).readText() }
        } catch (_: Exception) {
            null
        }
    }

    actual fun writeUtf8String(filePath: String, content: String): Boolean {
        val output = openOutputStream(filePath, mimeType = "text/plain") ?: return false
        return try {
            output.use { it.bufferedWriter(Charsets.UTF_8).use { writer -> writer.write(content) } }
            true
        } catch (_: Exception) {
            false
        }
    }
}
