package com.awan.app.core.data.zone

import com.awan.app.core.model.DayZone
import com.awan.app.core.network.dto.ZoneDto
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val ApiTime: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME

/** A zone whose window doesn't parse is dropped — an unusable window is worse than a missing one. */
internal fun ZoneDto.toModel(): DayZone? {
    val start = startTime.toLocalTimeOrNull() ?: return null
    val end = endTime.toLocalTimeOrNull() ?: return null
    return DayZone(
        id = id,
        name = name,
        startTime = start,
        endTime = end,
        colorHex = color,
    )
}

private fun String.toLocalTimeOrNull(): LocalTime? =
    runCatching { LocalTime.parse(this, ApiTime) }.getOrNull()
