package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.model.TaskSchedule
import javax.inject.Inject

class ScheduleTaskWithAiUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String): Result<TaskSchedule> =
        taskRepository.scheduleTask(taskId)
}
