package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.model.Task
import javax.inject.Inject

/**
 * Turns a plain sentence into a fully specified task. The returned task is already saved — whoever
 * calls this owns cleaning it up if the user then backs out.
 */
class CreateTaskWithAiUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(title: String, description: String? = null): Result<Task> =
        taskRepository.createTaskWithAi(title.trim(), description?.takeIf { it.isNotBlank() })
}
