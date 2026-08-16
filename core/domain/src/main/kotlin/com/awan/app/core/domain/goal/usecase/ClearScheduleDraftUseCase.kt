package com.awan.app.core.domain.goal.usecase

import com.awan.app.core.domain.goal.repository.GoalRepository
import javax.inject.Inject

class ClearScheduleDraftUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    suspend operator fun invoke(goalId: String) {
        repository.clearScheduleDraft(goalId)
    }
}
