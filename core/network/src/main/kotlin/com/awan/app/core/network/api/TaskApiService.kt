package com.awan.app.core.network.api

import com.awan.app.core.network.dto.task.CreateTaskRequest
import com.awan.app.core.network.dto.task.TaskInfoResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface TaskApiService {

    @POST("v1/tasks")
    suspend fun createTask(
        @Body request: CreateTaskRequest,
    ): TaskInfoResponse
}
