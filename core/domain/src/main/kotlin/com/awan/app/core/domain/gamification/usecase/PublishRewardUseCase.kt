package com.awan.app.core.domain.gamification.usecase

import com.awan.app.core.domain.gamification.model.RewardEvent
import com.awan.app.core.domain.gamification.repository.GamificationRepository
import javax.inject.Inject

class PublishRewardUseCase @Inject constructor(
    private val gamificationRepository: GamificationRepository,
) {
    operator fun invoke(event: RewardEvent) = gamificationRepository.publishReward(event)
}
