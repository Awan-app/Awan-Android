package com.awan.feature.profile.impl.helpers

import android.content.Context
import android.text.format.DateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale

object ProfileHelper {
    fun parseHour(time: String?): Int? {
        if (time == null) return null
        return try {
            time.split(":")[0].toInt()
        } catch (_: Exception) {
            null
        }
    }

    fun parseMinute(time: String?): Int? {
        if (time == null) return null
        return try {
            time.split(":")[1].toInt()
        } catch (_: Exception) {
            null
        }
    }

    fun formatToApiTime(hour: Int, minute: Int): String {
        return String.format(Locale.US, "%02d:%02d:00", hour, minute)
    }

    fun formatDisplayTime(context: Context, hour: Int, minute: Int): String {
        return try {
            val localTime = LocalTime.of(hour, minute)
            val zonedDateTime = ZonedDateTime.of(LocalDate.now(), localTime, ZoneId.systemDefault())
            val date = Date.from(zonedDateTime.toInstant())
            DateFormat.getTimeFormat(context).format(date)
        } catch (_: Exception) {
            String.format(Locale.US, "%02d:%02d", hour, minute)
        }
    }

    fun getDisplayName(firstName: String?, lastName: String?, email: String?): String {
        if (!firstName.isNullOrBlank() || !lastName.isNullOrBlank()) {
            return "${firstName.orEmpty()} ${lastName.orEmpty()}".trim()
        }
        return email?.substringBefore("@") ?: ""
    }
}