package com.awan.app.core.network.api

import com.awan.app.core.network.dto.CreateTaskRequest
import com.awan.app.core.network.dto.CreateTaskWithAiRequest
import com.awan.app.core.network.dto.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.ScheduleTaskRequest
import com.awan.app.core.network.dto.TaskInfoResponse
import com.awan.app.core.network.dto.TaskScheduleResponse
import com.awan.app.core.network.dto.TaskWithSessionsResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TaskApiService {

    @POST("v1/tasks")
    suspend fun createTask(
        @Body request: CreateTaskRequest,
    ): TaskInfoResponse

    @POST("v1/tasks/with-sessions")
    suspend fun createTaskWithSessions(
        @Body request: CreateTaskWithSessionsRequest,
    ): TaskWithSessionsResponse

    /**
     * Persists a task straight away with every field the model chose. This is not a preview — the
     * returned id is a real task already sitting in the user's Inbox, so any path that abandons the
     * flow it feeds has to delete it.
     */
    @POST("v1/ai/task-create")
    suspend fun createTaskWithAi(
        @Body request: CreateTaskWithAiRequest,
    ): TaskInfoResponse

    @POST("v1/schedule/task")
    suspend fun scheduleTask(
        @Body request: ScheduleTaskRequest,
    ): TaskScheduleResponse

    @DELETE("v1/tasks/{taskId}")
    suspend fun deleteTask(
        @Path("taskId") taskId: String,
        @Query("cascade") cascade: Boolean = false,
    )
}
