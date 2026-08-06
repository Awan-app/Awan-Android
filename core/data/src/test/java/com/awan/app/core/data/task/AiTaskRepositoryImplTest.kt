package com.awan.app.core.data.task

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.task.remote.TaskRemoteDataSource
import com.awan.app.core.network.dto.task.AiTextToTasksRequest
import com.awan.app.core.network.dto.task.BulkCreateTasksWithSessionsRequest
import com.awan.app.core.network.dto.task.CreateTaskRequest
import com.awan.app.core.network.dto.task.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.task.ProposedTaskDto
import com.awan.app.core.network.dto.task.ScheduleTaskRequest
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiTaskRepositoryImplTest {

    private var proposeResult: Result<TaskProposalResponse> = Result.Success(
        TaskProposalResponse(
            tasks = listOf(
                ProposedTaskDto(draft = CreateTaskWithSessionsRequest(task = CreateTaskRequest(title = "Read Clean Code"))),
            ),
        ),
    )
    private var createResult: Result<TaskWithSessionsDto>? = null
    private var createRequest: CreateTaskWithSessionsRequest? = null
    private var createCalls = 0

    private val remoteDataSource = object : TaskRemoteDataSource {
        override suspend fun createTask(request: CreateTaskRequest) = error("not used")

        override suspend fun createTaskWithSessions(
            request: CreateTaskWithSessionsRequest,
        ): Result<TaskWithSessionsDto> {
            createRequest = request
            createCalls++
            return createResult ?: Result.Success(
                TaskWithSessionsDto(
                    task = TaskInfoResponse(id = "task-1", title = request.task.title),
                    sessions = request.sessions.mapIndexed { index, session ->
                        SessionDto(id = "session-$index", start = session.start, end = session.end, zoneId = session.zoneId)
                    },
                ),
            )
        }

        override suspend fun createTasksWithSessions(
            request: BulkCreateTasksWithSessionsRequest,
        ): Result<TasksWithSessionsResponse> = error("not used")

        override suspend fun proposeTasksFromText(request: AiTextToTasksRequest): Result<TaskProposalResponse> =
            proposeResult

        override suspend fun proposeTasksFromImage(
            image: ByteArray,
            mimeType: String,
            note: String?,
        ): Result<TaskProposalResponse> = error("not used")

        override suspend fun scheduleTask(request: ScheduleTaskRequest): Result<TaskScheduleResponse> =
            error("not used")

        override suspend fun deleteTask(taskId: String): Result<Unit> = error("not used")
    }

    private val repository = AiTaskRepositoryImpl(remoteDataSource, UnconfinedTestDispatcher())

    private fun proposalWithSession(title: String, start: String, end: String) = Result.Success(
        TaskProposalResponse(
            tasks = listOf(
                ProposedTaskDto(
                    draft = CreateTaskWithSessionsRequest(task = CreateTaskRequest(title = title)),
                    aiProposedSessions = listOf(SessionDraftDto(start = start, end = end, zoneId = "zone-1")),
                ),
            ),
        ),
    )

    @Test
    fun `a proposal with a session becomes the task's real start and duration`() = runTest {
        proposeResult = proposalWithSession("Read Clean Code", "2026-07-26T09:30:00", "2026-07-26T10:15:00")

        val result = repository.createAndScheduleTask("Read Clean Code")

        val task = (result as Result.Success).data!!
        assertEquals("task-1", task.id)
        assertEquals("Read Clean Code", task.title)
        assertEquals("zone-1", task.zoneId)
        assertEquals(9 * 60 + 30, task.startMinutes)
        assertEquals(45, task.durationMinutes)
        assertEquals(1, createRequest?.sessions?.size)
    }

    @Test
    fun `a session spanning midnight keeps its real duration`() = runTest {
        proposeResult = proposalWithSession("Late one", "2026-07-26T23:30:00", "2026-07-27T00:15:00")

        val task = (repository.createAndScheduleTask("Late one") as Result.Success).data!!

        assertEquals(23 * 60 + 30, task.startMinutes)
        assertEquals(45, task.durationMinutes)
    }

    @Test
    fun `a proposal with no sessions creates the task but reports it as unscheduled`() = runTest {
        proposeResult = Result.Success(
            TaskProposalResponse(
                tasks = listOf(ProposedTaskDto(draft = CreateTaskWithSessionsRequest(task = CreateTaskRequest(title = "Read Clean Code")))),
            ),
        )

        val result = repository.createAndScheduleTask("Read Clean Code")

        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).data)
        assertEquals(1, createCalls)
    }

    @Test
    fun `an empty proposal list means nothing is created`() = runTest {
        proposeResult = Result.Success(TaskProposalResponse(tasks = emptyList()))

        val result = repository.createAndScheduleTask("Read Clean Code")

        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).data)
        assertEquals(0, createCalls)
    }

    @Test
    fun `a failed proposal never reaches create`() = runTest {
        proposeResult = Result.Error(AppError.Network)

        val result = repository.createAndScheduleTask("Read Clean Code")

        assertTrue(result is Result.Error)
        assertEquals(0, createCalls)
    }

    @Test
    fun `a failed create surfaces the error`() = runTest {
        createResult = Result.Error(AppError.Network)

        val result = repository.createAndScheduleTask("Read Clean Code")

        assertTrue(result is Result.Error)
    }
}
