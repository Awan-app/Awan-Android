package com.awan.app.core.domain.gamification.usecase

import com.awan.app.core.domain.gamification.model.RewardEvent
import com.awan.app.core.domain.gamification.repository.GamificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRewardEventsUseCase @Inject constructor(
    private val gamificationRepository: GamificationRepository,
) {
    operator fun invoke(): Flow<RewardEvent> = gamificationRepository.observeRewards()
}
