package com.awan.app.core.data.task.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.CreateTaskRequest
import com.awan.app.core.network.dto.TaskInfoResponse

interface TaskRemoteDataSource {
    suspend fun createTask(request: CreateTaskRequest): Result<TaskInfoResponse>
}
