package com.awan.app.core.network.api

import com.awan.app.core.network.dto.GoalPageResponse
import com.awan.app.core.network.dto.UserProfileResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface CalendarApiService {
    @GET("v1/users/me")
    suspend fun getUserProfile(): UserProfileResponse

    @GET("v1/goals")
    suspend fun getGoals(
        @Query("status") status: String = "ACTIVE",
        @Query("includeInbox") includeInbox: Boolean = false,
        @Query("expand") expand: Boolean = false,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "targetDate,asc",
    ): GoalPageResponse
}
