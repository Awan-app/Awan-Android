package com.awan.app.core.data.zones.mapper

import com.awan.app.core.model.DailyZone
import com.awan.app.core.model.DayOfWeek
import com.awan.app.core.model.TemplateOverride
import com.awan.app.core.model.WeeklyTemplate
import com.awan.app.core.network.dto.TemplateOverrideDto
import com.awan.app.core.network.dto.WeeklyTemplateDto
import com.awan.app.core.network.dto.ZoneDto

fun ZoneDto.toDomain(): DailyZone = DailyZone(
    id = id,
    name = name,
    startTime = startTime,
    endTime = endTime,
    color = color,
    templateId = templateId,
    templateOverrideId = templateOverrideId
)

fun DailyZone.toDto(): ZoneDto = ZoneDto(
    id = id,
    name = name,
    startTime = startTime,
    endTime = endTime,
    color = color,
    templateId = templateId,
    templateOverrideId = templateOverrideId
)

fun WeeklyTemplateDto.toDomain(): WeeklyTemplate = WeeklyTemplate(
    id = id,
    name = name,
    daysOfWeek = daysOfWeek.map { DayOfWeek.valueOf(it) },
    zones = zones.map { it.toDomain() }
)

fun TemplateOverrideDto.toDomain(): TemplateOverride = TemplateOverride(
    id = id,
    dateOfDay = dateOfDay,
    zones = zones.map { it.toDomain() }
)
