package com.awan.app.core.network.util

import com.awan.app.core.network.BuildConfig

object UrlResolver {
    fun resolve(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return if (url.startsWith("/")) {
            BuildConfig.AWAN_BASE_URL.removeSuffix("/api/").removeSuffix("/") + url
        } else {
            url
        }
    }
}
