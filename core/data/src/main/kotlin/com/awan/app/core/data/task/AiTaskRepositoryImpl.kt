package com.awan.app.core.data.task

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.task.remote.TaskRemoteDataSource
import com.awan.app.core.data.util.minutesOfDay
import com.awan.app.core.data.util.parseIsoDateTime
import com.awan.app.core.domain.task.repository.AiTaskRepository
import com.awan.app.core.model.FirstTask
import com.awan.app.core.network.dto.CreateTaskWithAiRequest
import com.awan.app.core.network.dto.ScheduleTaskRequest
import com.awan.app.core.network.dto.ScheduledSessionResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiTaskRepositoryImpl @Inject constructor(
    private val remoteDataSource: TaskRemoteDataSource,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : AiTaskRepository {

    override suspend fun createAndScheduleTask(title: String): Result<FirstTask?> = withContext(ioDispatcher) {
        val created = when (val result = remoteDataSource.createTaskWithAi(CreateTaskWithAiRequest(title = title))) {
            is Result.Success -> result.data
            is Result.Error -> return@withContext Result.Error(result.error)
            Result.Loading -> return@withContext Result.Loading
        }

        val request = ScheduleTaskRequest(taskId = created.id, horizonDays = HORIZON_DAYS)
        when (val result = remoteDataSource.scheduleTask(request)) {
            is Result.Success -> {
                val session = result.data.scheduledSessions.orEmpty().firstOrNull()
                Result.Success(session?.let { toFirstTask(it, created.id, created.title) })
            }
            is Result.Error -> Result.Error(result.error)
            Result.Loading -> Result.Loading
        }
    }

    private fun toFirstTask(
        session: ScheduledSessionResponse,
        taskId: String,
        title: String,
    ): FirstTask? {
        val start = session.start?.let(::parseIsoDateTime) ?: return null
        val end = session.end?.let(::parseIsoDateTime) ?: return null
        return FirstTask(
            id = taskId,
            title = title,
            zoneId = session.zoneId.orEmpty(),
            startMinutes = start.minutesOfDay(),
            durationMinutes = Duration.between(start, end).toMinutes().toInt(),
        )
    }

    private companion object {
        const val HORIZON_DAYS = 14
    }
}
