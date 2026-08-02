package com.awan.app.core.data.task

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.task.remote.TaskRemoteDataSource
import com.awan.app.core.data.util.minutesOfDay
import com.awan.app.core.domain.task.repository.AiTaskRepository
import com.awan.app.core.model.FirstTask
import com.awan.app.core.model.TaskWithSessions
import com.awan.app.core.model.toSessionDraft
import com.awan.app.core.network.dto.AiTextToTasksRequest
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

    /** No separate `/schedule/task` step: the proposal already carries availability-grounded timing. */
    override suspend fun createAndScheduleTask(title: String): Result<FirstTask?> = withContext(ioDispatcher) {
        val proposal = when (val result = remoteDataSource.proposeTasksFromText(AiTextToTasksRequest(title))) {
            is Result.Success -> result.data.tasks.firstOrNull()?.toModel() ?: return@withContext Result.Success(null)
            is Result.Error -> return@withContext Result.Error(result.error)
            Result.Loading -> return@withContext Result.Loading
        }

        val request = proposal.draft.toRequest(proposal.sessions.map { it.toSessionDraft() })
        when (val result = remoteDataSource.createTaskWithSessions(request)) {
            is Result.Success -> Result.Success(result.data.toModel().toFirstTask())
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
