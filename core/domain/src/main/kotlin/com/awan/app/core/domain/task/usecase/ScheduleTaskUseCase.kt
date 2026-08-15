package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.model.TaskSchedule
import javax.inject.Inject

/** Asks the AI scheduling engine to place an existing task on the calendar. */
class ScheduleTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String): Result<TaskSchedule> =
        taskRepository.scheduleTask(taskId)
}
