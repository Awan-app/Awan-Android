package com.awan.app.core.domain.zones.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.repository.ZonesRepository
import com.awan.app.core.model.DailyZone
import javax.inject.Inject

class UpdateTemplateZonesUseCase @Inject constructor(
    private val zonesRepository: ZonesRepository
) {
    suspend operator fun invoke(templateId: String, zones: List<DailyZone>): Result<List<DailyZone>> =
        zonesRepository.updateTemplateZones(templateId, zones)
}
