package com.awan.app.core.data.home.repository

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.home.mapper.HomeMapper
import com.awan.app.core.data.home.remote.HomeRemoteDataSource
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
import com.awan.app.core.model.SessionDetailInfo
import com.awan.app.core.model.SessionTaskDetail
import com.awan.app.core.model.TaskDetailInfo

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

    override suspend fun getSessionDetail(sessionId: String): Result<SessionTaskDetail> {
        val sessionResult = remoteDataSource.getSession(sessionId)
        if (sessionResult is Result.Error) {
            return Result.Error(sessionResult.error)
        }
        if (sessionResult !is Result.Success) {
            return Result.Error(AppError.Unknown(Throwable("Failed to fetch session")))
        }

        val sessionDto = sessionResult.data
        val taskId = sessionDto.taskId
        if (taskId.isNullOrBlank()) {
            return Result.Error(AppError.Unknown(Throwable("Session does not have a valid taskId")))
        }

        val taskResult = remoteDataSource.getTask(taskId)
        if (taskResult is Result.Error) {
            return Result.Error(taskResult.error)
        }
        if (taskResult !is Result.Success) {
            return Result.Error(AppError.Unknown(Throwable("Failed to fetch task")))
        }

        val taskDto = taskResult.data

        val sessionDetailInfo = SessionDetailInfo(
            id = sessionDto.id,
            start = sessionDto.start,
            end = sessionDto.end,
            status = sessionDto.status ?: "SCHEDULED",
            locked = sessionDto.locked,
            zoneId = sessionDto.zoneId,
            taskId = taskId,
        )

        val taskDetailInfo = TaskDetailInfo(
            id = taskDto.id,
            title = taskDto.title,
            description = taskDto.description,
            estimatedDuration = taskDto.estimatedDuration,
            status = taskDto.status ?: "SCHEDULED",
            mandatory = taskDto.mandatory ?: false,
            estimatedPoints = taskDto.estimatedPoints ?: 0,
            allowTaskSplitting = taskDto.allowTaskSplitting ?: false,
            goalId = taskDto.goalId,
            categoryName = taskDto.category?.name,
            dependsOnTaskIds = taskDto.dependsOnTaskIds ?: emptyList(),
        )

        return Result.Success(
            SessionTaskDetail(
                session = sessionDetailInfo,
                task = taskDetailInfo,
            )
        )
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
        locked: Boolean?,
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
            locked = locked,
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

    override suspend fun updateSessionLock(
        sessionId: String,
        locked: Boolean,
    ): Result<Unit> {
        val result = if (locked) {
            remoteDataSource.lockSession(sessionId)
        } else {
            remoteDataSource.unlockSession(sessionId)
        }
        return when (result) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(result.error)
            else -> Result.Error(AppError.Unknown(Throwable("Failed to update session lock state")))
        }
    }

    override suspend fun updateTaskDetails(
        taskId: String,
        title: String?,
        description: String?,
        estimatedDuration: Int?,
        estimatedPoints: Int?,
        mandatory: Boolean?,
        allowTaskSplitting: Boolean?,
    ): Result<Unit> {
        val request = com.awan.app.core.network.dto.task.TaskUpdateRequest(
            title = title,
            description = description,
            estimatedDuration = estimatedDuration,
            estimatedPoints = estimatedPoints,
            mandatory = mandatory,
            allowTaskSplitting = allowTaskSplitting,
        )
        val result = remoteDataSource.updateTask(taskId, request)
        return when (result) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(result.error)
            else -> Result.Error(AppError.Unknown(Throwable("Failed to update task details")))
        }
    }

    override suspend fun deleteSession(sessionId: String): Result<Unit> {
        val result = remoteDataSource.deleteSession(sessionId)
        return when (result) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(result.error)
            else -> Result.Error(AppError.Unknown(Throwable("Failed to delete session")))
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        val result = remoteDataSource.deleteTask(taskId, cascade = true)
        return when (result) {
            is Result.Success -> Result.Success(Unit)
            is Result.Error -> Result.Error(result.error)
            else -> Result.Error(AppError.Unknown(Throwable("Failed to delete task")))
        }
    }
}
