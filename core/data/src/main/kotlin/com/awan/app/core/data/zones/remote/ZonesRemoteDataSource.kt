package com.awan.app.core.data.zones.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.CreateOverrideRequest
import com.awan.app.core.network.dto.CreateTemplateRequest
import com.awan.app.core.network.dto.TemplateOverrideDto
import com.awan.app.core.network.dto.UpdateZonesRequest
import com.awan.app.core.network.dto.WeeklyTemplateDto
import com.awan.app.core.network.dto.ZoneDto

interface ZonesRemoteDataSource {
    suspend fun getTemplates(): Result<List<WeeklyTemplateDto>>
    suspend fun createTemplate(request: CreateTemplateRequest): Result<WeeklyTemplateDto>
    suspend fun updateTemplateZones(templateId: String, request: UpdateZonesRequest): Result<List<ZoneDto>>
    suspend fun deleteTemplate(templateId: String): Result<Unit>
    suspend fun getEffectiveZones(date: String): Result<List<ZoneDto>>
    suspend fun createOverride(request: CreateOverrideRequest): Result<TemplateOverrideDto>
    suspend fun updateOverrideZones(overrideId: String, request: UpdateZonesRequest): Result<List<ZoneDto>>
    suspend fun deleteOverride(overrideId: String): Result<Unit>
}
