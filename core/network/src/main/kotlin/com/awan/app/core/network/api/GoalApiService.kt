package com.awan.app.core.network.api

import com.awan.app.core.network.dto.GoalInfoResponse
import com.awan.app.core.network.dto.PageResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GoalApiService {

    @GET("v1/goals")
    suspend fun listGoals(
        @Query("status") status: String? = null,
        @Query("includeInbox") includeInbox: Boolean = false,
        @Query("expand") expand: Boolean = true,
    ): PageResponse<GoalInfoResponse>
}
