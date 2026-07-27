package com.awan.app.core.data.task.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.CreateAiTaskRequest
import com.awan.app.core.network.dto.CreateTaskRequest
import com.awan.app.core.network.dto.ScheduleTaskRequest
import com.awan.app.core.network.dto.TaskInfoResponse
import com.awan.app.core.network.dto.TaskScheduleResponse

interface TaskRemoteDataSource {
    suspend fun createTask(request: CreateTaskRequest): Result<TaskInfoResponse>

    suspend fun createTaskWithAi(request: CreateAiTaskRequest): Result<TaskInfoResponse>

    suspend fun scheduleTask(request: ScheduleTaskRequest): Result<TaskScheduleResponse>
}
