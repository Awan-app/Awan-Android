package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.model.AiTaskSuggestion
import javax.inject.Inject

/**
 * Turns a plain sentence into a proposed task. Nothing is saved — the caller creates the real task
 * (via [CreateTaskUseCase]) once the user confirms, possibly after editing what Awan proposed.
 */
class PreviewTaskWithAiUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(title: String, description: String? = null): Result<AiTaskSuggestion> =
        taskRepository.previewTaskWithAi(title.trim(), description?.takeIf { it.isNotBlank() })
}
