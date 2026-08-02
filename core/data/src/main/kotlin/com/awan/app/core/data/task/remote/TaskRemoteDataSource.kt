package com.awan.app.core.data.task.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.AiTextToTasksRequest
import com.awan.app.core.network.dto.BulkCreateTasksWithSessionsRequest
import com.awan.app.core.network.dto.CreateTaskRequest
import com.awan.app.core.network.dto.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.ScheduleTaskRequest
import com.awan.app.core.network.dto.TaskInfoResponse
import com.awan.app.core.network.dto.TaskProposalResponse
import com.awan.app.core.network.dto.TaskScheduleResponse
import com.awan.app.core.network.dto.TaskWithSessionsDto
import com.awan.app.core.network.dto.TasksWithSessionsResponse

interface TaskRemoteDataSource {
    suspend fun createTask(request: CreateTaskRequest): Result<TaskInfoResponse>

    suspend fun createTaskWithSessions(request: CreateTaskWithSessionsRequest): Result<TaskWithSessionsDto>

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

    suspend fun scheduleTask(request: ScheduleTaskRequest): Result<TaskScheduleResponse>

    suspend fun deleteTask(taskId: String): Result<Unit>
}
