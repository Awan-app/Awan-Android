package com.awan.feature.profile.impl.presentation

import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.profile.model.Profile

data class ProfileState(
    val profile: Profile? = null,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val useDarkTheme: Boolean = false,
    val language: String = "en",
    val isUpdatingField: Boolean = false,
    val fieldError: UiText? = null,
)
