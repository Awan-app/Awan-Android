package com.awan.app.core.data.goal.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.GoalDecomposeRequest
import com.awan.app.core.network.dto.GoalDecomposeResponse
import com.awan.app.core.network.dto.GoalInfoResponse

interface GoalRemoteDataSource {
    suspend fun getGoals(): Result<List<GoalInfoResponse>>

    suspend fun continueDecomposition(request: GoalDecomposeRequest): Result<GoalDecomposeResponse>

    suspend fun confirmDecomposition(sessionId: String): Result<GoalInfoResponse>
}
