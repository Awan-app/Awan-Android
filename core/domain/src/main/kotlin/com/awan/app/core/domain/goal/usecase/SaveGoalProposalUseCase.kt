package com.awan.app.core.domain.goal.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalProposal
import javax.inject.Inject

/** Saves a proposal with either zero tasks (draft) or every proposed task. */
class SaveGoalProposalUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    suspend operator fun invoke(
        sessionId: String,
        proposal: GoalProposal,
        addTasks: Boolean,
    ): Result<Goal> {
        val created = repository.createGoal(
            title = proposal.title,
            description = proposal.description,
            targetDate = proposal.targetDate,
            tasks = if (addTasks) proposal.tasks else emptyList(),
        )
        if (created is Result.Success) {
            // Goal creation is durable; a failed cleanup must not report a failed save.
            repository.cancelDecomposition(sessionId)
        }
        return created
    }
}
