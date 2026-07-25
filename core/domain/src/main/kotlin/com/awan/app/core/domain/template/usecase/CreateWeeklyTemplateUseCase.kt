package com.awan.app.core.domain.template.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.template.repository.TemplateRepository
import com.awan.app.core.model.Zone
import javax.inject.Inject

class CreateWeeklyTemplateUseCase @Inject constructor(
    private val templateRepository: TemplateRepository,
) {
    suspend operator fun invoke(zones: List<Zone>): Result<Unit> =
        templateRepository.createWeeklyTemplate(zones)
}
