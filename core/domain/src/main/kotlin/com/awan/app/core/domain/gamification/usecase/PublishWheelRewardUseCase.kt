package com.awan.app.core.domain.gamification.usecase

import com.awan.app.core.domain.gamification.model.WheelSpinResult
import com.awan.app.core.domain.gamification.repository.GamificationRepository
import javax.inject.Inject

class PublishWheelRewardUseCase @Inject constructor(
    private val gamificationRepository: GamificationRepository,
) {
    suspend operator fun invoke(result: WheelSpinResult) =
        gamificationRepository.publishWheelReward(result)
}
