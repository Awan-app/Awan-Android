package com.awan.app.core.domain.profile.model

data class UserData(
    val darkThemeEnabled: Boolean,
    val locale: String,
    val micPermissionRequested: Boolean = false,
)
