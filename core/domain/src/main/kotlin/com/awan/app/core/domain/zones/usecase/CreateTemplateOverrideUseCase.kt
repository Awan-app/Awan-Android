package com.awan.app.core.domain.zones.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.repository.ZonesRepository
import com.awan.app.core.model.DailyZone
import com.awan.app.core.model.TemplateOverride
import javax.inject.Inject

class CreateTemplateOverrideUseCase @Inject constructor(
    private val zonesRepository: ZonesRepository
) {
    suspend operator fun invoke(date: String, zones: List<DailyZone>): Result<TemplateOverride> =
        zonesRepository.createOverride(date, zones)
}
