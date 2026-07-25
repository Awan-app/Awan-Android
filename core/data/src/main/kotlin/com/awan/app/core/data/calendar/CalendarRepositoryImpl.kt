package com.awan.app.core.data.calendar

import com.awan.app.core.common.result.Result
import com.awan.app.core.data.calendar.remote.CalendarRemoteDataSource
import com.awan.app.core.database.dao.GoalDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.model.GoalEntity
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserPreferencesEntity
import com.awan.app.core.datastore.auth.AuthTokenProvider
import com.awan.app.core.model.CalendarUser
import com.awan.app.core.model.Goal
import com.awan.app.core.network.dto.GoalResponse
import com.awan.app.core.network.dto.UserProfileResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepositoryImpl @Inject constructor(
    private val remote: CalendarRemoteDataSource,
    private val authTokenProvider: AuthTokenProvider,
    private val userDao: UserDao,
    private val goalDao: GoalDao,
    private val taskDao: TaskDao,
) : CalendarRepository {
    override fun observeCalendar(): Flow<CalendarSnapshot?> = flow {
        val userId = authTokenProvider.getUserId()
        if (userId == null) {
            emit(null)
            return@flow
        }
        emitAll(combine(
            userDao.observeUser(userId),
            userDao.observePreferences(userId),
            goalDao.observeGoalsByStatus("ACTIVE"),
        ) { user, preferences, goals ->
            user?.let {
                CalendarSnapshot(
                    CalendarUser(it.id, it.streak, preferences?.timezone.orEmpty()),
                    goals.filterNot(GoalEntity::isInbox).map(GoalEntity::asCalendarGoal),
                )
            }
        })
    }

    override suspend fun refresh(): Result<Unit> {
        val profile = remote.getUserProfile()
        if (profile is Result.Error) return profile
        val goals = remote.getActiveGoals()
        if (goals is Result.Error) return goals
        persistUser((profile as Result.Success).data)
        replaceActiveGoals((goals as Result.Success).data)
        return Result.Success(Unit)
    }

    private suspend fun persistUser(profile: UserProfileResponse) {
        userDao.upsertUserWithPreferences(
            UserEntity(profile.id, profile.email, profile.firstName, profile.lastName, profile.birthDate, profile.points, profile.streak, profile.maxStreak),
            UserPreferencesEntity(profile.id, profile.preferences?.timezone.orEmpty(), profile.preferences?.preferredSessionDuration ?: 0, profile.preferences?.bufferBetweenSessions ?: 0, profile.preferences?.wakeupTime.orEmpty(), profile.preferences?.sleepTime.orEmpty(), profile.preferences?.schedulingType ?: "BALANCED"),
        )
    }

    private suspend fun replaceActiveGoals(remoteGoals: List<GoalResponse>) {
        val freshIds = remoteGoals.mapTo(hashSetOf()) { it.id }
        goalDao.getActiveNonInboxGoalIds().filterNot(freshIds::contains).forEach { id ->
            taskDao.deleteTasksByGoal(id)
            goalDao.deleteGoal(id)
        }
        goalDao.upsertGoals(remoteGoals.map(GoalResponse::asEntity))
    }
}

private fun GoalResponse.asEntity() = GoalEntity(id, title, description, status, targetDate, createdAt, inbox)
private fun GoalEntity.asCalendarGoal() = Goal(id, title, targetDate, status, isInbox)


