package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.model.Task
import javax.inject.Inject

class CompleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String): Result<Task> {
        return repository.completeTask(taskId)
    }
}
