package com.awan.app.core.domain.template.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.model.Zone

interface TemplateRepository {

    /**
     * Creates the user's weekly template — the [zones] they configured, applied to all seven days.
     * Only enabled zones are sent. Returns the created zones carrying the server's ids, which is
     * what a scheduled session's `zoneId` refers to.
     */
    suspend fun createWeeklyTemplate(zones: List<Zone>): Result<List<Zone>>
}
