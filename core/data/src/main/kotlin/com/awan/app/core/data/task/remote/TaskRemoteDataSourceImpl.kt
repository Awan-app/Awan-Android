package com.awan.app.core.data.task.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.TaskApiService
import com.awan.app.core.network.dto.task.CreateTaskRequest
import com.awan.app.core.network.dto.task.CreateTaskWithAiRequest
import com.awan.app.core.network.dto.task.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.task.ScheduleTaskRequest
import com.awan.app.core.network.dto.task.TaskInfoResponse
import com.awan.app.core.network.dto.task.TaskScheduleResponse
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject

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

    override suspend fun createTaskWithAi(request: CreateTaskWithAiRequest): Result<TaskWithSessionsDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.createTaskWithAi(request)
        }

    override suspend fun scheduleTask(request: ScheduleTaskRequest): Result<TaskScheduleResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.scheduleTask(request)
        }

    override suspend fun deleteTask(taskId: String): Result<Unit> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.deleteTask(taskId)
        }
}
