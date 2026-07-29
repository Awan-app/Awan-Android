package com.awan.app.core.network.api

import com.awan.app.core.network.dto.AiTaskPreviewResponse
import com.awan.app.core.network.dto.CreateTaskRequest
import com.awan.app.core.network.dto.CreateTaskWithAiRequest
import com.awan.app.core.network.dto.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.ScheduleTaskRequest
import com.awan.app.core.network.dto.TaskInfoResponse
import com.awan.app.core.network.dto.TaskScheduleResponse
import com.awan.app.core.network.dto.TaskWithSessionsDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
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
    ): TaskWithSessionsDto

    /**
     * Persists a task straight away with every field the model chose. The returned id is a real task
     * already sitting in the user's Inbox. Used only by onboarding's first-task flow, which schedules
     * it in the same breath — the add-task sheet uses [previewTaskWithAi] instead.
     */
    @POST("v1/ai/task-create")
    suspend fun createTaskWithAi(
        @Body request: CreateTaskWithAiRequest,
    ): TaskWithSessionsDto

    /**
     * Asks Awan to propose a task from [request] without saving anything — no id, no Inbox row. The
     * caller creates the real task itself (via [createTask] or [createTaskWithSessions]) once the
     * user confirms, possibly after editing what Awan proposed.
     */
    @POST("v1/ai/task-create?persist=false")
    suspend fun previewTaskWithAi(
        @Body request: CreateTaskWithAiRequest,
    ): AiTaskPreviewResponse

    @GET("v1/tasks/date/{date}")
    suspend fun getTasksByDate(
        @Path("date") date: String,
    ): List<TaskWithSessionsDto>

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

