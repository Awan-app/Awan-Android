package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.TaskSession
import javax.inject.Inject

class AddTaskSessionsUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(taskId: String, sessions: List<SessionDraft>): Result<List<TaskSession>> =
        taskRepository.addTaskSessions(taskId, sessions)
}
