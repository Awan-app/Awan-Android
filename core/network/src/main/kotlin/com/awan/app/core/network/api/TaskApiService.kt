package com.awan.app.core.network.api

import com.awan.app.core.network.dto.task.AiTextToTasksRequest
import com.awan.app.core.network.dto.task.BulkCreateTasksWithSessionsRequest
import com.awan.app.core.network.dto.task.CreateTaskRequest
import com.awan.app.core.network.dto.task.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.task.ScheduleTaskRequest
import com.awan.app.core.network.dto.task.TaskInfoResponse
import com.awan.app.core.network.dto.task.TaskProposalResponse
import com.awan.app.core.network.dto.task.TaskScheduleResponse
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import com.awan.app.core.network.dto.task.TasksWithSessionsResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
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

    @POST("v1/tasks/with-sessions/bulk")
    suspend fun createTasksWithSessions(
        @Body request: BulkCreateTasksWithSessionsRequest,
    ): TasksWithSessionsResponse


    /**
     * Asks Awan to turn a free-form note into one or more task proposals. Nothing is persisted —
     * every proposal carries a ready-to-POST [com.awan.app.core.network.dto.task.ProposedTaskDto.draft].
     */
    @POST("v1/ai/task-create")
    suspend fun proposeTasksFromText(
        @Body request: AiTextToTasksRequest,
    ): TaskProposalResponse

    /**
     * Same proposal contract as [proposeTasksFromText], sourced from a photo instead of typed text.
     * [note] is optional extra context ("finish these by Friday").
     */
    @Multipart
    @POST("v1/ai/image-to-tasks")
    suspend fun proposeTasksFromImage(
        @Part image: MultipartBody.Part,
        @Part("note") note: RequestBody?,
    ): TaskProposalResponse

    @GET("v1/tasks/{taskId}")
    suspend fun getTask(
        @Path("taskId") taskId: String,
    ): TaskInfoResponse

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
