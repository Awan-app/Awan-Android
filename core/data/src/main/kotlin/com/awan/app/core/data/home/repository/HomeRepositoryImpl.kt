package com.awan.app.core.data.home.repository

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.gamification.GamificationEventBus
import com.awan.app.core.data.gamification.mapper.toDomain
import com.awan.app.core.data.home.mapper.HomeMapper
import com.awan.app.core.data.home.remote.HomeRemoteDataSource
import com.awan.app.core.domain.gamification.model.SessionReward
import com.awan.app.core.domain.home.model.DaySchedule
import com.awan.app.core.domain.home.repository.HomeRepository
import com.awan.app.core.network.dto.zone.ZoneDto
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
    private val eventBus: GamificationEventBus,
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
                    profilePictureUrl = dto.profilePictureUrl,
                    isNew = dto.isNew ?: false,
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

    override suspend fun completeSession(sessionId: String): Result<SessionReward> =
        when (val result = remoteDataSource.completeSession(sessionId)) {
            is Result.Success -> {
                patchCachedStatus(sessionId, SessionStatus.COMPLETED)
                val reward = result.data.reward.toDomain()
                // Published here rather than from the caller so any future path that completes a
                // session celebrates identically, without each one remembering to.
                eventBus.publishSessionReward(reward)
                Result.Success(reward)
            }
            is Result.Error -> Result.Error(result.error)
            Result.Loading -> unexpectedLoading()
        }

    override suspend fun uncompleteSession(sessionId: String): Result<Unit> =
        // Nothing is published: undoing a completion does not take the points back, so there is no
        // change to celebrate and reversing the animation would misrepresent the balance.
        patchOnSuccess(remoteDataSource.uncompleteSession(sessionId), sessionId, SessionStatus.SCHEDULED)

    override suspend fun cancelSession(sessionId: String): Result<Unit> =
        patchOnSuccess(remoteDataSource.cancelSession(sessionId), sessionId, SessionStatus.CANCELLED)

    override suspend fun moveSession(
        sessionId: String,
        startIso: String,
        endIso: String,
    ): Result<Unit> =
        when (val result = remoteDataSource.moveSession(sessionId, startIso, endIso)) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(result.error)
            Result.Loading -> unexpectedLoading()
        }

    private fun patchOnSuccess(
        result: Result<*>,
        sessionId: String,
        status: SessionStatus,
    ): Result<Unit> = when (result) {
        is Result.Success -> {
            patchCachedStatus(sessionId, status)
            Result.Success(Unit)
        }
        is Result.Error -> Result.Error(result.error)
        Result.Loading -> unexpectedLoading()
    }

    /** Keeps the day's cached copy in step so a re-read does not resurrect the old status. */
    private fun patchCachedStatus(sessionId: String, status: SessionStatus) {
        scheduleCache.forEach { (date, cachedSchedule) ->
            if (cachedSchedule.sessions.any { it.id == sessionId }) {
                val updatedSessions = cachedSchedule.sessions.map { session ->
                    if (session.id == sessionId) session.copy(status = status) else session
                }
                scheduleCache[date] = cachedSchedule.copy(sessions = updatedSessions)
            }
        }
    }

    private fun unexpectedLoading(): Result.Error =
        Result.Error(AppError.Unknown(Throwable("Session call returned Loading")))
}
