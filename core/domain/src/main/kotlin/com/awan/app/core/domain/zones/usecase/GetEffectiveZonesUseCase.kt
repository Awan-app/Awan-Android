package com.awan.app.core.domain.zones.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.repository.ZonesRepository
import com.awan.app.core.model.DailyZone
import javax.inject.Inject

class GetEffectiveZonesUseCase @Inject constructor(
    private val zonesRepository: ZonesRepository
) {
    suspend operator fun invoke(date: String): Result<List<DailyZone>> =
        zonesRepository.getEffectiveZones(date)
}
