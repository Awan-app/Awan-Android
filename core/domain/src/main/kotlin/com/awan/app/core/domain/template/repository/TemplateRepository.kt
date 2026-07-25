package com.awan.app.core.domain.template.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.model.Zone

interface TemplateRepository {

    /**
     * Creates the user's weekly template — the [zones] they configured, applied to all seven days.
     * Only enabled zones are sent; the server owns the generated ids, so nothing is returned.
     */
    suspend fun createWeeklyTemplate(zones: List<Zone>): Result<Unit>
}
