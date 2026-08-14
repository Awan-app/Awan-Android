package com.awan.feature.profile.impl.presentation

import com.awan.app.core.model.NotificationPreferences

data class NotificationSettingsState(
    val preferences: NotificationPreferences = NotificationPreferences(),
    /**
     * Everything on this screen is inert while the OS permission is denied, so the screen says so
     * rather than letting the user toggle switches that cannot produce a notification.
     */
    val systemNotificationsEnabled: Boolean = true,
    val isLoading: Boolean = true,
)
