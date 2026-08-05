package com.awan.app.core.domain.image.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.image.repository.ImageRepository
import com.awan.app.core.model.ImageBytes
import javax.inject.Inject

class ReadImageUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
) {
    suspend operator fun invoke(uri: String): Result<ImageBytes> = imageRepository.read(uri)
}
