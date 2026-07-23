package com.awan.app.core.data.zone.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.ZoneDto

interface ZoneRemoteDataSource {
    /** [date] is `YYYY-MM-DD`. */
    suspend fun getZonesByDate(date: String): Result<List<ZoneDto>>
}
