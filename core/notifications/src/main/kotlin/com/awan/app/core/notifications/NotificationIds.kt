package com.awan.app.core.notifications

import com.awan.app.core.notifications.model.SessionNotificationEvent

/**
 * Notification ids are derived from the session id, never allocated.
 *
 * That is what lets the scheduler reconcile: it rebuilds the ids the plan justifies and cancels every
 * posted session notification outside that set — without persisting a ledger that
 * `replaceSessionsForDates` would wipe on the next sync.
 */
object NotificationIds {

    private const val REMINDER_TAG = "reminder"
    private const val LIVE_TAG = "live"
    private const val ENDED_TAG = "ended"

    fun forEvent(event: SessionNotificationEvent): Int = when (event) {
        is SessionNotificationEvent.Reminder -> reminder(event.session.id)
        is SessionNotificationEvent.Live -> live(event.session.id)
        is SessionNotificationEvent.Ended -> ended(event.session.id)
    }

    fun reminder(sessionId: String): Int = id(sessionId, REMINDER_TAG)

    fun live(sessionId: String): Int = id(sessionId, LIVE_TAG)

    fun ended(sessionId: String): Int = id(sessionId, ENDED_TAG)

    private fun id(sessionId: String, tag: String): Int = "$tag:$sessionId".hashCode()
}
