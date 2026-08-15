package com.awan.app.core.domain.gamification.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.gamification.model.WheelSpinResult
import com.awan.app.core.domain.gamification.repository.GamificationRepository
import javax.inject.Inject

class SpinWheelUseCase @Inject constructor(
    private val gamificationRepository: GamificationRepository,
) {
    suspend operator fun invoke(): Result<WheelSpinResult> = gamificationRepository.spinWheel()
}
