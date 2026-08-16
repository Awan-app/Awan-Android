package com.awan.app.core.domain.goal.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalProposal
import javax.inject.Inject

/** Saves a goal without tasks for drafts, or saves the customized/approved AI proposal for scheduling. */
class SaveGoalProposalUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    suspend operator fun invoke(
        sessionId: String,
        proposal: GoalProposal,
        addTasks: Boolean,
    ): Result<Goal> {
        val tasksToSave = if (addTasks) proposal.tasks else emptyList()
        val result = repository.createGoal(
            title = proposal.title,
            description = proposal.description,
            targetDate = proposal.targetDate,
            tasks = tasksToSave,
        )
        if (result is Result.Success) {
            repository.cancelDecomposition(sessionId)
        }
        return result
    }
}