package com.awan.app.core.domain.zones.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.repository.ZonesRepository
import com.awan.app.core.model.WeeklyTemplate
import javax.inject.Inject

class GetWeeklyTemplatesUseCase @Inject constructor(
    private val zonesRepository: ZonesRepository
) {
    suspend operator fun invoke(): Result<List<WeeklyTemplate>> =
        zonesRepository.getTemplates()
}
