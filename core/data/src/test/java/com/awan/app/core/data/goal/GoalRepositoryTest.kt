package com.awan.app.core.data.goal

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.goal.remote.GoalRemoteDataSource
import com.awan.app.core.data.goal.remote.GoalRemoteDataSourceImpl
import com.awan.app.core.database.dao.CategoryDao
import com.awan.app.core.database.dao.GoalDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.model.CategoryEntity
import com.awan.app.core.database.model.GoalEntity
import com.awan.app.core.database.model.TaskDependencyEntity
import com.awan.app.core.database.model.TaskEntity
import com.awan.app.core.network.api.GoalApiService
import com.awan.app.core.network.dto.GoalInfoResponse
import com.awan.app.core.network.dto.GoalStatusDto
import com.awan.app.core.network.dto.category.CategoryDto
import com.awan.app.core.network.dto.task.TaskInfoResponse
import com.awan.app.core.network.dto.PageResponse
import com.awan.app.core.model.ProposedTask
import com.awan.app.core.model.Task
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GoalRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }

    private open class FakeGoalApiService : GoalApiService {
        override suspend fun listGoals(
            status: String?,
            includeInbox: Boolean,
            expand: Boolean,
        ): PageResponse<GoalInfoResponse> = error("Not implemented")

        override suspend fun createGoal(request: com.awan.app.core.network.dto.goal.CreateGoalRequest): GoalInfoResponse = error("Not implemented")
        override suspend fun addTasksToGoal(goalId: String, request: com.awan.app.core.network.dto.goal.BulkCreateGoalTasksRequest): List<com.awan.app.core.network.dto.task.TaskInfoResponse> = error("Not implemented")
        override suspend fun getInboxGoal(): GoalInfoResponse = error("Not implemented")
        override suspend fun getGoal(goalId: String, expand: Boolean): GoalInfoResponse = error("Not implemented")
        override suspend fun updateGoal(goalId: String, request: com.awan.app.core.network.dto.goal.UpdateGoalRequest): GoalInfoResponse = error("Not implemented")
        override suspend fun deleteGoal(goalId: String) = error("Not implemented")

        override suspend fun decomposeGoal(
            request: com.awan.app.core.network.dto.GoalDecomposeRequest,
        ): com.awan.app.core.network.dto.GoalDecomposeResponse = error("Not implemented")

        override suspend fun confirmDecomposition(
            sessionId: String,
        ): GoalInfoResponse = error("Not implemented")

        override suspend fun getDecompositionTranscript(sessionId: String): com.awan.app.core.network.dto.goal.GoalDecompositionTranscriptResponse = error("Not implemented")
        override suspend fun cancelDecomposition(sessionId: String) = error("Not implemented")
        override suspend fun scheduleGoal(request: com.awan.app.core.network.dto.goal.ScheduleGoalRequest): com.awan.app.core.network.dto.task.TaskScheduleResponse = error("Not implemented")
        override suspend fun proposeGoalSchedule(request: com.awan.app.core.network.dto.goal.ScheduleGoalRequest): com.awan.app.core.network.dto.goal.AiGoalScheduleProposalResponse = error("Not implemented")
        override suspend fun confirmGoalSchedule(request: com.awan.app.core.network.dto.goal.ConfirmAiScheduleRequest) = error("Not implemented")
    }

    private open class FakeGoalRemoteDataSource(
        var response: Result<List<GoalInfoResponse>> = Result.Success(emptyList()),
    ) : GoalRemoteDataSource {
        var lastCreateRequest: com.awan.app.core.network.dto.goal.CreateGoalRequest? = null
        var lastBulkGoalId: String? = null
        var lastBulkRequest: com.awan.app.core.network.dto.goal.BulkCreateGoalTasksRequest? = null
        var bulkResponse: Result<List<TaskInfoResponse>> = Result.Success(emptyList())

        override suspend fun getGoals(): Result<List<GoalInfoResponse>> = response
        override suspend fun createGoal(request: com.awan.app.core.network.dto.goal.CreateGoalRequest): Result<GoalInfoResponse> {
            lastCreateRequest = request
            return Result.Success(GoalInfoResponse(id = "goal-created", title = request.title))
        }
        override suspend fun addTasksToGoal(goalId: String, request: com.awan.app.core.network.dto.goal.BulkCreateGoalTasksRequest): Result<List<TaskInfoResponse>> {
            lastBulkGoalId = goalId
            lastBulkRequest = request
            return bulkResponse
        }
        override suspend fun getInboxGoal(): Result<GoalInfoResponse> = error("Not implemented")
        override suspend fun getGoal(goalId: String, expand: Boolean): Result<GoalInfoResponse> = error("Not implemented")
        override suspend fun updateGoal(goalId: String, request: com.awan.app.core.network.dto.goal.UpdateGoalRequest): Result<GoalInfoResponse> = error("Not implemented")
        override suspend fun deleteGoal(goalId: String): Result<Unit> = error("Not implemented")
        override suspend fun continueDecomposition(request: com.awan.app.core.network.dto.GoalDecomposeRequest): Result<com.awan.app.core.network.dto.GoalDecomposeResponse> = error("Not implemented")
        override suspend fun confirmDecomposition(sessionId: String): Result<GoalInfoResponse> = error("Not implemented")
        override suspend fun getDecompositionTranscript(sessionId: String): Result<com.awan.app.core.network.dto.goal.GoalDecompositionTranscriptResponse> = error("Not implemented")
        override suspend fun cancelDecomposition(sessionId: String): Result<Unit> = error("Not implemented")
        override suspend fun scheduleGoal(goalId: String): Result<com.awan.app.core.network.dto.task.TaskScheduleResponse> = error("Not implemented")
        override suspend fun proposeGoalSchedule(goalId: String): Result<com.awan.app.core.network.dto.goal.AiGoalScheduleProposalResponse> = error("Not implemented")
        override suspend fun confirmGoalSchedule(request: com.awan.app.core.network.dto.goal.ConfirmAiScheduleRequest): Result<Unit> = error("Not implemented")
    }


    private class FakeGoalDao(
        private val stored: List<GoalEntity> = emptyList(),
    ) : GoalDao {
        val upserted = mutableListOf<GoalEntity>()
        override suspend fun upsertGoal(goal: GoalEntity) { upserted += goal }
        override suspend fun upsertGoals(goals: List<GoalEntity>) { upserted += goals }
        override fun observeAllGoals(): Flow<List<GoalEntity>> = flowOf(stored)
        override suspend fun getAllGoals(): List<GoalEntity> = stored
        override fun observeGoalsByStatus(status: String): Flow<List<GoalEntity>> = flowOf(emptyList())
        override fun observeGoal(goalId: String): Flow<GoalEntity?> = MutableStateFlow(null)
        override suspend fun getGoal(goalId: String): GoalEntity? = stored.firstOrNull { it.id == goalId }
        override suspend fun deleteGoal(goalId: String) {}
        override suspend fun getMinExpiryTime(): Long? = null
    }

    private class FakeTaskDao : TaskDao {
        override suspend fun upsertTask(task: TaskEntity) {}
        override suspend fun upsertTasks(tasks: List<TaskEntity>) {}
        override fun observeTasksByGoal(goalId: String): Flow<List<TaskEntity>> = flowOf(emptyList())
        override suspend fun getTasksByGoal(goalId: String): List<TaskEntity> = emptyList()
        override fun observeTask(taskId: String): Flow<TaskEntity?> = flowOf(null)
        override suspend fun getTask(taskId: String): TaskEntity? = null
        override suspend fun deleteTask(taskId: String) {}
        override suspend fun upsertDependency(dependency: TaskDependencyEntity) {}
        override suspend fun upsertDependencies(dependencies: List<TaskDependencyEntity>) {}
        override suspend fun deleteDependency(dependency: TaskDependencyEntity) {}
        override fun observeDependsOnIds(taskId: String): Flow<List<String>> = flowOf(emptyList())
        override suspend fun getDependsOnIds(taskId: String): List<String> = emptyList()
        override fun observeDependentIds(taskId: String): Flow<List<String>> = flowOf(emptyList())
        override suspend fun deleteAllDependenciesForTask(taskId: String) {}
        override suspend fun replaceTasksForGoal(goalId: String, tasks: List<TaskEntity>, dependencies: List<TaskDependencyEntity>) {}
        override suspend fun deleteTasksByGoal(goalId: String) {}
        override suspend fun nullifyOrphanedGoalReferences() {}
    }

    private class FakeCategoryDao : CategoryDao {
        override suspend fun upsertCategories(categories: List<CategoryEntity>) {}
        override suspend fun upsertCategory(category: CategoryEntity) {}
        override fun observeAllCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override suspend fun getAllCategories(): List<CategoryEntity> = emptyList()
        override suspend fun getCategory(id: String): CategoryEntity? = null
        override suspend fun deleteCategory(id: String) {}
        override suspend fun deleteAllCategories() {}
        override suspend fun getMinExpiryTime(): Long? = null
    }

    @Test
    fun `remote data source returns page content with expected default arguments`() = runTest(testDispatcher) {
        var capturedStatus: String? = "NON_NULL"
        var capturedIncludeInbox: Boolean? = null
        var capturedExpand: Boolean? = null

        val api = object : FakeGoalApiService() {
            override suspend fun listGoals(
                status: String?,
                includeInbox: Boolean,
                expand: Boolean,
            ): PageResponse<GoalInfoResponse> {
                capturedStatus = status
                capturedIncludeInbox = includeInbox
                capturedExpand = expand
                return PageResponse(
                    content = listOf(
                        GoalInfoResponse(id = "g-1", title = "Goal 1", status = GoalStatusDto.ACTIVE)
                    ),
                    totalElements = 1,
                )
            }
        }
        val remoteDataSource = GoalRemoteDataSourceImpl(api, json, testDispatcher)

        val result = remoteDataSource.getGoals()

        assertTrue(result is Result.Success)
        val content = (result as Result.Success).data
        assertEquals(1, content.size)
        assertEquals("g-1", content[0].id)
        assertEquals(null, capturedStatus)
        assertEquals(false, capturedIncludeInbox)
        assertEquals(true, capturedExpand)
    }

    @Test
    fun `repository reads from Room and maps entities to domain models`() = runTest(testDispatcher) {
        // Simulate: Room already has goals from a prior sync
        val storedEntities = listOf(
            GoalEntity(
                id = "goal-1",
                title = "🎯 Active Goal",
                description = null,
                status = "ACTIVE",
                targetDate = null,
                createdAt = "2026-01-01T00:00:00Z",
                isInbox = false,
            ),
        )
        val dao = FakeGoalDao(stored = storedEntities)
        val onlineMonitor = object : com.awan.app.core.domain.network.NetworkConnectivityMonitor {
            override val isOnline: kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.flowOf(true)
            override fun isCurrentlyOnline(): Boolean = true
        }
        val repository = GoalRepositoryImpl(
            remoteDataSource = FakeGoalRemoteDataSource(),
            goalDao = dao,
            taskDao = FakeTaskDao(),
            categoryDao = FakeCategoryDao(),
            connectivityMonitor = onlineMonitor,
<<<<<<< HEAD
            ioDispatcher = testDispatcher,
=======
            categoryDao = TestCategoryDao(),
            taskDao = TestTaskDao(),
>>>>>>> 99c21bd6 (AWAN-83: use goal task bulk endpoint)
        )

        val result = repository.getGoals()

        assertTrue(result is Result.Success)
        val goals = (result as Result.Success).data
        assertEquals(1, goals.size)
        assertEquals("goal-1", goals[0].id)
        assertEquals("Active Goal", goals[0].title)
        assertEquals("🎯", goals[0].emoji)
    }

    @Test
    fun `repository returns empty list when Room has no goals`() = runTest(testDispatcher) {
        val onlineMonitor = object : com.awan.app.core.domain.network.NetworkConnectivityMonitor {
            override val isOnline: kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.flowOf(true)
            override fun isCurrentlyOnline(): Boolean = true
        }
        val repository = GoalRepositoryImpl(
            remoteDataSource = FakeGoalRemoteDataSource(),
            goalDao = FakeGoalDao(stored = emptyList()),
            taskDao = FakeTaskDao(),
            categoryDao = FakeCategoryDao(),
            connectivityMonitor = onlineMonitor,
<<<<<<< HEAD
            ioDispatcher = testDispatcher,
=======
            categoryDao = TestCategoryDao(),
            taskDao = TestTaskDao(),
>>>>>>> 99c21bd6 (AWAN-83: use goal task bulk endpoint)
        )


        val result = repository.getGoals()

        assertTrue(result is Result.Success)
        assertEquals(0, (result as Result.Success).data.size)
    }

    @Test
    fun repositoryMapsEveryProposedTaskToLiveCreateRequest() = runTest(testDispatcher) {
        val remote = FakeGoalRemoteDataSource()
        val onlineMonitor = object : com.awan.app.core.domain.network.NetworkConnectivityMonitor {
            override val isOnline: kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.flowOf(true)
            override fun isCurrentlyOnline(): Boolean = true
        }
        val repository = GoalRepositoryImpl(
            remoteDataSource = remote,
            goalDao = FakeGoalDao(),
            connectivityMonitor = onlineMonitor,
            categoryDao = TestCategoryDao(),
            taskDao = TestTaskDao(),
        )

        val result = repository.createGoal(
            title = "Goal",
            description = "Description",
            targetDate = "2026-09-01",
            tasks = listOf(ProposedTask("Task A", estimatedDuration = null, estimatedPoints = null)),
        )

        assertTrue(result is Result.Success)
        val request = checkNotNull(remote.lastCreateRequest)
        assertEquals("Goal", request.title)
        assertEquals("Description", request.description)
        assertEquals("2026-09-01", request.targetDate)
        assertEquals(1, request.tasks.size)
        assertEquals("proposal-task-0", request.tasks.single().tempId)
        assertEquals("Task A", request.tasks.single().title)
        assertEquals(30, request.tasks.single().estimatedDuration)
        assertFalse(request.tasks.single().mandatory)
        assertEquals(0, request.tasks.single().estimatedPoints)
    }

    @Test
    fun `repository saves proposed tasks through the goal bulk endpoint`() = runTest(testDispatcher) {
        val remote = FakeGoalRemoteDataSource().apply {
            bulkResponse = Result.Success(
                listOf(TaskInfoResponse(id = "task-1", title = "Task A", estimatedDuration = 30, goalId = null)),
            )
        }
        val taskDao = TestTaskDao()
        val repository = GoalRepositoryImpl(
            remoteDataSource = remote,
            goalDao = FakeGoalDao(),
            connectivityMonitor = object : com.awan.app.core.domain.network.NetworkConnectivityMonitor {
                override val isOnline = flowOf(true)
                override fun isCurrentlyOnline() = true
            },
            categoryDao = TestCategoryDao(),
            taskDao = taskDao,
        )

        val result: Result<List<Task>> = repository.addTasksToGoal(
            goalId = "goal-1",
            tasks = listOf(ProposedTask("Task A", estimatedDuration = null, estimatedPoints = null)),
        )

        assertTrue(result is Result.Success)
        assertEquals("goal-1", remote.lastBulkGoalId)
        assertEquals("proposal-task-0", remote.lastBulkRequest?.tasks?.single()?.tempId)
        assertEquals(30, remote.lastBulkRequest?.tasks?.single()?.estimatedDuration)
        assertEquals("goal-1", taskDao.tasks.single().goalId)
        assertEquals("task-1", (result as Result.Success).data.single().id)
    }
    @Test(expected = CancellationException::class)
    fun `cancellation from the API is rethrown by remote data source`() = runTest(testDispatcher) {
        val api = object : FakeGoalApiService() {
            override suspend fun listGoals(
                status: String?,
                includeInbox: Boolean,
                expand: Boolean,
            ): PageResponse<GoalInfoResponse> {
                throw CancellationException("Cancelled")
            }
        }
        val remoteDataSource = GoalRemoteDataSourceImpl(api, json, testDispatcher)

        remoteDataSource.getGoals()
    }
}
