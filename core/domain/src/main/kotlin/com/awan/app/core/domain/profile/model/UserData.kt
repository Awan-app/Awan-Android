package com.awan.app.core.domain.profile.model

import com.awan.app.core.model.DarkThemeConfig

data class UserData(
    val darkThemeConfig: DarkThemeConfig,
    val locale: String,
    val micPermissionRequested: Boolean = false,
)
