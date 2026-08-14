package com.awan.app.core.notifications

import android.content.Context
import android.util.Log
import com.awan.app.core.notifications.model.SessionWindow
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The two things the engine has to remember about notifications it has already handled: that the user
 * pressed "Stop" on a running session, and that a one-shot notification has already been delivered.
 *
 * Both exist for the same reason. `rescheduleAll()` runs on every session write, every preference
 * change and every app foreground, and an event stays *due* for its whole grace window — so without a
 * note, a notification the user just tapped away is back on screen seconds later. Neither note can be
 * kept beside the session either: `SessionDao.replaceSessionsForDates` wipes and rewrites whole date
 * ranges on every sync.
 *
 * What is stored is a **window**, never a bare flag. A dismissal applies only while the session is
 * still scheduled exactly as it was when the button was pressed, and a delivery note applies only to
 * the moment that was actually posted — so a session that is moved, snoozed or rescheduled is a new
 * thing to be told about, and says so.
 *
 * ponytail: one flat map in SharedPreferences, pruned on every read so expired windows never
 * accumulate. It only ever holds sessions running right now plus events inside their grace, which is
 * a handful. Upgrade path if this grows: move it into the proto DataStore beside the preferences.
 */
@Singleton
class NotificationRecords @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val preferences by lazy {
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    /**
     * The live-notification dismissals that still mean something, keyed by session id.
     *
     * Reading prunes, here and in [posted]: this runs on every reschedule — every alarm, every
     * session write, every app foreground — so an entry outlives its window by one reschedule at
     * most.
     */
    fun dismissals(now: LocalDateTime): Map<String, SessionWindow> = current(DISMISSED_PREFIX, now)

    /** The one-shot notifications already delivered, keyed by notification id. */
    fun posted(now: LocalDateTime): Map<Int, SessionWindow> =
        current(POSTED_PREFIX, now).mapNotNull { (key, window) ->
            key.toIntOrNull()?.let { it to window }
        }.toMap()

    fun dismiss(sessionId: String, window: SessionWindow) = write(DISMISSED_PREFIX, sessionId, window)

    fun markPosted(notificationId: Int, window: SessionWindow) =
        write(POSTED_PREFIX, notificationId.toString(), window)

    private fun write(prefix: String, key: String, window: SessionWindow) {
        preferences.edit().putString(prefix + key, NotificationRecord.encode(window)).apply()
    }

    /**
     * Every readable, unexpired record under [prefix], with every dead one deleted as a side effect.
     *
     * Pruning ignores the prefix on purpose: one sweep clears both kinds, and entries written by an
     * older build under no prefix at all are unreadable here and go the same way.
     */
    private fun current(prefix: String, now: LocalDateTime): Map<String, SessionWindow> {
        val stored = preferences.all.mapNotNull { (key, raw) ->
            val window = (raw as? String)?.let(NotificationRecord::decode)
            if (window == null) {
                Log.w(TAG, "Dropping unreadable notification record for $key")
                return@mapNotNull null
            }
            key to window
        }
        val (expired, live) = stored.partition { (_, window) ->
            NotificationRecord.isExpired(window, now)
        }

        val unreadable = preferences.all.keys - stored.map { it.first }.toSet()
        val drop = expired.map { it.first } + unreadable
        if (drop.isNotEmpty()) {
            preferences.edit().apply { drop.forEach(::remove) }.apply()
        }

        return live.filter { (key, _) -> key.startsWith(prefix) }
            .associate { (key, window) -> key.removePrefix(prefix) to window }
    }

    private companion object {
        const val TAG = "AwanNotifications"
        const val FILE_NAME = "awan_live_notification_dismissals"

        /** Prefixed so the two kinds cannot collide: one is keyed by session id, the other by int. */
        const val DISMISSED_PREFIX = "live:"
        const val POSTED_PREFIX = "post:"
    }
}
