package com.awan.app.core.notifications.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.awan.app.core.notifications.work.NotificationRescheduleWorker

/**
 * Alarms do not survive a reboot, a reinstall, or a clock change, so the chain is rebuilt on each.
 *
 * `TIMEZONE_CHANGED` is not padding: sessions are stored as a local date and time, so flying
 * somewhere moves every trigger instant even though nothing in Room changed.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            -> NotificationRescheduleWorker.enqueue(context)
        }
    }
}
