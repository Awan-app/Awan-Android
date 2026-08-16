package com.awan.app.core.domain.goal.usecase

import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.model.Goal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveGoalUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    operator fun invoke(goalId: String): Flow<Goal?> = repository.observeGoal(goalId)
}
