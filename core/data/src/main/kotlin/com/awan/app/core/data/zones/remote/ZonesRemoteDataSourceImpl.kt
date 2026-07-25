package com.awan.app.core.data.zones.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.ZonesApiService
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
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ZonesRemoteDataSourceImpl @Inject constructor(
    private val zonesApiService: ZonesApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO)
    private val ioDispatcher: CoroutineDispatcher,
) : ZonesRemoteDataSource {

    override suspend fun getTemplates(): Result<List<WeeklyTemplateDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.getTemplates()
        }

    override suspend fun createTemplate(request: CreateTemplateRequest): Result<WeeklyTemplateDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.createTemplate(request)
        }

    override suspend fun getTemplate(templateId: String): Result<WeeklyTemplateDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.getTemplate(templateId)
        }

    override suspend fun updateTemplate(
        templateId: String,
        request: UpdateTemplateRequest
    ): Result<WeeklyTemplateDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.updateTemplate(templateId, request)
        }

    override suspend fun deleteTemplate(templateId: String): Result<Unit> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.deleteTemplate(templateId)
        }

    override suspend fun addZoneToTemplate(
        templateId: String,
        request: CreateZoneRequest
    ): Result<ZoneDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.addZoneToTemplate(templateId, request)
        }

    override suspend fun getTemplateZones(templateId: String): Result<List<ZoneDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.getTemplateZones(templateId)
        }

    override suspend fun updateTemplateZones(
        templateId: String,
        request: UpdateZonesRequest
    ): Result<List<ZoneDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.updateTemplateZones(templateId, request)
        }

    override suspend fun createOverride(request: CreateOverrideRequest): Result<TemplateOverrideDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.createOverride(request)
        }

    override suspend fun getOverrides(): Result<List<TemplateOverrideDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.getOverrides()
        }

    override suspend fun getOverride(overrideId: String): Result<TemplateOverrideDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.getOverride(overrideId)
        }

    override suspend fun updateOverride(
        overrideId: String,
        request: UpdateOverrideRequest
    ): Result<TemplateOverrideDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.updateOverride(overrideId, request)
        }

    override suspend fun deleteOverride(overrideId: String): Result<Unit> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.deleteOverride(overrideId)
        }

    override suspend fun addZoneToOverride(
        overrideId: String,
        request: CreateZoneRequest
    ): Result<ZoneDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.addZoneToOverride(overrideId, request)
        }

    override suspend fun getOverrideZones(overrideId: String): Result<List<ZoneDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.getOverrideZones(overrideId)
        }

    override suspend fun updateOverrideZones(
        overrideId: String,
        request: UpdateZonesRequest
    ): Result<List<ZoneDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.updateOverrideZones(overrideId, request)
        }

    override suspend fun getZone(zoneId: String): Result<ZoneDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.getZone(zoneId)
        }

    override suspend fun getZoneSessions(zoneId: String): Result<List<SessionDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.getZoneSessions(zoneId)
        }

    override suspend fun getEffectiveZones(date: String): Result<List<ZoneDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.getEffectiveZones(date)
        }

    override suspend fun updateZone(
        zoneId: String,
        request: UpdateZoneRequest
    ): Result<ZoneDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.updateZone(zoneId, request)
        }

    override suspend fun deleteZone(zoneId: String): Result<Unit> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.deleteZone(zoneId)
        }
}
