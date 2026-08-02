package com.awan.app.core.data.task.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.AiTaskPreviewResponse
import com.awan.app.core.network.dto.task.CreateTaskRequest
import com.awan.app.core.network.dto.task.CreateTaskWithAiRequest
import com.awan.app.core.network.dto.task.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.task.ScheduleTaskRequest
import com.awan.app.core.network.dto.task.TaskInfoResponse
import com.awan.app.core.network.dto.task.TaskScheduleResponse
import com.awan.app.core.network.dto.task.TaskWithSessionsDto

interface TaskRemoteDataSource {
    suspend fun createTask(request: CreateTaskRequest): Result<TaskInfoResponse>

    suspend fun createTaskWithSessions(request: CreateTaskWithSessionsRequest): Result<TaskWithSessionsDto>

    /** Persists straight away. Used only by onboarding's first-task flow. */
    suspend fun createTaskWithAi(request: CreateTaskWithAiRequest): Result<TaskWithSessionsDto>

    /** Preview only — nothing is saved. Used by the add-task sheet. */
    suspend fun previewTaskWithAi(request: CreateTaskWithAiRequest): Result<AiTaskPreviewResponse>

    suspend fun scheduleTask(request: ScheduleTaskRequest): Result<TaskScheduleResponse>

    suspend fun deleteTask(taskId: String): Result<Unit>
}
