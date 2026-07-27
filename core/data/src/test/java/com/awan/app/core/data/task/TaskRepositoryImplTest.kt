package com.awan.app.core.data.task

import com.awan.app.core.common.result.Result
import com.awan.app.core.data.task.remote.TaskRemoteDataSource
import com.awan.app.core.network.dto.CreateAiTaskRequest
import com.awan.app.core.network.dto.CreateTaskRequest
import com.awan.app.core.network.dto.ScheduleTaskRequest
import com.awan.app.core.network.dto.TaskInfoResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `createTask delegates request to remote data source`() = runTest(testDispatcher) {
        val fakeRemoteDataSource = object : TaskRemoteDataSource {
            override suspend fun createTask(request: CreateTaskRequest): Result<TaskInfoResponse> {
                return Result.Success(
                    TaskInfoResponse(
                        id = "t-1",
                        title = request.title,
                        estimatedDuration = request.estimatedDuration,
                    )
                )
            }

            override suspend fun createTaskWithAi(request: CreateAiTaskRequest) = error("not used")

            override suspend fun scheduleTask(request: ScheduleTaskRequest) = error("not used")
        }
        val repository = TaskRepositoryImpl(fakeRemoteDataSource, testDispatcher)
        val result = repository.createTask(title = "Read docs", estimatedDurationMinutes = 45)

        assertTrue(result is Result.Success)
        assertEquals("t-1", (result as Result.Success).data.id)
        assertEquals("Read docs", result.data.title)
    }
}
