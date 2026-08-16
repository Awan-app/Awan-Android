package com.awan.app.core.data.task.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.task.AddTaskSessionsRequest
import com.awan.app.core.network.dto.task.AiTextToTasksRequest
import com.awan.app.core.network.dto.task.BulkCreateTasksWithSessionsRequest
import com.awan.app.core.network.dto.task.CreateTaskRequest
import com.awan.app.core.network.dto.task.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.task.ScheduleTaskRequest
import com.awan.app.core.network.dto.task.TaskCompletionResponse
import com.awan.app.core.network.dto.task.TaskDependencyRequest
import com.awan.app.core.network.dto.task.TaskInfoResponse
import com.awan.app.core.network.dto.task.TaskMoveRequest
import com.awan.app.core.network.dto.task.TaskProposalResponse
import com.awan.app.core.network.dto.task.TaskScheduleResponse
import com.awan.app.core.network.dto.task.TaskUpdateRequest
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import com.awan.app.core.network.dto.task.TasksWithSessionsResponse

interface TaskRemoteDataSource {
    suspend fun createTask(request: CreateTaskRequest): Result<TaskInfoResponse>

    suspend fun createTaskWithSessions(request: CreateTaskWithSessionsRequest): Result<TaskWithSessionsDto>

    suspend fun updateTask(
        taskId: String,
        request: com.awan.app.core.network.dto.task.TaskUpdateRequest,
    ): Result<TaskInfoResponse>

    suspend fun createTasksWithSessions(
        request: BulkCreateTasksWithSessionsRequest,
    ): Result<TasksWithSessionsResponse>

    /** Nothing is saved — every proposal carries a ready-to-POST draft. */
    suspend fun proposeTasksFromText(request: AiTextToTasksRequest): Result<TaskProposalResponse>

    /** Same proposal contract, sourced from a photo. [note] is optional extra context. */
    suspend fun proposeTasksFromImage(
        image: ByteArray,
        mimeType: String,
        note: String?,
    ): Result<TaskProposalResponse>

    suspend fun getTasksByRange(
        startDate: String,
        endDate: String,
    ): Result<Map<String, List<TaskWithSessionsDto>>>

    suspend fun scheduleTask(request: ScheduleTaskRequest): Result<TaskScheduleResponse>

    suspend fun completeTask(taskId: String): Result<TaskCompletionResponse>

    suspend fun deleteTask(taskId: String, cascade: Boolean = false): Result<Unit>


    /** Fetches all inbox tasks (tasks with no goal) together with their sessions. */
    suspend fun getInboxTasks(): Result<List<TaskWithSessionsDto>>

    // ── Task Details ─────────────────────────────────────────────────────────

    suspend fun getTask(taskId: String): Result<TaskInfoResponse>

    suspend fun updateTask(taskId: String, request: TaskUpdateRequest): Result<TaskInfoResponse>

    suspend fun moveTask(taskId: String, request: TaskMoveRequest): Result<TaskInfoResponse>

    suspend fun addDependency(taskId: String, request: TaskDependencyRequest): Result<Unit>

    suspend fun removeDependency(taskId: String, dependsOnTaskId: String): Result<Unit>

    suspend fun getTaskDependencies(taskId: String): Result<List<TaskInfoResponse>>

    suspend fun getTaskDependents(taskId: String): Result<List<TaskInfoResponse>>

    suspend fun getTaskSessions(taskId: String, status: String?): Result<List<SessionDto>>

    suspend fun addTaskSessions(taskId: String, request: AddTaskSessionsRequest): Result<List<SessionDto>>
}

