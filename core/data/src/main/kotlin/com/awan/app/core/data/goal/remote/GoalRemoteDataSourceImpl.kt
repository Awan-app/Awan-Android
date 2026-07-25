package com.awan.app.core.data.goal.remote

import com.awan.app.core.network.api.GoalApiService
import com.awan.app.core.network.dto.GoalInfoResponse
import javax.inject.Inject

internal class GoalRemoteDataSourceImpl @Inject constructor(
    private val goalApiService: GoalApiService
) : GoalRemoteDataSource {

    override suspend fun getGoals(): List<GoalInfoResponse> {
        return goalApiService.listGoals().content
    }
}
