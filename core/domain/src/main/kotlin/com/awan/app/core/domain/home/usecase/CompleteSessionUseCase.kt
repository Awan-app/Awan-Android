package com.awan.app.core.domain.home.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.gamification.model.SessionReward
import com.awan.app.core.domain.home.repository.HomeRepository
import javax.inject.Inject

class CompleteSessionUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
) {
    suspend operator fun invoke(sessionId: String): Result<SessionReward> =
        homeRepository.completeSession(sessionId)
}
