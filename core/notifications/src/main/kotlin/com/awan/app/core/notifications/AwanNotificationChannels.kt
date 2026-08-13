package com.awan.app.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One channel per kind of notification, so muting the daily-spin nudge does not also mute the
 * reminder that a session is about to start.
 *
 * Creating a channel that already exists only updates its name and description — the user's own
 * importance and sound choices survive — so this is safe to call on every post.
 */
@Singleton
class AwanNotificationChannels @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun ensureCreated() {
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            channel(
                id = SESSION_REMINDERS,
                nameRes = R.string.notifications_channel_session_reminders_name,
                descriptionRes = R.string.notifications_channel_session_reminders_description,
                importance = NotificationManager.IMPORTANCE_HIGH,
            )
        )
        manager.createNotificationChannel(
            // Low: the live notification redraws every minute, and an alerting channel would buzz
            // on each redraw.
            channel(
                id = SESSION_LIVE,
                nameRes = R.string.notifications_channel_session_live_name,
                descriptionRes = R.string.notifications_channel_session_live_description,
                importance = NotificationManager.IMPORTANCE_LOW,
            )
        )
        manager.createNotificationChannel(
            channel(
                id = SESSION_END,
                nameRes = R.string.notifications_channel_session_end_name,
                descriptionRes = R.string.notifications_channel_session_end_description,
                importance = NotificationManager.IMPORTANCE_HIGH,
            )
        )
        manager.createNotificationChannel(
            channel(
                id = REWARDS,
                nameRes = R.string.notifications_channel_rewards_name,
                descriptionRes = R.string.notifications_channel_rewards_description,
                importance = NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
    }

    private fun channel(
        id: String,
        nameRes: Int,
        descriptionRes: Int,
        importance: Int,
    ) = NotificationChannel(id, context.getString(nameRes), importance).apply {
        description = context.getString(descriptionRes)
    }

    companion object {
        const val SESSION_REMINDERS = "session_reminders"
        const val SESSION_LIVE = "session_live"
        const val SESSION_END = "session_end"
        const val REWARDS = "rewards"
    }
}
