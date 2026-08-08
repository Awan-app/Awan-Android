package com.awan.app.core.data.home

import com.awan.app.core.data.gamification.GamificationEventBus
import com.awan.app.core.data.home.remote.HomeRemoteDataSource
import com.awan.app.core.data.home.repository.HomeRepositoryImpl
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserPreferencesEntity
import com.awan.app.core.database.model.UserWithPreferences
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.gamification.model.RewardEvent
import com.awan.app.core.network.dto.gamification.PointsRewardDto
import com.awan.app.core.network.dto.gamification.RewardDto
import com.awan.app.core.network.dto.gamification.StreakRewardDto
import com.awan.app.core.network.dto.onboarding.CompleteOnboardingResponse
import com.awan.app.core.network.dto.session.CompleteSessionResponse
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.task.TaskWithSessionsDto
import com.awan.app.core.network.dto.zone.TemplateOverrideDto
import com.awan.app.core.network.dto.zone.WeeklyTemplateDto
import com.awan.app.core.network.dto.zone.ZoneDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeRepositoryImplTest {

    private val session = SessionDto(id = SESSION_ID, start = START, end = END, status = "COMPLETED")

    private class FakeRemoteDataSource : HomeRemoteDataSource {
        var reward: RewardDto? = null
        var completeResult: Result<CompleteSessionResponse>? = null
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
            return Result.Success(SessionDto(id = sessionId, start = START, end = END, status = "SCHEDULED"))
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

        override suspend fun getZonesByDate(date: String): Result<List<ZoneDto>> = notNeeded()
        override suspend fun getTasksByDate(date: String): Result<List<TaskWithSessionsDto>> = notNeeded()
        override suspend fun getTemplates(): Result<List<WeeklyTemplateDto>> = notNeeded()
        override suspend fun getTemplateOverrides(): Result<List<TemplateOverrideDto>> = notNeeded()
        override suspend fun getUserProfile(): Result<CompleteOnboardingResponse> = notNeeded()
    }

    /**
     * Only [UserDao.getFirstUser] and [UserDao.upsertUser] sit on the profile path, and none of the
     * session paths touch the DAO at all — so everything else is deliberately unimplemented rather
     * than faked into looking covered.
     */
    private class FakeUserDao : UserDao {
        override suspend fun getFirstUser(): UserEntity? = null
        override suspend fun upsertUser(user: UserEntity) = Unit
        override fun observeUser(userId: String): Flow<UserEntity?> = notNeeded()
        override suspend fun getUser(userId: String): UserEntity? = notNeeded()
        override suspend fun deleteUser(userId: String) = notNeeded()
        override suspend fun upsertPreferences(preferences: UserPreferencesEntity) = notNeeded()
        override fun observePreferences(userId: String): Flow<UserPreferencesEntity?> = notNeeded()
        override suspend fun getPreferences(userId: String): UserPreferencesEntity? = notNeeded()
        override fun observeUserWithPreferences(userId: String): Flow<UserWithPreferences?> = notNeeded()
        override suspend fun getUserWithPreferences(userId: String): UserWithPreferences? = notNeeded()
        override suspend fun upsertUserWithPreferences(
            user: UserEntity,
            preferences: UserPreferencesEntity,
        ) = notNeeded()
    }

    private fun repository(
        remote: FakeRemoteDataSource,
        bus: GamificationEventBus,
    ) = HomeRepositoryImpl(
        remoteDataSource = remote,
        userDao = FakeUserDao(),
        eventBus = bus,
    )

    /** Collects on an unconfined dispatcher so emissions land before the assertions run. */
    private fun runCollecting(
        block: suspend (FakeRemoteDataSource, GamificationEventBus, List<RewardEvent>) -> Unit,
    ) = runTest {
        val remote = FakeRemoteDataSource()
        val bus = GamificationEventBus()
        val collected = mutableListOf<RewardEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            bus.rewards.collect(collected::add)
        }
        block(remote, bus, collected)
        job.cancel()
    }

    @Test
    fun `completing publishes points then streak, in that order`() = runCollecting { remote, bus, events ->
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

        val result = repository(remote, bus).completeSession(SESSION_ID)

        assertTrue(result is Result.Success)
        assertEquals(2, events.size)
        assertEquals(RewardEvent.Points(amount = 25, newTotal = 175), events[0])
        assertTrue(events[1] is RewardEvent.Streak)
        assertEquals(true, (events[1] as RewardEvent.Streak).maxStreakBroken)
        assertEquals(7, (events[1] as RewardEvent.Streak).maxStreakNew)
    }

    @Test
    fun `re-completing a session publishes nothing and returns an empty reward`() =
        runCollecting { remote, bus, events ->
            remote.reward = RewardDto(
                points = PointsRewardDto(awarded = false, amount = 25, oldValue = 150, newValue = 175),
                streak = StreakRewardDto(updated = false, oldValue = 5, newValue = 6),
            )

            val result = repository(remote, bus).completeSession(SESSION_ID)

            assertTrue(events.isEmpty())
            assertTrue((result as Result.Success).data.isEmpty)
        }

    @Test
    fun `a completion with no reward block publishes nothing`() = runCollecting { remote, bus, events ->
        remote.reward = null

        val result = repository(remote, bus).completeSession(SESSION_ID)

        assertTrue(events.isEmpty())
        assertTrue((result as Result.Success).data.isEmpty)
    }

    @Test
    fun `uncompleting publishes nothing - points are never taken back`() =
        runCollecting { remote, bus, events ->
            val result = repository(remote, bus).uncompleteSession(SESSION_ID)

            assertTrue(result is Result.Success)
            assertEquals(1, remote.uncompleteCalls)
            assertTrue(events.isEmpty())
        }

    @Test
    fun `moving a session publishes nothing and never sends a status`() =
        runCollecting { remote, bus, events ->
            val result = repository(remote, bus).moveSession(SESSION_ID, START, END)

            assertTrue(result is Result.Success)
            assertEquals(1, remote.moveCalls)
            assertTrue(events.isEmpty())
        }

    @Test
    fun `a failed completion publishes nothing`() = runCollecting { remote, bus, events ->
        remote.completeResult = Result.Error(AppError.Network)

        val result = repository(remote, bus).completeSession(SESSION_ID)

        assertTrue(result is Result.Error)
        assertTrue(events.isEmpty())
        assertEquals(0, bus.progress.value.points)
    }

    private companion object {
        const val SESSION_ID = "session-1"
        const val START = "2026-08-08T09:00:00"
        const val END = "2026-08-08T10:00:00"

        fun notNeeded(): Nothing = error("not exercised by these tests")
    }
}
