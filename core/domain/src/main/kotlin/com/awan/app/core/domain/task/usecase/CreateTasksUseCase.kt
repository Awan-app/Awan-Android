package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskWithSessionsDraft
import javax.inject.Inject

/** Accepts one or more proposals in a single atomic call. The backend caps a request at 50. */
class CreateTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(drafts: List<TaskWithSessionsDraft>): Result<List<Task>> =
        taskRepository.createTasksWithSessions(drafts.take(MAX_TASKS))

    private companion object {
        const val MAX_TASKS = 50
    }
}
