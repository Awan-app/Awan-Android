package com.awan.app.core.network.api

import com.awan.app.core.network.dto.CreateTaskRequest
import com.awan.app.core.network.dto.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.TaskInfoResponse
import com.awan.app.core.network.dto.TaskWithSessionsResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface TaskApiService {

    @POST("v1/tasks")
    suspend fun createTask(
        @Body request: CreateTaskRequest,
    ): TaskInfoResponse

    @POST("v1/tasks/with-sessions")
    suspend fun createTaskWithSessions(
        @Body request: CreateTaskWithSessionsRequest,
    ): TaskWithSessionsResponse
}
