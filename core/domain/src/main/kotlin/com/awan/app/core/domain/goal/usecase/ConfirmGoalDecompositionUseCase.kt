package com.awan.app.core.domain.goal.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.model.Goal
import javax.inject.Inject

/**
 * Confirms the AI-proposed goal decomposition for the given session, creating the goal and its
 * tasks in the backend. Returns the created [Goal] on success.
 */
class ConfirmGoalDecompositionUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    suspend operator fun invoke(sessionId: String): Result<Goal> =
        repository.confirmDecomposition(sessionId)
}
