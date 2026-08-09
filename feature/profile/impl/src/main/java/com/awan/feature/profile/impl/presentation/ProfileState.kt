package com.awan.feature.profile.impl.presentation

import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.model.Category

sealed interface PendingPicture {
    data object Clear : PendingPicture
    data class Picked(val uri: String) : PendingPicture
}

data class ProfileState(
    val profile: Profile? = null,
    val isLoading: Boolean = false,
    val errorMessage: UiText? = null,
    val useDarkTheme: Boolean = false,
    val language: String = "en",
    val isUpdatingField: Boolean = false,
    val isUploadingPicture: Boolean = false,
    val fieldError: UiText? = null,
    val pendingPicture: PendingPicture? = null,
    val categories: List<Category> = emptyList(),
)
