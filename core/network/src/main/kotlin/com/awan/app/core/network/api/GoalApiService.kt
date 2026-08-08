package com.awan.app.core.network.api

import com.awan.app.core.network.dto.GoalDecomposeRequest
import com.awan.app.core.network.dto.GoalDecomposeResponse
import com.awan.app.core.network.dto.GoalInfoResponse
import com.awan.app.core.network.dto.PageResponse
import retrofit2.http.Body
import retrofit2.http.GET
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

    /** `POST v1/ai/goal-decompose` — continue or start a decomposition session. */
    @POST("v1/ai/goal-decompose")
    suspend fun decomposeGoal(
        @Body request: GoalDecomposeRequest,
    ): GoalDecomposeResponse

    /** `POST v1/ai/goal-decompose/{sessionId}/confirm` — confirm the proposal and create the goal. */
    @POST("v1/ai/goal-decompose/{sessionId}/confirm")
    suspend fun confirmDecomposition(
        @Path("sessionId") sessionId: String,
    ): GoalInfoResponse
}
