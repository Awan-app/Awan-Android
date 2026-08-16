package com.awan.app.core.datastore.model

import com.awan.app.core.model.DarkThemeConfig

import com.awan.app.core.model.NotificationPreferences

data class UserPreferencesData(
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,

    val useDynamicColor: Boolean = true,

    val onboardingCompleted: Boolean = false,

    val defaultZone: String = "",

    val locale: String = "",

    val defaultRegion: String = "",

    val micPermissionRequested: Boolean = false,

    val notificationPreferences: NotificationPreferences = NotificationPreferences(),
)
