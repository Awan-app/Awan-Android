package com.awan.app.core.data.calendar.local

import androidx.room.withTransaction
import com.awan.app.core.data.calendar.asCalendarGoal
import com.awan.app.core.data.calendar.asEntity
import com.awan.app.core.database.AwanDatabase
import com.awan.app.core.database.dao.GoalDao
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.model.GoalEntity
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserPreferencesEntity
import com.awan.app.core.domain.calendar.repository.CalendarSnapshot
import com.awan.app.core.network.dto.GoalResponse
import com.awan.app.core.network.dto.UserProfileResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

interface CalendarLocalDataSource {
    fun observeCalendar(userId: String): Flow<CalendarSnapshot?>
    suspend fun upsertCalendar(profile: UserProfileResponse, goals: List<GoalResponse>)
}

@Singleton
class CalendarLocalDataSourceImpl @Inject constructor(
    private val database: AwanDatabase,
    private val userDao: UserDao,
    private val goalDao: GoalDao,
) : CalendarLocalDataSource {
    override fun observeCalendar(userId: String): Flow<CalendarSnapshot?> = combine(
        userDao.observeUser(userId),
        userDao.observePreferences(userId),
        goalDao.observeGoalsByStatus("ACTIVE"),
    ) { user, preferences, goals ->
        user?.let {
            CalendarSnapshot(
                user = com.awan.app.core.model.CalendarUser(it.id, it.streak, preferences?.timezone.orEmpty()),
                goals = goals.filterNot(GoalEntity::isInbox).map(GoalEntity::asCalendarGoal),
            )
        }
    }

    override suspend fun upsertCalendar(profile: UserProfileResponse, goals: List<GoalResponse>) =
        database.withTransaction {
            userDao.upsertUserWithPreferences(
                UserEntity(profile.id, profile.email, profile.firstName, profile.lastName, profile.birthDate, profile.points, profile.streak, profile.maxStreak),
                UserPreferencesEntity(profile.id, profile.preferences?.timezone.orEmpty(), profile.preferences?.preferredSessionDuration ?: 0, profile.preferences?.bufferBetweenSessions ?: 0, profile.preferences?.wakeupTime.orEmpty(), profile.preferences?.sleepTime.orEmpty(), profile.preferences?.schedulingType ?: "BALANCED"),
            )
            goalDao.upsertGoals(goals.map(GoalResponse::asEntity))
        }
}
