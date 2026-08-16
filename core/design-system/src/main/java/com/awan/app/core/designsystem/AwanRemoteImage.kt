package com.awan.app.core.designsystem

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun AwanRemoteImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    if (url.isNullOrBlank()) {
        Log.d("AwanRemoteImage", "[AwanRemoteImage] url is null or blank, skipping render")
        return
    }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .listener(
                onStart = { request ->
                    Log.d("AwanRemoteImage", "[AwanRemoteImage] Request START: ${request.data}")
                },
                onSuccess = { request, result ->
                    Log.d("AwanRemoteImage", "[AwanRemoteImage] Request SUCCESS: ${request.data} from ${result.dataSource}")
                },
                onError = { request, result ->
                    Log.e(
                        "AwanRemoteImage",
                        "[AwanRemoteImage] Request FAILED: ${request.data}, error: ${result.throwable.message}",
                        result.throwable
                    )
                }
            )
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}
