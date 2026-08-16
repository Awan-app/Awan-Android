package com.awan.app.core.data.task.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.TaskApiService
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.task.AddTaskSessionsRequest
import com.awan.app.core.network.dto.task.AiTextToTasksRequest
import com.awan.app.core.network.dto.task.BulkCreateTasksWithSessionsRequest
import com.awan.app.core.network.dto.task.CreateTaskRequest
import com.awan.app.core.network.dto.task.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.task.ScheduleTaskRequest
import com.awan.app.core.network.dto.task.TaskCompletionResponse
import com.awan.app.core.network.dto.task.TaskDependencyRequest
import com.awan.app.core.network.dto.task.TaskInfoResponse
import com.awan.app.core.network.dto.task.TaskMoveRequest
import com.awan.app.core.network.dto.task.TaskProposalResponse
import com.awan.app.core.network.dto.task.TaskScheduleResponse
import com.awan.app.core.network.dto.task.TaskUpdateRequest
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import com.awan.app.core.network.dto.task.TasksWithSessionsResponse
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

private const val IMAGE_PART_NAME = "image"
/** [ImageRepositoryImpl][com.awan.app.core.data.image.ImageRepositoryImpl] always re-encodes as JPEG. */
private const val IMAGE_FILENAME = "image.jpg"

class TaskRemoteDataSourceImpl @Inject constructor(
    private val taskApiService: TaskApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : TaskRemoteDataSource {

    override suspend fun createTask(request: CreateTaskRequest): Result<TaskInfoResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.createTask(request)
        }

    override suspend fun createTaskWithSessions(
        request: CreateTaskWithSessionsRequest,
    ): Result<TaskWithSessionsDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.createTaskWithSessions(request)
        }

    override suspend fun updateTask(
        taskId: String,
        request: com.awan.app.core.network.dto.task.TaskUpdateRequest,
    ): Result<TaskInfoResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.updateTask(taskId, request)
        }

    override suspend fun createTasksWithSessions(
        request: BulkCreateTasksWithSessionsRequest,
    ): Result<TasksWithSessionsResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.createTasksWithSessions(request)
        }

    override suspend fun proposeTasksFromText(request: AiTextToTasksRequest): Result<TaskProposalResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.proposeTasksFromText(request)
        }

    override suspend fun proposeTasksFromImage(
        image: ByteArray,
        mimeType: String,
        note: String?,
    ): Result<TaskProposalResponse> = safeApiCall(dispatcher = ioDispatcher, json = json) {
        val imagePart = MultipartBody.Part.createFormData(
            name = IMAGE_PART_NAME,
            filename = IMAGE_FILENAME,
            body = image.toRequestBody(mimeType.toMediaType()),
        )
        val notePart = note?.toRequestBody("text/plain".toMediaType())
        taskApiService.proposeTasksFromImage(imagePart, notePart)
    }

    override suspend fun getTasksByRange(
        startDate: String,
        endDate: String,
    ): Result<Map<String, List<TaskWithSessionsDto>>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.getTasksByRange(startDate, endDate)
        }

    override suspend fun scheduleTask(request: ScheduleTaskRequest): Result<TaskScheduleResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.scheduleTask(request)
        }

    override suspend fun completeTask(taskId: String): Result<TaskCompletionResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.completeTask(taskId)
        }


    override suspend fun deleteTask(taskId: String, cascade: Boolean): Result<Unit> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.deleteTask(taskId, cascade)
        }

    override suspend fun getInboxTasks(): Result<List<TaskWithSessionsDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.getInboxTasks().tasks.map { TaskWithSessionsDto(task = it) }
        }

    // ── Task Details ─────────────────────────────────────────────────────────

    override suspend fun getTask(taskId: String): Result<TaskInfoResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.getTask(taskId)
        }

    override suspend fun updateTask(taskId: String, request: TaskUpdateRequest): Result<TaskInfoResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.updateTask(taskId, request)
        }

    override suspend fun moveTask(taskId: String, request: TaskMoveRequest): Result<TaskInfoResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.moveTask(taskId, request)
        }

    override suspend fun addDependency(taskId: String, request: TaskDependencyRequest): Result<Unit> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.addDependency(taskId, request)
        }

    override suspend fun removeDependency(taskId: String, dependsOnTaskId: String): Result<Unit> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.removeDependency(taskId, dependsOnTaskId)
        }

    override suspend fun getTaskDependencies(taskId: String): Result<List<TaskInfoResponse>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.getTaskDependencies(taskId)
        }

    override suspend fun getTaskDependents(taskId: String): Result<List<TaskInfoResponse>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.getTaskDependents(taskId)
        }

    override suspend fun getTaskSessions(taskId: String, status: String?): Result<List<SessionDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.getTaskSessions(taskId, status)
        }

    override suspend fun addTaskSessions(taskId: String, request: AddTaskSessionsRequest): Result<List<SessionDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.addTaskSessions(taskId, request)
        }
}
