package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import javax.inject.Inject

class AddTaskDependencyUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String, dependsOnTaskId: String): Result<Unit> =
        taskRepository.addDependency(taskId, dependsOnTaskId)
}
