package com.awan.app.core.domain.zone.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zone.repository.ZoneRepository
import com.awan.app.core.model.DayZone
import java.time.LocalDate
import javax.inject.Inject

class GetZonesForDateUseCase @Inject constructor(
    private val zoneRepository: ZoneRepository,
) {
    suspend operator fun invoke(date: LocalDate): Result<List<DayZone>> =
        zoneRepository.getZonesForDate(date)
}
