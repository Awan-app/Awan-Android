package com.awan.app.core.data.task.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.TaskApiService
import com.awan.app.core.network.dto.CreateTaskRequest
import com.awan.app.core.network.dto.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.TaskInfoResponse
import com.awan.app.core.network.dto.TaskWithSessionsResponse
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

            override suspend fun createTaskWithSessions(
                request: CreateTaskWithSessionsRequest,
            ): TaskWithSessionsResponse = error("not used")
        }
        val dataSource = TaskRemoteDataSourceImpl(fakeApiService, json, testDispatcher)
        val result = dataSource.createTask(CreateTaskRequest(title = "Study Kotlin"))

        assertTrue(result is Result.Success)
        assertEquals("task-123", (result as Result.Success).data.id)
        assertEquals("Study Kotlin", result.data.title)
    }

    @Test
    fun `createTaskWithSessions returns Success when API call succeeds`() = runTest(testDispatcher) {
        val fakeApiService = object : TaskApiService {
            override suspend fun createTask(request: CreateTaskRequest): TaskInfoResponse = error("not used")

            override suspend fun createTaskWithSessions(
                request: CreateTaskWithSessionsRequest,
            ): TaskWithSessionsResponse = TaskWithSessionsResponse(
                task = TaskInfoResponse(id = "task-456", title = request.task.title, status = "SCHEDULED"),
            )
        }
        val dataSource = TaskRemoteDataSourceImpl(fakeApiService, json, testDispatcher)
        val result = dataSource.createTaskWithSessions(
            CreateTaskWithSessionsRequest(task = CreateTaskRequest(title = "Gym session")),
        )

        assertTrue(result is Result.Success)
        assertEquals("task-456", (result as Result.Success).data.task.id)
    }
}
