package com.awan.app.core.domain.gamification.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.gamification.model.WheelConfig
import com.awan.app.core.domain.gamification.repository.GamificationRepository
import javax.inject.Inject

class GetWheelConfigUseCase @Inject constructor(
    private val gamificationRepository: GamificationRepository,
) {
    suspend operator fun invoke(): Result<WheelConfig> = gamificationRepository.getWheelConfig()
}
