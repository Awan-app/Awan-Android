package com.awan.app.core.data.gamification

import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserPreferencesEntity
import com.awan.app.core.database.model.UserWithPreferences
import com.awan.app.core.domain.gamification.model.GamificationProgress
import com.awan.app.core.domain.gamification.model.PointsAward
import com.awan.app.core.domain.gamification.model.RewardEvent
import com.awan.app.core.domain.gamification.model.RewardSource
import com.awan.app.core.domain.gamification.model.SessionReward
import com.awan.app.core.domain.gamification.model.StreakChange
import com.awan.app.core.domain.gamification.model.WheelSpinResult
import com.awan.app.core.domain.gamification.model.WonItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GamificationEventBusTest {

    @Test
    fun `updatePoints atomically updates state and caches snapshot`() = runTest {
        val fakeDao = FakeUserDao()
        fakeDao.upsertUser(
            UserEntity(
                id = "u1",
                email = "test@awan.app",
                firstName = "Test",
                lastName = "User",
                birthDate = null,
                points = 100,
                streak = 2,
                maxStreak = 5,
            )
        )

        val eventBus = GamificationEventBus(fakeDao)
        eventBus.updatePoints(250)

        assertEquals(250, eventBus.progress.value.points)
        assertEquals(250, fakeDao.user?.points)
    }

    @Test
    fun `publishSessionReward updates progress atomically and caches snapshot`() = runTest {
        val fakeDao = FakeUserDao()
        fakeDao.upsertUser(
            UserEntity(
                id = "u1",
                email = "test@awan.app",
                firstName = "Test",
                lastName = "User",
                birthDate = null,
                points = 100,
                streak = 2,
                maxStreak = 5,
            )
        )

        val eventBus = GamificationEventBus(fakeDao)
        val reward = SessionReward(
            points = PointsAward(amount = 50, oldValue = 100, newValue = 150),
            streak = StreakChange(oldValue = 2, newValue = 3, maxStreakBroken = false, maxStreakNew = 5),
        )

        eventBus.publishSessionReward(reward)

        val progress = eventBus.progress.value
        assertEquals(150, progress.points)
        assertEquals(3, progress.streak)
        assertEquals(150, fakeDao.user?.points)
        assertEquals(3, fakeDao.user?.streak)
    }

    @Test
    fun `publishSessionReward emits points event`() = runTest {
        val fakeDao = FakeUserDao()
        val eventBus = GamificationEventBus(fakeDao)
        val collected = mutableListOf<RewardEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            eventBus.rewards.collect { collected.add(it) }
        }

        val reward = SessionReward(
            points = PointsAward(amount = 25, oldValue = 100, newValue = 125),
            streak = null,
        )
        eventBus.publishSessionReward(reward)

        assertEquals(1, collected.size)
        val event = collected.first() as RewardEvent.Points
        assertEquals(25, event.amount)
        assertEquals(125, event.newTotal)
        assertEquals(RewardSource.SESSION_COMPLETION, event.source)
        job.cancel()
    }

    @Test
    fun `empty reward emits nothing`() = runTest {
        val fakeDao = FakeUserDao()
        val eventBus = GamificationEventBus(fakeDao)
        val collected = mutableListOf<RewardEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            eventBus.rewards.collect { collected.add(it) }
        }

        val reward = SessionReward(points = null, streak = null)
        eventBus.publishSessionReward(reward)

        assertTrue(collected.isEmpty())
        job.cancel()
    }

    @Test
    fun `publishSessionReward emits points before streak ordering`() = runTest {
        val fakeDao = FakeUserDao()
        val eventBus = GamificationEventBus(fakeDao)
        val collected = mutableListOf<RewardEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            eventBus.rewards.collect { collected.add(it) }
        }

        val reward = SessionReward(
            points = PointsAward(amount = 50, oldValue = 100, newValue = 150),
            streak = StreakChange(oldValue = 2, newValue = 3, maxStreakBroken = true, maxStreakNew = 5),
        )
        eventBus.publishSessionReward(reward)

        assertEquals(2, collected.size)
        assertTrue(collected[0] is RewardEvent.Points)
        assertEquals(RewardSource.SESSION_COMPLETION, (collected[0] as RewardEvent.Points).source)
        assertTrue(collected[1] is RewardEvent.Streak)
        job.cancel()
    }

    @Test
    fun `publishWheelSpin with coins emits points with source DAILY_WHEEL`() = runTest {
        val fakeDao = FakeUserDao()
        val eventBus = GamificationEventBus(fakeDao)
        val collected = mutableListOf<RewardEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            eventBus.rewards.collect { collected.add(it) }
        }

        val spinResult = WheelSpinResult(
            segmentId = "seg1",
            coins = 100,
            item = null,
            newBalance = 500,
        )
        eventBus.publishWheelSpin(spinResult)

        assertEquals(1, collected.size)
        val event = collected.first() as RewardEvent.Points
        assertEquals(100, event.amount)
        assertEquals(500, event.newTotal)
        assertEquals(RewardSource.DAILY_WHEEL, event.source)
        assertEquals(500, eventBus.progress.value.points)
        job.cancel()
    }

    @Test
    fun `publishWheelSpin with item emits item event`() = runTest {
        val fakeDao = FakeUserDao()
        val eventBus = GamificationEventBus(fakeDao)
        val collected = mutableListOf<RewardEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            eventBus.rewards.collect { collected.add(it) }
        }

        val item = WonItem(id = "item1", name = "Hat", imageUrl = "http://image.png")
        val spinResult = WheelSpinResult(
            segmentId = "seg2",
            coins = 0,
            item = item,
            newBalance = 400,
        )
        eventBus.publishWheelSpin(spinResult)

        assertEquals(1, collected.size)
        val event = collected.first() as RewardEvent.Item
        assertEquals("Hat", event.name)
        assertEquals("http://image.png", event.imageUrl)
        job.cancel()
    }

    @Test
    fun `seedProgressIfEmpty does not undo a fresher award`() = runTest {
        val fakeDao = FakeUserDao()
        val eventBus = GamificationEventBus(fakeDao)

        eventBus.updatePoints(200)

        val staleSeed = GamificationProgress(points = 50, streak = 1, maxStreak = 1)
        eventBus.seedProgressIfEmpty(staleSeed)

        assertEquals(200, eventBus.progress.value.points)
    }

    private class FakeUserDao : UserDao {
        var user: UserEntity? = null
        var preferences: UserPreferencesEntity? = null

        override suspend fun upsertUser(user: UserEntity) { this.user = user }
        override fun observeUser(userId: String): Flow<UserEntity?> = flowOf(user)
        override suspend fun getUser(userId: String): UserEntity? = user
        override suspend fun getFirstUser(): UserEntity? = user
        override suspend fun deleteUser(userId: String) { this.user = null }
        override suspend fun getMinExpiryTime(): Long? = null
        override suspend fun upsertPreferences(preferences: UserPreferencesEntity) { this.preferences = preferences }
        override fun observePreferences(userId: String): Flow<UserPreferencesEntity?> = flowOf(preferences)
        override suspend fun getPreferences(userId: String): UserPreferencesEntity? = preferences
        override fun observeUserWithPreferences(userId: String): Flow<UserWithPreferences?> = flowOf(null)
        override suspend fun getUserWithPreferences(userId: String): UserWithPreferences? = null
    }
}
