package com.awan.feature.profile.impl.presentation

sealed interface NotificationSettingsAction {
    data class SetSessionReminders(val enabled: Boolean) : NotificationSettingsAction
    data class SetLiveActivity(val enabled: Boolean) : NotificationSettingsAction
    data class SetSessionEnd(val enabled: Boolean) : NotificationSettingsAction
    data class SetSessionFollowUp(val enabled: Boolean) : NotificationSettingsAction
    data class SetStreakReminder(val enabled: Boolean) : NotificationSettingsAction
    data class SetDailyBrief(val enabled: Boolean) : NotificationSettingsAction
    data class SetRewards(val enabled: Boolean) : NotificationSettingsAction
    data class SetReminderLead(val minutes: Int) : NotificationSettingsAction
    data class SetSnooze(val minutes: Int) : NotificationSettingsAction
    data class SetFollowUpDelay(val minutes: Int) : NotificationSettingsAction

    /** Re-read on resume: the user may have changed the OS permission and come back. */
    data class SystemPermissionChanged(val enabled: Boolean) : NotificationSettingsAction
}
