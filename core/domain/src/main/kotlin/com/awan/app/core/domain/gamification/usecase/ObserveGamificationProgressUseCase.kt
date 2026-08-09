package com.awan.app.core.domain.gamification.usecase

import com.awan.app.core.domain.gamification.model.GamificationProgress
import com.awan.app.core.domain.gamification.repository.GamificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveGamificationProgressUseCase @Inject constructor(
    private val gamificationRepository: GamificationRepository,
) {
    operator fun invoke(): Flow<GamificationProgress> = gamificationRepository.observeProgress()
}
