package com.awan.app.core.domain.zones.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.repository.ZonesRepository
import javax.inject.Inject

class DeleteWeeklyTemplateUseCase @Inject constructor(
    private val zonesRepository: ZonesRepository
) {
    suspend operator fun invoke(templateId: String): Result<Unit> =
        zonesRepository.deleteTemplate(templateId)
}
