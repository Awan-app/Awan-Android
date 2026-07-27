package com.awan.feature.profile.impl.helpers

import android.content.Context
import android.text.format.DateFormat
import java.util.Calendar
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

    fun formatDisplayTime(context: Context, hour: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        return DateFormat.getTimeFormat(context).format(calendar.time)
    }

    fun getDisplayName(firstName: String?, lastName: String?, email: String?): String {
        if (!firstName.isNullOrBlank() || !lastName.isNullOrBlank()) {
            return "${firstName.orEmpty()} ${lastName.orEmpty()}".trim()
        }
        return email?.substringBefore("@") ?: ""
    }
}