package com.awan.app.core.domain.task.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.model.AiTaskSuggestion
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskDraft
import com.awan.app.core.model.TaskSchedule
import com.awan.app.core.model.TaskWithSessions

interface TaskRepository {

    /** Creates an unscheduled task. A null `TaskDraft.goalId` puts it in the Inbox. */
    suspend fun createTask(draft: TaskDraft): Result<Task>

    /** Creates a task and its pre-scheduled sessions in one call. The backend caps [sessions] at 50. */
    suspend fun createTaskWithSessions(
        draft: TaskDraft,
        sessions: List<SessionDraft>,
    ): Result<TaskWithSessions>

    /**
     * Hands [title] and [description] to the backend's model, which proposes duration, points,
     * mandatory, splitting and category. This is a preview — nothing is persisted, so there is no
     * task id and nothing to clean up if the user backs out. Confirming it goes through [createTask]
     * or [createTaskWithSessions] instead.
     */
    suspend fun previewTaskWithAi(title: String, description: String?): Result<AiTaskSuggestion>

    /** Asks the scheduling engine to place an existing task. */
    suspend fun scheduleTask(taskId: String): Result<TaskSchedule>

    suspend fun deleteTask(taskId: String): Result<Unit>
}
