package com.awan.app.core.data.goal

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.goal.remote.GoalRemoteDataSource
import com.awan.app.core.data.goal.remote.GoalRemoteDataSourceImpl
import com.awan.app.core.network.api.GoalApiService
import com.awan.app.core.network.dto.GoalInfoResponse
import com.awan.app.core.network.dto.GoalStatusDto
import com.awan.app.core.network.dto.PageResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GoalRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }

    private open class FakeGoalApiService : GoalApiService {
        override suspend fun listGoals(
            status: String?,
            includeInbox: Boolean,
            expand: Boolean,
        ): PageResponse<GoalInfoResponse> = error("Not implemented")

        override suspend fun decomposeGoal(
            request: kotlinx.serialization.json.JsonObject,
        ): com.awan.app.core.network.dto.GoalDecomposeResponse = error("Not implemented")

        override suspend fun confirmDecomposition(
            sessionId: String,
        ): GoalInfoResponse = error("Not implemented")
    }

    private class FakeGoalRemoteDataSource(
        var response: Result<List<GoalInfoResponse>> = Result.Success(emptyList()),
    ) : GoalRemoteDataSource {
        override suspend fun getGoals(): Result<List<GoalInfoResponse>> = response

        override suspend fun continueDecomposition(
            request: com.awan.app.core.network.dto.GoalDecomposeRequest,
        ): Result<com.awan.app.core.network.dto.GoalDecomposeResponse> = error("Not implemented")

        override suspend fun confirmDecomposition(
            sessionId: String,
        ): Result<GoalInfoResponse> = error("Not implemented")
    }

    @Test
    fun `remote data source returns page content with expected default arguments`() = runTest(testDispatcher) {
        var capturedStatus: String? = "NON_NULL"
        var capturedIncludeInbox: Boolean? = null
        var capturedExpand: Boolean? = null

        val api = object : FakeGoalApiService() {
            override suspend fun listGoals(
                status: String?,
                includeInbox: Boolean,
                expand: Boolean,
            ): PageResponse<GoalInfoResponse> {
                capturedStatus = status
                capturedIncludeInbox = includeInbox
                capturedExpand = expand
                return PageResponse(
                    content = listOf(
                        GoalInfoResponse(id = "g-1", title = "Goal 1", status = GoalStatusDto.ACTIVE)
                    ),
                    totalElements = 1,
                )
            }
        }
        val remoteDataSource = GoalRemoteDataSourceImpl(api, json, testDispatcher)

        val result = remoteDataSource.getGoals()

        assertTrue(result is Result.Success)
        val content = (result as Result.Success).data
        assertEquals(1, content.size)
        assertEquals("g-1", content[0].id)
        assertEquals(null, capturedStatus)
        assertEquals(false, capturedIncludeInbox)
        assertEquals(true, capturedExpand)
    }

    @Test
    fun `repository maps remote source success result to domain models`() = runTest(testDispatcher) {
        val remoteGoals = listOf(
            GoalInfoResponse(
                id = "goal-1",
                title = "🎯 Active Goal",
                status = GoalStatusDto.ACTIVE,
            ),
        )
        val remote = FakeGoalRemoteDataSource(Result.Success(remoteGoals))
        val repository = GoalRepositoryImpl(remote)

        val result = repository.getGoals()

        assertTrue(result is Result.Success)
        val goals = (result as Result.Success).data
        assertEquals(1, goals.size)
        assertEquals("goal-1", goals[0].id)
        assertEquals("Active Goal", goals[0].title)
    }

    @Test
    fun `Result Error is propagated unchanged`() = runTest(testDispatcher) {
        val expectedError = Result.Error(AppError.Network)
        val remote = FakeGoalRemoteDataSource(expectedError)
        val repository = GoalRepositoryImpl(remote)

        val result = repository.getGoals()

        assertEquals(expectedError, result)
    }

    @Test(expected = CancellationException::class)
    fun `cancellation from the API is rethrown by remote data source`() = runTest(testDispatcher) {
        val api = object : FakeGoalApiService() {
            override suspend fun listGoals(
                status: String?,
                includeInbox: Boolean,
                expand: Boolean,
            ): PageResponse<GoalInfoResponse> {
                throw CancellationException("Cancelled")
            }
        }
        val remoteDataSource = GoalRemoteDataSourceImpl(api, json, testDispatcher)

        remoteDataSource.getGoals()
    }
}
