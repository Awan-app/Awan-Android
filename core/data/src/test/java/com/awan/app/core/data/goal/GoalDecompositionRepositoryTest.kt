package com.awan.app.core.data.goal

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.goal.remote.GoalRemoteDataSource
import com.awan.app.core.data.goal.remote.GoalRemoteDataSourceImpl
import com.awan.app.core.network.api.GoalApiService
import com.awan.app.core.network.dto.GoalDecomposeRequest
import com.awan.app.core.network.dto.GoalDecomposeResponse
import com.awan.app.core.network.dto.GoalInfoResponse
import com.awan.app.core.network.dto.GoalStatusDto
import com.awan.app.core.network.dto.PageResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.awan.app.core.database.dao.GoalDao
import com.awan.app.core.database.model.GoalEntity

/**
 * Tests for the remote/repository layer:
 *   - First and continuation requests pass exact session/message
 *   - Confirm passes the exact sessionId and maps the returned goal
 *   - Result.Error propagates unchanged
 *   - CancellationException is rethrown from both API methods
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GoalDecompositionRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }

    private val onlineMonitor = object : com.awan.app.core.domain.network.NetworkConnectivityMonitor {
        override val isOnline: Flow<Boolean> = flowOf(true)
        override fun isCurrentlyOnline(): Boolean = true
    }


    // --- Fake API service stub ---

    private open class FakeGoalApiService : GoalApiService {
        override suspend fun listGoals(
            status: String?,
            includeInbox: Boolean,
            expand: Boolean,
        ): PageResponse<GoalInfoResponse> = error("Not implemented")

        override suspend fun createGoal(request: com.awan.app.core.network.dto.goal.CreateGoalRequest): GoalInfoResponse = error("Not implemented")
        override suspend fun getInboxGoal(): GoalInfoResponse = error("Not implemented")
        override suspend fun getGoal(goalId: String, expand: Boolean): GoalInfoResponse = error("Not implemented")
        override suspend fun deleteGoal(goalId: String) = error("Not implemented")

        override suspend fun decomposeGoal(
            request: com.awan.app.core.network.dto.GoalDecomposeRequest,
        ): GoalDecomposeResponse = error("Not implemented")

        override suspend fun confirmDecomposition(
            sessionId: String,
        ): GoalInfoResponse = error("Not implemented")

        override suspend fun getDecompositionTranscript(sessionId: String): com.awan.app.core.network.dto.goal.GoalDecompositionTranscriptResponse = error("Not implemented")
        override suspend fun cancelDecomposition(sessionId: String) = error("Not implemented")
        override suspend fun scheduleGoal(request: com.awan.app.core.network.dto.goal.ScheduleGoalRequest): com.awan.app.core.network.dto.task.TaskScheduleResponse = error("Not implemented")
        override suspend fun proposeGoalSchedule(request: com.awan.app.core.network.dto.goal.ScheduleGoalRequest): com.awan.app.core.network.dto.goal.AiGoalScheduleProposalResponse = error("Not implemented")
        override suspend fun confirmGoalSchedule(request: com.awan.app.core.network.dto.goal.ConfirmAiScheduleRequest) = error("Not implemented")
    }


    private val emptyDecomposeResponse = GoalDecomposeResponse(
        sessionId = "sess-returned",
        blocks = emptyList(),
        hasProposal = false,
    )

    /** No-op goal DAO – decomposition tests do not exercise Room. */
    private val noOpGoalDao = object : GoalDao {
        override suspend fun upsertGoal(goal: GoalEntity) {}
        override suspend fun upsertGoals(goals: List<GoalEntity>) {}
        override fun observeAllGoals(): Flow<List<GoalEntity>> = flowOf(emptyList())
        override suspend fun getAllGoals(): List<GoalEntity> = emptyList()
        override fun observeGoalsByStatus(status: String): Flow<List<GoalEntity>> = flowOf(emptyList())
        override fun observeGoal(goalId: String): Flow<GoalEntity?> = MutableStateFlow(null)
        override suspend fun getGoal(goalId: String): GoalEntity? = null
        override fun observeInboxGoal(): Flow<GoalEntity?> = MutableStateFlow(null)
        override suspend fun deleteGoal(goalId: String) {}
        override suspend fun getActiveNonInboxGoalIds(): List<String> = emptyList()
        override suspend fun getMinExpiryTime(): Long? = null
    }

    // --- C. Remote data source / repository behavior ---

    @Test
    fun `first continue call passes null sessionId and exact message`() = runTest(testDispatcher) {
        var capturedRequest: JsonObject? = null
        val api = object : FakeGoalApiService() {
            override suspend fun decomposeGoal(request: JsonObject): GoalDecomposeResponse {
                capturedRequest = request
                return emptyDecomposeResponse
            }
        }
        val ds = GoalRemoteDataSourceImpl(api, json, testDispatcher)
        ds.continueDecomposition(GoalDecomposeRequest(sessionId = null, message = "Learn Spanish"))

        assertEquals(JsonNull, capturedRequest?.get("sessionId"))
        assertEquals(JsonPrimitive("Learn Spanish"), capturedRequest?.get("message"))
    }

    @Test
    fun `continuation call passes the returned sessionId and exact message`() = runTest(testDispatcher) {
        var capturedRequest: JsonObject? = null
        val api = object : FakeGoalApiService() {
            override suspend fun decomposeGoal(request: JsonObject): GoalDecomposeResponse {
                capturedRequest = request
                return emptyDecomposeResponse
            }
        }
        val ds = GoalRemoteDataSourceImpl(api, json, testDispatcher)
        ds.continueDecomposition(GoalDecomposeRequest(sessionId = "sess-abc", message = "Three months"))

        assertEquals(JsonPrimitive("sess-abc"), capturedRequest?.get("sessionId"))
        assertEquals(JsonPrimitive("Three months"), capturedRequest?.get("message"))
    }

    @Test
    fun `confirm passes exact sessionId and returns mapped goal`() = runTest(testDispatcher) {
        var capturedSessionId: String? = null
        val api = object : FakeGoalApiService() {
            override suspend fun confirmDecomposition(sessionId: String): GoalInfoResponse {
                capturedSessionId = sessionId
                return GoalInfoResponse(id = "goal-1", title = "🎯 Learn Spanish", status = GoalStatusDto.ACTIVE)
            }
        }
        val ds = GoalRemoteDataSourceImpl(api, json, testDispatcher)
        val result = ds.confirmDecomposition("sess-confirm-123")

        assertEquals("sess-confirm-123", capturedSessionId)
        assertTrue(result is Result.Success)
        val info = (result as Result.Success).data
        assertEquals("goal-1", info.id)
    }

    private open class FakeGoalRemoteDataSource : GoalRemoteDataSource {
        override suspend fun getGoals(): Result<List<GoalInfoResponse>> = Result.Success(emptyList())
        override suspend fun createGoal(request: com.awan.app.core.network.dto.goal.CreateGoalRequest): Result<GoalInfoResponse> = error("Not implemented")
        override suspend fun getInboxGoal(): Result<GoalInfoResponse> = error("Not implemented")
        override suspend fun getGoal(goalId: String): Result<GoalInfoResponse> = error("Not implemented")
        override suspend fun deleteGoal(goalId: String): Result<Unit> = error("Not implemented")
        override suspend fun continueDecomposition(request: GoalDecomposeRequest): Result<GoalDecomposeResponse> = error("Not implemented")
        override suspend fun confirmDecomposition(sessionId: String): Result<GoalInfoResponse> = error("Not implemented")
        override suspend fun getDecompositionTranscript(sessionId: String): Result<com.awan.app.core.network.dto.goal.GoalDecompositionTranscriptResponse> = error("Not implemented")
        override suspend fun cancelDecomposition(sessionId: String): Result<Unit> = error("Not implemented")
        override suspend fun scheduleGoal(goalId: String): Result<com.awan.app.core.network.dto.task.TaskScheduleResponse> = error("Not implemented")
        override suspend fun proposeGoalSchedule(goalId: String): Result<com.awan.app.core.network.dto.goal.AiGoalScheduleProposalResponse> = error("Not implemented")
        override suspend fun confirmGoalSchedule(request: com.awan.app.core.network.dto.goal.ConfirmAiScheduleRequest): Result<Unit> = error("Not implemented")
    }

    @Test
    fun `Result Error from decomposeGoal propagates unchanged through repository`() = runTest(testDispatcher) {
        val expectedError = Result.Error(AppError.Network)

        val fakeDs = object : FakeGoalRemoteDataSource() {
            override suspend fun continueDecomposition(request: GoalDecomposeRequest): Result<GoalDecomposeResponse> =
                expectedError
        }
        val repository = GoalRepositoryImpl(fakeDs, noOpGoalDao, onlineMonitor)
        val result = repository.continueDecomposition(sessionId = null, message = "Test")

        assertEquals(expectedError, result)
    }

    @Test
    fun `Result Error from confirmDecomposition propagates unchanged through repository`() = runTest(testDispatcher) {
        val expectedError = Result.Error(AppError.Unauthorized)

        val fakeDs = object : FakeGoalRemoteDataSource() {
            override suspend fun confirmDecomposition(sessionId: String): Result<GoalInfoResponse> =
                expectedError
        }
        val repository = GoalRepositoryImpl(fakeDs, noOpGoalDao, onlineMonitor)

        val result = repository.confirmDecomposition(sessionId = "sess-x")


        assertEquals(expectedError, result)
    }

    @Test(expected = CancellationException::class)
    fun `CancellationException from decomposeGoal is rethrown by remote data source`() = runTest(testDispatcher) {
        val api = object : FakeGoalApiService() {
            override suspend fun decomposeGoal(request: JsonObject): GoalDecomposeResponse {
                throw CancellationException("Cancelled")
            }
        }
        val ds = GoalRemoteDataSourceImpl(api, json, testDispatcher)
        ds.continueDecomposition(GoalDecomposeRequest(sessionId = null, message = "test"))
    }

    @Test(expected = CancellationException::class)
    fun `CancellationException from confirmDecomposition is rethrown by remote data source`() = runTest(testDispatcher) {
        val api = object : FakeGoalApiService() {
            override suspend fun confirmDecomposition(sessionId: String): GoalInfoResponse {
                throw CancellationException("Cancelled")
            }
        }
        val ds = GoalRemoteDataSourceImpl(api, json, testDispatcher)
        ds.confirmDecomposition("sess-cancel")
    }
}
