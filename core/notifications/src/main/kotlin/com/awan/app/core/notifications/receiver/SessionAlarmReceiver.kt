package com.awan.app.core.notifications.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.awan.app.core.notifications.SessionNotificationScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fires when the single scheduled alarm comes due.
 *
 * Does not need to know *which* event it was woken for — it just asks the scheduler to rebuild
 * everything, which posts whatever is now due and sets the next alarm.
 */
@AndroidEntryPoint
class SessionAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scheduler: SessionNotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob()).launch {
            try {
                scheduler.rescheduleAll()
            } catch (e: Exception) {
                // A throw here would kill the alarm chain silently; the next foreground catch-up
                // recovers, but the reason must not be lost.
                Log.e(TAG, "Failed to handle session alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "AwanNotifications"
    }
}
