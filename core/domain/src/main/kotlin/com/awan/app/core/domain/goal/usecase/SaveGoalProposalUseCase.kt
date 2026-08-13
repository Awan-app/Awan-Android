package com.awan.app.core.domain.goal.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalProposal
import javax.inject.Inject

/** Saves the goal first, then optionally adds all proposed tasks through the bulk endpoint. */
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
            tasks = emptyList(),
        )
        if (created !is Result.Success) return created

        if (addTasks) {
            when (val tasks = repository.addTasksToGoal(created.data.id, proposal.tasks)) {
                is Result.Success -> Unit
                is Result.Error -> {
                    repository.deleteGoal(created.data.id)
                    return tasks
                }
                Result.Loading -> return Result.Loading
            }
        }

        // Goal creation is durable; cleanup failure must not report a failed save.
        repository.cancelDecomposition(sessionId)
        return created
    }
}
