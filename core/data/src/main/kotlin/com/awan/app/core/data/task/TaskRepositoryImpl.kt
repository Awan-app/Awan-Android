package com.awan.app.core.data.task

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.data.task.remote.TaskRemoteDataSource
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
import com.awan.app.core.network.dto.task.ScheduleTaskRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val remoteDataSource: TaskRemoteDataSource,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : TaskRepository {

    override suspend fun createTask(draft: TaskDraft): Result<Task> = withContext(ioDispatcher) {
        remoteDataSource.createTask(draft.toRequest()).map { it.toTaskModel() }
    }

    override suspend fun createTaskWithSessions(
        draft: TaskDraft,
        sessions: List<SessionDraft>,
    ): Result<TaskWithSessions> = withContext(ioDispatcher) {
        remoteDataSource.createTaskWithSessions(draft.toRequest(sessions)).map { it.toWithSessionsModel() }
    }

    override suspend fun createTasksWithSessions(
        drafts: List<TaskWithSessionsDraft>,
    ): Result<List<Task>> = withContext(ioDispatcher) {
        val request = BulkCreateTasksWithSessionsRequest(tasks = drafts.map { it.toRequest() })
        remoteDataSource.createTasksWithSessions(request).map { it.toModel() }
    }

    override suspend fun proposeTasksFromText(text: String): Result<TaskProposals> =
        withContext(ioDispatcher) {
            remoteDataSource.proposeTasksFromText(AiTextToTasksRequest(text)).map { it.toModel() }
        }

    override suspend fun proposeTasksFromImage(
        image: ByteArray,
        mimeType: String,
        note: String?,
    ): Result<TaskProposals> = withContext(ioDispatcher) {
        remoteDataSource.proposeTasksFromImage(image, mimeType, note).map { it.toModel() }
    }

    override suspend fun scheduleTask(taskId: String): Result<TaskSchedule> = withContext(ioDispatcher) {
        remoteDataSource.scheduleTask(ScheduleTaskRequest(taskId)).map { it.toScheduleModel() }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> = withContext(ioDispatcher) {
        remoteDataSource.deleteTask(taskId)
    }

    override suspend fun getInboxTasks(): Result<List<TaskWithSessions>> = withContext(ioDispatcher) {
        remoteDataSource.getInboxTasks().map { list -> list.map { it.toWithSessionsModel() } }
    }
}
