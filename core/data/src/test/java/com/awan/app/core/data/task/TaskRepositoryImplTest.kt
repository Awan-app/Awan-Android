package com.awan.app.core.data.task

import com.awan.app.core.common.result.Result
import com.awan.app.core.database.dao.CategoryDao
import com.awan.app.core.database.dao.SessionDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.model.CategoryEntity
import com.awan.app.core.database.model.SessionEntity
import com.awan.app.core.database.model.UpcomingSessionRow
import com.awan.app.core.database.model.TaskDependencyEntity
import com.awan.app.core.database.model.TaskEntity
import com.awan.app.core.data.task.remote.TaskRemoteDataSource
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

// ---------------------------------------------------------------------------
// Fakes — no Mockito
// ---------------------------------------------------------------------------

private class FakeTaskDao : TaskDao {
    val upsertedTasks = mutableListOf<TaskEntity>()
    val deletedTaskIds = mutableListOf<String>()

    override suspend fun upsertTask(task: TaskEntity) { upsertedTasks += task }
    override suspend fun upsertTasks(tasks: List<TaskEntity>) { upsertedTasks += tasks }
    override fun observeTasksByGoal(goalId: String): Flow<List<TaskEntity>> = flowOf(emptyList())
    override fun observeInboxTasks(): Flow<List<TaskEntity>> = flowOf(emptyList())
    override fun observeAllTasks(): Flow<List<TaskEntity>> = flowOf(emptyList())
    override suspend fun getAllTasks(): List<TaskEntity> = emptyList()
    override fun observeTask(taskId: String): Flow<TaskEntity?> = MutableStateFlow(null)
    override suspend fun getTask(taskId: String): TaskEntity? = null
    override suspend fun deleteTask(taskId: String) { deletedTaskIds += taskId }
    override suspend fun upsertDependency(dependency: TaskDependencyEntity) {}
    override suspend fun upsertDependencies(dependencies: List<TaskDependencyEntity>) {}
    override suspend fun deleteDependency(dependency: TaskDependencyEntity) {}
    override fun observeDependsOnIds(taskId: String): Flow<List<String>> = flowOf(emptyList())
    override fun observeDependentIds(taskId: String): Flow<List<String>> = flowOf(emptyList())
    override suspend fun deleteAllDependenciesForTask(taskId: String) {}
    override suspend fun replaceTasksForGoal(
        goalId: String,
        tasks: List<TaskEntity>,
        dependencies: List<TaskDependencyEntity>,
    ) {}
    override suspend fun deleteTasksByGoal(goalId: String) {}
    override suspend fun nullifyOrphanedGoalReferences() {}
}

private class FakeCategoryDao : CategoryDao {
    val upserted = mutableListOf<CategoryEntity>()
    override suspend fun upsertCategories(categories: List<CategoryEntity>) { upserted += categories }
    override suspend fun upsertCategory(category: CategoryEntity) { upserted += category }
    override fun observeAllCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
    override suspend fun getAllCategories(): List<CategoryEntity> = emptyList()
    override suspend fun getCategory(id: String): CategoryEntity? = null
    override suspend fun deleteCategory(id: String) {}
    override suspend fun deleteAllCategories() {}
    override suspend fun getMinExpiryTime(): Long? = null
}

private class FakeSessionDao : SessionDao {
    val upserted = mutableListOf<SessionEntity>()
    override suspend fun upsertSession(session: SessionEntity) { upserted += session }
    override suspend fun upsertSessions(sessions: List<SessionEntity>) { upserted += sessions }
    override fun observeSessionsForDate(date: String): Flow<List<SessionEntity>> = flowOf(emptyList())
    override fun observeSessionsForDateRange(startDate: String, endDate: String): Flow<List<SessionEntity>> = flowOf(emptyList())
    override suspend fun getSessionsForDate(date: String): List<SessionEntity> = emptyList()
    override suspend fun getSessionsForDateRange(startDate: String, endDate: String): List<SessionEntity> = emptyList()
    override fun observeUpcomingSessions(startDate: String, endDate: String): Flow<List<UpcomingSessionRow>> = flowOf(emptyList())
    override suspend fun getUpcomingSessions(startDate: String, endDate: String): List<UpcomingSessionRow> = emptyList()
    override suspend fun getSession(id: String): SessionEntity? = null
    override suspend fun deleteSessionsForDates(dates: List<String>) {}
    override suspend fun deleteSession(id: String) {}
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val onlineMonitor = object : NetworkConnectivityMonitor {
        override val isOnline: Flow<Boolean> = flowOf(true)
        override fun isCurrentlyOnline(): Boolean = true
    }

    private val offlineMonitor = object : NetworkConnectivityMonitor {
        override val isOnline: Flow<Boolean> = flowOf(false)
        override fun isCurrentlyOnline(): Boolean = false
    }

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

        override suspend fun getTasksByRange(
            startDate: String,
            endDate: String,
        ): Result<Map<String, List<TaskWithSessionsDto>>> {
            return Result.Success(emptyMap())
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

        override suspend fun getInboxTasks(): Result<List<TaskWithSessionsDto>> = Result.Success(emptyList())
    }

private class FakeGoalDao : com.awan.app.core.database.dao.GoalDao {
    override suspend fun upsertGoal(goal: com.awan.app.core.database.model.GoalEntity) {}
    override suspend fun upsertGoals(goals: List<com.awan.app.core.database.model.GoalEntity>) {}
    override fun observeAllGoals(): Flow<List<com.awan.app.core.database.model.GoalEntity>> = flowOf(emptyList())
    override suspend fun getAllGoals(): List<com.awan.app.core.database.model.GoalEntity> = emptyList()
    override fun observeGoalsByStatus(status: String): Flow<List<com.awan.app.core.database.model.GoalEntity>> = flowOf(emptyList())
    override fun observeGoal(goalId: String): Flow<com.awan.app.core.database.model.GoalEntity?> = MutableStateFlow(null)
    override suspend fun getGoal(goalId: String): com.awan.app.core.database.model.GoalEntity? = null
    override fun observeInboxGoal(): Flow<com.awan.app.core.database.model.GoalEntity?> = MutableStateFlow(null)
    override suspend fun deleteGoal(goalId: String) {}
    override suspend fun getActiveNonInboxGoalIds(): List<String> = emptyList()
    override suspend fun getMinExpiryTime(): Long? = null
}

    private fun buildRepository(
        remote: TaskRemoteDataSource = FakeRemoteDataSource(),
        taskDao: TaskDao = FakeTaskDao(),
        categoryDao: CategoryDao = FakeCategoryDao(),
        sessionDao: SessionDao = FakeSessionDao(),
        goalDao: com.awan.app.core.database.dao.GoalDao = FakeGoalDao(),
        monitor: NetworkConnectivityMonitor = onlineMonitor,
    ) = TaskRepositoryImpl(remote, taskDao, categoryDao, sessionDao, goalDao, monitor, testDispatcher)

    @Test
    fun `createTask maps the draft to a request and the response to a model`() = runTest(testDispatcher) {
        val remote = FakeRemoteDataSource()
        val repository = buildRepository(remote = remote)

        val result = repository.createTask(TaskDraft(title = "  Read docs  ", durationMinutes = 45, mandatory = true))

        assertEquals("Read docs", remote.lastCreateRequest?.title)
        assertTrue(result is Result.Success)
        assertEquals("t-1", (result as Result.Success).data.id)
        assertEquals(45, result.data.estimatedDurationMinutes)
        assertTrue(result.data.mandatory)
    }

    @Test
    fun `createTask returns Network error when offline`() = runTest(testDispatcher) {
        val remote = FakeRemoteDataSource()
        val repository = buildRepository(remote = remote, monitor = offlineMonitor)

        val result = repository.createTask(TaskDraft(title = "Read docs"))

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is com.awan.app.core.common.error.AppError.Network)
    }

    @Test
    fun `createTask persists the returned task entity to Room`() = runTest(testDispatcher) {
        val fakeDao = FakeTaskDao()
        val remote = FakeRemoteDataSource()
        val repository = buildRepository(remote = remote, taskDao = fakeDao)

        repository.createTask(TaskDraft(title = "Test task", durationMinutes = 30))

        assertEquals(1, fakeDao.upsertedTasks.size)
        assertEquals("t-1", fakeDao.upsertedTasks.first().id)
    }

    @Test
    fun `createTaskWithSessions sends offset-free local date times`() = runTest(testDispatcher) {
        val remote = FakeRemoteDataSource()
        val repository = buildRepository(remote = remote)
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
    fun `createTaskWithSessions persists task and sessions to Room`() = runTest(testDispatcher) {
        val fakeTaskDao = FakeTaskDao()
        val fakeSessionDao = FakeSessionDao()
        val start = LocalDateTime.of(2026, 7, 25, 9, 0)
        val repository = buildRepository(taskDao = fakeTaskDao, sessionDao = fakeSessionDao)

        repository.createTaskWithSessions(
            draft = TaskDraft(title = "Gym session"),
            sessions = listOf(SessionDraft(start = start, end = start.plusMinutes(60), zoneId = "zone-1")),
        )

        assertEquals(1, fakeTaskDao.upsertedTasks.size)
        assertEquals(1, fakeSessionDao.upserted.size)
    }

    @Test
    fun `createTaskWithSessions returns Network error when offline`() = runTest(testDispatcher) {
        val repository = buildRepository(monitor = offlineMonitor)

        val result = repository.createTaskWithSessions(
            draft = TaskDraft(title = "Gym"),
            sessions = emptyList(),
        )

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is com.awan.app.core.common.error.AppError.Network)
    }

    @Test
    fun `deleteTask removes task from Room after successful remote delete`() = runTest(testDispatcher) {
        val fakeTaskDao = FakeTaskDao()
        val remote = FakeRemoteDataSource()
        val repository = buildRepository(remote = remote, taskDao = fakeTaskDao)

        val result = repository.deleteTask("task-xyz")

        assertEquals("task-xyz", remote.deletedTaskId)
        assertTrue(fakeTaskDao.deletedTaskIds.contains("task-xyz"))
        assertTrue(result is Result.Success)
    }

    @Test
    fun `deleteTask returns Network error when offline`() = runTest(testDispatcher) {
        val repository = buildRepository(monitor = offlineMonitor)

        val result = repository.deleteTask("task-xyz")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is com.awan.app.core.common.error.AppError.Network)
    }
}
