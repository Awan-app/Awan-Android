package com.awan.app.core.domain.profile.repository

import com.awan.app.core.model.NotificationPreferences
import com.awan.app.core.domain.profile.model.UserData
import com.awan.app.core.model.DarkThemeConfig
import kotlinx.coroutines.flow.Flow

interface UserDataRepository {
    val userData: Flow<UserData>
    suspend fun setDarkThemeConfig(config: DarkThemeConfig)
    suspend fun setLocale(locale: String)
    suspend fun setMicPermissionRequested(requested: Boolean)
    suspend fun setNotificationPreferences(preferences: NotificationPreferences)
}
