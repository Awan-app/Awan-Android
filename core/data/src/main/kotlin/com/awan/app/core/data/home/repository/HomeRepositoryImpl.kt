package com.awan.app.core.data.home.repository

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.database.dao.CategoryDao
import com.awan.app.core.database.dao.SessionDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.dao.TemplateDao
import com.awan.app.core.database.dao.TemplateOverrideDao
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.dao.ZoneDao
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.data.home.remote.HomeRemoteDataSource
import com.awan.app.core.domain.home.model.DaySchedule
import com.awan.app.core.domain.home.model.DaySession
import com.awan.app.core.domain.home.model.DayZone
import com.awan.app.core.domain.home.model.SessionStatus
import com.awan.app.core.domain.home.model.UserProfileInfo
import com.awan.app.core.domain.home.repository.HomeRepository
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import android.util.Log
import com.awan.app.core.data.common.extractTimeFromIso
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import com.awan.app.core.model.SessionDetailInfo
import com.awan.app.core.model.SessionTaskDetail
import com.awan.app.core.model.TaskDetailInfo

@Singleton
class HomeRepositoryImpl @Inject constructor(
    private val remoteDataSource: HomeRemoteDataSource,
    private val userDao: UserDao,
    private val taskDao: TaskDao,
    private val sessionDao: SessionDao,
    private val zoneDao: ZoneDao,
    private val templateDao: TemplateDao,
    private val templateOverrideDao: TemplateOverrideDao,
    private val categoryDao: CategoryDao,
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
                        profilePictureUrl = dto.profilePictureUrl,
                        isNew = dto.isNew ?: false,
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
            Result.Error(AppError.NotFound)
        }
    }

    override fun getDaySchedule(date: LocalDate): Flow<Result<DaySchedule>> {
        val dateStr = date.toString()

        return sessionDao.observeSessionsForDate(dateStr)
            .map { sessionEntities ->
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
                        zones = resolveZonesForDate(dateStr, date),
                        sessions = daySessions,
                    )
                )
            }
            .flowOn(ioDispatcher)
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

    override suspend fun updateSessionStatus(
        sessionId: String,
        status: SessionStatus,
        locked: Boolean?,
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
            locked = locked,
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
                        startTime = extractTimeFromIso(dto.start, existing.startTime),
                        endTime = extractTimeFromIso(dto.end, existing.endTime),
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
        } catch (e: Exception) {
            Log.w("HomeRepositoryImpl", "Failed to parse local time: $timeStr", e)
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

    private suspend fun resolveZonesForDate(dateStr: String, date: LocalDate): List<DayZone> {
        val override = templateOverrideDao.getOverrideForDate(dateStr)
        val zones = if (override != null) {
            zoneDao.observeZonesForOverride(override.id).first()
        } else {
            val dayOfWeek = date.dayOfWeek.name.uppercase()
            val templateAssignment = templateDao.getDayAssignment(dayOfWeek)
            if (templateAssignment != null) {
                zoneDao.observeZonesForTemplate(templateAssignment.templateId).first()
            } else {
                emptyList()
            }
        }
        
        return zones.map { entity ->
            val startLocalTime = parseLocalTime(entity.startTime)
            val endLocalTime = parseLocalTime(entity.endTime)
            val startMinutes = startLocalTime.hour * 60 + startLocalTime.minute
            val endMinutes = endLocalTime.hour * 60 + endLocalTime.minute
            DayZone(
                id = entity.id,
                name = entity.name,
                categoryId = "",
                categoryName = "",
                startMinutes = startMinutes,
                endMinutes = endMinutes,
                color = entity.color
            )
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
