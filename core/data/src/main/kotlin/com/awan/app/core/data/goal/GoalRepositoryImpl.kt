package com.awan.app.core.data.goal

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.data.goal.remote.GoalRemoteDataSource
import com.awan.app.core.database.dao.GoalDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.dao.CategoryDao
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalDecompositionReply
import com.awan.app.core.model.GoalDecompositionTranscript
import com.awan.app.core.model.GoalScheduleProposal
import com.awan.app.core.model.ProposedGoalSession
import com.awan.app.core.model.ProposedTask
import com.awan.app.core.network.dto.GoalDecomposeRequest
import com.awan.app.core.network.dto.goal.ConfirmAiScheduleRequest
import com.awan.app.core.network.dto.goal.CreateGoalRequest
import com.awan.app.core.network.dto.goal.CreateGoalTaskDto
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
import com.awan.app.core.data.category.toModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
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
    private val categoryDao: CategoryDao,
    private val connectivityMonitor: NetworkConnectivityMonitor,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : GoalRepository {

    override fun observeGoals(): Flow<List<Goal>> {
        val goalsFlow = goalDao.observeAllGoals()
        val categoriesFlow = categoryDao.observeAllCategories()

        return combine(goalsFlow, categoriesFlow) { goalEntities, categoryEntities ->
            val categoryMap = categoryEntities.associateBy { it.id }
            goalEntities to categoryMap
        }.flatMapLatest { (goalEntities, categoryMap) ->
            if (goalEntities.isEmpty()) return@flatMapLatest kotlinx.coroutines.flow.flowOf(emptyList())

            val goalFlows = goalEntities.map { entity ->
                taskDao.observeTasksByGoal(entity.id).map { taskEntities ->
                    val tasks = taskEntities.map { taskEntity ->
                        val category = taskEntity.categoryId?.let { catId ->
                            categoryMap[catId]?.let { it.toModel() }
                        }
                        taskEntity.toTaskModel(
                            dependsOnTaskIds = taskDao.getDependsOnIds(taskEntity.id),
                            category = category
                        )
                    }
                    val goalModel = entity.toModel()
                    goalModel.copy(tasks = tasks)
                }
            }
            combine(goalFlows) { it.toList() }
        }.flowOn(ioDispatcher)
    }

    override fun observeGoal(goalId: String): Flow<Goal?> {
        val goalFlow = goalDao.observeGoal(goalId)
        val categoriesFlow = categoryDao.observeAllCategories()

        return combine(goalFlow, categoriesFlow) { entity, categoryEntities ->
            if (entity == null) return@combine kotlinx.coroutines.flow.flowOf(null)
            val categoryMap = categoryEntities.associateBy { it.id }

            taskDao.observeTasksByGoal(entity.id).map { taskEntities ->
                val tasks = taskEntities.map { taskEntity ->
                    val category = taskEntity.categoryId?.let { categoryMap[it]?.toModel() }
                    taskEntity.toTaskModel(
                        dependsOnTaskIds = taskDao.getDependsOnIds(taskEntity.id),
                        category = category
                    )
                }
                entity.toModel().copy(tasks = tasks)
            }
        }.flatMapLatest { it }
            .flowOn(ioDispatcher)
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
        tasks: List<ProposedTask>,
    ): Result<Goal> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
        remoteDataSource.createGoal(
            CreateGoalRequest(
                title = title,
                description = description,
                targetDate = targetDate,
                tasks = tasks.mapIndexed { index, task ->
                    CreateGoalTaskDto(
                        tempId = "proposal-task-$index",
                        title = task.title,
                        estimatedDuration = task.estimatedDuration?.takeIf { it > 0 } ?: 30,
                        mandatory = false,
                        estimatedPoints = task.estimatedPoints ?: 0,
                        allowTaskSplitting = false,
                    )
                },
            ),
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
            remoteDataSource.getGoal(goalId, expand = true).suspendOnSuccess { syncGoal(it) }
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
        val categoryEntities = categoryDao.getAllCategories()
        val categoryMap = categoryEntities.associateBy { it.id }
        
        val tasks = taskDao.getTasksByGoal(id).map { taskEntity ->
            val category = taskEntity.categoryId?.let { categoryMap[it]?.toModel() }
            taskEntity.toTaskModel(
                dependsOnTaskIds = taskDao.getDependsOnIds(taskEntity.id),
                category = category
            )
        }
        val goalModel = toModel()
        return goalModel.copy(tasks = tasks)
    }

    private suspend fun syncGoal(dto: GoalInfoResponse) {
        val expiry = SyncTtl.computeExpiry(SyncTtl.GOALS_TTL_MS)
        goalDao.upsertGoal(dto.toEntity().copy(expiryTime = expiry))
        dto.tasks?.let { tasks ->
            val entities = tasks.map { it.toEntity(expiryTime = expiry) }
            val dependencies = tasks.flatMap { it.toDependencyEntities() }
            taskDao.replaceTasksForGoal(dto.id, entities, dependencies)
        }
    }

    private suspend fun syncGoals(dtos: List<GoalInfoResponse>) {
        val expiry = SyncTtl.computeExpiry(SyncTtl.GOALS_TTL_MS)
        goalDao.upsertGoals(dtos.map { it.toEntity().copy(expiryTime = expiry) })
        dtos.forEach { dto ->
            dto.tasks?.let { tasks ->
                val entities = tasks.map { it.toEntity(expiryTime = expiry) }
                val dependencies = tasks.flatMap { it.toDependencyEntities() }
                taskDao.replaceTasksForGoal(dto.id, entities, dependencies)
            }
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
