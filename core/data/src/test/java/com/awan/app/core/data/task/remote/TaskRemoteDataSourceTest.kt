package com.awan.app.core.data.task.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.TaskApiService
import com.awan.app.core.network.dto.task.AiTextToTasksRequest
import com.awan.app.core.network.dto.task.BulkCreateTasksWithSessionsRequest
import com.awan.app.core.network.dto.task.CreateTaskRequest
import com.awan.app.core.network.dto.task.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.task.ProposedTaskDto
import com.awan.app.core.network.dto.task.ScheduleTaskRequest
import com.awan.app.core.network.dto.task.ScheduledSessionResponse
import com.awan.app.core.network.dto.task.TaskInfoResponse
import com.awan.app.core.network.dto.task.TaskProposalResponse
import com.awan.app.core.network.dto.task.TaskScheduleResponse
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import com.awan.app.core.network.dto.task.TasksWithSessionsResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every endpoint fails loudly unless the test under it opts in, so a stray call can't pass silently. */
private open class FakeTaskApiService : TaskApiService {
    override suspend fun createTask(request: CreateTaskRequest): TaskInfoResponse = error("not used")

    override suspend fun createTaskWithSessions(
        request: CreateTaskWithSessionsRequest,
    ): TaskWithSessionsDto = error("not used")

    override suspend fun createTasksWithSessions(
        request: BulkCreateTasksWithSessionsRequest,
    ): TasksWithSessionsResponse = error("not used")

    override suspend fun proposeTasksFromText(request: AiTextToTasksRequest): TaskProposalResponse =
        error("not used")

    override suspend fun proposeTasksFromImage(
        image: MultipartBody.Part,
        note: RequestBody?,
    ): TaskProposalResponse = error("not used")

    override suspend fun getTasksByDate(date: String): List<TaskWithSessionsDto> = error("not used")

    override suspend fun scheduleTask(request: ScheduleTaskRequest): TaskScheduleResponse =
        error("not used")

    override suspend fun deleteTask(taskId: String, cascade: Boolean): Unit = error("not used")
}

private fun dataSource(api: TaskApiService, json: Json, dispatcher: kotlinx.coroutines.CoroutineDispatcher) =
    TaskRemoteDataSourceImpl(api, json, dispatcher)

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRemoteDataSourceTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }

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
        val result = dataSource(api, json, testDispatcher).createTask(CreateTaskRequest(title = "Study Kotlin"))

        assertTrue(result is Result.Success)
        assertEquals("task-123", (result as Result.Success<TaskInfoResponse>).data.id)
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
        val result = dataSource(api, json, testDispatcher).createTaskWithSessions(
            CreateTaskWithSessionsRequest(task = CreateTaskRequest(title = "Gym session")),
        )

        assertTrue(result is Result.Success)
        assertEquals("task-456", (result as Result.Success<TaskWithSessionsDto>).data.task.id)
    }

    @Test
    fun `createTasksWithSessions returns Success when API call succeeds`() = runTest(testDispatcher) {
        val api = object : FakeTaskApiService() {
            override suspend fun createTasksWithSessions(
                request: BulkCreateTasksWithSessionsRequest,
            ) = TasksWithSessionsResponse(
                tasks = request.tasks.mapIndexed { index, task ->
                    TaskWithSessionsDto(
                        task = TaskInfoResponse(id = "task-$index", title = task.task.title, status = "SCHEDULED"),
                    )
                },
            )
        }
        val result = dataSource(api, json, testDispatcher).createTasksWithSessions(
            BulkCreateTasksWithSessionsRequest(
                tasks = listOf(
                    CreateTaskWithSessionsRequest(task = CreateTaskRequest(title = "Build login page")),
                    CreateTaskWithSessionsRequest(task = CreateTaskRequest(title = "Set up DB schema")),
                ),
            ),
        )

        assertTrue(result is Result.Success)
        assertEquals(2, (result as Result.Success).data.tasks.size)
        assertEquals("Build login page", result.data.tasks[0].task.title)
    }

    @Test
    fun `proposeTasksFromText returns Success when API call succeeds`() = runTest(testDispatcher) {
        val api = object : FakeTaskApiService() {
            override suspend fun proposeTasksFromText(request: AiTextToTasksRequest) = TaskProposalResponse(
                tasks = listOf(
                    ProposedTaskDto(
                        draft = CreateTaskWithSessionsRequest(
                            task = CreateTaskRequest(title = "Build login page", estimatedDuration = 60),
                        ),
                    ),
                ),
            )
        }
        val result = dataSource(api, json, testDispatcher).proposeTasksFromText(AiTextToTasksRequest(text = "Build login page"))

        assertTrue(result is Result.Success)
        assertEquals(1, (result as Result.Success).data.tasks.size)
        assertEquals("Build login page", result.data.tasks.single().draft.task.title)
    }

    @Test
    fun `proposeTasksFromImage returns Success when API call succeeds`() = runTest(testDispatcher) {
        val api = object : FakeTaskApiService() {
            override suspend fun proposeTasksFromImage(image: MultipartBody.Part, note: RequestBody?) =
                TaskProposalResponse(
                    sourceSummary = "TASK 1: Buy groceries",
                    tasks = listOf(
                        ProposedTaskDto(
                            draft = CreateTaskWithSessionsRequest(
                                task = CreateTaskRequest(title = "Buy groceries"),
                            ),
                        ),
                    ),
                )
        }
        val result = dataSource(api, json, testDispatcher).proposeTasksFromImage(
            image = ByteArray(4) { it.toByte() },
            mimeType = "image/jpeg",
            note = "Focus on the top item",
        )

        assertTrue(result is Result.Success)
        assertEquals("TASK 1: Buy groceries", (result as Result.Success).data.sourceSummary)
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
        val result = dataSource(api, json, testDispatcher).scheduleTask(ScheduleTaskRequest(taskId = "task-ai"))

        assertTrue(result is Result.Success)
        assertEquals("s-1", (result as Result.Success<TaskScheduleResponse>).data.scheduledSessions?.single()?.sessionId)
    }

    @Test
    fun `deleteTask returns Success when API call succeeds`() = runTest(testDispatcher) {
        var deleted: String? = null
        val api = object : FakeTaskApiService() {
            override suspend fun deleteTask(taskId: String, cascade: Boolean) {
                deleted = taskId
            }
        }
        val result = dataSource(api, json, testDispatcher).deleteTask("task-ai")

        assertTrue(result is Result.Success)
        assertEquals("task-ai", deleted)
    }
}
