package com.awan.app.core.data.goal

import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.data.goal.remote.GoalRemoteDataSource
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalDecompositionReply
import com.awan.app.core.network.dto.GoalDecomposeRequest
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val remoteDataSource: GoalRemoteDataSource,
) : GoalRepository {

    override suspend fun getGoals(): Result<List<Goal>> =
        remoteDataSource.getGoals().map { networkGoals ->
            networkGoals.map { it.toModel() }
        }

    override suspend fun continueDecomposition(
        sessionId: String?,
        message: String,
    ): Result<GoalDecompositionReply> =
        remoteDataSource.continueDecomposition(
            GoalDecomposeRequest(sessionId = sessionId, message = message),
        ).map { it.toDecompositionReply() }

    override suspend fun confirmDecomposition(sessionId: String): Result<Goal> =
        remoteDataSource.confirmDecomposition(sessionId).map { it.toModel() }
}
