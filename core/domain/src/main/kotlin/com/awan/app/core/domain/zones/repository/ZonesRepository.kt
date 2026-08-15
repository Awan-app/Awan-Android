package com.awan.app.core.domain.zones.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.Session
import com.awan.app.core.domain.zones.model.TemplateOverride
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.app.core.model.DayZone
import java.time.LocalDate

interface ZonesRepository {

    /**
     * Pulls the whole zone model into Room, replacing what is there. Every mutation ends with it, so
     * screens only need it when they open — before that, they render whatever Room already holds.
     */
    suspend fun refreshZones(): Result<Unit>

    suspend fun getZonesForDate(date: LocalDate): Result<List<DayZone>>

    // Templates
    suspend fun getTemplates(): Result<List<WeeklyTemplate>>
    suspend fun createTemplate(name: String, daysOfWeek: List<DayOfWeek>, zones: List<DailyZone>): Result<WeeklyTemplate>
    suspend fun getTemplate(templateId: String): Result<WeeklyTemplate>
    suspend fun updateTemplate(templateId: String, name: String, daysOfWeek: List<DayOfWeek>): Result<WeeklyTemplate>
    suspend fun deleteTemplate(templateId: String): Result<Unit>
    suspend fun addZoneToTemplate(templateId: String, zone: DailyZone): Result<DailyZone>
    suspend fun getTemplateZones(templateId: String): Result<List<DailyZone>>
    suspend fun updateTemplateZones(templateId: String, zones: List<DailyZone>): Result<List<DailyZone>>

    // Overrides
    suspend fun createOverride(date: String, zones: List<DailyZone>): Result<TemplateOverride>
    suspend fun getOverrides(): Result<List<TemplateOverride>>
    suspend fun getOverride(overrideId: String): Result<TemplateOverride>
    suspend fun updateOverride(overrideId: String, name: String?, date: String): Result<TemplateOverride>
    suspend fun deleteOverride(overrideId: String): Result<Unit>
    suspend fun addZoneToOverride(overrideId: String, zone: DailyZone): Result<DailyZone>
    suspend fun getOverrideZones(overrideId: String): Result<List<DailyZone>>
    suspend fun updateOverrideZones(overrideId: String, zones: List<DailyZone>): Result<List<DailyZone>>

    // Zones
    suspend fun getZone(zoneId: String): Result<DailyZone>
    suspend fun getZoneSessions(zoneId: String): Result<List<Session>>
    suspend fun getEffectiveZones(date: String): Result<List<DailyZone>>
    suspend fun updateZone(zoneId: String, zone: DailyZone): Result<DailyZone>
    suspend fun deleteZone(zoneId: String): Result<Unit>
}
