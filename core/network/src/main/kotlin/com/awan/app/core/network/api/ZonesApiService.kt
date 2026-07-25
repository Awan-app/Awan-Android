package com.awan.app.core.network.api

import com.awan.app.core.network.dto.auth.SessionDto
import com.awan.app.core.network.dto.zone.CreateOverrideRequest
import com.awan.app.core.network.dto.zone.CreateTemplateRequest
import com.awan.app.core.network.dto.zone.CreateZoneRequest
import com.awan.app.core.network.dto.zone.TemplateOverrideDto
import com.awan.app.core.network.dto.zone.UpdateOverrideRequest
import com.awan.app.core.network.dto.zone.UpdateTemplateRequest
import com.awan.app.core.network.dto.zone.UpdateZoneRequest
import com.awan.app.core.network.dto.zone.UpdateZonesRequest
import com.awan.app.core.network.dto.zone.WeeklyTemplateDto
import com.awan.app.core.network.dto.zone.ZoneDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ZonesApiService {

    ///////// Templates /////////////

    // List Templates
    @GET("v1/templates")
    suspend fun getTemplates(): List<WeeklyTemplateDto>

    // Create Template
    @POST("v1/templates")
    suspend fun createTemplate(@Body request: CreateTemplateRequest): WeeklyTemplateDto

    // Get Template
    @GET("v1/templates/{templateId}")
    suspend fun getTemplate(@Path("templateId") templateId: String): WeeklyTemplateDto

    // Update Template (name + daysOfWeek only — zones are managed separately)
    @PUT("v1/templates/{templateId}")
    suspend fun updateTemplate(
        @Path("templateId") templateId: String,
        @Body request: UpdateTemplateRequest
    ): WeeklyTemplateDto

    // Delete Template
    @DELETE("v1/templates/{templateId}")
    suspend fun deleteTemplate(@Path("templateId") templateId: String)

    // Add Zone to Template
    @POST("v1/templates/{templateId}/zones")
    suspend fun addZoneToTemplate(
        @Path("templateId") templateId: String,
        @Body request: CreateZoneRequest
    ): ZoneDto

    // Get Template Zones
    @GET("v1/templates/{templateId}/zones")
    suspend fun getTemplateZones(@Path("templateId") templateId: String): List<ZoneDto>

    // Bulk Update Template Zones (replace-all)
    @PUT("v1/templates/{templateId}/zones")
    suspend fun updateTemplateZones(
        @Path("templateId") templateId: String,
        @Body request: UpdateZonesRequest
    ): List<ZoneDto>

    ///////// Template Overrides /////////////

    // Create Override
    @POST("v1/template-overrides")
    suspend fun createOverride(@Body request: CreateOverrideRequest): TemplateOverrideDto

    // List Overrides
    @GET("v1/template-overrides")
    suspend fun getOverrides(): List<TemplateOverrideDto>

    // Get Override
    @GET("v1/template-overrides/{overrideId}")
    suspend fun getOverride(@Path("overrideId") overrideId: String): TemplateOverrideDto

    // Update Override (name + dateOfDay only — zones are managed separately)
    @PUT("v1/template-overrides/{overrideId}")
    suspend fun updateOverride(
        @Path("overrideId") overrideId: String,
        @Body request: UpdateOverrideRequest
    ): TemplateOverrideDto

    // Delete Override
    @DELETE("v1/template-overrides/{overrideId}")
    suspend fun deleteOverride(@Path("overrideId") overrideId: String)

    // Add Zone to Override
    @POST("v1/template-overrides/{overrideId}/zones")
    suspend fun addZoneToOverride(
        @Path("overrideId") overrideId: String,
        @Body request: CreateZoneRequest
    ): ZoneDto

    // Get Override Zones
    @GET("v1/template-overrides/{overrideId}/zones")
    suspend fun getOverrideZones(@Path("overrideId") overrideId: String): List<ZoneDto>

    // Bulk Update Override Zones (replace-all)
    @PUT("v1/template-overrides/{overrideId}/zones")
    suspend fun updateOverrideZones(
        @Path("overrideId") overrideId: String,
        @Body request: UpdateZonesRequest
    ): List<ZoneDto>

    ///////// Zones /////////////

    // Get Zone
    @GET("v1/zones/{zoneId}")
    suspend fun getZone(@Path("zoneId") zoneId: String): ZoneDto

    // Get Zone Sessions
    @GET("v1/zones/{zoneId}/sessions")
    suspend fun getZoneSessions(@Path("zoneId") zoneId: String): List<SessionDto>

    // Get Effective Zones by Date (override takes priority over template)
    @GET("v1/zones/date/{date}")
    suspend fun getEffectiveZones(@Path("date") date: String): List<ZoneDto>

    // Update Zone
    @PUT("v1/zones/{zoneId}")
    suspend fun updateZone(
        @Path("zoneId") zoneId: String,
        @Body request: UpdateZoneRequest
    ): ZoneDto

    // Delete Zone
    @DELETE("v1/zones/{zoneId}")
    suspend fun deleteZone(@Path("zoneId") zoneId: String)
}