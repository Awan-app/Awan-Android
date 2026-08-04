package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.error.ValidationReason
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.model.ImageBytes
import com.awan.app.core.model.TaskProposals
import javax.inject.Inject

/**
 * Same proposal contract as [ProposeTasksFromTextUseCase], sourced from a photo instead of typed text.
 *
 * The size and format checks run before the upload — a 12 MB photo would otherwise spend the whole
 * transfer only to come back as a generic 400.
 */
class ProposeTasksFromImageUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(image: ImageBytes, note: String? = null): Result<TaskProposals> =
        when {
            image.mimeType.lowercase() !in SUPPORTED_MIME_TYPES ->
                Result.Error(AppError.Validation(ValidationReason.IMAGE_TYPE_UNSUPPORTED))

            image.bytes.size > MAX_IMAGE_BYTES ->
                Result.Error(AppError.Validation(ValidationReason.IMAGE_TOO_LARGE))

            else -> taskRepository.proposeTasksFromImage(
                image = image.bytes,
                mimeType = image.mimeType,
                note = note?.trim()?.takeIf { it.isNotBlank() },
            )
        }

    companion object {
        /** Both limits are what `POST /v1/ai/image-to-tasks` enforces. */
        const val MAX_IMAGE_BYTES = ImageBytes.MAX_BYTES
        val SUPPORTED_MIME_TYPES = setOf("image/png", "image/jpeg", "image/webp", "image/gif")
    }
}
