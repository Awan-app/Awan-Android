package com.awan.app.core.network.api

import com.awan.app.core.network.dto.gamification.GamificationProgressDto
import com.awan.app.core.network.dto.gamification.WheelConfigDto
import com.awan.app.core.network.dto.gamification.WheelSpinDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface GamificationApiService {

    @GET("v1/gamification/progress")
    suspend fun getProgress(): GamificationProgressDto

    @GET("v1/gamification/activity-dates")
    suspend fun getActivityDates(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
    ): List<String>

    @GET("v1/gamification/wheel/config")
    suspend fun getWheelConfig(): WheelConfigDto

    @POST("v1/gamification/wheel/spin")
    suspend fun spinWheel(): WheelSpinDto
}
