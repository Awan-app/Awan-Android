package com.awan.app.core.domain.home.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.home.model.DaySchedule
import com.awan.app.core.domain.home.repository.HomeRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetDayScheduleUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
) {
    operator fun invoke(date: LocalDate): Flow<Result<DaySchedule>> =
        homeRepository.getDaySchedule(date)
}
