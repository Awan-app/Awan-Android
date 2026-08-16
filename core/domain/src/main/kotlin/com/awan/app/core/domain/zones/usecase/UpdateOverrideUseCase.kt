package com.awan.app.core.domain.zones.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.model.TemplateOverride
import com.awan.app.core.domain.zones.repository.ZonesRepository
import javax.inject.Inject

class UpdateOverrideUseCase @Inject constructor(
    private val zonesRepository: ZonesRepository
) {
    suspend operator fun invoke(overrideId: String, name: String?, date: String): Result<TemplateOverride> =
        zonesRepository.updateOverride(overrideId, name, date)
}
