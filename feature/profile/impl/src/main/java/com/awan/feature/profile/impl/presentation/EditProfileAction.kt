package com.awan.feature.profile.impl.presentation

sealed interface EditProfileAction {
    data class FirstNameChange(val name: String) : EditProfileAction
    data class LastNameChange(val name: String) : EditProfileAction
    data class BirthDateChange(val date: String) : EditProfileAction
    data class UpdateSleepSchedule(val wakeup: String, val sleep: String) : EditProfileAction
    data class UpdateTimezone(val timezone: String) : EditProfileAction
    data class UpdateSessionDuration(val duration: Int) : EditProfileAction
    data class UpdateSchedulingType(val type: String) : EditProfileAction
    data object Save : EditProfileAction
}
