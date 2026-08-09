package com.awan.app.core.data.zones.mapper

import com.awan.app.core.data.category.toModel
import com.awan.app.core.data.common.extractDateFromIso
import com.awan.app.core.data.common.extractTimeFromIso
import com.awan.app.core.database.model.SessionEntity
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.Session
import com.awan.app.core.domain.zones.model.TemplateOverride
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.network.dto.session.SessionDto
import com.awan.app.core.network.dto.zone.TemplateOverrideDto
import com.awan.app.core.network.dto.zone.WeeklyTemplateDto
import com.awan.app.core.network.dto.zone.ZoneDto
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val SessionDateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

// The server reads `categoryId` and writes back a nested `category`. Reading the id off that nested
// object here is what lets a load-edit-save round-trip keep its category without every call site
// having to carry one — and the backend rejects a zone that arrives without it.
fun ZoneDto.toDomain(): DailyZone = DailyZone(
    id = id,
    name = name,
    startTime = startTime,
    endTime = endTime,
    color = color ?: "#2E8BFF",
    templateId = templateId,
    templateOverrideId = templateOverrideId,
    categoryId = categoryId ?: category?.id,
    category = category?.toModel()
)

fun DailyZone.toDto(): ZoneDto = ZoneDto(
    id = id,
    name = name,
    startTime = startTime,
    endTime = endTime,
    color = color ?: "#2E8BFF",
    templateId = templateId,
    templateOverrideId = templateOverrideId,
    categoryId = categoryId
)

fun WeeklyTemplateDto.toDomain(): WeeklyTemplate = WeeklyTemplate(
    id = id,
    name = name,
    daysOfWeek = daysOfWeek.mapNotNull { 
        try {
            DayOfWeek.valueOf(it.uppercase()) 
        } catch (e: Exception) {
            null // Handle invalid data explicitly by skipping it instead of defaulting to MONDAY
        }
    },
    zones = zones.map { it.toDomain() },
    category = category?.toModel()
)

fun TemplateOverrideDto.toDomain(): TemplateOverride = TemplateOverride(
    id = id,
    name = name,
    dateOfDay = dateOfDay,
    zones = zones.map { it.toDomain() },
    category = category?.toModel()
)

fun SessionDto.toDomain(): Session = Session(
    id = id,
    start = LocalDateTime.parse(start, SessionDateTimeFormatter),
    end = LocalDateTime.parse(end, SessionDateTimeFormatter),
    status = status.toSessionStatus(),
    locked = locked,
    zoneId = zoneId,
    taskId = taskId,
    category = category?.toModel()
)

fun String?.toSessionStatus(): SessionStatus {
    if (this == null) return SessionStatus.SCHEDULED
    return when (this.uppercase()) {
        "SCHEDULED" -> SessionStatus.SCHEDULED
        "IN_PROGRESS" -> SessionStatus.IN_PROGRESS
        "COMPLETED" -> SessionStatus.COMPLETED
        "MISSED" -> SessionStatus.MISSED
        "CANCELLED" -> SessionStatus.CANCELLED
        else -> SessionStatus.UNKNOWN
    }
}

fun SessionDto.toEntity(): SessionEntity = SessionEntity(
    id = id,
    taskId = taskId ?: "",
    zoneId = zoneId,
    date = extractDateFromIso(start),
    startTime = extractTimeFromIso(start),
    endTime = extractTimeFromIso(end),
    status = status ?: "SCHEDULED",
    locked = locked
)
