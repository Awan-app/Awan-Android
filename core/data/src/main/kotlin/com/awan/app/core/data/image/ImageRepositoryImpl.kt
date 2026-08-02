package com.awan.app.core.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.image.repository.ImageRepository
import com.awan.app.core.model.ImageBytes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private data class ImageDimensions(val width: Int, val height: Int)

@Singleton
class ImageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ImageRepository {

    override suspend fun read(uri: String): Result<ImageBytes> = withContext(ioDispatcher) {
        val parsed = Uri.parse(uri)
        val dimensions = decodeDimensions(parsed) ?: return@withContext Result.Error(AppError.Unknown())
        val sampleSize = sampleSizeFor(dimensions)
        val bitmap = runCatching {
            context.contentResolver.openInputStream(parsed)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            }
        }.getOrNull() ?: return@withContext Result.Error(AppError.Unknown())

        val bytes = bitmap.scaleToMaxEdge(MAX_EDGE).compressUnder(ImageBytes.MAX_BYTES)
        Result.Success(ImageBytes(bytes = bytes, mimeType = "image/jpeg"))
    }

    private fun decodeDimensions(uri: Uri): ImageDimensions? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // A bounds-only decode always returns null; the dimensions it wrote are the only signal.
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }
        return ImageDimensions(options.outWidth, options.outHeight).takeIf { it.width > 0 && it.height > 0 }
    }

    /** Coarse power-of-two downsample during decode, so a huge photo never fully loads before scaling. */
    private fun sampleSizeFor(dimensions: ImageDimensions): Int {
        var sample = 1
        while (dimensions.width / sample > MAX_EDGE * 2 || dimensions.height / sample > MAX_EDGE * 2) {
            sample *= 2
        }
        return sample
    }

    /**
     * Steps quality down, then halves the bitmap, until the JPEG fits the upload cap — an oversized
     * photo is compressed instead of rejected. Terminates: every pass either drops quality or halves
     * both edges, and a bitmap too small to halve returns whatever it produced.
     */
    private fun Bitmap.compressUnder(maxBytes: Int): ByteArray {
        var bitmap = this
        var quality = JPEG_QUALITY
        while (true) {
            val bytes = bitmap.toJpeg(quality)
            if (bytes.size <= maxBytes) return bytes
            when {
                quality > MIN_JPEG_QUALITY -> quality -= QUALITY_STEP
                bitmap.width >= 2 && bitmap.height >= 2 -> {
                    bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width / 2, bitmap.height / 2, true)
                    quality = JPEG_QUALITY
                }

                else -> return bytes
            }
        }
    }

    private fun Bitmap.toJpeg(quality: Int): ByteArray = ByteArrayOutputStream().use { out ->
        compress(Bitmap.CompressFormat.JPEG, quality, out)
        out.toByteArray()
    }

    private fun Bitmap.scaleToMaxEdge(maxEdge: Int): Bitmap {
        val longestEdge = maxOf(width, height)
        if (longestEdge <= maxEdge) return this
        val scale = maxEdge.toFloat() / longestEdge
        return Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
    }

    private companion object {
        const val MAX_EDGE = 2048
        const val JPEG_QUALITY = 85
        const val MIN_JPEG_QUALITY = 40
        const val QUALITY_STEP = 15
    }
}
