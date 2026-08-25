package com.awan.app.core.notifications.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.awan.app.core.notifications.NotificationMaintenanceScheduler
import com.awan.app.core.notifications.SessionNotificationScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fires when a notification or maintenance alarm comes due.
 *
 * Does not need to know *which* event it was woken for — it just asks the scheduler to rebuild
 * everything, which posts whatever is now due and sets the next event alarms. The independent
 * maintenance alarm is also renewed here so an empty schedule can never terminate every future
 * wake-up.
 */
@AndroidEntryPoint
class SessionAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduler: SessionNotificationScheduler

    @Inject
    lateinit var maintenanceScheduler: NotificationMaintenanceScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob()).launch {
            try {
                scheduler.rescheduleAll()
            } catch (e: Exception) {
                // A throw here would kill the event-alarm chain silently. The maintenance alarm and
                // next foreground catch-up recover it, but the reason must not be lost.
                Log.e(TAG, "Failed to handle session alarm", e)
            } finally {
                // Independent of whether the current plan contains another event, and independent of
                // rescheduleAll succeeding. This is the safety net that guarantees another local-day
                // reconciliation attempt.
                maintenanceScheduler.scheduleNext()
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AwanNotifications"
    }
}
