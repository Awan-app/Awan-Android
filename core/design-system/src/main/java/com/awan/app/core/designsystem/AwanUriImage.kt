package com.awan.app.core.designsystem

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Every call site here draws a small thumbnail; a full camera photo would be ~50 MB in memory. */
private const val ThumbnailMaxEdgePx = 512

/**
 * Creates a unique `content://` URI for a camera to write to, backed by a file in the app's cache.
 * Authority matches `${applicationId}.fileprovider` as declared in the manifest.
 */
fun createCameraOutputUri(context: Context, subDir: String = "images"): Uri {
    val dir = File(context.cacheDir, subDir).apply { mkdirs() }
    val file = File(dir, "captured_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/**
 * Decodes a `content://` (or any resolver-readable) [uri] into a bitmap and draws it. Renders
 * nothing while decoding or on failure — callers that need a placeholder wrap this themselves.
 */
@Composable
fun AwanUriImage(uri: String, contentDescription: String?, modifier: Modifier = Modifier) {
    val resolver = LocalContext.current.contentResolver
    val decoded by produceState(initialValue = null as Bitmap?, uri) {
        value = withContext(Dispatchers.IO) { resolver.decodeThumbnail(uri) }
    }
    decoded?.let {
        Image(bitmap = it.asImageBitmap(), contentDescription = contentDescription, modifier = modifier)
    }
}

/** Bounds pass first, so the full-size bitmap never lands in memory on the way to the thumbnail. */
private fun ContentResolver.decodeThumbnail(uri: String): Bitmap? = runCatching {
    val parsed = Uri.parse(uri)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }

    var sample = 1
    while (bounds.outWidth / sample > ThumbnailMaxEdgePx || bounds.outHeight / sample > ThumbnailMaxEdgePx) {
        sample *= 2
    }
    openInputStream(parsed)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}.getOrNull()
