package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.model.Task
import javax.inject.Inject

/** Returns all tasks belonging to [goalId]. Used to populate the dependency picker sheet. */
class GetTasksByGoalUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(goalId: String): Result<List<Task>> =
        taskRepository.getTasksByGoal(goalId)
}
