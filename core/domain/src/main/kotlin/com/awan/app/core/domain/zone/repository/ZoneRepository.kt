package com.awan.app.core.domain.zone.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.model.DayZone
import java.time.LocalDate

interface ZoneRepository {

    /** The zones actually in effect on [date] — override first, then weekly template, else empty. */
    suspend fun getZonesForDate(date: LocalDate): Result<List<DayZone>>
}
