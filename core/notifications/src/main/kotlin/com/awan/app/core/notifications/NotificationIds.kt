package com.awan.app.core.notifications

import com.awan.app.core.notifications.model.AwanNotificationEvent
import com.awan.app.core.notifications.model.DayNotificationEvent
import com.awan.app.core.notifications.model.SessionNotificationEvent
import java.time.LocalDate

/**
 * Notification ids are derived from what the notification is about, never allocated.
 *
 * That is what lets the scheduler reconcile: it rebuilds the ids the plan justifies and cancels every
 * posted session notification outside that set — without persisting a ledger that
 * `replaceSessionsForDates` would wipe on the next sync.
 */
object NotificationIds {

    private const val REMINDER_TAG = "reminder"
    private const val LIVE_TAG = "live"
    private const val ENDED_TAG = "ended"
    private const val FOLLOW_UP_TAG = "followup"
    private const val STREAK_RISK_TAG = "streak"
    private const val DAILY_BRIEF_TAG = "brief"

    fun forEvent(event: AwanNotificationEvent): Int = when (event) {
        is SessionNotificationEvent.Reminder -> reminder(event.session.id)
        is SessionNotificationEvent.Live -> live(event.session.id)
        is SessionNotificationEvent.Ended -> ended(event.session.id)
        is SessionNotificationEvent.FollowUp -> followUp(event.session.id)
        is DayNotificationEvent.StreakRisk -> streakRisk(event.date)
        is DayNotificationEvent.DailyBrief -> dailyBrief(event.date, event.slot)
    }

    fun reminder(sessionId: String): Int = id(sessionId, REMINDER_TAG)

    fun live(sessionId: String): Int = id(sessionId, LIVE_TAG)

    fun ended(sessionId: String): Int = id(sessionId, ENDED_TAG)

    fun followUp(sessionId: String): Int = id(sessionId, FOLLOW_UP_TAG)

    fun streakRisk(date: LocalDate): Int = id(date.toString(), STREAK_RISK_TAG)

    /**
     * The slot is part of the id, so the midday nudge is a notification of its own. Sharing an id
     * with the morning brief would make the scheduler read the midday post as a re-post of one
     * already on screen and skip it.
     */
    fun dailyBrief(date: LocalDate, slot: DayNotificationEvent.DailyBrief.Slot): Int =
        id(date.toString(), "$DAILY_BRIEF_TAG:${slot.name}")

    private fun id(key: String, tag: String): Int = "$tag:$key".hashCode()
}
