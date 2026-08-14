package com.awan.app.core.notifications

import android.content.Context
import android.util.Log
import com.awan.app.core.notifications.model.SessionWindow
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers that the user pressed "Stop" on a running session's notification.
 *
 * The live event is due for the whole session, and `rescheduleAll()` runs on every session write and
 * every app foreground, so without this the notification is back on screen seconds after it was
 * dismissed. It cannot be kept beside the session either: `SessionDao.replaceSessionsForDates` wipes
 * and rewrites whole date ranges on every sync.
 *
 * What is stored is the session's **window**, not just its id. The dismissal applies only while the
 * session is still scheduled exactly as it was when the button was pressed — move it, snooze it or
 * reschedule it and the live notification comes back, which is right: a re-planned block is a new
 * thing to be told about, and silently swallowing its live update is the worse failure.
 *
 * ponytail: one flat map in SharedPreferences, pruned on every read so expired windows never
 * accumulate. It only ever holds sessions running right now, which the conflict engine keeps to
 * roughly one. Upgrade path if this grows: move it into the proto DataStore beside the preferences.
 */
@Singleton
class LiveNotificationDismissals @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val preferences by lazy {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    /**
     * The dismissals that still mean something, with every expired one deleted as a side effect.
     *
     * Pruning here rather than on a timer is what keeps the file from growing forever: this is read
     * on every reschedule — every alarm, every session write, every app foreground — so an entry
     * outlives its session by one reschedule at most.
     */
    fun current(now: LocalDateTime): Map<String, SessionWindow> {
        val stored = preferences.all.mapNotNull { (sessionId, raw) ->
            val window = (raw as? String)?.let(LiveDismissalRecord::decode)
            if (window == null) {
                Log.w(TAG, "Dropping unreadable dismissal record for $sessionId")
                return@mapNotNull null
            }
            sessionId to window
        }
        val (expired, live) = stored.partition { (_, window) ->
            LiveDismissalRecord.isExpired(window, now)
        }

        val unreadable = preferences.all.keys - stored.map { it.first }.toSet()
        val drop = expired.map { it.first } + unreadable
        if (drop.isNotEmpty()) {
            preferences.edit().apply { drop.forEach(::remove) }.apply()
        }

        return live.toMap()
    }

    fun dismiss(sessionId: String, window: SessionWindow) {
        preferences.edit().putString(sessionId, LiveDismissalRecord.encode(window)).apply()
    }

    private companion object {
        const val TAG = "AwanNotifications"
        const val FILE_NAME = "awan_live_notification_dismissals"
    }
}
