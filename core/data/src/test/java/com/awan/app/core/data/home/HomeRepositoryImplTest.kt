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
import com.awan.app.core.database.dao.SessionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import com.awan.app.core.domain.home.model.DaySchedule
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

private class FakeHomeLocalDataSource(
    templateZones: List<ZoneEntity> = emptyList(),
    private val tasks: Map<String, com.awan.app.core.database.model.TaskEntity> = emptyMap(),
) : com.awan.app.core.data.home.local.HomeLocalDataSource {

    val effectiveZones = MutableStateFlow(templateZones)
    val sessions = MutableStateFlow(emptyList<com.awan.app.core.database.model.SessionEntity>())
    var cachedSessions = mutableListOf<SessionDto>()
    var deletedSessions = mutableListOf<String>()
    var deletedTasks = mutableListOf<String>()
    var cachedTasks = mutableListOf<String>()
    var upsertedUsers = mutableListOf<UserEntity>()
    var storedUser: UserEntity? = null

    override fun observeSessionsForDate(date: String) = sessions
    override fun observeEffectiveZonesForDate(date: String, dayOfWeek: String) = effectiveZones
    override suspend fun getTask(taskId: String) = tasks[taskId]
    override suspend fun getCategory(categoryId: String): com.awan.app.core.database.model.CategoryEntity? = null
    override suspend fun getCachedUser() = storedUser
    override suspend fun upsertUser(user: UserEntity) { upsertedUsers += user; storedUser = user }
    override suspend fun cacheSession(session: SessionDto): Boolean {
        cachedSessions += session
        return true
    }
    override suspend fun getSession(sessionId: String): com.awan.app.core.database.model.SessionEntity? =
        sessions.value.firstOrNull { it.id == sessionId }
    override suspend fun deleteSession(sessionId: String) { deletedSessions += sessionId }
    override suspend fun cacheTask(taskId: String, task: TaskInfoResponse) { cachedTasks += taskId }
    override suspend fun deleteTask(taskId: String) { deletedTasks += taskId }
}

private class FakeScheduleSynchronizer : com.awan.app.core.data.sync.ScheduleSynchronizer {
    var syncedRanges = mutableListOf<Triple<java.time.LocalDate, java.time.LocalDate, Boolean>>()
    var result = true

    override suspend fun syncScheduleRange(
        startDate: java.time.LocalDate,
        endDate: java.time.LocalDate,
        forceRefresh: Boolean,
    ): Boolean {
        syncedRanges += Triple(startDate, endDate, forceRefresh)
        return result
    }
}

private open class AlwaysOnlineMonitor : com.awan.app.core.domain.network.NetworkConnectivityMonitor {
    override val isOnline: Flow<Boolean> = flowOf(true)
    override fun isCurrentlyOnline(): Boolean = true
}

class HomeRepositoryImplTest {

    private val eventBus = GamificationEventBus()

    private fun createRepository(
        fakeRemote: HomeRemoteDataSource,
        local: FakeHomeLocalDataSource = FakeHomeLocalDataSource(),
        scheduleSynchronizer: com.awan.app.core.data.sync.ScheduleSynchronizer = FakeScheduleSynchronizer(),
        connectivityMonitor: com.awan.app.core.domain.network.NetworkConnectivityMonitor = AlwaysOnlineMonitor(),
    ): HomeRepositoryImpl {
        return HomeRepositoryImpl(
            remoteDataSource = fakeRemote,
            local = local,
            eventBus = eventBus,
            scheduleSynchronizer = scheduleSynchronizer,
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
            local = FakeHomeLocalDataSource(
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
        )

        val result = repository.getDaySchedule(date).first()

        assertTrue(result is Result.Success)
        assertEquals("Study", (result as Result.Success).data.zones.single().categoryName)
    }

    /**
     * The reported bug: a zone edited elsewhere reached Room but Home kept showing the old one until
     * the day changed, because the Flow was invalidated by the `sessions` table alone.
     */
    @Test
    fun `getDaySchedule re-emits when only the zones change`() = runTest {
        val local = FakeHomeLocalDataSource()
        val repository = createRepository(fakeRemote = FakeHomeRemoteDataSource(), local = local)

        val emissions = mutableListOf<DaySchedule>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.getDaySchedule(java.time.LocalDate.of(2026, 8, 9)).collect {
                if (it is Result.Success) emissions += it.data
            }
        }

        local.effectiveZones.value = listOf(
            ZoneEntity(
                id = "zone-work",
                name = "Work",
                startTime = "09:00:00",
                endTime = "12:00:00",
                color = null,
                templateId = "template-1",
                templateOverrideId = null,
            ),
        )
        job.cancel()

        assertEquals(2, emissions.size)
        assertTrue(emissions.first().zones.isEmpty())
        assertEquals("Work", emissions.last().zones.single().name)
    }

    @Test
    fun `refreshSchedule forces a sync for the single day and reports failure`() = runTest {
        val synchronizer = FakeScheduleSynchronizer()
        val repository = createRepository(
            fakeRemote = FakeHomeRemoteDataSource(),
            scheduleSynchronizer = synchronizer,
        )
        val date = java.time.LocalDate.of(2026, 8, 9)

        assertTrue(repository.refreshSchedule(date) is Result.Success)
        assertEquals(listOf(Triple(date, date, true)), synchronizer.syncedRanges)

        synchronizer.result = false
        assertTrue(repository.refreshSchedule(date) is Result.Error)
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

