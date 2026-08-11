package com.awan.app.core.domain.goal.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.model.Goal
import javax.inject.Inject

class UpdateGoalUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    suspend operator fun invoke(
        goalId: String,
        title: String? = null,
        description: String? = null,
        status: String? = null,
        targetDate: String? = null,
    ): Result<Goal> = repository.updateGoal(goalId, title, description, status, targetDate)
}
