package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.error.ValidationReason
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.model.TaskProposals
import javax.inject.Inject

/**
 * Turns a plain sentence into one or more proposed tasks. Nothing is saved — the caller creates the
 * real tasks (via [CreateTasksUseCase] or [CreateTaskUseCase]) once the user confirms, possibly
 * after editing what Awan proposed.
 *
 * Blank and over-length input is rejected here rather than round-tripped: the backend answers both
 * with the same generic 422, which cannot tell the user which one they hit.
 */
class ProposeTasksFromTextUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(text: String): Result<TaskProposals> {
        val trimmed = text.trim()
        return when {
            trimmed.isEmpty() -> Result.Error(AppError.Validation(ValidationReason.TEXT_BLANK))
            trimmed.length > MAX_TEXT_LENGTH -> Result.Error(AppError.Validation(ValidationReason.TEXT_TOO_LONG))
            else -> taskRepository.proposeTasksFromText(trimmed)
        }
    }

    companion object {
        /** `POST /v1/ai/task-create` rejects anything longer. */
        const val MAX_TEXT_LENGTH = 4000
    }
}
