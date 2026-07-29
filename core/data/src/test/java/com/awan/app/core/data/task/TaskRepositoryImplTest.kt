package com.awan.app.core.data.task

import com.awan.app.core.common.result.Result
import com.awan.app.core.data.task.remote.TaskRemoteDataSource
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.TaskDraft
import com.awan.app.core.network.dto.category.CategoryDto
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.task.CreateTaskRequest
import com.awan.app.core.network.dto.task.CreateTaskWithAiRequest
import com.awan.app.core.network.dto.task.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.task.ScheduleTaskRequest
import com.awan.app.core.network.dto.task.ScheduledSessionResponse
import com.awan.app.core.network.dto.task.TaskInfoResponse
import com.awan.app.core.network.dto.task.TaskScheduleResponse
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private open class FakeRemoteDataSource : TaskRemoteDataSource {
        var lastCreateRequest: CreateTaskRequest? = null
        var lastWithSessionsRequest: CreateTaskWithSessionsRequest? = null
        var lastAiRequest: CreateTaskWithAiRequest? = null
        var deletedTaskId: String? = null

        override suspend fun createTask(request: CreateTaskRequest): Result<TaskInfoResponse> {
            lastCreateRequest = request
            return Result.Success(
                TaskInfoResponse(
                    id = "t-1",
                    title = request.title,
                    estimatedDuration = request.estimatedDuration,
                    status = "SCHEDULED",
                    mandatory = request.mandatory,
                )
            )
        }

        override suspend fun createTaskWithSessions(
            request: CreateTaskWithSessionsRequest,
        ): Result<TaskWithSessionsDto> {
            lastWithSessionsRequest = request
            return Result.Success(
                TaskWithSessionsDto(
                    task = TaskInfoResponse(id = "t-2", title = request.task.title, status = "SCHEDULED"),
                    sessions = request.sessions.mapIndexed { index, session ->
                        SessionDto(
                            id = "s-$index",
                            start = session.start,
                            end = session.end,
                            status = "SCHEDULED",
                            zoneId = session.zoneId,
                        )
                    },
                )
            )
        }

        override suspend fun createTaskWithAi(request: CreateTaskWithAiRequest): Result<TaskWithSessionsDto> {
            lastAiRequest = request
            return Result.Success(
                TaskWithSessionsDto(
                    task = TaskInfoResponse(
                        id = "t-ai",
                        title = request.title,
                        description = request.description,
                        estimatedDuration = 90,
                        status = "SCHEDULED",
                        estimatedPoints = 8,
                        allowTaskSplitting = true,
                        category = CategoryDto(id = "cat-1", name = "Afternoon Work"),
                    ),
                )
            )
        }

        override suspend fun scheduleTask(request: ScheduleTaskRequest): Result<TaskScheduleResponse> =
            Result.Success(
                TaskScheduleResponse(
                    taskId = request.taskId,
                    scheduledSessions = listOf(
                        ScheduledSessionResponse(
                            sessionId = "s-ai",
                            zoneId = "zone-1",
                            start = "2026-07-25T09:00:00",
                            end = "2026-07-25T10:30:00",
                        ),
                    ),
                )
            )

        override suspend fun deleteTask(taskId: String): Result<Unit> {
            deletedTaskId = taskId
            return Result.Success(Unit)
        }
    }

    @Test
    fun `createTask maps the draft to a request and the response to a model`() = runTest(testDispatcher) {
        val remote = FakeRemoteDataSource()
        val repository = TaskRepositoryImpl(remote, testDispatcher)

        val result = repository.createTask(TaskDraft(title = "  Read docs  ", durationMinutes = 45))

        assertEquals("Read docs", remote.lastCreateRequest?.title)
        assertTrue(result is Result.Success)
        assertEquals("t-1", (result as Result.Success).data.id)
        assertEquals(45, result.data.estimatedDurationMinutes)
        assertTrue(result.data.mandatory)
    }

    @Test
    fun `createTaskWithSessions sends offset-free local date times`() = runTest(testDispatcher) {
        val remote = FakeRemoteDataSource()
        val repository = TaskRepositoryImpl(remote, testDispatcher)
        val start = LocalDateTime.of(2026, 7, 24, 18, 0)

        val result = repository.createTaskWithSessions(
            draft = TaskDraft(title = "Gym session", categoryId = "cat-1"),
            sessions = listOf(SessionDraft(start = start, end = start.plusMinutes(60), zoneId = "zone-1")),
        )

        val sent = remote.lastWithSessionsRequest?.sessions?.single()
        assertEquals("2026-07-24T18:00:00", sent?.start)
        assertEquals("2026-07-24T19:00:00", sent?.end)
        assertEquals("zone-1", sent?.zoneId)
        assertEquals("cat-1", remote.lastWithSessionsRequest?.task?.categoryId)

        assertTrue(result is Result.Success)
        val session = (result as Result.Success).data.sessions.single()
        assertEquals(start, session.start)
        assertEquals("zone-1", session.zoneId)
    }

    @Test
    fun `createTaskWithAi carries the model's own fields through the mapper`() = runTest(testDispatcher) {
        val remote = FakeRemoteDataSource()
        val repository = TaskRepositoryImpl(remote, testDispatcher)

        val result = repository.createTaskWithAi("Build login page", "with email and password")

        assertEquals("Build login page", remote.lastAiRequest?.title)
        assertTrue(result is Result.Success)
        val task = (result as Result.Success).data
        assertEquals(90, task.estimatedDurationMinutes)
        assertEquals(8, task.estimatedPoints)
        assertTrue(task.allowTaskSplitting)
        assertEquals("Afternoon Work", task.category?.name)
    }

    @Test
    fun `an empty schedule reports a reason rather than passing for scheduled`() = runTest(testDispatcher) {
        val remote = object : FakeRemoteDataSource() {
            override suspend fun scheduleTask(request: ScheduleTaskRequest) =
                Result.Success(TaskScheduleResponse(taskId = request.taskId))
        }
        val repository = TaskRepositoryImpl(remote, testDispatcher)

        val result = repository.scheduleTask("t-ai")

        assertTrue(result is Result.Success)
        val schedule = (result as Result.Success).data
        assertTrue(schedule.sessions.isEmpty())
        assertNotNull(schedule.unscheduledReason)
        assertFalse(schedule.isScheduled)
    }

    @Test
    fun `scheduleTask maps placed sessions`() = runTest(testDispatcher) {
        val remote = FakeRemoteDataSource()
        val repository = TaskRepositoryImpl(remote, testDispatcher)

        val result = repository.scheduleTask("t-ai")

        assertTrue(result is Result.Success)
        val schedule = (result as Result.Success).data
        assertTrue(schedule.isScheduled)
        assertEquals("zone-1", schedule.sessions.single().zoneId)
        assertEquals(LocalDateTime.of(2026, 7, 25, 9, 0), schedule.sessions.single().start)
    }

    @Test
    fun `sessions with unparseable times are dropped rather than failing the create`() = runTest(testDispatcher) {
        val remote = object : TaskRemoteDataSource by FakeRemoteDataSource() {
            override suspend fun createTaskWithSessions(
                request: CreateTaskWithSessionsRequest,
            ): Result<TaskWithSessionsDto> = Result.Success(
                TaskWithSessionsDto(
                    task = TaskInfoResponse(id = "t-3", title = "Gym", status = "SCHEDULED"),
                    sessions = listOf(SessionDto(id = "s-0", start = "not-a-date", end = "also-not")),
                )
            )
        }
        val repository = TaskRepositoryImpl(remote, testDispatcher)
        val start = LocalDateTime.of(2026, 7, 24, 18, 0)

        val result = repository.createTaskWithSessions(
            draft = TaskDraft(title = "Gym"),
            sessions = listOf(SessionDraft(start = start, end = start.plusMinutes(30))),
        )

        assertTrue(result is Result.Success)
        assertEquals("t-3", (result as Result.Success).data.task.id)
        assertTrue(result.data.sessions.isEmpty())
    }

    @Test
    fun `an unknown status maps to UNKNOWN instead of throwing`() = runTest(testDispatcher) {
        val remote = object : TaskRemoteDataSource by FakeRemoteDataSource() {
            override suspend fun createTask(request: CreateTaskRequest): Result<TaskInfoResponse> =
                Result.Success(TaskInfoResponse(id = "t-4", title = "Gym", status = "TELEPORTED"))
        }
        val repository = TaskRepositoryImpl(remote, testDispatcher)

        val result = repository.createTask(TaskDraft(title = "Gym"))

        assertTrue(result is Result.Success)
        assertEquals(com.awan.app.core.model.TaskStatus.UNKNOWN, (result as Result.Success).data.status)
        assertNull(result.data.goalId)
    }
}
