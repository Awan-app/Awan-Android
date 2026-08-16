package com.awan.app.core.domain.task.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskDraft
import com.awan.app.core.model.TaskProposals
import com.awan.app.core.model.TaskSchedule
import com.awan.app.core.model.TaskWithSessions
import com.awan.app.core.model.TaskWithSessionsDraft

interface TaskRepository {

    /** Creates an unscheduled task. A null `TaskDraft.goalId` puts it in the Inbox. */
    suspend fun createTask(draft: TaskDraft): Result<Task>

    /** Creates a task and its pre-scheduled sessions in one call. The backend caps [sessions] at 50. */
    suspend fun createTaskWithSessions(
        draft: TaskDraft,
        sessions: List<SessionDraft>,
    ): Result<TaskWithSessions>

    /** Creates multiple tasks (each with its own sessions) atomically. The backend caps [drafts] at 50. */
    suspend fun createTasksWithSessions(drafts: List<TaskWithSessionsDraft>): Result<List<Task>>

    /**
     * Hands [text] to the backend's model, which proposes one or more tasks — duration, points,
     * mandatory, splitting, category and timing. Nothing is persisted, so there is no task id and
     * nothing to clean up if the user backs out. Confirming a proposal goes through
     * [createTaskWithSessions] or [createTasksWithSessions] instead.
     */
    suspend fun proposeTasksFromText(text: String): Result<TaskProposals>

    /** Same proposal contract as [proposeTasksFromText], sourced from a photo. */
    suspend fun proposeTasksFromImage(image: ByteArray, mimeType: String, note: String?): Result<TaskProposals>

    /** Asks the scheduling engine to place an existing task. */
    suspend fun scheduleTask(taskId: String): Result<TaskSchedule>

    /** Marks a task as COMPLETED on the backend and updates it locally. */
    suspend fun completeTask(taskId: String): Result<Task>

    /** Moves a task to a different goal (or to the inbox if [goalId] is null). */
    suspend fun moveTask(taskId: String, goalId: String?): Result<Task>

    suspend fun deleteTask(taskId: String): Result<Unit>

    /**
     * Returns all tasks whose [Task.goalId] is null (the Inbox), each bundled with their sessions.
     * Session list may be empty for drafted tasks.
     */
    suspend fun getInboxTasks(): Result<List<TaskWithSessions>>
}
