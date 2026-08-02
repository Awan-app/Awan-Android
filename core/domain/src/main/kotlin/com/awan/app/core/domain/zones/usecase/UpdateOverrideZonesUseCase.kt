package com.awan.app.core.domain.zones.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.repository.ZonesRepository
import com.awan.app.core.domain.zones.model.DailyZone
import javax.inject.Inject

class UpdateOverrideZonesUseCase @Inject constructor(
    private val zonesRepository: ZonesRepository
) {
    suspend operator fun invoke(overrideId: String, zones: List<DailyZone>): Result<List<DailyZone>> =
        zonesRepository.updateOverrideZones(overrideId, zones)
}
