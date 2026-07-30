package com.awan.app.core.domain.goal.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalDecompositionReply

interface GoalRepository {
    suspend fun getGoals(): Result<List<Goal>>

    suspend fun continueDecomposition(
        sessionId: String?,
        message: String,
    ): Result<GoalDecompositionReply>

    suspend fun confirmDecomposition(sessionId: String): Result<Goal>
}
