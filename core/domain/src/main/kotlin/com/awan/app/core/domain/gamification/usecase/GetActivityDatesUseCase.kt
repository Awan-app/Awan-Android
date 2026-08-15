package com.awan.app.core.domain.gamification.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.gamification.repository.GamificationRepository
import java.time.LocalDate
import javax.inject.Inject

class GetActivityDatesUseCase @Inject constructor(
    private val gamificationRepository: GamificationRepository,
) {
    suspend operator fun invoke(startDate: LocalDate, endDate: LocalDate): Result<Set<LocalDate>> =
        gamificationRepository.getActivityDates(startDate = startDate, endDate = endDate)
}
