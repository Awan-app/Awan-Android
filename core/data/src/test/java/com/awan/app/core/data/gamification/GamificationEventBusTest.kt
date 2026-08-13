package com.awan.app.core.data.gamification

import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserPreferencesEntity
import com.awan.app.core.database.model.UserWithPreferences
import com.awan.app.core.domain.gamification.model.GamificationProgress
import com.awan.app.core.domain.gamification.model.PointsAward
import com.awan.app.core.domain.gamification.model.SessionReward
import com.awan.app.core.domain.gamification.model.StreakChange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

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
