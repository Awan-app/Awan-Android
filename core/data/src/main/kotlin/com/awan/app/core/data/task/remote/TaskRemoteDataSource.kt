package com.awan.app.core.data.task.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.CreateAiTaskRequest
import com.awan.app.core.network.dto.CreateTaskRequest
import com.awan.app.core.network.dto.CreateTaskWithAiRequest
import com.awan.app.core.network.dto.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.ScheduleTaskRequest
import com.awan.app.core.network.dto.TaskInfoResponse
import com.awan.app.core.network.dto.TaskScheduleResponse
import com.awan.app.core.network.dto.TaskWithSessionsResponse
import com.awan.app.core.network.dto.ScheduleTaskRequest

interface TaskRemoteDataSource {
    suspend fun createTask(request: CreateTaskRequest): Result<TaskInfoResponse>

    suspend fun createTaskWithSessions(request: CreateTaskWithSessionsRequest): Result<TaskWithSessionsResponse>

    suspend fun createTaskWithAi(request: CreateTaskWithAiRequest): Result<TaskInfoResponse>

    suspend fun scheduleTask(request: ScheduleTaskRequest): Result<TaskScheduleResponse>

    suspend fun deleteTask(taskId: String): Result<Unit>
}
