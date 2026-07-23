package com.awan.app.core.data.task.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.TaskApiService
import com.awan.app.core.network.dto.CreateTaskRequest
import com.awan.app.core.network.dto.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.TaskInfoResponse
import com.awan.app.core.network.dto.TaskWithSessionsResponse
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
    ): Result<TaskWithSessionsResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            taskApiService.createTaskWithSessions(request)
        }
}
