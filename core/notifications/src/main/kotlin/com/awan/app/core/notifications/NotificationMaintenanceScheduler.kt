package com.awan.app.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.awan.app.core.notifications.receiver.SessionAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the notification scheduler reachable even when today's plan has no future events.
 *
 * Session alarms intentionally disappear when there is nothing left to notify about. Without an
 * independent wake-up, that can also remove the only opportunity to build tomorrow's daily events,
 * leaving notifications dormant until the app process starts again. This alarm is not a user
 * notification; it simply wakes [SessionAlarmReceiver] shortly after the local date changes so the
 * normal scheduler can rebuild from Room.
 */
@Singleton
class NotificationMaintenanceScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleNext() {
        val now = LocalDateTime.now(clock)
        val todayCheckpoint = now.toLocalDate().atTime(CHECKPOINT_TIME)
        val nextCheckpoint = if (now.isBefore(todayCheckpoint)) {
            todayCheckpoint
        } else {
            todayCheckpoint.plusDays(1)
        }
        val triggerAt = nextCheckpoint.atZone(clock.zone).toInstant().toEpochMilli()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MAINTENANCE_ALARM_REQUEST_CODE,
            Intent(context, SessionAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            Log.w(TAG, "Exact maintenance alarm unavailable; falling back to inexact")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private companion object {
        const val TAG = "AwanNotifications"
        const val MAINTENANCE_ALARM_REQUEST_CODE = 3003
        val CHECKPOINT_TIME: LocalTime = LocalTime.of(0, 5)
    }
}
