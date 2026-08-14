package com.awan.app.core.data.goal

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.data.goal.remote.GoalRemoteDataSource
import com.awan.app.core.data.sync.SyncTtl
import com.awan.app.core.data.task.toEntity
import com.awan.app.core.database.dao.CategoryDao
import com.awan.app.core.database.dao.GoalDao
import com.awan.app.core.database.dao.ScheduleDraftDao
import com.awan.app.core.database.dao.SessionDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.model.CategoryEntity
import com.awan.app.core.database.model.ScheduleDraftEntity
import com.awan.app.core.database.model.ScheduleDraftSessionEntity
import com.awan.app.core.database.model.ScheduleDraftUnscheduledTaskEntity
import com.awan.app.core.database.model.TaskDependencyEntity
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.model.ConfirmedGoalSession
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalDecompositionReply
import com.awan.app.core.model.GoalDecompositionTranscript
import com.awan.app.core.model.GoalScheduleProposal
import com.awan.app.core.model.GoalScheduleSuggestion
import com.awan.app.core.model.ProposedGoalSession
import com.awan.app.core.model.ProposedTask
import com.awan.app.core.model.ScheduleOverlapInfo
import com.awan.app.core.model.UnscheduledTask
import com.awan.app.core.network.dto.GoalDecomposeRequest
import com.awan.app.core.network.dto.goal.ConfirmAiScheduleRequest
import com.awan.app.core.network.dto.goal.ConfirmedGoalSessionDto
import com.awan.app.core.network.dto.goal.CreateGoalRequest
import com.awan.app.core.network.dto.goal.CreateGoalTaskDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val remoteDataSource: GoalRemoteDataSource,
    private val goalDao: GoalDao,
    private val connectivityMonitor: NetworkConnectivityMonitor,
    private val categoryDao: CategoryDao,
    private val taskDao: TaskDao,
    private val scheduleDraftDao: ScheduleDraftDao,
    private val sessionDao: SessionDao,
) : GoalRepository {

    override suspend fun getGoals(): Result<List<Goal>> {
        if (connectivityMonitor.isCurrentlyOnline()) {
            when (val remoteResult = remoteDataSource.getGoals()) {
                is Result.Success -> {
                    val remoteGoals = remoteResult.data
                    goalDao.upsertGoals(remoteGoals.map { it.toEntity() })
                    return Result.Success(goalDao.getAllGoals().map { it.toModel() })
                }
                is Result.Error -> {
                    val cached = goalDao.getAllGoals()
                    return if (cached.isNotEmpty()) {
                        Result.Success(cached.map { it.toModel() })
                    } else {
                        Result.Error(remoteResult.error)
                    }
                }
                Result.Loading -> { /* no-op */ }
            }
        }
        val cached = goalDao.getAllGoals()
        return Result.Success(cached.map { it.toModel() })
    }

    override suspend fun createGoal(
        title: String,
        description: String?,
        targetDate: String?,
        tasks: List<ProposedTask>,
    ): Result<Goal> {
        val taskDtos = tasks.mapIndexed { index, task ->
            CreateGoalTaskDto(
                tempId = "proposal-task-$index",
                title = task.title,
                description = null,
                estimatedDuration = task.estimatedDuration ?: 30,
                mandatory = false,
                estimatedPoints = task.estimatedPoints ?: 0,
                allowTaskSplitting = false,
                dependsOnTempIds = emptyList(),
                categoryId = null,
            )
        }
        val request = CreateGoalRequest(
            title = title,
            description = description,
            targetDate = targetDate,
            tasks = taskDtos,
        )
        return when (val result = remoteDataSource.createGoal(request)) {
            is Result.Success -> {
                val goalResponse = result.data
                goalDao.upsertGoal(goalResponse.toEntity())
                if (goalResponse.tasks.isNotEmpty()) {
                    val taskEntities = goalResponse.tasks.map { it.toEntity(goalId = goalResponse.id) }
                    taskDao.upsertTasks(taskEntities)
                }
                Result.Success(goalResponse.toModel())
            }
            is Result.Error -> Result.Error(result.error)
            Result.Loading -> Result.Loading
        }
    }

    override suspend fun getInboxGoal(): Result<Goal> {
        if (connectivityMonitor.isCurrentlyOnline()) {
            when (val result = remoteDataSource.getInboxGoal()) {
                is Result.Success -> {
                    val goalResponse = result.data
                    goalDao.upsertGoal(goalResponse.toEntity())
                    if (goalResponse.tasks.isNotEmpty()) {
                        val taskEntities = goalResponse.tasks.map { it.toEntity(goalId = goalResponse.id) }
                        taskDao.upsertTasks(taskEntities)
                    }
                    return Result.Success(goalResponse.toModel())
                }
                is Result.Error -> {
                    val cached = goalDao.getAllGoals().firstOrNull { it.isInbox }
                    return if (cached != null) {
                        Result.Success(cached.toModel())
                    } else {
                        Result.Error(result.error)
                    }
                }
                Result.Loading -> { /* no-op */ }
            }
        }
        val cached = goalDao.getAllGoals().firstOrNull { it.isInbox }
        return if (cached != null) {
            Result.Success(cached.toModel())
        } else {
            Result.Error(AppError.Network)
        }
    }

    override suspend fun getGoal(goalId: String): Result<Goal> {
        if (connectivityMonitor.isCurrentlyOnline()) {
            when (val result = remoteDataSource.getGoal(goalId)) {
                is Result.Success -> {
                    val goalResponse = result.data
                    goalDao.upsertGoal(goalResponse.toEntity())
                    if (goalResponse.tasks.isNotEmpty()) {
                        val taskEntities = goalResponse.tasks.map { it.toEntity(goalId = goalResponse.id) }
                        taskDao.upsertTasks(taskEntities)
                    }
                    return Result.Success(goalResponse.toModel())
                }
                is Result.Error -> {
                    val cached = goalDao.getGoal(goalId)
                    return if (cached != null) {
                        Result.Success(cached.toModel())
                    } else {
                        Result.Error(result.error)
                    }
                }
                Result.Loading -> { /* no-op */ }
            }
        }
        val cached = goalDao.getGoal(goalId)
        return if (cached != null) {
            Result.Success(cached.toModel())
        } else {
            Result.Error(AppError.Network)
        }
    }

    override suspend fun deleteGoal(goalId: String): Result<Unit> {
        return when (val result = remoteDataSource.deleteGoal(goalId)) {
            is Result.Success -> {
                goalDao.deleteGoal(goalId)
                taskDao.deleteTasksByGoal(goalId)
                scheduleDraftDao.deleteDraft(goalId)
                Result.Success(Unit)
            }
            is Result.Error -> Result.Error(result.error)
            Result.Loading -> Result.Loading
        }
    }

    override suspend fun continueDecomposition(
        sessionId: String?,
        message: String,
    ): Result<GoalDecompositionReply> {
        val request = GoalDecomposeRequest(sessionId = sessionId, message = message)
        return remoteDataSource.continueDecomposition(request).map { it.toDecompositionReply() }
    }

    override suspend fun confirmDecomposition(sessionId: String): Result<Goal> {
        return when (val result = remoteDataSource.confirmDecomposition(sessionId)) {
            is Result.Success -> {
                val goalResponse = result.data
                goalDao.upsertGoal(goalResponse.toEntity())
                if (goalResponse.tasks.isNotEmpty()) {
                    val categories = goalResponse.tasks.mapNotNull { it.category }.distinctBy { it.id }.map {
                        CategoryEntity(id = it.id, name = it.name)
                    }
                    if (categories.isNotEmpty()) {
                        categoryDao.upsertCategories(categories)
                    }
                    val taskEntities = goalResponse.tasks.map { it.toEntity(goalId = goalResponse.id) }
                    taskDao.upsertTasks(taskEntities)

                    val dependencyEntities = goalResponse.tasks.flatMap { task ->
                        task.dependsOnTaskIds.orEmpty().map { depId ->
                            TaskDependencyEntity(taskId = task.id, dependsOnTaskId = depId)
                        }
                    }
                    if (dependencyEntities.isNotEmpty()) {
                        taskDao.upsertDependencies(dependencyEntities)
                    }
                }
                Result.Success(goalResponse.toModel())
            }
            is Result.Error -> Result.Error(result.error)
            Result.Loading -> Result.Loading
        }
    }

    override suspend fun getDecompositionTranscript(sessionId: String): Result<GoalDecompositionTranscript> {
        return remoteDataSource.getDecompositionTranscript(sessionId).map { it.toTranscript() }
    }

    override suspend fun cancelDecomposition(sessionId: String): Result<Unit> =
        remoteDataSource.cancelDecomposition(sessionId)

    override suspend fun scheduleGoal(goalId: String): Result<Unit> {
        return remoteDataSource.scheduleGoal(goalId).map { }
    }

    override suspend fun proposeGoalSchedule(goalId: String): Result<GoalScheduleProposal> {
        scheduleDraftDao.insertDraftIfNotExists(
            ScheduleDraftEntity(goalId = goalId, state = "AWAITING_PROPOSAL")
        )
        return when (val result = remoteDataSource.proposeGoalSchedule(goalId)) {
            is Result.Success -> {
                val response = result.data
                val draft = ScheduleDraftEntity(goalId = goalId, state = "READY")
                val sessionEntities = mutableListOf<ScheduleDraftSessionEntity>()

                response.proposedSessions.forEach { dto ->
                    sessionEntities += ScheduleDraftSessionEntity(
                        goalId = goalId,
                        taskId = dto.taskId,
                        taskTitle = dto.taskTitle,
                        zoneId = dto.zoneId,
                        start = dto.start,
                        end = dto.end,
                        suggestionType = null,
                        suggestionReason = null,
                        overlapTaskTitle = null,
                        overlapStart = null,
                        overlapEnd = null,
                        overlapMandatory = null,
                        overlapPoints = null,
                        isSelected = true,
                    )
                }

                response.suggestions.forEach { dto ->
                    sessionEntities += ScheduleDraftSessionEntity(
                        goalId = goalId,
                        taskId = dto.taskId,
                        taskTitle = dto.taskTitle,
                        zoneId = dto.zoneId,
                        start = dto.start,
                        end = dto.end,
                        suggestionType = dto.suggestionType,
                        suggestionReason = dto.reason,
                        overlapTaskTitle = dto.overlapInfo?.taskTitle,
                        overlapStart = dto.overlapInfo?.start,
                        overlapEnd = dto.overlapInfo?.end,
                        overlapMandatory = dto.overlapInfo?.mandatory,
                        overlapPoints = dto.overlapInfo?.points,
                        isSelected = false,
                    )
                }

                val unscheduledEntities = response.unscheduledTasks.map { dto ->
                    ScheduleDraftUnscheduledTaskEntity(
                        taskId = dto.taskId,
                        goalId = goalId,
                        taskTitle = dto.taskTitle,
                        message = dto.message,
                    )
                }

                scheduleDraftDao.replaceDraft(draft, sessionEntities, unscheduledEntities)

                val proposal = GoalScheduleProposal(
                    goalId = goalId,
                    proposedSessions = response.proposedSessions.map {
                        ProposedGoalSession(
                            taskId = it.taskId,
                            taskTitle = it.taskTitle,
                            zoneId = it.zoneId,
                            start = it.start,
                            end = it.end,
                            isSelected = true,
                        )
                    },
                    suggestions = response.suggestions.map {
                        GoalScheduleSuggestion(
                            taskId = it.taskId,
                            taskTitle = it.taskTitle,
                            zoneId = it.zoneId,
                            start = it.start,
                            end = it.end,
                            suggestionType = it.suggestionType,
                            reason = it.reason,
                            overlapInfo = it.overlapInfo?.let { info ->
                                ScheduleOverlapInfo(
                                    taskTitle = info.taskTitle,
                                    start = info.start,
                                    end = info.end,
                                    mandatory = info.mandatory,
                                    points = info.points,
                                )
                            },
                            isSelected = false,
                        )
                    },
                    unscheduledTasks = response.unscheduledTasks.map {
                        UnscheduledTask(
                            taskId = it.taskId,
                            taskTitle = it.taskTitle,
                            message = it.message,
                        )
                    },
                )
                Result.Success(proposal)
            }
            is Result.Error -> Result.Error(result.error)
            Result.Loading -> Result.Loading
        }
    }

    override suspend fun confirmGoalSchedule(
        goalId: String,
        sessions: List<ProposedGoalSession>,
    ): Result<List<ConfirmedGoalSession>> {
        scheduleDraftDao.updateDraftState(goalId, "CONFIRMING")
        val sessionDtos = sessions.map {
            ConfirmedGoalSessionDto(
                taskId = it.taskId,
                zoneId = it.zoneId,
                start = it.start,
                end = it.end,
            )
        }
        val request = ConfirmAiScheduleRequest(goalId = goalId, sessions = sessionDtos)
        return when (val result = remoteDataSource.confirmGoalSchedule(request)) {
            is Result.Success -> {
                val remoteSessions = result.data
                val confirmedSessions = if (remoteSessions.isNotEmpty()) {
                    remoteSessions.mapIndexed { index, dto ->
                        val fallback = sessions.getOrNull(index)
                        ConfirmedGoalSession(
                            id = dto.effectiveId,
                            taskId = dto.taskId?.takeIf { it.isNotBlank() } ?: fallback?.taskId ?: java.util.UUID.randomUUID().toString(),
                            zoneId = dto.zoneId ?: fallback?.zoneId,
                            start = dto.start?.takeIf { it.isNotBlank() } ?: fallback?.start.orEmpty(),
                            end = dto.end?.takeIf { it.isNotBlank() } ?: fallback?.end.orEmpty(),
                        )
                    }
                } else {
                    sessions.map { req ->
                        ConfirmedGoalSession(
                            id = java.util.UUID.randomUUID().toString(),
                            taskId = req.taskId,
                            zoneId = req.zoneId,
                            start = req.start,
                            end = req.end,
                        )
                    }
                }

                val expiryTime = SyncTtl.computeExpiry(SyncTtl.SCHEDULE_TTL_MS)
                val sessionEntities = confirmedSessions.map { s ->
                    val date = if (s.start.length >= 10) s.start.substring(0, 10) else ""
                    val startTime = when {
                        s.start.length >= 19 -> s.start.substring(11, 19)
                        s.start.length >= 16 -> s.start.substring(11, 16) + ":00"
                        else -> "00:00:00"
                    }
                    val endTime = when {
                        s.end.length >= 19 -> s.end.substring(11, 19)
                        s.end.length >= 16 -> s.end.substring(11, 16) + ":00"
                        else -> "00:00:00"
                    }
                    com.awan.app.core.database.model.SessionEntity(
                        id = s.id,
                        taskId = s.taskId,
                        zoneId = s.zoneId,
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        status = "SCHEDULED",
                        locked = false,
                        expiryTime = expiryTime,
                    )
                }
                sessionDao.upsertSessions(sessionEntities)
                scheduleDraftDao.deleteDraft(goalId)

                Result.Success(confirmedSessions)
            }
            is Result.Error -> Result.Error(result.error)
            Result.Loading -> Result.Loading
        }
    }

    override suspend fun clearScheduleDraft(goalId: String) {
        scheduleDraftDao.deleteDraft(goalId)
    }

    override suspend fun getPendingScheduleDraftGoalId(): Result<String?> =
        Result.Success(scheduleDraftDao.getPendingDraftGoalId())
}
