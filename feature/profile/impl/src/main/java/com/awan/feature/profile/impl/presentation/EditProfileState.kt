package com.awan.feature.profile.impl.presentation

import com.awan.app.core.common.text.UiText

data class EditProfileState(
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: String = "",
    val wakeupTime: String = "07:30:00",
    val sleepTime: String = "23:00:00",
    val timezone: String = "UTC",
    val preferredSessionDuration: Int = 60,
    val schedulingType: String = "SMART",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUpdatingField: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: UiText? = null,
    val fieldErrorMessage: UiText? = null,
)
