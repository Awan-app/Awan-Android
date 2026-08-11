package com.awan.app.core.data.goal

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.data.goal.remote.GoalRemoteDataSource
import com.awan.app.core.database.dao.GoalDao
import com.awan.app.core.database.dao.TaskDao
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
import com.awan.app.core.network.dto.goal.UpdateGoalRequest
import com.awan.app.core.network.dto.goal.ProposedGoalSessionDto
import com.awan.app.core.network.dto.GoalInfoResponse
import com.awan.app.core.data.sync.SyncTtl
import com.awan.app.core.database.model.GoalEntity
import com.awan.app.core.database.model.TaskDependencyEntity
import com.awan.app.core.data.task.toDependencyEntities
import com.awan.app.core.data.task.toEntity
import com.awan.app.core.data.task.toTaskModel
import com.awan.app.core.common.result.suspendOnSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Reads goals exclusively from the local Room database (SSOT).
 * Remote data is injected into Room by [OfflineSyncCoordinator]; this
 * repository never performs a remote GET for UI reads.
 *
 * Mutations (create, decompose, confirm) check connectivity and persist
 * the result into Room before returning.
 */
class GoalRepositoryImpl @Inject constructor(
    private val remoteDataSource: GoalRemoteDataSource,
    private val goalDao: GoalDao,
    private val taskDao: TaskDao,
    private val connectivityMonitor: NetworkConnectivityMonitor,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : GoalRepository {

    override fun observeGoals(): Flow<List<Goal>> {
        return goalDao.observeAllGoals().map { entities ->
            entities.map { entity ->
                val tasks = taskDao.getTasksByGoal(entity.id).map { taskEntity ->
                    taskEntity.toTaskModel(dependsOnTaskIds = taskDao.getDependsOnIds(taskEntity.id))
                }
                entity.toModel().copy(tasks = tasks)
            }
        }
    }

    override suspend fun getGoals(): Result<List<Goal>> = withContext(ioDispatcher) {
        if (connectivityMonitor.isCurrentlyOnline()) {
            remoteDataSource.getGoals().suspendOnSuccess { syncGoals(it) }
        }
        val models = goalDao.getAllGoals().map { it.toModelWithTasks() }
        Result.Success(models)
    }

    override suspend fun createGoal(
        title: String,
        description: String?,
        targetDate: String?,
    ): Result<Goal> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
        remoteDataSource.createGoal(
            CreateGoalRequest(title = title, description = description, targetDate = targetDate),
        ).map { dto ->
            syncGoal(dto)
            dto.toEntity().toModelWithTasks()
        }
    }

    override suspend fun getInboxGoal(): Result<Goal> = withContext(ioDispatcher) {
        if (connectivityMonitor.isCurrentlyOnline()) {
            remoteDataSource.getInboxGoal().suspendOnSuccess { syncGoal(it) }
        }
        goalDao.getAllGoals().find { it.isInbox }?.let {
            Result.Success(it.toModelWithTasks())
        } ?: Result.Error(AppError.NotFound)
    }

    override suspend fun getGoal(goalId: String): Result<Goal> = withContext(ioDispatcher) {
        if (connectivityMonitor.isCurrentlyOnline()) {
            remoteDataSource.getGoal(goalId).suspendOnSuccess { syncGoal(it) }
        }
        goalDao.getGoal(goalId)?.let {
            Result.Success(it.toModelWithTasks())
        } ?: Result.Error(AppError.NotFound)
    }

    override suspend fun updateGoal(
        goalId: String,
        title: String?,
        description: String?,
        status: String?,
        targetDate: String?,
    ): Result<Goal> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }

        // Restriction: Inbox cannot be edited
        val existing = goalDao.getGoal(goalId)
        if (existing?.isInbox == true) {
            return@withContext Result.Error(AppError.Unknown(Throwable("Inbox goal cannot be edited")))
        }

        remoteDataSource.updateGoal(
            goalId = goalId,
            request = UpdateGoalRequest(
                title = title,
                description = description,
                status = status,
                targetDate = targetDate,
            ),
        ).map { dto ->
            syncGoal(dto)
            dto.toEntity().toModelWithTasks()
        }
    }

    override suspend fun deleteGoal(goalId: String): Result<Unit> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }

        // Restriction: Inbox cannot be deleted
        val existing = goalDao.getGoal(goalId)
        if (existing?.isInbox == true) {
            return@withContext Result.Error(AppError.Unknown(Throwable("Inbox goal cannot be deleted")))
        }

        remoteDataSource.deleteGoal(goalId).map {
            goalDao.deleteGoal(goalId)
            taskDao.deleteTasksByGoal(goalId)
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

    override suspend fun confirmDecomposition(sessionId: String): Result<Goal> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
        remoteDataSource.confirmDecomposition(sessionId).map { dto ->
            syncGoal(dto)
            dto.toEntity().toModelWithTasks()
        }
    }

    private suspend fun GoalEntity.toModelWithTasks(): Goal {
        val tasks = taskDao.getTasksByGoal(id).map { taskEntity ->
            taskEntity.toTaskModel(dependsOnTaskIds = taskDao.getDependsOnIds(taskEntity.id))
        }
        return toModel().copy(tasks = tasks)
    }

    private suspend fun syncGoal(dto: GoalInfoResponse) {
        val expiry = SyncTtl.computeExpiry(SyncTtl.GOALS_TTL_MS)
        goalDao.upsertGoal(dto.toEntity().copy(expiryTime = expiry))
        val entities = dto.tasks.map { it.toEntity(expiryTime = expiry) }
        val dependencies = dto.tasks.flatMap { it.toDependencyEntities() }
        taskDao.replaceTasksForGoal(dto.id, entities, dependencies)
    }

    private suspend fun syncGoals(dtos: List<GoalInfoResponse>) {
        val expiry = SyncTtl.computeExpiry(SyncTtl.GOALS_TTL_MS)
        goalDao.upsertGoals(dtos.map { it.toEntity().copy(expiryTime = expiry) })
        dtos.forEach { dto ->
            val entities = dto.tasks.map { it.toEntity(expiryTime = expiry) }
            val dependencies = dto.tasks.flatMap { it.toDependencyEntities() }
            taskDao.replaceTasksForGoal(dto.id, entities, dependencies)
        }
    }

    override suspend fun getDecompositionTranscript(sessionId: String): Result<GoalDecompositionTranscript> {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return Result.Error(AppError.Network)
        }
        return remoteDataSource.getDecompositionTranscript(sessionId).map { it.toTranscript() }
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
}
