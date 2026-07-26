package com.awan.app.core.network.api

import com.awan.app.core.network.dto.CreateTaskRequest
import com.awan.app.core.network.dto.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.TaskInfoResponse
import com.awan.app.core.network.dto.TaskWithSessionsDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TaskApiService {

    @POST("v1/tasks")
    suspend fun createTask(
        @Body request: CreateTaskRequest,
    ): TaskInfoResponse

    @GET("v1/tasks/date/{date}")
    suspend fun getTasksByDate(
        @Path("date") date: String,
    ): List<TaskWithSessionsDto>

    @POST("v1/tasks/with-sessions")
    suspend fun createTaskWithSessions(
        @Body request: CreateTaskWithSessionsRequest,
    ): TaskWithSessionsDto
}

