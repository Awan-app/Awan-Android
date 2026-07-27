package com.awan.app.core.data.task.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.TaskApiService
import com.awan.app.core.network.dto.CreateAiTaskRequest
import com.awan.app.core.network.dto.CreateTaskRequest
import com.awan.app.core.network.dto.ScheduleTaskRequest
import com.awan.app.core.network.dto.TaskInfoResponse
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

            override suspend fun getTasksByDate(date: String): List<com.awan.app.core.network.dto.TaskWithSessionsDto> {
                return emptyList()
            }

            override suspend fun createTaskWithSessions(request: com.awan.app.core.network.dto.CreateTaskWithSessionsRequest): com.awan.app.core.network.dto.TaskWithSessionsDto {
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
