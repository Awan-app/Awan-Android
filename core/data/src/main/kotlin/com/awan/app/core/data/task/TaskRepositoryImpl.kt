package com.awan.app.core.data.task

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.task.remote.TaskRemoteDataSource
import com.awan.app.core.network.dto.CreateTaskRequest
import com.awan.app.core.network.dto.TaskInfoResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val remoteDataSource: TaskRemoteDataSource,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : TaskRepository {

    override suspend fun createTask(
        title: String,
        description: String?,
        estimatedDurationMinutes: Int?,
        mandatory: Boolean?,
        estimatedPoints: Int?,
        allowTaskSplitting: Boolean?,
        goalId: String?,
    ): Result<TaskInfoResponse> = withContext(ioDispatcher) {
        val request = CreateTaskRequest(
            title = title,
            description = description,
            estimatedDuration = estimatedDurationMinutes,
            mandatory = mandatory,
            estimatedPoints = estimatedPoints,
            allowTaskSplitting = allowTaskSplitting,
            goalId = goalId,
        )
        remoteDataSource.createTask(request)
    }
}
