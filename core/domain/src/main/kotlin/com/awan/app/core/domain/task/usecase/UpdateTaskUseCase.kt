package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.model.Task
import javax.inject.Inject

class UpdateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(
        taskId: String,
        title: String? = null,
        description: String? = null,
        estimatedDuration: Int? = null,
        status: String? = null,
        mandatory: Boolean? = null,
        estimatedPoints: Int? = null,
        allowTaskSplitting: Boolean? = null,
        categoryId: String? = null,
    ): Result<Task> = taskRepository.updateTask(
        taskId = taskId,
        title = title,
        description = description,
        estimatedDuration = estimatedDuration,
        status = status,
        mandatory = mandatory,
        estimatedPoints = estimatedPoints,
        allowTaskSplitting = allowTaskSplitting,
        categoryId = categoryId,
    )
}
