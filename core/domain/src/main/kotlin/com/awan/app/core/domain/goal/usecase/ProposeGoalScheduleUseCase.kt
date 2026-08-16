package com.awan.app.core.domain.goal.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.repository.GoalRepository
import com.awan.app.core.model.GoalScheduleProposal
import javax.inject.Inject

class ProposeGoalScheduleUseCase @Inject constructor(
    private val repository: GoalRepository,
) {
    suspend operator fun invoke(goalId: String): Result<GoalScheduleProposal> {
        return repository.proposeGoalSchedule(goalId)
    }
}
