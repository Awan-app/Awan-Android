package com.awan.feature.profile.impl.presentation

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.Session
import com.awan.app.core.domain.zones.model.TemplateOverride
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.app.core.domain.zones.repository.ZonesRepository
import com.awan.app.core.model.DayZone
import java.time.LocalDate

class FakeZonesRepository : ZonesRepository {
    var templates: List<WeeklyTemplate> = emptyList()
    var overrides: List<TemplateOverride> = emptyList()
    var failWith: com.awan.app.core.common.error.AppError? = null

    override suspend fun getTemplates(): Result<List<WeeklyTemplate>> =
        failWith?.let { Result.Error(it) } ?: Result.Success(templates)

    override suspend fun getOverrides(): Result<List<TemplateOverride>> =
        failWith?.let { Result.Error(it) } ?: Result.Success(overrides)

    override suspend fun updateTemplateZones(templateId: String, zones: List<DailyZone>): Result<List<DailyZone>> {
        templates = templates.map { if (it.id == templateId) it.copy(zones = zones) else it }
        return Result.Success(zones)
    }

    override suspend fun updateOverrideZones(overrideId: String, zones: List<DailyZone>): Result<List<DailyZone>> {
        overrides = overrides.map { if (it.id == overrideId) it.copy(zones = zones) else it }
        return Result.Success(zones)
    }

    override suspend fun getZonesForDate(date: LocalDate): Result<List<DayZone>> = Result.Success(emptyList())

    override suspend fun createTemplate(name: String, daysOfWeek: List<DayOfWeek>, zones: List<DailyZone>): Result<WeeklyTemplate> {
        val new = WeeklyTemplate("new", name, daysOfWeek, zones)
        templates = templates + new
        return Result.Success(new)
    }

    override suspend fun getTemplate(templateId: String): Result<WeeklyTemplate> = 
        Result.Success(templates.first { it.id == templateId })

    override suspend fun updateTemplate(templateId: String, name: String, daysOfWeek: List<DayOfWeek>): Result<WeeklyTemplate> = TODO()

    override suspend fun deleteTemplate(templateId: String): Result<Unit> {
        templates = templates.filterNot { it.id == templateId }
        return Result.Success(Unit)
    }

    override suspend fun addZoneToTemplate(templateId: String, zone: DailyZone): Result<DailyZone> = TODO()

    override suspend fun getTemplateZones(templateId: String): Result<List<DailyZone>> = TODO()

    override suspend fun createOverride(date: String, zones: List<DailyZone>): Result<TemplateOverride> = TODO()

    override suspend fun getOverride(overrideId: String): Result<TemplateOverride> = TODO()

    override suspend fun updateOverride(overrideId: String, name: String?, date: String): Result<TemplateOverride> = TODO()

    override suspend fun deleteOverride(overrideId: String): Result<Unit> = TODO()

    override suspend fun addZoneToOverride(overrideId: String, zone: DailyZone): Result<DailyZone> = TODO()

    override suspend fun getOverrideZones(overrideId: String): Result<List<DailyZone>> = TODO()

    override suspend fun getZone(zoneId: String): Result<DailyZone> = TODO()

    override suspend fun getZoneSessions(zoneId: String): Result<List<Session>> = TODO()

    override suspend fun getEffectiveZones(date: String): Result<List<DailyZone>> = TODO()

    override suspend fun updateZone(zoneId: String, zone: DailyZone): Result<DailyZone> = TODO()

    override suspend fun deleteZone(zoneId: String): Result<Unit> = TODO()
}
