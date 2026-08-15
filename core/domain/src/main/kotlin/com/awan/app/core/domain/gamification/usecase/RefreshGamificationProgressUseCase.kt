package com.awan.app.core.domain.gamification.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.gamification.model.GamificationProgress
import com.awan.app.core.domain.gamification.repository.GamificationRepository
import javax.inject.Inject

class RefreshGamificationProgressUseCase @Inject constructor(
    private val gamificationRepository: GamificationRepository,
) {
    suspend operator fun invoke(): Result<GamificationProgress> =
        gamificationRepository.refreshProgress()
}
