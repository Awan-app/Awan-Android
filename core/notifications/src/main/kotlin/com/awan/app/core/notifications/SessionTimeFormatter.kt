package com.awan.app.core.notifications

import android.content.Context
import android.text.format.DateFormat
import com.awan.app.core.domain.notifications.model.UpcomingSession
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Formats session times for notification text.
 *
 * Goes through [DateFormat.getTimeFormat] rather than a fixed pattern so the notification follows the
 * device's own 12/24-hour setting and locale — including Arabic numerals when the app is in Arabic.
 */
@Singleton
class SessionTimeFormatter @Inject constructor() {

    fun timeRange(context: Context, session: UpcomingSession): String {
        val format = DateFormat.getTimeFormat(context)
        val start = format.format(session.start.toDate())
        val end = format.format(session.end.toDate())
        return context.getString(R.string.notifications_time_range, start, end)
    }

    fun time(context: Context, at: LocalDateTime): String =
        DateFormat.getTimeFormat(context).format(at.toDate())

    private fun LocalDateTime.toDate(): Date = Date.from(atZone(ZoneId.systemDefault()).toInstant())
}
