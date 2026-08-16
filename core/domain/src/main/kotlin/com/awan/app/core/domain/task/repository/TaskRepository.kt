package com.awan.app.core.domain.task.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskDraft
import com.awan.app.core.model.TaskProposals
import com.awan.app.core.model.TaskSchedule
import com.awan.app.core.model.TaskSession
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

    suspend fun deleteTask(taskId: String, cascade: Boolean = false): Result<Unit>

    /**
     * Returns all tasks whose [Task.goalId] is null (the Inbox), each bundled with their sessions.
     * Session list may be empty for drafted tasks.
     */
    suspend fun getInboxTasks(): Result<List<TaskWithSessions>>

    // ── Task Details ──────────────────────────────────────────────────────────

    /** Fetches a single task by its ID. */
    suspend fun getTask(taskId: String): Result<Task> = error("not implemented")

    /**
     * Partially updates a task. Any field that is `null` is left unchanged by the backend.
     * Returns the updated task.
     */
    suspend fun updateTask(
        taskId: String,
        title: String? = null,
        description: String? = null,
        estimatedDuration: Int? = null,
        status: String? = null,
        mandatory: Boolean? = null,
        estimatedPoints: Int? = null,
        allowTaskSplitting: Boolean? = null,
        categoryId: String? = null,
    ): Result<Task> = error("not implemented")

    /**
     * Moves a task to a different goal (or to the inbox if [goalId] is null). The backend enforces that all dependency links on the task
     * must be removed before a move is allowed (dependencies are same-goal only).
     */
    suspend fun moveTask(taskId: String, goalId: String?): Result<Task> = error("not implemented")

    // ── Dependencies ──────────────────────────────────────────────────────────

    /**
     * Creates a prerequisite link: [taskId] will depend on [dependsOnTaskId].
     * Both tasks must be in the same goal. A cycle or self-reference is rejected by the backend.
     */
    suspend fun addDependency(taskId: String, dependsOnTaskId: String): Result<Unit> = error("not implemented")

    /** Removes the dependency link between [taskId] and [dependsOnTaskId]. */
    suspend fun removeDependency(taskId: String, dependsOnTaskId: String): Result<Unit> = error("not implemented")

    /** Returns all tasks that [taskId] directly depends on (prerequisites). */
    suspend fun getTaskDependencies(taskId: String): Result<List<Task>> = Result.Success(emptyList())

    /** Returns all downstream tasks that depend on [taskId] (successors). */
    suspend fun getTaskDependents(taskId: String): Result<List<Task>> = Result.Success(emptyList())

    /** Returns all tasks that belong to [goalId]. Used to populate the dependency picker. */
    suspend fun getTasksByGoal(goalId: String): Result<List<Task>> = Result.Success(emptyList())

    // ── Sessions ──────────────────────────────────────────────────────────────

    /** Returns all sessions booked for [taskId], optionally filtered by [status]. */
    suspend fun getTaskSessions(taskId: String, status: String? = null): Result<List<TaskSession>> = Result.Success(emptyList())

    /** Adds new calendar sessions to an existing task. */
    suspend fun addTaskSessions(taskId: String, sessions: List<SessionDraft>): Result<List<TaskSession>> = error("not implemented")
}
