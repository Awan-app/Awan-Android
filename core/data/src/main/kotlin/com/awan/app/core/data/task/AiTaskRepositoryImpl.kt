package com.awan.app.core.data.task

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.database.dao.SessionDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.model.SessionEntity
import com.awan.app.core.database.model.TaskEntity
import com.awan.app.core.data.task.remote.TaskRemoteDataSource
import com.awan.app.core.data.util.minutesOfDay
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.domain.task.repository.AiTaskRepository
import com.awan.app.core.domain.onboarding.model.FirstTask
import com.awan.app.core.model.TaskWithSessions
import com.awan.app.core.model.toSessionDraft
import com.awan.app.core.network.dto.task.AiTextToTasksRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiTaskRepositoryImpl @Inject constructor(
    private val remoteDataSource: TaskRemoteDataSource,
    private val taskDao: TaskDao,
    private val sessionDao: SessionDao,
    private val connectivityMonitor: NetworkConnectivityMonitor,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : AiTaskRepository {

    /** No separate `/schedule/task` step: the proposal already carries availability-grounded timing. */
    override suspend fun createAndScheduleTask(title: String): Result<FirstTask?> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }

        val proposal = when (val result = remoteDataSource.proposeTasksFromText(AiTextToTasksRequest(title))) {
            is Result.Success -> result.data.tasks.firstOrNull()?.toModel() ?: return@withContext Result.Success(null)
            is Result.Error -> return@withContext Result.Error(result.error)
            Result.Loading -> return@withContext Result.Loading
        }

        val request = proposal.draft.toRequest(proposal.sessions.map { it.toSessionDraft() })
        when (val result = remoteDataSource.createTaskWithSessions(request)) {
            is Result.Success -> {
                val dto = result.data
                val t = dto.task
                val taskEntity = TaskEntity(
                    id = t.id,
                    title = t.title,
                    description = t.description,
                    estimatedDuration = t.estimatedDuration ?: 0,
                    status = t.status ?: "SCHEDULED",
                    mandatory = t.mandatory ?: false,
                    estimatedPoints = t.estimatedPoints ?: 0,
                    allowTaskSplitting = t.allowTaskSplitting ?: false,
                    goalId = t.goalId?.takeIf { it.isNotBlank() },
                    categoryId = t.category?.id,
                )
                taskDao.upsertTask(taskEntity)

                val sessionEntities = dto.sessions.map { s ->
                    SessionEntity(
                        id = s.id,
                        taskId = s.taskId ?: t.id,
                        zoneId = s.zoneId,
                        date = if (s.start.length >= 10) s.start.substring(0, 10) else "",
                        startTime = if (s.start.length >= 19) s.start.substring(11, 19) else "00:00:00",
                        endTime = if (s.end.length >= 19) s.end.substring(11, 19) else "00:00:00",
                        status = s.status ?: "SCHEDULED",
                        locked = s.locked,
                    )
                }
                if (sessionEntities.isNotEmpty()) {
                    sessionDao.upsertSessions(sessionEntities)
                }
                val model = dto.toWithSessionsModel()
                Result.Success(model.toFirstTask())
            }
            is Result.Error -> Result.Error(result.error)
            Result.Loading -> Result.Loading
        }
    }

    private fun TaskWithSessions.toFirstTask(): FirstTask? {
        val session = sessions.minByOrNull { it.start } ?: return null
        return FirstTask(
            id = task.id,
            title = task.title,
            zoneId = session.zoneId.orEmpty(),
            startMinutes = session.start.minutesOfDay(),
            durationMinutes = Duration.between(session.start, session.end).toMinutes().toInt(),
        )
    }
}
