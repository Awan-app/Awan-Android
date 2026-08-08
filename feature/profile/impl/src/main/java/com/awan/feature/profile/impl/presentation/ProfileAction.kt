package com.awan.feature.profile.impl.presentation

sealed interface ProfileAction {
    data object Refresh : ProfileAction
    data class SetTheme(val useDarkTheme: Boolean) : ProfileAction
    data class SetLanguage(val languageCode: String) : ProfileAction
    data class UpdateSleepSchedule(val wakeupTime: String, val sleepTime: String) : ProfileAction
    data class UpdateSessionDuration(val duration: Int) : ProfileAction
    data class UpdateTimezone(val timezone: String) : ProfileAction
    data class UpdatePersonalInfo(val firstName: String, val lastName: String, val birthDate: String) : ProfileAction
    data class UpdateProfilePicture(val uri: String) : ProfileAction
    data object DeleteProfilePicture : ProfileAction
    data object Logout : ProfileAction
}
