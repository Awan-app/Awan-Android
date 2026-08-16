package com.awan.app.core.domain.goal.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.model.Goal
import javax.inject.Inject

class CreateGoalUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    suspend operator fun invoke(
        title: String,
        description: String? = null,
        targetDate: String? = null,
    ): Result<Goal> = repository.createGoal(title, description, targetDate)
}
