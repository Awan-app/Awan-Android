package com.awan.app.core.data.task.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.TaskApiService
import com.awan.app.core.network.dto.task.CreateAiTaskRequest
import com.awan.app.core.network.dto.task.CreateTaskRequest
import com.awan.app.core.network.dto.task.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.task.ScheduleTaskRequest
import com.awan.app.core.network.dto.task.TaskInfoResponse
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRemoteDataSourceTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `createTask returns Success when API call succeeds`() = runTest(testDispatcher) {
        val fakeApiService = object : TaskApiService {
            override suspend fun createTask(request: CreateTaskRequest): TaskInfoResponse {
                return TaskInfoResponse(
                    id = "task-123",
                    title = request.title,
                    estimatedDuration = request.estimatedDuration,
                    status = "SCHEDULED",
                )
            }

            override suspend fun createTaskWithAi(request: CreateAiTaskRequest) = error("not used")

            override suspend fun scheduleTask(request: ScheduleTaskRequest) = error("not used")

            override suspend fun getTasksByDate(date: String): List<TaskWithSessionsDto> {
                return emptyList()
            }

            override suspend fun createTaskWithSessions(request: CreateTaskWithSessionsRequest): TaskWithSessionsDto {
                throw NotImplementedError()
            }
        }
        val dataSource = TaskRemoteDataSourceImpl(fakeApiService, json, testDispatcher)
        val result = dataSource.createTask(CreateTaskRequest(title = "Study Kotlin"))

        assertTrue(result is Result.Success)
        assertEquals("task-123", (result as Result.Success).data.id)
        assertEquals("Study Kotlin", result.data.title)
    }
}
