package com.lbthomas.healthcoach.core.share

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Platform-agnostic sharing API.
 *
 * Common interface in commonMain with platform-specific implementations
 * in androidMain (Intent.ACTION_SEND) and jvmMain (AWT clipboard + Desktop.browse).
 */
expect object ShareApi {
    /**
     * Share plain text.
     * On Android: opens the native share sheet with text.
     * On Desktop: copies text to clipboard.
     * Returns a user-facing feedback message, or null if the action is silent (e.g., native share sheet).
     */
    fun shareText(text: String): String?

    /**
     * Share an image.
     * On Android: opens the native share sheet with an image.
     * On Desktop: copies the image to clipboard.
     * Returns a user-facing feedback message, or null if the action is silent.
     */
    fun shareImage(bitmap: ImageBitmap): String?

    /**
     * Share text and an image together.
     * On Android: opens the native share sheet with both text and image.
     * On Desktop: copies both to clipboard (image takes precedence).
     * Returns a user-facing feedback message, or null if the action is silent.
     */
    fun shareTextAndImage(text: String, bitmap: ImageBitmap): String?

    /**
     * Open a URL in the default browser.
     * On Android: launches a browser Intent.
     * On Desktop: uses java.awt.Desktop.browse().
     * Returns a user-facing feedback message, or null if the action is silent.
     */
    fun openShareUrl(url: String): String?

    /**
     * Copy text to the system clipboard.
     * Returns a user-facing feedback message.
     */
    fun copyTextToClipboard(text: String): String?

    /**
     * Copy an image to the system clipboard.
     * Returns a user-facing feedback message.
     */
    fun copyImageToClipboard(bitmap: ImageBitmap): String?
}

// URL builder helpers — pure functions, no platform dependency

private fun encodeUrl(s: String): String {
    // Use Java's URLEncoder for URL encoding
    return java.net.URLEncoder.encode(s, "UTF-8")
}

fun buildXUrl(text: String): String {
    return "https://twitter.com/intent/tweet?text=${encodeUrl(text)}"
}

fun buildFacebookUrl(text: String): String {
    return "https://www.facebook.com/sharer/sharer.php?quote=${encodeUrl(text)}"
}

fun buildRedditUrl(text: String): String {
    return "https://reddit.com/submit?url=&title=${encodeUrl(text)}"
}

fun buildMastodonUrl(text: String, instance: String): String {
    val host = if (instance.startsWith("http://") || instance.startsWith("https://")) {
        instance
    } else {
        "https://$instance"
    }
    return "$host/share?text=${encodeUrl(text)}"
}
