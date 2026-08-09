package com.awan.app.core.data.home

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.dao.TemplateDao
import com.awan.app.core.database.dao.ZoneDao
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserPreferencesEntity
import com.awan.app.core.database.model.UserWithPreferences
import com.awan.app.core.database.model.TemplateDayOfWeekEntity
import com.awan.app.core.database.model.ZoneEntity
import com.awan.app.core.data.gamification.GamificationEventBus
import com.awan.app.core.domain.gamification.model.RewardEvent
import com.awan.app.core.network.dto.gamification.PointsRewardDto
import com.awan.app.core.network.dto.gamification.RewardDto
import com.awan.app.core.network.dto.gamification.StreakRewardDto
import com.awan.app.core.network.dto.session.CompleteSessionResponse
import com.awan.app.core.data.home.remote.HomeRemoteDataSource
import com.awan.app.core.data.home.repository.HomeRepositoryImpl
import com.awan.app.core.network.dto.category.CategoryDto
import com.awan.app.core.network.dto.onboarding.CompleteOnboardingResponse
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.task.TaskInfoResponse
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import com.awan.app.core.network.dto.zone.TemplateOverrideDto
import com.awan.app.core.network.dto.zone.WeeklyTemplateDto
import com.awan.app.core.network.dto.zone.ZoneDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeHomeRemoteDataSource : HomeRemoteDataSource {
    var sessionResult: Result<SessionDto> = Result.Error(AppError.Unknown())
    var taskResult: Result<TaskInfoResponse> = Result.Error(AppError.Unknown())
    var reward: RewardDto? = null
    var completeResult: Result<CompleteSessionResponse>? = null
    var deleteResult: Result<Unit> = Result.Success(Unit)
    var uncompleteCalls = 0
    var moveCalls = 0

    override suspend fun completeSession(sessionId: String): Result<CompleteSessionResponse> =
        completeResult ?: Result.Success(
            CompleteSessionResponse(
                session = SessionDto(id = sessionId, start = START, end = END, status = "COMPLETED"),
                reward = reward,
            )
        )

    override suspend fun uncompleteSession(sessionId: String): Result<SessionDto> {
        uncompleteCalls++
        return Result.Success(
            SessionDto(id = sessionId, start = START, end = END, status = "SCHEDULED")
        )
    }

    override suspend fun cancelSession(sessionId: String): Result<SessionDto> =
        Result.Success(SessionDto(id = sessionId, start = START, end = END, status = "CANCELLED"))

    override suspend fun moveSession(
        sessionId: String,
        startIso: String,
        endIso: String,
    ): Result<SessionDto> {
        moveCalls++
        return Result.Success(SessionDto(id = sessionId, start = startIso, end = endIso))
    }

    override suspend fun getZonesByDate(date: String): Result<List<ZoneDto>> = Result.Success(emptyList())
    override suspend fun getTasksByDate(date: String): Result<List<TaskWithSessionsDto>> = Result.Success(emptyList())
    override suspend fun getTemplates(): Result<List<WeeklyTemplateDto>> = Result.Success(emptyList())
    override suspend fun getTemplateOverrides(): Result<List<TemplateOverrideDto>> = Result.Success(emptyList())
    override suspend fun getUserProfile(): Result<CompleteOnboardingResponse> = Result.Error(AppError.Unknown())
    override suspend fun getSession(sessionId: String): Result<SessionDto> = sessionResult
    override suspend fun getTask(taskId: String): Result<TaskInfoResponse> = taskResult
    override suspend fun lockSession(sessionId: String): Result<SessionDto> = sessionResult
    override suspend fun unlockSession(sessionId: String): Result<SessionDto> = sessionResult
    override suspend fun updateTask(
        taskId: String,
        request: com.awan.app.core.network.dto.task.TaskUpdateRequest,
    ): Result<TaskInfoResponse> = taskResult
    override suspend fun deleteSession(sessionId: String): Result<Unit> = deleteResult
    override suspend fun deleteTask(taskId: String, cascade: Boolean): Result<Unit> = Result.Success(Unit)
}

private class FakeUserDao : UserDao {
    override suspend fun upsertUser(user: UserEntity) {}
    override fun observeUser(userId: String): Flow<UserEntity?> = flowOf(null)
    override suspend fun getUser(userId: String): UserEntity? = null
    override suspend fun getFirstUser(): UserEntity? = null
    override suspend fun deleteUser(userId: String) {}
    override suspend fun upsertPreferences(preferences: UserPreferencesEntity) {}
    override fun observePreferences(userId: String): Flow<UserPreferencesEntity?> = flowOf(null)
    override suspend fun getPreferences(userId: String): UserPreferencesEntity? = null
    override fun observeUserWithPreferences(userId: String): Flow<UserWithPreferences?> = flowOf(null)
    override suspend fun getUserWithPreferences(userId: String): UserWithPreferences? = null
    override suspend fun getMinExpiryTime(): Long? = null
}

private class FakeTaskDao : com.awan.app.core.database.dao.TaskDao {
    override suspend fun upsertTask(task: com.awan.app.core.database.model.TaskEntity) {}
    override suspend fun upsertTasks(tasks: List<com.awan.app.core.database.model.TaskEntity>) {}
    override fun observeTasksByGoal(goalId: String): Flow<List<com.awan.app.core.database.model.TaskEntity>> = flowOf(emptyList())
    override fun observeInboxTasks(): Flow<List<com.awan.app.core.database.model.TaskEntity>> = flowOf(emptyList())
    override fun observeAllTasks(): Flow<List<com.awan.app.core.database.model.TaskEntity>> = flowOf(emptyList())
    override suspend fun getAllTasks(): List<com.awan.app.core.database.model.TaskEntity> = emptyList()
    override fun observeTask(taskId: String): Flow<com.awan.app.core.database.model.TaskEntity?> = flowOf(null)
    override suspend fun getTask(taskId: String): com.awan.app.core.database.model.TaskEntity? = null
    override suspend fun deleteTask(taskId: String) {}
    override suspend fun upsertDependency(dependency: com.awan.app.core.database.model.TaskDependencyEntity) {}
    override suspend fun upsertDependencies(dependencies: List<com.awan.app.core.database.model.TaskDependencyEntity>) {}
    override suspend fun deleteDependency(dependency: com.awan.app.core.database.model.TaskDependencyEntity) {}
    override fun observeDependsOnIds(taskId: String): Flow<List<String>> = flowOf(emptyList())
    override fun observeDependentIds(taskId: String): Flow<List<String>> = flowOf(emptyList())
    override suspend fun deleteAllDependenciesForTask(taskId: String) {}
    override suspend fun deleteTasksByGoal(goalId: String) {}
    override suspend fun nullifyOrphanedGoalReferences() {}
}

private class FakeSessionDao : com.awan.app.core.database.dao.SessionDao {
    override suspend fun upsertSession(session: com.awan.app.core.database.model.SessionEntity) {}
    override suspend fun upsertSessions(sessions: List<com.awan.app.core.database.model.SessionEntity>) {}
    override fun observeSessionsForDate(date: String): Flow<List<com.awan.app.core.database.model.SessionEntity>> = flowOf(emptyList())
    override fun observeSessionsForDateRange(startDate: String, endDate: String): Flow<List<com.awan.app.core.database.model.SessionEntity>> = flowOf(emptyList())
    override suspend fun getSessionsForDate(date: String): List<com.awan.app.core.database.model.SessionEntity> = emptyList()
    override suspend fun getSessionsForDateRange(startDate: String, endDate: String): List<com.awan.app.core.database.model.SessionEntity> = emptyList()
    override suspend fun getSession(id: String): com.awan.app.core.database.model.SessionEntity? = null
    override suspend fun deleteSessionsForDates(dates: List<String>) {}
    override suspend fun deleteSession(id: String) {}
    override suspend fun replaceSessionsForDates(dates: List<String>, sessions: List<com.awan.app.core.database.model.SessionEntity>) {}
}

private class FakeZoneDao(
    private val templateZones: List<ZoneEntity> = emptyList(),
) : ZoneDao {
    override suspend fun upsertZone(zone: com.awan.app.core.database.model.ZoneEntity) {}
    override suspend fun upsertZones(zones: List<com.awan.app.core.database.model.ZoneEntity>) {}
    override fun observeZone(zoneId: String): Flow<com.awan.app.core.database.model.ZoneEntity?> = flowOf(null)
    override suspend fun getZone(zoneId: String): com.awan.app.core.database.model.ZoneEntity? = null
    override fun observeZonesForTemplate(templateId: String): Flow<List<ZoneEntity>> = flowOf(templateZones)
    override fun observeZonesForOverride(overrideId: String): Flow<List<com.awan.app.core.database.model.ZoneEntity>> = flowOf(emptyList())
    override suspend fun deleteZone(zoneId: String) {}
    override suspend fun deleteZonesForTemplate(templateId: String) {}
    override suspend fun deleteZonesForOverride(overrideId: String) {}
}

private class FakeTemplateDao(
    private val dayAssignment: TemplateDayOfWeekEntity? = null,
) : TemplateDao {
    override suspend fun upsertTemplate(template: com.awan.app.core.database.model.TemplateEntity) {}
    override suspend fun upsertTemplates(templates: List<com.awan.app.core.database.model.TemplateEntity>) {}
    override fun observeAllTemplates(): Flow<List<com.awan.app.core.database.model.TemplateEntity>> = flowOf(emptyList())
    override fun observeTemplate(templateId: String): Flow<com.awan.app.core.database.model.TemplateEntity?> = flowOf(null)
    override suspend fun getTemplate(templateId: String): com.awan.app.core.database.model.TemplateEntity? = null
    override suspend fun deleteTemplate(templateId: String) {}
    override suspend fun getMinExpiryTime(): Long? = null
    override suspend fun upsertDays(days: List<com.awan.app.core.database.model.TemplateDayOfWeekEntity>) {}
    override fun observeDaysForTemplate(templateId: String): Flow<List<com.awan.app.core.database.model.TemplateDayOfWeekEntity>> = flowOf(emptyList())
    override suspend fun getDayAssignment(dayOfWeek: String): TemplateDayOfWeekEntity? = dayAssignment
    override suspend fun deleteDaysForTemplate(templateId: String) {}
}

private class FakeTemplateOverrideDao : com.awan.app.core.database.dao.TemplateOverrideDao {
    override suspend fun upsertOverride(override: com.awan.app.core.database.model.TemplateOverrideEntity) {}
    override suspend fun upsertOverrides(overrides: List<com.awan.app.core.database.model.TemplateOverrideEntity>) {}
    override fun observeAllOverrides(): Flow<List<com.awan.app.core.database.model.TemplateOverrideEntity>> = flowOf(emptyList())
    override fun observeOverride(overrideId: String): Flow<com.awan.app.core.database.model.TemplateOverrideEntity?> = flowOf(null)
    override suspend fun getOverride(overrideId: String): com.awan.app.core.database.model.TemplateOverrideEntity? = null
    override suspend fun getOverrideForDate(date: String): com.awan.app.core.database.model.TemplateOverrideEntity? = null
    override suspend fun deleteOverride(overrideId: String) {}
}

private class FakeCategoryDao : com.awan.app.core.database.dao.CategoryDao {
    override suspend fun upsertCategories(categories: List<com.awan.app.core.database.model.CategoryEntity>) {}
    override suspend fun upsertCategory(category: com.awan.app.core.database.model.CategoryEntity) {}
    override fun observeAllCategories(): Flow<List<com.awan.app.core.database.model.CategoryEntity>> = flowOf(emptyList())
    override suspend fun getAllCategories(): List<com.awan.app.core.database.model.CategoryEntity> = emptyList()
    override suspend fun getCategory(id: String): com.awan.app.core.database.model.CategoryEntity? = null
    override suspend fun deleteCategory(id: String) {}
    override suspend fun deleteAllCategories() {}
    override suspend fun getMinExpiryTime(): Long? = null
}

private open class AlwaysOnlineMonitor : com.awan.app.core.domain.network.NetworkConnectivityMonitor {
    override val isOnline: Flow<Boolean> = flowOf(true)
    override fun isCurrentlyOnline(): Boolean = true
}

class HomeRepositoryImplTest {

    private val eventBus = GamificationEventBus()

    private fun createRepository(
        fakeRemote: HomeRemoteDataSource,
        zoneDao: ZoneDao = FakeZoneDao(),
        templateDao: TemplateDao = FakeTemplateDao(),
        connectivityMonitor: com.awan.app.core.domain.network.NetworkConnectivityMonitor = AlwaysOnlineMonitor(),
    ): HomeRepositoryImpl {
        return HomeRepositoryImpl(
            remoteDataSource = fakeRemote,
            userDao = FakeUserDao(),
            eventBus = eventBus,
            taskDao = FakeTaskDao(),
            sessionDao = FakeSessionDao(),
            zoneDao = zoneDao,
            templateDao = templateDao,
            templateOverrideDao = FakeTemplateOverrideDao(),
            categoryDao = FakeCategoryDao(),
            connectivityMonitor = connectivityMonitor,
            ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
        )
    }

    @Test
    fun `getSessionDetail fetches session and task sequentially and returns success`() = runTest {
        val fakeRemote = FakeHomeRemoteDataSource()
        val repository = createRepository(fakeRemote)

        val sessionId = "session-123"
        val taskId = "task-456"

        fakeRemote.sessionResult = Result.Success(
            SessionDto(
                id = sessionId,
                start = "2026-08-05T09:00:00",
                end = "2026-08-05T09:30:00",
                status = "SCHEDULED",
                locked = false,
                zoneId = "zone-789",
                taskId = taskId,
            )
        )

        fakeRemote.taskResult = Result.Success(
            TaskInfoResponse(
                id = taskId,
                title = "Study French Vocabulary",
                description = "Review 50 new words",
                estimatedDuration = 30,
                status = "SCHEDULED",
                mandatory = true,
                estimatedPoints = 10,
                allowTaskSplitting = false,
                goalId = "goal-000",
                category = CategoryDto(id = "cat-1", name = "Language Learning"),
                dependsOnTaskIds = emptyList(),
            )
        )

        val result = repository.getSessionDetail(sessionId)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(sessionId, data.session.id)
        assertEquals(taskId, data.task.id)
        assertEquals("Study French Vocabulary", data.task.title)
        assertEquals("Language Learning", data.task.categoryName)
        assertEquals(true, data.task.mandatory)
    }

    @Test
    fun `getSessionDetail returns error if session call fails`() = runTest {
        val fakeRemote = FakeHomeRemoteDataSource()
        val repository = createRepository(fakeRemote)

        val sessionId = "session-error"
        val error = AppError.Network
        fakeRemote.sessionResult = Result.Error(error)
        val result = repository.getSessionDetail(sessionId)

        assertTrue(result is Result.Error)
        assertEquals(error, (result as Result.Error).error)
    }

    @Test
    fun `getDaySchedule emits schedule reactively from sessionDao flow`() = runTest {
        val fakeRemote = FakeHomeRemoteDataSource()
        val repository = createRepository(fakeRemote)
        val date = java.time.LocalDate.now()

        val results = mutableListOf<Result<com.awan.app.core.domain.home.model.DaySchedule>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getDaySchedule(date).collect { results.add(it) }
        }

        assertTrue(results.isNotEmpty())
        assertTrue(results.first() is Result.Success)
        val schedule = (results.first() as Result.Success).data
        assertEquals(date, schedule.date)
    }

    /** Collects on an unconfined dispatcher so emissions land before the assertions run. */
    private fun runCollecting(
        block: suspend (FakeHomeRemoteDataSource, List<RewardEvent>) -> Unit,
    ) = runTest {
        val fakeRemote = FakeHomeRemoteDataSource()
        val collected = mutableListOf<RewardEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            eventBus.rewards.collect(collected::add)
        }
        block(fakeRemote, collected)
        job.cancel()
    }

    @Test
    fun `completing publishes points then streak, in that order`() = runCollecting { remote, events ->
        remote.reward = RewardDto(
            points = PointsRewardDto(awarded = true, amount = 25, oldValue = 150, newValue = 175),
            streak = StreakRewardDto(
                updated = true,
                oldValue = 5,
                newValue = 6,
                maxStreakBroken = true,
                maxStreakNew = 7,
            ),
        )

        val result = createRepository(remote).completeSession(SESSION_ID)

        assertTrue(result is Result.Success)
        assertEquals(2, events.size)
        assertEquals(RewardEvent.Points(amount = 25, newTotal = 175), events[0])
        assertTrue(events[1] is RewardEvent.Streak)
        assertEquals(true, (events[1] as RewardEvent.Streak).maxStreakBroken)
        assertEquals(7, (events[1] as RewardEvent.Streak).maxStreakNew)
    }

    @Test
    fun `re-completing a session publishes nothing and returns an empty reward`() =
        runCollecting { remote, events ->
            remote.reward = RewardDto(
                points = PointsRewardDto(awarded = false, amount = 25, oldValue = 150, newValue = 175),
                streak = StreakRewardDto(updated = false, oldValue = 5, newValue = 6),
            )

            val result = createRepository(remote).completeSession(SESSION_ID)

            assertTrue(events.isEmpty())
            assertTrue((result as Result.Success).data.isEmpty)
        }

    @Test
    fun `a completion with no reward block publishes nothing`() = runCollecting { remote, events ->
        remote.reward = null

        val result = createRepository(remote).completeSession(SESSION_ID)

        assertTrue(events.isEmpty())
        assertTrue((result as Result.Success).data.isEmpty)
    }

    @Test
    fun `uncompleting publishes nothing - points are never taken back`() =
        runCollecting { remote, events ->
            val result = createRepository(remote).uncompleteSession(SESSION_ID)

            assertTrue(result is Result.Success)
            assertEquals(1, remote.uncompleteCalls)
            assertTrue(events.isEmpty())
        }

    @Test
    fun `moving a session publishes nothing and never sends a status`() =
        runCollecting { remote, events ->
            val result = createRepository(remote).moveSession(SESSION_ID, START, END)

            assertTrue(result is Result.Success)
            assertEquals(1, remote.moveCalls)
            assertTrue(events.isEmpty())
        }

    @Test
    fun `a failed completion publishes nothing`() = runCollecting { remote, events ->
        remote.completeResult = Result.Error(AppError.Network)

        val result = createRepository(remote).completeSession(SESSION_ID)

        assertTrue(result is Result.Error)
        assertTrue(events.isEmpty())
        assertEquals(0, eventBus.progress.value.points)
    }

    @Test
    fun `getDaySchedule preserves the Room zone name for Home labels`() = runTest {
        val date = java.time.LocalDate.of(2026, 8, 9)
        val repository = createRepository(
            fakeRemote = FakeHomeRemoteDataSource(),
            zoneDao = FakeZoneDao(
                templateZones = listOf(
                    ZoneEntity(
                        id = "zone-study",
                        name = "Study",
                        startTime = "09:00:00",
                        endTime = "12:00:00",
                        color = "#4F46E5",
                        templateId = "template-1",
                        templateOverrideId = null,
                    ),
                ),
            ),
            templateDao = FakeTemplateDao(
                dayAssignment = TemplateDayOfWeekEntity(
                    dayOfWeek = date.dayOfWeek.name,
                    templateId = "template-1",
                ),
            ),
        )

        val result = repository.getDaySchedule(date).first()

        assertTrue(result is Result.Success)
        assertEquals("Study", (result as Result.Success).data.zones.single().categoryName)
    }

    @Test
    fun `deleteSession returns AppError Network when offline`() = runTest {
        val remote = FakeHomeRemoteDataSource()
        val offlineMonitor = object : AlwaysOnlineMonitor() {
            override fun isCurrentlyOnline(): Boolean = false
        }
        val repository = createRepository(remote, connectivityMonitor = offlineMonitor)
        
        val result = repository.deleteSession("s1")
        
        assertTrue(result is Result.Error)
        assertEquals(AppError.Network, (result as Result.Error).error)
    }

    @Test
    fun `deleteSession returns remote error when call fails`() = runTest {
        val remote = FakeHomeRemoteDataSource()
        val error = AppError.Server(500)
        remote.deleteResult = Result.Error(error)
        val repository = createRepository(remote)
        
        val result = repository.deleteSession("s1")
        
        assertTrue(result is Result.Error)
        assertEquals(error, (result as Result.Error).error)
    }

    @Test
    fun `mapStatus falls back to UNKNOWN for unrecognized server values`() = runTest {
        val fakeRemote = FakeHomeRemoteDataSource()
        val sessionId = "session-123"
        val taskId = "task-456"

        fakeRemote.sessionResult = Result.Success(
            SessionDto(
                id = sessionId,
                start = "2026-08-05T09:00:00",
                end = "2026-08-05T09:30:00",
                status = "TOTALLY_NEW_STATUS",
                locked = false,
                zoneId = "zone-789",
                taskId = taskId,
            )
        )

        fakeRemote.taskResult = Result.Success(
            TaskInfoResponse(
                id = taskId,
                title = "Study",
                estimatedDuration = 30,
                status = "SCHEDULED",
                estimatedPoints = 10,
            )
        )

        val repository = createRepository(fakeRemote)
        val result = repository.getSessionDetail(sessionId)

        assertTrue(result is Result.Success)
        assertEquals(com.awan.app.core.model.SessionStatus.UNKNOWN, (result as Result.Success).data.session.status)
    }
}

private const val SESSION_ID = "session-1"
private const val START = "2026-08-08T09:00:00"
private const val END = "2026-08-08T10:00:00"

