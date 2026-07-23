package com.awan.app.core.data.task

import com.awan.app.core.common.result.Result
import com.awan.app.core.data.task.remote.TaskRemoteDataSource
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.TaskDraft
import com.awan.app.core.network.dto.CreateTaskRequest
import com.awan.app.core.network.dto.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.SessionDto
import com.awan.app.core.network.dto.TaskInfoResponse
import com.awan.app.core.network.dto.TaskWithSessionsResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeRemoteDataSource : TaskRemoteDataSource {
        var lastCreateRequest: CreateTaskRequest? = null
        var lastWithSessionsRequest: CreateTaskWithSessionsRequest? = null

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
        ): Result<TaskWithSessionsResponse> {
            lastWithSessionsRequest = request
            return Result.Success(
                TaskWithSessionsResponse(
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
            draft = TaskDraft(title = "Gym session", zoneId = "zone-1"),
            sessions = listOf(SessionDraft(start = start, end = start.plusMinutes(60), zoneId = "zone-1")),
        )

        val sent = remote.lastWithSessionsRequest?.sessions?.single()
        assertEquals("2026-07-24T18:00:00", sent?.start)
        assertEquals("2026-07-24T19:00:00", sent?.end)
        assertEquals("zone-1", sent?.zoneId)

        assertTrue(result is Result.Success)
        val session = (result as Result.Success).data.sessions.single()
        assertEquals(start, session.start)
        assertEquals("zone-1", session.zoneId)
    }

    @Test
    fun `sessions with unparseable times are dropped rather than failing the create`() = runTest(testDispatcher) {
        val remote = object : TaskRemoteDataSource by FakeRemoteDataSource() {
            override suspend fun createTaskWithSessions(
                request: CreateTaskWithSessionsRequest,
            ): Result<TaskWithSessionsResponse> = Result.Success(
                TaskWithSessionsResponse(
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
