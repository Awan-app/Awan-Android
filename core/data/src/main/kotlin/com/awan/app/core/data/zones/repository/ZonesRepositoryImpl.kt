package com.awan.app.core.data.zones.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.data.zones.mapper.toDomain
import com.awan.app.core.data.zones.mapper.toDto
import com.awan.app.core.data.zones.remote.ZonesRemoteDataSource
import com.awan.app.core.domain.zones.repository.ZonesRepository
import com.awan.app.core.model.DailyZone
import com.awan.app.core.model.DayOfWeek
import com.awan.app.core.model.TemplateOverride
import com.awan.app.core.model.WeeklyTemplate
import com.awan.app.core.network.dto.CreateOverrideRequest
import com.awan.app.core.network.dto.CreateTemplateRequest
import com.awan.app.core.network.dto.UpdateZonesRequest
import javax.inject.Inject

class ZonesRepositoryImpl @Inject constructor(
    private val zonesRemoteDataSource: ZonesRemoteDataSource
) : ZonesRepository {

    override suspend fun getTemplates(): Result<List<WeeklyTemplate>> =
        zonesRemoteDataSource.getTemplates().map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun createTemplate(
        name: String,
        daysOfWeek: List<DayOfWeek>,
        zones: List<DailyZone>
    ): Result<WeeklyTemplate> =
        zonesRemoteDataSource.createTemplate(
            CreateTemplateRequest(
                name = name,
                daysOfWeek = daysOfWeek.map { it.name },
                zones = zones.map { it.toDto() }
            )
        ).map { it.toDomain() }

    override suspend fun updateTemplateZones(
        templateId: String,
        zones: List<DailyZone>
    ): Result<List<DailyZone>> =
        zonesRemoteDataSource.updateTemplateZones(
            templateId,
            UpdateZonesRequest(zones = zones.map { it.toDto() })
        ).map { list -> list.map { it.toDomain() } }

    override suspend fun deleteTemplate(templateId: String): Result<Unit> =
        zonesRemoteDataSource.deleteTemplate(templateId)

    override suspend fun getEffectiveZones(date: String): Result<List<DailyZone>> =
        zonesRemoteDataSource.getEffectiveZones(date).map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun createOverride(
        date: String,
        zones: List<DailyZone>
    ): Result<TemplateOverride> =
        zonesRemoteDataSource.createOverride(
            CreateOverrideRequest(
                dateOfDay = date,
                zones = zones.map { it.toDto() }
            )
        ).map { it.toDomain() }

    override suspend fun updateOverrideZones(
        overrideId: String,
        zones: List<DailyZone>
    ): Result<List<DailyZone>> =
        zonesRemoteDataSource.updateOverrideZones(
            overrideId,
            UpdateZonesRequest(zones = zones.map { it.toDto() })
        ).map { list -> list.map { it.toDomain() } }

    override suspend fun deleteOverride(overrideId: String): Result<Unit> =
        zonesRemoteDataSource.deleteOverride(overrideId)
}
