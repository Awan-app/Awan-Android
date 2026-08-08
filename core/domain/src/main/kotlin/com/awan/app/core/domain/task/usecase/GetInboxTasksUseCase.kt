package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.model.TaskWithSessions
import javax.inject.Inject

/**
 * Returns all inbox tasks (tasks not attached to any goal) with their sessions.
 */
class GetInboxTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(): Result<List<TaskWithSessions>> = taskRepository.getInboxTasks()
}
