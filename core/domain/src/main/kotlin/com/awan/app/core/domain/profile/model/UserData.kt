package com.awan.app.core.domain.profile.model

import com.awan.app.core.model.NotificationPreferences

data class UserData(
    val darkThemeEnabled: Boolean,
    val locale: String,
    val micPermissionRequested: Boolean = false,
    val notificationPreferences: NotificationPreferences = NotificationPreferences(),
)
