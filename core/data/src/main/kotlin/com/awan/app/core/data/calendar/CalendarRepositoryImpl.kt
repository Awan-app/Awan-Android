package com.awan.app.core.data.calendar

import android.database.SQLException
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.calendar.local.CalendarLocalDataSource
import com.awan.app.core.data.calendar.remote.CalendarRemoteDataSource
import com.awan.app.core.database.model.GoalEntity
import com.awan.app.core.datastore.auth.AuthTokenProvider
import com.awan.app.core.domain.calendar.repository.CalendarRepository
import com.awan.app.core.domain.calendar.repository.CalendarSnapshot
import com.awan.app.core.model.Goal
import com.awan.app.core.network.dto.GoalResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepositoryImpl @Inject constructor(
    private val remote: CalendarRemoteDataSource,
    private val authTokenProvider: AuthTokenProvider,
    private val local: CalendarLocalDataSource,
) : CalendarRepository {
    override fun observeCalendar(): Flow<CalendarSnapshot?> = flow {
        val userId = authTokenProvider.getUserId()
        if (userId == null) {
            emit(null)
            return@flow
        }
        emitAll(local.observeCalendar(userId))
    }

    override suspend fun refresh(): Result<Unit> {
        val profile = remote.getUserProfile()
        if (profile is Result.Error) return profile
        val goals = remote.getActiveGoals()
        if (goals is Result.Error) return goals
        return try {
            local.upsertCalendar(
                profile = (profile as Result.Success).data,
                goals = (goals as Result.Success).data,
            )
            Result.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLException) {
            Result.Error(AppError.Unknown(e))
        }
    }
}

internal fun GoalResponse.asEntity() = GoalEntity(id, title, description, status, targetDate, createdAt, inbox)
internal fun GoalEntity.asCalendarGoal() = Goal(id, title, targetDate, status, isInbox)

