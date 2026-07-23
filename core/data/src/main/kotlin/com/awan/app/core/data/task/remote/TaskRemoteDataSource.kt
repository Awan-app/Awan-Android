package com.awan.app.core.data.task.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.CreateTaskRequest
import com.awan.app.core.network.dto.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.TaskInfoResponse
import com.awan.app.core.network.dto.TaskWithSessionsResponse

interface TaskRemoteDataSource {
    suspend fun createTask(request: CreateTaskRequest): Result<TaskInfoResponse>

    suspend fun createTaskWithSessions(request: CreateTaskWithSessionsRequest): Result<TaskWithSessionsResponse>
}
