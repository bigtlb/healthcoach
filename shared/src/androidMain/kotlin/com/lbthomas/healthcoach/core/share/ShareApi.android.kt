package com.lbthomas.healthcoach.core.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

actual object ShareApi {

    private fun getContext(): Context? {
        // Will be set by the app on startup
        return appContext
    }

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    actual fun shareText(text: String): String? {
        val context = getContext() ?: return "No context available"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share progress").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        return null
    }

    actual fun shareImage(bitmap: ImageBitmap): String? {
        val context = getContext() ?: return "No context available"
        val imageFile = saveBitmapToFile(context, bitmap) ?: return "Failed to save image"
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share progress").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        return null
    }

    actual fun shareTextAndImage(text: String, bitmap: ImageBitmap): String? {
        val context = getContext() ?: return "No context available"
        val imageFile = saveBitmapToFile(context, bitmap) ?: return "Failed to save image"
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share progress").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        return null
    }

    actual fun openShareUrl(url: String): String? {
        val context = getContext() ?: return "No context available"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return null
    }

    actual fun copyTextToClipboard(text: String): String? {
        val context = getContext() ?: return "No context available"
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(
            android.content.ClipData.newPlainText("HealthCoach Share", text)
        )
        Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
        return "Text copied to clipboard"
    }

    actual fun copyImageToClipboard(bitmap: ImageBitmap): String? {
        val context = getContext() ?: return "No context available"
        val imageFile = saveBitmapToFile(context, bitmap) ?: return "Failed to save image"
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(
            android.content.ClipData.newUri(context.contentResolver, "HealthCoach Image", uri)
        )
        Toast.makeText(context, "Image copied to clipboard", Toast.LENGTH_SHORT).show()
        return "Image copied to clipboard"
    }

    private fun saveBitmapToFile(context: Context, bitmap: ImageBitmap): File? {
        val tempFile = File(context.cacheDir, "healthcoach_share_${System.currentTimeMillis()}.png")
        return try {
            val androidBitmap = bitmap.toAndroidBitmap()
            val outputStream = FileOutputStream(tempFile)
            androidBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun ImageBitmap.toAndroidBitmap(): android.graphics.Bitmap {
        val pixels = IntArray(width * height)
        readPixels(pixels)
        return android.graphics.Bitmap.createBitmap(pixels, width, height, android.graphics.Bitmap.Config.ARGB_8888)
    }
}
