package com.awan.feature.profile.impl.helpers

import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.feature.profile.impl.R
import java.time.LocalDate
import java.util.Locale

object DailyZonesHelper {

    fun isOverlapping(newZone: DailyZone, existingZones: List<DailyZone>): Boolean {
        val newStart = parseTimeToMinutes(newZone.startTime) ?: return true
        val newEnd = parseTimeToMinutes(newZone.endTime) ?: return true

        if (newStart >= newEnd) return true

        return existingZones.any { existing ->
            if (existing.id != null && newZone.id != null && existing.id == newZone.id) return@any false

            val existStart = parseTimeToMinutes(existing.startTime) ?: return@any false
            val existEnd = parseTimeToMinutes(existing.endTime) ?: return@any false

            (newStart < existEnd && newEnd > existStart)
        }
    }

    fun hasOverlappingZones(zones: List<DailyZone>): Boolean {
        if (zones.isEmpty()) return false
        val sorted = zones.mapNotNull { zone ->
            val start = parseTimeToMinutes(zone.startTime)
            val end = parseTimeToMinutes(zone.endTime)
            if (start != null && end != null) zone to (start to end) else null
        }.sortedBy { it.second.first }

        if (sorted.size < zones.size) return true // Some were unparseable

        for (i in 0 until sorted.size - 1) {
            val currentEnd = sorted[i].second.second
            val nextStart = sorted[i + 1].second.first
            if (currentEnd > nextStart) return true
        }
        return false
    }

    fun parseTimeToMinutes(time: String): Int? {
        return try {
            val parts = time.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            null
        }
    }

    fun getDayNameRes(day: DayOfWeek): Int = when (day) {
        DayOfWeek.MONDAY -> R.string.profile_day_monday
        DayOfWeek.TUESDAY -> R.string.profile_day_tuesday
        DayOfWeek.WEDNESDAY -> R.string.profile_day_wednesday
        DayOfWeek.THURSDAY -> R.string.profile_day_thursday
        DayOfWeek.FRIDAY -> R.string.profile_day_friday
        DayOfWeek.SATURDAY -> R.string.profile_day_saturday
        DayOfWeek.SUNDAY -> R.string.profile_day_sunday
    }

    fun getDayAbbreviationRes(day: DayOfWeek): Int = when (day) {
        DayOfWeek.MONDAY -> R.string.profile_day_monday_short
        DayOfWeek.TUESDAY -> R.string.profile_day_tuesday_short
        DayOfWeek.WEDNESDAY -> R.string.profile_day_wednesday_short
        DayOfWeek.THURSDAY -> R.string.profile_day_thursday_short
        DayOfWeek.FRIDAY -> R.string.profile_day_friday_short
        DayOfWeek.SATURDAY -> R.string.profile_day_saturday_short
        DayOfWeek.SUNDAY -> R.string.profile_day_sunday_short
    }

    fun formatTime12h(context: android.content.Context, time: String): String {
        return try {
            val parts = time.split(":")
            val hours = parts[0].toInt()
            val minutes = parts[1].toInt()
            ProfileHelper.formatDisplayTime(context, hours, minutes)
        } catch (e: Exception) {
            time
        }
    }

    fun getCurrentDay(date: LocalDate): DayOfWeek {
        val javaDay = date.dayOfWeek
        return when (javaDay) {
            java.time.DayOfWeek.MONDAY -> DayOfWeek.MONDAY
            java.time.DayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
            java.time.DayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
            java.time.DayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
            java.time.DayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
            java.time.DayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
            java.time.DayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
        }
    }

    fun isToday(day: DayOfWeek, today: LocalDate): Boolean {
        return day == getCurrentDay(today)
    }

    fun formatMinutesToTime(minutes: Int): String {
        val h = (minutes / 60) % 24
        val m = minutes % 60
        return String.format(Locale.US, "%02d:%02d:00", h, m)
    }

    /**
     * The API guarantees at most one override per date.
     * This method resolves the active override for a given date.
     */
    fun findOverrideForDate(
        overrides: List<com.awan.app.core.domain.zones.model.TemplateOverride>, 
        dateStr: String?
    ): com.awan.app.core.domain.zones.model.TemplateOverride? {
        if (dateStr == null) return null
        return overrides.find { it.dateOfDay == dateStr }
    }

    fun hasTemplateForDay(
        templates: List<com.awan.app.core.domain.zones.model.WeeklyTemplate>,
        day: DayOfWeek
    ): Boolean {
        return templates.any { it.daysOfWeek.contains(day) && it.zones.isNotEmpty() }
    }
}
