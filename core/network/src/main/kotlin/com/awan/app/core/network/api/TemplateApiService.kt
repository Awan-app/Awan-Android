package com.awan.app.core.network.api

import com.awan.app.core.network.dto.CreateTemplateOverrideRequest
import com.awan.app.core.network.dto.CreateZoneRequest
import com.awan.app.core.network.dto.TemplateDto
import com.awan.app.core.network.dto.TemplateOverrideResponseDto
import com.awan.app.core.network.dto.ZoneDto
import com.awan.app.core.network.dto.CreateTemplateRequest
import com.awan.app.core.network.dto.TemplateResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface TemplateApiService {

    @GET("v1/templates")
    suspend fun getTemplates(): List<TemplateDto>

    @GET("v1/template-overrides")
    suspend fun getTemplateOverrides(): List<TemplateOverrideResponseDto>

    @POST("v1/template-overrides")
    suspend fun createTemplateOverride(
        @Body request: CreateTemplateOverrideRequest,
    ): TemplateOverrideResponseDto

    @POST("v1/template-overrides/{overrideId}/zones")
    suspend fun addZoneToOverride(
        @Path("overrideId") overrideId: String,
        @Body request: CreateZoneRequest,
    ): ZoneDto
    @POST("v1/templates")
    suspend fun createTemplate(
        @Body request: CreateTemplateRequest,
    ): TemplateResponse
}
