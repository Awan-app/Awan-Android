package com.awan.app.core.data.calendar.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.GoalResponse
import com.awan.app.core.network.dto.UserProfileResponse

interface CalendarRemoteDataSource {
    suspend fun getUserProfile(): Result<UserProfileResponse>
    suspend fun getActiveGoals(): Result<List<GoalResponse>>
}
