package com.awan.app.core.data.task

import com.awan.app.core.common.result.Result
import com.awan.app.core.data.task.remote.TaskRemoteDataSource
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.TaskDraft
import com.awan.app.core.model.TaskWithSessionsDraft
import com.awan.app.core.network.dto.task.AiTextToTasksRequest
import com.awan.app.core.network.dto.task.BulkCreateTasksWithSessionsRequest
import com.awan.app.core.network.dto.task.CreateTaskRequest
import com.awan.app.core.network.dto.task.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.task.ProposedTaskDto
import com.awan.app.core.network.dto.task.ScheduleTaskRequest
import com.awan.app.core.network.dto.task.ScheduledSessionResponse
import com.awan.app.core.network.dto.task.SessionDraftDto
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.task.TaskInfoResponse
import com.awan.app.core.network.dto.task.TaskProposalResponse
import com.awan.app.core.network.dto.task.TaskScheduleResponse
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import com.awan.app.core.network.dto.task.TasksWithSessionsResponse
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
        var lastBulkRequest: BulkCreateTasksWithSessionsRequest? = null
        var lastProposeTextRequest: String? = null
        var lastProposeImageNote: String? = null
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

        override suspend fun createTasksWithSessions(
            request: BulkCreateTasksWithSessionsRequest,
        ): Result<TasksWithSessionsResponse> {
            lastBulkRequest = request
            return Result.Success(
                TasksWithSessionsResponse(
                    tasks = request.tasks.mapIndexed { index, task ->
                        TaskWithSessionsDto(
                            task = TaskInfoResponse(id = "t-bulk-$index", title = task.task.title, status = "SCHEDULED"),
                        )
                    },
                )
            )
        }

        override suspend fun proposeTasksFromText(request: AiTextToTasksRequest): Result<TaskProposalResponse> {
            lastProposeTextRequest = request.text
            return Result.Success(
                TaskProposalResponse(
                    tasks = listOf(
                        ProposedTaskDto(
                            draft = CreateTaskWithSessionsRequest(
                                task = CreateTaskRequest(
                                    title = request.text,
                                    estimatedDuration = 90,
                                    estimatedPoints = 8,
                                    allowTaskSplitting = true,
                                    categoryId = "cat-1",
                                ),
                            ),
                            aiProposedSessions = listOf(
                                SessionDraftDto(start = "2026-07-25T09:00:00", end = "2026-07-25T10:30:00"),
                            ),
                            reason = "Morning zone has room.",
                        ),
                    ),
                )
            )
        }

        override suspend fun proposeTasksFromImage(
            image: ByteArray,
            mimeType: String,
            note: String?,
        ): Result<TaskProposalResponse> {
            lastProposeImageNote = note
            return Result.Success(
                TaskProposalResponse(
                    sourceSummary = "TASK 1: Buy groceries",
                    tasks = listOf(
                        ProposedTaskDto(draft = CreateTaskWithSessionsRequest(task = CreateTaskRequest(title = "Buy groceries"))),
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

        val result = repository.createTask(TaskDraft(title = "  Read docs  ", durationMinutes = 45, mandatory = true))

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
    fun `createTasksWithSessions sends every draft in one bulk request`() = runTest(testDispatcher) {
        val remote = FakeRemoteDataSource()
        val repository = TaskRepositoryImpl(remote, testDispatcher)
        val start = LocalDateTime.of(2026, 7, 24, 18, 0)

        val result = repository.createTasksWithSessions(
            listOf(
                TaskWithSessionsDraft(task = TaskDraft(title = "Build login page")),
                TaskWithSessionsDraft(
                    task = TaskDraft(title = "Gym session"),
                    sessions = listOf(SessionDraft(start = start, end = start.plusMinutes(60))),
                ),
            ),
        )

        assertEquals(2, remote.lastBulkRequest?.tasks?.size)
        assertTrue(result is Result.Success)
        val tasks = (result as Result.Success).data
        assertEquals(2, tasks.size)
        assertEquals("Build login page", tasks[0].title)
    }

    @Test
    fun `proposeTasksFromText maps the model's fields and merges suggested sessions`() = runTest(testDispatcher) {
        val remote = FakeRemoteDataSource()
        val repository = TaskRepositoryImpl(remote, testDispatcher)

        val result = repository.proposeTasksFromText("Build login page")

        assertEquals("Build login page", remote.lastProposeTextRequest)
        assertTrue(result is Result.Success)
        val proposal = (result as Result.Success).data.tasks.single()
        assertEquals(90, proposal.draft.durationMinutes)
        assertEquals(8, proposal.draft.estimatedPoints)
        assertTrue(proposal.draft.allowTaskSplitting)
        assertEquals("cat-1", proposal.draft.categoryId)
        assertEquals("Morning zone has room.", proposal.reason)
        val session = proposal.sessions.single()
        assertTrue(session.isAiSuggested)
        assertEquals(LocalDateTime.of(2026, 7, 25, 9, 0), session.start)
    }

    @Test
    fun `both session channels merge into one list, tagged by who proposed them`() {
        val proposal = ProposedTaskDto(
            draft = CreateTaskWithSessionsRequest(
                task = CreateTaskRequest(title = "Gym"),
                sessions = listOf(SessionDraftDto(start = "2026-07-24T18:00:00", end = "2026-07-24T19:00:00")),
            ),
            aiProposedSessions = listOf(
                SessionDraftDto(start = "2026-07-25T09:00:00", end = "2026-07-25T10:00:00"),
            ),
        ).toModel()

        assertEquals(2, proposal.sessions.size)
        val stated = proposal.sessions[0]
        assertFalse(stated.isAiSuggested)
        assertEquals(LocalDateTime.of(2026, 7, 24, 18, 0), stated.start)
        val suggested = proposal.sessions[1]
        assertTrue(suggested.isAiSuggested)
        assertEquals(LocalDateTime.of(2026, 7, 25, 9, 0), suggested.start)
    }

    @Test
    fun `a suggestion echoing a stated time is dropped rather than shown twice`() {
        val proposal = ProposedTaskDto(
            draft = CreateTaskWithSessionsRequest(
                task = CreateTaskRequest(title = "Gym"),
                sessions = listOf(SessionDraftDto(start = "2026-07-24T18:00:00", end = "2026-07-24T19:00:00")),
            ),
            aiProposedSessions = listOf(
                SessionDraftDto(start = "2026-07-24T18:00:00", end = "2026-07-24T19:00:00"),
            ),
        ).toModel()

        val session = proposal.sessions.single()
        assertFalse(session.isAiSuggested)
        assertEquals(LocalDateTime.of(2026, 7, 24, 18, 0), session.start)
    }

    @Test
    fun `proposeTasksFromImage carries the source summary through`() = runTest(testDispatcher) {
        val remote = FakeRemoteDataSource()
        val repository = TaskRepositoryImpl(remote, testDispatcher)

        val result = repository.proposeTasksFromImage(ByteArray(1), "image/jpeg", "Focus on top item")

        assertEquals("Focus on top item", remote.lastProposeImageNote)
        assertTrue(result is Result.Success)
        assertEquals("TASK 1: Buy groceries", (result as Result.Success).data.sourceSummary)
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
                Result.Success(TaskInfoResponse(id = "t-4", title = "Gym", status = "GHOST"))
        }
        val repository = TaskRepositoryImpl(remote, testDispatcher)

        val result = repository.createTask(TaskDraft(title = "Gym"))

        assertTrue(result is Result.Success)
        assertEquals(com.awan.app.core.model.TaskStatus.UNKNOWN, (result as Result.Success).data.status)
        assertNull(result.data.goalId)
    }
}
