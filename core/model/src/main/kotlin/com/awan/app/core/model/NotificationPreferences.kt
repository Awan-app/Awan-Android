package com.awan.app.core.model

/**
 * Which local notifications the user wants, and how they are timed.
 *
 * Every toggle defaults to on. The proto stores them negated because proto3 bools default to
 * `false`; the flip happens once, in the datastore mapper, so nothing above it reads a double
 * negative.
 */
data class NotificationPreferences(
    val sessionRemindersEnabled: Boolean = true,
    val sessionLiveActivityEnabled: Boolean = true,
    val sessionEndEnabled: Boolean = true,
    val sessionFollowUpEnabled: Boolean = true,
    val streakReminderEnabled: Boolean = true,
    val dailyBriefEnabled: Boolean = true,
    val rewardsEnabled: Boolean = true,
    val reminderLeadMinutes: Int = DEFAULT_REMINDER_LEAD_MINUTES,
    val snoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    val followUpDelayMinutes: Int = DEFAULT_FOLLOW_UP_MINUTES,
) {
    companion object {
        const val DEFAULT_REMINDER_LEAD_MINUTES = 10

        /**
         * Snooze asks for a length instead of applying a stored one.
         *
         * A sentinel rather than a second field: it travels through the same proto int, the same
         * choice row and the same worker input as a real duration, and every place that has to tell
         * the two apart already has to branch anyway.
         */
        const val SNOOZE_ASK = -1

        /** New users are asked; there is no length to guess before they have snoozed anything. */
        const val DEFAULT_SNOOZE_MINUTES = SNOOZE_ASK

        /** Long enough that it reads as "how did that go?" rather than as nagging. */
        const val DEFAULT_FOLLOW_UP_MINUTES = 90

        val REMINDER_LEAD_CHOICES = listOf(5, 10, 15, 30)
        val SNOOZE_CHOICES = listOf(5, 10, 15, SNOOZE_ASK)
        // Must contain DEFAULT_FOLLOW_UP_MINUTES: the choice row renders nothing as selected when
        // the stored value is not one of its options.
        val FOLLOW_UP_CHOICES = listOf(30, 60, 90, 120)

        /** The lengths that are actual lengths — the picker's buttons, and what a snooze may move by. */
        val SNOOZE_LENGTH_CHOICES = SNOOZE_CHOICES.filter { it != SNOOZE_ASK }
    }
}
