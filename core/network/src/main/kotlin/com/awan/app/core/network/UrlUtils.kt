package com.awan.app.core.network

/**
 * Resolves an image path returned by the backend against the server base URL (excluding the `/api` prefix).
 * If [imagePath] is already an absolute HTTP/HTTPS URL, it is returned directly.
 *
 * Example:
 * - `imagePath = "/images/store/twilight_01.png"`, `baseUrl = "https://example.com/api/"`
 *   -> `"https://example.com/images/store/twilight_01.png"`
 */
fun resolveBackendImageUrl(
    imagePath: String?,
    baseUrl: String = BuildConfig.AWAN_BASE_URL
): String? {
    if (imagePath.isNullOrBlank()) return null
    if (imagePath.startsWith("http://", ignoreCase = true) || imagePath.startsWith("https://", ignoreCase = true)) {
        return imagePath
    }
    val cleanBase = baseUrl.trimEnd('/').removeSuffix("/api")
    val cleanPath = imagePath.trimStart('/')
    return "$cleanBase/$cleanPath"
}
