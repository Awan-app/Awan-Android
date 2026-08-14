package com.awan.feature.home.impl.ui

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.gamification.model.GamificationProgress
import com.awan.app.core.domain.gamification.model.RewardEvent
import com.awan.app.core.domain.gamification.model.SessionReward
import com.awan.app.core.domain.gamification.model.WheelConfig
import com.awan.app.core.domain.gamification.model.WheelSpinResult
import com.awan.app.core.domain.gamification.repository.GamificationRepository
import com.awan.app.core.domain.gamification.usecase.GetWheelConfigUseCase
import com.awan.app.core.domain.gamification.usecase.ObserveGamificationProgressUseCase
import com.awan.app.core.domain.gamification.usecase.PublishWheelRewardUseCase
import com.awan.app.core.domain.gamification.usecase.RefreshGamificationProgressUseCase
import com.awan.app.core.domain.gamification.usecase.SpinWheelUseCase
import com.awan.app.core.domain.home.model.DaySchedule
import com.awan.app.core.domain.home.model.UserProfileInfo
import com.awan.app.core.domain.home.repository.HomeRepository
import com.awan.app.core.domain.home.usecase.CompleteSessionUseCase
import com.awan.app.core.domain.home.usecase.DeleteSessionUseCase
import com.awan.app.core.domain.home.usecase.GetDayScheduleUseCase
import com.awan.app.core.domain.home.usecase.GetSessionDetailUseCase
import com.awan.app.core.domain.home.usecase.MoveSessionUseCase
import com.awan.app.core.domain.home.usecase.RefreshDayScheduleUseCase
import com.awan.app.core.domain.home.usecase.UncompleteSessionUseCase
import com.awan.app.core.domain.home.usecase.UpdateSessionLockUseCase
import com.awan.app.core.domain.home.usecase.UpdateTaskDetailUseCase
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.profile.repository.ProfileRepository
import com.awan.app.core.domain.profile.usecase.GetProfileUseCase
import com.awan.app.core.domain.profile.usecase.ObserveProfileUseCase
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.Session
import com.awan.app.core.domain.zones.model.TemplateOverride
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.app.core.domain.zones.repository.SessionRepository
import com.awan.app.core.domain.zones.repository.ZonesRepository
import com.awan.app.core.domain.zones.usecase.RefreshZonesUseCase
import com.awan.app.core.model.DayZone
import com.awan.app.core.model.SessionDetailInfo
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.model.SessionTaskDetail
import com.awan.app.core.model.TaskDetailInfo
import com.awan.app.core.model.TaskStatus
import com.awan.app.core.model.UpdateSessionParams
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeHomeRepository = FakeHomeRepository()
    private val fakeGamificationRepository = FakeGamificationRepository()
    private val fakeZonesRepository = FakeZonesRepository()
    private val fakeProfileRepository = FakeProfileRepository()
    private val fakeSessionRepository = FakeSessionRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel = HomeViewModel(
        getDayScheduleUseCase = GetDayScheduleUseCase(fakeHomeRepository),
        observeGamificationProgressUseCase = ObserveGamificationProgressUseCase(fakeGamificationRepository),
        refreshGamificationProgressUseCase = RefreshGamificationProgressUseCase(fakeGamificationRepository),
        getWheelConfigUseCase = GetWheelConfigUseCase(fakeGamificationRepository),
        spinWheelUseCase = SpinWheelUseCase(fakeGamificationRepository),
        publishWheelRewardUseCase = PublishWheelRewardUseCase(fakeGamificationRepository),
        refreshDayScheduleUseCase = RefreshDayScheduleUseCase(fakeHomeRepository),
        refreshZonesUseCase = RefreshZonesUseCase(fakeZonesRepository),
        getSessionDetailUseCase = GetSessionDetailUseCase(fakeHomeRepository),
        updateTaskDetailUseCase = UpdateTaskDetailUseCase(fakeHomeRepository),
        deleteSessionUseCase = DeleteSessionUseCase(fakeSessionRepository),
        completeSessionUseCase = CompleteSessionUseCase(fakeHomeRepository),
        uncompleteSessionUseCase = UncompleteSessionUseCase(fakeHomeRepository),
        moveSessionUseCase = MoveSessionUseCase(fakeHomeRepository),
        updateSessionLockUseCase = UpdateSessionLockUseCase(fakeHomeRepository),
        getProfileUseCase = GetProfileUseCase(fakeProfileRepository),
        observeProfileUseCase = ObserveProfileUseCase(fakeProfileRepository),
    )

    @Test
    fun `toggleSessionStatusFromDialog triggers completeSessionUseCase when status is scheduled`() = runTest {
        val detail = createMockSessionTaskDetail(SessionStatus.SCHEDULED)
        fakeHomeRepository.detailToReturn = Result.Success(detail)

        val viewModel = createViewModel()
        viewModel.onSessionClicked("s_1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSessionStatusFromDialog()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeHomeRepository.completedSessions.size)
        assertEquals("s_1", fakeHomeRepository.completedSessions.first())
        val state = viewModel.uiState.value.selectedSessionDetailState
        assertEquals(SessionStatus.COMPLETED, state?.detail?.session?.status)
    }

    @Test
    fun `saveSessionDetailEdits success closes bottom sheet dialog`() = runTest {
        val detail = createMockSessionTaskDetail(SessionStatus.SCHEDULED)
        fakeHomeRepository.detailToReturn = Result.Success(detail)
        fakeHomeRepository.updateTaskResult = Result.Success(Unit)
        fakeHomeRepository.moveSessionResult = Result.Success(Unit)

        val viewModel = createViewModel()
        viewModel.onSessionClicked("s_1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveSessionDetailEdits()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedSessionDetailState)
    }

    @Test
    fun `saveSessionDetailEdits failure keeps bottom sheet open and exposes error`() = runTest {
        val detail = createMockSessionTaskDetail(SessionStatus.SCHEDULED)
        fakeHomeRepository.detailToReturn = Result.Success(detail)
        fakeHomeRepository.updateTaskResult = Result.Error(AppError.Network)

        val viewModel = createViewModel()
        viewModel.onSessionClicked("s_1")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.saveSessionDetailEdits()
        testDispatcher.scheduler.advanceUntilIdle()

        val dialogState = viewModel.uiState.value.selectedSessionDetailState
        assertNotNull(dialogState)
        assertEquals(false, dialogState?.isSaving)
        assertNotNull(dialogState?.errorMessage)
    }

    private fun createMockSessionTaskDetail(status: SessionStatus): SessionTaskDetail {
        val now = LocalDateTime.now()
        val session = SessionDetailInfo(
            id = "s_1",
            start = now,
            end = now.plusMinutes(30),
            status = status,
            locked = false,
            zoneId = "z_1",
            taskId = "t_1",
        )
        val task = TaskDetailInfo(
            id = "t_1",
            title = "Test Task",
            description = "Test Description",
            estimatedDuration = 30,
            status = TaskStatus.SCHEDULED,
            mandatory = true,
            estimatedPoints = 10,
            allowTaskSplitting = false,
            goalId = null,
            categoryName = "Work",
            dependsOnTaskIds = emptyList(),
        )
        return SessionTaskDetail(
            session = session,
            task = task,
            relatedSessions = listOf(session),
        )
    }
}

private class FakeHomeRepository : HomeRepository {
    var detailToReturn: Result<SessionTaskDetail> = Result.Error(AppError.Unknown())
    var updateTaskResult: Result<Unit> = Result.Success(Unit)
    var moveSessionResult: Result<Unit> = Result.Success(Unit)
    val completedSessions = mutableListOf<String>()

    override fun getDaySchedule(date: LocalDate): Flow<Result<DaySchedule>> =
        flowOf(Result.Success(DaySchedule(date, emptyList(), emptyList())))

    override suspend fun refreshSchedule(date: LocalDate): Result<Unit> = Result.Success(Unit)
    override suspend fun getUserProfile(): Result<UserProfileInfo> =
        Result.Success(UserProfileInfo("user_1", "Test", "User", 0, 0))

    override suspend fun getSessionDetail(sessionId: String): Result<SessionTaskDetail> = detailToReturn

    override suspend fun completeSession(sessionId: String): Result<SessionReward> {
        completedSessions.add(sessionId)
        return Result.Success(SessionReward(points = null, streak = null))
    }

    override suspend fun uncompleteSession(sessionId: String): Result<Unit> = Result.Success(Unit)
    override suspend fun cancelSession(sessionId: String): Result<Unit> = Result.Success(Unit)
    override suspend fun moveSession(sessionId: String, startIso: String, endIso: String): Result<Unit> = moveSessionResult
    override suspend fun updateSessionLock(sessionId: String, locked: Boolean): Result<Unit> = Result.Success(Unit)

    override suspend fun updateTaskDetails(
        taskId: String,
        title: String?,
        description: String?,
        estimatedDuration: Int?,
        estimatedPoints: Int?,
        mandatory: Boolean?,
        allowTaskSplitting: Boolean?,
    ): Result<Unit> = updateTaskResult

    override suspend fun deleteSession(sessionId: String): Result<Unit> = Result.Success(Unit)
    override suspend fun deleteTask(taskId: String): Result<Unit> = Result.Success(Unit)
}

private class FakeSessionRepository : SessionRepository {
    override suspend fun getSessionsByDate(date: LocalDate): Result<List<Session>> = Result.Success(emptyList())
    override suspend fun getSessionsByRange(startDate: LocalDate, endDate: LocalDate): Result<Map<LocalDate, List<Session>>> = Result.Success(emptyMap())
    override suspend fun getSession(sessionId: String): Result<Session> = Result.Error(AppError.Unknown())
    override suspend fun updateSession(sessionId: String, params: UpdateSessionParams): Result<Session> = Result.Error(AppError.Unknown())
    override suspend fun lockSession(sessionId: String): Result<Session> = Result.Error(AppError.Unknown())
    override suspend fun unlockSession(sessionId: String): Result<Session> = Result.Error(AppError.Unknown())
    override suspend fun deleteSession(sessionId: String): Result<Unit> = Result.Success(Unit)
}

private class FakeGamificationRepository : GamificationRepository {
    override fun observeProgress(): Flow<GamificationProgress> = flowOf(GamificationProgress(0, 0))
    override fun observeRewards(): Flow<RewardEvent> = flowOf()
    override suspend fun refreshProgress(): Result<GamificationProgress> = Result.Success(GamificationProgress(0, 0))
    override suspend fun getWheelConfig(): Result<WheelConfig> = Result.Success(WheelConfig(claimedToday = false, segments = emptyList()))
    override suspend fun spinWheel(): Result<WheelSpinResult> = Result.Error(AppError.Unknown())
    override suspend fun publishWheelReward(result: WheelSpinResult) {}
    override suspend fun getActivityDates(startDate: LocalDate, endDate: LocalDate): Result<Set<LocalDate>> = Result.Success(emptySet())
}

private class FakeZonesRepository : ZonesRepository {
    private val dummyZone = DailyZone("z1", "Work", "09:00", "12:00", "#4F46E5")
    private val dummyTemplate = WeeklyTemplate("t1", "T", listOf(DayOfWeek.MONDAY), listOf(dummyZone))
    private val dummyOverride = TemplateOverride("o1", null, "2026-08-09", listOf(dummyZone))

    override suspend fun getZonesForDate(date: LocalDate): Result<List<DayZone>> = Result.Success(emptyList())
    override suspend fun getTemplates(): Result<List<WeeklyTemplate>> = Result.Success(listOf(dummyTemplate))
    override suspend fun createTemplate(name: String, daysOfWeek: List<DayOfWeek>, zones: List<DailyZone>): Result<WeeklyTemplate> = Result.Success(dummyTemplate)
    override suspend fun getTemplate(templateId: String): Result<WeeklyTemplate> = Result.Success(dummyTemplate)
    override suspend fun updateTemplate(templateId: String, name: String, daysOfWeek: List<DayOfWeek>): Result<WeeklyTemplate> = Result.Success(dummyTemplate)
    override suspend fun deleteTemplate(templateId: String): Result<Unit> = Result.Success(Unit)
    override suspend fun addZoneToTemplate(templateId: String, zone: DailyZone): Result<DailyZone> = Result.Success(dummyZone)
    override suspend fun getTemplateZones(templateId: String): Result<List<DailyZone>> = Result.Success(listOf(dummyZone))
    override suspend fun updateTemplateZones(templateId: String, zones: List<DailyZone>): Result<List<DailyZone>> = Result.Success(zones)
    override suspend fun createOverride(date: String, zones: List<DailyZone>): Result<TemplateOverride> = Result.Success(dummyOverride)
    override suspend fun getOverrides(): Result<List<TemplateOverride>> = Result.Success(listOf(dummyOverride))
    override suspend fun getOverride(overrideId: String): Result<TemplateOverride> = Result.Success(dummyOverride)
    override suspend fun updateOverride(overrideId: String, name: String?, date: String): Result<TemplateOverride> = Result.Success(dummyOverride)
    override suspend fun deleteOverride(overrideId: String): Result<Unit> = Result.Success(Unit)
    override suspend fun addZoneToOverride(overrideId: String, zone: DailyZone): Result<DailyZone> = Result.Success(dummyZone)
    override suspend fun getOverrideZones(overrideId: String): Result<List<DailyZone>> = Result.Success(listOf(dummyZone))
    override suspend fun updateOverrideZones(overrideId: String, zones: List<DailyZone>): Result<List<DailyZone>> = Result.Success(zones)
    override suspend fun getZone(zoneId: String): Result<DailyZone> = Result.Success(dummyZone)
    override suspend fun getZoneSessions(zoneId: String): Result<List<Session>> = Result.Success(emptyList())
    override suspend fun getEffectiveZones(date: String): Result<List<DailyZone>> = Result.Success(listOf(dummyZone))
    override suspend fun updateZone(zoneId: String, zone: DailyZone): Result<DailyZone> = Result.Success(zone)
    override suspend fun deleteZone(zoneId: String): Result<Unit> = Result.Success(Unit)
    override suspend fun refreshZones(): Result<Unit> = Result.Success(Unit)
}

private class FakeProfileRepository : ProfileRepository {
    private val profile = Profile(
        id = "user_1",
        email = "test@example.com",
        firstName = "Test",
        lastName = "User",
        birthDate = null,
        points = 0,
        streak = 0,
        maxStreak = 0,
        profilePictureUrl = null,
        isNew = false,
        preferences = null,
    )

    override fun observeProfile(): Flow<Profile?> = flowOf(profile)
    override suspend fun getProfile(): Result<Profile> = Result.Success(profile)
    override suspend fun updateName(firstName: String, lastName: String): Result<Profile> = Result.Success(profile)
    override suspend fun updateBirthDate(birthDate: String): Result<Profile> = Result.Success(profile)
    override suspend fun updateProfilePicture(imageBytes: ByteArray, mimeType: String): Result<Profile> = Result.Success(profile)
    override suspend fun deleteProfilePicture(): Result<Profile> = Result.Success(profile)
    override suspend fun updateProfilePartial(
        firstName: String?,
        lastName: String?,
        timezone: String?,
        preferredSessionDuration: Int?,
        bufferBetweenSessions: Int?,
        wakeupTime: String?,
        sleepTime: String?,
        schedulingType: String?,
    ): Result<Profile> = Result.Success(profile)
    override suspend fun updateTimezone(timezone: String): Result<Profile> = Result.Success(profile)
    override suspend fun updateSessionSettings(preferredSessionDuration: Int, bufferBetweenSessions: Int): Result<Profile> = Result.Success(profile)
    override suspend fun updateSleepSchedule(wakeupTime: String, sleepTime: String): Result<Profile> = Result.Success(profile)
    override suspend fun updateSchedulingType(schedulingType: String): Result<Profile> = Result.Success(profile)
}
