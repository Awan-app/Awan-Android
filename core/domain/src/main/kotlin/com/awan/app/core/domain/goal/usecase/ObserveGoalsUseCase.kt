package com.awan.app.core.domain.goal.usecase

import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.model.Goal
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveGoalsUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    operator fun invoke(): Flow<List<Goal>> = repository.observeGoals()
}
