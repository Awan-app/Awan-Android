package com.awan.app.core.network.api

import com.awan.app.core.network.dto.GoalDecomposeRequest
import com.awan.app.core.network.dto.GoalDecomposeResponse
import com.awan.app.core.network.dto.GoalInfoResponse
import com.awan.app.core.network.dto.PageResponse
import com.awan.app.core.network.dto.goal.AiGoalScheduleProposalResponse
import com.awan.app.core.network.dto.goal.ConfirmAiScheduleRequest
import com.awan.app.core.network.dto.goal.CreateGoalRequest
import com.awan.app.core.network.dto.goal.GoalDecompositionTranscriptResponse
import com.awan.app.core.network.dto.goal.ScheduleGoalRequest
import com.awan.app.core.network.dto.goal.UpdateGoalRequest
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.task.TaskScheduleResponse
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GoalApiService {
    @GET("v1/goals")
    suspend fun listGoals(
        @Query("status") status: String? = null,
        @Query("includeInbox") includeInbox: Boolean = false,
        @Query("expand") expand: Boolean = true,
    ): PageResponse<GoalInfoResponse>

    @POST("v1/goals")
    suspend fun createGoal(
        @Body request: CreateGoalRequest,
    ): GoalInfoResponse

    @GET("v1/goals/inbox")
    suspend fun getInboxGoal(): GoalInfoResponse

    @GET("v1/goals/{goalId}")
    suspend fun getGoal(
        @Path("goalId") goalId: String,
        @Query("expand") expand: Boolean = false,
    ): GoalInfoResponse

    @PATCH("v1/goals/{goalId}")
    suspend fun updateGoal(
        @Path("goalId") goalId: String,
        @Body request: UpdateGoalRequest,
    ): GoalInfoResponse

    @DELETE("v1/goals/{goalId}")
    suspend fun deleteGoal(
        @Path("goalId") goalId: String,
    )

    /** `POST v1/ai/goal-decompose` ΓÇö continue or start a decomposition session. */
    @POST("v1/ai/goal-decompose")
    suspend fun decomposeGoal(
        @Body request: GoalDecomposeRequest,
    ): GoalDecomposeResponse

    /** `POST v1/ai/goal-decompose/{sessionId}/confirm` ΓÇö confirm the proposal and create the goal. */
    @POST("v1/ai/goal-decompose/{sessionId}/confirm")
    suspend fun confirmDecomposition(
        @Path("sessionId") sessionId: String,
    ): GoalInfoResponse

    /** `GET v1/ai/goal-decompose/{sessionId}` ΓÇö get full decomposition transcript. */
    @GET("v1/ai/goal-decompose/{sessionId}")
    suspend fun getDecompositionTranscript(
        @Path("sessionId") sessionId: String,
    ): GoalDecompositionTranscriptResponse

    /** `POST v1/ai/goal-decompose/{sessionId}/cancel` ΓÇö cancel active decomposition session. */
    @POST("v1/ai/goal-decompose/{sessionId}/cancel")
    suspend fun cancelDecomposition(
        @Path("sessionId") sessionId: String,
    )

    /** `POST v1/schedule` ΓÇö schedule all unscheduled tasks for a goal. */
    @POST("v1/schedule")
    suspend fun scheduleGoal(
        @Body request: ScheduleGoalRequest,
    ): TaskScheduleResponse

    /** `POST v1/ai/schedule` ΓÇö proposal for scheduling goal tasks. */
    @POST("v1/ai/schedule")
    suspend fun proposeGoalSchedule(
        @Body request: ScheduleGoalRequest,
    ): AiGoalScheduleProposalResponse

    /** `POST v1/ai/schedule/confirm` ΓÇö confirm accepted proposed sessions. */
    @POST("v1/ai/schedule/confirm")
    suspend fun confirmGoalSchedule(
        @Body request: ConfirmAiScheduleRequest,
    ): kotlinx.serialization.json.JsonElement
}
