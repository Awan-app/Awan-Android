package com.awan.app.core.data.goal.remote

import com.awan.app.core.network.dto.GoalInfoResponse

interface GoalRemoteDataSource {
    suspend fun getGoals(): List<GoalInfoResponse>
}
