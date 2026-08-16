package com.awan.app.core.data.zones.repository

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.common.result.suspendOnSuccess
import com.awan.app.core.data.zone.toModel
import com.awan.app.core.data.zones.mapper.toDomain
import com.awan.app.core.data.zones.mapper.toDto
import com.awan.app.core.data.sync.SyncTtl
import com.awan.app.core.data.zones.local.ZonesLocalDataSource
import com.awan.app.core.data.zones.remote.ZonesRemoteDataSource
import com.awan.app.core.database.model.ZoneEntity
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ZonesRepositoryImpl @Inject constructor(
    private val zonesRemoteDataSource: ZonesRemoteDataSource,
    private val zonesLocalDataSource: ZonesLocalDataSource,
    private val connectivityMonitor: NetworkConnectivityMonitor,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ZonesRepository {

    private fun checkOnline(): Result<Nothing>? {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return Result.Error(AppError.Network)
        }
        return null
    }

    /**
     * Re-reads the whole zone model into Room. Every mutation ends with this instead of mirroring
     * its own response: the endpoints return partial views (one template, one zone), so mirroring
     * cannot express a deletion, and Home reads Room. Two GETs per zone edit is cheap — zone edits
     * only happen on the profile screens, and they are already online-gated.
     */
    override suspend fun refreshZones(): Result<Unit> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }

        val templates = zonesRemoteDataSource.getTemplates()
        if (templates is Result.Error) return@withContext templates
        val overrides = zonesRemoteDataSource.getOverrides()
        if (overrides is Result.Error) return@withContext overrides
        if (templates !is Result.Success || overrides !is Result.Success) return@withContext Result.Loading

        zonesLocalDataSource.replaceAll(
            templates = templates.data,
            overrides = overrides.data,
            expiryTime = SyncTtl.computeExpiry(SyncTtl.TEMPLATES_TTL_MS),
        )
        Result.Success(Unit)
    }

    /**
     * Resolves effective zones for a date from Room (SSOT).
     * Resolution: override for date → template for day-of-week → empty list.
     */
    override suspend fun getZonesForDate(date: LocalDate): Result<List<DayZone>> = withContext(ioDispatcher) {
        val zones = resolveZoneEntitiesForDate(date)
        Result.Success(zones.map { it.toDayZone() })
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
        ).map { it.toDomain() }.suspendOnSuccess { refreshZones() }
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
        ).map { it.toDomain() }.suspendOnSuccess { refreshZones() }
    }

    override suspend fun deleteTemplate(templateId: String): Result<Unit> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.deleteTemplate(templateId).suspendOnSuccess { refreshZones() }
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
        ).map { it.toDomain() }.suspendOnSuccess { refreshZones() }
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
        ).map { list -> list.map { it.toDomain() } }.suspendOnSuccess { refreshZones() }
    }

    override suspend fun createOverride(
        date: String,
        zones: List<DailyZone>,
        name: String?,
    ): Result<TemplateOverride> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.createOverride(
            CreateOverrideRequest(
                name = name,
                dateOfDay = date,
                zones = zones.map { it.toDto() }
            )
        ).map { it.toDomain() }.suspendOnSuccess { refreshZones() }
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
        ).map { it.toDomain() }.suspendOnSuccess { refreshZones() }
    }

    override suspend fun deleteOverride(overrideId: String): Result<Unit> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.deleteOverride(overrideId).suspendOnSuccess { refreshZones() }
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
        ).map { it.toDomain() }.suspendOnSuccess { refreshZones() }
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
        ).map { list -> list.map { it.toDomain() } }.suspendOnSuccess { refreshZones() }
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
        val zones = resolveZoneEntitiesForDate(LocalDate.parse(date))
        Result.Success(zones.map { it.toDailyZone() })
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
        ).map { it.toDomain() }.suspendOnSuccess { refreshZones() }
    }

    override suspend fun deleteZone(zoneId: String): Result<Unit> = withContext(ioDispatcher) {
        checkOnline()?.let { return@withContext it }
        zonesRemoteDataSource.deleteZone(zoneId).suspendOnSuccess { refreshZones() }
    }

    /** Room resolves override-over-template in one query; see [ZonesLocalDataSource]. */
    private suspend fun resolveZoneEntitiesForDate(date: LocalDate): List<ZoneEntity> =
        zonesLocalDataSource.observeEffectiveZonesForDate(date.toString(), date.dayOfWeek.name).first()

    private fun ZoneEntity.toDayZone(): DayZone {
        val startLocalTime = try { LocalTime.parse(startTime) } catch (_: Exception) { LocalTime.of(0, 0) }
        val endLocalTime = try { LocalTime.parse(endTime) } catch (_: Exception) { LocalTime.of(0, 0) }
        return DayZone(
            id = id,
            name = name,
            startTime = startLocalTime,
            endTime = endLocalTime,
            colorHex = color,
            category = null,
        )
    }

    private fun ZoneEntity.toDailyZone(): DailyZone = DailyZone(
        id = id,
        name = name,
        startTime = startTime,
        endTime = endTime,
        color = color.orEmpty(),
    )
}
