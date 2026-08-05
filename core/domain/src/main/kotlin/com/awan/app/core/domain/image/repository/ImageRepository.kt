package com.awan.app.core.domain.image.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.model.ImageBytes

/** Reads a picked or captured image, downscaled so it never exceeds an upload's size limit. */
interface ImageRepository {
    suspend fun read(uri: String): Result<ImageBytes>
}
