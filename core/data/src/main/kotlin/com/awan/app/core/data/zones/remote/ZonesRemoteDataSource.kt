package com.awan.app.core.data.zones.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.session.SessionDto
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

interface ZonesRemoteDataSource {
    // Templates
    suspend fun getTemplates(): Result<List<WeeklyTemplateDto>>
    suspend fun createTemplate(request: CreateTemplateRequest): Result<WeeklyTemplateDto>
    suspend fun getTemplate(templateId: String): Result<WeeklyTemplateDto>
    suspend fun updateTemplate(templateId: String, request: UpdateTemplateRequest): Result<WeeklyTemplateDto>
    suspend fun deleteTemplate(templateId: String): Result<Unit>
    suspend fun addZoneToTemplate(templateId: String, request: CreateZoneRequest): Result<ZoneDto>
    suspend fun getTemplateZones(templateId: String): Result<List<ZoneDto>>
    suspend fun updateTemplateZones(templateId: String, request: UpdateZonesRequest): Result<List<ZoneDto>>

    // Overrides
    suspend fun createOverride(request: CreateOverrideRequest): Result<TemplateOverrideDto>
    suspend fun getOverrides(): Result<List<TemplateOverrideDto>>
    suspend fun getOverride(overrideId: String): Result<TemplateOverrideDto>
    suspend fun updateOverride(overrideId: String, request: UpdateOverrideRequest): Result<TemplateOverrideDto>
    suspend fun deleteOverride(overrideId: String): Result<Unit>
    suspend fun addZoneToOverride(overrideId: String, request: CreateZoneRequest): Result<ZoneDto>
    suspend fun getOverrideZones(overrideId: String): Result<List<ZoneDto>>
    suspend fun updateOverrideZones(overrideId: String, request: UpdateZonesRequest): Result<List<ZoneDto>>

    // Zones
    suspend fun getZone(zoneId: String): Result<ZoneDto>
    suspend fun getZoneSessions(zoneId: String): Result<List<SessionDto>>
    suspend fun getEffectiveZones(date: String): Result<List<ZoneDto>>
    suspend fun updateZone(zoneId: String, request: UpdateZoneRequest): Result<ZoneDto>
    suspend fun deleteZone(zoneId: String): Result<Unit>
}
