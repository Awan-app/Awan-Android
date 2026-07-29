package com.awan.app.core.domain.goal.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.model.GoalDecompositionReply
import javax.inject.Inject

/**
 * Sends a message to the AI goal-decomposition endpoint and returns the reply.
 *
 * Pass [sessionId] = null for the first message; pass the [GoalDecompositionReply.sessionId]
 * returned by each call back into the next call to continue the session.
 */
class ContinueGoalDecompositionUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    suspend operator fun invoke(
        sessionId: String?,
        message: String,
    ): Result<GoalDecompositionReply> = repository.continueDecomposition(
        sessionId = sessionId,
        message = message,
    )
}
