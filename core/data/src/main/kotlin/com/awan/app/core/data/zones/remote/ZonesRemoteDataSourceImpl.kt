package com.awan.app.core.data.zones.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.ZonesApiService
import com.awan.app.core.network.dto.CreateOverrideRequest
import com.awan.app.core.network.dto.CreateTemplateRequest
import com.awan.app.core.network.dto.TemplateOverrideDto
import com.awan.app.core.network.dto.UpdateZonesRequest
import com.awan.app.core.network.dto.WeeklyTemplateDto
import com.awan.app.core.network.dto.ZoneDto
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

    override suspend fun updateTemplateZones(
        templateId: String,
        request: UpdateZonesRequest
    ): Result<List<ZoneDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.updateTemplateZones(templateId, request)
        }

    override suspend fun deleteTemplate(templateId: String): Result<Unit> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.deleteTemplate(templateId)
        }

    override suspend fun getEffectiveZones(date: String): Result<List<ZoneDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.getEffectiveZones(date)
        }

    override suspend fun createOverride(request: CreateOverrideRequest): Result<TemplateOverrideDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.createOverride(request)
        }

    override suspend fun updateOverrideZones(
        overrideId: String,
        request: UpdateZonesRequest
    ): Result<List<ZoneDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.updateOverrideZones(overrideId, request)
        }

    override suspend fun deleteOverride(overrideId: String): Result<Unit> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            zonesApiService.deleteOverride(overrideId)
        }
}
