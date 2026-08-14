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
import com.awan.app.core.model.ProposedGoalSession
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


        override suspend fun getGoals(): Result<List<GoalInfoResponse>> = response
        override suspend fun createGoal(request: com.awan.app.core.network.dto.goal.CreateGoalRequest): Result<GoalInfoResponse> {
            lastCreateRequest = request
            return Result.Success(GoalInfoResponse(id = "goal-created", title = request.title))
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
        override suspend fun confirmGoalSchedule(request: com.awan.app.core.network.dto.goal.ConfirmAiScheduleRequest): Result<List<com.awan.app.core.network.dto.goal.AiConfirmedSessionItemDto>> = error("Not implemented")
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
<<<<<<< HEAD
>>>>>>> 99c21bd6 (AWAN-83: use goal task bulk endpoint)
=======
            scheduleDraftDao = TestScheduleDraftDao(),
            sessionDao = TestSessionDao()
>>>>>>> f20bbd00 (AWAN-83: reuse ai-tasks screen for goal schedule review, persist drafts, and resilient confirm parsing)
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
<<<<<<< HEAD
>>>>>>> 99c21bd6 (AWAN-83: use goal task bulk endpoint)
=======
            scheduleDraftDao = TestScheduleDraftDao(),
            sessionDao = TestSessionDao()
>>>>>>> f20bbd00 (AWAN-83: reuse ai-tasks screen for goal schedule review, persist drafts, and resilient confirm parsing)
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
            scheduleDraftDao = TestScheduleDraftDao(),
            sessionDao = TestSessionDao()
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

    @Test
    fun `confirmGoalSchedule with missing remote taskId falls back to requested taskId and upserts`() = runTest(testDispatcher) {
        val remote = object : FakeGoalRemoteDataSource() {
            override suspend fun confirmGoalSchedule(request: com.awan.app.core.network.dto.goal.ConfirmAiScheduleRequest): Result<List<com.awan.app.core.network.dto.goal.AiConfirmedSessionItemDto>> {
                return Result.Success(listOf(com.awan.app.core.network.dto.goal.AiConfirmedSessionItemDto(id = "s1", taskId = null, start = "2026-08-13T10:00:00Z", end = "2026-08-13T11:00:00Z")))
            }
        }
        val onlineMonitor = object : com.awan.app.core.domain.network.NetworkConnectivityMonitor {
            override val isOnline: kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.flowOf(true)
            override fun isCurrentlyOnline(): Boolean = true
        }
        val draftDao = TestScheduleDraftDao()
        val sessionDao = TestSessionDao()
        val repository = GoalRepositoryImpl(
            remoteDataSource = remote,
            goalDao = FakeGoalDao(),
            connectivityMonitor = onlineMonitor,
            categoryDao = TestCategoryDao(),
            taskDao = TestTaskDao(),
            scheduleDraftDao = draftDao,
            sessionDao = sessionDao
        )

        val result = repository.confirmGoalSchedule("g1", listOf(ProposedGoalSession(taskId = "req-t1", taskTitle = "Title", zoneId = null, start = "2026-08-13T10:00:00Z", end = "2026-08-13T11:00:00Z", isSelected = true)))

        assertTrue(result is Result.Success)
        assertEquals(1, sessionDao.upserted.size)
        assertEquals("req-t1", sessionDao.upserted[0].taskId)
        assertEquals(1, draftDao.deletedDrafts.size)
    }

    @Test
    fun `confirmGoalSchedule assigns TTL and atomically cleans up draft`() = runTest(testDispatcher) {
        val remote = object : FakeGoalRemoteDataSource() {
            override suspend fun confirmGoalSchedule(request: com.awan.app.core.network.dto.goal.ConfirmAiScheduleRequest): Result<List<com.awan.app.core.network.dto.goal.AiConfirmedSessionItemDto>> {
                return Result.Success(listOf(com.awan.app.core.network.dto.goal.AiConfirmedSessionItemDto(id = "s1", taskId = "t1", start = "2026-08-13T10:00:00Z", end = "2026-08-13T11:00:00Z")))
            }
        }
        val onlineMonitor = object : com.awan.app.core.domain.network.NetworkConnectivityMonitor {
            override val isOnline: kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.flowOf(true)
            override fun isCurrentlyOnline(): Boolean = true
        }
        val draftDao = TestScheduleDraftDao()
        val sessionDao = TestSessionDao()
        val repository = GoalRepositoryImpl(
            remoteDataSource = remote,
            goalDao = FakeGoalDao(),
            connectivityMonitor = onlineMonitor,
            categoryDao = TestCategoryDao(),
            taskDao = TestTaskDao(),
            scheduleDraftDao = draftDao,
            sessionDao = sessionDao
        )

        val result = repository.confirmGoalSchedule("g1", listOf())

        assertTrue(result is Result.Success)
        assertEquals(1, sessionDao.upserted.size)
        assertTrue(sessionDao.upserted[0].expiryTime > 0)
        assertEquals(1, draftDao.deletedDrafts.size)
        assertEquals("g1", draftDao.deletedDrafts[0])
    }
    @Test
    fun `proposeGoalSchedule inserts awaiting draft if not exists`() = runTest(testDispatcher) {
        val remote = object : FakeGoalRemoteDataSource() {
            override suspend fun proposeGoalSchedule(goalId: String): Result<com.awan.app.core.network.dto.goal.AiGoalScheduleProposalResponse> {
                return Result.Error(AppError.Network) // Force network error to observe pre-network state
            }
        }
        val onlineMonitor = object : com.awan.app.core.domain.network.NetworkConnectivityMonitor {
            override val isOnline: kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.flowOf(true)
            override fun isCurrentlyOnline(): Boolean = true
        }
        val draftDao = TestScheduleDraftDao()
        val repository = GoalRepositoryImpl(
            remoteDataSource = remote,
            goalDao = FakeGoalDao(),
            connectivityMonitor = onlineMonitor,
            categoryDao = TestCategoryDao(),
            taskDao = TestTaskDao(),
            scheduleDraftDao = draftDao,
            sessionDao = TestSessionDao()
        )

        val result = repository.proposeGoalSchedule("g1")
        assertTrue(result is Result.Error)
        assertEquals(1, draftDao.insertedDraftsIfNotExist.size)
        assertEquals("g1", draftDao.insertedDraftsIfNotExist[0].goalId)
        assertEquals("AWAITING_PROPOSAL", draftDao.insertedDraftsIfNotExist[0].state)
    }

    @Test
    fun `proposeGoalSchedule maps all proposal types correctly`() = runTest(testDispatcher) {
        val remote = object : FakeGoalRemoteDataSource() {
            override suspend fun proposeGoalSchedule(goalId: String): Result<com.awan.app.core.network.dto.goal.AiGoalScheduleProposalResponse> {
                return Result.Success(
                    com.awan.app.core.network.dto.goal.AiGoalScheduleProposalResponse(
                        goalId = "g1",
                        proposedSessions = listOf(
                            com.awan.app.core.network.dto.goal.ProposedGoalSessionDto(
                                taskId = "t1", taskTitle = "Task 1", zoneId = "z1", start = "10:00", end = "11:00"
                            )
                        ),
                        suggestions = listOf(
                            com.awan.app.core.network.dto.goal.GoalScheduleSuggestionDto(
                                taskId = "t2", taskTitle = "Task 2", zoneId = null, start = "11:00", end = "12:00",
                                suggestionType = "OVERLAP", reason = "Overlap",
                                overlapInfo = com.awan.app.core.network.dto.goal.ScheduleOverlapInfoDto(
                                    taskTitle = "Task X", start = "11:00", end = "12:00", mandatory = true, points = 10
                                )
                            )
                        ),
                        unscheduledTasks = listOf(
                            com.awan.app.core.network.dto.goal.UnscheduledTaskDto(
                                taskId = "t3", taskTitle = "Task 3", message = "No time"
                            )
                        )
                    )
                )
            }
        }
        val repository = GoalRepositoryImpl(
            remoteDataSource = remote,
            goalDao = FakeGoalDao(),
            connectivityMonitor = object : com.awan.app.core.domain.network.NetworkConnectivityMonitor {
                override val isOnline: kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.flowOf(true)
                override fun isCurrentlyOnline(): Boolean = true
            },
            categoryDao = TestCategoryDao(),
            taskDao = TestTaskDao(),
            scheduleDraftDao = TestScheduleDraftDao(),
            sessionDao = TestSessionDao()
        )

        val result = repository.proposeGoalSchedule("g1")
        assertTrue(result is Result.Success)
        val proposal = (result as Result.Success).data
        assertEquals(1, proposal.proposedSessions.size)
        assertEquals("t1", proposal.proposedSessions[0].taskId)
        assertEquals("Task 1", proposal.proposedSessions[0].taskTitle)
        assertEquals("z1", proposal.proposedSessions[0].zoneId)
        assertEquals("10:00", proposal.proposedSessions[0].start)
        assertEquals("11:00", proposal.proposedSessions[0].end)
        assertTrue(proposal.proposedSessions[0].isSelected)

        assertEquals(1, proposal.suggestions.size)
        assertEquals("t2", proposal.suggestions[0].taskId)
        assertEquals("Task 2", proposal.suggestions[0].taskTitle)
        assertEquals(null, proposal.suggestions[0].zoneId)
        assertEquals("11:00", proposal.suggestions[0].start)
        assertEquals("12:00", proposal.suggestions[0].end)
        assertEquals("OVERLAP", proposal.suggestions[0].suggestionType)
        assertEquals("Overlap", proposal.suggestions[0].reason)
        assertEquals("Task X", proposal.suggestions[0].overlapInfo?.taskTitle)
        assertEquals("11:00", proposal.suggestions[0].overlapInfo?.start)
        assertEquals("12:00", proposal.suggestions[0].overlapInfo?.end)
        assertEquals(true, proposal.suggestions[0].overlapInfo?.mandatory)
        assertEquals(10, proposal.suggestions[0].overlapInfo?.points)
        assertFalse(proposal.suggestions[0].isSelected)

        assertEquals(1, proposal.unscheduledTasks.size)
        assertEquals("t3", proposal.unscheduledTasks[0].taskId)
        assertEquals("Task 3", proposal.unscheduledTasks[0].taskTitle)
        assertEquals("No time", proposal.unscheduledTasks[0].message)
    }

    @Test
    fun `confirmGoalSchedule network failure keeps CONFIRMING state`() = runTest(testDispatcher) {
        val remote = object : FakeGoalRemoteDataSource() {
            override suspend fun confirmGoalSchedule(request: com.awan.app.core.network.dto.goal.ConfirmAiScheduleRequest): Result<List<com.awan.app.core.network.dto.goal.AiConfirmedSessionItemDto>> {
                return Result.Error(AppError.Network)
            }
        }
        val draftDao = TestScheduleDraftDao()
        val repository = GoalRepositoryImpl(
            remoteDataSource = remote,
            goalDao = FakeGoalDao(),
            connectivityMonitor = object : com.awan.app.core.domain.network.NetworkConnectivityMonitor {
                override val isOnline: kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.flowOf(true)
                override fun isCurrentlyOnline(): Boolean = true
            },
            categoryDao = TestCategoryDao(),
            taskDao = TestTaskDao(),
            scheduleDraftDao = draftDao,
            sessionDao = TestSessionDao()
        )

        val result = repository.confirmGoalSchedule("g1", listOf())
        assertTrue(result is Result.Error)
        assertEquals("CONFIRMING", draftDao.draftStates["g1"])
    }

    @Test
    fun `proposeGoalSchedule network failure keeps AWAITING_PROPOSAL state`() = runTest(testDispatcher) {
        val remote = object : FakeGoalRemoteDataSource() {
            override suspend fun proposeGoalSchedule(goalId: String): Result<com.awan.app.core.network.dto.goal.AiGoalScheduleProposalResponse> {
                return Result.Error(AppError.Network)
            }
        }
        val draftDao = TestScheduleDraftDao()
        val repository = GoalRepositoryImpl(
            remoteDataSource = remote,
            goalDao = FakeGoalDao(),
            connectivityMonitor = object : com.awan.app.core.domain.network.NetworkConnectivityMonitor {
                override val isOnline: kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.flowOf(true)
                override fun isCurrentlyOnline(): Boolean = true
            },
            categoryDao = TestCategoryDao(),
            taskDao = TestTaskDao(),
            scheduleDraftDao = draftDao,
            sessionDao = TestSessionDao()
        )

        val result = repository.proposeGoalSchedule("g1")
        assertTrue(result is Result.Error)
        assertEquals(1, draftDao.insertedDraftsIfNotExist.size)
        assertEquals("g1", draftDao.insertedDraftsIfNotExist[0].goalId)
        assertEquals("AWAITING_PROPOSAL", draftDao.insertedDraftsIfNotExist[0].state)
    }

    @Test
    fun `proposeGoalSchedule success sets READY state`() = runTest(testDispatcher) {
        val remote = object : FakeGoalRemoteDataSource() {
            override suspend fun proposeGoalSchedule(goalId: String): Result<com.awan.app.core.network.dto.goal.AiGoalScheduleProposalResponse> {
                return Result.Success(com.awan.app.core.network.dto.goal.AiGoalScheduleProposalResponse("g1"))
            }
        }
        val draftDao = object : TestScheduleDraftDao() {
            override suspend fun replaceDraft(
                draft: com.awan.app.core.database.model.ScheduleDraftEntity,
                sessions: List<com.awan.app.core.database.model.ScheduleDraftSessionEntity>,
                unscheduledTasks: List<com.awan.app.core.database.model.ScheduleDraftUnscheduledTaskEntity>
            ) {
                draftStates[draft.goalId] = draft.state
            }
        }
        val repository = GoalRepositoryImpl(
            remoteDataSource = remote,
            goalDao = FakeGoalDao(),
            connectivityMonitor = object : com.awan.app.core.domain.network.NetworkConnectivityMonitor {
                override val isOnline: kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.flowOf(true)
                override fun isCurrentlyOnline(): Boolean = true
            },
            categoryDao = TestCategoryDao(),
            taskDao = TestTaskDao(),
            scheduleDraftDao = draftDao,
            sessionDao = TestSessionDao()
        )

        val result = repository.proposeGoalSchedule("g1")
        assertTrue(result is Result.Success)
        assertEquals("READY", draftDao.draftStates["g1"])
    }
}
