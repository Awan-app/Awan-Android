package com.awan.app.core.network.api

import com.awan.app.core.network.dto.ZoneDto
import retrofit2.http.GET
import retrofit2.http.Path

interface ZoneApiService {

    /** [date] is `YYYY-MM-DD`. Empty list when no template or override covers that day. */
    @GET("v1/zones/date/{date}")
    suspend fun getZonesByDate(
        @Path("date") date: String,
    ): List<ZoneDto>
}
