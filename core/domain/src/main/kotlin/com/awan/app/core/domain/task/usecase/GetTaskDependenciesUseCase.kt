package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.model.Task
import javax.inject.Inject

class GetTaskDependenciesUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String): Result<List<Task>> =
        taskRepository.getTaskDependencies(taskId)
}
