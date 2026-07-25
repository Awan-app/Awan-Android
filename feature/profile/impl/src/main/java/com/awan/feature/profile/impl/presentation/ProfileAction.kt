package com.awan.feature.profile.impl.presentation

sealed interface ProfileAction {
    data object Refresh : ProfileAction
    data class SetTheme(val useDarkTheme: Boolean) : ProfileAction
    data class SetLanguage(val languageCode: String) : ProfileAction
    data class UpdateSleepSchedule(val wakeupTime: String, val sleepTime: String) : ProfileAction
    data class UpdateSessionDuration(val duration: Int) : ProfileAction
    data class UpdateTimezone(val timezone: String) : ProfileAction
    data object Logout : ProfileAction
}
