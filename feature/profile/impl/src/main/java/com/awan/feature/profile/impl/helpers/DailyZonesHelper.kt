package com.awan.feature.profile.impl.helpers

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.error.toUiText
import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.feature.profile.impl.R
import java.text.SimpleDateFormat
import java.util.*

object DailyZonesHelper {

    fun isOverlapping(newZone: DailyZone, existingZones: List<DailyZone>): Boolean {
        val newStart = parseTimeToMinutes(newZone.startTime)
        val newEnd = parseTimeToMinutes(newZone.endTime)

        if (newStart >= newEnd) return true

        return existingZones.any { existing ->
            if (existing.id != null && newZone.id != null && existing.id == newZone.id) return@any false

            val existStart = parseTimeToMinutes(existing.startTime)
            val existEnd = parseTimeToMinutes(existing.endTime)

            (newStart < existEnd && newEnd > existStart)
        }
    }

    fun hasOverlappingZones(zones: List<DailyZone>): Boolean {
        if (zones.isEmpty()) return false
        val sorted = zones.sortedBy { parseTimeToMinutes(it.startTime) }
        for (i in 0 until sorted.size - 1) {
            val currentEnd = parseTimeToMinutes(sorted[i].endTime)
            val nextStart = parseTimeToMinutes(sorted[i + 1].startTime)
            if (currentEnd > nextStart) return true
        }
        return false
    }

    fun parseTimeToMinutes(time: String): Int {
        return try {
            val parts = time.split(":")
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun zonesErrorToUiText(error: AppError): UiText = when {
        error is AppError.Api && error.errorCode == "ZONE_OVERLAP" ->
            UiText.StringResource(R.string.profile_daily_zones_error_overlap)
        error is AppError.Api && error.errorCode == "DAY_ALREADY_ASSIGNED" ->
            UiText.StringResource(R.string.profile_daily_zones_error_day_assigned)
        error is AppError.Network || error is AppError.Timeout ->
            UiText.StringResource(R.string.profile_daily_zones_error_network)
        error is AppError.Api ->
            UiText.StringResource(R.string.profile_daily_zones_error_generic)
        else -> error.toUiText()
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

    fun getCurrentDay(): DayOfWeek {
        val calendar = Calendar.getInstance()
        return calendarDayToDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK))
    }

    fun isToday(day: DayOfWeek): Boolean {
        return day == getCurrentDay()
    }


    private fun calendarDayToDayOfWeek(calendarDay: Int): DayOfWeek = when (calendarDay) {
        Calendar.MONDAY -> DayOfWeek.MONDAY
        Calendar.TUESDAY -> DayOfWeek.TUESDAY
        Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
        Calendar.THURSDAY -> DayOfWeek.THURSDAY
        Calendar.FRIDAY -> DayOfWeek.FRIDAY
        Calendar.SATURDAY -> DayOfWeek.SATURDAY
        Calendar.SUNDAY -> DayOfWeek.SUNDAY
        else -> DayOfWeek.MONDAY
    }

    fun formatMinutesToTime(minutes: Int): String {
        val h = (minutes / 60) % 24
        val m = minutes % 60
        return String.format(Locale.US, "%02d:%02d", h, m)
    }
}
