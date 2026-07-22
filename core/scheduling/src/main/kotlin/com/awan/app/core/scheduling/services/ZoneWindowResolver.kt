package com.awan.app.core.scheduling.services

import com.awan.app.core.scheduling.entities.Zone
import com.awan.app.core.scheduling.valueobjects.TimeRange
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

interface ZoneWindowResolving {
    fun window(zone: Zone, day: Instant, timeZone: ZoneId): TimeRange
}

class CalendarZoneWindowResolver : ZoneWindowResolving {
    override fun window(zone: Zone, day: Instant, timeZone: ZoneId): TimeRange {
        val zoned = ZonedDateTime.ofInstant(day, timeZone)

        var start = zoned
            .withHour(zone.startTime.hour)
            .withMinute(zone.startTime.minute)
            .withSecond(0)
            .withNano(0)

        var end = zoned
            .withHour(zone.endTime.hour)
            .withMinute(zone.endTime.minute)
            .withSecond(0)
            .withNano(0)

        // Handle overnight zones (e.g. 22:00 – 06:00)
        if (zone.endTime <= zone.startTime) {
            end = end.plusDays(1)
        }

        return TimeRange(start.toInstant(), end.toInstant())
    }
}
