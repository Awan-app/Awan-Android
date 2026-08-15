package com.awan.app.core.datastore.model

import com.awan.app.core.model.NotificationPreferences

data class UserPreferencesData(
    val darkThemeEnabled: Boolean = false,

    val useDynamicColor: Boolean = true,

    val onboardingCompleted: Boolean = false,

    val defaultZone: String = "",

    val locale: String = "",

    val defaultRegion: String = "",

    val micPermissionRequested: Boolean = false,

    val notificationPreferences: NotificationPreferences = NotificationPreferences(),
)
