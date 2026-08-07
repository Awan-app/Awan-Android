package com.awan.app.core.data.home.repository

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.database.dao.CachedScheduleDateDao
import com.awan.app.core.database.dao.CategoryDao
import com.awan.app.core.database.dao.SessionDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.dao.ZoneDao
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.data.home.remote.HomeRemoteDataSource
import com.awan.app.core.data.sync.OfflineSyncCoordinator
import com.awan.app.core.domain.home.model.DaySchedule
import com.awan.app.core.domain.home.model.DaySession
import com.awan.app.core.domain.home.model.SessionStatus
import com.awan.app.core.domain.home.model.UserProfileInfo
import com.awan.app.core.domain.home.repository.HomeRepository
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepositoryImpl @Inject constructor(
    private val remoteDataSource: HomeRemoteDataSource,
    private val userDao: UserDao,
    private val taskDao: TaskDao,
    private val sessionDao: SessionDao,
    private val zoneDao: ZoneDao,
    private val categoryDao: CategoryDao,
    private val cachedScheduleDateDao: CachedScheduleDateDao,
    private val offlineSyncCoordinator: OfflineSyncCoordinator,
    private val connectivityMonitor: NetworkConnectivityMonitor,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : HomeRepository {

    override suspend fun getUserProfile(): Result<UserProfileInfo> = withContext(ioDispatcher) {
        if (connectivityMonitor.isCurrentlyOnline()) {
            val result = remoteDataSource.getUserProfile()
            if (result is Result.Success) {
                val dto = result.data
                val firstName = dto.firstName ?: "User"
                val lastName = dto.lastName ?: ""
                val points = dto.points ?: 0
                val streak = dto.streak ?: 0
                userDao.upsertUser(
                    UserEntity(
                        id = dto.id,
                        email = dto.email ?: "",
                        firstName = firstName,
                        lastName = lastName,
                        birthDate = dto.birthDate,
                        points = points,
                        streak = streak,
                        maxStreak = dto.maxStreak ?: 0,
                    )
                )
                return@withContext Result.Success(
                    UserProfileInfo(
                        id = dto.id,
                        firstName = firstName,
                        lastName = lastName,
                        points = points,
                        streak = streak,
                    )
                )
            }
        }

        val cachedUser = userDao.getFirstUser()
        if (cachedUser != null) {
            Result.Success(
                UserProfileInfo(
                    id = cachedUser.id,
                    firstName = cachedUser.firstName ?: "User",
                    lastName = cachedUser.lastName ?: "",
                    points = cachedUser.points,
                    streak = cachedUser.streak,
                )
            )
        } else {
            Result.Error(AppError.Network)
        }
    }

    override suspend fun getDaySchedule(date: LocalDate): Result<DaySchedule> = withContext(ioDispatcher) {
        val dateStr = date.toString()

        val isCached = cachedScheduleDateDao.isDateCached(dateStr)
        if (!isCached && connectivityMonitor.isCurrentlyOnline()) {
            offlineSyncCoordinator.syncScheduleRange(date, date)
        }

        val sessionEntities = sessionDao.getSessionsForDate(dateStr)


        val daySessions = sessionEntities.mapNotNull { s ->
            val task = taskDao.getTask(s.taskId) ?: return@mapNotNull null
            val category = task.categoryId?.let { categoryDao.getCategory(it) }
            val startLocalTime = parseLocalTime(s.startTime)
            val endLocalTime = parseLocalTime(s.endTime)
            val startMinutes = startLocalTime.hour * 60 + startLocalTime.minute
            val durationMinutes = run {
                val endMinutes = endLocalTime.hour * 60 + endLocalTime.minute
                if (endMinutes > startMinutes) endMinutes - startMinutes else 0
            }

            DaySession(
                id = s.id,
                taskId = task.id,
                taskTitle = task.title,
                zoneId = s.zoneId,
                startMinutes = startMinutes,
                durationMinutes = durationMinutes,
                status = mapStatus(s.status),
                locked = s.locked,
                points = task.estimatedPoints,
                categoryId = task.categoryId,
                categoryName = category?.name,
            )
        }

        Result.Success(
            DaySchedule(
                date = date,
                zones = emptyList(),
                sessions = daySessions,
            )
        )
    }

    override suspend fun updateSessionStatus(
        sessionId: String,
        status: SessionStatus,
        startIso: String?,
        endIso: String?,
    ): Result<Unit> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
        val statusString = when (status) {
            SessionStatus.COMPLETED -> "COMPLETED"
            SessionStatus.IN_PROGRESS -> "IN_PROGRESS"
            SessionStatus.CANCELLED -> "CANCELLED"
            SessionStatus.SCHEDULED -> "SCHEDULED"
        }
        val result = remoteDataSource.updateSession(
            sessionId = sessionId,
            status = statusString,
            startIso = startIso,
            endIso = endIso,
        )
        if (result is Result.Success) {
            val dto = result.data
            val existing = sessionDao.getSession(sessionId)
            if (existing != null) {
                sessionDao.upsertSession(
                    existing.copy(
                        status = dto.status ?: statusString,
                        startTime = if (dto.start.length >= 19) dto.start.substring(11, 19) else existing.startTime,
                        endTime = if (dto.end.length >= 19) dto.end.substring(11, 19) else existing.endTime,
                        locked = dto.locked,
                    )
                )
            }
            Result.Success(Unit)
        } else {
            Result.Error((result as Result.Error).error)
        }
    }

    private fun parseLocalTime(timeStr: String): LocalTime {
        return try {
            LocalTime.parse(timeStr)
        } catch (_: Exception) {
            LocalTime.of(0, 0)
        }
    }

    private fun mapStatus(statusStr: String): SessionStatus {
        return when (statusStr.uppercase()) {
            "COMPLETED" -> SessionStatus.COMPLETED
            "IN_PROGRESS" -> SessionStatus.IN_PROGRESS
            "CANCELLED" -> SessionStatus.CANCELLED
            else -> SessionStatus.SCHEDULED
        }
    }
}
