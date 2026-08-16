package com.awan.app.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun AwanRemoteImage(
    url: String?,
    modifier: Modifier = Modifier,
    accessToken: String? = null,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (url.isNullOrBlank()) return

    // Prepend base URL if relative
    val fullUrl = if (url.startsWith("/")) {
        "https://backend-production-c701.up.railway.app$url"
    } else {
        url
    }

    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(fullUrl)
        .apply {
            if (accessToken != null) {
                val headers = NetworkHeaders.Builder()
                    .set("Authorization", "Bearer $accessToken")
                    .build()
                httpHeaders(headers)
            }
        }
        .crossfade(true)
        .build()

    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}
