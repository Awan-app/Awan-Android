package com.awan.app.core.data.goal

import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.data.goal.remote.GoalRemoteDataSource
import com.awan.app.core.database.dao.GoalDao
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalDecompositionReply
import com.awan.app.core.network.dto.GoalDecomposeRequest
import javax.inject.Inject

/**
 * Reads goals exclusively from the local Room database (SSOT).
 * Remote data is injected into Room by [OfflineSyncCoordinator]; this
 * repository never performs a remote GET for UI reads.
 *
 * Mutations (decompose, confirm) still go to the network and persist
 * the result into Room before returning.
 */
class GoalRepositoryImpl @Inject constructor(
    private val remoteDataSource: GoalRemoteDataSource,
    private val goalDao: GoalDao,
) : GoalRepository {

    override suspend fun getGoals(): Result<List<Goal>> {
        val entities = goalDao.getAllGoals()
        return Result.Success(entities.map { it.toModel() })
    }

    override suspend fun continueDecomposition(
        sessionId: String?,
        message: String,
    ): Result<GoalDecompositionReply> =
        remoteDataSource.continueDecomposition(
            GoalDecomposeRequest(sessionId = sessionId, message = message),
        ).map { it.toDecompositionReply() }

    override suspend fun confirmDecomposition(sessionId: String): Result<Goal> =
        remoteDataSource.confirmDecomposition(sessionId).map { dto ->
            // Persist confirmed goal to Room so future reads see it immediately
            goalDao.upsertGoal(dto.toEntity())
            dto.toModel()
        }
}
