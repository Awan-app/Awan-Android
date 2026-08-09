package com.awan.app.core.data.goal

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.data.goal.remote.GoalRemoteDataSource
import com.awan.app.core.database.dao.GoalDao
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalDecompositionReply
import com.awan.app.core.model.GoalDecompositionTranscript
import com.awan.app.core.model.GoalScheduleProposal
import com.awan.app.core.model.ProposedGoalSession
import com.awan.app.core.network.dto.GoalDecomposeRequest
import com.awan.app.core.network.dto.goal.ConfirmAiScheduleRequest
import com.awan.app.core.network.dto.goal.CreateGoalRequest
import com.awan.app.core.network.dto.goal.ProposedGoalSessionDto
import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import com.awan.app.core.data.task.toEntity
import com.awan.app.core.data.task.toModel
import com.awan.app.core.data.task.toSessionModel
import com.awan.app.core.model.TaskWithSessions

class GoalRepositoryImpl @Inject constructor(
    private val remoteDataSource: GoalRemoteDataSource,
    private val goalDao: GoalDao,
    private val taskDao: com.awan.app.core.database.dao.TaskDao,
    private val sessionDao: com.awan.app.core.database.dao.SessionDao,
    private val connectivityMonitor: NetworkConnectivityMonitor,
) : GoalRepository {

    override fun observeGoals(): Flow<List<Goal>> {
        return combine(
            goalDao.observeAllGoals(),
            taskDao.observeAllTasks(),
            sessionDao.observeSessionsForDateRange("0000-01-01", "9999-12-31") // Observe all sessions
        ) { goals, tasks, sessions ->
            goals.map { entity ->
                entity.toModel().copy(
                    tasks = tasks.filter { it.goalId == entity.id }.map { taskEntity ->
                        TaskWithSessions(
                            task = taskEntity.toModel(),
                            sessions = sessions.filter { it.taskId == taskEntity.id }.mapNotNull { it.toSessionModel() }
                        )
                    },
                )
            }
        }
    }

    override suspend fun getGoals(): Result<List<Goal>> {
        val entities = goalDao.getAllGoals()
        val tasks = taskDao.getAllTasks()
        val sessions = sessionDao.getAllSessions()
        return Result.Success(
            entities.map { entity ->
                entity.toModel().copy(
                    tasks = tasks.filter { it.goalId == entity.id }.map { taskEntity ->
                        TaskWithSessions(
                            task = taskEntity.toModel(),
                            sessions = sessions.filter { it.taskId == taskEntity.id }.mapNotNull { it.toSessionModel() }
                        )
                    },
                )
            },
        )
    }

    override suspend fun createGoal(
        title: String,
        description: String?,
        targetDate: String?,
    ): Result<Goal> {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return Result.Error(AppError.Network)
        }
        return remoteDataSource.createGoal(
            CreateGoalRequest(
                title = title,
                description = description,
                targetDate = targetDate,
            ),
        ).map { dto ->
            val entity = dto.toEntity()
            goalDao.upsertGoal(entity)
            entity.toModel()
        }
    }

    override suspend fun getInboxGoal(): Result<Goal> {
        val cached = goalDao.getAllGoals().find { it.isInbox }
        if (cached != null) {
            val tasks = taskDao.getAllTasks().filter { it.goalId == cached.id }
            val sessions = sessionDao.getAllSessions()
            return Result.Success(
                cached.toModel().copy(
                    tasks = tasks.map { taskEntity ->
                        TaskWithSessions(
                            task = taskEntity.toModel(),
                            sessions = sessions.filter { it.taskId == taskEntity.id }.mapNotNull { it.toSessionModel() }
                        )
                    },
                ),
            )
        }
        return Result.Error(AppError.NotFound)
    }

    override suspend fun getGoal(goalId: String): Result<Goal> {
        val entity = goalDao.getGoal(goalId)
        if (entity != null) {
            val tasks = taskDao.getAllTasks().filter { it.goalId == goalId }
            val sessions = sessionDao.getAllSessions()
            return Result.Success(
                entity.toModel().copy(
                    tasks = tasks.map { taskEntity ->
                        TaskWithSessions(
                            task = taskEntity.toModel(),
                            sessions = sessions.filter { it.taskId == taskEntity.id }.mapNotNull { it.toSessionModel() }
                        )
                    },
                ),
            )
        }
        return Result.Error(AppError.NotFound)
    }

    override suspend fun deleteGoal(goalId: String): Result<Unit> {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return Result.Error(AppError.Network)
        }
        return remoteDataSource.deleteGoal(goalId).map {
            goalDao.deleteGoal(goalId)
        }
    }

    override suspend fun continueDecomposition(
        sessionId: String?,
        message: String,
    ): Result<GoalDecompositionReply> {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return Result.Error(AppError.Network)
        }
        return remoteDataSource.continueDecomposition(
            GoalDecomposeRequest(sessionId = sessionId, message = message),
        ).map { it.toDecompositionReply() }
    }

    override suspend fun confirmDecomposition(sessionId: String): Result<Goal> {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return Result.Error(AppError.Network)
        }
        return remoteDataSource.confirmDecomposition(sessionId).map { dto ->
            val entity = dto.toEntity()
            goalDao.upsertGoal(entity)

            // Immediately persist tasks so they show up in the UI without a refresh
            // Note: GoalInfoResponse.tasks is a List<TaskInfoResponse>.
            // TaskInfoResponse does not contain sessions directly in this DTO.
            val tasks = dto.tasks.map { it.toEntity(goalId = dto.id) }
            if (tasks.isNotEmpty()) {
                taskDao.upsertTasks(tasks)
            }

            dto.toModel()
        }
    }

    override suspend fun getDecompositionTranscript(sessionId: String): Result<GoalDecompositionTranscript> {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return Result.Error(AppError.Network)
        }
        return remoteDataSource.getDecompositionTranscript(sessionId).map { it.toTranscript() }
    }

    override suspend fun confirmGoalSchedule(
        goalId: String,
        sessions: List<ProposedGoalSession>,
    ): Result<Unit> {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return Result.Error(AppError.Network)
        }
        return remoteDataSource.confirmGoalSchedule(
            ConfirmAiScheduleRequest(
                goalId = goalId,
                sessions = sessions.map {
                    ProposedGoalSessionDto(
                        taskId = it.taskId,
                        zoneId = it.zoneId,
                        start = it.start,
                        end = it.end,
                    )
                },
            ),
        )
    }

    override suspend fun cancelDecomposition(sessionId: String): Result<Unit> {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return Result.Error(AppError.Network)
        }
        return remoteDataSource.cancelDecomposition(sessionId)
    }

    override suspend fun scheduleGoal(goalId: String): Result<Unit> {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return Result.Error(AppError.Network)
        }
        return remoteDataSource.scheduleGoal(goalId).map { }
    }

    override suspend fun proposeGoalSchedule(goalId: String): Result<GoalScheduleProposal> {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return Result.Error(AppError.Network)
        }
        return remoteDataSource.proposeGoalSchedule(goalId).map { dto ->
            GoalScheduleProposal(
                goalId = dto.goalId,
                proposedSessions = dto.proposedSessions.map {
                    ProposedGoalSession(
                        taskId = it.taskId,
                        zoneId = it.zoneId,
                        start = it.start,
                        end = it.end,
                    )
                },
                reason = dto.reason,
            )
        }
    }
}
