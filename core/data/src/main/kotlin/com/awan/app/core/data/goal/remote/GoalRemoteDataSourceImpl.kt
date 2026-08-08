package com.awan.app.core.data.goal.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.GoalApiService
import com.awan.app.core.network.dto.GoalDecomposeRequest
import com.awan.app.core.network.dto.GoalDecomposeResponse
import com.awan.app.core.network.dto.GoalInfoResponse
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject

class GoalRemoteDataSourceImpl @Inject constructor(
    private val goalApiService: GoalApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : GoalRemoteDataSource {

    override suspend fun getGoals(): Result<List<GoalInfoResponse>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            goalApiService.listGoals().content
        }

    override suspend fun continueDecomposition(
        request: GoalDecomposeRequest,
    ): Result<GoalDecomposeResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            goalApiService.decomposeGoal(request)
        }

    override suspend fun confirmDecomposition(
        sessionId: String,
    ): Result<GoalInfoResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            goalApiService.confirmDecomposition(sessionId)
        }
}
