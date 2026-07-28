package com.awan.app.core.network.api

import com.awan.app.core.network.dto.ZoneDto
import retrofit2.http.GET
import retrofit2.http.Path

interface ZonesApiService {

    @GET("v1/zones/date/{date}")
    suspend fun getZonesByDate(
        @Path("date") date: String,
    ): List<ZoneDto>
}
