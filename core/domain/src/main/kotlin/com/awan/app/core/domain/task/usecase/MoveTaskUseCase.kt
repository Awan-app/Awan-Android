package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.model.Task
import javax.inject.Inject

/** Moves a task to a different goal (or to the inbox if [goalId] is null). All dependency links must be cleared first (same-goal rule). */
class MoveTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String, goalId: String?): Result<Task> =
        taskRepository.moveTask(taskId, goalId)
}
