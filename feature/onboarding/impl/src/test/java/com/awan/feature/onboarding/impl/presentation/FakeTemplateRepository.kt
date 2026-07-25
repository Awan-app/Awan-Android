package com.awan.feature.onboarding.impl.presentation

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.template.repository.TemplateRepository
import com.awan.app.core.model.Zone

class FakeTemplateRepository : TemplateRepository {
    var createdZones: List<Zone>? = null

    /** Stands in for the server handing back the same zones under its own ids. */
    override suspend fun createWeeklyTemplate(zones: List<Zone>): Result<List<Zone>> {
        createdZones = zones
        return Result.Success(zones.map { it.copy(id = SERVER_ID_PREFIX + it.id) })
    }

    companion object {
        const val SERVER_ID_PREFIX = "server-"
    }
}
