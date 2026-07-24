package com.awan.app.core.network.api

import com.awan.app.core.network.dto.CreateOverrideRequest
import com.awan.app.core.network.dto.CreateTemplateRequest
import com.awan.app.core.network.dto.TemplateOverrideDto
import com.awan.app.core.network.dto.UpdateZonesRequest
import com.awan.app.core.network.dto.WeeklyTemplateDto
import com.awan.app.core.network.dto.ZoneDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ZonesApiService {

    @GET("v1/templates")
    suspend fun getTemplates(): List<WeeklyTemplateDto>

    @POST("v1/templates")
    suspend fun createTemplate(@Body request: CreateTemplateRequest): WeeklyTemplateDto

    @PUT("v1/templates/{templateId}/zones")
    suspend fun updateTemplateZones(
        @Path("templateId") templateId: String,
        @Body request: UpdateZonesRequest
    ): List<ZoneDto>

    @DELETE("v1/templates/{templateId}")
    suspend fun deleteTemplate(@Path("templateId") templateId: String)

    @GET("v1/zones/date/{date}")
    suspend fun getEffectiveZones(@Path("date") date: String): List<ZoneDto>

    @POST("v1/template-overrides")
    suspend fun createOverride(@Body request: CreateOverrideRequest): TemplateOverrideDto

    @PUT("v1/template-overrides/{overrideId}/zones")
    suspend fun updateOverrideZones(
        @Path("overrideId") overrideId: String,
        @Body request: UpdateZonesRequest
    ): List<ZoneDto>

    @DELETE("v1/template-overrides/{overrideId}")
    suspend fun deleteOverride(@Path("overrideId") overrideId: String)
}
