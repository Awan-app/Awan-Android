package com.awan.app.core.data.zones.repository

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.data.zone.toModel
import com.awan.app.core.data.zones.mapper.toDomain
import com.awan.app.core.data.zones.mapper.toDto
import com.awan.app.core.data.zones.remote.ZonesRemoteDataSource
import com.awan.app.core.database.dao.TemplateDao
import com.awan.app.core.database.dao.TemplateOverrideDao
import com.awan.app.core.database.dao.ZoneDao
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.Session
import com.awan.app.core.domain.zones.model.TemplateOverride
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.app.core.domain.zones.repository.ZonesRepository
import com.awan.app.core.model.DayZone
import com.awan.app.core.network.dto.zone.CreateOverrideRequest
import com.awan.app.core.network.dto.zone.CreateTemplateRequest
import com.awan.app.core.network.dto.zone.CreateZoneRequest
import com.awan.app.core.network.dto.zone.UpdateOverrideRequest
import com.awan.app.core.network.dto.zone.UpdateTemplateRequest
import com.awan.app.core.network.dto.zone.UpdateZoneRequest
import com.awan.app.core.network.dto.zone.UpdateZonesRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ZonesRepositoryImpl @Inject constructor(
    private val zonesRemoteDataSource: ZonesRemoteDataSource,
    private val zoneDao: ZoneDao,
    private val templateDao: TemplateDao,
    private val templateOverrideDao: TemplateOverrideDao,
    private val connectivityMonitor: NetworkConnectivityMonitor,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ZonesRepository {

    private fun checkOnline(): Result<Nothing>? {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return Result.Error(AppError.Network)
        }
        return null
    }

    override suspend fun getZonesForDate(date: LocalDate): Result<List<DayZone>> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.getEffectiveZones(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .map { zones -> zones.mapNotNull { it.toModel() } }
    }

    override suspend fun getTemplates(): Result<List<WeeklyTemplate>> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.getTemplates().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun createTemplate(
        name: String,
        daysOfWeek: List<DayOfWeek>,
        zones: List<DailyZone>,
    ): Result<WeeklyTemplate> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.createTemplate(
            CreateTemplateRequest(
                name = name,
                daysOfWeek = daysOfWeek.map { it.name },
                zones = zones.map { it.toDto() }
            )
        ).map { it.toDomain() }
    }

    override suspend fun getTemplate(templateId: String): Result<WeeklyTemplate> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.getTemplate(templateId).map { it.toDomain() }
    }

    override suspend fun updateTemplate(
        templateId: String,
        name: String,
        daysOfWeek: List<DayOfWeek>,
    ): Result<WeeklyTemplate> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.updateTemplate(
            templateId,
            UpdateTemplateRequest(
                name = name,
                daysOfWeek = daysOfWeek.map { it.name }
            )
        ).map { it.toDomain() }
    }

    override suspend fun deleteTemplate(templateId: String): Result<Unit> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.deleteTemplate(templateId)
    }

    override suspend fun addZoneToTemplate(
        templateId: String,
        zone: DailyZone,
    ): Result<DailyZone> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.addZoneToTemplate(
            templateId,
            CreateZoneRequest(
                name = zone.name,
                startTime = zone.startTime,
                endTime = zone.endTime,
                color = zone.color,
                categoryId = zone.categoryId
            )
        ).map { it.toDomain() }
    }

    override suspend fun getTemplateZones(templateId: String): Result<List<DailyZone>> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.getTemplateZones(templateId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun updateTemplateZones(
        templateId: String,
        zones: List<DailyZone>,
    ): Result<List<DailyZone>> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.updateTemplateZones(
            templateId,
            UpdateZonesRequest(zones = zones.map { it.toDto() })
        ).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun createOverride(
        date: String,
        zones: List<DailyZone>,
    ): Result<TemplateOverride> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.createOverride(
            CreateOverrideRequest(
                dateOfDay = date,
                zones = zones.map { it.toDto() }
            )
        ).map { it.toDomain() }
    }

    override suspend fun getOverrides(): Result<List<TemplateOverride>> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.getOverrides().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getOverride(overrideId: String): Result<TemplateOverride> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.getOverride(overrideId).map { it.toDomain() }
    }

    override suspend fun updateOverride(
        overrideId: String,
        name: String?,
        date: String,
    ): Result<TemplateOverride> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.updateOverride(
            overrideId,
            UpdateOverrideRequest(
                name = name,
                dateOfDay = date
            )
        ).map { it.toDomain() }
    }

    override suspend fun deleteOverride(overrideId: String): Result<Unit> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.deleteOverride(overrideId)
    }

    override suspend fun addZoneToOverride(
        overrideId: String,
        zone: DailyZone,
    ): Result<DailyZone> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.addZoneToOverride(
            overrideId,
            CreateZoneRequest(
                name = zone.name,
                startTime = zone.startTime,
                endTime = zone.endTime,
                color = zone.color,
                categoryId = zone.categoryId
            )
        ).map { it.toDomain() }
    }

    override suspend fun getOverrideZones(overrideId: String): Result<List<DailyZone>> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.getOverrideZones(overrideId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun updateOverrideZones(
        overrideId: String,
        zones: List<DailyZone>,
    ): Result<List<DailyZone>> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.updateOverrideZones(
            overrideId,
            UpdateZonesRequest(zones = zones.map { it.toDto() })
        ).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getZone(zoneId: String): Result<DailyZone> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.getZone(zoneId).map { it.toDomain() }
    }

    override suspend fun getZoneSessions(zoneId: String): Result<List<Session>> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.getZoneSessions(zoneId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getEffectiveZones(date: String): Result<List<DailyZone>> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.getEffectiveZones(date).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun updateZone(
        zoneId: String,
        zone: DailyZone,
    ): Result<DailyZone> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.updateZone(
            zoneId,
            UpdateZoneRequest(
                name = zone.name,
                startTime = zone.startTime,
                endTime = zone.endTime,
                color = zone.color,
                categoryId = zone.categoryId
            )
        ).map { it.toDomain() }
    }

    override suspend fun deleteZone(zoneId: String): Result<Unit> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.deleteZone(zoneId)
    }
}
