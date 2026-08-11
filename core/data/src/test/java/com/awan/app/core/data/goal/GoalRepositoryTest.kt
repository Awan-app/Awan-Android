package com.awan.app.core.data.goal

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.goal.remote.GoalRemoteDataSource
import com.awan.app.core.data.goal.remote.GoalRemoteDataSourceImpl
import com.awan.app.core.database.dao.GoalDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.model.GoalEntity
import com.awan.app.core.database.model.TaskDependencyEntity
import com.awan.app.core.database.model.TaskEntity
import com.awan.app.core.network.api.GoalApiService
import com.awan.app.core.network.dto.GoalInfoResponse
import com.awan.app.core.network.dto.GoalStatusDto
import com.awan.app.core.network.dto.PageResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
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
        override suspend fun getGoals(): Result<List<GoalInfoResponse>> = response
        override suspend fun createGoal(request: com.awan.app.core.network.dto.goal.CreateGoalRequest): Result<GoalInfoResponse> = error("Not implemented")
        override suspend fun getInboxGoal(): Result<GoalInfoResponse> = error("Not implemented")
        override suspend fun getGoal(goalId: String): Result<GoalInfoResponse> = error("Not implemented")
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
        override fun getDependsOnIds(taskId: String): List<String> = emptyList()
        override fun observeDependentIds(taskId: String): Flow<List<String>> = flowOf(emptyList())
        override suspend fun deleteAllDependenciesForTask(taskId: String) {}
        override suspend fun replaceTasksForGoal(goalId: String, tasks: List<TaskEntity>, dependencies: List<TaskDependencyEntity>) {}
        override suspend fun deleteTasksByGoal(goalId: String) {}
        override suspend fun nullifyOrphanedGoalReferences() {}
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
            connectivityMonitor = onlineMonitor,
            ioDispatcher = testDispatcher,
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
            connectivityMonitor = onlineMonitor,
            ioDispatcher = testDispatcher,
        )


        val result = repository.getGoals()

        assertTrue(result is Result.Success)
        assertEquals(0, (result as Result.Success).data.size)
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
