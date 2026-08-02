package com.awan.app.core.data.home.repository

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.home.mapper.HomeMapper
import com.awan.app.core.data.home.remote.HomeRemoteDataSource
import com.awan.app.core.domain.home.model.DaySchedule
import com.awan.app.core.domain.home.repository.HomeRepository
import com.awan.app.core.network.dto.ZoneDto
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.domain.home.model.SessionStatus
import com.awan.app.core.domain.home.model.UserProfileInfo

@Singleton
class HomeRepositoryImpl @Inject constructor(
    private val remoteDataSource: HomeRemoteDataSource,
    private val userDao: UserDao,
) : HomeRepository {

    private val scheduleCache = java.util.concurrent.ConcurrentHashMap<LocalDate, DaySchedule>()

    override suspend fun getUserProfile(): Result<UserProfileInfo> {
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
            return Result.Success(
                UserProfileInfo(
                    id = dto.id,
                    firstName = firstName,
                    lastName = lastName,
                    points = points,
                    streak = streak,
                )
            )
        }

        val cachedUser = userDao.getFirstUser()
        return if (cachedUser != null) {
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
            Result.Error((result as? Result.Error)?.error ?: AppError.Unknown())
        }
    }

    override suspend fun getDaySchedule(date: LocalDate): Result<DaySchedule> {
        val dateStr = date.toString()
        scheduleCache.remove(date)

        val zonesResult = remoteDataSource.getZonesByDate(dateStr)
        val zones: List<ZoneDto> = when (zonesResult) {
            is Result.Success -> zonesResult.data
            is Result.Error -> {
                if (zonesResult.error !is AppError.Server) {
                    scheduleCache[date]?.let { return Result.Success(it) }
                    return zonesResult
                }
                emptyList()
            }
            is Result.Loading -> emptyList()
        }

        val effectiveZones = if (zones.isEmpty()) {
            resolveZoneFallback(date, dateStr)
        } else {
            zones
        }

        val tasksResult = remoteDataSource.getTasksByDate(dateStr)
        if (tasksResult is Result.Error) {
            scheduleCache[date]?.let { return Result.Success(it) }
            return tasksResult
        }

        val tasks = (tasksResult as Result.Success).data
        val schedule = HomeMapper.toDaySchedule(date, effectiveZones, tasks)
        scheduleCache[date] = schedule
        return Result.Success(schedule)
    }


    @Suppress("NewApi")
    private suspend fun resolveZoneFallback(date: LocalDate, dateStr: String): List<ZoneDto> {
        val overridesResult = remoteDataSource.getTemplateOverrides()
        if (overridesResult is Result.Success) {
            val matchingOverride = overridesResult.data.firstOrNull { it.dateOfDay == dateStr }
            if (matchingOverride != null && matchingOverride.zones.isNotEmpty()) {
                return matchingOverride.zones
            }
        }

        val templatesResult = remoteDataSource.getTemplates()
        if (templatesResult is Result.Success) {
            val dayOfWeek = date.dayOfWeek.name
            val matchingTemplate = templatesResult.data.firstOrNull { template ->
                template.daysOfWeek.any { it.uppercase() == dayOfWeek }
            }
            if (matchingTemplate != null) return matchingTemplate.zones
        }

        return emptyList()
    }

    override suspend fun updateSessionStatus(
        sessionId: String,
        status: SessionStatus,
        startIso: String?,
        endIso: String?,
    ): Result<Unit> {
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
        return when (result) {
            is Result.Success -> {
                scheduleCache.forEach { (date, cachedSchedule) ->
                    if (cachedSchedule.sessions.any { it.id == sessionId }) {
                        val updatedSessions = cachedSchedule.sessions.map { session ->
                            if (session.id == sessionId) session.copy(status = status) else session
                        }
                        scheduleCache[date] = cachedSchedule.copy(sessions = updatedSessions)
                    }
                }
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(result.error)
            else -> Result.Error(AppError.Unknown(Throwable("Failed to update session status")))
        }
    }
}
