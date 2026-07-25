package com.awan.app.core.data.goal

import com.awan.app.core.common.result.Result
import com.awan.app.core.data.goal.remote.GoalRemoteDataSource
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.model.Goal
import javax.inject.Inject

internal class GoalRepositoryImpl @Inject constructor(
    private val remoteDataSource: GoalRemoteDataSource,
) : GoalRepository {

    override suspend fun getGoals(): Result<List<Goal>> =
        try {
            val networkGoals = remoteDataSource.getGoals()
            Result.Success(networkGoals.map { it.toModel() })
        } catch (e: Exception) {
            Result.Error(com.awan.app.core.common.error.AppError.Unknown(e))
        }
}
