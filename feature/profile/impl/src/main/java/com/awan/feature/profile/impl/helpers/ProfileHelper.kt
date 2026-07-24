package com.awan.feature.profile.impl.helpers

import java.util.Locale

object ProfileHelper {
    fun parseHour(time: String?): Int {
        if (time == null) return 7
        return try {
            time.split(":")[0].toInt()
        } catch (_: Exception) {
            7
        }
    }

    fun parseMinute(time: String?): Int {
        if (time == null) return 30
        return try {
            time.split(":")[1].toInt()
        } catch (_: Exception) {
            30
        }
    }

    fun formatToApiTime(hour: Int, minute: Int): String {
        return String.format(Locale.US, "%02d:%02d:00", hour, minute)
    }

    fun formatDisplayTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val h = if (hour % 12 == 0) 12 else hour % 12
        return String.format(Locale.getDefault(), "%02d:%02d %s", h, minute, amPm)
    }

    fun getDisplayName(firstName: String?, lastName: String?, email: String?): String {
        if (!firstName.isNullOrBlank() || !lastName.isNullOrBlank()) {
            return "${firstName.orEmpty()} ${lastName.orEmpty()}".trim()
        }
        return email?.substringBefore("@") ?: "User"
    }
}