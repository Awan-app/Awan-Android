package com.awan.app.core.data.task

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.database.dao.CategoryDao
import com.awan.app.core.database.dao.SessionDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.model.CategoryEntity
import com.awan.app.core.database.model.SessionEntity
import com.awan.app.core.database.model.TaskEntity
import com.awan.app.core.data.gamification.GamificationEventBus
import com.awan.app.core.data.gamification.mapper.toDomain
import com.awan.app.core.data.task.remote.TaskRemoteDataSource
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.model.SessionDraft
import com.awan.app.core.model.Task
import com.awan.app.core.model.TaskDraft
import com.awan.app.core.model.TaskProposals
import com.awan.app.core.model.TaskSchedule
import com.awan.app.core.model.TaskWithSessions
import com.awan.app.core.model.TaskWithSessionsDraft
import com.awan.app.core.network.dto.task.AiTextToTasksRequest
import com.awan.app.core.network.dto.task.BulkCreateTasksWithSessionsRequest
import com.awan.app.core.network.dto.task.TaskCompletionResponse
import com.awan.app.core.network.dto.task.ScheduleTaskRequest
import com.awan.app.core.data.common.extractDateFromIso
import com.awan.app.core.data.common.extractTimeFromIso
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

import com.awan.app.core.database.dao.GoalDao

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val remoteDataSource: TaskRemoteDataSource,
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val sessionDao: SessionDao,
    private val goalDao: GoalDao,
    private val eventBus: GamificationEventBus,
    private val connectivityMonitor: NetworkConnectivityMonitor,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : TaskRepository {



    override suspend fun createTask(draft: TaskDraft): Result<Task> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
        val result = remoteDataSource.createTask(draft.toRequest())
        if (result is Result.Success) {
            val dto = result.data
            val categoryEntity = dto.category?.let { CategoryEntity(id = it.id, name = it.name) }
            if (categoryEntity != null) {
                categoryDao.upsertCategory(categoryEntity)
            }
            val taskEntity = dto.toEntity()
            taskDao.upsertTask(taskEntity)
            Result.Success(dto.toTaskModel())
        } else {
            Result.Error((result as Result.Error).error)
        }
    }

    override suspend fun createTaskWithSessions(
        draft: TaskDraft,
        sessions: List<SessionDraft>,
    ): Result<TaskWithSessions> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
        val result = remoteDataSource.createTaskWithSessions(draft.toRequest(sessions))
        if (result is Result.Success) {
            val dto = result.data
            val t = dto.task
            val categoryEntity = t.category?.let { CategoryEntity(id = it.id, name = it.name) }
            if (categoryEntity != null) {
                categoryDao.upsertCategory(categoryEntity)
            }
            val taskEntity = t.toEntity()
            taskDao.upsertTask(taskEntity)

            val sessionEntities = dto.sessions.map { s ->
                s.toEntity(taskId = t.id, date = "")
            }
            if (sessionEntities.isNotEmpty()) {
                sessionDao.upsertSessions(sessionEntities)
            }

            Result.Success(dto.toWithSessionsModel())
        } else {
            Result.Error((result as Result.Error).error)
        }
    }

    override suspend fun createTasksWithSessions(
        drafts: List<TaskWithSessionsDraft>,
    ): Result<List<Task>> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
        val request = BulkCreateTasksWithSessionsRequest(tasks = drafts.map { it.toRequest() })
        val result = remoteDataSource.createTasksWithSessions(request)
        if (result is Result.Success) {
            val response = result.data
            for (item in response.tasks) {
                val t = item.task
                val categoryEntity = t.category?.let { CategoryEntity(id = it.id, name = it.name) }
                if (categoryEntity != null) {
                    categoryDao.upsertCategory(categoryEntity)
                }
                val taskEntity = t.toEntity()
                taskDao.upsertTask(taskEntity)

                val sessionEntities = item.sessions.map { s ->
                    s.toEntity(taskId = t.id, date = "")
                }
                if (sessionEntities.isNotEmpty()) {
                    sessionDao.upsertSessions(sessionEntities)
                }
            }
            Result.Success(response.toModel())
        } else {
            Result.Error((result as Result.Error).error)
        }
    }

    override suspend fun proposeTasksFromText(text: String): Result<TaskProposals> =
        withContext(ioDispatcher) {
            if (!connectivityMonitor.isCurrentlyOnline()) {
                return@withContext Result.Error(AppError.Network)
            }
            remoteDataSource.proposeTasksFromText(AiTextToTasksRequest(text)).map { it.toModel() }
        }

    override suspend fun proposeTasksFromImage(
        image: ByteArray,
        mimeType: String,
        note: String?,
    ): Result<TaskProposals> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
        remoteDataSource.proposeTasksFromImage(image, mimeType, note).map { it.toModel() }
    }

    override suspend fun scheduleTask(taskId: String): Result<TaskSchedule> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
        remoteDataSource.scheduleTask(ScheduleTaskRequest(taskId)).map { it.toScheduleModel() }
    }

    override suspend fun completeTask(taskId: String): Result<Task> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
        val result = remoteDataSource.completeTask(taskId)
        if (result is Result.Success) {
            val response = result.data
            val t = response.task
            val taskEntity = t.toEntity()
            taskDao.upsertTask(taskEntity)

            // Update sessions if any were returned as completed
            val sessionEntities = response.completedSessions.map { s ->
                s.toEntity(taskId = t.id, date = "")
            }
            if (sessionEntities.isNotEmpty()) {
                sessionDao.upsertSessions(sessionEntities)
            }

            // Publish rewards if any were awarded
            response.reward?.let { rewardDto ->
                eventBus.publishSessionReward(rewardDto.toDomain())
            }

            Result.Success(t.toTaskModel())
        } else {
            Result.Error((result as Result.Error).error)
        }
    }

    override suspend fun moveTask(taskId: String, goalId: String?): Result<Task> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
        // Moving a task is an update of its goalId.
        // The backend's v1/tasks/{taskId} PATCH endpoint accepts goalId in the body.
        val request = com.awan.app.core.network.dto.task.TaskUpdateRequest(goalId = goalId)
        val result = remoteDataSource.updateTask(taskId, request)
        when (result) {
            is Result.Success -> {
                val dto = result.data
                val taskEntity = dto.toEntity()
                taskDao.upsertTask(taskEntity)
                Result.Success(dto.toTaskModel())
            }
            is Result.Error -> Result.Error(result.error)
            Result.Loading -> Result.Loading
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
        val result = remoteDataSource.deleteTask(taskId)
        if (result is Result.Success) {
            taskDao.deleteTask(taskId)
            Result.Success(Unit)
        } else {
            Result.Error((result as Result.Error).error)
        }
    }

    override suspend fun getInboxTasks(): Result<List<TaskWithSessions>> = withContext(ioDispatcher) {
        remoteDataSource.getInboxTasks().map { list -> list.map { it.toWithSessionsModel() } }
    }
}
