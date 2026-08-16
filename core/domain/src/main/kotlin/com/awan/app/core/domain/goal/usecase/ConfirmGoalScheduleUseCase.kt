package com.awan.app.core.domain.goal.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.model.ConfirmedGoalSession
import com.awan.app.core.model.ProposedGoalSession
import javax.inject.Inject

class ConfirmGoalScheduleUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    suspend operator fun invoke(goalId: String, sessions: List<ProposedGoalSession>): Result<List<ConfirmedGoalSession>> {
        return repository.confirmGoalSchedule(goalId, sessions)
    }
}
