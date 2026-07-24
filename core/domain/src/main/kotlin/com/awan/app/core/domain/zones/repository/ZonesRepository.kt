package com.awan.app.core.domain.zones.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.model.DailyZone
import com.awan.app.core.model.DayOfWeek
import com.awan.app.core.model.TemplateOverride
import com.awan.app.core.model.WeeklyTemplate

interface ZonesRepository {
    suspend fun getTemplates(): Result<List<WeeklyTemplate>>
    suspend fun createTemplate(name: String, daysOfWeek: List<DayOfWeek>, zones: List<DailyZone>): Result<WeeklyTemplate>
    suspend fun updateTemplateZones(templateId: String, zones: List<DailyZone>): Result<List<DailyZone>>
    suspend fun deleteTemplate(templateId: String): Result<Unit>
    suspend fun getEffectiveZones(date: String): Result<List<DailyZone>>
    suspend fun createOverride(date: String, zones: List<DailyZone>): Result<TemplateOverride>
    suspend fun updateOverrideZones(overrideId: String, zones: List<DailyZone>): Result<List<DailyZone>>
    suspend fun deleteOverride(overrideId: String): Result<Unit>
}
