package com.awan.app.core.domain.zones.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.app.core.domain.zones.repository.ZonesRepository
import javax.inject.Inject

class UpdateWeeklyTemplateUseCase @Inject constructor(
    private val zonesRepository: ZonesRepository
) {
    suspend operator fun invoke(
        templateId: String,
        name: String,
        daysOfWeek: List<DayOfWeek>
    ): Result<WeeklyTemplate> = zonesRepository.updateTemplate(templateId, name, daysOfWeek)
}
