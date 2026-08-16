package com.awan.app.core.domain.task.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.domain.zones.usecase.GetZonesForDateUseCase
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskDraft
import javax.inject.Inject

/**
 * The single entry point for creating a task. A draft with no `startAt` has nothing to schedule, so
 * it becomes a plain Inbox task; a draft with one becomes a task plus its first session, which the
 * backend only accepts as one combined `with-sessions` call.
 *
 * This is also where the two halves of the zone/category split meet: the draft names a *category*,
 * and the session needs the *zone* that hands that category a window on the chosen day. A day with
 * no matching window still schedules — the session is simply zone-less rather than refused.
 */
class CreateTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val getZonesForDate: GetZonesForDateUseCase,
) {
    suspend operator fun invoke(draft: TaskDraft): Result<Task> {
        val start = draft.startAt ?: return taskRepository.createTask(draft)
        val minutes = draft.durationMinutes ?: TaskDraft.DEFAULT_DURATION_MINUTES
        val session = SessionDraft(
            start = start,
            end = start.plusMinutes(minutes.toLong()),
            zoneId = zoneIdFor(draft),
        )
        return when (val result = taskRepository.createTaskWithSessions(draft, listOf(session))) {
            is Result.Success -> Result.Success(result.data.task)
            is Result.Error -> result
            Result.Loading -> Result.Loading
        }
    }

    /** A failed zone lookup is not a failed create — the session just lands without a zone. */
    private suspend fun zoneIdFor(draft: TaskDraft): String? {
        val categoryId = draft.categoryId ?: return null
        val date = draft.startAt?.toLocalDate() ?: return null
        val zones = when (val result = getZonesForDate(date)) {
            is Result.Success -> result.data
            else -> return null
        }
        return zones.firstOrNull { it.category?.id == categoryId }?.id
    }
}
