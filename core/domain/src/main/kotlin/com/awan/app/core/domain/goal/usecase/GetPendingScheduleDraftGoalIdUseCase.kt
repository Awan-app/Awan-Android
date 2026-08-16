package com.awan.app.core.domain.goal.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.repository.GoalRepository
import javax.inject.Inject

class GetPendingScheduleDraftGoalIdUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    suspend operator fun invoke(): Result<String?> = repository.getPendingScheduleDraftGoalId()
}