package com.awan.app.core.data.task

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.task.TaskInfoResponse
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(
        title: String,
        description: String? = null,
        estimatedDurationMinutes: Int? = null,
        mandatory: Boolean? = false,
        estimatedPoints: Int? = 0,
        allowTaskSplitting: Boolean? = false,
        goalId: String? = null,
    ): Result<TaskInfoResponse> = taskRepository.createTask(
        title = title,
        description = description,
        estimatedDurationMinutes = estimatedDurationMinutes,
        mandatory = mandatory,
        estimatedPoints = estimatedPoints,
        allowTaskSplitting = allowTaskSplitting,
        goalId = goalId,
    )
}
