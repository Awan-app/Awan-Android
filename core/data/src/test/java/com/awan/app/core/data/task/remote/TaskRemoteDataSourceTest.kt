package com.awan.app.core.data.task.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.TaskApiService
import com.awan.app.core.network.dto.AiTaskPreviewResponse
import com.awan.app.core.network.dto.AiTaskPreviewTaskResponse
import com.awan.app.core.network.dto.CreateTaskRequest
import com.awan.app.core.network.dto.CreateTaskWithAiRequest
import com.awan.app.core.network.dto.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.ScheduleTaskRequest
import com.awan.app.core.network.dto.ScheduledSessionResponse
import com.awan.app.core.network.dto.TaskInfoResponse
import com.awan.app.core.network.dto.TaskScheduleResponse
import com.awan.app.core.network.dto.TaskWithSessionsDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every endpoint fails loudly unless the test under it opts in, so a stray call can't pass silently. */
private open class FakeTaskApiService : TaskApiService {
    override suspend fun createTask(request: CreateTaskRequest): TaskInfoResponse = error("not used")

    override suspend fun createTaskWithSessions(
        request: CreateTaskWithSessionsRequest,
    ): TaskWithSessionsDto = error("not used")

    override suspend fun createTaskWithAi(request: CreateTaskWithAiRequest): TaskWithSessionsDto =
        error("not used")

    override suspend fun previewTaskWithAi(request: CreateTaskWithAiRequest): AiTaskPreviewResponse =
        error("not used")

    override suspend fun scheduleTask(request: ScheduleTaskRequest): TaskScheduleResponse =
        error("not used")

    override suspend fun getTasksByDate(date: String): List<TaskWithSessionsDto> = error("not used")

    override suspend fun deleteTask(taskId: String, cascade: Boolean): Unit = error("not used")
}

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRemoteDataSourceTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }

    private fun dataSource(api: TaskApiService) = TaskRemoteDataSourceImpl(api, json, testDispatcher)

    @Test
    fun `createTask returns Success when API call succeeds`() = runTest(testDispatcher) {
        val api = object : FakeTaskApiService() {
            override suspend fun createTask(request: CreateTaskRequest) = TaskInfoResponse(
                id = "task-123",
                title = request.title,
                estimatedDuration = request.estimatedDuration,
                status = "SCHEDULED",
            )
        }
        val result = dataSource(api).createTask(CreateTaskRequest(title = "Study Kotlin"))

        assertTrue(result is Result.Success)
        assertEquals("task-123", (result as Result.Success).data.id)
        assertEquals("Study Kotlin", result.data.title)
    }

    @Test
    fun `createTaskWithSessions returns Success when API call succeeds`() = runTest(testDispatcher) {
        val api = object : FakeTaskApiService() {
            override suspend fun createTaskWithSessions(
                request: CreateTaskWithSessionsRequest,
            ) = TaskWithSessionsDto(
                task = TaskInfoResponse(id = "task-456", title = request.task.title, status = "SCHEDULED"),
            )
        }
        val result = dataSource(api).createTaskWithSessions(
            CreateTaskWithSessionsRequest(task = CreateTaskRequest(title = "Gym session")),
        )

        assertTrue(result is Result.Success)
        assertEquals("task-456", (result as Result.Success).data.task.id)
    }

    @Test
    fun `createTaskWithAi returns Success when API call succeeds`() = runTest(testDispatcher) {
        val api = object : FakeTaskApiService() {
            override suspend fun createTaskWithAi(request: CreateTaskWithAiRequest) = TaskWithSessionsDto(
                task = TaskInfoResponse(
                    id = "task-ai",
                    title = request.title,
                    estimatedDuration = 90,
                    status = "SCHEDULED",
                ),
            )
        }
        val result = dataSource(api).createTaskWithAi(CreateTaskWithAiRequest(title = "Build login page"))

        assertTrue(result is Result.Success)
        assertEquals("task-ai", (result as Result.Success).data.task.id)
        assertEquals(90, result.data.task.estimatedDuration)
    }

    @Test
    fun `previewTaskWithAi returns Success when API call succeeds`() = runTest(testDispatcher) {
        val api = object : FakeTaskApiService() {
            override suspend fun previewTaskWithAi(request: CreateTaskWithAiRequest) = AiTaskPreviewResponse(
                task = AiTaskPreviewTaskResponse(title = request.title, estimatedDuration = 90),
            )
        }
        val result = dataSource(api).previewTaskWithAi(CreateTaskWithAiRequest(title = "Build login page"))

        assertTrue(result is Result.Success)
        assertEquals(90, (result as Result.Success).data.task?.estimatedDuration)
    }

    @Test
    fun `scheduleTask returns Success when API call succeeds`() = runTest(testDispatcher) {
        val api = object : FakeTaskApiService() {
            override suspend fun scheduleTask(request: ScheduleTaskRequest) = TaskScheduleResponse(
                taskId = request.taskId,
                scheduledSessions = listOf(
                    ScheduledSessionResponse(
                        sessionId = "s-1",
                        start = "2026-07-25T09:00:00",
                        end = "2026-07-25T10:30:00",
                    ),
                ),
            )
        }
        val result = dataSource(api).scheduleTask(ScheduleTaskRequest(taskId = "task-ai"))

        assertTrue(result is Result.Success)
        assertEquals("s-1", (result as Result.Success).data.scheduledSessions?.single()?.sessionId)
    }

    @Test
    fun `deleteTask returns Success when API call succeeds`() = runTest(testDispatcher) {
        var deleted: String? = null
        val api = object : FakeTaskApiService() {
            override suspend fun deleteTask(taskId: String, cascade: Boolean) {
                deleted = taskId
            }
        }
        val result = dataSource(api).deleteTask("task-ai")

        assertTrue(result is Result.Success)
        assertEquals("task-ai", deleted)
    }
}
