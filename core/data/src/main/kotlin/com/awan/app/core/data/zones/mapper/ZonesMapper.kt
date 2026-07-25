package com.awan.app.core.data.zones.mapper

import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.Session
import com.awan.app.core.domain.zones.model.TemplateOverride
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.app.core.network.dto.auth.SessionDto
import com.awan.app.core.network.dto.zone.TemplateOverrideDto
import com.awan.app.core.network.dto.zone.WeeklyTemplateDto
import com.awan.app.core.network.dto.zone.ZoneDto

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
    name = name,
    dateOfDay = dateOfDay,
    zones = zones.map { it.toDomain() }
)

fun SessionDto.toDomain(): Session = Session(
    id = id,
    start = start,
    end = end,
    status = status,
    locked = locked,
    zoneId = zoneId,
    taskId = taskId
)
