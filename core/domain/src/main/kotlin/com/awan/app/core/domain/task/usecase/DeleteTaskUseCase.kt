package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    /**
     * Deletes a task.
     *
     * @param taskId the task to delete.
     * @param cascade when `true`, also deletes all tasks that depend on this one. When `false`
     *   (the default), the backend returns 409 Conflict if any dependents exist.
     */
    suspend operator fun invoke(taskId: String, cascade: Boolean = false): Result<Unit> =
        taskRepository.deleteTask(taskId, cascade)
}
