package com.awan.feature.onboarding.impl.presentation

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.template.repository.TemplateRepository
import com.awan.app.core.model.Zone

class FakeTemplateRepository : TemplateRepository {
    var createdZones: List<Zone>? = null

    override suspend fun createWeeklyTemplate(zones: List<Zone>): Result<Unit> {
        createdZones = zones
        return Result.Success(Unit)
    }
}
