package com.awan.app.core.domain.profile.usecase

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.error.ValidationReason
import com.awan.app.core.common.result.Result as AppResult
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.profile.repository.ProfileRepository
import javax.inject.Inject

class UpdateProfilePictureUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(imageBytes: ByteArray, mimeType: String): AppResult<Profile> =
        when {
            mimeType.lowercase() !in SUPPORTED_MIME_TYPES ->
                AppResult.Error(AppError.Validation(ValidationReason.IMAGE_TYPE_UNSUPPORTED))

            imageBytes.size > MAX_IMAGE_BYTES ->
                AppResult.Error(AppError.Validation(ValidationReason.IMAGE_TOO_LARGE))

            else -> repository.updateProfilePicture(imageBytes, mimeType)
        }

    companion object {
        const val MAX_IMAGE_BYTES = 5 * 1024 * 1024
        val SUPPORTED_MIME_TYPES = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")
    }
}
