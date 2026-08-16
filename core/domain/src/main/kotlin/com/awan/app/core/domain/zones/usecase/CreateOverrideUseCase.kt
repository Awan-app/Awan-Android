package com.awan.app.core.domain.zones.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.TemplateOverride
import com.awan.app.core.domain.zones.repository.ZonesRepository
import javax.inject.Inject

class CreateOverrideUseCase @Inject constructor(
    private val zonesRepository: ZonesRepository
) {
    suspend operator fun invoke(date: String, zones: List<DailyZone>, name: String? = null): Result<TemplateOverride> =
        zonesRepository.createOverride(date, zones, name)
}
