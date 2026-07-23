package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskDraft
import javax.inject.Inject

/**
 * The single entry point for creating a task. A draft with no `startAt` has nothing to schedule, so
 * it becomes a plain Inbox task; a draft with one becomes a task plus its first session, which the
 * backend only accepts as one combined `with-sessions` call.
 */
class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
) {
    suspend operator fun invoke(draft: TaskDraft): Result<Task> {
        val start = draft.startAt ?: return taskRepository.createTask(draft)
        val minutes = draft.durationMinutes ?: TaskDraft.DEFAULT_DURATION_MINUTES
        val session = SessionDraft(
            start = start,
            end = start.plusMinutes(minutes.toLong()),
            zoneId = draft.zoneId,
        )
        return when (val result = taskRepository.createTaskWithSessions(draft, listOf(session))) {
            is Result.Success -> Result.Success(result.data.task)
            is Result.Error -> result
            Result.Loading -> Result.Loading
        }
    }
}
