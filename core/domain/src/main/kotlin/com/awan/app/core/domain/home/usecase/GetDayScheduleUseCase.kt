package com.awan.app.core.domain.home.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.home.model.DaySchedule
import com.awan.app.core.domain.home.repository.HomeRepository
import java.time.LocalDate
import javax.inject.Inject

class GetDayScheduleUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
) {
    suspend operator fun invoke(date: LocalDate): Result<DaySchedule> =
        homeRepository.getDaySchedule(date)
}
