package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String): Result<Unit> = taskRepository.deleteTask(taskId)
}
