package com.awan.app.core.data.goal.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.GoalApiService
import com.awan.app.core.network.dto.GoalDecomposeRequest
import com.awan.app.core.network.dto.GoalDecomposeResponse
import com.awan.app.core.network.dto.GoalInfoResponse
import com.awan.app.core.network.dto.goal.AiConfirmedSessionItemDto
import com.awan.app.core.network.dto.goal.AiGoalScheduleProposalResponse
import com.awan.app.core.network.dto.goal.ConfirmAiScheduleRequest
import com.awan.app.core.network.dto.goal.CreateGoalRequest
import com.awan.app.core.network.dto.goal.GoalDecompositionTranscriptResponse
import com.awan.app.core.network.dto.goal.ScheduleGoalRequest
import com.awan.app.core.network.dto.goal.UpdateGoalRequest
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.task.TaskScheduleResponse
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
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

    override suspend fun createGoal(request: CreateGoalRequest): Result<GoalInfoResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            goalApiService.createGoal(request)
        }

    override suspend fun getInboxGoal(): Result<GoalInfoResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            goalApiService.getInboxGoal()
        }

    override suspend fun getGoal(goalId: String): Result<GoalInfoResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            goalApiService.getGoal(goalId)
        }

    override suspend fun updateGoal(
        goalId: String,
        request: UpdateGoalRequest,
    ): Result<GoalInfoResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            goalApiService.updateGoal(goalId, request)
        }

    override suspend fun deleteGoal(goalId: String): Result<Unit> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            goalApiService.deleteGoal(goalId)
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

    override suspend fun getDecompositionTranscript(
        sessionId: String,
    ): Result<GoalDecompositionTranscriptResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            goalApiService.getDecompositionTranscript(sessionId)
        }

    override suspend fun cancelDecomposition(sessionId: String): Result<Unit> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            goalApiService.cancelDecomposition(sessionId)
        }

    override suspend fun scheduleGoal(goalId: String): Result<TaskScheduleResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            goalApiService.scheduleGoal(ScheduleGoalRequest(goalId))
        }

    override suspend fun proposeGoalSchedule(goalId: String): Result<AiGoalScheduleProposalResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            goalApiService.proposeGoalSchedule(ScheduleGoalRequest(goalId))
        }

    override suspend fun confirmGoalSchedule(request: ConfirmAiScheduleRequest): Result<List<AiConfirmedSessionItemDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            val jsonElement = goalApiService.confirmGoalSchedule(request)
            parseConfirmedSessions(jsonElement, json)
        }

    private fun parseConfirmedSessions(
        element: JsonElement,
        json: Json,
    ): List<AiConfirmedSessionItemDto> {
        return when (element) {
            is JsonArray -> {
                element.map { item ->
                    json.decodeFromJsonElement<AiConfirmedSessionItemDto>(item)
                }
            }
            is JsonObject -> {
                val sessionsArray = element["sessions"] as? JsonArray
                    ?: element["scheduledSessions"] as? JsonArray
                    ?: element["data"] as? JsonArray
                sessionsArray?.map { item ->
                    json.decodeFromJsonElement<AiConfirmedSessionItemDto>(item)
                } ?: emptyList()
            }
            else -> emptyList()
        }
    }
}
