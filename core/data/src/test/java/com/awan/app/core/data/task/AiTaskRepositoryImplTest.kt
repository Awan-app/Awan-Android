package com.awan.app.core.data.task

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.task.remote.TaskRemoteDataSource
import com.awan.app.core.network.dto.AiTaskPreviewResponse
import com.awan.app.core.network.dto.CreateTaskRequest
import com.awan.app.core.network.dto.CreateTaskWithAiRequest
import com.awan.app.core.network.dto.CreateTaskWithSessionsRequest
import com.awan.app.core.network.dto.ScheduleTaskRequest
import com.awan.app.core.network.dto.ScheduledSessionResponse
import com.awan.app.core.network.dto.TaskInfoResponse
import com.awan.app.core.network.dto.TaskScheduleResponse
import com.awan.app.core.network.dto.TaskWithSessionsDto
import com.awan.app.core.network.dto.UnscheduledTaskResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiTaskRepositoryImplTest {

    private var createResult: Result<TaskWithSessionsDto> =
        Result.Success(TaskWithSessionsDto(task = TaskInfoResponse(id = "task-1", title = "Read Clean Code")))
    private var scheduleResult: Result<TaskScheduleResponse> = Result.Success(TaskScheduleResponse())
    private var scheduleRequest: ScheduleTaskRequest? = null
    private var scheduleCalls = 0

    private val remoteDataSource = object : TaskRemoteDataSource {
        override suspend fun createTask(request: CreateTaskRequest) = error("not used")

        override suspend fun createTaskWithSessions(
            request: CreateTaskWithSessionsRequest,
        ): Result<TaskWithSessionsDto> = error("not used")

        override suspend fun createTaskWithAi(request: CreateTaskWithAiRequest) = createResult

        override suspend fun previewTaskWithAi(request: CreateTaskWithAiRequest): Result<AiTaskPreviewResponse> =
            error("not used")

        override suspend fun scheduleTask(request: ScheduleTaskRequest): Result<TaskScheduleResponse> {
            scheduleRequest = request
            scheduleCalls++
            return scheduleResult
        }

        override suspend fun deleteTask(taskId: String): Result<Unit> = error("not used")
    }

    private val repository = AiTaskRepositoryImpl(remoteDataSource, UnconfinedTestDispatcher())

    private fun scheduled(start: String, end: String) = Result.Success(
        TaskScheduleResponse(
            taskId = "task-1",
            scheduledSessions = listOf(
                ScheduledSessionResponse(
                    sessionId = "session-1",
                    taskId = "task-1",
                    zoneId = "zone-1",
                    start = start,
                    end = end,
                ),
            ),
        ),
    )

    @Test
    fun `a scheduled session becomes the task's real start and duration`() = runTest {
        scheduleResult = scheduled("2026-07-26T09:30:00", "2026-07-26T10:15:00")

        val result = repository.createAndScheduleTask("Read Clean Code")

        val task = (result as Result.Success).data!!
        assertEquals("task-1", task.id)
        assertEquals("Read Clean Code", task.title)
        assertEquals("zone-1", task.zoneId)
        assertEquals(9 * 60 + 30, task.startMinutes)
        assertEquals(45, task.durationMinutes)
        assertEquals("task-1", scheduleRequest?.taskId)
    }

    @Test
    fun `a session spanning midnight keeps its real duration`() = runTest {
        scheduleResult = scheduled("2026-07-26T23:30:00", "2026-07-27T00:15:00")

        val task = (repository.createAndScheduleTask("Late one") as Result.Success).data!!

        assertEquals(23 * 60 + 30, task.startMinutes)
        assertEquals(45, task.durationMinutes)
    }

    @Test
    fun `no session means created but unscheduled, not an error`() = runTest {
        scheduleResult = Result.Success(
            TaskScheduleResponse(
                taskId = "task-1",
                scheduledSessions = emptyList(),
                unscheduledTasks = listOf(UnscheduledTaskResponse(taskId = "task-1", reason = "NO_SLOT")),
            ),
        )

        val result = repository.createAndScheduleTask("Read Clean Code")

        assertTrue(result is Result.Success)
        assertNull((result as Result.Success).data)
    }

    @Test
    fun `a failed create never reaches the scheduler`() = runTest {
        createResult = Result.Error(AppError.Network)

        val result = repository.createAndScheduleTask("Read Clean Code")

        assertTrue(result is Result.Error)
        assertEquals(0, scheduleCalls)
    }
}
