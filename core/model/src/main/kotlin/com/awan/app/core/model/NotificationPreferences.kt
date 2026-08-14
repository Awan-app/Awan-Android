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
        const val DEFAULT_SNOOZE_MINUTES = 10

        /** Long enough that it reads as "how did that go?" rather than as nagging. */
        const val DEFAULT_FOLLOW_UP_MINUTES = 90

        val REMINDER_LEAD_CHOICES = listOf(5, 10, 15, 30)
        val SNOOZE_CHOICES = listOf(5, 10, 15)
        // Must contain DEFAULT_FOLLOW_UP_MINUTES: the choice row renders nothing as selected when
        // the stored value is not one of its options.
        val FOLLOW_UP_CHOICES = listOf(30, 60, 90, 120)
    }
}
