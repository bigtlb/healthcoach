package com.lbthomas.healthcoach.core.share

import androidx.compose.ui.graphics.ImageBitmap
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.image.BufferedImage
import java.net.URI
import javax.imageio.ImageIO

actual object ShareApi {

    actual fun shareText(text: String): String? {
        return copyTextToClipboard(text)
    }

    actual fun shareImage(bitmap: ImageBitmap): String? {
        return copyImageToClipboard(bitmap)
    }

    actual fun shareTextAndImage(text: String, bitmap: ImageBitmap): String? {
        // Desktop clipboard can hold only one item at a time;
        // copy the image (more valuable)
        return copyImageToClipboard(bitmap)
    }

    actual fun openShareUrl(url: String): String? {
        try {
            // Try java.awt.Desktop first
            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    java.awt.EventQueue.invokeLater {
                        try {
                            desktop.browse(URI.create(url))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    return "Opening in browser..."
                }
            }
            // Fallback: use platform-specific process launcher (xdg-open on Linux, start on Windows, open on macOS)
            return openUrlViaProcess(url)
        } catch (e: Exception) {
            // If everything fails, try process launcher as last resort
            return openUrlViaProcess(url) ?: "Failed to open browser: ${e.message}"
        }
    }

    private fun openUrlViaProcess(url: String): String? {
        try {
            val os = System.getProperty("os.name").lowercase()
            val command = when {
                os.contains("linux") -> listOf("xdg-open", url)
                os.contains("mac") -> listOf("open", url)
                os.contains("windows") -> listOf("cmd", "/c", "start", url)
                else -> return null
            }
            ProcessBuilder(command).start()
            return "Opening in browser..."
        } catch (e: Exception) {
            return "Failed to open browser: ${e.message}"
        }
    }

    actual fun copyTextToClipboard(text: String): String? {
        try {
            java.awt.EventQueue.invokeLater {
                try {
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    clipboard.setContents(StringSelection(text), null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return "Text copied to clipboard"
        } catch (e: Exception) {
            return "Failed to copy to clipboard: ${e.message}"
        }
    }

    actual fun copyImageToClipboard(bitmap: ImageBitmap): String? {
        try {
            java.awt.EventQueue.invokeLater {
                try {
                    val bufferedImage = imageBitmapToBufferedImage(bitmap)
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    clipboard.setContents(ImageTransferable(bufferedImage), null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return "Image copied to clipboard"
        } catch (e: Exception) {
            return "Failed to copy to clipboard: ${e.message}"
        }
    }

    private fun imageBitmapToBufferedImage(bitmap: ImageBitmap): BufferedImage {
        val bufferedImage = BufferedImage(bitmap.width, bitmap.height, BufferedImage.TYPE_INT_ARGB)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.readPixels(pixels)
        bufferedImage.setRGB(0, 0, bitmap.width, bitmap.height, pixels, 0, bitmap.width)
        return bufferedImage
    }

    private class ImageTransferable(private val image: BufferedImage) : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> {
            return arrayOf(DataFlavor.imageFlavor)
        }

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
            return flavor == DataFlavor.imageFlavor
        }

        override fun getTransferData(flavor: DataFlavor): Any {
            if (flavor == DataFlavor.imageFlavor) {
                return image
            }
            throw java.io.IOException("Unsupported flavor: $flavor")
        }
    }
}
