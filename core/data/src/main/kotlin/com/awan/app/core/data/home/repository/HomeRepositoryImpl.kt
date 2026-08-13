package com.awan.app.core.data.home.repository

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.gamification.GamificationEventBus
import com.awan.app.core.data.gamification.mapper.toDomain
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.ZoneEntity
import com.awan.app.core.data.home.local.HomeLocalDataSource
import com.awan.app.core.data.home.remote.HomeRemoteDataSource
import com.awan.app.core.data.sync.ScheduleSynchronizer
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.domain.gamification.model.SessionReward
import com.awan.app.core.domain.home.model.DaySchedule
import com.awan.app.core.domain.home.model.DaySession
import com.awan.app.core.domain.home.model.DayZone
import com.awan.app.core.domain.home.model.UserProfileInfo
import com.awan.app.core.domain.home.repository.HomeRepository
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import android.util.Log
import com.awan.app.core.common.result.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import com.awan.app.core.data.util.parseIsoDateTime
import javax.inject.Inject
import javax.inject.Singleton
import com.awan.app.core.model.SessionDetailInfo
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.model.SessionTaskDetail
import com.awan.app.core.model.TaskDetailInfo
import com.awan.app.core.model.TaskStatus

@Singleton
class HomeRepositoryImpl @Inject constructor(
    private val remoteDataSource: HomeRemoteDataSource,
    private val local: HomeLocalDataSource,
    private val eventBus: GamificationEventBus,
    private val scheduleSynchronizer: ScheduleSynchronizer,
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
                local.upsertUser(
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

        val cachedUser = local.getCachedUser()
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

    override suspend fun refreshSchedule(date: LocalDate): Result<Unit> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        // Forced: a screen open must not be answered from a TTL window the user cannot see.
        if (scheduleSynchronizer.syncScheduleRange(date, date, forceRefresh = true)) {
            Result.Success(Unit)
        } else {
            Result.Error(AppError.Network)
        }
    }

    /**
     * Both halves are Room Flows so the timeline reacts to either changing. Resolving zones with a
     * suspend lookup inside the map — as this did — produces a Flow that Room only invalidates for
     * the `sessions` table, so a zone edit stayed invisible until a day change built a new Flow.
     */
    override fun getDaySchedule(date: LocalDate): Flow<Result<DaySchedule>> {
        val dateStr = date.toString()

        return combine(
            local.observeSessionsForDate(dateStr),
            local.observeEffectiveZonesForDate(dateStr, date.dayOfWeek.name),
        ) { sessionEntities, zoneEntities ->
                val daySessions = sessionEntities.mapNotNull { s ->
                    val task = local.getTask(s.taskId) ?: return@mapNotNull null
                    val category = task.categoryId?.let { local.getCategory(it) }
                    val startLocalTime = parseLocalTime(s.startTime)
                    val endLocalTime = parseLocalTime(s.endTime)
                    val startMinutes = startLocalTime.hour * 60 + startLocalTime.minute
                    val durationMinutes = run {
                        val endMinutes = endLocalTime.hour * 60 + endLocalTime.minute
                        if (endMinutes > startMinutes) {
                            endMinutes - startMinutes
                        } else if (endMinutes < startMinutes) {
                            (endMinutes + 24 * 60) - startMinutes
                        } else if (s.startTime != s.endTime) {
                            24 * 60
                        } else {
                            0
                        }
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
                        zones = zoneEntities.map(::toDayZone),
                        sessions = daySessions,
                    )
                )
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getSessionDetail(sessionId: String): Result<SessionTaskDetail> =
        withContext(ioDispatcher) {
            val sessionResult = remoteDataSource.getSession(sessionId)
            val sessionDto = when (sessionResult) {
                is Result.Success -> {
                    local.cacheSession(sessionResult.data)
                    sessionResult.data
                }
                is Result.Error -> {
                    val cached = local.getSession(sessionId)
                    if (cached != null) null else return@withContext Result.Error(sessionResult.error)
                }
                Result.Loading -> return@withContext Result.Loading
            }

            val sessionDetailInfo = if (sessionDto != null) {
                val startDt = parseIsoDateTime(sessionDto.start) ?: LocalDateTime.parse(sessionDto.start)
                var endDt = parseIsoDateTime(sessionDto.end) ?: LocalDateTime.parse(sessionDto.end)
                if (!endDt.isAfter(startDt) && sessionDto.start != sessionDto.end) {
                    endDt = endDt.plusDays(1)
                }
                SessionDetailInfo(
                    id = sessionDto.id,
                    start = startDt,
                    end = endDt,
                    status = mapStatus(sessionDto.status),
                    locked = sessionDto.locked,
                    zoneId = sessionDto.zoneId,
                    taskId = sessionDto.taskId ?: "",
                )
            } else {
                val cached = local.getSession(sessionId)!!
                val startDt = LocalDateTime.parse("${cached.date}T${cached.startTime}")
                var endDt = LocalDateTime.parse("${cached.date}T${cached.endTime}")
                if (!endDt.isAfter(startDt) && cached.startTime != cached.endTime) {
                    endDt = endDt.plusDays(1)
                }
                SessionDetailInfo(
                    id = cached.id,
                    start = startDt,
                    end = endDt,
                    status = mapStatus(cached.status),
                    locked = cached.locked,
                    zoneId = cached.zoneId,
                    taskId = cached.taskId,
                )
            }

            val taskId = sessionDetailInfo.taskId
            val taskResult = remoteDataSource.getTask(taskId)
            val taskDto = when (taskResult) {
                is Result.Success -> {
                    local.cacheTask(taskId, taskResult.data)
                    taskResult.data
                }
                is Result.Error -> {
                    val cached = local.getTask(taskId)
                    if (cached != null) null else return@withContext Result.Error(taskResult.error)
                }
                Result.Loading -> return@withContext Result.Loading
            }

            val taskDetailInfo = if (taskDto != null) {
                TaskDetailInfo(
                    id = taskDto.id,
                    title = taskDto.title,
                    description = taskDto.description,
                    estimatedDuration = taskDto.estimatedDuration,
                    status = mapTaskStatus(taskDto.status),
                    mandatory = taskDto.mandatory ?: false,
                    estimatedPoints = taskDto.estimatedPoints ?: 0,
                    allowTaskSplitting = taskDto.allowTaskSplitting ?: false,
                    goalId = taskDto.goalId,
                    categoryName = taskDto.category?.name,
                    dependsOnTaskIds = taskDto.dependsOnTaskIds ?: emptyList(),
                )
            } else {
                val cached = local.getTask(taskId)!!
                val category = cached.categoryId?.let { local.getCategory(it) }
                TaskDetailInfo(
                    id = cached.id,
                    title = cached.title,
                    description = cached.description,
                    estimatedDuration = cached.estimatedDuration,
                    status = mapTaskStatus(cached.status),
                    mandatory = cached.mandatory,
                    estimatedPoints = cached.estimatedPoints,
                    allowTaskSplitting = cached.allowTaskSplitting,
                    goalId = cached.goalId,
                    categoryName = category?.name,
                    dependsOnTaskIds = emptyList(), // Local fallback doesn't include deps for now
                )
            }

            Result.Success(
                SessionTaskDetail(
                    session = sessionDetailInfo,
                    task = taskDetailInfo,
                )
            )
        }

    override suspend fun completeSession(sessionId: String): Result<SessionReward> =
        withContext(ioDispatcher) {
            if (!connectivityMonitor.isCurrentlyOnline()) {
                return@withContext Result.Error(AppError.Network)
            }
            when (val result = remoteDataSource.completeSession(sessionId)) {
                is Result.Success -> {
                    local.cacheSession(result.data.session)
                    val reward = result.data.reward.toDomain()
                    // Published here rather than from the caller so any future path that completes
                    // a session celebrates identically, without each one remembering to.
                    eventBus.publishSessionReward(reward)
                    Result.Success(reward)
                }
                is Result.Error -> Result.Error(result.error)
                Result.Loading -> unexpectedLoading()
            }
        }

    override suspend fun uncompleteSession(sessionId: String): Result<Unit> =
        // Nothing is published: undoing a completion does not take the points back, so there is no
        // change to celebrate and reversing the animation would misrepresent the balance.
        applySessionChange { remoteDataSource.uncompleteSession(sessionId) }

    override suspend fun cancelSession(sessionId: String): Result<Unit> =
        applySessionChange { remoteDataSource.cancelSession(sessionId) }

    override suspend fun moveSession(
        sessionId: String,
        startIso: String,
        endIso: String,
    ): Result<Unit> = applySessionChange {
        remoteDataSource.moveSession(sessionId, startIso, endIso)
    }

    private suspend fun applySessionChange(
        call: suspend () -> Result<SessionDto>,
    ): Result<Unit> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
        when (val result = call()) {
            is Result.Success -> {
                local.cacheSession(result.data)
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(result.error)
            Result.Loading -> unexpectedLoading()
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

    private fun mapStatus(statusStr: String?): SessionStatus {
        if (statusStr == null) return SessionStatus.SCHEDULED
        return try {
            SessionStatus.valueOf(statusStr.uppercase())
        } catch (e: Exception) {
            SessionStatus.UNKNOWN
        }
    }

    private fun mapTaskStatus(statusStr: String?): TaskStatus {
        if (statusStr == null) return TaskStatus.SCHEDULED
        return try {
            TaskStatus.valueOf(statusStr.uppercase())
        } catch (e: Exception) {
            TaskStatus.UNKNOWN
        }
    }

    private fun toDayZone(entity: ZoneEntity): DayZone {
        val startLocalTime = parseLocalTime(entity.startTime)
        val endLocalTime = parseLocalTime(entity.endTime)
        return DayZone(
            id = entity.id,
            name = entity.name,
            categoryId = entity.id,
            categoryName = entity.name,
            startMinutes = startLocalTime.hour * 60 + startLocalTime.minute,
            endMinutes = endLocalTime.hour * 60 + endLocalTime.minute,
            color = entity.color
        )
    }

    override suspend fun updateSessionLock(
        sessionId: String,
        locked: Boolean,
    ): Result<Unit> = applySessionChange {
        if (locked) {
            remoteDataSource.lockSession(sessionId)
        } else {
            remoteDataSource.unlockSession(sessionId)
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
            is Result.Success -> {
                local.cacheTask(taskId, result.data)
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(result.error)
            else -> Result.Error(AppError.Unknown(Throwable("Failed to update task details")))
        }
    }

    override suspend fun deleteSession(sessionId: String): Result<Unit> {
        checkOnline()?.let { return it }
        val result = remoteDataSource.deleteSession(sessionId)
        return when (result) {
            is Result.Success -> {
                local.deleteSession(sessionId)
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(result.error)
            else -> Result.Error(AppError.Unknown(Throwable("Failed to delete session")))
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        checkOnline()?.let { return it }
        val result = remoteDataSource.deleteTask(taskId, cascade = true)
        return when (result) {
            is Result.Success -> {
                local.deleteTask(taskId)
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(result.error)
            else -> Result.Error(AppError.Unknown(Throwable("Failed to delete task")))
        }
    }

    private fun checkOnline(): Result<Nothing>? =
        if (connectivityMonitor.isCurrentlyOnline()) null else Result.Error(AppError.Network)

    private fun unexpectedLoading(): Result.Error =
        Result.Error(AppError.Unknown(Throwable("Session call returned Loading")))
}
