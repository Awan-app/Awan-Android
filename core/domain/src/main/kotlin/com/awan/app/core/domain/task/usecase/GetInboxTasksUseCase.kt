package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.model.TaskWithSessions
import javax.inject.Inject

/**
 * Returns all inbox tasks (tasks not attached to any goal) with their sessions.
 *
 * Callers can derive a display-level [InboxTaskDisplayStatus] from the returned [TaskWithSessions]
 * without any additional network calls:
 * - **Drafted** — sessions list is empty.
 * - **Active** — at least one session has status SCHEDULED.
 * - **Completed** — no non-CANCELLED session is NOT completed, and at least one is COMPLETED.
 * - **Cancelled** — every session is CANCELLED.
 */
class GetInboxTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(): Result<List<TaskWithSessions>> = taskRepository.getInboxTasks()
}
